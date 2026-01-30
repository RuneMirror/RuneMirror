package net.runelite.client.plugins.highroller;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(HighRollerFlipperConfig.GROUP)
public interface HighRollerFlipperConfig extends Config
{
	String GROUP = "highroller";

	@ConfigItem(
		keyName = "signalStoreEnabled",
		name = "Enable signal layer",
		description = "Track recent flips and build private signals for the high-roller system"
	)
	default boolean signalStoreEnabled()
	{
		return true;
	}

	@Range(
		min = 15,
		max = 240
	)
	@ConfigItem(
		keyName = "windowMinutes",
		name = "Signal window (minutes)",
		description = "How many minutes of realized flips to keep in memory"
	)
	default int windowMinutes()
	{
		return 120;
	}

	@Range(
		min = 10,
		max = 120
	)
	@ConfigItem(
		keyName = "refreshSeconds",
		name = "Refresh cadence (seconds)",
		description = "How frequently to rebuild the signal snapshot"
	)
	default int refreshSeconds()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "debugLoggingEnabled",
		name = "Log snapshot summary",
		description = "Writes a short snapshot summary to the log each refresh"
	)
	default boolean debugLoggingEnabled()
	{
		return true;
	}
}

