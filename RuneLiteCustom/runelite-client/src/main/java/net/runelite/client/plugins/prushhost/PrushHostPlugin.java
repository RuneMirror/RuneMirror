package net.runelite.client.plugins.prushhost;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.prushsync.PrushAction;
import net.runelite.client.plugins.prushsync.PrushActionType;
import net.runelite.client.plugins.prushsync.PrushSyncGson;

@Slf4j
@PluginDescriptor(
	name = "RuneMirror Host",
	description = "Host for synchronized multi-client control",
	tags = {"sync"},
	conflicts = {"RuneMirror Guest"}
)
public class PrushHostPlugin extends Plugin
{
	private static final int PROTOCOL_VERSION = 1;
	// Number of client ticks to wait/retry for LocalDestinationLocation before using fallback
	private static final int MAX_FALLBACK_RETRIES = 5;
	// Maximum allowed relative offset (tiles) to advertise to guests; prevents huge teleports
	private static final int MAX_RELATIVE_CLAMP = 8;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PrushHostConfig config;

	@Inject
	private KeyManager keyManager;

	private final AtomicLong seq = new AtomicLong(0);
	private Gson gson;
	private PrushHostBroadcaster broadcaster;
	private final KeyListener keyListener = new KeyListener()
	{
		@Override
		public void keyTyped(KeyEvent e)
		{
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			if (!config.enabled())
			{
				log.debug("[RuneMirrorHost] Ignoring keyPressed because host config is disabled");
				return;
			}

			if (e.getKeyCode() != KeyEvent.VK_SPACE)
			{
				// Only mirror bare spacebar presses for now.
				return;
			}
			PrushAction a = new PrushAction();
			a.setV(PROTOCOL_VERSION);
			a.setSeq(seq.incrementAndGet());
			a.setTick(client.getTickCount());
			a.setType(PrushActionType.DIALOG_CONTINUE);

			String json = gson.toJson(a);
			log.info("[RuneMirrorHost] Sending DIALOG_CONTINUE action: {}", a);
			broadcaster.broadcast(a, json);
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	};

	@Override
	protected void startUp()
	{
		gson = PrushSyncGson.create();
		broadcaster = new PrushHostBroadcaster(config.connectTimeoutMs());
		reloadTargets();
		if (keyManager != null)
		{
			keyManager.registerKeyListener(keyListener);
		}
		log.info("[RuneMirrorHost] Started (client={})", client != null ? client.hashCode() : "null");
	}

	@Override
	protected void shutDown()
	{
		if (keyManager != null)
		{
			keyManager.unregisterKeyListener(keyListener);
		}
		if (broadcaster != null)
		{
			broadcaster.close();
			broadcaster = null;
		}
		log.info("[RuneMirrorHost] Stopped");
	}

	private void reloadTargets()
	{
		if (broadcaster == null)
		{
			return;
		}

		List<InetSocketAddress> targets = new ArrayList<>();
		String raw = config.guestTargets();
		if (raw != null && !raw.trim().isEmpty())
		{
			String[] parts = raw.split(",");
			for (String p : parts)
			{
				String s = p.trim();
				if (s.isEmpty())
				{
					continue;
				}

				int idx = s.lastIndexOf(':');
				if (idx <= 0 || idx >= s.length() - 1)
				{
					log.warn("[RuneMirrorHost] Invalid guest target '{}'. Expected host:port", s);
					continue;
				}

				String host = s.substring(0, idx);
				int port;
				try
				{
					port = Integer.parseInt(s.substring(idx + 1));
				}
				catch (NumberFormatException e)
				{
					log.warn("[RuneMirrorHost] Invalid port in guest target '{}'", s);
					continue;
				}

				targets.add(new InetSocketAddress(host, port));
			}
		}

		broadcaster.setTargets(targets);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!config.enabled())
		{
			log.debug("[RuneMirrorHost] Ignoring MenuOptionClicked because host config is disabled");
			return;
		}

		MenuEntry me = event.getMenuEntry();
		if (me == null)
		{
			return;
		}

		MenuAction actionType = me.getType();
		if (actionType == null)
		{
			return;
		}

		// Special-case walking: mirror relative step from the local player instead of raw scene coords.
		if (actionType == MenuAction.WALK)
		{
			try
			{
				if (client.getLocalPlayer() == null)
				{
					log.warn("[RuneMirrorHost] WALK detected but localPlayer is null");
					return;
				}

				// Get player's current world position.
				WorldPoint playerWp = WorldPoint.fromLocal(client, client.getLocalPlayer().getLocalLocation());
				if (playerWp == null)
				{
					log.warn("[RuneMirrorHost] WALK detected but could not get player world position");
					return;
				}

				// Prefer the selected scene tile (exact click) when available
				Tile selected = client.getSelectedSceneTile();
				if (selected != null)
				{
					try
					{
						WorldPoint destFromTile = selected.getWorldLocation();
						if (destFromTile != null)
						{
							// Replay the original WALK menu action first so guests can set local destination/view
							PrushAction menuAImmediate = new PrushAction();
							menuAImmediate.setV(PROTOCOL_VERSION);
							menuAImmediate.setSeq(seq.incrementAndGet());
							menuAImmediate.setTick(client.getTickCount());
							menuAImmediate.setType(PrushActionType.MENU_ACTION);
							menuAImmediate.setParam0(me.getParam0());
							menuAImmediate.setParam1(me.getParam1());
							menuAImmediate.setOpcode(MenuAction.WALK.getId());
							menuAImmediate.setIdentifier(me.getIdentifier());
							menuAImmediate.setItemId(me.getItemId());
							menuAImmediate.setOption(me.getOption());
							menuAImmediate.setTarget(me.getTarget());
							if (client.getTopLevelWorldView() != null)
							{
								menuAImmediate.setHostBaseX(client.getTopLevelWorldView().getBaseX());
								menuAImmediate.setHostBaseY(client.getTopLevelWorldView().getBaseY());
							}
							menuAImmediate.setHostPlayerWorldX(playerWp.getX());
							menuAImmediate.setHostPlayerWorldY(playerWp.getY());
							menuAImmediate.setHostPlayerWorldPlane(playerWp.getPlane());
							net.runelite.api.coords.LocalPoint plimm = client.getLocalPlayer().getLocalLocation();
							if (plimm != null)
							{
								menuAImmediate.setHostPlayerSceneX(plimm.getSceneX());
								menuAImmediate.setHostPlayerSceneY(plimm.getSceneY());
							}
							broadcaster.broadcast(menuAImmediate, gson.toJson(menuAImmediate));
							// Also send world destination so guests that fail to set local destination can use it.
							sendWalkAction(playerWp, destFromTile);
							return;
						}
					}
					catch (Exception e)
					{
						log.warn("[RuneMirrorHost] Failed to use selectedSceneTile fallback: {}", e.getMessage(), e);
					}
				}

				// Try to get destination from LocalDestinationLocation first (most reliable).
				LocalPoint destLocal = client.getLocalDestinationLocation();
				if (destLocal == null)
				{
					// Local destination is sometimes set a tick after the MenuOptionClicked event.
					// Retry a few times before falling back to a scene-relative computed destination.
					attemptSendWalkFallback(me, playerWp, MAX_FALLBACK_RETRIES);
					return;
				}

				WorldPoint destWp = WorldPoint.fromLocal(client, destLocal);

				if (destWp == null)
				{
					log.warn("[RuneMirrorHost] WALK detected but could not determine destination");
					return;
				}

				int dx = destWp.getX() - playerWp.getX();
				int dy = destWp.getY() - playerWp.getY();

				log.info("[RuneMirrorHost] Mirroring WALK as relative step dx={} dy={} from player world=({}, {}, {}) to dest world=({}, {}, {})",
					dx, dy, playerWp.getX(), playerWp.getY(), playerWp.getPlane(), destWp.getX(), destWp.getY(), destWp.getPlane());

				// Broadcast only the original WALK menu action so guests execute the client-native walk logic
				PrushAction menuAImmediate = new PrushAction();
				menuAImmediate.setV(PROTOCOL_VERSION);
				menuAImmediate.setSeq(seq.incrementAndGet());
				menuAImmediate.setTick(client.getTickCount());
				menuAImmediate.setType(PrushActionType.MENU_ACTION);
				menuAImmediate.setParam0(me.getParam0());
				menuAImmediate.setParam1(me.getParam1());
				menuAImmediate.setOpcode(MenuAction.WALK.getId());
				menuAImmediate.setIdentifier(me.getIdentifier());
				menuAImmediate.setItemId(me.getItemId());
				menuAImmediate.setOption(me.getOption());
				menuAImmediate.setTarget(me.getTarget());
				if (client.getTopLevelWorldView() != null)
				{
					menuAImmediate.setHostBaseX(client.getTopLevelWorldView().getBaseX());
					menuAImmediate.setHostBaseY(client.getTopLevelWorldView().getBaseY());
				}
				menuAImmediate.setHostPlayerWorldX(playerWp.getX());
				menuAImmediate.setHostPlayerWorldY(playerWp.getY());
				menuAImmediate.setHostPlayerWorldPlane(playerWp.getPlane());
				net.runelite.api.coords.LocalPoint plimm = client.getLocalPlayer().getLocalLocation();
				if (plimm != null)
				{
					menuAImmediate.setHostPlayerSceneX(plimm.getSceneX());
					menuAImmediate.setHostPlayerSceneY(plimm.getSceneY());
				}
				broadcaster.broadcast(menuAImmediate, gson.toJson(menuAImmediate));
				// Also send world destination so guests that fail to set local destination can use it.
				sendWalkAction(playerWp, destWp);
			}
			catch (Exception e)
			{
				log.warn("[RuneMirrorHost] Failed to build WALK_WORLD action: {}", e.getMessage(), e);
			}

			return;
		}

		log.info("[RuneMirrorHost] Mirroring MENU_ACTION type={} p0={} p1={} id={} itemId={} opt='{}' tgt='{}'",
			actionType, me.getParam0(), me.getParam1(), me.getIdentifier(), me.getItemId(), me.getOption(), me.getTarget());

		// Do not mirror RuneLite injected actions.
		if (actionType == MenuAction.RUNELITE || actionType == MenuAction.RUNELITE_OVERLAY || actionType == MenuAction.RUNELITE_OVERLAY_CONFIG
			|| actionType == MenuAction.RUNELITE_HIGH_PRIORITY || actionType == MenuAction.RUNELITE_LOW_PRIORITY)
		{
			return;
		}

		PrushAction a = new PrushAction();
		a.setV(PROTOCOL_VERSION);
		a.setSeq(seq.incrementAndGet());
		a.setTick(client.getTickCount());
		a.setType(PrushActionType.MENU_ACTION);
		a.setParam0(me.getParam0());
		a.setParam1(me.getParam1());
		a.setOpcode(actionType.getId());
		a.setIdentifier(me.getIdentifier());
		a.setItemId(me.getItemId());
		a.setOption(me.getOption());
		a.setTarget(me.getTarget());

		if (client.getTopLevelWorldView() != null)
		{
			a.setHostBaseX(client.getTopLevelWorldView().getBaseX());
			a.setHostBaseY(client.getTopLevelWorldView().getBaseY());
		}
		WorldPoint pw = WorldPoint.fromLocal(client, client.getLocalPlayer().getLocalLocation());
		if (pw != null)
		{
			a.setHostPlayerWorldX(pw.getX());
			a.setHostPlayerWorldY(pw.getY());
			a.setHostPlayerWorldPlane(pw.getPlane());
		}

		String json = gson.toJson(a);
		broadcaster.broadcast(a, json);
	}

