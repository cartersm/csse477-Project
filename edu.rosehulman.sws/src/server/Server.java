/*
 * Server.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 */

package server;

import gui.WebServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import protocol.Protocol;
import protocol.plugin.AbstractPlugin;

/**
 * This represents a welcoming server for the incoming
 * TCP request from a HTTP client such as a web browser. 
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
	private static final String PLUGIN_ROOT = new File("plugins").getAbsolutePath();
	private String rootDirectory;
	private int port;
	private boolean stop;
	private ServerSocket welcomeSocket;

	private long connections;
	private long serviceTime;
	
	private Map<String, List<Socket>> activeConnections;
	private Map<String, List<Socket>> queuedConnections;
	
	private static final int MAX_PROCESSING_CONNECTIONS = 10;
	private int numProcessingConnections;
	
	private List<String> bannedInetAddresses;
	private static final int MAX_ACTIVE_CONNECTIONS_FOR_USER = 10;

	private WebServer window;
	private Map<String, AbstractPlugin> plugins;
	private final WatchService watcher;
	private Thread thread;

	/**
	 * @param rootDirectory
	 * @param port
	 * @throws IOException
	 */
	public Server(String rootDirectory, int port, WebServer window) throws IOException {
		this.rootDirectory = rootDirectory;
		this.port = port;
		this.stop = false;
		this.connections = 0;
		this.serviceTime = 0;
		this.window = window;
		this.plugins = new HashMap<>();
		
		this.activeConnections = new HashMap<String, List<Socket>>();
		this.queuedConnections = new HashMap<String, List<Socket>>();
		this.bannedInetAddresses = new ArrayList<String>();
		
		// Watch for directory changes
		watcher = FileSystems.getDefault().newWatchService();
		Path path = Paths.get(URI.create("file:///" + PLUGIN_ROOT.replace("\\", "//").replace(" ", "%20")));
		path.register(watcher, 
				StandardWatchEventKinds.ENTRY_CREATE, 
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path file : stream) {
				addPlugin(file.toFile().getName());
			}
		} catch (IOException | DirectoryIteratorException x) {
			System.err.println(x);
		}

		// Listen asynchronously for directory changes
		this.thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listenForNewPlugins();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}, "DirectoryWatcher");
		this.thread.start();
	}

	/**
	 * Gets the root directory for this web server.
	 * 
	 * @return the rootDirectory
	 */
	public String getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Gets the port number for this web server.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns connections serviced per second. 
	 * Synchronized to be used in threaded environment.
	 * 
	 * @return
	 */
	public synchronized double getServiceRate() {
		if (this.serviceTime == 0)
			return Long.MIN_VALUE;
		double rate = this.connections / (double) this.serviceTime;
		rate = rate * 1000;
		return rate;
	}

	/**
	 * Increments number of connection by the supplied value.
	 * Synchronized to be used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementConnections(long value) {
		this.connections += value;
	}
	
	public synchronized void incrementProcessingConnections(long value) {
		this.numProcessingConnections += value;
	}
	
	public synchronized void decrementProcessingConnections(long value) {
		this.numProcessingConnections -= value;
	}

	/**
	 * Increments the service time by the supplied value.
	 * Synchronized to be used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementServiceTime(long value) {
		this.serviceTime += value;
	}

	/**
	 * The entry method for the main server thread that accepts incoming
	 * TCP connection request and creates a {@link ConnectionHandler} for
	 * the request.
	 */
	public void run() {
		try {
			this.welcomeSocket = new ServerSocket(port);

			// Now keep welcoming new connections until stop flag is set to true
			while (true) {
				// Listen for incoming socket connection
				// This method block until somebody makes a request
				Socket connectionSocket = this.welcomeSocket.accept();

				// Come out of the loop if the stop flag is set
				if (this.stop)
					break;
				
				String inetAddress = connectionSocket.getInetAddress().toString();
				if (bannedInetAddresses.contains(inetAddress)) {
					continue;
				}

				int numConnections = 0;
				List<Socket> queuedSockets = null;
				if (queuedConnections.containsKey(inetAddress)) {
					queuedSockets = queuedConnections.get(inetAddress);
					numConnections += queuedSockets.size();
				} else {
					queuedSockets = new ArrayList<Socket>();
					queuedConnections.put(inetAddress, queuedSockets);
				}
				
				List<Socket> activeSockets = new ArrayList<Socket>();
				if (activeConnections.containsKey(inetAddress)) {
					activeSockets = activeConnections.get(inetAddress);
					numConnections += activeConnections.get(inetAddress).size();
				}
				
				if (numConnections + 1 > MAX_PROCESSING_CONNECTIONS) {
					this.bannedInetAddresses.add(inetAddress);
					queuedSockets.clear();
					activeSockets.clear();
					continue;
				}
				
				queuedSockets.add(connectionSocket);
			}
			this.welcomeSocket.close();
		} catch (Exception e) {
			window.showSocketException(e);
		}
	}
	
	private class PriorityQueueHandler implements Runnable {
		
		private Server server;
		
		public PriorityQueueHandler(Server server) {
			this.server = server;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			while (true) {
				for (String key : activeConnections.keySet()) {
					while (numProcessingConnections > MAX_PROCESSING_CONNECTIONS) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					server.incrementProcessingConnections(1);
					
					List<Socket> activeRequests = activeConnections.get(key);
					
					Socket connectionToHandle = activeRequests.get(0);
					// Create a handler for this connection and start the
					// handler in a new thread
					ConnectionHandler handler = new ConnectionHandler(server, connectionToHandle);
					new Thread(handler).start();
					
					activeRequests.remove(0);
					if (activeRequests.size() <= 0) {
						activeConnections.remove(key);
					}
				}
				
				for (String key : queuedConnections.keySet()) {
					List<Socket> requestsToAdd = queuedConnections.get(key);
					queuedConnections.remove(key);
					List<Socket> activeRequests = null;
					if (activeConnections.containsKey(key)) {
						activeRequests = activeConnections.get(key);
					} else {
						activeRequests = new ArrayList<Socket>();
						activeConnections.put(key, activeRequests);
					}
					activeRequests.addAll(requestsToAdd);
				}
			}
		}
		
	}

	/**
	 * Stops the server from listening further.
	 */
	public synchronized void stop() {
		if (this.stop)
			return;

		// Set the stop flag to be true
		this.stop = true;
		try {
			// This will force welcomeSocket to come out of the blocked accept() method 
			// in the main loop of the start() method
			Socket socket = new Socket(InetAddress.getLocalHost(), port);

			// We do not have any other job for this socket so just close it
			socket.close();
			this.thread.join();
		} catch (Exception e) {
		}
	}

	/**
	 * Checks if the server is stopeed or not.
	 * @return
	 */
	public boolean isStopped() {
		if (this.welcomeSocket != null)
			return this.welcomeSocket.isClosed();
		return true;
	}

	/* ----- Plugin handling and listening ----- */

	public AbstractPlugin getPlugin(String key) {
		return this.plugins.get(key);
	}

	private void listenForNewPlugins() throws IOException {
		for (;;) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();

				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}

				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				String filename = ev.context().toFile().getName();

				// Ignore if it's not a JAR
				if (!filename.endsWith(".jar")) {
					continue;
				}

				if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					addPlugin(filename);
				} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
					removePlugin(filename);
				}
			}

			if (!key.reset()) {
				throw new IOException("Plugin file is no longer accessible");
			}
		}
	}

	private void addPlugin(String filename) {
		JarClassLoader jarLoader = new JarClassLoader(PLUGIN_ROOT + "/" + filename);
		/* Load the class from the jar file and resolve it. */
		Class<?> c;
		try {
			String className = filename.substring(0, filename.lastIndexOf('.'));
			// FIXME: the plugin has to be in the toplevel of its jar file
			c = (Class<?>) jarLoader.loadClass(className, true);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		/*
		 * Create an instance of the class.
		 * 
		 * Note that created object's constructor-taking-no-arguments will be
		 * called as part of the object's creation.
		 */
		Object o = null;
		try {
			final String pluginRootDirectory = rootDirectory + Protocol.SYSTEM_SEPARATOR + c.getSimpleName();
			o = c.getDeclaredConstructor(String.class).newInstance(pluginRootDirectory);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (o instanceof AbstractPlugin) {
			AbstractPlugin plugin = (AbstractPlugin) o;
			for (AbstractPlugin loadedPlugin : this.plugins.values()) {
				// if plugin's simple name exists in the map...
				if (loadedPlugin.getClass().getSimpleName().equals(plugin.getClass().getSimpleName())) {
					// and plugin's fully-qualified name equals that plugin's
					// fully-qualified name
					if (loadedPlugin.getClass().getName().equals(plugin.getClass().getName())) {
						// ... then they are (assumed to be) the same, so
						// overwrite the existing plugin.
						System.out.println("Updating plugin " + plugin.getClass().getSimpleName());
						this.plugins.put(plugin.getClass().getSimpleName(), plugin);
						return;
					} else {
						// Else, they're different and we have a name clash, so
						// ignore the new one.
						return; // TODO: throw some sort of error here?
					}
				}
			}
			System.out.println("Adding new plugin " + plugin.getClass().getSimpleName());
			this.plugins.put(plugin.getClass().getSimpleName(), plugin);
		}
		return;
	}

	private void removePlugin(String filename) {
		System.out.println("Removing plugin " + filename);
		this.plugins.remove(filename);
	}
}
