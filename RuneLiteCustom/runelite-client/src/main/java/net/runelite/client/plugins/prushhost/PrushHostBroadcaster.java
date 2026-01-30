package net.runelite.client.plugins.prushhost;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.prushsync.PrushAction;

@Slf4j
public class PrushHostBroadcaster
{
	private final List<GuestConn> conns = new ArrayList<>();
	private final int connectTimeoutMs;

	public PrushHostBroadcaster(int connectTimeoutMs)
	{
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public void setTargets(List<InetSocketAddress> targets)
	{
		close();
		for (InetSocketAddress addr : targets)
		{
			try
			{
				Socket s = new Socket();
				s.connect(addr, connectTimeoutMs);
				s.setTcpNoDelay(true);
				BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
				conns.add(new GuestConn(addr, s, w));
				log.info("[RuneMirrorHost] Connected guest {}", addr);
			}
			catch (IOException e)
			{
				log.warn("[RuneMirrorHost] Failed to connect guest {}: {}", addr, e.getMessage());
			}
		}
	}

	public void broadcastJson(String json)
	{
		for (int i = conns.size() - 1; i >= 0; i--)
		{
			GuestConn c = conns.get(i);
			try
			{
				c.writer.write(json);
				c.writer.write('\n');
				c.writer.flush();
			}
			catch (IOException e)
			{
				log.warn("[RuneMirrorHost] Dropping guest {}: {}", c.addr, e.getMessage());
				c.close();
				conns.remove(i);
			}
		}
	}

	public void broadcast(PrushAction action, String json)
	{
		broadcastJson(json);
	}

	public void close()
	{
		for (GuestConn c : conns)
		{
			c.close();
		}
		conns.clear();
	}

	private static class GuestConn
	{
		private final InetSocketAddress addr;
		private final Socket socket;
		private final BufferedWriter writer;

		private GuestConn(InetSocketAddress addr, Socket socket, BufferedWriter writer)
		{
			this.addr = addr;
			this.socket = socket;
			this.writer = writer;
		}

		private void close()
		{
			try
			{
				socket.close();
			}
			catch (IOException ignored)
			{
			}
		}
	}
}
