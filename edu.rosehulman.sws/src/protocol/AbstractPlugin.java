package protocol;

import java.util.Map;
import java.util.NoSuchElementException;

public abstract class AbstractPlugin {
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
	protected final IServlet getServlet(String key) throws NoSuchElementException {
		final IServlet servlet = this.servlets.get(key);
		// Let the extending class handle the case where we don't have that
		// servlet
		if (servlet == null) {
			throw new NoSuchElementException("Servlet " + key + "cannot be found.");
		}
		return servlet;
	}

	/**
	 * Parses the Servlet to use from the given URI string, and returns the
	 * Servlet instance corresponding to that name.
	 * 
	 * @throws NoSuchElementException
	 * 
	 */
	protected final IServlet getServletFromUri(final String uri) throws NoSuchElementException {
		final String className = this.getClass().getSimpleName();
		// the character after the "/" after the plugin name
		int start = uri.indexOf(className) + className.length() + 1;
		// the next "/"
		int end = uri.indexOf("/", start) + 1;

		String servletUri = uri.substring(start, end);
		return getServlet(servletUri);
	}
}
