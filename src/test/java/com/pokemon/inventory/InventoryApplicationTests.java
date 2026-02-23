package com.pokemon.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryApplicationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper mapper = new ObjectMapper();

    private static Long savedCardId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ── TCGdex API Search Tests ──────────────────────────

    @Test
    @Order(1)
    @DisplayName("Search for Pikachu cards")
    void searchPikachu() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/search")
                        .param("q", "pikachu")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalCount").exists())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        System.out.println("Found " + json.get("totalCount").asInt() + " Pikachu cards");
        if (json.get("data").size() > 0) {
            JsonNode first = json.get("data").get(0);
            System.out.println("First result: " + first.get("name").asText()
                    + " (" + first.get("id").asText() + ")");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Get a specific card by ID (Base Set Pikachu)")
    void getCardById() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/search/base1-58"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pikachu"))
                .andReturn();

        JsonNode card = mapper.readTree(result.getResponse().getContentAsString());
        System.out.println("Card: " + card.get("name").asText());
        System.out.println("Category: " + (card.has("category") ? card.get("category").asText() : "N/A"));
        System.out.println("Rarity: " + (card.has("rarity") ? card.get("rarity").asText() : "N/A"));
        System.out.println("HP: " + (card.has("hp") ? card.get("hp").asInt() : "N/A"));
        if (card.has("image")) {
            System.out.println("Image: " + card.get("image").asText() + "/high.webp");
        }
    }

    @Test
    @Order(3)
    @DisplayName("List all available sets")
    void listSets() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sets"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        int setCount = json.size();
        System.out.println("Total sets available: " + setCount);
        if (setCount > 0) {
            System.out.println("First set: " + json.get(0).get("name").asText()
                    + " (" + json.get(0).get("id").asText() + ")");
        }
    }

    @Test
    @Order(4)
    @DisplayName("List available rarities")
    void listRarities() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rarities"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        System.out.println("Available rarities: " + json);
    }

    @Test
    @Order(5)
    @DisplayName("List available types")
    void listTypes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/types"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        System.out.println("Available types: " + json);
    }

    // ── Collection CRUD Tests ────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Add Base Set Charizard to collection")
    void addCardToCollection() throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of(
                "cardId", "base1-4",
                "quantity", 1,
                "condition", "NM",
                "notes", "Test card - Base Set Charizard"
        ));

        MvcResult result = mockMvc.perform(post("/api/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Card added to collection"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        savedCardId = json.get("id").asLong();
        System.out.println("Added card to collection with ID: " + savedCardId);
    }

    @Test
    @Order(7)
    @DisplayName("Add Base Set Pikachu to collection")
    void addPikachuToCollection() throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of(
                "cardId", "base1-58",
                "quantity", 3,
                "condition", "LP",
                "notes", "Base Set Pikachu x3"
        ));

        mockMvc.perform(post("/api/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Card added to collection"));

        System.out.println("Added Pikachu to collection");
    }

    @Test
    @Order(8)
    @DisplayName("List collection and verify cards are present")
    void listCollection() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/collection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.cards").isArray())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        int total = json.get("total").asInt();
        System.out.println("Collection has " + total + " unique card(s):");

        for (JsonNode card : json.get("cards")) {
            System.out.println("  - " + card.get("name").asText()
                    + " | Set: " + card.get("setName").asText()
                    + " | Qty: " + card.get("quantity").asInt()
                    + " | Condition: " + card.get("condition").asText());
        }

        Assertions.assertTrue(total >= 2, "Should have at least 2 cards");
    }

    @Test
    @Order(9)
    @DisplayName("Filter collection by name")
    void filterCollectionByName() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/collection").param("q", "charizard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        System.out.println("Filtered for 'charizard': " + json.get("total").asInt() + " result(s)");
    }

    @Test
    @Order(10)
    @DisplayName("Get collection stats")
    void getCollectionStats() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/collection/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unique_cards").exists())
                .andExpect(jsonPath("$.total_cards").exists())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        System.out.println("=== Collection Stats ===");
        System.out.println("Unique cards: " + json.get("unique_cards").asInt());
        System.out.println("Total cards: " + json.get("total_cards").asInt());
        System.out.println("Total sets: " + json.get("total_sets").asInt());
    }

    @Test
    @Order(11)
    @DisplayName("Update card quantity and condition")
    void updateCard() throws Exception {
        Assertions.assertNotNull(savedCardId, "savedCardId should be set from addCard test");

        String body = mapper.writeValueAsString(java.util.Map.of(
                "quantity", 2,
                "condition", "MP",
                "notes", "Updated - found another copy"
        ));

        mockMvc.perform(patch("/api/collection/" + savedCardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card updated"));

        // Verify the update
        MvcResult result = mockMvc.perform(get("/api/collection").param("q", "charizard"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        JsonNode card = json.get("cards").get(0);
        System.out.println("Updated: " + card.get("name").asText()
                + " | Qty: " + card.get("quantity").asInt()
                + " | Condition: " + card.get("condition").asText());

        Assertions.assertEquals(2, card.get("quantity").asInt());
        Assertions.assertEquals("MP", card.get("condition").asText());
    }

    @Test
    @Order(12)
    @DisplayName("Delete a card from collection")
    void deleteCard() throws Exception {
        Assertions.assertNotNull(savedCardId, "savedCardId should be set from addCard test");

        mockMvc.perform(delete("/api/collection/" + savedCardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card removed"));

        // Verify deletion
        mockMvc.perform(delete("/api/collection/" + savedCardId))
                .andExpect(status().isNotFound());

        System.out.println("Card " + savedCardId + " deleted and verified gone");
    }

    @Test
    @Order(13)
    @DisplayName("Delete returns 404 for non-existent card")
    void deleteNonExistentCard() throws Exception {
        mockMvc.perform(delete("/api/collection/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Card not found"));
    }
}