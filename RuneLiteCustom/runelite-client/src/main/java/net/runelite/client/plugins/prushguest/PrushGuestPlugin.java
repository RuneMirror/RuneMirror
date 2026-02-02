package net.runelite.client.plugins.prushguest;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
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
			log.debug("[RuneMirrorGuest] Received action while guest config disabled: {}", a.getType());
			return;
		}

		log.info("[RuneMirrorGuest] Received action: type={} tick={} seq={}", a.getType(), a.getTick(), a.getSeq());

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
				log.info("[RuneMirrorGuest] Replaying MENU_ACTION opcode={} p0={} p1={} id={} itemId={} opt='{}' tgt='{}'",
					a.getOpcode(), a.getParam0(), a.getParam1(), a.getIdentifier(), a.getItemId(),
					a.getOption(), a.getTarget());
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
			log.warn("[RuneMirrorGuest] WALK_WORLD missing required fields: wx={} wy={} plane={}", wx, wy, wp);
			return;
		}

		clientThread.invoke(() -> {
			try
			{
				if (client.getLocalPlayer() == null)
				{
					log.warn("[RuneMirrorGuest] WALK_WORLD: localPlayer is null");
					return;
				}
				// Compute destination as the absolute world coordinates sent by the host.
				WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
				if (playerWp == null)
				{
					log.warn("[RuneMirrorGuest] WALK_WORLD: could not get player world location");
					return;
				}

				WorldPoint dest = new WorldPoint(wx, wy, wp);
				int baseX = client.getTopLevelWorldView().getBaseX();
				int baseY = client.getTopLevelWorldView().getBaseY();
				int sceneX = dest.getX() - baseX;
				int sceneY = dest.getY() - baseY;
				int dx = dest.getX() - playerWp.getX();
				int dy = dest.getY() - playerWp.getY();

				log.info("[RuneMirrorGuest] WALK_WORLD dest/worldX={} worldY={} relative dx={} dy={} from player world=({}, {}, {}) -> dest world=({}, {}, {}) scene=({}, {})",
					wx, wy, dx, dy, playerWp.getX(), playerWp.getY(), playerWp.getPlane(), dest.getX(), dest.getY(), dest.getPlane(), sceneX, sceneY);
				
				if (sceneX < 0 || sceneY < 0 || sceneX >= client.getTopLevelWorldView().getSizeX() || sceneY >= client.getTopLevelWorldView().getSizeY())
				{
					log.warn("[RuneMirrorGuest] WALK_WORLD: computed scene coords ({}, {}) are out of bounds (size: {}x{})",
						sceneX, sceneY, client.getTopLevelWorldView().getSizeX(), client.getTopLevelWorldView().getSizeY());
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
				log.warn("[RuneMirrorGuest] Walk exec failed: {}", e.getMessage(), e);
			}
		});
	}

	private void scheduleDialogContinue()
	{
		clientThread.invoke(() -> {
			try
			{
				log.info("[RuneMirrorGuest] scheduleDialogContinue invoked");

				// First, try to simulate the actual spacebar keypress on the game canvas.
				Canvas canvas = client.getCanvas();
				if (canvas != null)
				{
					long now = System.currentTimeMillis();
					log.info("[RuneMirrorGuest] Dispatching synthetic spacebar KeyEvent to canvas");
					canvas.dispatchEvent(new KeyEvent(canvas, KeyEvent.KEY_PRESSED, now, 0, KeyEvent.VK_SPACE, ' '));
					canvas.dispatchEvent(new KeyEvent(canvas, KeyEvent.KEY_RELEASED, now, 0, KeyEvent.VK_SPACE, ' '));
				}

				// Also try clicking the actual chat continue widget as a backup.
				// This does not depend on mouse position and mirrors "spacebar" behaviour reliably.
				Widget w = client.getWidget(InterfaceID.ChatBoth.CONTINUE);
				if (w == null)
				{
					w = client.getWidget(InterfaceID.ChatRight.CONTINUE);
				}
				if (w == null)
				{
					w = client.getWidget(InterfaceID.ChatLeft.CONTINUE);
				}

				if (w != null)
				{
					int componentId = w.getId();
					log.info("[RuneMirrorGuest] Found CONTINUE widget id={}, sending WIDGET_CONTINUE", componentId);
					client.menuAction(0, 0, MenuAction.WIDGET_CONTINUE, componentId, -1, "", "");
					return;
				}

				// Fallback: attempt to find a widget-continue menu entry.
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
					log.info("[RuneMirrorGuest] No WIDGET_CONTINUE menu entry found; nothing to do");
					return;
				}

				log.info("[RuneMirrorGuest] Using fallback WIDGET_CONTINUE menu entry option='{}' target='{}'",
					continueEntry.getOption(), continueEntry.getTarget());

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
