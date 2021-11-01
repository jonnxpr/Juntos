package com.juntos.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Room implements Parcelable {
    private String roomId;
    private String roomName;
    private String roomNickname;
    private String profileUrl;

    public Room(){}
    public Room(String roomId, String roomName, String roomNickname, String profileUrl){
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomNickname = roomNickname;
        this.profileUrl = profileUrl;
    }
    //construtor para permitir ao parcelable enviar o objeto a outra activity
    protected Room(Parcel in){
        roomId = in.readString();
        roomName = in.readString();
        roomNickname = in.readString();
        profileUrl = in.readString();
    }

    public static final Creator<Room> CREATOR = new Creator<Room>() {
        @Override
        public Room createFromParcel(Parcel in) {
            return new Room(in);
        }

        @Override
        public Room[] newArray(int size) {
            return new Room[size];
        }
    };

    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {this.roomId = roomId; }
    public String getRoomName() {
        return roomName;
    }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomNickname(){ return this.roomNickname; }
    public void setRoomNickname(String roomNickname) { this.roomNickname = roomNickname; }
    public String getProfileUrl() {
        return profileUrl;
    }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(roomId);
        dest.writeString(roomName);
        dest.writeString(roomNickname);
        dest.writeString(profileUrl);
    }
}

