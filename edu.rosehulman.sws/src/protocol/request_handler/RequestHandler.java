package protocol.request_handler;

import protocol.HttpRequest;
import protocol.HttpResponse;
import server.Server;

public interface RequestHandler {
	public HttpResponse handle(HttpRequest request, Server server);
}
