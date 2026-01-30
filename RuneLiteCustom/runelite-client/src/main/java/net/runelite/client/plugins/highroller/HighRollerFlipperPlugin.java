package net.runelite.client.plugins.highroller;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class HighRollerFlipperPlugin
{
	@Inject
	private SignalStore signalStore;

	@Inject
	@Named("highRollerExecutor")
	private ScheduledExecutorService executorService;

	protected void startUp() throws Exception
	{
		signalStore.start();
		log.info("High Roller Flipper plugin started");
	}

	protected void shutDown() throws Exception
	{
		signalStore.stop();
		executorService.shutdownNow();
		log.info("High Roller Flipper plugin stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!HighRollerFlipperConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		log.debug("High Roller config changed, refreshing signal store");
		signalStore.restart();
	}

	@Provides
	HighRollerFlipperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HighRollerFlipperConfig.class);
	}

	@Provides
	@Named("highRollerExecutor")
	ScheduledExecutorService provideExecutorService()
	{
		return Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder()
				.setNameFormat("highroller-%d")
				.setDaemon(true)
				.build());
	}
}

