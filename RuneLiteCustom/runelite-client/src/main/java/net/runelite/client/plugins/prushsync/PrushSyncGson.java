package net.runelite.client.plugins.prushsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class PrushSyncGson
{
	private PrushSyncGson()
	{
	}

	public static Gson create()
	{
		return new GsonBuilder().create();
	}
}
