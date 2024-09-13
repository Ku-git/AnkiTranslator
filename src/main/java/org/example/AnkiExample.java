package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AnkiExample {

    private static final String ANKI_CONNECT_URL = "http://localhost:8765";

    public static void main(String[] args) throws IOException, InterruptedException {

        String response = connectToANKI();
        System.out.println(response);
    }

    private static String connectToANKI() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANKI_CONNECT_URL))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private static JsonArray getDeckNames() throws IOException, InterruptedException {

        JsonObject jsonObject = getValByKey("deckNames");
        System.out.println(jsonObject);
        return jsonObject.getAsJsonArray("result");
    }

    private static JsonArray getCardsInDeck(String deckName) throws IOException, InterruptedException {

        JsonObject jsonObject = getValByKey("findCards", "query", "deck:" + deckName);
        return jsonObject.getAsJsonArray("result");
    }

    private static JsonObject getValByKey(String action, String... params) throws IOException, InterruptedException {

        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty("version", 6);


        if (params.length > 0) {
            JsonObject paramsJson = new JsonObject();
            for (int i = 0; i < params.length; i += 2) {
                paramsJson.addProperty(params[i], params[i + 1]);
            }
            json.add("params", paramsJson);
        }

        System.out.println(json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANKI_CONNECT_URL))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

}
