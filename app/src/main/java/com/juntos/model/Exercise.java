package com.juntos.model;

public class Exercise {
    private String exercId;
    private String description;
    private String imgUrl;
    private String sender;

    public Exercise(){}
    public Exercise(String exercId, String description, String imgUrl,String sender){
        this.exercId = exercId;
        this.description = description;
        this.imgUrl = imgUrl;
        this.sender = sender;
    }

    public String getExercId() {
        return exercId;
    }

    public void setExercId(String exercId) {
        this.exercId = exercId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
