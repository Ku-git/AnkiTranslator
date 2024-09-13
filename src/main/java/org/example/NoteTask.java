package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class NoteTask implements Callable<Void> {

    private static final String ANKI_CONNECT_URL = "http://localhost:8765";

    private static final String TRANSLATE_API = "https://translation.googleapis.com/language/translate/v2";

    private static final HttpClient TRANSLATE_CLIENT = HttpClient.newHttpClient();

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private final JsonArray noteInfo;

    public NoteTask(JsonArray noteInfo) {
        this.noteInfo = noteInfo;
    }

    @Override
    public Void call() {

        for (int i = 0; i < noteInfo.size(); i++ ){
            JsonObject note = noteInfo.get(i).getAsJsonObject();
            JsonObject fields = note.getAsJsonObject("fields");

            String noteId = note.get("note").getAsString();

            String question = fields.getAsJsonObject("Text") == null? "":
                    fields.getAsJsonObject("Text").get("value").getAsString();
            String answer = fields.getAsJsonObject("Extra") == null? "":
                    fields.getAsJsonObject("Extra").get("value").getAsString();

            if(question.isEmpty() && answer.isEmpty()) {
                continue;
            }

            try {
                String translatedQues = translateProcess(question);
                String translatedAns = translateProcess(answer);

                JsonObject updateFields = new JsonObject();
                updateFields.addProperty("Text", translatedQues);
                updateFields.addProperty("Extra", translatedAns);

                updateNote(noteId, updateFields);
            } catch (RuntimeException | IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }

        executorService.shutdown();

        return null;
    }

    private String translateProcess(String text) throws IOException, InterruptedException {

        StringBuilder sb = new StringBuilder();
        String translatedText;
        if (text.length() <= 128) {
            translatedText = translate(text);
            sb.append(translatedText);
            return sb.toString();
        }

        List<String> spiltText = spiltText(text, 135);

        List<Callable<String>> translateTask = new ArrayList<>();
        for (String content: spiltText) {
            translateTask.add(new TranslateTask(content));
        }

        List<Future<String>> futures = executorService.invokeAll(translateTask);
        for (Future<String> future: futures) {
            try {
                sb.append(future.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        String result = sb.toString();
        if (result.contains("<img")) {
            result = result.replaceAll("。", ".");
            result = result.replaceAll("&quot;", "\"");
            result = result.replaceAll("&gt;", ">");
        }

        return result;
    }

    private static String translate(String text) throws IOException, InterruptedException {

        Map<String, String> params = Map.of(
                "q", text,
                "target", "zh-tw",
                "key", ""
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

    private static List<String> spiltText(String text, int maxLength) {

        List<String> result = new ArrayList<>();
        String[] sentences = text.split("(?<=\\.)\\s+(?!jpg)|(?=<[^>]+>)|(?<=</[^>]>)");
        StringBuilder currentPart = new StringBuilder();

        for (String sentence: sentences) {

            if (currentPart.length() + sentence.length() > maxLength) {
                result.add(currentPart.toString());
                currentPart = new StringBuilder();
            }
            if (sentence.length() > maxLength) {
                while (sentence.length() > maxLength) {

                    int splitIndex = findSplitIndex(sentence, maxLength);
                    result.add(sentence.substring(0, splitIndex).trim());
                    sentence = sentence.substring(splitIndex).trim(); // 剩余部分继续处理
                }
                result.add(sentence);
                continue;
            }

            currentPart.append(sentence);
        }

        if(!currentPart.isEmpty()) {
            result.add(currentPart.toString());
        }
        result.removeAll(Arrays.asList("", null));

        return result;
    }

    private static int findSplitIndex(String sentence, int maxLength) {

        int splitIndex = sentence.lastIndexOf(',', maxLength);
        if (splitIndex == -1 || splitIndex == 0) {
            splitIndex = maxLength; // 如果找不到合适的拆分点，就直接在128字符处拆分
        }
        return splitIndex;
    }

    private void updateNote(String noteId, JsonObject fields) throws IOException, InterruptedException {

        JsonObject param = new JsonObject();
        param.addProperty("id", new BigDecimal(noteId));
        param.add("fields", fields);

        JsonObject note = new JsonObject();
        note.add("note", param);

        JsonObject json = new JsonObject();
        json.addProperty("action", "updateNoteFields");
        json.addProperty("version", 6);

        json.add("params", note);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANKI_CONNECT_URL))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }


}
