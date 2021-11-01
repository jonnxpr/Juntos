package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.juntos.R;
import com.juntos.model.Exercise;
import com.juntos.model.User;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.OnItemClickListener;
import com.xwray.groupie.ViewHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class QuestDatabaseActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private User me;
    private String roomId;
    private String roomNickName;
    private EditText edtExDesc;
    private Button btn_addEx, btn_selEx;
    private ImageView mImgPhoto;
    private Uri mSelectedUri;

    private int CHOOSE_EXERCISE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest_database);

        edtExDesc = findViewById(R.id.editText_exerDescript);
        btn_addEx = findViewById(R.id.button_addexercise);
        btn_selEx = findViewById(R.id.button_selExerc);
        mImgPhoto = findViewById(R.id.imageView_db);

        RecyclerView rv = findViewById(R.id.recycler_DB);
        adapter = new GroupAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        //pegar os dados do objeto enviado pelo Bundle
        //da activity chatroom
        Intent receiverIntent = getIntent();
        Bundle bundle = receiverIntent.getExtras();
        if (bundle != null) {
            String label = bundle.getString("nickname");
            roomId = bundle.getString("roomId");
            getSupportActionBar().setTitle(label + " Solved Exercises");
            roomNickName = label;
        }
        BottomNavigationView bottomNavigationView = findViewById(R.id.botom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.contacts:
                        startActivity(new Intent(getApplicationContext(), ContactsActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.collaborators:
                        startActivity(new Intent(getApplicationContext(), CollaboratorsActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.myMessages:
                        startActivity(new Intent(getApplicationContext(), MessagesActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.rooms:
                        startActivity(new Intent(getApplicationContext(), RoomsActivity.class));
                        overridePendingTransition(0, 0);
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
                        fetchExercises();
                    }
                });

        btn_selEx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectExerciseImage();
            }
        });

        btn_addEx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveExerciseInFirebase();
            }
        });
    }


    //pegar a foto selecionada quando terminar o evento de selecao da foto
    //no retorno da activity PICK
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CHOOSE_EXERCISE) {
            if(data != null) {
                mSelectedUri = data.getData();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mSelectedUri);
                    mImgPhoto.setImageDrawable(new BitmapDrawable(bitmap));
                    btn_selEx.setAlpha(0);

                } catch (IOException e) {
                    Toast.makeText(this, getString(R.string.somethingWrongHappened), Toast.LENGTH_SHORT).show();
                    Log.e("Teste", "QuestDatabaseActivity " + e.getMessage());
                } catch (OutOfMemoryError er) {
                    Toast.makeText(this, getString(R.string.imageTooBig), Toast.LENGTH_LONG).show();
                    Log.e("Teste", "QuestDatabaseActivity " + er.getMessage());
                }
            }
        }
    }

    private void selectExerciseImage(){
        //criar uma nova intent para pegar a foto da galeria
        Intent intent = new Intent(Intent.ACTION_PICK);
        //setar o tipo de coisa que será buscada
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_EXERCISE);
    }

    private void saveExerciseInFirebase(){

        String description = edtExDesc.getText().toString();
        String fromId = FirebaseAuth.getInstance().getUid();

        if(description == null || description.isEmpty()){
            Toast.makeText(QuestDatabaseActivity.this, getResources().getString(R.string.descriptionEmpty), Toast.LENGTH_SHORT).show();
            return;
        } else if (mSelectedUri == null) {
            Toast.makeText(QuestDatabaseActivity.this, getResources().getString(R.string.selectAnImage), Toast.LENGTH_SHORT).show();
            return;
        }

        final String filename = UUID.randomUUID().toString();

        final StorageReference ref = FirebaseStorage.getInstance().getReference("/images/"+filename);

        ref.putFile(mSelectedUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Log.i("Teste", uri.toString());

                                String roomid = roomId;
                                String exercid = UUID.randomUUID().toString();
                                String descrip = edtExDesc.getText().toString();
                                String imgUrl = uri.toString();

                                Exercise myEx = new Exercise(exercid, descrip, imgUrl, me.getUsername());
                                edtExDesc.setText(null);

                                FirebaseFirestore.getInstance().collection("/rooms")
                                        .document(roomid)
                                        .collection("exercises")
                                        .document(exercid)
                                        .set(myEx)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Log.i("Teste", getString(R.string.exerciseSaved));
                                                Toast.makeText(getApplicationContext(), getString(R.string.exerciseSaved), Toast.LENGTH_SHORT).show();
                                                btn_selEx.setVisibility(View.VISIBLE);
                                                mImgPhoto.setImageDrawable(getDrawable(R.drawable.googleg_standard_color_18));
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("Teste", e.getMessage());
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


    private void fetchExercises() {
        if (roomId != null) {
            //pegar a lista de exercicios dessa sala da coleção rooms-conversations
            FirebaseFirestore.getInstance().collection("/rooms")
                    .document(roomId)
                    .collection("exercises")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<DocumentChange> documentChanges = null;
                            //toda vez que pegar um doc da coleção envia para o adapter
                            if(queryDocumentSnapshots != null) {
                                documentChanges = queryDocumentSnapshots.getDocumentChanges();
                            }

                            if (documentChanges != null) {
                                for (DocumentChange doc : documentChanges) {
                                    Log.i("Teste", "FetchExercises " + doc.getDocument().toString());
                                    if (doc.getType() == DocumentChange.Type.ADDED) {
                                        //transforma o documento em um objeto do tipo Video
                                        Exercise exerc = doc.getDocument().toObject(Exercise.class);
                                        adapter.add(new QuestDatabaseActivity.ExerciseItem(exerc));
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private class ExerciseItem extends Item<ViewHolder> {

        private final Exercise exercise;

        private ExerciseItem(Exercise exercise) {
            this.exercise = exercise;
            Log.i("Teste", "Exercise: " + exercise.getExercId());
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            Log.i("Teste", "Bind called");
            TextView txtExDescription = viewHolder.itemView.findViewById(R.id.textView_contact2);
            TextView txtSender = viewHolder.itemView.findViewById(R.id.textView_last_message2);
            ImageView imgV = viewHolder.itemView.findViewById(R.id.imageView_last_message2);

            Log.i("Teste", "Desc:"+exercise.getDescription());
            Log.i("Teste", "Sender:"+exercise.getSender());
            Log.i("Teste", "Url:"+exercise.getImgUrl());
            txtExDescription.setText(exercise.getDescription());
            txtSender.setText(exercise.getSender());
            Picasso.get().load(exercise.getImgUrl()).into(imgV);
        }

        @Override
        public int getLayout() {
            return R.layout.item_database;
        }
    }
}