import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

public class GhostsServlet implements IServlet {
	// indices of "/" characters in the expected URI
	private static final int SLASH_BEFORE_GHOSTS = 18;
	private static final int SLASH_AFTER_GHOSTS = 24;
	
	private static final String GET_ALL_COMMAND = 
			"SELECT *" + 
			" FROM " + DBHelper.GHOSTS_TABLE_NAME;
	private static final String GET_ONE_COMMAND = 
			"SELECT *"+ 
			" FROM " + DBHelper.GHOSTS_TABLE_NAME + 
			" WHERE ID=";

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
			try {
				File file = dbHelper.executeQuery(GET_ALL_COMMAND);
				HttpResponse response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				file.delete();
				return response;
			} catch (SQLException | IOException e) {
				e.printStackTrace();
				return HttpResponseFactory.create500InternalServerError(Protocol.CLOSE);
			}
		} else if (index == SLASH_AFTER_GHOSTS) {
			// TODO: return the ghost given by id
			String id = uri.substring(index + 1);
			try {
				File file = dbHelper.executeQuery(GET_ONE_COMMAND + id);
				HttpResponse response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				file.delete();
				return response;
			} catch (SQLException | IOException e) {
				e.printStackTrace();
				return HttpResponseFactory.create500InternalServerError(Protocol.CLOSE);
			}
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
