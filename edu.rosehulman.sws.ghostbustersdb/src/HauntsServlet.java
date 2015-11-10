import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

public class HauntsServlet implements IServlet {
	// indices of "/" characters in the expected URI
	private static final int SLASH_BEFORE_HAUNTS = 18;
	private static final int SLASH_AFTER_HAUNTS = 25;
	
	private static final String METHODS = 
			Protocol.GET + ", " + 
			Protocol.PUT + ", " + 
			Protocol.POST + ", " + 
			Protocol.DELETE;
	
	private static final String GET_ALL_COMMAND = 
			"SELECT * " + 
			"FROM " + DBHelper.HAUNTS_TABLE_NAME;
	private static final String GET_ONE_COMMAND = 
			"SELECT * "+ 
			"FROM " + DBHelper.HAUNTS_TABLE_NAME + " " +
			"WHERE ID=";
	private static final String PUT_COMMAND = 
			"UPDATE " + DBHelper.HAUNTS_TABLE_NAME + " " +
			"SET NAME=%s " + 
			"WHERE ID=%s";
	private static final String POST_COMMAND = 
			"INSERT INTO " + DBHelper.HAUNTS_TABLE_NAME + " " + 
			"(NAME) VALUES(%s)";
	private static final String DELETE_COMMAND = 
			"DELETE FROM " + DBHelper.HAUNTS_TABLE_NAME + " " +
			"WHERE ID=";

	private DBHelper dbHelper;

	public void setDbHelper(DBHelper dbHelper) {
		this.dbHelper = dbHelper;
	}
	
	@Override
	public HttpResponse doGet(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_BEFORE_HAUNTS || index + 1 == uri.length()) {
			try {
				File file = this.dbHelper.executeQuery(GET_ALL_COMMAND, rootDirectory);
				HttpResponse response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				response.put(Protocol.CONTENT_TYPE, Protocol.APPLICATION_JSON);
				file.deleteOnExit();
				return response;
			} catch (SQLException | IOException e) {
				e.printStackTrace();
				return HttpResponseFactory.create500InternalServerError(Protocol.CLOSE);
			}
		} else if (index == SLASH_AFTER_HAUNTS) {
			String id = uri.substring(index + 1);
			try {
				File file = dbHelper.executeQuery(GET_ONE_COMMAND + id, rootDirectory);
				HttpResponse response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				response.put(Protocol.CONTENT_TYPE, Protocol.APPLICATION_JSON);
				file.deleteOnExit();
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
		if (index == SLASH_AFTER_HAUNTS && index + 1 < uri.length()) {
			String id = uri.substring(index + 1);
			try {
				JsonParser parser = new JsonParser();
				JsonObject o = (JsonObject) parser.parse(new String(request.getBody()));
				String command = String.format(PUT_COMMAND, o.get("name"), id);
				boolean dbResponse = dbHelper.executeUpdate(command);
				if (!dbResponse) {
					return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
				}
				return HttpResponseFactory.create204NoContent(Protocol.CLOSE);
			} catch (SQLException e) {
				e.printStackTrace();
				return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
			}
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doPost(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_BEFORE_HAUNTS || index + 1 == uri.length()) {
			try {
				JsonParser parser = new JsonParser();
				JsonObject o = (JsonObject) parser.parse(new String(request.getBody()));
				String command = String.format(POST_COMMAND, o.get("name"));
				boolean dbResponse = dbHelper.executeUpdate(command);
				if (!dbResponse) {
					return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
				}
				return HttpResponseFactory.create201Created(Protocol.CLOSE);
			} catch (SQLException e) {
				e.printStackTrace();
				return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
			}
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doDelete(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_AFTER_HAUNTS && index + 1 < uri.length()) {
			String id = uri.substring(index + 1);
			try {
				String command = DELETE_COMMAND + id;
				boolean dbResponse = dbHelper.executeUpdate(command);
				if (!dbResponse) {
					return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
				}
				return HttpResponseFactory.create204NoContent(Protocol.CLOSE);
			} catch (SQLException e) {
				e.printStackTrace();
				return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
			}
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}
	
	@Override
	public HttpResponse doOptions(HttpRequest request, String rootDirectory) {
		HttpResponse response = HttpResponseFactory.create204NoContent(Protocol.CLOSE);
		response.put(Protocol.ACCESS_CONTROL_ALLOW_ORIGIN, request.getHeader().get("origin"));
		response.put(Protocol.ACCESS_CONTROL_ALLOW_METHODS, METHODS);
		response.put(Protocol.ACCESS_CONTROL_ALLOW_HEADERS, request.getHeader().get("access-control-request-headers"));
		return response;
	}

	// Unused
	@Override
	public String getFilePath() {
		return null;
	}
}
