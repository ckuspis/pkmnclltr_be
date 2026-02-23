package com.pokemon.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_id", columnList = "cardId"),
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_set_id", columnList = "setId"),
        @Index(name = "idx_rarity", columnList = "rarity"),
        @Index(name = "idx_user_id", columnList = "userId")
})
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cardId;

    @Column(nullable = false)
    private String name;

    private String setId;
    private String setName;
    private String series;
    private String rarity;
    private String types;
    private String subtypes;
    private String category;
    private Integer hp;
    private String imageSmall;
    private String imageLarge;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private String condition = "NM";

    private String notes;

    private Long userId;

    private Double priceLow;
    private Double priceMid;
    private Double priceHigh;
    private Double priceMarket;
    private String priceUpdatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSetId() { return setId; }
    public void setSetId(String setId) { this.setId = setId; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }
    public String getTypes() { return types; }
    public void setTypes(String types) { this.types = types; }
    public String getSubtypes() { return subtypes; }
    public void setSubtypes(String subtypes) { this.subtypes = subtypes; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getHp() { return hp; }
    public void setHp(Integer hp) { this.hp = hp; }
    public String getImageSmall() { return imageSmall; }
    public void setImageSmall(String imageSmall) { this.imageSmall = imageSmall; }
    public String getImageLarge() { return imageLarge; }
    public void setImageLarge(String imageLarge) { this.imageLarge = imageLarge; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Double getPriceLow() { return priceLow; }
    public void setPriceLow(Double priceLow) { this.priceLow = priceLow; }
    public Double getPriceMid() { return priceMid; }
    public void setPriceMid(Double priceMid) { this.priceMid = priceMid; }
    public Double getPriceHigh() { return priceHigh; }
    public void setPriceHigh(Double priceHigh) { this.priceHigh = priceHigh; }
    public Double getPriceMarket() { return priceMarket; }
    public void setPriceMarket(Double priceMarket) { this.priceMarket = priceMarket; }
    public String getPriceUpdatedAt() { return priceUpdatedAt; }
    public void setPriceUpdatedAt(String priceUpdatedAt) { this.priceUpdatedAt = priceUpdatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}