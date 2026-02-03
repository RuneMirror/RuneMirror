package net.runelite.client.plugins.prushguest;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
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
	// How many client ticks to wait for menuAction to set a local destination
	private static final int MAX_MENU_ACTION_RETRIES = 6;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PrushGuestConfig config;

	private Gson gson;
	private PrushGuestServer server;
	/** The most recent WALK_WORLD action received from the host (for verification/override) */
	private volatile PrushAction lastWalkWorldAction;

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

				// Verify across multiple ticks to allow the client to set local destination
				verifyMenuActionResult(a, ma, MAX_MENU_ACTION_RETRIES);
			}
			catch (Exception e)
			{
				log.debug("[RuneMirrorGuest] Exec failed: {}", e.getMessage());
			}
		});
	}

	private void verifyMenuActionResult(PrushAction a, MenuAction ma, int remaining)
	{
		clientThread.invokeLater(() -> {
			try
			{
				net.runelite.api.coords.LocalPoint dest = client.getLocalDestinationLocation();
				if (dest != null)
				{
					log.info("[RuneMirrorGuest] MENU_ACTION resulted in local destination on retry: scene=({}, {})", dest.getSceneX(), dest.getSceneY());
					// If this MENU_ACTION was a WALK and the host also sent a WALK_WORLD for the same tick,
					// ensure the resulting scene matches the authoritative WALK_WORLD. If not, override it.
					if (ma == MenuAction.WALK && lastWalkWorldAction != null && lastWalkWorldAction.getTick() == a.getTick())
					{
						try
						{
							PrushAction w = lastWalkWorldAction;
							WorldPoint intended = null;
							Integer hostBaseX = w.getHostBaseX();
							Integer hostBaseY = w.getHostBaseY();
							Integer hostPlayerWx = w.getHostPlayerWorldX();
							Integer hostPlayerWy = w.getHostPlayerWorldY();
							Integer hostPlayerWpl = w.getHostPlayerWorldPlane();
							int sceneX = w.getParam0();
							int sceneY = w.getParam1();
							if (hostBaseX != null && hostBaseY != null && hostPlayerWx != null && hostPlayerWy != null)
							{
								WorldPoint hostClicked = new WorldPoint(hostBaseX + sceneX, hostBaseY + sceneY, hostPlayerWpl == null ? client.getPlane() : hostPlayerWpl);
								int relx = hostClicked.getX() - hostPlayerWx;
								int rely = hostClicked.getY() - hostPlayerWy;
								WorldPoint playerWp = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
								if (playerWp != null)
								{
									intended = new WorldPoint(playerWp.getX() + relx, playerWp.getY() + rely, playerWp.getPlane());
								}
							}
							else
							{
								net.runelite.api.coords.LocalPoint playerLocal = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getLocalLocation();
								int playerSceneX = playerLocal == null ? 0 : playerLocal.getSceneX();
								int playerSceneY = playerLocal == null ? 0 : playerLocal.getSceneY();
								int dx = sceneX - playerSceneX;
								int dy = sceneY - playerSceneY;
								WorldPoint playerWp = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
								if (playerWp != null)
								{
									intended = new WorldPoint(playerWp.getX() + dx, playerWp.getY() + dy, playerWp.getPlane());
								}
							}

							if (intended != null)
							{
								net.runelite.api.WorldView wv = client.findWorldViewFromWorldPoint(intended);
								if (wv != null)
								{
									net.runelite.api.coords.LocalPoint lpInt = net.runelite.api.coords.LocalPoint.fromWorld(wv, intended);
									if (lpInt != null)
									{
										if (lpInt.getSceneX() != dest.getSceneX() || lpInt.getSceneY() != dest.getSceneY())
										{
											log.info("[RuneMirrorGuest] MENU_ACTION resulted scene=({},{}), but WALK_WORLD intends scene=({},{}). Overriding to authoritative WALK_WORLD.", dest.getSceneX(), dest.getSceneY(), lpInt.getSceneX(), lpInt.getSceneY());
											client.menuAction(lpInt.getSceneX(), lpInt.getSceneY(), MenuAction.WALK, 0, -1, "Walk here", "");
											lastWalkWorldAction = null;
											verifyMenuActionResult(a, ma, Math.min(MAX_MENU_ACTION_RETRIES, 3));
											return;
										}
									}
								}
							}
						}
						catch (Exception e)
						{
							log.debug("[RuneMirrorGuest] override check failed: {}", e.getMessage(), e);
						}
					}
					return;
				}
				if (remaining > 1)
				{
					verifyMenuActionResult(a, ma, remaining - 1);
					return;
				}
				// Final attempt: try synthetic canvas click fallback for WALK actions (or scene-based clicks)
				try
				{
					if (a.getOpcode() == MenuAction.WALK.getId())
					{
						// Attempt to reconstruct destination world from host-provided context
						Integer hostBaseX = a.getHostBaseX();
						Integer hostBaseY = a.getHostBaseY();
						Integer hostPlayerWx = a.getHostPlayerWorldX();
						Integer hostPlayerWy = a.getHostPlayerWorldY();
						Integer hostPlayerWpl = a.getHostPlayerWorldPlane();
						int sceneX = a.getParam0();
						int sceneY = a.getParam1();
						WorldPoint destWp = null;
						if (hostBaseX != null && hostBaseY != null && hostPlayerWx != null && hostPlayerWy != null)
						{
							WorldPoint hostClicked = new WorldPoint(hostBaseX + sceneX, hostBaseY + sceneY, hostPlayerWpl == null ? client.getPlane() : hostPlayerWpl);
							int relx = hostClicked.getX() - hostPlayerWx;
							int rely = hostClicked.getY() - hostPlayerWy;
							WorldPoint playerWp = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
							if (playerWp != null)
							{
								destWp = new WorldPoint(playerWp.getX() + relx, playerWp.getY() + rely, playerWp.getPlane());
							}
						}
						else
						{
							// Fallback: apply scene delta from host to this guest's player scene
							net.runelite.api.coords.LocalPoint playerLocal = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getLocalLocation();
							int playerSceneX = playerLocal == null ? 0 : playerLocal.getSceneX();
							int playerSceneY = playerLocal == null ? 0 : playerLocal.getSceneY();
							int dx = sceneX - playerSceneX;
							int dy = sceneY - playerSceneY;
							WorldPoint playerWp = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
							if (playerWp != null)
							{
								destWp = new WorldPoint(playerWp.getX() + dx, playerWp.getY() + dy, playerWp.getPlane());
							}
						}
						if (destWp != null)
						{
							net.runelite.api.WorldView wv = client.findWorldViewFromWorldPoint(destWp);
							if (wv != null)
							{
								net.runelite.api.coords.LocalPoint lp = net.runelite.api.coords.LocalPoint.fromWorld(wv, destWp);
								if (lp != null)
								{
									Point p = Perspective.localToCanvas(client, lp, destWp.getPlane());
									if (p != null)
									{
										Canvas canvas = client.getCanvas();
										long now = System.currentTimeMillis();
										canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, now, 0, p.getX(), p.getY(), 1, false));
										canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, now, 0, p.getX(), p.getY(), 1, false));
										log.info("[RuneMirrorGuest] Dispatched synthetic canvas click at {} to replicate host WALK", p);
										return;
									}
								}
							}
						}
						log.warn("[RuneMirrorGuest] Canvas-click fallback could not resolve destination; giving up");
					}
				}
				catch (Exception e)
				{
					log.debug("[RuneMirrorGuest] canvas-click fallback failed: {}", e.getMessage(), e);
				}
			}
			catch (Exception e)
			{
				log.debug("[RuneMirrorGuest] verifyMenuActionResult failed: {}", e.getMessage(), e);
			}
		});
	}

	private void scheduleWalkWorld(PrushAction a)
	{
		// Remember the last WALK_WORLD from host (used to verify/override replayed MENU_ACTIONs)
		lastWalkWorldAction = a;
		Integer wx = a.getWorldX();
		Integer wy = a.getWorldY();
		Integer wp = a.getWorldPlane();
		Integer rdx = a.getRelDx();
		Integer rdy = a.getRelDy();
		if ((wx == null || wy == null || wp == null) && (rdx == null || rdy == null))
		{
			log.warn("[RuneMirrorGuest] WALK_WORLD missing required fields: wx={} wy={} plane={} relDx={} relDy={}", wx, wy, wp, rdx, rdy);
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
				// Compute destination using relative offsets if provided, otherwise use absolute world coords.
				WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
				if (playerWp == null)
				{
					log.warn("[RuneMirrorGuest] WALK_WORLD: could not get player world location");
					return;
				}
				WorldPoint dest;
				// Prefer host-base reconstruction: if host provided its worldview base and host player world
				Integer hostBaseX = a.getHostBaseX();
				Integer hostBaseY = a.getHostBaseY();
				Integer hostPlayerWx = a.getHostPlayerWorldX();
				Integer hostPlayerWy = a.getHostPlayerWorldY();
				Integer hostPlayerWpl = a.getHostPlayerWorldPlane();
				if (hostBaseX != null && hostBaseY != null && hostPlayerWx != null && hostPlayerWy != null && a.getParam0() != 0 && a.getParam1() != 0)
				{
					// Reconstruct the world point the host clicked using host base + scene coords
					int sceneX = a.getParam0();
					int sceneY = a.getParam1();
					WorldPoint hostClicked = new WorldPoint(hostBaseX + sceneX, hostBaseY + sceneY, hostPlayerWpl == null ? playerWp.getPlane() : hostPlayerWpl);
					int relx = hostClicked.getX() - hostPlayerWx;
					int rely = hostClicked.getY() - hostPlayerWy;
					// Apply the same vector to this guest's player world position
					dest = new WorldPoint(playerWp.getX() + relx, playerWp.getY() + rely, playerWp.getPlane());
					int absDx = Math.abs(relx);
					int absDy = Math.abs(rely);
					if (absDx > 8 || absDy > 8)
					{
						log.warn("[RuneMirrorGuest] Computed rel vector too large relx={} rely={} — falling back to absolute world coords if available", relx, rely);
						if (wx != null && wy != null && wp != null)
						{
							dest = new WorldPoint(wx, wy, wp);
							log.info("[RuneMirrorGuest] Using absolute fallback dest world={}", dest);
						}
						else
						{
							log.warn("[RuneMirrorGuest] No absolute world fallback present; aborting walk_world");
							return;
						}
					}
					else
					{
						log.info("[RuneMirrorGuest] Reconstructed hostClicked={} rel=({}, {}) -> guest dest={}", hostClicked, relx, rely, dest);
					}
				}
				else if (rdx != null && rdy != null)
				{
					// Use relative offset from host player: apply to this guest's player world position.
					dest = new WorldPoint(playerWp.getX() + rdx, playerWp.getY() + rdy, playerWp.getPlane());
					// Safety: reject obviously huge offsets (likely bad data) to avoid long teleports.
					int absDx = Math.abs(rdx);
					int absDy = Math.abs(rdy);
					if (absDx > 8 || absDy > 8)
					{
						log.warn("[RuneMirrorGuest] WALK_WORLD rel offset too large relDx={} relDy={} — falling back to absolute world coords if available", rdx, rdy);
						if (wx != null && wy != null && wp != null)
						{
							dest = new WorldPoint(wx, wy, wp);
							log.info("[RuneMirrorGuest] Using absolute fallback dest world={}", dest);
						}
						else
						{
							log.warn("[RuneMirrorGuest] No absolute world fallback present; aborting walk_world");
							return;
						}
					}
					else
					{
						log.info("[RuneMirrorGuest] WALK_WORLD using relative offset relDx={} relDy={} -> guest dest world={} (player world={},{},{})", rdx, rdy, dest, playerWp.getX(), playerWp.getY(), playerWp.getPlane());
					}
				}
				else
				{
					dest = new WorldPoint(wx, wy, wp);
				}

				// Resolve the WorldView that contains the destination, and convert using that WorldView.
				net.runelite.api.WorldView wv = client.findWorldViewFromWorldPoint(dest);
				if (wv == null)
				{
					log.warn("[RuneMirrorGuest] WALK_WORLD: no WorldView found for destination {}", dest);
					return;
				}

				net.runelite.api.coords.LocalPoint lp = net.runelite.api.coords.LocalPoint.fromWorld(wv, dest);
				if (lp == null)
				{
					log.warn("[RuneMirrorGuest] WALK_WORLD: destination {} is not in resolved WorldView (id={}) baseX={} baseY={} size={}x{}",
						dest, wv.getId(), wv.getBaseX(), wv.getBaseY(), wv.getSizeX(), wv.getSizeY());
					return;
				}

				int sceneX = lp.getSceneX();
				int sceneY = lp.getSceneY();

				int dx = dest.getX() - playerWp.getX();
				int dy = dest.getY() - playerWp.getY();

				log.info("[RuneMirrorGuest] WALK_WORLD dest/worldX={} worldY={} relative dx={} dy={} from player world=({}, {}, {}) -> dest world=({}, {}, {}) scene=({}, {})",
					wx, wy, dx, dy, playerWp.getX(), playerWp.getY(), playerWp.getPlane(), dest.getX(), dest.getY(), dest.getPlane(), sceneX, sceneY);
				
				if (sceneX < 0 || sceneY < 0 || sceneX >= wv.getSizeX() || sceneY >= wv.getSizeY())
				{
					log.warn("[RuneMirrorGuest] WALK_WORLD: computed scene coords ({}, {}) are out of bounds for WorldView id={} (size: {}x{})",
						sceneX, sceneY, wv.getId(), wv.getSizeX(), wv.getSizeY());
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
