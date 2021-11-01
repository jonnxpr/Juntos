package com.juntos.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.juntos.R;
import com.juntos.YoutubeConfig;

public class PlayerVideoActivity extends YouTubeBaseActivity {

    Button btnPlay, btnBack;
    YouTubePlayerView mYoutubePlayerView;
    YouTubePlayer.OnInitializedListener mOnInitializedListener;
    private String link, videoName;
    private String roomid, roomNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_video);

        btnPlay = (Button) findViewById(R.id.button_playYoutube);
        btnBack = (Button) findViewById(R.id.button_backYoutube);
        mYoutubePlayerView = (YouTubePlayerView) findViewById(R.id.youtubePlayer);

        Log.i("Teste", "onCreate: Starting");
        //pegar os dados do objeto enviado pelo Bundle
        //da activity chatroom
        Intent receiverIntent = getIntent();
        Bundle bundle = receiverIntent.getExtras();
        if(bundle != null){
            link = bundle.getString("link");
            videoName = bundle.getString("videoName");
            roomid = bundle.getString("roomId");
            roomNickname = bundle.getString("nickname");
        }

        mOnInitializedListener = new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                Log.i("Teste", "onClick: Done initializing.");
                youTubePlayer.loadVideo(link);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                Log.i("Teste", "onClick: Failed to initialize.");
            }
        };

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Teste", "onClick: Initializing Youtube Player...");
                mYoutubePlayerView.initialize(YoutubeConfig.getApiKey(), mOnInitializedListener);

            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentSender = new Intent(getApplicationContext(), MicroLearningActivity.class);
                Bundle parameters = new Bundle();

                parameters.putString("roomId", roomid);
                parameters.putString("nickname", roomNickname);

                intentSender.putExtras(parameters);
                //enviar os dados da sala para a nova activity
                startActivity(intentSender);
            }
        });
    }
}