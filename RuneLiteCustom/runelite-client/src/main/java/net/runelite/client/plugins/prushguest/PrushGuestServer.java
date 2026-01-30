package net.runelite.client.plugins.prushguest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrushGuestServer
{
	public interface LineHandler
	{
		void onLine(String line);
	}

	private final int port;
	private final LineHandler handler;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread thread;
	private ServerSocket serverSocket;

	public PrushGuestServer(int port, LineHandler handler)
	{
		this.port = port;
		this.handler = handler;
	}

	public void start()
	{
		if (!running.compareAndSet(false, true))
		{
			return;
		}

		thread = new Thread(this::runLoop, "RuneMirrorGuestServer");
		thread.setDaemon(true);
		thread.start();
	}

	private void runLoop()
	{
		try (ServerSocket ss = new ServerSocket(port))
		{
			serverSocket = ss;
			log.info("[RuneMirrorGuest] Listening on {}", port);
			while (running.get())
			{
				Socket s = ss.accept();
				s.setTcpNoDelay(true);
				log.info("[RuneMirrorGuest] Host connected from {}", s.getRemoteSocketAddress());

				try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)))
				{
					String line;
					while (running.get() && (line = r.readLine()) != null)
					{
						if (line.isEmpty())
						{
							continue;
						}
						handler.onLine(line);
					}
				}
				catch (IOException e)
				{
					log.warn("[RuneMirrorGuest] Connection ended: {}", e.getMessage());
				}
				finally
				{
					try
					{
						s.close();
					}
					catch (IOException ignored)
					{
					}
				}
			}
		}
		catch (IOException e)
		{
			log.error("[RuneMirrorGuest] Server error on port {}: {}", port, e.getMessage());
		}
		finally
		{
			running.set(false);
		}
	}

	public void stop()
	{
		if (!running.compareAndSet(true, false))
		{
			return;
		}

		try
		{
			if (serverSocket != null)
			{
				serverSocket.close();
			}
		}
		catch (IOException ignored)
		{
		}
	}
}
