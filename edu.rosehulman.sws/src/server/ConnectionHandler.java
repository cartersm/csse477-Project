/*
 * ConnectionHandler.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 */

package server;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import protocol.plugin.AbstractPlugin;

/**
 * This class is responsible for handling a incoming request by creating a
 * {@link HttpRequest} object and sending the appropriate this.response be creating a
 * {@link Httpthis.response} object. It implements {@link Runnable} to be used in
 * multi-threaded environment.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class ConnectionHandler implements Runnable {
	private ServerWorker server;
	private HttpRequest request;
	private HttpResponse response;

	public ConnectionHandler(ServerWorker server, HttpRequest request) {
		this.server = server;
		this.request = request;
		this.response = null;
	}

	/**
	 * The entry point for connection handler. It first parses incoming request
	 * and creates a {@link HttpRequest} object, then it creates an appropriate
	 * {@link Httpthis.response} object and sends the this.response back to the client
	 * (web browser).
	 */
	public void run() {
		long start = System.currentTimeMillis();
		try {
			if (!request.getVersion().equalsIgnoreCase(Protocol.VERSION)) {
			} else {

				if (request.getUri().contains("favicon") || request.getUri().equals("/")) {
					this.response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
				} else {

					AbstractPlugin plugin = getPluginFromUri(request.getUri());
					if (plugin != null) {
						this.response = plugin.handle(request);
					} else {
						this.response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
					}
					this.server.addToAuditTrail(request);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (this.response == null) {
			this.response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}

		long end = System.currentTimeMillis();
		this.response.setServiceTime(end - start);
		this.response.setSocketHash(this.request.getSocketHash());
	}

	private AbstractPlugin getPluginFromUri(String uri) {
		final String pluginString;
		if (uri.contains("v1/")) {
			pluginString = uri.substring(uri.indexOf("v1/") + 3, uri.indexOf("/", 4));
		} else {
			pluginString = uri.substring(1, uri.indexOf("/", 1));
		}
		return this.server.getPlugin(pluginString);
	}
}
