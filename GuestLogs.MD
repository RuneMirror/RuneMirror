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
2026-02-02 09:16:21 EST [main] INFO  net.runelite.client.RuneLite - RuneLite 1.12.7-SNAPSHOT (launcher version unknown) starting up, args: none
2026-02-02 09:16:21 EST [main] INFO  net.runelite.client.RuneLite - Java VM arguments: -Duser.home=C:\Users\James\Desktop\RuneMirrorGuestHome1 -Drunemirror.guest.port=46001 -Xmx768m -Xss2m -XX:CompileThreshold=1500 -Dhttps.protocols=TLSv1.2
2026-02-02 09:16:23 EST [Preloader] INFO  net.runelite.client.rs.ClientLoader - injected-client 19898596820.337
2026-02-02 09:16:23 EST [main] INFO  n.r.client.account.SessionManager - No session file exists
2026-02-02 09:16:23 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'default' (sync: false, active: false, id: 12842902090800)
2026-02-02 09:16:23 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'Imported Profile' (sync: false, active: true, id: 12913209869100)
2026-02-02 09:16:23 EST [main] INFO  n.r.client.config.ConfigManager - Using profile: Imported Profile (12913209869100)
2026-02-02 09:16:24 EST [pool-2-thread-1] WARN  net.runelite.client.game.ItemClient - Error looking up prices: Response{protocol=h2, code=404, message=, url=https://api.runelite.net/runelite-1.12.7-SNAPSHOT/item/prices.js}
2026-02-02 09:16:25 EST [pool-2-thread-1] WARN  n.runelite.client.game.WorldService - Error looking up worlds
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
2026-02-02 09:16:25 EST [main] INFO  n.r.c.plugins.kourendlibrary.Library - Library is now reset
2026-02-02 09:16:26 EST [main] INFO  n.r.client.discord.DiscordService - Initializing Discord RPC service.
2026-02-02 09:16:28 EST [AWT-EventQueue-0] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Started (client=967316073)
2026-02-02 09:16:28 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestServer - [RuneMirrorGuest] Listening on 46001
2026-02-02 09:16:28 EST [main] INFO  net.runelite.client.RuneLite - Client initialization took 7250ms. Uptime: 8607ms
2026-02-02 09:16:28 EST [pool-2-thread-1] INFO  n.r.client.discord.DiscordService - Discord RPC service is ready with user prush_.
2026-02-02 09:16:29 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestServer - [RuneMirrorGuest] Host connected from /127.0.0.1:57684
2026-02-02 09:16:44 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 09:16:44 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 09:16:45 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/2611: 973BEC2DE26EF911FD097FB168CB4B58B5F6FDF739AA11700CEA7B1A74040B0A != 7E3E372CFE585F4F2FF4ABF6720B2E25623A3E4F6233F13307BC07AD10E7DB95
2026-02-02 09:16:45 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 09:16:58 EST [pool-2-thread-1] WARN  n.r.client.ClientSessionManager - Unable to ping session service
java.io.IOException: Unsuccessful ping
        at net.runelite.client.SessionClient.ping(SessionClient.java:66)
        at net.runelite.client.ClientSessionManager.ping(ClientSessionManager.java:85)
        at net.runelite.client.util.RunnableExceptionLogger.run(RunnableExceptionLogger.java:41)
        at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:572)
        at java.base/java.util.concurrent.FutureTask.runAndReset(FutureTask.java:358)
        at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:305)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
        at java.base/java.lang.Thread.run(Thread.java:1583)
2026-02-02 09:17:04 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=MENU_ACTION tick=17 seq=1
2026-02-02 09:17:04 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Replaying MENU_ACTION opcode=9 p0=0 p1=0 id=2226 itemId=-1 opt='Talk-to' tgt='<col=ffff00>Gielinor Guide'
2026-02-02 09:17:07 EST [RuneMirrorGuestServer] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Received action: type=DIALOG_CONTINUE tick=22 seq=2
2026-02-02 09:17:07 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] scheduleDialogContinue invoked
2026-02-02 09:17:07 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Dispatching synthetic spacebar KeyEvent to canvas
2026-02-02 09:17:07 EST [Client] INFO  n.r.c.p.prushguest.PrushGuestPlugin - [RuneMirrorGuest] Found CONTINUE widget id=15138821, sending WIDGET_CONTINUE