package com.pokemon.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pokemon.inventory.dto.AddCardRequest;
import com.pokemon.inventory.dto.UpdateCardRequest;
import com.pokemon.inventory.model.Card;
import com.pokemon.inventory.repository.CardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CardService {

    private final CardRepository repo;
    private final TcgdexApiService tcgdex;

    public CardService(CardRepository repo, TcgdexApiService tcgdex) {
        this.repo = repo;
        this.tcgdex = tcgdex;
    }

    public Card addCard(Long userId, AddCardRequest request) {
        JsonNode data = tcgdex.getCard(request.getCardId());
        Card card = mapToEntity(data);
        card.setUserId(userId);
        card.setQuantity(request.getQuantity());
        card.setCondition(request.getCondition());
        card.setNotes(request.getNotes());
        return repo.save(card);
    }

    public List<Card> getCollection(Long userId, String set, String rarity, String category,
                                    String type, String q, String sort, String order) {
        List<Card> cards = repo.findByUserId(userId);
        return cards.stream()
                .filter(c -> set == null || set.equals(c.getSetId()))
                .filter(c -> rarity == null || rarity.equals(c.getRarity()))
                .filter(c -> category == null || category.equals(c.getCategory()))
                .filter(c -> type == null || (c.getTypes() != null && c.getTypes().contains(type)))
                .filter(c -> q == null || c.getName().toLowerCase().contains(q.toLowerCase()))
                .sorted(getComparator(sort, order))
                .toList();
    }

    public Map<String, Object> getStats(Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("unique_cards", repo.countByUserId(userId));
        stats.put("total_cards", repo.getTotalCards(userId));
        stats.put("total_sets", repo.getTotalSets(userId));
        stats.put("total_value", repo.getTotalValue(userId));

        List<Map<String, Object>> bySet = new ArrayList<>();
        for (Object[] row : repo.getStatsBySet(userId)) {
            bySet.add(Map.of(
                    "set_name", row[0] != null ? row[0] : "Unknown",
                    "set_id", row[1] != null ? row[1] : "",
                    "cards", row[2],
                    "total", row[3]
            ));
        }
        stats.put("bySet", bySet);

        List<Map<String, Object>> byRarity = new ArrayList<>();
        for (Object[] row : repo.getStatsByRarity(userId)) {
            byRarity.add(Map.of(
                    "rarity", row[0] != null ? row[0] : "Unknown",
                    "cards", row[1],
                    "total", row[2]
            ));
        }
        stats.put("byRarity", byRarity);
        return stats;
    }

    public Card updateCard(Long userId, Long id, UpdateCardRequest request) {
        Card card = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Card not found"));
        if (request.getQuantity() != null) card.setQuantity(request.getQuantity());
        if (request.getCondition() != null) card.setCondition(request.getCondition());
        if (request.getNotes() != null) card.setNotes(request.getNotes());
        card.setUpdatedAt(LocalDateTime.now());
        return repo.save(card);
    }

    public void deleteCard(Long userId, Long id) {
        Card card = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Card not found"));
        repo.delete(card);
    }

    public int refreshPrices(Long userId) {
        List<Card> cards = repo.findByUserId(userId);
        int updated = 0;
        for (Card card : cards) {
            try {
                JsonNode data = tcgdex.getCard(card.getCardId());
                mapPricing(data, card);
                card.setUpdatedAt(LocalDateTime.now());
                repo.save(card);
                updated++;
            } catch (Exception e) {
                // skip cards that fail
            }
        }
        return updated;
    }

    // ── Map TCGdex response to Card entity ───────────────

    private Card mapToEntity(JsonNode data) {
        Card card = new Card();
        card.setCardId(textOrNull(data, "id"));
        card.setName(textOrNull(data, "name"));
        card.setCategory(textOrNull(data, "category"));
        card.setRarity(textOrNull(data, "rarity"));
        card.setHp(data.has("hp") ? data.get("hp").asInt() : null);
        card.setTypes(data.has("types") ? data.get("types").toString() : "[]");
        card.setSubtypes(data.has("stage") ? "[\"" + data.get("stage").asText() + "\"]" : "[]");

        // Set info
        JsonNode set = data.get("set");
        if (set != null) {
            card.setSetId(textOrNull(set, "id"));
            card.setSetName(textOrNull(set, "name"));
            JsonNode series = set.get("series");
            if (series != null) {
                card.setSeries(series.isObject() ? textOrNull(series, "name") : series.asText());
            }
        }

        // Images: TCGdex gives a base URL, append quality suffix
        String imageBase = textOrNull(data, "image");
        if (imageBase != null) {
            card.setImageSmall(imageBase + "/low.webp");
            card.setImageLarge(imageBase + "/high.webp");
        }

        mapPricing(data, card);
        return card;
    }

    private void mapPricing(JsonNode data, Card card) {
        JsonNode pricing = data.get("pricing");
        if (pricing == null || pricing.isNull()) return;
        JsonNode tcgplayer = pricing.get("tcgplayer");
        if (tcgplayer == null || tcgplayer.isNull()) return;

        // Prefer normal, fall back to holo, then reverse
        JsonNode variant = null;
        for (String key : new String[]{"normal", "holofoil", "reverse-holofoil"}) {
            if (tcgplayer.has(key) && !tcgplayer.get(key).isNull()) {
                variant = tcgplayer.get(key);
                break;
            }
        }
        if (variant == null) return;

        card.setPriceLow(doubleOrNull(variant, "lowPrice"));
        card.setPriceMid(doubleOrNull(variant, "midPrice"));
        card.setPriceHigh(doubleOrNull(variant, "highPrice"));
        card.setPriceMarket(doubleOrNull(variant, "marketPrice"));

        if (tcgplayer.has("updated") && !tcgplayer.get("updated").isNull()) {
            card.setPriceUpdatedAt(tcgplayer.get("updated").asText());
        }
    }

    private Double doubleOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : null;
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Comparator<Card> getComparator(String sort, String order) {
        Comparator<Card> comp;
        if ("name".equals(sort)) comp = Comparator.comparing(Card::getName, Comparator.nullsLast(String::compareToIgnoreCase));
        else if ("set_name".equals(sort)) comp = Comparator.comparing(Card::getSetName, Comparator.nullsLast(String::compareToIgnoreCase));
        else if ("rarity".equals(sort)) comp = Comparator.comparing(Card::getRarity, Comparator.nullsLast(String::compareToIgnoreCase));
        else if ("quantity".equals(sort)) comp = Comparator.comparingInt(Card::getQuantity);
        else if ("price".equals(sort)) comp = Comparator.comparing(Card::getPriceMid, Comparator.nullsLast(Double::compareTo));
        else comp = Comparator.comparing(Card::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
        return "asc".equalsIgnoreCase(order) ? comp : comp.reversed();
    }
}
