package protocol.request_handler;

import java.io.File;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import server.Server;

public class DeleteRequestHandler implements IRequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request, Server server) {
		HttpResponse response = null;
		// Handling POST request here
		// Get relative URI path from request
		String uri = request.getUri();
		// Get root directory path from server
		String rootDirectory = server.getRootDirectory();
		final String deleted = rootDirectory + System.getProperty("file.separator") + "deleted.txt";

		// Combine them together to form absolute file path
		File file = new File(rootDirectory + uri);
		System.out.println(file.getAbsolutePath());

		// Check if the file exists
		if (file.exists()) {
			if (file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					if (!file.getName().equalsIgnoreCase("deleted.txt") && file.delete()) {
						response = HttpResponseFactory.create200OK(new File(deleted), Protocol.CLOSE);
					} else {
						response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
					}
				} else {
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
				}
			} else {
				if (!file.getName().equalsIgnoreCase("deleted.txt") && file.delete()) {
					response = HttpResponseFactory.create200OK(new File(deleted), Protocol.CLOSE);
				} else {
					response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
				}
			}
		} else {
			response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
		}
		return response;
	}

}
