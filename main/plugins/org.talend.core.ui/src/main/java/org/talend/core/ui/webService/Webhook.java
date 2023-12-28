// ============================================================================
//
// Copyright (C) 2022-2023 Talaxie Inc. - www.deilink.fr
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talaxie SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.ui.webService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.HttpEntity;
// import org.talend.core.prefs.ITalendCorePrefConstants;
// import org.talend.core.ui.CoreUIPlugin;
// import org.talend.utils.json.JSONObject;

public class Webhook {

    public Webhook() {
    }

    public String sendFile(String fileLocation) {
        String serviceUrl = ""; // CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_HOST) + "/api/Livraison/DeployFile"; // http://localhost:9010/api/Livraison/DeployFile
        String finalToken = ""; // CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER); // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOjEsIklkZW50aWZpYW50IjoiYWRtaW4iLCJQc2V1ZG8iOiJBZG1pbiIsIlJvbGVJZCI6MSwiQWNjZXNzR3JvdXBJRCI6MCwiaWF0IjoxNjk5OTUyODc5fQ.lVK7WoyXE8t_gCYZRrfAXku6R6g_Wu18SI0QPTKjJTI
        serviceUrl = "http://localhost:9010/api/Livraison/DeployFile";
        finalToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOjEsIklkZW50aWZpYW50IjoiYWRtaW4iLCJQc2V1ZG8iOiJBZG1pbiIsIlJvbGVJZCI6MSwiQWNjZXNzR3JvdXBJRCI6MCwiaWF0IjoxNjk5OTUyODc5fQ.lVK7WoyXE8t_gCYZRrfAXku6R6g_Wu18SI0QPTKjJTI";

        try {
            HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addPart("file", new FileBody(new File(fileLocation)))
                .build();
            Pipe pipe = Pipe.open();
            new Thread(() -> {
                try (OutputStream outputStream = Channels.newOutputStream(pipe.sink())) {
                    httpEntity.writeTo(outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(new URI(serviceUrl))
                .setHeader("Authorization", "Bearer " + finalToken)
                .header("Content-Type", httpEntity.getContentType().getValue())
                .POST(BodyPublishers.ofInputStream(() -> Channels.newInputStream(pipe.source()))).build();
            HttpResponse<String> responseBody = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println(responseBody.body());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String postTest() {
        String serviceUrl = ""; // CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_HOST); // "http://localhost:9010/test/call";
        String finalToken = ""; // CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER);
        serviceUrl = "http://localhost:9010/api/Test/debug";
        finalToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOjEsIklkZW50aWZpYW50IjoiYWRtaW4iLCJQc2V1ZG8iOiJBZG1pbiIsIlJvbGVJZCI6MSwiQWNjZXNzR3JvdXBJRCI6MCwiaWF0IjoxNjk5OTUyODc5fQ.lVK7WoyXE8t_gCYZRrfAXku6R6g_Wu18SI0QPTKjJTI";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serviceUrl))
            .setHeader("Content-Type","application/json")
            .setHeader("Authorization", "Bearer " + finalToken)
            .POST(HttpRequest.BodyPublishers.ofString("{\"test\": \"123456azerty\"}"))
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("postTest : " + response.statusCode());
            // JSONObject responseBodyJson = new JSONObject(response.body());
            // if (responseBodyJson.has("data")) {
            //     result = responseBodyJson.getString("data");
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String test() {
      String message = "Confirm message V7 :\n";
      // message += "WEBHOOK_HOST: " + CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_HOST) + "\n";
      // message += "WEBHOOK_BEARER: " + CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER) + "\n";

      return message;
    }
}
