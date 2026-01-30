package net.runelite.client.plugins.prushguest;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.prushsync.PrushAction;
import net.runelite.client.plugins.prushsync.PrushActionType;
import net.runelite.client.plugins.prushsync.PrushSyncGson;

@Slf4j
@PluginDescriptor(
	name = "RuneMirror Guest",
	description = "Guest executor for synchronized multi-client control",
	tags = {"sync"},
	conflicts = {"RuneMirror Host"}
)
public class PrushGuestPlugin extends Plugin
{
	private static final int PROTOCOL_VERSION = 1;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PrushGuestConfig config;

	private Gson gson;
	private PrushGuestServer server;

	@Override
	protected void startUp()
	{
		gson = PrushSyncGson.create();
		startServer();
		log.info("[RuneMirrorGuest] Started (client={})", client != null ? client.hashCode() : "null");
	}

	@Override
	protected void shutDown()
	{
		stopServer();
		log.info("[RuneMirrorGuest] Stopped");
	}

	private int resolveListenPort()
	{
		String override = System.getProperty("runemirror.guest.port");
		if (override == null || override.trim().isEmpty())
		{
			override = System.getProperty("prush.guest.port");
		}
		if (override != null && !override.trim().isEmpty())
		{
			try
			{
				return Integer.parseInt(override.trim());
			}
			catch (NumberFormatException e)
			{
				// Fall back to config
			}
		}
		return config.listenPort();
	}

	private void startServer()
	{
		stopServer();
		server = new PrushGuestServer(resolveListenPort(), this::handleLine);
		server.start();
	}

	private void stopServer()
	{
		if (server != null)
		{
			server.stop();
			server = null;
		}
	}

	private void handleLine(String line)
	{
		PrushAction a;
		try
		{
			a = gson.fromJson(line, PrushAction.class);
		}
		catch (Exception e)
		{
			log.debug("[RuneMirrorGuest] Bad json: {}", e.getMessage());
			return;
		}

		if (a == null || a.getType() == null)
		{
			return;
		}
		if (a.getV() != PROTOCOL_VERSION)
		{
			return;
		}
		if (!config.enabled())
		{
			return;
		}

		int nowTick = client.getTickCount();
		int lag = nowTick - a.getTick();
		if (lag > config.maxTickLag())
		{
			return;
		}

		if (a.getType() == PrushActionType.MENU_ACTION)
		{
			scheduleMenuAction(a);
		}
		else if (a.getType() == PrushActionType.WALK_WORLD)
		{
			scheduleWalkWorld(a);
		}
		else if (a.getType() == PrushActionType.DIALOG_CONTINUE)
		{
			scheduleDialogContinue();
		}
	}

	private void scheduleMenuAction(PrushAction a)
	{
		clientThread.invoke(() -> {
			try
			{
				MenuAction ma = MenuAction.of(a.getOpcode());
				if (ma == MenuAction.UNKNOWN)
				{
					return;
				}

				client.menuAction(
					a.getParam0(),
					a.getParam1(),
					ma,
					a.getIdentifier(),
					a.getItemId(),
					a.getOption() == null ? "" : a.getOption(),
					a.getTarget() == null ? "" : a.getTarget()
				);
			}
			catch (Exception e)
			{
				log.debug("[RuneMirrorGuest] Exec failed: {}", e.getMessage());
			}
		});
	}

	private void scheduleWalkWorld(PrushAction a)
	{
		Integer wx = a.getWorldX();
		Integer wy = a.getWorldY();
		Integer wp = a.getWorldPlane();
		if (wx == null || wy == null || wp == null)
		{
			return;
		}

		clientThread.invoke(() -> {
			try
			{
				WorldPoint dest = new WorldPoint(wx, wy, wp);
				int sceneX = dest.getX() - client.getTopLevelWorldView().getBaseX();
				int sceneY = dest.getY() - client.getTopLevelWorldView().getBaseY();
				if (sceneX < 0 || sceneY < 0 || sceneX >= client.getTopLevelWorldView().getSizeX() || sceneY >= client.getTopLevelWorldView().getSizeY())
				{
					return;
				}

				client.menuAction(
					sceneX,
					sceneY,
					MenuAction.WALK,
					0,
					-1,
					"Walk here",
					""
				);
			}
			catch (Exception e)
			{
				log.debug("[RuneMirrorGuest] Walk exec failed: {}", e.getMessage());
			}
		});
	}

	private void scheduleDialogContinue()
	{
		clientThread.invoke(() -> {
			try
			{
				MenuEntry[] entries = client.getMenuEntries();
				if (entries == null || entries.length == 0)
				{
					return;
				}

				MenuEntry continueEntry = null;
				for (int i = entries.length - 1; i >= 0; i--)
				{
					MenuEntry e = entries[i];
					if (e == null)
					{
						continue;
					}
					// Tutorial Island and other dialogues use WIDGET_CONTINUE with option text like
					// "Click here to continue" (not always exactly "Continue").
					if (e.getType() == MenuAction.WIDGET_CONTINUE)
					{
						continueEntry = e;
						break;
					}
				}

				if (continueEntry == null)
				{
					return;
				}

				client.menuAction(
					continueEntry.getParam0(),
					continueEntry.getParam1(),
					continueEntry.getType(),
					continueEntry.getIdentifier(),
					continueEntry.getItemId(),
					continueEntry.getOption() == null ? "" : continueEntry.getOption(),
					continueEntry.getTarget() == null ? "" : continueEntry.getTarget()
				);
			}
			catch (Exception e)
			{
				log.debug("[RuneMirrorGuest] Continue exec failed: {}", e.getMessage());
			}
		});
	}

	@Provides
	PrushGuestConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrushGuestConfig.class);
	}
}
