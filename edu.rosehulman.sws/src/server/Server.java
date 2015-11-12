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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import gui.WebServer;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.plugin.AbstractPlugin;

/**
 * This represents a welcoming server for the incoming TCP request from a HTTP
 * client such as a web browser.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {

	private static final Logger LOGGER = LogManager.getLogger(Server.class);
	private static final int MAX_NUM_REQUESTS_BEFORE_BAN = 10;
	private static final int MAX_PROCESSING_REQUESTS = 5;
	private static final String HOST = "cartersm-w530.rose-hulman.edu";
	static final String SERVER_QUEUE = "server-queue";
	private String rootDirectory;
	private int port;
	private boolean stop;
	private ServerSocket welcomeSocket;

	private long connections;
	private long serviceTime;

	WebServer window;
	private Map<String, AbstractPlugin> plugins;

	private final Object waitingRequestsLock = new Object();
	private List<Socket> waitingRequests;

	private final Object bannedUsersLock = new Object();
	private List<String> bannedUsers;

	private final Object hadATurnLock = new Object();
	private List<String> hadATurn;

	private final Object numActiveRequestsLock = new Object();
	private Map<String, Integer> numActiveRequests;

	private final Object numProcessingRequestsLock = new Object();
	private int numProcessingRequests;
	private Map<Integer, Socket> sockets;

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

		this.waitingRequests = new ArrayList<Socket>();
		this.bannedUsers = new ArrayList<String>();
		this.numActiveRequests = new HashMap<String, Integer>();
		this.hadATurn = new ArrayList<String>();
		this.numProcessingRequests = 0;

		PriorityQueueHandler queueHandler = new PriorityQueueHandler(this);
		new Thread(queueHandler).start();
		
		this.sockets = new HashMap<>();
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
	 * Returns connections serviced per second. Synchronized to be used in
	 * threaded environment.
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
	 * Increments number of connection by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementConnections(long value) {
		this.connections += value;
	}

	public synchronized void addToAuditTrail(HttpRequest request) {
		LOGGER.info("\n" + request);
	}

	/**
	 * Increments the service time by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementServiceTime(long value) {
		this.serviceTime += value;
	}

	/**
	 * The entry method for the main server thread that accepts incoming TCP
	 * connection request and creates a {@link ConnectionHandler} for the
	 * request.
	 */
	public synchronized void addWaitingRequest(Socket connection) {
		synchronized (this.waitingRequestsLock) {
			this.waitingRequests.add(connection);
		}
	}

	public synchronized Socket getNextConnection() {
		synchronized (this.waitingRequestsLock) {
			synchronized (this.hadATurnLock) {
				if (waitingRequests.size() <= 0) {
					return null;
				}

				for (int i = 0; i < this.waitingRequests.size(); i++) {
					Socket connection = this.waitingRequests.get(i);
					if (this.hadATurn.contains(connection.getInetAddress().toString())) {
						continue;
					}
					this.hadATurn.add(connection.getInetAddress().toString());
					this.waitingRequests.remove(i);
					return connection;
				}

				this.hadATurn = new ArrayList<String>();
				Socket connection = this.waitingRequests.get(0);
				this.waitingRequests.remove(0);
				return connection;
			}
		}
	}

	public synchronized void removeAllRequests(String inetAddress) {
		synchronized (this.waitingRequestsLock) {
			for (int i = 0; i < waitingRequests.size(); i++) {
				if (this.waitingRequests.get(i).getInetAddress().toString().equals(inetAddress)) {
					this.waitingRequests.remove(i);
					i--;
				}
			}
		}
	}

	public synchronized void addBanneduser(String inetAddress) {
		synchronized (this.bannedUsersLock) {
			this.bannedUsers.add(inetAddress);
		}
	}

	public synchronized boolean userIsBanned(String inetAddress) {
		synchronized (this.bannedUsersLock) {
			return this.bannedUsers.contains(inetAddress);
		}
	}

	public synchronized int getNumActiveRequests(String inetAddress) {
		synchronized (this.numActiveRequestsLock) {
			if (this.numActiveRequests.containsKey(inetAddress)) {
				return this.numActiveRequests.get(inetAddress);
			}
			return 0;
		}
	}

	public synchronized void incrementNumActiveRequests(String inetAddress) {
		synchronized (this.numActiveRequestsLock) {
			if (this.numActiveRequests.containsKey(inetAddress)) {
				int numActiveRequests = this.numActiveRequests.get(inetAddress);
				this.numActiveRequests.put(inetAddress, numActiveRequests + 1);
			} else {
				this.numActiveRequests.put(inetAddress, 1);
			}
		}
	}

	public synchronized void decrementNumActiveRequests(String inetAddress) {
		synchronized (this.numActiveRequestsLock) {
			if (this.numActiveRequests.containsKey(inetAddress)) {
				int numActiveRequests = this.numActiveRequests.get(inetAddress);
				this.numActiveRequests.put(inetAddress, numActiveRequests - 1);
			} else {
				this.numActiveRequests.put(inetAddress, 1);
			}
		}
	}

	public synchronized void incrementNumProcessingRequests() {
		synchronized (this.numProcessingRequestsLock) {
			this.numProcessingRequests++;
		}
	}

	public synchronized void decrementNumProcessingRequests() {
		synchronized (this.numProcessingRequestsLock) {
			this.numProcessingRequests--;
		}
	}

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

				if (this.userIsBanned(inetAddress)) {
					System.out.println(inetAddress + "'s request was ignored because he is banned.");
					continue;
				}

				int numActiveRequests = this.getNumActiveRequests(inetAddress);
				if (numActiveRequests > Server.MAX_NUM_REQUESTS_BEFORE_BAN) {
					System.out.println(inetAddress + " was banned for making too many requests.");
					this.addBanneduser(inetAddress);
					this.removeAllRequests(inetAddress);
					continue;
				}

				this.incrementNumActiveRequests(inetAddress);
				this.addWaitingRequest(connectionSocket);

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

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(HOST);

			try {
				Connection connection = factory.newConnection();
				Channel channel = connection.createChannel();
				channel.queueDeclare(SERVER_QUEUE, true, false, false, null);

				while (true) {
					if (server.numProcessingRequests >= Server.MAX_PROCESSING_REQUESTS) {
						try {
							System.out.println("Currently processing" + Server.MAX_PROCESSING_REQUESTS
									+ " or more requests. Waiting for processes to finish before processing more.");
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}

					final Socket socket = server.getNextConnection();
					if (socket == null) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}

					long start = System.currentTimeMillis();

					InputStream inStream = null;
					OutputStream outStream = null;

					try {
						inStream = socket.getInputStream();
						outStream = socket.getOutputStream();
					} catch (Exception e) {
						// Cannot do anything if we have exception reading input
						// or output
						// stream
						// May be have text to log this for further analysis?
						e.printStackTrace();

						// Increment number of connections by 1
						server.incrementConnections(1);
						// Get the end time
						long end = System.currentTimeMillis();
						this.server.incrementServiceTime(end - start);
						continue;
					}

					// At this point we have the input and output stream of the
					// socket
					// Now lets create a HttpRequest object
					HttpRequest request = null;
					HttpResponse response = null;
					try {
						request = HttpRequest.read(inStream);
						request.setSocketHash(socket.hashCode());
					} catch (ProtocolException pe) {
						// We have some sort of protocol exception. Get its
						// status code and
						// create response
						// We know only two kind of exception is possible inside
						// fromInputStream
						// Protocol.BAD_REQUEST_CODE and
						// Protocol.NOT_SUPPORTED_CODE
						int status = pe.getStatus();
						if (status == Protocol.BAD_REQUEST_CODE) {
							response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
						}
						// TODO: Handle version not supported code as well
					} catch (Exception e) {
						e.printStackTrace();
						// For any other error, we will create bad request
						// response as well
						response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
					}

					if (response != null) {
						// Means there was an error, now write the response
						// object to the
						// socket
						try {
							response.write(outStream);
							// System.out.println(response);
						} catch (Exception e) {
							// We will ignore this exception
							e.printStackTrace();
						}

						// Increment number of connections by 1
						server.incrementConnections(1);
						// Get the end time
						long end = System.currentTimeMillis();
						this.server.incrementServiceTime(end - start);
						return;
					}
					
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ObjectOutputStream objOut = new ObjectOutputStream(out);
					objOut.writeObject(request);
					
					channel.basicPublish("", SERVER_QUEUE, MessageProperties.PERSISTENT_TEXT_PLAIN, out.toByteArray());
					sockets.put(socket.hashCode(), socket);
					
					objOut.close();
					out.close();
					
					
					final ConnectionHandler handler = new ConnectionHandler(this.server, socket);
					server.incrementNumProcessingRequests();
					new Thread(new Runnable() {
						@Override
						public void run() {
							handler.run();
							server.decrementNumActiveRequests(socket.getInetAddress().toString());
							server.decrementNumProcessingRequests();
						}
					}).start();
				}
			} catch (Exception e) {
				window.showSocketException(e);
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
			// This will force welcomeSocket to come out of the blocked
			// accept()
			// method
			// in the main loop of the start() method
			Socket socket = new Socket(InetAddress.getLocalHost(), port);

			// We do not have any other job for this socket so just close it
			socket.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Checks if the server is stopeed or not.
	 * 
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

	int getNumProcessingRequests() {
		return numProcessingRequests;
	}

	void setNumProcessingRequests(int numProcessingRequests) {
		this.numProcessingRequests = numProcessingRequests;
	}
}