	@Provides
	PrushHostConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrushHostConfig.class);
	}

	/**
	 * Retry-check for LocalDestinationLocation for a few ticks before sending a scene-relative fallback.
	 */
	private void attemptSendWalkFallback(MenuEntry me, WorldPoint playerWp, int remainingAttempts)
	{
		clientThread.invokeLater(() -> {
			try
			{
				LocalPoint dl = client.getLocalDestinationLocation();
				if (dl != null)
				{
					WorldPoint destWpDelayed = WorldPoint.fromLocal(client, dl);
					if (destWpDelayed != null)
					{
						// Replay the original WALK menu action first so guests can set local destination/view
						PrushAction menuADel = new PrushAction();
						menuADel.setV(PROTOCOL_VERSION);
						menuADel.setSeq(seq.incrementAndGet());
						menuADel.setTick(client.getTickCount());
						menuADel.setType(PrushActionType.MENU_ACTION);
						menuADel.setParam0(me.getParam0());
						menuADel.setParam1(me.getParam1());
						menuADel.setOpcode(MenuAction.WALK.getId());
						menuADel.setIdentifier(me.getIdentifier());
						menuADel.setItemId(me.getItemId());
						menuADel.setOption(me.getOption());
						menuADel.setTarget(me.getTarget());
						if (client.getTopLevelWorldView() != null)
						{
							menuADel.setHostBaseX(client.getTopLevelWorldView().getBaseX());
							menuADel.setHostBaseY(client.getTopLevelWorldView().getBaseY());
						}
						menuADel.setHostPlayerWorldX(playerWp.getX());
						menuADel.setHostPlayerWorldY(playerWp.getY());
						menuADel.setHostPlayerWorldPlane(playerWp.getPlane());
						net.runelite.api.coords.LocalPoint pld = client.getLocalPlayer().getLocalLocation();
						if (pld != null)
						{
							menuADel.setHostPlayerSceneX(pld.getSceneX());
							menuADel.setHostPlayerSceneY(pld.getSceneY());
						}
						broadcaster.broadcast(menuADel, gson.toJson(menuADel));
						// Also send a reliable absolute world destination so guests can fall back
						// to world-based conversion if the MENU_ACTION doesn't set a local destination.
						sendWalkAction(playerWp, destWpDelayed);
						return;
					}
				}
				// if we still have attempts left, try again next tick
				if (remainingAttempts > 1)
				{
					attemptSendWalkFallback(me, playerWp, remainingAttempts - 1);
					return;
				}
				// Last attempt: compute scene-relative fallback and clamp the vector.
				int sceneX = me.getParam0();
				int sceneY = me.getParam1();
				net.runelite.api.coords.LocalPoint playerLocal = client.getLocalPlayer().getLocalLocation();
				int playerSceneX = playerLocal == null ? 0 : playerLocal.getSceneX();
				int playerSceneY = playerLocal == null ? 0 : playerLocal.getSceneY();
				int dx = sceneX - playerSceneX;
				int dy = sceneY - playerSceneY;
				// Clamp dx/dy to avoid advertising huge relative jumps
				int clampedDx = Math.max(-MAX_RELATIVE_CLAMP, Math.min(MAX_RELATIVE_CLAMP, dx));
				int clampedDy = Math.max(-MAX_RELATIVE_CLAMP, Math.min(MAX_RELATIVE_CLAMP, dy));
				WorldPoint fallback = new WorldPoint(playerWp.getX() + clampedDx, playerWp.getY() + clampedDy, playerWp.getPlane());
				if (clampedDx != dx || clampedDy != dy)
				{
					log.warn("[RuneMirrorHost] Clamped scene-relative fallback dx/dy from ({},{}) to ({},{}) to avoid large teleport", dx, dy, clampedDx, clampedDy);
				}
				log.info("[RuneMirrorHost] WALK (final fallback): Using scene coords ({}, {}) relative dx={} dy={} -> world {} (player world={},{},{})",
					sceneX, sceneY, clampedDx, clampedDy, fallback, playerWp.getX(), playerWp.getY(), playerWp.getPlane());
				// Replay the original WALK menu action first so guests can set local destination/view
				PrushAction menuA = new PrushAction();
				menuA.setV(PROTOCOL_VERSION);
				menuA.setSeq(seq.incrementAndGet());
				menuA.setTick(client.getTickCount());
				menuA.setType(PrushActionType.MENU_ACTION);
				menuA.setParam0(me.getParam0());
				menuA.setParam1(me.getParam1());
				menuA.setOpcode(MenuAction.WALK.getId());
				menuA.setIdentifier(me.getIdentifier());
				menuA.setItemId(me.getItemId());
				menuA.setOption(me.getOption());
				menuA.setTarget(me.getTarget());
				if (client.getTopLevelWorldView() != null)
				{
					menuA.setHostBaseX(client.getTopLevelWorldView().getBaseX());
					menuA.setHostBaseY(client.getTopLevelWorldView().getBaseY());
				}
				menuA.setHostPlayerWorldX(playerWp.getX());
				menuA.setHostPlayerWorldY(playerWp.getY());
				menuA.setHostPlayerWorldPlane(playerWp.getPlane());
				net.runelite.api.coords.LocalPoint pl = client.getLocalPlayer().getLocalLocation();
				if (pl != null)
				{
					menuA.setHostPlayerSceneX(pl.getSceneX());
					menuA.setHostPlayerSceneY(pl.getSceneY());
				}
				broadcaster.broadcast(menuA, gson.toJson(menuA));
				// Also send WALK_WORLD fallback so guests can convert absolute world coords when MENU_ACTION fails
				try
				{
					sendWalkAction(playerWp, fallback);
				}
				catch (Exception e)
				{
					log.warn("[RuneMirrorHost] Failed to send WALK_WORLD fallback: {}", e.getMessage(), e);
				}
			}
			catch (Exception e)
			{
				log.warn("[RuneMirrorHost] attemptSendWalkFallback failed: {}", e.getMessage(), e);
			}
		});
	}

	private void sendWalkAction(net.runelite.api.coords.WorldPoint playerWp, net.runelite.api.coords.WorldPoint destWp)
	{
		int dx = destWp.getX() - playerWp.getX();
		int dy = destWp.getY() - playerWp.getY();

		PrushAction a = new PrushAction();
		a.setV(PROTOCOL_VERSION);
		a.setSeq(seq.incrementAndGet());
		a.setTick(client.getTickCount());
		a.setType(PrushActionType.WALK_WORLD);
		// Send absolute world destination coordinates to the guest.
		a.setWorldX(destWp.getX());
		a.setWorldY(destWp.getY());
		a.setWorldPlane(destWp.getPlane());
		// Also send relative offsets so guests can compute destination from their own player position.
		a.setRelDx(dx);
		a.setRelDy(dy);

		// Attach host worldview + player world for guest reconstruction
		if (client.getTopLevelWorldView() != null)
		{
			a.setHostBaseX(client.getTopLevelWorldView().getBaseX());
			a.setHostBaseY(client.getTopLevelWorldView().getBaseY());
		}
		a.setHostPlayerWorldX(playerWp.getX());
		a.setHostPlayerWorldY(playerWp.getY());
		a.setHostPlayerWorldPlane(playerWp.getPlane());
		net.runelite.api.coords.LocalPoint pl = client.getLocalPlayer().getLocalLocation();
		if (pl != null)
		{
			a.setHostPlayerSceneX(pl.getSceneX());
			a.setHostPlayerSceneY(pl.getSceneY());
		}

		log.info("[RuneMirrorHost] Mirroring WALK as relative step dx={} dy={} from player world=({}, {}, {}) to dest world=({}, {}, {})",
			dx, dy, playerWp.getX(), playerWp.getY(), playerWp.getPlane(), destWp.getX(), destWp.getY(), destWp.getPlane());

		String json = gson.toJson(a);
		broadcaster.broadcast(a, json);

	}
}
