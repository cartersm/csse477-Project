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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gui.WebServer;
import protocol.HttpRequest;
import protocol.plugin.AbstractPlugin;

/**
 * This represents a welcoming server for the incoming
 * TCP request from a HTTP client such as a web browser. 
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(Server.class);
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
	
	private static final int MAX_SIZE_OF_AUDIT_TRAIL = 100;
	private List<HttpRequest> auditTrail;

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
		this.numProcessingConnections = 0;
		this.auditTrail = new ArrayList<HttpRequest>();
		
		PriorityQueueHandler queueHandler = new PriorityQueueHandler(this);
		new Thread(queueHandler).start();
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
	
	public synchronized void addToAuditTrail(HttpRequest request) {
		LOGGER.info("\n" + request);
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
				System.out.println("Request from " + inetAddress);
				if (bannedInetAddresses.contains(inetAddress)) {
					System.out.println("But " + inetAddress + " is banned!");
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
				
				if (numConnections + 1 > MAX_ACTIVE_CONNECTIONS_FOR_USER) {
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

	/**
	 * @return
	 */
	public Map<String, AbstractPlugin> getPlugins() {
		return Collections.unmodifiableMap(this.plugins);
	}

	/**
	 * @param simpleName
	 * @param plugin
	 */
	public void addPlugin(String simpleName, AbstractPlugin plugin) {
		this.plugins.put(simpleName, plugin);
	}

	/**
	 * @param filename
	 */
	public void removePlugin(String filename) {
		this.plugins.remove(filename);
	}
}
