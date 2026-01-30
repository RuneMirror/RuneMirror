package net.runelite.client.plugins.highroller;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.customflippingcopilot.controller.ItemController;
import net.runelite.client.plugins.customflippingcopilot.manager.CopilotLoginManager;
import net.runelite.client.plugins.customflippingcopilot.model.FlipManager;
import net.runelite.client.plugins.customflippingcopilot.model.FlipStatus;
import net.runelite.client.plugins.customflippingcopilot.model.FlipV2;
import net.runelite.client.plugins.customflippingcopilot.model.OsrsLoginManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Singleton
public class SignalStore
{
	private static final int MIN_WINDOW_SECONDS = 60;
	private static final double SECONDS_PER_HOUR = 3600.0d;

	private final FlipManager flipManager;
	private final CopilotLoginManager copilotLoginManager;
	private final OsrsLoginManager osrsLoginManager;
	private final ItemController itemController;
	private final HighRollerFlipperConfig config;
	private final ScheduledExecutorService executorService;
	private final Clock clock;

	private final AtomicLong snapshotCounter = new AtomicLong();
	private volatile Snapshot latestSnapshot;
	private ScheduledFuture<?> refreshFuture;

	@Inject
	public SignalStore(
		FlipManager flipManager,
		CopilotLoginManager copilotLoginManager,
		OsrsLoginManager osrsLoginManager,
		ItemController itemController,
		HighRollerFlipperConfig config,
		@Named("highRollerExecutor") ScheduledExecutorService executorService)
	{
		this(flipManager, copilotLoginManager, osrsLoginManager, itemController, config, executorService, Clock.systemUTC());
	}

	SignalStore(
		FlipManager flipManager,
		CopilotLoginManager copilotLoginManager,
		OsrsLoginManager osrsLoginManager,
		ItemController itemController,
		HighRollerFlipperConfig config,
		ScheduledExecutorService executorService,
		Clock clock)
	{
		this.flipManager = flipManager;
		this.copilotLoginManager = copilotLoginManager;
		this.osrsLoginManager = osrsLoginManager;
		this.itemController = itemController;
		this.config = config;
		this.executorService = executorService;
		this.clock = clock;
	}

	public synchronized void start()
	{
		if (refreshFuture != null && !refreshFuture.isCancelled())
		{
			return;
		}
		int refreshSeconds = Math.max(1, config.refreshSeconds());
		refreshFuture = executorService.scheduleWithFixedDelay(this::safeRefresh, 0, refreshSeconds, TimeUnit.SECONDS);
	}

	public synchronized void stop()
	{
		if (refreshFuture != null)
		{
			refreshFuture.cancel(true);
			refreshFuture = null;
		}
		latestSnapshot = null;
		snapshotCounter.set(0L);
	}

	public synchronized void restart()
	{
		stop();
		start();
	}

	public Optional<Snapshot> snapshot()
	{
		return Optional.ofNullable(latestSnapshot);
	}

	private void safeRefresh()
	{
		try
		{
			refresh();
		}
		catch (Exception ex)
		{
			log.warn("Failed to refresh High Roller signal snapshot", ex);
		}
	}

	private void refresh()
	{
		if (!config.signalStoreEnabled())
		{
			latestSnapshot = null;
			return;
		}

		final Instant now = clock.instant();
		final int windowSeconds = Math.max(MIN_WINDOW_SECONDS, config.windowMinutes() * 60);
		final int windowStart = (int) (now.getEpochSecond() - windowSeconds);

		Integer accountId = resolveActiveAccountId();
		List<RealizedFlip> flips = new ArrayList<>();
		Map<Integer, MutableItemStats> mutableStats = new LinkedHashMap<>();

		flipManager.aggregateFlips(windowStart, accountId, false, flip -> maybeAddFlip(windowStart, flips, mutableStats, flip));

		List<RealizedFlip> immutableFlips = Collections.unmodifiableList(flips);
		Map<Integer, ItemMetrics> metricsByItemId = buildMetricsMap(mutableStats);

		long totalProfit = immutableFlips.stream().mapToLong(RealizedFlip::getProfit).sum();
		double profitPerHour = windowSeconds > 0
			? totalProfit / (windowSeconds / SECONDS_PER_HOUR)
			: 0d;

		latestSnapshot = new Snapshot(
			snapshotCounter.incrementAndGet(),
			now.toEpochMilli(),
			windowSeconds,
			totalProfit,
			profitPerHour,
			immutableFlips.size(),
			immutableFlips,
			metricsByItemId);

		if (config.debugLoggingEnabled())
		{
			log.info("HighRoller snapshot v{} flips={} profit={} gp/hr≈{} trackedItems={}",
				latestSnapshot.getVersion(),
				latestSnapshot.getFlipCount(),
				latestSnapshot.getTotalProfit(),
				String.format("%.1f", latestSnapshot.getProfitPerHour()),
				metricsByItemId.size());
		}
	}

