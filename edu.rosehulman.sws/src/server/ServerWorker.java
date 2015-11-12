package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.UnknownHostException;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.Protocol;
import protocol.plugin.AbstractPlugin;

public class ServerWorker implements Runnable {
	private static final int MAX_PROCESSING_REQUESTS = 5;
	private static final String PLUGIN_ROOT = new File("plugins").getAbsolutePath();

	private static final Logger LOGGER = LogManager.getLogger(ServerWorker.class);

	private String host;
	private int port;
	private String rootDirectory;

	private Map<String, AbstractPlugin> plugins;
	private WatchService watcher;

	private final Object numActiveRequestsLock = new Object();
	private Map<String, Integer> numActiveRequests;

	private final Object numProcessingRequestsLock = new Object();
	private int numProcessingRequests;

	public ServerWorker(String host, int port, String rootDirectory) throws IOException {
		this.host = host;
		this.port = port;
		this.rootDirectory = rootDirectory;

		this.plugins = new HashMap<>();

		this.numActiveRequests = new HashMap<String, Integer>();
		this.numProcessingRequests = 0;

		initDirectoryWatcher();
	}

	@Override
	public void run() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setUsername("root");
		factory.setPassword("root");

		try {
			Connection connection = factory.newConnection();
			final Channel channel = connection.createChannel();
			channel.queueDeclare(Server.SERVER_QUEUE, true, false, false, null);
			channel.queueDeclare(Server.SERVER_RESPONSE_QUEUE, true, false, false, null);

			channel.basicQos(1);
			final Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					
					if (getNumProcessingRequests() >= MAX_PROCESSING_REQUESTS) {
						System.out.println("Currently processing" + MAX_PROCESSING_REQUESTS
								+ " or more requests. Waiting for processes to finish before processing more.");
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					ByteArrayInputStream in = new ByteArrayInputStream(body);
					ObjectInputStream objIn = new ObjectInputStream(in);
					
					HttpRequest request;
					try {
						request = (HttpRequest) objIn.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}

					final ConnectionHandler handler = new ConnectionHandler(ServerWorker.this, request);
					incrementNumProcessingRequests();
					new Thread(new Runnable() {
						@Override
						public void run() {
							handler.run();
							decrementNumProcessingRequests();
							HttpResponse response = handler.getResponse();
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							ObjectOutputStream objOut;
							try {
								objOut = new ObjectOutputStream(out);
								objOut.writeObject(response);
								channel.basicPublish("", Server.SERVER_RESPONSE_QUEUE, MessageProperties.PERSISTENT_TEXT_PLAIN, out.toByteArray());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).start();

					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			};
			channel.basicConsume(Server.SERVER_QUEUE, false, consumer);
		} catch (Exception e) {
			e.printStackTrace();
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

	int getNumProcessingRequests() {
		return numProcessingRequests;
	}

	void setNumProcessingRequests(int numProcessingRequests) {
		this.numProcessingRequests = numProcessingRequests;
	}

	public synchronized void addToAuditTrail(HttpRequest request) {
		LOGGER.info("\n" + request);
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

	private void initDirectoryWatcher() throws IOException {
		// Watch for directory changes
		this.watcher = FileSystems.getDefault().newWatchService();
		Path path = Paths.get(URI.create("file:///" + PLUGIN_ROOT.replace("\\", "//").replace(" ", "%20")));
		path.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path file : stream) {
				addPlugin(file.toFile().getName());
			}
		} catch (IOException | DirectoryIteratorException x) {
			System.err.println(x);
		}

		// Listen asynchronously for directory changes
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listenForNewPlugins();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}, "DirectoryWatcher").start();
	}

	private void listenForNewPlugins() throws IOException {
		for (;;) {
			WatchKey key;
			try {
				key = this.watcher.take();
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
			final String pluginRootDirectory = getRootDirectory() + Protocol.SYSTEM_SEPARATOR + c.getSimpleName();
			o = c.getDeclaredConstructor(String.class).newInstance(pluginRootDirectory);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (o instanceof AbstractPlugin) {
			AbstractPlugin plugin = (AbstractPlugin) o;
			for (AbstractPlugin loadedPlugin : getPlugins().values()) {
				// if plugin's simple name exists in the map...
				if (loadedPlugin.getUriName().equals(plugin.getUriName())) {
					// and plugin's fully-qualified name equals that plugin's
					// fully-qualified name
					if (loadedPlugin.getClass().getName().equals(plugin.getClass().getName())) {
						// ... then they are (assumed to be) the same, so
						// overwrite the existing plugin.
						System.out.println("Updating plugin " + plugin.getUriName());
						addPlugin(plugin.getUriName(), plugin);
						return;
					} else {
						// Else, they're different and we have a name clash, so
						// ignore the new one.
						return; // TODO: throw some sort of error here?
					}
				}
			}
			System.out.println("Adding new plugin " + plugin.getUriName());
			addPlugin(plugin.getUriName(), plugin);
		}
		return;
	}

	private String getRootDirectory() {
		return this.rootDirectory;
	}

	public static void main(String[] args) throws UnknownHostException, IOException {
		Scanner in = new Scanner(System.in);
		System.out.println("Enter the host IP and port to connect to (Format: \"IP:Port\"): ");
		String ipString = in.nextLine();

		String host = ipString.substring(0, ipString.indexOf(":"));
		int port = Integer.parseInt(ipString.substring(ipString.indexOf(":") + 1));

		String currentDirectory = new File(".").getAbsolutePath();

		System.out.println("Specify the root directory to use, relative to " + currentDirectory + ":");
		String relativePath = in.nextLine();
		in.close();

		new ServerWorker(host, port, relativePath).run();
	}
}