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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class App {

    private static final String ANKI_CONNECT_URL = "http://localhost:8765";

    private static final ExecutorService noteExecutor = Executors.newFixedThreadPool(5);

    public static void main( String[] args ) throws IOException, InterruptedException {

        translateProcess();
    }


    private static void translateProcess() throws IOException, InterruptedException {

        JsonArray cards = getAllCards();
        System.out.println("cards size: " + cards.size());

        JsonArray noteInfo = null;
        List<NoteTask> noteTasks = new LinkedList<>();

        long start = System.currentTimeMillis();
        for (JsonElement ele: cards) {
            noteInfo = getNoteInfo(ele.getAsString());

            long initStart = System.nanoTime();
            noteTasks.add(new NoteTask(noteInfo));
            System.out.println("init note task cost: " + (System.nanoTime() - initStart) + "ns");
        }
        System.out.println((System.currentTimeMillis() - start) + "ms");

        List<Future<Void>> futures = noteExecutor.invokeAll(noteTasks);

        for (Future<Void> future: futures) {

            try {
                future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        noteExecutor.shutdown();
        System.out.println("done!");
    }

    private static JsonArray getAllCards() throws IOException, InterruptedException {

        JsonObject jsonObject = doActionByKey("findCards", "query", "");
        return jsonObject.getAsJsonArray("result");
    }


    private static JsonArray getNoteInfo(String cardId) throws IOException, InterruptedException {

        JsonObject jsonObject = getNoteInfo("cardsInfo", new BigDecimal(cardId));
        return jsonObject.getAsJsonArray("result");
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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANKI_CONNECT_URL))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        return JsonParser.parseString(response.body()).getAsJsonObject();
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

}
