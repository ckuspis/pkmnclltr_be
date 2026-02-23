package com.pokemon.inventory.repository;

import com.pokemon.inventory.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserIdAndCardId(Long userId, String cardId);
    List<Card> findByUserId(Long userId);
    Optional<Card> findByIdAndUserId(Long id, Long userId);

    List<Card> findByUserIdIsNull();

    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM Card c WHERE c.userId = :userId")
    int getTotalCards(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT c.setId) FROM Card c WHERE c.userId = :userId")
    int getTotalSets(@Param("userId") Long userId);

    @Query("SELECT c.setName, c.setId, COUNT(c), SUM(c.quantity) FROM Card c WHERE c.userId = :userId GROUP BY c.setId, c.setName ORDER BY SUM(c.quantity) DESC")
    List<Object[]> getStatsBySet(@Param("userId") Long userId);

    @Query("SELECT c.rarity, COUNT(c), SUM(c.quantity) FROM Card c WHERE c.userId = :userId AND c.rarity IS NOT NULL GROUP BY c.rarity ORDER BY SUM(c.quantity) DESC")
    List<Object[]> getStatsByRarity(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.priceMid * c.quantity), 0) FROM Card c WHERE c.userId = :userId AND c.priceMid IS NOT NULL")
    double getTotalValue(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}
