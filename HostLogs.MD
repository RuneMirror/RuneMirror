Launching RuneMirror Host...
Using Java: C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe
Jar: C:\Users\James\Desktop\RuneMirror\RuneLiteCustom\runelite-client\target\client-1.12.7-SNAPSHOT-shaded.jar

Picked up _JAVA_OPTIONS: -Dhttps.protocols=TLSv1.2
2026-02-02 17:32:01 EST [main] INFO  net.runelite.client.RuneLite - RuneLite 1.12.7-SNAPSHOT (launcher version unknown) starting up, args: none
2026-02-02 17:32:01 EST [main] INFO  net.runelite.client.RuneLite - Java VM arguments: -Duser.home=C:\Users\James\Desktop\RuneMirrorHostHome -Xmx768m -Xss2m -XX:CompileThreshold=1500 -Dhttps.protocols=TLSv1.2
2026-02-02 17:32:03 EST [Preloader] INFO  net.runelite.client.rs.ClientLoader - injected-client 19898596820.337
2026-02-02 17:32:04 EST [main] INFO  n.r.client.account.SessionManager - No session file exists
2026-02-02 17:32:04 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'default' (sync: false, active: false, id: 12972238793900)
2026-02-02 17:32:04 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'Imported Profile' (sync: false, active: true, id: 13002370698400)
2026-02-02 17:32:04 EST [main] INFO  n.r.client.config.ConfigManager - Using profile: Imported Profile (13002370698400)
2026-02-02 17:32:05 EST [pool-2-thread-1] WARN  net.runelite.client.game.ItemClient - Error looking up prices: Response{protocol=h2, code=404, message=, url=https://api.runelite.net/runelite-1.12.7-SNAPSHOT/item/prices.js}
2026-02-02 17:32:06 EST [pool-2-thread-1] WARN  n.runelite.client.game.WorldService - Error looking up worlds
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
2026-02-02 17:32:06 EST [main] INFO  n.r.c.plugins.kourendlibrary.Library - Library is now reset
2026-02-02 17:32:07 EST [main] INFO  n.r.client.discord.DiscordService - Initializing Discord RPC service.
2026-02-02 17:32:09 EST [AWT-EventQueue-0] INFO  n.r.c.p.p.PrushHostBroadcaster - [RuneMirrorHost] Connected guest /127.0.0.1:46001
2026-02-02 17:32:09 EST [AWT-EventQueue-0] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Started (client=999870264)
2026-02-02 17:32:09 EST [main] INFO  net.runelite.client.RuneLite - Client initialization took 7729ms. Uptime: 9066ms
2026-02-02 17:32:26 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 17:32:27 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 17:32:27 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/2611: 973BEC2DE26EF911FD097FB168CB4B58B5F6FDF739AA11700CEA7B1A74040B0A != 7E3E372CFE585F4F2FF4ABF6720B2E25623A3E4F6233F13307BC07AD10E7DB95
2026-02-02 17:32:27 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 17:32:38 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] WALK (delayed fallback): Using scene coords (358, 253) relative dx=304 dy=199 -> world WorldPoint(x=3398, y=3309, plane=0) (player world=3094,3110,0)
2026-02-02 17:32:38 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Mirroring WALK as relative step dx=304 dy=199 from player world=(3094, 3110, 0) to dest world=(3398, 3309, 0)
2026-02-02 17:32:40 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] WALK (delayed fallback): Using scene coords (386, 273) relative dx=333 dy=219 -> world WorldPoint(x=3426, y=3329, plane=0) (player world=3093,3110,0)
2026-02-02 17:32:40 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Mirroring WALK as relative step dx=333 dy=219 from player world=(3093, 3110, 0) to dest world=(3426, 3329, 0)
2026-02-02 17:32:54 EST [pool-2-thread-1] WARN  n.r.client.ClientSessionManager - Unable to ping session service
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