	private Integer resolveActiveAccountId()
	{
		String displayName = osrsLoginManager.getLastDisplayName();
		if (displayName == null)
		{
			return null;
		}
		Integer accountId = copilotLoginManager.getAccountId(displayName);
		if (accountId == null || accountId == -1)
		{
			return null;
		}
		return accountId;
	}

	private void maybeAddFlip(int windowStart, List<RealizedFlip> sink, Map<Integer, MutableItemStats> stats, FlipV2 flip)
	{
		if (flip == null || flip.getStatus() != FlipStatus.FINISHED)
		{
			return;
		}
		int closedTime = flip.getClosedTime();
		if (closedTime <= 0 || closedTime < windowStart)
		{
			return;
		}

		int quantity = flip.getClosedQuantity();
		if (quantity <= 0)
		{
			return;
		}

		long durationSeconds = Math.max(1L, (long) flip.getClosedTime() - flip.getOpenedTime());
		int itemId = flip.getItemId();

		RealizedFlip realizedFlip = new RealizedFlip(
			itemId,
			itemController.getItemName(itemId),
			flip.getAccountId(),
			flip.getProfit(),
			quantity,
			flip.getAvgBuyPrice(),
			flip.getAvgSellPrice(),
			durationSeconds,
			flip.getClosedTime());

		sink.add(realizedFlip);
		stats.computeIfAbsent(itemId, id -> new MutableItemStats(realizedFlip.getItemName(), id))
			.accumulate(realizedFlip);
	}

	private Map<Integer, ItemMetrics> buildMetricsMap(Map<Integer, MutableItemStats> mutableStats)
	{
		Map<Integer, ItemMetrics> metrics = new LinkedHashMap<>();
		mutableStats.values().forEach(stat -> metrics.put(stat.itemId, stat.toImmutable()));
		return Collections.unmodifiableMap(metrics);
	}

	@Value
	public static class Snapshot
	{
		long version;
		long generatedAtMillis;
		int windowSeconds;
		long totalProfit;
		double profitPerHour;
		int flipCount;
		List<RealizedFlip> flips;
		Map<Integer, ItemMetrics> metricsByItemId;
	}

	@Value
	public static class RealizedFlip
	{
		int itemId;
		String itemName;
		int accountId;
		long profit;
		int quantity;
		long avgBuyPrice;
		long avgSellPrice;
		long durationSeconds;
		long closedTime;
	}

	@Value
	public static class ItemMetrics
	{
		int itemId;
		String itemName;
		int flipCount;
		long totalProfit;
		long totalQuantity;
		long totalDurationSeconds;
		double profitPerHour;
		long averageProfitPerFlip;
		long bestProfit;
		long lastClosedTime;
	}

	private static final class MutableItemStats
	{
		private final String itemName;
		private final int itemId;

		private int flipCount;
		private long totalProfit;
		private long totalQuantity;
		private long totalDurationSeconds;
		private long bestProfit;
		private long lastClosedTime;

		private MutableItemStats(String itemName, int itemId)
		{
			this.itemName = itemName;
			this.itemId = itemId;
		}

		private void accumulate(RealizedFlip flip)
		{
			flipCount++;
			totalProfit += flip.getProfit();
			totalQuantity += flip.getQuantity();
			totalDurationSeconds += flip.getDurationSeconds();
			bestProfit = Math.max(bestProfit, flip.getProfit());
			lastClosedTime = Math.max(lastClosedTime, flip.getClosedTime());
		}

		private ItemMetrics toImmutable()
		{
			long averageProfitPerFlip = flipCount > 0 ? Math.round((double) totalProfit / flipCount) : 0L;
			double profitPerHour = totalDurationSeconds > 0
				? totalProfit / (totalDurationSeconds / SECONDS_PER_HOUR)
				: 0d;
			return new ItemMetrics(
				itemId,
				itemName,
				flipCount,
				totalProfit,
				totalQuantity,
				totalDurationSeconds,
				profitPerHour,
				averageProfitPerFlip,
				bestProfit,
				lastClosedTime);
		}
	}
}

