package com.pokemon.inventory.dto;

public class AuthResponse {
    private String username;
    private String displayName;

    public AuthResponse(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
