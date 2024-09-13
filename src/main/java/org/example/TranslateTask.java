package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.entity.ContentType;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class TranslateTask implements Callable<String> {

    private static final String TRANSLATE_API = "https://translation.googleapis.com/language/translate/v2";

    private static final String TRANS_LAN = "zh-tw";

    private static final String API_KEY = "";

    private final String text;

    private final HttpClient TRANSLATE_CLIENT;

    public TranslateTask(String text) {

        this.text = text;
        this.TRANSLATE_CLIENT = HttpClient.newHttpClient();
    }

    @Override
    public String call() throws Exception {

        Map<String, String> params = Map.of(
                "q", text,
                "target", TRANS_LAN,
                "key", API_KEY
        );

        String formParams = params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
                .uri(URI.create(TRANSLATE_API))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.ofString(formParams))
                .build();

        HttpResponse<String> response = TRANSLATE_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject data = responseJson.getAsJsonObject("data");
        JsonObject translation = data.getAsJsonArray("translations").get(0).getAsJsonObject();

        return translation.get("translatedText").getAsString();
    }

}
