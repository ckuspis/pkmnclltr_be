package com.pokemon.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TcgdexApiService {

    @Value("${tcgdex.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public TcgdexApiService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    private JsonNode apiGet(String url) {
        ResponseEntity<String> response = restTemplate.getForEntity(java.net.URI.create(url), String.class);
        try {
            return mapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse TCGdex API response", e);
        }
    }

    public JsonNode searchCards(String q, String set, String rarity,
                                String type, String category,
                                int page, int pageSize) {

        System.out.println("=== SEARCH: q=" + q + " set=" + set + " rarity=" + rarity
                + " type=" + type + " category=" + category + " ===");

        // ─── Build query string using TCGDex filtering API ───────────
        StringBuilder url = new StringBuilder(baseUrl + "/cards?");
        List<String> params = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            params.add("name=" + URLEncoder.encode(q, StandardCharsets.UTF_8));
        }
        if (set != null && !set.isBlank()) {
            params.add("set.id=eq:" + URLEncoder.encode(set, StandardCharsets.UTF_8));
        }
        if (rarity != null && !rarity.isBlank()) {
            params.add("rarity=eq:" + URLEncoder.encode(rarity, StandardCharsets.UTF_8));
        }
        if (type != null && !type.isBlank()) {
            params.add("types=" + URLEncoder.encode(type, StandardCharsets.UTF_8));
        }
        if (category != null && !category.isBlank()) {
            params.add("category=eq:" + URLEncoder.encode(category, StandardCharsets.UTF_8));
        }

        // Pagination
        params.add("pagination:page=" + page);
        params.add("pagination:itemsPerPage=" + pageSize);

        url.append(String.join("&", params));

        System.out.println("=== API URL: " + url + " ===");

        // ─── Fetch filtered results from API ─────────────────────────
        JsonNode apiResult = apiGet(url.toString());
        ArrayNode cards = mapper.createArrayNode();

        if (apiResult != null && apiResult.isArray()) {
            for (JsonNode card : apiResult) {
                cards.add(card);
            }
        }

        System.out.println("=== FILTERED CARDS: " + cards.size() + " ===");

        // ─── Fetch full details for each card on this page ───────────
        ArrayNode pageData = mapper.createArrayNode();
        for (JsonNode card : cards) {
            try {
                String cardId = card.get("id").asText();
                JsonNode fullCard = apiGet(baseUrl + "/cards/" + cardId);
                pageData.add(fullCard);
            } catch (Exception e) {
                pageData.add(card);
            }
        }

        // ─── Build response ──────────────────────────────────────────
        ObjectNode result = mapper.createObjectNode();
        result.set("data", pageData);
        result.put("page", page);
        result.put("pageSize", pageSize);
        // Note: TCGDex doesn't return totalCount with pagination,
        // so we estimate based on whether a full page was returned
        result.put("totalCount", cards.size() < pageSize ? ((page - 1) * pageSize + cards.size()) : (page * pageSize + 1));
        return result;
    }

    /**
     * Get full card details by ID (e.g. "base1-4", "swsh1-1").
     */
    public JsonNode getCard(String cardId) {
        return apiGet(baseUrl + "/cards/" + cardId);
    }

    /**
     * List all available sets.
     */
    public JsonNode getSets() {
        return apiGet(baseUrl + "/sets");
    }

    /**
     * Get full details for a specific set.
     */
    public JsonNode getSet(String setId) {
        return apiGet(baseUrl + "/sets/" + setId);
    }

    /**
     * List all available card rarities.
     */
    public JsonNode getRarities() {
        return apiGet(baseUrl + "/rarities");
    }

    /**
     * List all Pokémon types.
     */
    public JsonNode getTypes() {
        return apiGet(baseUrl + "/types");
    }

    /**
     * List all card categories (Pokémon, Trainer, Energy).
     */
    public JsonNode getCategories() {
        return apiGet(baseUrl + "/categories");
    }
}