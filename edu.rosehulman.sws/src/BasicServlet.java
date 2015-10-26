

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
public class BasicServlet implements IServlet {

	@Override
	public HttpResponse doGet(HttpRequest request, String rootDirectory) {
		HttpResponse response;
		// Map<String, String> header = request.getHeader();
		// String date = header.get("if-modified-since");
		// String hostName = header.get("host");
		//
		// Handling GET request here
		// Get relative URI path from request
		String uri = request.getUri();
		// Combine them together to form absolute file path
		String path = getPathFromUri(uri);
		File file = new File(rootDirectory + path);
		// Check if the file exists
		if (file.exists()) {
			if (file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					// Lets create 200 OK response
					response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				} else {
					// File does not exist so lets create 404 file not found
					// code
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
				}
			} else { // Its a file
						// Lets create 200 OK response
				response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
			}
		} else {
			// File does not exist so lets create 404 file not found code
			response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
		}
		return response;
	}

	@Override
	public HttpResponse doPut(HttpRequest request, String rootDirectory) {
		HttpResponse response;

		// Handling PUT request here
		// Get relative URI path from request
		String uri = request.getUri();
		String path = getPathFromUri(uri);
		// Get body of request
		String body = new String(request.getBody());
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + path);
		// Check if the file exists
		if (file.exists()) {
			if (file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					// Let's overwrite it
					response = overwriteFile(file, body);
				} else {
					response = createFile(file, body);
				}
				// It's a file. Let's overwrite it.
			} else {
				response = overwriteFile(file, body);
			}
		} else {
			// File does not exist so lets create it
			response = createFile(file, body);
		}
		return response;
	}

	@Override
	public HttpResponse doPost(HttpRequest request, String rootDirectory) {
		HttpResponse response;

		// Handling POST request here
		// Get relative URI path from request
		String uri = request.getUri();
		String path = getPathFromUri(uri);
		// Get body of request
		String body = new String(request.getBody());
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + path);
		// Check if the file exists
		if (file.exists()) {
			if (file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
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

	@Override
	public HttpResponse doDelete(HttpRequest request, String rootDirectory) {
		HttpResponse response = null;
		// Handling POST request here
		// Get relative URI path from request
		String uri = request.getUri();
		String path = getPathFromUri(uri);
		final String deleted = rootDirectory + Protocol.SYSTEM_SEPARATOR + "deleted.txt";

		// Combine them together to form absolute file path
		File file = new File(rootDirectory + path);

		// Check if the file exists
		if (file.exists()) {
			if (file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + path + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
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
		final int servlet = uri.indexOf(this.getClass().getSimpleName());
		final int start = uri.indexOf("/", servlet);
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
