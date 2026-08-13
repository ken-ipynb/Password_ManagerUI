package com.example.password_managerui;

public class PasswordModel {

    private String id;
    private String website;
    private String username;
    private String password;
    private String category;

    public PasswordModel() {
    }

    public PasswordModel(String id, String website, String username,
                         String password, String category) {

        this.id = id;
        this.website = website;
        this.username = username;
        this.password = password;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}