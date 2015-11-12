package server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

class QueueHandler implements Runnable {

	private Server server;
	private String host;
	private int port;

	public QueueHandler(Server server, String host, int port) {
		this.server = server;
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		// TODO: move all server increment/decrement/banning/plugins here
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setPort(port);
		try {
			Connection connection = factory.newConnection();
			final Channel channel = connection.createChannel();
			channel.queueDeclare(Server.SERVER_QUEUE, true, false, false, null);

			channel.basicQos(1);
			final Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					if (server.getNumProcessingRequests() >= Server.MAX_PROCESSING_REQUESTS) {
						System.out.println("Currently processing" + Server.MAX_PROCESSING_REQUESTS
								+ " or more requests. Waiting for processes to finish before processing more.");
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					byte[] buffer = new byte[body.length];
					Input input = new Input(new ByteArrayInputStream(buffer));
					final Socket socket = new Kryo().readObject(input, Socket.class);

					final ConnectionHandler handler = new ConnectionHandler(QueueHandler.this.server, socket);
					server.incrementNumProcessingRequests();
					new Thread(new Runnable() {
						@Override
						public void run() {
							handler.run();
							QueueHandler.this.server.decrementNumActiveRequests(socket.getInetAddress().toString());
							QueueHandler.this.server.decrementNumProcessingRequests();
						}
					}).start();

					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			};
			channel.basicConsume(Server.SERVER_QUEUE, false, consumer);
		} catch (Exception e) {
			this.server.window.showSocketException(e);
		}
	}

	public static void main(String[] args) throws UnknownHostException {
		Scanner in = new Scanner(System.in);
		System.out.println("Enter the host IP and port to connect to (Format: \"IP:Port\"): ");
		String ipString = in.nextLine();
		
		String host = ipString.substring(ipString.indexOf(":"));
		int port = Integer.parseInt(ipString.substring(ipString.indexOf(":") + 1));
		
		new QueueHandler(null, host, port);
	}
}
