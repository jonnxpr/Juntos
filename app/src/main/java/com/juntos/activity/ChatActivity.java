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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.juntos.R;
import com.juntos.model.Contact;
import com.juntos.model.Message;
import com.juntos.model.Notification;
import com.juntos.model.User;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.ViewHolder;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private User user; //para quem estou enviando mensagem
    private User me; //(usuario logado)
    private Button btnSend;
    private EditText edtChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //pegar os dados do objeto enviado pelo Parcelabel
        //enviado pela activity contacts
        user = getIntent().getExtras().getParcelable("user");
        getSupportActionBar().setTitle(user.getUsername());

        RecyclerView rv = findViewById(R.id.recycler_chat);
        edtChat = findViewById(R.id.editText_chat);
        btnSend = findViewById(R.id.button_send);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        adapter = new GroupAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        if(FirebaseAuth.getInstance().getUid()==null){ startActivity(new Intent(ChatActivity.this, LoginActivity.class)); }

        //pegar dados do usuario logado no firestore
        FirebaseFirestore.getInstance().collection("/users")
                .document(FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        me = documentSnapshot.toObject(User.class);
                        fetchMessages();
                    }
                });
    }

    private void fetchMessages(){
        if(me != null && user != null){

            String fromId = me.getUserid();
            String toId = user.getUserid();

            //pegar a lista de conversas da coleção conversations
            //para as quais eu sou o sender e quem eu estou conversando é o receiver
            //de forma ordenada pelo tempo de envio
            FirebaseFirestore.getInstance().collection("/conversations")
                    .document(fromId)
                    .collection(toId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<DocumentChange> documentChanges = null;

                            //toda vez que pegar uma coleção cria os baloezinhos de mensagem
                            if(queryDocumentSnapshots != null) {
                                documentChanges = queryDocumentSnapshots.getDocumentChanges();
                            }

                            if(documentChanges != null){
                                for (DocumentChange doc : documentChanges ) {
                                    if(doc.getType() == DocumentChange.Type.ADDED) {//objeto que acabou de ser adicionado
                                        //transforma o documento em um objeto do tipo Message
                                        Message message = doc.getDocument().toObject(Message.class);
                                        //adiciona a mensagem ao adapter
                                        adapter.add(new MessageItem(message));
                                    }
                                }
                            }
                        }
                    });
        }

    }

    private void sendMessage(){
        String text = edtChat.getText().toString();

        edtChat.setText(null);

        final String fromId = FirebaseAuth.getInstance().getUid();
        final String toId = user.getUserid();
        long timestamp = System.currentTimeMillis();

        final Message message = new Message();
        message.setFromId(fromId);
        message.setToId(toId);
        message.setTimestamp(timestamp);
        message.setText(text);

        //cria no firebase um documento mensagem do meu usuario como origem
        //para uma colecao de usuarios de destino como se fosse 1:N
        //onde o contato é pra quem eu enviei
        if(!message.getText().isEmpty()){
            FirebaseFirestore.getInstance().collection("/conversations")
                    .document(fromId)
                    .collection(toId)
                    .add(message)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("Teste", documentReference.getId());

                            Contact contact = new Contact();
                            contact.setUserId(fromId);
                            contact.setUsername(me.getUsername());
                            contact.setPhotoUrl(me.getProfileUrl());
                            contact.setTimestamp(message.getTimestamp());
                            contact.setLastMessage(message.getText());

                            //criar um novo nó no Firestore para as ultimas mensagens enviadas
                            FirebaseFirestore.getInstance().collection("last-messages")
                                    .document(fromId)
                                    .collection("contacts")
                                    .document(toId)
                                    .set(contact);

                            //adicionar mais um nó para notificacoes qdo o usuario nao estiver online
                            if(user != null && !user.isOnline()){
                                String tokenExpress;
                                if(user.getToken() == null || user.getToken().isEmpty()){
                                    tokenExpress = user.getUserid();
                                } else {
                                    tokenExpress = user.getToken();
                                }
                                Notification notification = new Notification();
                                notification.setFromId(message.getFromId());
                                notification.setToId(message.getToId());
                                notification.setTimestamp(message.getTimestamp());
                                notification.setFromName(me.getUsername());

                                FirebaseFirestore.getInstance().collection("/notifications")
                                        .document(tokenExpress)
                                        .set(notification);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Teste", e.getMessage(), e);
                        }
                    });
            //faz o mesmo para o outro lado como se fosse o outro lado do relacionamento N:N
            //cria uma colecao de mensagem que foram enviadas para esse usuario com quem eu
            //estou conversando, oriundas de uma colecao de usuarios origem
            FirebaseFirestore.getInstance().collection("/conversations")
                    .document(toId)
                    .collection(fromId)
                    .add(message)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("Teste", documentReference.getId());

                            Contact contact = new Contact();
                            contact.setUserId(fromId);
                            contact.setUsername(me.getUsername());
                            contact.setPhotoUrl(me.getProfileUrl());
                            contact.setTimestamp(message.getTimestamp());
                            contact.setLastMessage(message.getText());

                            FirebaseFirestore.getInstance().collection("last-messages")
                                    .document(toId)
                                    .collection("contacts")
                                    .document(fromId)
                                    .set(contact);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Teste", e.getMessage(), e);
                        }
                    });

            if(me != null) {
                //incrementar a participação do usuario
                int part = me.getPersonParticipations();
                part = part + 1;
                FirebaseFirestore.getInstance().collection("users")
                        .document(me.getUserid())
                        .update("personParticipations", part);
            }
        }
    }

    private class MessageItem extends Item<ViewHolder> {

        private final Message message;

        private MessageItem(Message message){
            this.message = message;
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            TextView txtMsg = viewHolder.itemView.findViewById(R.id.textView_message_user);
            ImageView imgMsg = viewHolder.itemView.findViewById(R.id.imageView_message_user);

            txtMsg.setText(message.getText());
            if(message.getFromId().equals(FirebaseAuth.getInstance().getUid())){
                Picasso.get()
                        .load(me.getProfileUrl())
                        .into(imgMsg);
            } else {
                Picasso.get()
                        .load(user.getProfileUrl())
                        .into(imgMsg);
            }
        }

        @Override
        public int getLayout() {
            //se a msg for do usuario logado, retorna layout esquerda
            //caso contrario retorna layout direita
            return message.getFromId().equals(FirebaseAuth.getInstance().getUid())
                    ? R.layout.item_message_right
                    : R.layout.item_message_left;
        }
    }
}