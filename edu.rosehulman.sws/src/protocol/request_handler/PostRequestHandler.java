package protocol.request_handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import server.Server;

public class PostRequestHandler implements IRequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request, Server server) {
		HttpResponse response;

		// Handling POST request here
		// Get relative URI path from request
		String uri = request.getUri();
		// Get body of request
		String body = new String(request.getBody());
		// Get root directory path from server
		String rootDirectory = server.getRootDirectory();
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + uri);
		// Check if the file exists
		if (file.exists()) {
			if (file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + uri
						+ System.getProperty("file.separator")
						+ Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					// Lets create 200 OK response
					response = appendToFile(file, body);
				} else {
					response = createFile(file, body);
				}
				// It's a file. Let's append to it.
			} else {
				response = appendToFile(file, body);
			}
		} else {
			// File does not exist so lets create it
			response = createFile(file, body);
		}
		return response;
	}

	/**
	 * @param file
	 * @param body
	 * @return
	 */
	private HttpResponse createFile(File file, String body) {
		HttpResponse response;
		PrintWriter out = null;
		try {
			out = new PrintWriter(file.getAbsolutePath());
			out.println(body);
			response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		} finally {
			out.close();
		}
		return response;
	}

	/**
	 * @param file
	 * @param body
	 * @return
	 */
	private HttpResponse appendToFile(File file, String body) {
		HttpResponse response;
		try {
			Files.write(Paths.get(file.getAbsolutePath()), body.getBytes(),
					StandardOpenOption.APPEND);
			response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
		} catch (IOException e) {
			e.printStackTrace();
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}
		return response;
	}

}
