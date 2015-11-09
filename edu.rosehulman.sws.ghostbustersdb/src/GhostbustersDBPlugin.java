import java.util.HashMap;
import java.util.Map;

import protocol.IServlet;
import protocol.plugin.AbstractPlugin;
import protocol.plugin.ServletUndefinedException;

public class GhostbustersDBPlugin extends AbstractPlugin {
	private DBHelper dbHelper;

	public GhostbustersDBPlugin(String rootDirectory) {
		super(rootDirectory);
		this.dbHelper = new DBHelper();
	}

	@Override
	protected IServlet getServletFromUri(String uri) throws ServletUndefinedException {
		try {
			uri = uri.substring(uri.indexOf("v1/") + 3);
		} catch (StringIndexOutOfBoundsException e) {
			throw new ServletUndefinedException("API version not found");
		}
		
		int index = uri.indexOf("/");
		if (index == -1) {
			// We should have only the servlet name.
			// getServlet() will throw an exception if we don't.
			return getServlet(uri);
		}
		return getServlet(uri.substring(0, index));
	}

	@Override
	protected Map<String, IServlet> createServlets() {
		Map<String, IServlet> servletMap = new HashMap<>();
		servletMap.put("ghosts", new GhostsServlet(this.dbHelper));
		servletMap.put("haunts", new HauntsServlet(this.dbHelper));
		return servletMap;
	}

	@Override
	public String getUriName() {
		return "GhostbustersDB";
	}

}
