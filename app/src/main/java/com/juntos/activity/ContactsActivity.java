package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.juntos.R;
import com.juntos.model.User;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.OnItemClickListener;
import com.xwray.groupie.ViewHolder;

import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        BottomNavigationView bottomNavigationView = findViewById(R.id.botom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.contacts);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.contacts:
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
        edtSearch = findViewById(R.id.editTextSearchContact);
        RecyclerView rv = findViewById(R.id.recycler_contact);

        adapter = new GroupAdapter();
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter.setOnItemClickListener(new OnItemClickListener() {
            //pegar o evento de clique no usuario que se deseja conversar
            @Override
            public void onItemClick(@NonNull Item item, @NonNull View view) {
                Intent intent = new Intent(ContactsActivity.this, ChatActivity.class);

                //esses dois comandos permitem enviar o objeto e seus dados a outra activity
                //tem que pega-lo no outro lado, na activity chamada na intent
                UserItem userItem = (UserItem) item;
                intent.putExtra("user", userItem.user);

                startActivity(intent);
            }
        });
        //Busca usuarios no Firebase
        fetchUsers();

    }


    private void fetchUsers(){
        FirebaseFirestore.getInstance().collection("/users")
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
                            if(doc != null) {
                                User user = doc.toObject(User.class);
                                String uid = FirebaseAuth.getInstance().getUid();
                                //para nao listar o usuario logado como um contato possivel
                                if (user.getUserid().equals(uid))
                                    continue;
                                adapter.add(new UserItem(user));
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
            TextView txtUsername = viewHolder.itemView.findViewById(R.id.textView);
            ShapeableImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imageView_last_message);

            txtUsername.setText(user.getUsername());
            Picasso.get().load(user.getProfileUrl()).into(imgPhoto);
        }

        @Override
        public int getLayout() {
            return R.layout.item_contact;
        }
    }
}
