package com.example.geminialexa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;

@SpringBootApplication
@RestController
public class GeminiAlexaApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeminiAlexaApplication.class, args);
    }

    private final AtomicBoolean geminiMode = new AtomicBoolean(true);

    private final String GEMINI_API_KEY = "AIzaSyAWTEZxVIJ6gGNhmyhi7o59LHgWeqtZGv8";
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/alexa")
    public ResponseEntity<Object> handleAlexaRequest(@RequestBody JsonNode input) {
        try {
            String intentName = input.path("request").path("intent").path("name").asText();
            String responseText;

            switch (intentName) {
                case "SwitchModeIntent":
                    String mode = input.path("request").path("intent").path("slots").path("Mode").path("value").asText().toLowerCase();
                    if ("gemini".equals(mode)) {
                        geminiMode.set(true);
                        responseText = "Gemini mode activated.";
                    } else if ("alexa".equals(mode)) {
                        geminiMode.set(false);
                        responseText = "Alexa mode activated.";
                    } else {
                        responseText = "Sorry, I didn't understand that mode.";
                    }
                    break;

                case "GeminiTalkIntent":
                    String userInput = input.path("request").path("intent").path("slots").path("UserInput").path("value").asText();
                    if (geminiMode.get()) {
                        responseText = callGemini(userInput);
                    } else {
                        responseText = "You're in Alexa mode. Say 'switch to Gemini mode' to talk to Gemini.";
                    }
                    break;

                default:
                    responseText = "Unknown intent.";
            }

            ObjectNode response = mapper.createObjectNode();
            response.put("version", "1.0");
            ObjectNode outputSpeech = mapper.createObjectNode();
            outputSpeech.put("type", "PlainText");
            outputSpeech.put("text", responseText);

            ObjectNode responseBody = mapper.createObjectNode();
            responseBody.set("outputSpeech", outputSpeech);
            responseBody.put("shouldEndSession", false);

            ObjectNode finalResponse = mapper.createObjectNode();
            finalResponse.set("response", responseBody);
            finalResponse.put("version", "1.0");

            return ResponseEntity.ok().body(finalResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request.");
        }
    }

    private String callGemini(String prompt) {
        try {
            ObjectNode part = mapper.createObjectNode().put("text", prompt);
            ObjectNode content = mapper.createObjectNode().set("parts", mapper.createArrayNode().add(part));
            ObjectNode requestBody = mapper.createObjectNode().set("contents", mapper.createArrayNode().add(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
            JsonNode response = restTemplate.postForObject(GEMINI_URL, request, JsonNode.class);

            return response.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Gemini couldn't respond right now.";
        }
    }
}
