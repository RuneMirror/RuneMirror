package net.runelite.client.plugins.prushhost;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("runemirrorhost")
public interface PrushHostConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Enabled",
		description = "Enable Host mirroring"
	)
	default boolean enabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "guestTargets",
		name = "Guest targets",
		description = "Comma-separated list of guest endpoints in form host:port (example: 127.0.0.1:46001,127.0.0.1:46002)"
	)
	default String guestTargets()
	{
		return "127.0.0.1:46001";
	}

	@ConfigItem(
		keyName = "connectTimeoutMs",
		name = "Connect timeout (ms)",
		description = "TCP connect timeout"
	)
	default int connectTimeoutMs()
	{
		return 200;
	}
}
