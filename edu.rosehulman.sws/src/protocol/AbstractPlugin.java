package protocol;

import java.text.ParseException;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class AbstractPlugin {
	// TODO? BasicServlet implementation as an instance var? Else new instance
	// every time.
	protected Map<String, IServlet> servlets;
	private String rootDirectory;

	public AbstractPlugin(String rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.servlets = createServlets();
	}

	public abstract HttpResponse handle(HttpRequest request);

	public abstract Map<String, IServlet> createServlets();

	/**
	 * Returns the root working directory for this Plugin
	 * 
	 * @return
	 */
	protected final String getRootDirectory() {
		return this.rootDirectory;
	}

	/**
	 * Retrieves a Servlet from this Plugin's IServlet map.
	 * 
	 */
	protected final IServlet getServlet(String key) throws ServletUndefinedException {
		final IServlet servlet = this.servlets.get(key);
		// Let the extending class handle the case where we don't have that
		// servlet
		if (servlet == null) {
			throw new ServletUndefinedException("Servlet \"" + key + "\" does not exist.");
		}
		return servlet;
	}

	/**
	 * Parses the Servlet to use from the given URI string, and returns the
	 * Servlet instance corresponding to that name.
	 * 
	 * @throws NoSuchElementException
	 *             If the given servlet is not found in the map.
	 * 
	 * @throws ParseException
	 *             If there is no servlet in the URI to be parsed.
	 * 
	 */
	protected final IServlet getServletFromUri(final String uri) throws ServletUndefinedException {
		final String className = this.getClass().getSimpleName();
		// the character after the "/" after the plugin name
		int start = uri.indexOf(className) + className.length() + 1;
		// the next "/"
		int end = uri.indexOf("/", start);

		String servletUri;
		try {
			if (end == -1) { // We didn't find another "/"
				// We still may have a servlet, but there is no "/" after it.
				servletUri = uri.substring(start);
			} else {
				servletUri = uri.substring(start, end);
			}
		} catch (StringIndexOutOfBoundsException e) {
			throw new ServletUndefinedException("No servlet found in URI \"" + uri + "\"");
		}
		return getServlet(servletUri);
	}

	protected final class ServletUndefinedException extends Exception {
		private static final long serialVersionUID = -4473656847785419240L;

		public ServletUndefinedException(String message) {
			super(message);
		}
	}
}
