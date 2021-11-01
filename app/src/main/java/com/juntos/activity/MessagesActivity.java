package com.juntos.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
//import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal;
import com.google.firebase.messaging.FirebaseMessaging;
import com.juntos.ChatApplication;
import com.juntos.FCMService;
import com.juntos.R;
import com.juntos.model.Contact;
import com.juntos.model.User;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.OnItemClickListener;
import com.xwray.groupie.ViewHolder;

import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private User sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        BottomNavigationView bottomNavigationView = findViewById(R.id.botom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.myMessages);
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
                        return true;
                    case R.id.rooms:
                        startActivity(new Intent(getApplicationContext(), RoomsActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });

        //necessario para funcionar o ciclo de vida que precisamos monitorar
        //para enviar notificacoes a usuarios offline
        ChatApplication application = (ChatApplication) getApplication();
        getApplication().registerActivityLifecycleCallbacks(application);

        RecyclerView rv = findViewById(R.id.recycler_messages);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GroupAdapter();
        rv.setAdapter(adapter);

        updateToken();

        verifyAuthentication();

        fetchLastMessage();

        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull Item item, @NonNull View view) {
                final Intent intent = new Intent(MessagesActivity.this, ChatActivity.class);

                //esses dois comandos permitem enviar o objeto e seus dados a outra activity
                //tem que pega-lo no outro lado, na activity chamada na intent
                MessagesActivity.ContactItem contactItem = (MessagesActivity.ContactItem) item;

                //recuperar dados do sender para enviar a activity chat
                if(contactItem != null && contactItem.contact != null){
                    FirebaseFirestore.getInstance().collection("/users")
                            .document(contactItem.contact.getUserId())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    sender = documentSnapshot.toObject(User.class);
                                    intent.putExtra("user", sender);
                                    startActivity(intent);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MessagesActivity.this, "Impossible to continue.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });
    }

    private void updateToken(){
        //pegar um token unico gerado para esse usuario
        //String token = FirebaseInstanceId.getInstance().getToken(); deprecated
        Task<String> token = FirebaseMessaging.getInstance().getToken();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.e("Teste", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        String uid = FirebaseAuth.getInstance().getUid();

                        //Associar o token gerado ao usuario logado
                        if(uid != null){
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(uid)
                                    .update("token", token);
                        }
                    }
                });

    }
    private void fetchLastMessage() {
        final String uid = FirebaseAuth.getInstance().getUid();

        if(uid != null && !uid.isEmpty()) {
            //busca no Firestore as ultimas mensagens enviadas para o contato do usuario logado
            FirebaseFirestore.getInstance().collection("/last-messages")
                    .document(uid)
                    .collection("contacts")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<DocumentChange> documentChanges = null;

                            if(queryDocumentSnapshots != null) {
                                documentChanges = queryDocumentSnapshots.getDocumentChanges();
                            }

                            if (documentChanges != null) {
                                for (DocumentChange doc : documentChanges) {
                                    if (doc.getType() == DocumentChange.Type.ADDED) {//se o tipo do documento for do tipo de adicao
                                        Contact contact = doc.getDocument().toObject(Contact.class);//transforma o documento em um objeto contact
                                        //nao mostrar as ultimas mensagens que eu mesma enviei apenas as que foram enviadas para mim
                                        if (!contact.getUserId().equals(uid))
                                            adapter.add(new ContactItem(contact));
                                    }
                                }
                            }
                        }
                    });
        }
    }

    //se o usuario não tiver logado direciona para a activity de login
    public void verifyAuthentication(){
        if(FirebaseAuth.getInstance().getUid() == null) {
            Intent intent = new Intent(MessagesActivity.this, LoginActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        }
    }
    private class ContactItem extends Item<ViewHolder> {

        private final Contact contact;

        private ContactItem(Contact contact) {
            this.contact = contact;
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            TextView userName = viewHolder.itemView.findViewById(R.id.textView_contact);
            TextView message = viewHolder.itemView.findViewById(R.id.textView_last_message);
            ImageView imgPhoto = viewHolder.itemView.findViewById(R.id.imageView_last_message);

            userName.setText(contact.getUsername());
            message.setText(contact.getLastMessage());
            Picasso.get()
                    .load(contact.getPhotoUrl())
                    .into(imgPhoto);
        }

        @Override
        public int getLayout() {
            return R.layout.item_user_message;
        }
    }

}