package com.pokemon.inventory.service;

import com.pokemon.inventory.model.Card;
import com.pokemon.inventory.model.User;
import com.pokemon.inventory.repository.CardRepository;
import com.pokemon.inventory.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataMigrationService implements CommandLineRunner {

    private final CardRepository cardRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataMigrationService(CardRepository cardRepo, UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.cardRepo = cardRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<Card> orphans = cardRepo.findByUserIdIsNull();
        if (orphans.isEmpty()) return;

        // Create or find default "chris" user
        User chris = userRepo.findByUsername("chris").orElseGet(() -> {
            User user = new User();
            user.setUsername("chris");
            user.setDisplayName("Chris");
            user.setPassword(passwordEncoder.encode("pokemon"));
            return userRepo.save(user);
        });

        for (Card card : orphans) {
            card.setUserId(chris.getId());
            cardRepo.save(card);
        }

        System.out.println("Migrated " + orphans.size() + " orphan cards to user 'chris'");
    }
}
