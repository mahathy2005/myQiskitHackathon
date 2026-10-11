package com.sheshield.app;

public class YouTubeVideo {
    private String videoId;
    private String category;
    private String title;
    private String description;

    public YouTubeVideo(String videoId, String category, String title, String description) {
        this.videoId = videoId;
        this.category = category;
        this.title = title;
        this.description = description;
    }

    public String getVideoId() { return videoId; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}