package com.juntos.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable, Comparable<User>{
    private String userid;
    private String username;
    private String profileUrl;
    private int chatParticipations;
    private int personParticipations;
    private String token;
    private boolean online;

    public User(){}
    public User(String userid, String username, String profileUrl, int chatParticipations, int personParticipations){
        this.userid = userid;
        this.username = username;
        this.profileUrl = profileUrl;
        this.chatParticipations = chatParticipations;
        this.personParticipations = personParticipations;
    }
    //construtor para permitir ao parcelable enviar o objeto a outra activity
    protected User(Parcel in){
        userid = in.readString();
        username = in.readString();
        profileUrl = in.readString();
        chatParticipations = in.readInt();
        personParticipations = in.readInt();
        token = in.readString();
        online = in.readInt() == 1; //no Parcel nao tem readBoolean
    }
    //construtor eigido pela implementacao Parceable
    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getUserid() { return userid; }
    public String getUsername() { return username; }
    public String getProfileUrl() { return profileUrl; }
    public int getChatParticipations() { return this.chatParticipations; }
    public int getPersonParticipations() { return this.personParticipations; }
    public String getToken() { return token; }
    public boolean isOnline() { return online; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userid);
        dest.writeString(username);
        dest.writeString(profileUrl);
        dest.writeInt(chatParticipations);
        dest.writeInt(personParticipations);
        dest.writeString(token);
        dest.writeInt(online ? 1 : 0); //se online = 1 -> escreve 1, senao escreve 0
    }

    @Override
    public int compareTo(User o) {
        if(this.chatParticipations > o.getChatParticipations())
            return -1;
        else if(this.chatParticipations < o.getChatParticipations())
            return 1;
        else if(this.personParticipations > o.getPersonParticipations())
            return -1;
        else if (this.personParticipations < o.getPersonParticipations())
            return 1;
        else
            return 0;
    }
}
