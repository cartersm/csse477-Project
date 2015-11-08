package edu.rosehulman.sws.ghostbustersdb;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

public class HauntsServlet implements IServlet {
	// indices of "/" characters in the expected URI
	private static final int SLASH_BEFORE_HAUNTS = 18;
	private static final int SLASH_AFTER_HAUNTS = 24;

	@Override
	public HttpResponse doGet(HttpRequest request, String rootDirectory) {
		String uri = request.getUri();
		int index = uri.lastIndexOf("/");
		if (index == SLASH_BEFORE_HAUNTS || index + 1 == uri.length()) {
			// TODO: return a list of haunts
			return null;
		} else if (index == SLASH_AFTER_HAUNTS) {
			// TODO: return the haunt given by id
			// consider how we will handle adding new ghosts 
			//     (might require a real DB to keep things up-to-date), 
			//     or, just remove that feature.
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
