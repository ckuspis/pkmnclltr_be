package com.pokemon.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ClaudeVisionService {

    @Value("${claude.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClaudeVisionService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Send an image to Claude and ask it to identify the Pokémon card.
     * Returns a JSON object with: cardName, setName (if readable), and any other details.
     */
    public JsonNode identifyCard(String base64Image, String mediaType) {
        try {
            // Build the request body
            ObjectNode body = mapper.createObjectNode();
            body.put("model", "claude-sonnet-4-20250514");
            body.put("max_tokens", 300);

            // System prompt
            ArrayNode systemArr = mapper.createArrayNode();
            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("type", "text");
            systemMsg.put("text",
                    "You are a Pokémon card identifier. Look at the image and extract the card details. " +
                            "Respond ONLY with a JSON object (no markdown, no backticks) containing: " +
                            "\"cardName\" (the Pokémon or card name), " +
                            "\"setName\" (the set name if visible, or null), " +
                            "\"cardNumber\" (the card number if visible, like \"025/182\", or null), " +
                            "\"rarity\" (if visible, or null). " +
                            "Example: {\"cardName\": \"Charizard\", \"setName\": \"Base Set\", \"cardNumber\": \"4/102\", \"rarity\": \"Rare Holo\"}"
            );
            systemArr.add(systemMsg);
            body.set("system", systemArr);

            // Messages with image
            ArrayNode messages = mapper.createArrayNode();
            ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");

            ArrayNode content = mapper.createArrayNode();

            // Image block
            ObjectNode imageBlock = mapper.createObjectNode();
            imageBlock.put("type", "image");
            ObjectNode source = mapper.createObjectNode();
            source.put("type", "base64");
            source.put("media_type", mediaType);
            source.put("data", base64Image);
            imageBlock.set("source", source);
            content.add(imageBlock);

            // Text block
            ObjectNode textBlock = mapper.createObjectNode();
            textBlock.put("type", "text");
            textBlock.put("text", "What Pokémon card is this? Return only the JSON object.");
            content.add(textBlock);

            userMsg.set("content", content);
            messages.add(userMsg);
            body.set("messages", messages);

            // Make the request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.anthropic.com/v1/messages",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Parse Claude's response
            JsonNode responseJson = mapper.readTree(response.getBody());
            JsonNode contentArray = responseJson.get("content");

            if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                String text = contentArray.get(0).get("text").asText();
                // Clean up any markdown formatting just in case
                text = text.replace("```json", "").replace("```", "").trim();
                return mapper.readTree(text);
            }

            return mapper.createObjectNode().put("error", "No response from Claude");

        } catch (Exception e) {
            e.printStackTrace();
            ObjectNode error = mapper.createObjectNode();
            error.put("error", "Failed to identify card: " + e.getMessage());
            return error;
        }
    }
}