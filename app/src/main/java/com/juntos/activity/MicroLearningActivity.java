package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.juntos.R;
import com.juntos.model.User;
import com.juntos.model.Video;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.OnItemClickListener;
import com.xwray.groupie.ViewHolder;

import java.util.List;
import java.util.UUID;

public class MicroLearningActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private User me;
    private String roomId;
    private String roomNickName;
    private EditText edtLinkMicro;
    private EditText edtDesc;
    private Button btn_addlink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_micro_learning);

        edtLinkMicro = findViewById(R.id.editText_microlearn);
        edtDesc = findViewById(R.id.editText_desc_microlearn);
        btn_addlink = findViewById(R.id.button_addlink);

        RecyclerView rv = findViewById(R.id.recycler_microlearn);
        adapter = new GroupAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        //pegar os dados do objeto enviado pelo Bundle
        //da activity chatroom
        Intent receiverIntent = getIntent();
        Bundle bundle = receiverIntent.getExtras();
        if(bundle != null){
            String label = bundle.getString("nickname");
            roomId = bundle.getString("roomId");
            getSupportActionBar().setTitle(label + " Videos List");
            roomNickName = label;
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.botom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.contacts:
                        startActivity(new Intent(getApplicationContext(), ContactsActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.collaborators:
                        startActivity(new Intent(getApplicationContext(), CollaboratorsActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.myMessages:
                        startActivity(new Intent(getApplicationContext(), MessagesActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.rooms:
                        startActivity(new Intent(getApplicationContext(), RoomsActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });
        //pegar os dados do usuario logado
        FirebaseFirestore.getInstance().collection("/users")
                .document(FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        me = documentSnapshot.toObject(User.class);
                        fetchLinks();
                    }
                });

        btn_addlink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLinkVideo();
            }
        });
        adapter.setOnItemClickListener(new OnItemClickListener() {
            //pegar o evento de clique no video que se deseja assistir
            @Override
            public void onItemClick(@NonNull Item item, @NonNull View view) {
                MicroLearningActivity.VideoItem videoItem = (MicroLearningActivity.VideoItem) item;
                String link = videoItem.video.getLink();
                String videoName = videoItem.video.getVideoName();

                Intent intentSender = new Intent(MicroLearningActivity.this, PlayerVideoActivity.class);

                Bundle parameters = new Bundle();
                parameters.putString("link", link);
                parameters.putString("videoName", videoName);
                parameters.putString("roomId", roomId);
                parameters.putString("nickname", roomNickName);

                intentSender.putExtras(parameters);
                startActivity(intentSender);
            }
        });
    }

    private void saveLinkVideo(){
        if(me == null) { startActivity(new Intent(MicroLearningActivity.this, LoginActivity.class));}

        Log.i("Teste", "Try to save link");

        String linkVideo = edtLinkMicro.getText().toString();
        String descrip = edtDesc.getText().toString();

        if(linkVideo == null || linkVideo.isEmpty() || descrip == null || descrip.isEmpty()){
            Toast.makeText(MicroLearningActivity.this, "All fileds are required", Toast.LENGTH_SHORT).show();
            return;
        }

        edtLinkMicro.setText(null);
        edtDesc.setText(null);
        String clearLink="";
        String videoId = UUID.randomUUID().toString();
        Log.i("Teste", "VideoId "+videoId);
        long timestamp = System.currentTimeMillis();
        Log.i("Teste", String.valueOf(timestamp));

        if(linkVideo.contains("?v=")) {
            clearLink = linkVideo.substring(linkVideo.lastIndexOf("?v=")+3);
        } else if(linkVideo.contains("=")){
            clearLink = linkVideo.substring(linkVideo.lastIndexOf('='));
        } else {
            clearLink = linkVideo;
        }
        Log.i("Teste", "Link:"+clearLink);

        final Video v = new Video();
        v.setVideoId(videoId);
        v.setVideoName(descrip);
        v.setLink(clearLink);
        v.setSenderName(me.getUsername());
        v.setTimestamp(timestamp);

        //cria no firebase um documento mensagem do usuario logado
        if(roomId != null && !roomId.isEmpty()){
            FirebaseFirestore.getInstance().collection("/rooms")
                    .document(roomId)
                    .collection("videos")
                    .add(v)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.i("Teste", documentReference.getId());

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });

            if(me != null) {
                //incrementar a participação do usuario
                int part = me.getChatParticipations();
                part = part + 2;
                FirebaseFirestore.getInstance().collection("users")
                        .document(me.getUserid())
                        .update("chatParticipations", part);
            }
        }
    }

    private void fetchLinks(){
        if(roomId != null) {
            //pegar a lista de conversas dessa sala da coleção rooms-conversations
            FirebaseFirestore.getInstance().collection("/rooms")
                    .document(roomId)
                    .collection("videos")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            //toda vez que pegar um doc da coleção envia para o adapter
                            List<DocumentChange> documentChanges = queryDocumentSnapshots.getDocumentChanges();

                            if (documentChanges != null) {
                                for (DocumentChange doc : documentChanges) {
                                    Log.i("Teste", "FetchLinks " + doc.getDocument().toString());
                                    if (doc.getType() == DocumentChange.Type.ADDED) {
                                        //transforma o documento em um objeto do tipo Video
                                        Video video = doc.getDocument().toObject(Video.class);
                                        adapter.add(new MicroLearningActivity.VideoItem(video));
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private class VideoItem extends Item<ViewHolder> {

        private final Video video;

        private VideoItem(Video video) {
            this.video = video;
            Log.i("Teste", "Video: " + video.getVideoId());
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            Log.i("Teste", "Bind called");
            TextView txtVideoname = viewHolder.itemView.findViewById(R.id.textView_title_item_video);
            TextView txtSender = viewHolder.itemView.findViewById(R.id.textView_sender_item_video);

            txtVideoname.setText(video.getVideoName());
            txtSender.setText("Posted by "+video.getSenderName());
        }

        @Override
        public int getLayout() {
            return R.layout.item_video;
        }
    }

}