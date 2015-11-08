import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

public class GhostsServlet implements IServlet {
	// indices of "/" characters in the expected URI
	private static final int SLASH_BEFORE_GHOSTS = 18;
	private static final int SLASH_AFTER_GHOSTS = 24;
	
	private static final String GET_ALL_FORMAT = "SELECT * FROM " + DBHelper.GHOSTS_TABLE_NAME;
	private static final String GET_ONE_FORMAT = "SELECT * FROM " + DBHelper.GHOSTS_TABLE_NAME + " WHERE ID=%d";

	private DBHelper dbHelper;

	public GhostsServlet(DBHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	@Override
	public HttpResponse doGet(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_BEFORE_GHOSTS || index + 1 == uri.length()) {
			// TODO: return a list of ghosts
			return null;
		} else if (index == SLASH_AFTER_GHOSTS) {
			// TODO: return the ghost given by id
			String id = uri.substring(index + 1);
			return null;
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doPut(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_AFTER_GHOSTS && index + 1 < uri.length()) {
			// TODO: update the ghost given by id
			String id = uri.substring(index + 1);
			return null;
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doPost(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_BEFORE_GHOSTS || index + 1 == uri.length()) {
			// TODO: add a new ghost
			return null;
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doDelete(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_AFTER_GHOSTS && index + 1 < uri.length()) {
			// TODO: delete the ghost given by id
			String id = uri.substring(index + 1);
			return null;
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	// Unused
	@Override
	public String getFilePath() {
		return null;
	}
}
