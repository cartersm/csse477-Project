package protocol;

public interface IServlet {
	public HttpResponse doGet(HttpRequest request, String rootDirectory);

	public HttpResponse doPut(HttpRequest request, String rootDirectory);

	public HttpResponse doPost(HttpRequest request, String rootDirectory);

	public HttpResponse doDelete(HttpRequest request, String rootDirectory);

}
