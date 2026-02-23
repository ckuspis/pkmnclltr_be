package com.pokemon.inventory.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pokemon.inventory.dto.AddCardRequest;
import com.pokemon.inventory.dto.UpdateCardRequest;
import com.pokemon.inventory.model.Card;
import com.pokemon.inventory.service.CardService;
import com.pokemon.inventory.service.TcgdexApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pokemon.inventory.service.ClaudeVisionService;
import jakarta.servlet.http.HttpSession;

import java.util.*;

@RestController
@RequestMapping("/api")
public class CardController {

    private final CardService cardService;
    private final TcgdexApiService tcgdex;
    private final ClaudeVisionService claudeVision;

    public CardController(CardService cardService, TcgdexApiService tcgdex, ClaudeVisionService claudeVision) {
        this.cardService = cardService;
        this.tcgdex = tcgdex;
        this.claudeVision = claudeVision;
    }

    private Long getUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    // ── TCGdex Search Routes ──────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<?> searchCards(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String set,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            JsonNode result = tcgdex.searchCards(q, set, rarity, type, category, page, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to search cards"));
        }
    }

    @GetMapping("/search/{cardId}")
    public ResponseEntity<?> getCardFromApi(@PathVariable String cardId) {
        try {
            JsonNode result = tcgdex.getCard(cardId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch card"));
        }
    }

    @GetMapping("/sets")
    public ResponseEntity<?> getSets() {
        try {
            JsonNode result = tcgdex.getSets();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch sets"));
        }
    }

    @GetMapping("/sets/{setId}")
    public ResponseEntity<?> getSet(@PathVariable String setId) {
        try {
            JsonNode result = tcgdex.getSet(setId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch set"));
        }
    }

    @GetMapping("/rarities")
    public ResponseEntity<?> getRarities() {
        try {
            return ResponseEntity.ok(tcgdex.getRarities());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch rarities"));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<?> getTypes() {
        try {
            return ResponseEntity.ok(tcgdex.getTypes());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch types"));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            return ResponseEntity.ok(tcgdex.getCategories());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch categories"));
        }
    }

    // ── Collection CRUD Routes ────────────────────────────

    @PostMapping("/collection")
    public ResponseEntity<?> addCard(@RequestBody AddCardRequest request, HttpSession session) {
        try {
            Card card = cardService.addCard(getUserId(session), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Card added to collection",
                    "id", card.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to add card: " + e.getMessage()));
        }
    }

    @GetMapping("/collection")
    public ResponseEntity<?> getCollection(
            @RequestParam(required = false) String set,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            HttpSession session) {
        try {
            List<Card> cards = cardService.getCollection(getUserId(session), set, rarity, category, type, q, sort, order);
            return ResponseEntity.ok(Map.of("cards", cards, "total", cards.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch collection"));
        }
    }

    @GetMapping("/collection/stats")
    public ResponseEntity<?> getStats(HttpSession session) {
        try {
            return ResponseEntity.ok(cardService.getStats(getUserId(session)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch stats"));
        }
    }

    @PatchMapping("/collection/{id}")
    public ResponseEntity<?> updateCard(@PathVariable Long id, @RequestBody UpdateCardRequest request, HttpSession session) {
        try {
            cardService.updateCard(getUserId(session), id, request);
            return ResponseEntity.ok(Map.of("message", "Card updated"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Card not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update card"));
        }
    }

    @DeleteMapping("/collection/{id}")
    public ResponseEntity<?> deleteCard(@PathVariable Long id, HttpSession session) {
        try {
            cardService.deleteCard(getUserId(session), id);
            return ResponseEntity.ok(Map.of("message", "Card removed"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Card not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete card"));
        }
    }

    @PostMapping("/collection/refresh-prices")
    public ResponseEntity<?> refreshPrices(HttpSession session) {
        try {
            int updated = cardService.refreshPrices(getUserId(session));
            return ResponseEntity.ok(Map.of("message", "Prices refreshed", "updated", updated));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to refresh prices"));
        }
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanCard(@RequestBody Map<String, String> request) {
        try {
            String base64Image = request.get("image");
            String mediaType = request.getOrDefault("mediaType", "image/png");

            // Strip data URL prefix if present (e.g. "data:image/png;base64,")
            if (base64Image.contains(",")) {
                String[] parts = base64Image.split(",");
                if (parts[0].contains("image/")) {
                    mediaType = parts[0].split(":")[1].split(";")[0];
                }
                base64Image = parts[1];
            }

            var result = claudeVision.identifyCard(base64Image, mediaType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to scan card: " + e.getMessage()));
        }
    }
}
