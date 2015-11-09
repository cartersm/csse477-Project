import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

public class HauntsServlet implements IServlet {
	// indices of "/" characters in the expected URI
	private static final int SLASH_BEFORE_HAUNTS = 18;
	private static final int SLASH_AFTER_HAUNTS = 24;
	private static final String JSON_HAUNT_ID_KEY = "id";
	private static final String JSON_HAUNT_NAME_KEY = "name";
	private DBHelper dbHelper;

	public HauntsServlet(DBHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	@Override
	public HttpResponse doGet(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_BEFORE_HAUNTS || index + 1 == uri.length()) {
			// TODO: Return actual data rather than dummy data
			FileWriter file = null;
			HttpResponse response = null;
			try {
				JSONArray jsonArray = new JSONArray();
				
				JSONObject abandonedMill = new JSONObject();
				abandonedMill.put(JSON_HAUNT_ID_KEY, 111);
				abandonedMill.put(JSON_HAUNT_NAME_KEY, "The Abandoned Mill");
				
				JSONObject quaintManor = new JSONObject();
				quaintManor.put(JSON_HAUNT_ID_KEY, 222);
				quaintManor.put(JSON_HAUNT_NAME_KEY, "The Quaint Manor");
				
				jsonArray.put(abandonedMill);
				jsonArray.put(quaintManor);
				
				file = new FileWriter("response.json");
				file.write(jsonArray.toString());
				file.flush();
				file.close();
				response = HttpResponseFactory.create200OK(
						new File("response.json"), Protocol.CLOSE);
			} catch (JSONException e) {
				response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
			} catch (IOException e) {
				response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
			}			
			return response;
		} else if (index == SLASH_AFTER_HAUNTS) {
			// TODO: return the haunt given by id
			// consider how we will handle adding new ghosts
			// (might require a real DB to keep things up-to-date),
			// or, just remove that feature.
			String id = uri.substring(index + 1);
			return null;
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doPut(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_AFTER_HAUNTS && index + 1 < uri.length()) {
			// TODO: update the haunt given by id
			String id = uri.substring(index + 1);
			return null;
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doPost(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_BEFORE_HAUNTS || index + 1 == uri.length()) {
			// TODO: add a new haunt
			return null;
		}
		return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doDelete(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_AFTER_HAUNTS && index + 1 < uri.length()) {
			// TODO: delete the haunt given by id
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
