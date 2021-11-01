package com.juntos.model;

public class Notification extends Message {

    private String fromName; //notificação de msg vinda de quem

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
