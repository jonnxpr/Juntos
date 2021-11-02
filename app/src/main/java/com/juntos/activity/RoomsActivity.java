package com.juntos.activity;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.juntos.R;
import com.juntos.model.Room;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.OnItemClickListener;
import com.xwray.groupie.ViewHolder;

import java.util.List;

//Nessa activity sao listadas todas as salas para tirar duvidas
public class RoomsActivity extends AppCompatActivity {

    private GroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        BottomNavigationView bottomNavigationView = findViewById(R.id.botom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.rooms);
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
                        return true;
                }
                return false;
            }
        });

        RecyclerView rv = findViewById(R.id.recycler_rooms);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GroupAdapter();
        rv.setAdapter(adapter);

        verifyAuthentication();

        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull Item item, @NonNull View view) {
                Intent intent = new Intent(RoomsActivity.this, ChatRoomActivity.class);

                //enviar o objeto clicado para a nova activity
                RoomsActivity.RoomItem roomItem = (RoomsActivity.RoomItem) item;
                intent.putExtra("room", roomItem.room);

                startActivity(intent);
            }
        });

        fetchRooms();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.exercises:
                Toast.makeText(RoomsActivity.this, "Choose a room to questions database", Toast.LENGTH_SHORT).show();
                break;
            case R.id.video :
                Toast.makeText(RoomsActivity.this, "Choose a room to send a video", Toast.LENGTH_SHORT).show();
                break;
            case R.id.logout:
                String uid = FirebaseAuth.getInstance().getUid();
                if(uid != null) {
                    FirebaseFirestore.getInstance().collection("users")
                            .document(uid)
                            .update("online", false);
                }
                FirebaseAuth.getInstance().signOut();
                verifyAuthentication();
                break;
            case R.id.newRoom:
                Intent intent = new Intent(RoomsActivity.this, MainConfigActivity.class);
                startActivity(intent);
                break;
            case R.id.close:
                this.finishAffinity();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchRooms(){
        FirebaseFirestore.getInstance().collection("/rooms")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if(e != null){

                            Log.e("Teste", e.getMessage());
                            return;
                        }

                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        adapter.clear();
                        for (DocumentSnapshot doc: docs ) {
                            Room room = doc.toObject(Room.class);

                            adapter.add(new RoomsActivity.RoomItem(room));
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    //faz o logout e direciona para a activity de login
    private void verifyAuthentication(){
        if(FirebaseAuth.getInstance().getUid() == null) {
            Intent intent = new Intent(RoomsActivity.this, LoginActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        }
    }

    private class RoomItem extends Item<ViewHolder> {

        private final Room room;

        private RoomItem(Room room){
            this.room = room;
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            TextView txtRoomName = viewHolder.itemView.findViewById(R.id.textView_room);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imageView_room_message);

            txtRoomName.setText(room.getRoomName());
            if(room.getProfileUrl() != null && !room.getProfileUrl().isEmpty()) {
                Picasso.get().load(room.getProfileUrl()).into(imgPhoto);
            }
            else {
                Picasso.get().load(R.drawable.icone_juntos).into(imgPhoto);
            }
        }

        @Override
        public int getLayout() {
            return R.layout.item_room;
        }
    }
}
