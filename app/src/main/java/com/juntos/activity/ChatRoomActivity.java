package com.juntos.activity;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.juntos.model.Message;
import com.juntos.model.Room;
import com.juntos.model.User;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.ViewHolder;

import java.util.List;

public class ChatRoomActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private User me;
    private Room room;
    private Button btnSend;
    private EditText edtChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        //pegar os dados do objeto enviado pelo Parcelabel
        //enviado pela activity rooms
        room = getIntent().getExtras().getParcelable("room");
        getSupportActionBar().setTitle(room.getRoomNickname());

        edtChat = findViewById(R.id.editText_chatRoom);
        btnSend = findViewById(R.id.button_send_room);

        RecyclerView rv = findViewById(R.id.recycler_chatRoom);
        adapter = new GroupAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        if(FirebaseAuth.getInstance().getUid() == null) {
            startActivity(new Intent(ChatRoomActivity.this, LoginActivity.class));
            Toast.makeText(this, "It's necessary to login to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
        //pegar os dados do usuario logado
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

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        String roomId = room.getRoomId();;
        String roomNickname= room.getRoomNickname();;

        switch(item.getItemId()){
            case R.id.exercises:
                Intent intSender = new Intent(ChatRoomActivity.this, QuestDatabaseActivity.class);
                Bundle param = new Bundle();

                param.putString("roomId", roomId);
                param.putString("nickname", roomNickname);

                intSender.putExtras(param);
                //enviar os dados da sala para a nova activity
                startActivity(intSender);
                break;
            case R.id.video:
                Intent intentSender = new Intent(getApplicationContext(), MicroLearningActivity.class);
                Bundle parameters = new Bundle();

                parameters.putString("roomId", roomId);
                parameters.putString("nickname", roomNickname);

                intentSender.putExtras(parameters);
                //enviar os dados da sala para a nova activity
                startActivity(intentSender);
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
                Intent intent = new Intent(ChatRoomActivity.this, MainConfigActivity.class);
                startActivity(intent);
                break;
            case R.id.close:
                this.finishAffinity();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void fetchMessages(){

        String roomId = room.getRoomId();

        //pegar a lista de conversas dessa sala da coleção rooms-conversations
        FirebaseFirestore.getInstance().collection("/rooms-conversations")
                .document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        List<DocumentChange> documentChanges = null;

                        //toda vez que pegar um documento da coleção cria os baloezinhos de mensagem na sala
                        if(queryDocumentSnapshots != null) {
                            documentChanges = queryDocumentSnapshots.getDocumentChanges();
                        }

                        if(documentChanges != null){
                            for (DocumentChange doc : documentChanges ) {
                                if(doc.getType() == DocumentChange.Type.ADDED) {
                                    //transforma o documento em um objeto do tipo Message
                                    Message message = doc.getDocument().toObject(Message.class);
                                    //adiciona a mensagem ao adapter
                                    adapter.add(new ChatRoomActivity.MessageItem(message));
                                }
                            }
                        }
                    }
                });

    }

    private void sendMessage(){
        String text = edtChat.getText().toString();

        edtChat.setText(null);

        final String fromId = FirebaseAuth.getInstance().getUid();
        final String roomId = room.getRoomId();
        long timestamp = System.currentTimeMillis();

        final Message message = new Message();
        message.setFromId(fromId);
        message.setToId(roomId);
        message.setSender(me);
        message.setTimestamp(timestamp);
        message.setText(text);

        //cria no firebase um documento mensagem do usuario logado
        if(!message.getText().isEmpty()){
            FirebaseFirestore.getInstance().collection("/rooms-conversations")
                    .document(roomId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.i("Teste", documentReference.getId());

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("Teste", e.getMessage());
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

    //faz o logout e direciona para a activity de login
    private void verifyAuthentication(){
        if(FirebaseAuth.getInstance().getUid() == null) {
            Intent intent = new Intent(ChatRoomActivity.this, LoginActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        }
    }

    private class MessageItem extends Item<ViewHolder> {

        private final Message message;

        private MessageItem(Message message){
            this.message = message;
            Log.i("Teste", "Msg: "+message.getFromId() +" "+message.getText());
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            TextView txtMsg = viewHolder.itemView.findViewById(R.id.textView_message_user);
            ImageView imgMsg = viewHolder.itemView.findViewById(R.id.imageView_message_user);

            txtMsg.setText(message.getText());

            if (message.getFromId().equals(FirebaseAuth.getInstance().getUid())) {
                Picasso.get()
                        .load(me.getProfileUrl())
                        .into(imgMsg);
            } else if (message.getSender() != null) {
                Picasso.get()
                        .load(message.getSender().getProfileUrl())
                        .into(imgMsg);
            } else {
                Picasso.get().load(R.drawable.bg_btn_rounded).into(imgMsg);
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