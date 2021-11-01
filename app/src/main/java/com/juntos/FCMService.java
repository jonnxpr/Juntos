package com.juntos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.juntos.activity.ChatActivity;
import com.juntos.model.User;

import java.util.Map;

//classe responsavel por escutar os eventos de notificacoes vindas do servidor Firebase Cloud Message
//precisamos informar ao Firebase que vamos ter um serviço que vai fazer isso
//essa classe herda as propriedades do FirebaseMessagingService
public class FCMService  extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        final Map<String, String> data = remoteMessage.getData();

        if(data == null || data.get("sender") == null) return;

        final Intent ii = new Intent(FCMService.this, ChatActivity.class);
        FirebaseFirestore.getInstance().collection("/users")
                .document(data.get("sender"))
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User sender = documentSnapshot.toObject(User.class);

                        ii.putExtra("user", sender);
                        //criar uma intenção pendente (que espera algo acontecer)
                        PendingIntent pIntent = PendingIntent.getActivity(
                                getApplicationContext(), 0, ii, 0);

                        //vamos criar alguem para gerenciar as notificacoes (um canal de comunicação)
                        NotificationManager notificationManager = (NotificationManager)
                                getSystemService(Context.NOTIFICATION_SERVICE);
                        String notificationChannelId = "my_channel_id_01";

                        //se o Android for o Oreo é diferente a forma das notificações
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel notificationChannel =
                                    new NotificationChannel(notificationChannelId, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

                            notificationChannel.setDescription("Channel description");
                            notificationChannel.enableLights(true);
                            notificationChannel.setLightColor(Color.RED);

                            notificationManager.createNotificationChannel(notificationChannel);
                        }
                        //para as versoes mais antigas
                        NotificationCompat.Builder builder =
                                new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

                        builder.setAutoCancel(true)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(data.get("title"))
                                .setContentText(data.get("body"))
                                .setContentIntent(pIntent);
                        //pedir para construir efetivamente a notificação
                        notificationManager.notify(1, builder.build());
                    }
                });
    }
}
//Precisamos declarar esse novo serviço no Manifest

