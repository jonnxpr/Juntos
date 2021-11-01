package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.juntos.R;
import com.juntos.model.User;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.ViewHolder;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollaboratorsActivity extends AppCompatActivity {

    private GroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collaborators);

        BottomNavigationView bottomNavigationView = findViewById(R.id.botom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.collaborators);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.contacts:
                        startActivity(new Intent(getApplicationContext(), ContactsActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.collaborators:
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

        RecyclerView rv = findViewById(R.id.recycler_collaborators);

        adapter = new GroupAdapter();
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));

        FirebaseFirestore.getInstance().collection("/users")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if(e != null){
                            Log.e("Teste", e.getMessage());
                            return;
                        }
                        ArrayList<User> users = new ArrayList<>();
                        //listar todos os usuarios do banco de dados
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        adapter.clear();
                        if(docs != null) {
                            for (DocumentSnapshot doc : docs) {
                                User u = doc.toObject(User.class);
                                users.add(u);
                            }
                        }
                        Collections.sort(users);
                        if(users.size() > 0) {
                            for (User collaborator : users) {
                                adapter.add(new CollaboratorsActivity.UserItem(collaborator));
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

    }

    private class UserItem extends Item<ViewHolder> {

        private final User user;

        private UserItem(User user){
            this.user = user;
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            TextView txtUsername = viewHolder.itemView.findViewById(R.id.textView_item_col_name);
            TextView txtPartic = viewHolder.itemView.findViewById(R.id.textView_item_col_part);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imageView_item_col);
            ImageView medal1 = viewHolder.itemView.findViewById(R.id.imageView_medal1);
            ImageView medal2 = viewHolder.itemView.findViewById(R.id.imageView_medal2);
            ImageView medal3 = viewHolder.itemView.findViewById(R.id.imageView_medal3);
            ImageView medal4 = viewHolder.itemView.findViewById(R.id.imageView_medal4);
            ImageView medal5 = viewHolder.itemView.findViewById(R.id.imageView_medal5);
            if(user != null) {
                int total = user.getChatParticipations() + user.getPersonParticipations();

                txtUsername.setText(user.getUsername());
                txtPartic.setText(total + " participações");
                Picasso.get().load(user.getProfileUrl()).into(imgPhoto);

                //Verificar a participação para dar a bonificação com 5 níveis de medalhas
                if ((user.getPersonParticipations() + user.getChatParticipations()) > 100) {
                    Picasso.get().load(R.drawable.crown).into(medal1);
                    Picasso.get().load(R.drawable.crown).into(medal2);
                    Picasso.get().load(R.drawable.crown).into(medal3);
                    Picasso.get().load(R.drawable.crown).into(medal4);
                    Picasso.get().load(R.drawable.crown).into(medal5);
                } else if ((user.getPersonParticipations() + user.getChatParticipations()) > 80) {
                    Picasso.get().load(R.drawable.crown).into(medal1);
                    Picasso.get().load(R.drawable.crown).into(medal2);
                    Picasso.get().load(R.drawable.crown).into(medal3);
                    Picasso.get().load(R.drawable.crown).into(medal4);
                } else if ((user.getPersonParticipations() + user.getChatParticipations()) > 60) {
                    Picasso.get().load(R.drawable.crown).into(medal1);
                    Picasso.get().load(R.drawable.crown).into(medal2);
                    Picasso.get().load(R.drawable.crown).into(medal3);
                } else if ((user.getPersonParticipations() + user.getChatParticipations()) > 40) {
                    Picasso.get().load(R.drawable.crown).into(medal1);
                    Picasso.get().load(R.drawable.crown).into(medal2);
                } else if ((user.getPersonParticipations() + user.getChatParticipations()) > 20) {
                    Picasso.get().load(R.drawable.crown).into(medal1);
                }
            }
        }

        @Override
        public int getLayout() {
            return R.layout.item_collaborator;
        }
    }
}