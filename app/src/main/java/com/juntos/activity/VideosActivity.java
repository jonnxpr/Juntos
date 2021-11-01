package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.juntos.R;
import com.juntos.model.User;
import com.juntos.model.Video;

import java.util.UUID;

public class VideosActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST = 1;
    private User me;
    private String roomId;
    private String roomNickname;
    private Button btnChoose, btnUpload, btnList;
    private VideoView videoView;
    private Uri videoUri;
    MediaController mediaController;
    private EditText edtVideoName;
    private ProgressBar progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos);

        btnChoose = findViewById(R.id.button_choose);
        btnUpload = findViewById(R.id.button_upload);
        btnList = findViewById(R.id.button_list);
        videoView = findViewById(R.id.Video_view);
        progressBar = findViewById(R.id.progress_bar);
        edtVideoName = findViewById(R.id.editText_videoName);

        //pegar os dados do objeto enviado pelo Bundle
        //da activity chatroom
        Intent receiverIntent = getIntent();
        Bundle bundle = receiverIntent.getExtras();
        if(bundle != null){
            roomNickname = bundle.getString("nickname");
            roomId = bundle.getString("roomId");
            getSupportActionBar().setTitle(roomNickname);
        }

        mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        videoView.start();

        //verificar se usuario logado não é null
        if(FirebaseAuth.getInstance().getUid() == null) {
            startActivity(new Intent(VideosActivity.this, LoginActivity.class));
            Toast.makeText(this, "It's necessary to login to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
        //pegar os dados do usuario
        FirebaseFirestore.getInstance().collection("/users")
                .document(FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        me = documentSnapshot.toObject(User.class);
                    }
                });


        btnChoose.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                chooseVideo();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadVideo();
            }
        });

        btnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intentSender = new Intent(getApplicationContext(), VideosListActivity.class);
                Bundle parameters = new Bundle();

                parameters.putString("roomId", roomId);
                parameters.putString("nickname", roomNickname);

                intentSender.putExtras(parameters);

                //enviar os dados da sala para a nova activity
                startActivity(intentSender);
            }
        });

    }

    private void chooseVideo(){
        //criar uma intenção para pegar o conteúdo do video selecionado
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    //salvar o resultado no fim da activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            videoUri = data.getData();
            videoView.setVideoURI(videoUri);
        }
    }

    private void uploadVideo(){

        final String filename = UUID.randomUUID().toString();
        progressBar.setVisibility(View.VISIBLE);

        if(videoUri != null) {
            final StorageReference reference = FirebaseStorage.getInstance().getReference("/videos/"+filename);

            reference.putFile(videoUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.INVISIBLE);
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Log.i("Teste", uri.toString());
                                    String roomid = roomId;
                                    String fromId = FirebaseAuth.getInstance().getUid();
                                    String videoId = UUID.randomUUID().toString();
                                    Video video = new Video(videoId, edtVideoName.getText().toString(), taskSnapshot.getUploadSessionUri().toString(),
                                            me.getUsername(), System.currentTimeMillis());
                                    FirebaseFirestore.getInstance().collection("/rooms")
                                            .document(roomid)
                                            .collection("videos")
                                            .add(video)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Log.i("Teste", "Upload video "+documentReference.get());
                                                    Toast.makeText(getApplicationContext(), "Uploading succeessful", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e("Uploading failed", e.getMessage());
                                                }
                                            });
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("Teste", e.getMessage());
                            Toast.makeText(VideosActivity.this, "Uploading failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }

    }
}