package com.juntos.model;

public class Video {
    private String videoId;
    private String videoName;
    private String link;
    private String senderName;
    private long timestamp;

    public Video(){}

    public Video(String videoId, String name, String link, String senderName, long timestamp){
        if(name.trim().equals("")){
            videoName = "Not available";
        }
        this.videoId = videoId;
        this.videoName = name;
        this.link = link;
        this.senderName = senderName;
        this.timestamp = timestamp;
    }

    public String getVideoId() { return videoId; }

    public String getVideoName() {
        return videoName;
    }

    public String getLink() {
        return link;
    }

    public String getSenderName() { return senderName; }

    public long getTimestamp() { return timestamp; }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
