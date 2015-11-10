package protocol.plugin;

import java.util.Map;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

/**
 * The superclass for all plugins that the server will use.
 * 
 */
public abstract class AbstractPlugin {
	/*
	 * The servlets available to this plugin.
	 */
	private Map<String, IServlet> servlets;

	/*
	 * The public directory available to this plugin.
	 */
	private String rootDirectory;

	/*
	 * The default servlet implementation. Used exclusively for handling HTTP
	 * operations performed on "/Plugin/Path".
	 */
	private DefaultServlet defaultServlet;

	public AbstractPlugin(String rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.servlets = createServlets();
		this.defaultServlet = new DefaultServlet();
	}

	/**
	 * Calls the servlet defined in the given request to perform the requested
	 * operation.
	 * 
	 * If the servlet does not exist
	 * 
	 * @param request
	 * @return
	 */
	public final HttpResponse handle(HttpRequest request) {
		// TODO?: parse form fields if they exist
		final String uri = request.getUri();
		IServlet servlet;
		try {
			servlet = getServletFromUri(uri);
		} catch (ServletUndefinedException e) {
			e.printStackTrace();
			// TODO: return a file or header that includes the error message?
			return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}

		switch (request.getMethod()) {
		case Protocol.GET:
			return servlet.doGet(request, this.rootDirectory);
		case Protocol.PUT:
			return servlet.doPut(request, this.rootDirectory);
		case Protocol.POST:
			return servlet.doPost(request, this.rootDirectory);
		case Protocol.DELETE:
			return servlet.doDelete(request, this.rootDirectory);
		case Protocol.OPTIONS:
			return servlet.doOptions(request, this.rootDirectory);
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	/**
	 * Returns a map of URI names to IServlets. The key for an item in the map
	 * should be the Servlet's name as it appears in the request URI.
	 */
	protected abstract Map<String, IServlet> createServlets();

	/**
	 * Returns the name of this plugin as it will appear in a request URI
	 */
	public abstract String getUriName();

	/**
	 * Retrieves a Servlet from this Plugin's IServlet map.
	 * 
	 */
	protected final IServlet getServlet(String key) throws ServletUndefinedException {
		final IServlet servlet = this.servlets.get(key);
		if (servlet == null) {
			throw new ServletUndefinedException("Servlet \"" + key + "\" does not exist.");
		}
		return servlet;
	}

	/**
	 * Parses the Servlet to use from the given URI string, and returns the
	 * Servlet instance corresponding to that name.
	 * 
	 * @throws ServletUndefinedException
	 *             If the given servlet is not found in the map.
	 * 
	 */
	protected IServlet getServletFromUri(final String uri) throws ServletUndefinedException {
		final String className = getUriName();
		// the character after the "/" after the plugin name
		int start = uri.indexOf(className) + className.length() + 1;

		String servletUri = uri.substring(start).trim();
		if (!(servletUri.endsWith("Servlet") || servletUri.endsWith("Servlet/"))) {
			return this.defaultServlet;
		}
		return getServlet(servletUri);
	}

	protected final String getRootDirectory() {
		return this.rootDirectory;
	}
}
