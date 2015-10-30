

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
import protocol.IServlet;
import protocol.Protocol;

/**
 * A basic servlet implementation to do standard HTTP operations with no
 * additional logic.
 * 
 */
// TODO: with all servlets: we should only have <root>/Plugin/Servlet, NOT <root>/Plugin/Servlet/Path.
// The latter should be <root>/Plugin/Path. e.g., standard operations act on the plugin itself.
// The Servlet always acts a specific way on a specific resource/resources (with "?var=val&var2=val2", etc. in the URL)
public class BasicServlet implements IServlet {

	@Override
	public HttpResponse doGet(HttpRequest request, String rootDirectory) {
		HttpResponse response;
		// Map<String, String> header = request.getHeader();
		// String date = header.get("if-modified-since");
		// String hostName = header.get("host");
		//
		// Handling GET request here
		String uri = request.getUri();
		String path = getPathFromUri(uri);
		
		File file = new File(rootDirectory + path);

		if (file.exists()) {
			if (file.isDirectory()) {
				String location = rootDirectory + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				} else {
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
				}
			} else { 
				response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
			}
		} else {
			response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
		}
		return response;
	}

	@Override
	public HttpResponse doPut(HttpRequest request, String rootDirectory) {
		HttpResponse response;

		// Handling PUT request here
		String uri = request.getUri();
		String path = getPathFromUri(uri);
		String body = new String(request.getBody());

		File file = new File(rootDirectory + path);

		if (file.exists()) {
			if (file.isDirectory()) {
				String location = rootDirectory + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					response = overwriteFile(file, body);
				} else {
					response = createFile(file, body);
				}
			} else {
				response = overwriteFile(file, body);
			}
		} else {
			response = createFile(file, body);
		}
		return response;
	}

	@Override
	public HttpResponse doPost(HttpRequest request, String rootDirectory) {
		HttpResponse response;

		// Handling POST request here
		String uri = request.getUri();
		String path = getPathFromUri(uri);
		String body = new String(request.getBody());
		
		File file = new File(rootDirectory + path);
		
		if (file.exists()) {
			if (file.isDirectory()) {
				String location = rootDirectory + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					response = appendToFile(file, body);
				} else {
					response = createFile(file, body);
				}
			} else {
				response = appendToFile(file, body);
			}
		} else {
			response = createFile(file, body);
		}
		return response;
	}

	@Override
	public HttpResponse doDelete(HttpRequest request, String rootDirectory) {
		HttpResponse response;
		// Handling POST request here
		String uri = request.getUri();
		String path = getPathFromUri(uri);
		final String deleted = rootDirectory + Protocol.SYSTEM_SEPARATOR + "deleted.txt";

		File file = new File(rootDirectory + path);

		if (file.exists()) {
			if (file.isDirectory()) {
				String location = rootDirectory + path + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
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

	/* ----- Helper methods ----- */
	protected String getPathFromUri(String uri) {
		if (uri.contains(this.getClass().getSimpleName())) {
			// If the URI is /Plugin/Servlet, return the path to index.html
			return Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
		}
		// Find the beginning of the path
		final int start = uri.indexOf("/", 1);
		return uri.substring(start);
	}

	public HttpResponse createFile(File file, String body) {
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

	public HttpResponse overwriteFile(File file, String body) {
		file.delete();
		return this.createFile(file, body);
	}

	public HttpResponse appendToFile(File file, String body) {
		HttpResponse response;
		try {
			Files.write(Paths.get(file.getAbsolutePath()), body.getBytes(), StandardOpenOption.APPEND);
			response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
		} catch (IOException e) {
			e.printStackTrace();
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}
		return response;
	}

}
