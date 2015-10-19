package protocol.request_handler;

import java.io.File;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import server.Server;

public class GetRequestHandler implements RequestHandler {

	public HttpResponse handle(HttpRequest request, Server server) {
		HttpResponse response = null;
//				Map<String, String> header = request.getHeader();
//				String date = header.get("if-modified-since");
//				String hostName = header.get("host");
//				
		// Handling GET request here
		// Get relative URI path from request
		String uri = request.getUri();
		// Get root directory path from server
		String rootDirectory = server.getRootDirectory();
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + uri);
		// Check if the file exists
		if(file.exists()) {
			if(file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
				file = new File(location);
				if(file.exists()) {
					// Lets create 200 OK response
					response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				}
				else {
					// File does not exist so lets create 404 file not found code
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
				}
			}
			else { // Its a file
				// Lets create 200 OK response
				response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
			}
		}
		else {
			// File does not exist so lets create 404 file not found code
			response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
		}
		return response;
	}
}
