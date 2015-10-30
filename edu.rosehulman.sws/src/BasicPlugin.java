

import java.util.HashMap;
import java.util.Map;

import protocol.AbstractPlugin;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

/**
 * A basic Plugin implementation that demonstrates the usages and
 * implementations of the methods defined in {@link AbstractPlugin}
 * 
 */
public class BasicPlugin extends AbstractPlugin {

	public BasicPlugin(String rootDirectory) {
		super(rootDirectory);
	}

	@Override
	// TODO: pull up and make final? really the servlet will have the logic here.
	public HttpResponse handle(HttpRequest request) {
		// TODO: if no servlet, ensure that there's a filepath, and do basic static resource operations
		// Else, make sure there's no path after the servlet, and parse form fields if they exist
		final String uri = request.getUri();
		IServlet servlet;
		try {
			servlet = getServletFromUri(uri);
		} catch (ServletUndefinedException e) {
//			return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
			// TODO: logging of some sort?
			// TODO: check whether it's a file (or just route traffic to BasicServlet?)
			servlet = new BasicServlet(); // Handles basic static file operations
		}
		
		switch (request.getMethod()) {
		case Protocol.GET:
			return servlet.doGet(request, getRootDirectory());
		case Protocol.PUT:
			return servlet.doPut(request, getRootDirectory());
		case Protocol.POST:
			return servlet.doPost(request, getRootDirectory());
		case Protocol.DELETE:
			return servlet.doDelete(request, getRootDirectory());
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public Map<String, IServlet> createServlets() {
		Map<String, IServlet> map = new HashMap<>();
		map.put(BasicServlet.class.getSimpleName(), new BasicServlet());
		return map;
	}

}
