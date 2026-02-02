Windows PowerShell
Copyright (C) Microsoft Corporation. All rights reserved.

Install the latest PowerShell for new features and improvements! https://aka.ms/PSWindows

PS C:\Users\James\Desktop\RuneMirror\tools\launch> .\LaunchRuneMirror_Guest.bat 1
Launching RuneMirror Guest...
Using Java: C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe
Jar: C:\Users\James\Desktop\RuneMirror\RuneLiteCustom\runelite-client\target\client-1.12.7-SNAPSHOT-shaded.jar
Guest index: 1
Guest home: C:\Users\James\Desktop\RuneMirrorGuestHome1
Guest port: 46001

Picked up _JAVA_OPTIONS: -Dhttps.protocols=TLSv1.2
2026-02-02 11:10:08 EST [main] INFO  net.runelite.client.RuneLite - RuneLite 1.12.7-SNAPSHOT (launcher version unknown) starting up, args: none
2026-02-02 11:10:08 EST [main] INFO  net.runelite.client.RuneLite - Java VM arguments: -Duser.home=C:\Users\James\Desktop\RuneMirrorGuestHome1 -Drunemirror.guest.port=46001 -Xmx768m -Xss2m -XX:CompileThreshold=1500 -Dhttps.protocols=TLSv1.2
2026-02-02 11:10:10 EST [Preloader] INFO  net.runelite.client.rs.ClientLoader - injected-client 19898596820.337
2026-02-02 11:10:10 EST [main] INFO  n.r.client.account.SessionManager - No session file exists
2026-02-02 11:10:10 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'default' (sync: false, active: false, id: 12842902090800)
2026-02-02 11:10:10 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'Imported Profile' (sync: false, active: true, id: 12913209869100)
2026-02-02 11:10:10 EST [main] INFO  n.r.client.config.ConfigManager - Using profile: Imported Profile (12913209869100)
2026-02-02 11:10:11 EST [pool-2-thread-1] WARN  net.runelite.client.game.ItemClient - Error looking up prices: Response{protocol=h2, code=404, message=, url=https://api.runelite.net/runelite-1.12.7-SNAPSHOT/item/prices.js}
2026-02-02 11:10:11 EST [pool-2-thread-1] WARN  n.runelite.client.game.WorldService - Error looking up worlds
java.io.IOException: unsuccessful response looking up worlds
        at net.runelite.client.game.WorldClient.lookupWorlds(WorldClient.java:66)
        at net.runelite.client.game.WorldService.fetch(WorldService.java:97)
        at net.runelite.client.game.WorldService.tick(WorldService.java:82)
        at net.runelite.client.util.RunnableExceptionLogger.run(RunnableExceptionLogger.java:41)
        at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:572)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
        at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
        at java.base/java.lang.Thread.run(Thread.java:1583)
2026-02-02 11:10:12 EST [main] INFO  n.r.c.plugins.kourendlibrary.Library - Library is now reset
2026-02-02 11:10:13 EST [main] INFO  n.r.client.discord.DiscordService - Initializing Discord RPC service.
2026-02-02 11:10:13 EST [AWT-EventQueue-0] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Started (client=764116189)
2026-02-02 11:10:13 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestServer - [RuneMirrorGuest] Listening on 46001
2026-02-02 11:10:14 EST [main] INFO  net.runelite.client.RuneLite - Client initialization took 5604ms. Uptime: 6291ms
2026-02-02 11:10:15 EST [pool-2-thread-1] INFO  n.r.client.discord.DiscordService - Discord RPC service is ready with user prush_.
2026-02-02 11:10:21 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestServer - [RuneMirrorGuest] Host connected from /127.0.0.1:53669
2026-02-02 11:10:29 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 11:10:29 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 11:10:29 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/2611: 973BEC2DE26EF911FD097FB168CB4B58B5F6FDF739AA11700CEA7B1A74040B0A != 7E3E372CFE585F4F2FF4ABF6720B2E25623A3E4F6233F13307BC07AD10E7DB95
2026-02-02 11:10:30 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 11:10:41 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=MENU_ACTION tick=8 seq=1
2026-02-02 11:10:41 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Replaying MENU_ACTION opcode=9 p0=0 p1=0 id=2226 itemId=-1 opt='Talk-to' tgt='<col=ffff00>Gielinor Guide'
2026-02-02 11:10:43 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=DIALOG_CONTINUE tick=10 seq=2
2026-02-02 11:10:43 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] scheduleDialogContinue invoked
2026-02-02 11:10:43 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Dispatching synthetic spacebar KeyEvent to canvas
2026-02-02 11:10:43 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Found CONTINUE widget id=15138821, sending WIDGET_CONTINUE
2026-02-02 11:10:44 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=WALK_WORLD tick=12 seq=3
2026-02-02 11:10:44 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD dest/worldX=3395 worldY=3271 relative dx=301 dy=164 from player world=(3094, 3107, 0) -> dest world=(3395, 3271, 0) scene=(355, 215)
2026-02-02 11:10:44 EST [Client] WARN  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD: computed scene coords (355, 215) are out of bounds (size: 104x104)
2026-02-02 11:10:46 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=WALK_WORLD tick=16 seq=4
2026-02-02 11:10:46 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD dest/worldX=3364 worldY=3265 relative dx=270 dy=158 from player world=(3094, 3107, 0) -> dest world=(3364, 3265, 0) scene=(324, 209)
2026-02-02 11:10:46 EST [Client] WARN  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD: computed scene coords (324, 209) are out of bounds (size: 104x104)
2026-02-02 11:10:49 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=WALK_WORLD tick=21 seq=5
2026-02-02 11:10:49 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD dest/worldX=3454 worldY=3316 relative dx=360 dy=209 from player world=(3094, 3107, 0) -> dest world=(3454, 3316, 0) scene=(414, 260)
2026-02-02 11:10:49 EST [Client] WARN  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD: computed scene coords (414, 260) are out of bounds (size: 104x104)
2026-02-02 11:10:55 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=WALK_WORLD tick=30 seq=6
2026-02-02 11:10:55 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD dest/worldX=3404 worldY=3312 relative dx=310 dy=202 from player world=(3094, 3110, 0) -> dest world=(3404, 3312, 0) scene=(364, 256)
2026-02-02 11:10:55 EST [Client] WARN  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] WALK_WORLD: computed scene coords (364, 256) are out of bounds (size: 104x104)