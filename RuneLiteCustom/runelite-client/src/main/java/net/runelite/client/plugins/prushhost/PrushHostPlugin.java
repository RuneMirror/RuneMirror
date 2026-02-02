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

				// Try to get destination from LocalDestinationLocation first (most reliable).
				WorldPoint destWp = null;
				LocalPoint destLocal = client.getLocalDestinationLocation();
				if (destLocal != null)
				{
					destWp = WorldPoint.fromLocal(client, destLocal);
				}

				// Fallback: use the menu entry's param0/param1 (scene coordinates).
				if (destWp == null)
				{
					try
					{
						int sceneX = me.getParam0();
						int sceneY = me.getParam1();
						// Use the client-scoped conversion which accounts for the correct plane/base
						destWp = WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
						log.info("[RuneMirrorHost] WALK: Using fallback scene coords ({}, {}) -> world {} (baseX={}, baseY={}, size={}x{})",
							sceneX, sceneY, destWp,
							client.getTopLevelWorldView().getBaseX(), client.getTopLevelWorldView().getBaseY(),
							client.getTopLevelWorldView().getSizeX(), client.getTopLevelWorldView().getSizeY());
					}
					catch (Exception e)
					{
						log.warn("[RuneMirrorHost] WALK: Could not convert scene coords to world: {}", e.getMessage());
						return;
					}
				}

				if (destWp == null)
				{
					log.warn("[RuneMirrorHost] WALK detected but could not determine destination");
					return;
				}

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

				log.info("[RuneMirrorHost] Mirroring WALK as relative step dx={} dy={} from player world=({}, {}, {}) to dest world=({}, {}, {})",
					dx, dy, playerWp.getX(), playerWp.getY(), playerWp.getPlane(), destWp.getX(), destWp.getY(), destWp.getPlane());

				String json = gson.toJson(a);
				broadcaster.broadcast(a, json);
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

		String json = gson.toJson(a);
		broadcaster.broadcast(a, json);
	}

	@Provides
	PrushHostConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrushHostConfig.class);
	}
}
