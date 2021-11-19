package com.juntos.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.juntos.R;
import com.juntos.model.User;

import java.io.IOException;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    EditText edtName, edtEmail, edtPassword;
    Button btnRegister, btnSelPhoto;
    ImageView mImgPhoto;
    private Uri mSelectedUri;

    private int CHOOSE_PHOTO = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtEmail = findViewById(R.id.editText_email);
        edtPassword = findViewById(R.id.editText_password);
        edtName = findViewById(R.id.editText_name);
        btnRegister = findViewById(R.id.button_register);
        btnSelPhoto = findViewById(R.id.button_select_image);
        mImgPhoto = findViewById(R.id.imageView_photo);

        btnSelPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser();
            }
        });
    }

    private void createUser(){
        Log.i("Teste","createUser() method called");
        String name = edtName.getText().toString();
        String email = edtEmail.getText().toString();
        String password = edtPassword.getText().toString();

        if(email == null || email.isEmpty() || password==null || password.isEmpty() || name==null || name.isEmpty()){
            Toast.makeText(RegisterActivity.this, "All fileds are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i("Teste","Register User: "+ name);
        Log.i("Teste","Register email: "+ email);
        Log.i("Teste", "Register password: "+ password);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Log.i("Teste", "Created User:" + task.getResult().getUser().getUid());
                            Toast.makeText(RegisterActivity.this, getResources().getText(R.string.registerInProgress), Toast.LENGTH_SHORT).show();
                            saveUserInFirebase();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("Teste", e.getMessage());
                        Toast.makeText(RegisterActivity.this, getResources().getText(R.string.emailUsed_SmallPassword), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    //pegar a foto selecionada quando terminar o evento de selecao da foto
    //no retorno da activity PICK
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CHOOSE_PHOTO) {
            Log.i("Teste","onActivityResult CHOOSE_PHOTO");
            if(data != null) {
                mSelectedUri = data.getData();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mSelectedUri);
                    mImgPhoto.setImageDrawable(new BitmapDrawable(bitmap));
                    btnSelPhoto.setAlpha(0); //esconde o botão - fica transparente
                } catch (IOException e) {
                }
            }
        }
    }

    private void selectPhoto(){
        Log.i("Teste","selectPhoto() method called");
        //criar uma nova intent para pegar a foto da galeria
        Intent intent = new Intent(Intent.ACTION_PICK);
        //setar o tipo de coisa que será buscada
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    private void saveUserInFirebase(){
        if(mSelectedUri == null) {
            mSelectedUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                    + "://" + this.getResources().getResourcePackageName(R.drawable.logotipo_android_black)
                    + '/' + this.getResources().getResourceTypeName(R.drawable.logotipo_android_black)
                    + '/' + this.getResources().getResourceEntryName(R.drawable.logotipo_android_black));
        }

        Log.i("Teste", "mSelectedUri:"+ mSelectedUri.toString());

        String filename = UUID.randomUUID().toString(); //gera uma hash com uma referência aleatória
        final StorageReference ref = FirebaseStorage.getInstance().getReference("/images/"+filename);
        ref.putFile(mSelectedUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Log.i("Teste", "urlImage:"+ uri.toString());

                                String uid = FirebaseAuth.getInstance().getUid();
                                String username = edtName.getText().toString();
                                String profileUrl = uri.toString();

                                User user = new User(uid, username, profileUrl,0,0);

                                FirebaseFirestore.getInstance().collection("users")
                                        .document(uid)
                                        .set(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Intent intent = new Intent(RegisterActivity.this, RoomsActivity.class);
                                                //Essas flags fazem com que essa seja a activity principal nesse momento
                                                //Não volta para a tela de login porque já realizou o cadastro
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("Teste", e.getMessage());
                                                Toast.makeText(RegisterActivity.this, "Register faild.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Teste", e.getMessage());
                    }
                });

    }
}