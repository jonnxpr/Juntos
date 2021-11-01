package com.juntos;

//classe apenas para guardar a chave da API Youtube
public class YoutubeConfig {

    private static final String API_KEY = "AIzaSyAgk5BiMm8__g3ewA_A8R_yKPrvLgiLtbY";
    public YoutubeConfig(){
    }
    public static String getApiKey(){
        return API_KEY;
    }
}
