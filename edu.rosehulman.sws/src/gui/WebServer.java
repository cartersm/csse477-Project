/*
 * WebServer.java
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

package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import protocol.Protocol;
import protocol.plugin.AbstractPlugin;
import server.JarClassLoader;
import server.Server;

/**
 * The application window for the {@link Server}, where you can update some
 * parameters and start and stop the server.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class WebServer extends JFrame {
	private static final long serialVersionUID = 5042579745743827174L;
	private static final String PLUGIN_ROOT = new File("plugins").getAbsolutePath();

	private JPanel panelRunServer;
	private JLabel lblPortNumber;
	private JTextField txtPortNumber;
	private JLabel lblRootDirectory;
	private JTextField txtRootDirectory;
	private JButton butSelect;

	private JPanel panelInput;
	private JButton butStartServer;
	private JButton butStopServer;
	private JLabel lblServiceRate;
	private JTextField txtServiceRate;

	private Server server;
	private ServiceRateUpdater rateUpdater;
	private WatchService watcher;

	/**
	 * For constantly updating the service rate in the GUI.
	 * 
	 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
	 */
	private class ServiceRateUpdater implements Runnable {
		public boolean stop = false;

		public void run() {
			while (!stop) {
				// Poll if server is not null and server is still accepting
				// connections
				if (server != null && !server.isStopped()) {
					double rate = server.getServiceRate();
					if (rate == Double.MIN_VALUE)
						WebServer.this.txtServiceRate.setText("Unknown");
					else
						WebServer.this.txtServiceRate.setText(Double.toString(rate));
				}

				// Poll at an interval of 500 milliseconds
				try {
					Thread.sleep(500);
				} catch (Exception e) {
				}
			}
		}
	}

	/** Creates new form WebServer */
	public WebServer() {
		initComponents();
		this.addListeners();
	}

	private void initComponents() {
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setTitle("Simple Web Server (SWS) Window");

		// Input panel widgets
		this.panelInput = new JPanel();
		this.lblPortNumber = new JLabel("Port Number");
		this.txtPortNumber = new JTextField("8080");
		this.lblRootDirectory = new JLabel("Select Root Directory");
		// Set the root directory to be the current working directory
		this.txtRootDirectory = new JTextField(System.getProperty("user.dir"));
		this.txtRootDirectory.setEditable(false);
		this.txtRootDirectory.setPreferredSize(new Dimension(400, 21));
		this.butSelect = new JButton("Select");

		this.panelInput.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
		this.panelInput.setLayout(new SpringLayout());
		this.panelInput.add(this.lblPortNumber);
		this.panelInput.add(this.txtPortNumber);
		this.panelInput.add(this.lblRootDirectory);
		this.panelInput.add(this.txtRootDirectory);
		this.panelInput.add(new JLabel("")); // Empty label
		this.panelInput.add(this.butSelect);

		// Compact the grid
		SpringUtilities.makeCompactGrid(this.panelInput, 3, 2, 5, 5, 5, 5);

		// Run server widgets
		this.panelRunServer = new JPanel();
		this.butStartServer = new JButton("Start Simple Web Server");
		this.butStopServer = new JButton("Stop Simple Web Server");
		this.butStopServer.setEnabled(false);
		this.lblServiceRate = new JLabel("Service Rate (Connections Serviced/Second)");
		this.txtServiceRate = new JTextField("Unknown");

		// panelRunServer uses FlowLayout by default
		this.panelRunServer.setBorder(BorderFactory.createTitledBorder("Run Server"));
		this.panelRunServer.setLayout(new SpringLayout());
		this.panelRunServer.add(this.butStartServer);
		this.panelRunServer.add(this.butStopServer);
		this.panelRunServer.add(this.lblServiceRate);
		this.panelRunServer.add(this.txtServiceRate);

		// Compact the grid
		SpringUtilities.makeCompactGrid(this.panelRunServer, 2, 2, 5, 5, 5, 5);

		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.add(this.panelInput, BorderLayout.CENTER);
		contentPane.add(this.panelRunServer, BorderLayout.SOUTH);

		pack();
	}

	private void addListeners() {
		// Add the action to be done when select directory button is pressed
		this.butSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get hold of the current directory
				String currentDirectory = WebServer.this.txtRootDirectory.getText();
				JFileChooser fileChooser = new JFileChooser(currentDirectory);
				fileChooser.setDialogTitle("Chose Web Server Root Directory");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setAcceptAllFileFilterUsed(false);
				if (fileChooser.showOpenDialog(WebServer.this) == JFileChooser.APPROVE_OPTION) {
					// A folder has been chosen
					currentDirectory = fileChooser.getSelectedFile().getAbsolutePath();
					WebServer.this.txtRootDirectory.setText(currentDirectory);
				}
			}
		});

		// Add action for run server
		this.butStartServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (server != null && !server.isStopped()) {
					JOptionPane.showMessageDialog(WebServer.this, "The web server is still running, try again later.",
							"Server Still Running Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Read port number
				int port = 80;
				try {
					port = Integer.parseInt(WebServer.this.txtPortNumber.getText());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(WebServer.this, "Invalid Port Number!", "Web Server Input Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Get hold of the root directory
				String rootDirectory = WebServer.this.txtRootDirectory.getText();

				// Now run the server in non-gui thread
				try {
					server = new Server(rootDirectory, port, WebServer.this);
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(1); // kill it if we get an error on the server
				}
				rateUpdater = new ServiceRateUpdater();

				// Disable widgets
				WebServer.this.disableWidgets();

				// Now run the server in a separate thread
				new Thread(new TimerThread(server)).start();

				// Also run the service rate updater thread
				new Thread(rateUpdater).start();

				try {
					initDirectoryWatcher();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		// Add action for stop button
		this.butStopServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (server != null && !server.isStopped())
					server.stop();
				if (rateUpdater != null)
					rateUpdater.stop = true;
				WebServer.this.enableWidgets();
			}
		});

		// Make sure the web server is stopped before closing the window
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (server != null && !server.isStopped())
					server.stop();
				if (rateUpdater != null)
					rateUpdater.stop = true;
			}
		});
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
			final String pluginRootDirectory = this.server.getRootDirectory() + Protocol.SYSTEM_SEPARATOR + c.getSimpleName();
			o = c.getDeclaredConstructor(String.class).newInstance(pluginRootDirectory);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (o instanceof AbstractPlugin) {
			AbstractPlugin plugin = (AbstractPlugin) o;
			for (AbstractPlugin loadedPlugin : this.server.getPlugins().values()) {
				// if plugin's simple name exists in the map...
				if (loadedPlugin.getClass().getSimpleName().equals(plugin.getClass().getSimpleName())) {
					// and plugin's fully-qualified name equals that plugin's
					// fully-qualified name
					if (loadedPlugin.getClass().getName().equals(plugin.getClass().getName())) {
						// ... then they are (assumed to be) the same, so
						// overwrite the existing plugin.
						System.out.println("Updating plugin " + plugin.getClass().getSimpleName());
						this.server.addPlugin(plugin.getClass().getSimpleName(), plugin);
						return;
					} else {
						// Else, they're different and we have a name clash, so
						// ignore the new one.
						return; // TODO: throw some sort of error here?
					}
				}
			}
			System.out.println("Adding new plugin " + plugin.getClass().getSimpleName());
			this.server.addPlugin(plugin.getClass().getSimpleName(), plugin);
		}
		return;
	}

	private void removePlugin(String filename) {
		System.out.println("Removing plugin " + filename);
		this.server.removePlugin(filename);
	}

	private void disableWidgets() {
		this.txtPortNumber.setEnabled(false);
		this.butSelect.setEnabled(false);
		this.butStartServer.setEnabled(false);
		this.butStopServer.setEnabled(true);
	}

	private void enableWidgets() {
		this.txtPortNumber.setEnabled(true);
		this.butSelect.setEnabled(true);
		this.butStartServer.setEnabled(true);
		this.butStopServer.setEnabled(false);
	}

	/**
	 * For displaying exception.
	 * 
	 * @param e
	 */
	public void showSocketException(Exception e) {
		JOptionPane.showMessageDialog(this, e.getMessage(), "Web Server Socket Problem", JOptionPane.ERROR_MESSAGE);
		if (this.server != null)
			this.server.stop();
		this.server = null;

		if (this.rateUpdater != null)
			this.rateUpdater.stop = true;
		this.rateUpdater = null;
		this.enableWidgets();
	}

	/**
	 * The application start point.
	 * 
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new WebServer().setVisible(true);
			}
		});
	}
}
