package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.juntos.R;
import com.juntos.model.Room;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainConfigActivity extends AppCompatActivity {

    private EditText edtName, edtNickname, edtPass;
    private Button btn_register, btn_sel_photo;
    private ImageView mImgPhoto;
    private Uri mSelectedUri;

    private int CHOOSE_ROOM_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_config);

        edtNickname = findViewById(R.id.editText_nickname);
        edtName = findViewById(R.id.editText_rName);
        edtPass = findViewById(R.id.editText_passRoom);
        btn_register = findViewById(R.id.button_create);
        btn_sel_photo = findViewById(R.id.button_select_roomImage);
        mImgPhoto = findViewById(R.id.imageView_roomPhoto);

        btn_sel_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto();
            }
        });
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRoom();
            }
        });

    }

    private void createRoom(){
        String name = edtName.getText().toString();
        String nickname = edtNickname.getText().toString();
        String password = edtPass.getText().toString();

        if(nickname == null || nickname.isEmpty() || name==null || name.isEmpty() || password==null || password.isEmpty()){
            Toast.makeText(MainConfigActivity.this, "Todos os campos devem ser preenchidos.", Toast.LENGTH_SHORT).show();
            return;
        }

        //Toast.makeText(MainConfigActivity.this, "Fields filled correctly", Toast.LENGTH_SHORT).show();

        verifyAuthentication();
    }

    //pegar a foto selecionada quando terminar o evento de selecao da foto
    //no retorno da activity PICK
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CHOOSE_ROOM_IMAGE) {
            if(data != null) {
                mSelectedUri = data.getData();

                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mSelectedUri);
                    mImgPhoto.setImageDrawable(new BitmapDrawable(bitmap));
                    btn_sel_photo.setAlpha(0);
                } catch (IOException e) {
                }
            }
        }
    }

    private void selectPhoto(){
        //criar uma nova intent para pegar a foto da galeria
        Intent intent = new Intent(Intent.ACTION_PICK);
        //setar o tipo de coisa que será buscada
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_ROOM_IMAGE);
    }

    private void saveRoomInFirebase(){
        final String filename = UUID.randomUUID().toString();

        final StorageReference referencia = FirebaseStorage.getInstance().getReference("/images/"+filename);
        if(mSelectedUri == null) {
            mSelectedUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                    + "://" + this.getResources().getResourcePackageName(R.drawable.teamwork)
                    + '/' + this.getResources().getResourceTypeName(R.drawable.teamwork)
                    + '/' + this.getResources().getResourceEntryName(R.drawable.teamwork));
        }
        referencia.putFile(mSelectedUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        referencia.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Log.i("Teste", uri.toString());

                                String roomid = UUID.randomUUID().toString();
                                String roomName = edtName.getText().toString();
                                String roomNickname = edtNickname.getText().toString();
                                String profileUrl = uri.toString();

                                Room myRoom = new Room(roomid, roomName, roomNickname, profileUrl);
                                edtName.setText(null); edtNickname.setText(null); edtPass.setText(null);
                                FirebaseFirestore.getInstance().collection("/rooms")
                                        .document(roomid)
                                        .set(myRoom)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Log.i("Teste", "Room created");
                                                Toast.makeText(MainConfigActivity.this, "Sala criada com sucesso!", Toast.LENGTH_SHORT).show();
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
    }

    private void verifyAuthentication(){
        if(FirebaseAuth.getInstance().getUid() == null) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainConfigActivity.this, LoginActivity.class));
        } else {
            if(edtPass.getText().toString().equals("@dmin@2021")){
                saveRoomInFirebase();
            } else {
                Toast.makeText(this, "Apenas administradores podem criar novas salas.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainConfigActivity.this, RoomsActivity.class));
                finish();
            }
        }
    }
}
