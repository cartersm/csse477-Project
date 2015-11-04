import java.io.File;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IServlet;
import protocol.Protocol;

public class HelloServlet implements IServlet {

	@Override
	public HttpResponse doGet(HttpRequest request, String rootDirectory) {
		HttpResponse response;
		String path = getFilePath();

		File file = new File(rootDirectory + path);
		
		if (file.exists()) {
			if (file.isDirectory()) {
				String location = rootDirectory + Protocol.SYSTEM_SEPARATOR + Protocol.DEFAULT_FILE;
				file = new File(location);
				if (file.exists()) {
					response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
				} else {
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
				}
			} else {
				response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
			}
		} else {
			response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
		}
		return response;
	}

	@Override
	public HttpResponse doPut(HttpRequest request, String rootDirectory) {
		return HttpResponseFactory.create505NotSupported(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doPost(HttpRequest request, String rootDirectory) {
		return HttpResponseFactory.create505NotSupported(Protocol.CLOSE);
	}

	@Override
	public HttpResponse doDelete(HttpRequest request, String rootDirectory) {
		return HttpResponseFactory.create505NotSupported(Protocol.CLOSE);
	}

	@Override
	public String getFilePath() {
		return Protocol.SYSTEM_SEPARATOR + "hello.json";
	}

}
