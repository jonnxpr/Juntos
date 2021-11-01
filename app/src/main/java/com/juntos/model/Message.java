package com.juntos.model;

public class Message {

    private String text;
    private long timestamp;
    private String fromId;
    private User sender;
    private String toId;

    public String getText() { return this.text; }

    public long getTimestamp() { return timestamp; }

    public String getFromId() {
        if(sender != null)
            return sender.getUserid();
        else
            return fromId;
    }

    public User getSender() { return sender; }

    public String getToId() { return toId; }

    public void setText(String text) { this.text = text; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public void setFromId(String fromId) { this.fromId = fromId; }

    public void setSender(User sender) { this.sender = sender; }

    public void setToId(String toId) { this.toId = toId; }
}
