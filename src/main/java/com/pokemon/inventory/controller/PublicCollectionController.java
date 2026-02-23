package com.pokemon.inventory.controller;

import com.pokemon.inventory.model.User;
import com.pokemon.inventory.repository.UserRepository;
import com.pokemon.inventory.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/u/{username}")
public class PublicCollectionController {

    private final UserRepository userRepo;
    private final CardService cardService;

    public PublicCollectionController(UserRepository userRepo, CardService cardService) {
        this.userRepo = userRepo;
        this.cardService = cardService;
    }

    @GetMapping("/collection")
    public ResponseEntity<?> getPublicCollection(
            @PathVariable String username,
            @RequestParam(required = false) String set,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {
        return userRepo.findByUsername(username.toLowerCase())
                .map(user -> {
                    var cards = cardService.getCollection(user.getId(), set, rarity, category, type, q, sort, order);
                    return ResponseEntity.ok((Object) Map.of(
                            "cards", cards,
                            "total", cards.size(),
                            "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getPublicStats(@PathVariable String username) {
        return userRepo.findByUsername(username.toLowerCase())
                .map(user -> {
                    var stats = cardService.getStats(user.getId());
                    stats.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
                    return ResponseEntity.ok((Object) stats);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
