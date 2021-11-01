package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
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

public class VideosListActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private User me;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos_list);

        //pegar os dados do objeto enviado pelo Bundle
        //da activity chatroom
        Intent receiverIntent = getIntent();
        Bundle bundle = receiverIntent.getExtras();
        if(bundle != null){
            String label = bundle.getString("nickname");
            getSupportActionBar().setTitle(label + " Videos List");
            roomId = bundle.getString("roomId");
        }

        RecyclerView rv = findViewById(R.id.recycler_videos);
        adapter = new GroupAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        //pegar os dados do usuario logado
        FirebaseFirestore.getInstance().collection("/users")
                .document(FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        me = documentSnapshot.toObject(User.class);
                        fetchVideos();
                    }
                });

        adapter.setOnItemClickListener(new OnItemClickListener() {
            //pegar o evento de clique no video que se deseja assistir
            @Override
            public void onItemClick(@NonNull Item item, @NonNull View view) {
                VideoItem videoItem = (VideoItem) item;
                String videoLink = videoItem.video.getLink();
                String videoName = videoItem.video.getVideoName();

                Intent intentSender = new Intent(VideosListActivity.this, PlayerVideoActivity.class);

                Bundle parameters = new Bundle();
                parameters.putString("videoLink", videoLink);
                parameters.putString("videoName", videoName);

                intentSender.putExtras(parameters);
                startActivity(intentSender);
            }
        });
    }

    private void fetchVideos(){

        //pegar a lista de conversas dessa sala da coleção rooms-conversations
        FirebaseFirestore.getInstance().collection("/rooms")
                .document(roomId)
                .collection("videos")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        //toda vez que pegar um doc da coleção envia para o adapter
                        List<DocumentChange> documentChanges = queryDocumentSnapshots.getDocumentChanges();

                        if(documentChanges != null){
                            for (DocumentChange doc : documentChanges ) {
                                Log.i("Teste", "FetchVideos "+doc.getDocument().toString());
                                if(doc.getType() == DocumentChange.Type.ADDED) {
                                    //transforma o documento em um objeto do tipo Video
                                    Video video = doc.getDocument().toObject(Video.class);
                                    Toast.makeText(getApplicationContext(), "Video founded "+video.getVideoName(), Toast.LENGTH_SHORT).show();
                                    adapter.add(new VideoItem(video));
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                });

    }

    private class VideoItem extends Item<ViewHolder> {

        private final Video video;

        private VideoItem(Video video) {
            this.video = video;
            Log.i("Teste", "Video: " + video.getVideoId());
            Toast.makeText(VideosListActivity.this, "Object VideoItem constructed", Toast.LENGTH_SHORT).show();
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
