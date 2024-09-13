package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AnkiRefreshNoteId {

    private static final String ANKI_CONNECT_URL = "http://localhost:8765";

    public static void main(String[] args) throws Exception {

        addAll();

    }

    private static void addAll() throws Exception {

        JsonArray cards = getCardsInDeck("JAnkiUW II 中文");
        System.out.println("cards size: " + cards.size());

        JsonArray noteInfo = null;

        for(JsonElement ele: cards) {
            noteInfo = getNoteInfo(ele.getAsString());

            for (int i = 0; i < noteInfo.size(); i++) {
                JsonObject note = noteInfo.get(i).getAsJsonObject();
                JsonObject fields = note.getAsJsonObject("fields");

                // 创建新的笔记，放入新 deck 中
                JsonArray notes = new JsonArray();

                JsonObject newNote = new JsonObject();
                newNote.addProperty("deckName", "\"deck:JAnkiUW II 中文v2\"");  // 指定新的 deck 名称
                newNote.addProperty("modelName", "Basic");

                JsonObject newFields = new JsonObject();
                newFields.addProperty("Text", fields.get("Text").getAsJsonObject().get("value").getAsString());  // 翻译后的问题
                newFields.addProperty("Extra", fields.get("Extra").getAsJsonObject().get("value").getAsString());  // 翻译后的答案

                newNote.add("fields", newFields);

                JsonArray tags = new JsonArray();
                tags.add("translated");  // 添加标签，可以根据需要自定义
                newNote.add("tags", tags);

                notes.add(newNote);

                // 调用 addNotes API 添加到新的笔记
                addNotes(notes);
            }
        }

    }

    // 添加新笔记到指定的 deck
    public static void addNotes(JsonArray notes) throws Exception {
        JsonObject params = new JsonObject();
        params.add("notes", notes);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("action", "addNotes");
        requestBody.add("params", params);
        requestBody.addProperty("version", 6);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(ANKI_CONNECT_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }

    private static JsonArray getCardsInDeck(String deckName) throws IOException, InterruptedException {

        JsonObject jsonObject = doActionByKey("findCards", "query", "\"deck:JAnkiUW II 中文\"");
        return jsonObject.getAsJsonArray("result");
    }

    private static JsonArray getNoteInfo(String cardId) throws IOException, InterruptedException {

        JsonObject jsonObject = getNoteInfo("cardsInfo", new BigDecimal(cardId));
        return jsonObject.getAsJsonArray("result");
    }

    private static JsonObject getNoteInfo(String action, BigDecimal id) throws IOException, InterruptedException {

        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty("version", 6);

        JsonObject paramsJson = new JsonObject();
        JsonArray idArray = new JsonArray();
        idArray.add(id);
        paramsJson.add("cards", idArray);

        json.add("params", paramsJson);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANKI_CONNECT_URL))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static JsonObject doActionByKey(String action, String... params) throws IOException, InterruptedException {

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

//        System.out.println(json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANKI_CONNECT_URL))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
