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
				return;
			}

			if (e.getKeyCode() != KeyEvent.VK_SPACE)
			{
				return;
			}
			PrushAction a = new PrushAction();
			a.setV(PROTOCOL_VERSION);
			a.setSeq(seq.incrementAndGet());
			a.setTick(client.getTickCount());
			a.setType(PrushActionType.DIALOG_CONTINUE);

			String json = gson.toJson(a);
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

		// Mirror walking via world coordinates to avoid differences in scene/minimap parameters between clients.
		if (actionType == MenuAction.WALK)
		{
			WorldPoint wp = null;
			try
			{
				LocalPoint dest = client.getLocalDestinationLocation();
				if (dest != null)
				{
					wp = WorldPoint.fromLocal(client, dest);
				}
			}
			catch (Exception ignored)
			{
			}

			if (wp == null)
			{
				try
				{
					wp = WorldPoint.fromScene(client, me.getParam0(), me.getParam1(), client.getPlane());
				}
				catch (Exception e)
				{
					return;
				}
			}

			PrushAction a = new PrushAction();
			a.setV(PROTOCOL_VERSION);
			a.setSeq(seq.incrementAndGet());
			a.setTick(client.getTickCount());
			a.setType(PrushActionType.WALK_WORLD);
			a.setWorldX(wp.getX());
			a.setWorldY(wp.getY());
			a.setWorldPlane(wp.getPlane());

			String json = gson.toJson(a);
			broadcaster.broadcast(a, json);
			return;
		}

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
