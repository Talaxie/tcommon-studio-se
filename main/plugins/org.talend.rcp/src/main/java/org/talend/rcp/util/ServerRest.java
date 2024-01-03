// ============================================================================
//
// Copyright (C) 2006-2023 Talaxie Inc. - www.deilink.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talaxie SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.rcp.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ServerRest {

  public static void startServer(String port) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(port)), 0);
    server.createContext("/hello", new HelloHandler());
    server.setExecutor(null);
    server.start();
    System.out.println("Server started. Listening on port " + port);
  }

  static class HelloHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      // Handling a GET request
      if ("GET".equals(exchange.getRequestMethod())) {
        String response = "Hello, Simple RESTful Service!";
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(response.getBytes());
        }
      } else {
        // Method not allowed
        exchange.sendResponseHeaders(405, -1);
      }
    }
  }

}
