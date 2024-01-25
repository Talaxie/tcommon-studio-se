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
import java.nio.file.Paths;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.HttpEntity;
import org.apache.log4j.Logger;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.ui.CoreUIPlugin;
import org.talend.utils.json.JSONArray;
import org.talend.utils.json.JSONException;
import org.talend.utils.json.JSONObject;


public class Webhook {

    private static final Logger LOGGER = Logger.getLogger(Webhook.class);

    public static Boolean backTest(String backHostField, String bearerField) {
        Boolean result = false;
        try {
            String serviceUrl = backHostField + "/api/Test/BackTest";
            String finalToken = bearerField;

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Talaxie serviceUrl:" + serviceUrl);
                LOGGER.info("Talaxie finalToken:" + finalToken);
            }

            JSONObject paramJson = new JSONObject();
            paramJson.put("app", "Talaxie");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl))
                .setHeader("Content-Type","application/json")
                .setHeader("Authorization", "Bearer " + finalToken)
                .POST(HttpRequest.BodyPublishers.ofString(paramJson.toString()))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject responseBodyJson = new JSONObject(response.body());
            if (
                responseBodyJson.has("success") &&
                responseBodyJson.getBoolean("success")
            ) {
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String sendFile(String fileLocation) {
        String result = "";
        try {
            String serviceUrl = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BACK_HOST) + "/upload";
            String finalToken = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER);
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
            result = responseBody.body();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static HashMap<String, String> JobArchiveCheck(String fileLocation) {
        HashMap<String, String> jobData = new HashMap<String, String>();
        try {
            String serviceUrl = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BACK_HOST) + "/api/Job/JobArchiveCheck";
            String finalToken = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER);
            String fileName = Paths.get(fileLocation).getFileName().toString();

            JSONObject paramJson = new JSONObject();
            paramJson.put("FileName", fileName);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl))
                .setHeader("Content-Type","application/json")
                .setHeader("Authorization", "Bearer " + finalToken)
                .POST(HttpRequest.BodyPublishers.ofString(paramJson.toString()))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject responseBodyJson = new JSONObject(response.body());
            if (
                responseBodyJson.has("success") &&
                responseBodyJson.getBoolean("success")
            ) {
                jobData.put("Projet", responseBodyJson.getString("Projet"));
                jobData.put("Sequenceur", responseBodyJson.getString("Sequenceur"));
                jobData.put("JobVersion", responseBodyJson.getString("JobVersion"));
                jobData.put("TalendVersion", responseBodyJson.getString("TalendVersion"));
                jobData.put("context", responseBodyJson.getString("context"));
                jobData.put("fileName", fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jobData;
    }

    public static Boolean Deploy(HashMap<String, String> jobData) {
        Boolean result = false;

        try {
            String serviceUrl = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BACK_HOST) + "/api/Livraison/Deploy";
            String finalToken = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER);

            JSONObject paramJson = new JSONObject();
            paramJson.put("FileName", jobData.get("fileName"));
            paramJson.put("Origin", "Temp");
            paramJson.put("Target", "ref_DEV");
            paramJson.put("Projet", jobData.get("Projet"));
            paramJson.put("Sequenceur", jobData.get("Sequenceur"));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl))
                .setHeader("Content-Type","application/json")
                .setHeader("Authorization", "Bearer " + finalToken)
                .POST(HttpRequest.BodyPublishers.ofString(paramJson.toString()))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject responseBodyJson = new JSONObject(response.body());
            if (
                responseBodyJson.has("success") &&
                responseBodyJson.getBoolean("success")
            ) {
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String getJobUrl(HashMap<String, String> jobData) {
        String jobURL = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_FRONT_HOST) + "/#/Job/View/";
        jobURL += "?Environnement=ref_DEV";
        jobURL += "&Projet=" + jobData.get("Projet");
        jobURL += "&Sequenceur=" + jobData.get("Sequenceur");
        jobURL += "&JobNom=" + jobData.get("Sequenceur");
        return jobURL;
    }

	public static List<HashMap<String, String>> codeReviewAnalyseText(String JobType, String JobName, String xmlText) {
        List<HashMap<String, String>> codereviewItems = new ArrayList<>();

		JSONObject paramJson = new JSONObject();
		try {
			paramJson.put("JobType", JobType);
			paramJson.put("JobName", JobName);
			paramJson.put("xmlText", xmlText);
		} catch (JSONException e) {
			e.printStackTrace();
		}

        String serviceUrl = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BACK_HOST) + "/api/Job/CodeReviewAnalyseText";
        String finalToken = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(serviceUrl))
			.setHeader("Content-Type","application/json")
			.setHeader("Authorization", "Bearer " + finalToken)
			.POST(HttpRequest.BodyPublishers.ofString(paramJson.toString()))
			.build();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JSONObject responseBodyJson = new JSONObject(response.body());
			if (
				responseBodyJson.has("success") &&
				responseBodyJson.getBoolean("success")
			) {
				JSONArray errorList = responseBodyJson.getJSONObject("data").getJSONArray("errorList");
				for (int i = 0; i < errorList.length(); i++) {
					JSONObject error = errorList.getJSONObject(i);
					JSONObject codeReviewRule = error.getJSONObject("CodeReviewRule");
					HashMap<String, String> codereviewItem = new HashMap<String, String>();
					codereviewItem.put("Type", String.valueOf(codeReviewRule.getInt("Type")));
					codereviewItem.put("Titre", codeReviewRule.getString("Titre"));
					codereviewItem.put("Description", codeReviewRule.getString("Description"));
					codereviewItem.put("Url", codeReviewRule.getString("Url"));
					if (error.has("UNIQUE_NAME")) {
						codereviewItem.put("UNIQUE_NAME", error.getString("UNIQUE_NAME"));
					} else {
						codereviewItem.put("UNIQUE_NAME", "");
					}
					if (error.has("componentName")) {
						codereviewItem.put("componentName", error.getString("componentName"));
					} else {
						codereviewItem.put("componentName", "");
					}
					codereviewItems.add(codereviewItem);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return codereviewItems;
	}

    public static String postTest() {
        String serviceUrl = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BACK_HOST) + "/api/Test/debug";
        String finalToken = CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER);
        // serviceUrl = "http://localhost:9010/api/Test/debug";
        // finalToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOjEsIklkZW50aWZpYW50IjoiYWRtaW4iLCJQc2V1ZG8iOiJBZG1pbiIsIlJvbGVJZCI6MSwiQWNjZXNzR3JvdXBJRCI6MCwiaWF0IjoxNjk5OTUyODc5fQ.lVK7WoyXE8t_gCYZRrfAXku6R6g_Wu18SI0QPTKjJTI";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serviceUrl))
            .setHeader("Content-Type","application/json")
            .setHeader("Authorization", "Bearer " + finalToken)
            .POST(HttpRequest.BodyPublishers.ofString("{\"test\": \"123456azerty\"}"))
            .build();
        String result = "";
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("postTest : " + response.statusCode());
            // result = response.body();
            JSONObject responseBodyJson = new JSONObject(response.body());
            if (responseBodyJson.has("data")) {
                result = responseBodyJson.getString("data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String test() {
      String message = "";
      message += "WEBHOOK_FRONT_HOST: " + CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_FRONT_HOST) + "\n";
      message += "WEBHOOK_BACK_HOST: " + CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BACK_HOST) + "\n";
      message += "WEBHOOK_BEARER: " + CoreUIPlugin.getDefault().getPreferenceStore().getString(ITalendCorePrefConstants.WEBHOOK_BEARER) + "\n";

      return message;
    }
}
