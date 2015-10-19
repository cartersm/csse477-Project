package protocol.request_handler;

import java.io.File;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.Protocol;
import server.Server;

public class PutRequestHandler implements RequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request, Server server) {
		HttpResponse response = null;
		// Handling POST request here
		// Get relative URI path from request
		String uri = request.getUri();
		// Get root directory path from server
		String rootDirectory = server.getRootDirectory();
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + uri);

		String body = new String(request.getBody());
		
		// Check if the file exists
		if (file.exists()) {
			if (file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					// TODO: overwrite file with body
					// response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				} else {
					// TODO: create file and write body
					// response = HttpResponseFactory.create200OK(Protocol.CLOSE);
				}
			} else {
				// TODO: overwrite file with body
				// response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
			}
		} else {
			// TODO: create file and write body
			// response = HttpResponseFactory.create200OK(Protocol.CLOSE);
		}
		return response;
	}

}
