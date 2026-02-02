Launching RuneMirror Host...
Using Java: C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe
Jar: C:\Users\James\Desktop\RuneMirror\RuneLiteCustom\runelite-client\target\client-1.12.7-SNAPSHOT-shaded.jar

Picked up _JAVA_OPTIONS: -Dhttps.protocols=TLSv1.2
2026-02-02 11:10:12 EST [main] INFO  net.runelite.client.RuneLite - RuneLite 1.12.7-SNAPSHOT (launcher version unknown) starting up, args: none
2026-02-02 11:10:13 EST [main] INFO  net.runelite.client.RuneLite - Java VM arguments: -Duser.home=C:\Users\James\Desktop\RuneMirrorHostHome -Xmx768m -Xss2m -XX:CompileThreshold=1500 -Dhttps.protocols=TLSv1.2
2026-02-02 11:10:16 EST [Preloader] INFO  net.runelite.client.rs.ClientLoader - injected-client 19898596820.337
2026-02-02 11:10:16 EST [main] INFO  n.r.client.account.SessionManager - No session file exists
2026-02-02 11:10:16 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'default' (sync: false, active: false, id: 12972238793900)
2026-02-02 11:10:16 EST [main] INFO  n.r.client.config.ConfigManager - Profile 'Imported Profile' (sync: false, active: true, id: 13002370698400)
2026-02-02 11:10:16 EST [main] INFO  n.r.client.config.ConfigManager - Using profile: Imported Profile (13002370698400)
2026-02-02 11:10:17 EST [pool-2-thread-1] WARN  net.runelite.client.game.ItemClient - Error looking up prices: Response{protocol=h2, code=404, message=, url=https://api.runelite.net/runelite-1.12.7-SNAPSHOT/item/prices.js}
2026-02-02 11:10:17 EST [pool-2-thread-1] WARN  n.runelite.client.game.WorldService - Error looking up worlds
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
2026-02-02 11:10:18 EST [main] INFO  n.r.c.plugins.kourendlibrary.Library - Library is now reset
2026-02-02 11:10:19 EST [main] INFO  n.r.client.discord.DiscordService - Initializing Discord RPC service.
2026-02-02 11:10:21 EST [AWT-EventQueue-0] INFO  n.r.c.p.p.PrushHostBroadcaster - [RuneMirrorHost] Connected guest /127.0.0.1:46001
2026-02-02 11:10:21 EST [AWT-EventQueue-0] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Started (client=1025922961)
2026-02-02 11:10:21 EST [main] INFO  net.runelite.client.RuneLite - Client initialization took 7921ms. Uptime: 10180ms
2026-02-02 11:10:21 EST [pool-2-thread-1] INFO  n.r.client.discord.DiscordService - Discord RPC service is ready with user prush_.
2026-02-02 11:10:36 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 11:10:37 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 11:10:37 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/2611: 973BEC2DE26EF911FD097FB168CB4B58B5F6FDF739AA11700CEA7B1A74040B0A != 7E3E372CFE585F4F2FF4ABF6720B2E25623A3E4F6233F13307BC07AD10E7DB95
2026-02-02 11:10:38 EST [Client] WARN  injected-client - Mismatch in overlaid cache archive hash for 12/84: F0103CEB37BECA33FD56D8BE41B8E88CA4D8E44EC1C4F688563E6AF2E4F1E9CC != 28EC734FE4D8A8E219E8DBD6AD85925BDA0135B9926B81687D99883FF3C4E494
2026-02-02 11:10:41 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Mirroring MENU_ACTION type=NPC_FIRST_OPTION p0=0 p1=0 id=2226 itemId=-1 opt='Talk-to' tgt='<col=ffff00>Gielinor Guide'
2026-02-02 11:10:43 EST [AWT-EventQueue-0] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Sending DIALOG_CONTINUE action: PrushAction(v=1, seq=2, tick=10, type=DIALOG_CONTINUE, param0=0, param1=0, opcode=0, identifier=0, itemId=0, option=null, target=null, worldX=null, worldY=null, worldPlane=null)
2026-02-02 11:10:44 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] WALK: Using fallback scene coords (347, 215) -> world WorldPoint(x=3395, y=3271, plane=0) (baseX=3048, baseY=3056, size=104x104)
2026-02-02 11:10:44 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Mirroring WALK as relative step dx=299 dy=164 from player world=(3096, 3107, 0) to dest world=(3395, 3271, 0)
2026-02-02 11:10:46 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] WALK: Using fallback scene coords (316, 209) -> world WorldPoint(x=3364, y=3265, plane=0) (baseX=3048, baseY=3056, size=104x104)
2026-02-02 11:10:46 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Mirroring WALK as relative step dx=269 dy=157 from player world=(3095, 3108, 0) to dest world=(3364, 3265, 0)
2026-02-02 11:10:49 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] WALK: Using fallback scene coords (406, 260) -> world WorldPoint(x=3454, y=3316, plane=0) (baseX=3048, baseY=3056, size=104x104)
2026-02-02 11:10:49 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Mirroring WALK as relative step dx=361 dy=206 from player world=(3093, 3110, 0) to dest world=(3454, 3316, 0)
2026-02-02 11:10:55 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] WALK: Using fallback scene coords (356, 256) -> world WorldPoint(x=3404, y=3312, plane=0) (baseX=3048, baseY=3056, size=104x104)
2026-02-02 11:10:55 EST [Client] INFO  n.r.c.p.prushhost.PrushHostPlugin - [RuneMirrorHost] Mirroring WALK as relative step dx=310 dy=202 from player world=(3094, 3110, 0) to dest world=(3404, 3312, 0)