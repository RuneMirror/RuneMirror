package net.runelite.client.plugins.prushguest;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("runemirrorguest")
public interface PrushGuestConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Enabled",
		description = "Enable Guest execution"
	)
	default boolean enabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "listenPort",
		name = "Listen port",
		description = "Port for Host to connect to"
	)
	default int listenPort()
	{
		return 46001;
	}

	@ConfigItem(
		keyName = "maxTickLag",
		name = "Max tick lag",
		description = "Drop actions if already older than this many ticks"
	)
	default int maxTickLag()
	{
		return 1;
	}
}
