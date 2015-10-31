
import java.util.HashMap;
import java.util.Map;

import protocol.IServlet;
import protocol.plugin.AbstractPlugin;

/**
 * A basic Plugin implementation that performs standard HTTP operations.
 * 
 */
public class BasicPlugin extends AbstractPlugin {

	public BasicPlugin(String rootDirectory) {
		super(rootDirectory);
	}

	@Override
	public Map<String, IServlet> createServlets() {
		Map<String, IServlet> map = new HashMap<>();
		map.put(BasicServlet.class.getSimpleName(), new BasicServlet());
		return map;
	}

}
