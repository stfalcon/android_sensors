package com.example.sasha;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by alexandr on 19.08.14.
 */
public class WriteService extends Service {
    private int NOTIFICATION = 1000;
    private OutputStreamWriter outputStreamWriterA;
    private OutputStreamWriter outputStreamWriterS;
    private OutputStreamWriter outputStreamWriterL;
    public WriteBinder binder = new WriteBinder();
    public static final int TYPE_A = 0;
    public static final int TYPE_S = 1;
    public static final int TYPE_L = 2;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void startListening() {
        startService(new Intent(this, WriteService.class));
        startForeground(NOTIFICATION, makeNotification());
        Log.v("Loger", "START_DONE");

        try {
            long time = System.currentTimeMillis();
            File myFileA = new File("/sdcard/AccelDataA" + time + ".txt");
            myFileA.createNewFile();
            FileOutputStream fOutA = new FileOutputStream(myFileA);
            File myFileS = new File("/sdcard/AccelDataS" + time + ".txt");
            myFileS.createNewFile();
            FileOutputStream fOutS = new FileOutputStream(myFileS);
            File myFileL = new File("/sdcard/AccelDataL" + time + ".txt");
            myFileL.createNewFile();
            FileOutputStream fOutL = new FileOutputStream(myFileL);
            outputStreamWriterA = new OutputStreamWriter(fOutA);
            outputStreamWriterS = new OutputStreamWriter(fOutS);
            outputStreamWriterL = new OutputStreamWriter(fOutL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void stopListening() {
        Log.v("Loger", "STOP_DONE");

        stopForeground(true);

        try {
            outputStreamWriterA.close();
            outputStreamWriterS.close();
            outputStreamWriterL.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    public void writeNewData(String data, int type) {
        try {
            switch (type) {
                case TYPE_A:
                    outputStreamWriterA.write(data);
                    break;
                case TYPE_S:
                    outputStreamWriterS.write(data);
                    break;
                case TYPE_L:
                    outputStreamWriterL.write(data);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Notification makeNotification() {

        Intent intent = new Intent(this, MyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_sys_upload)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText("Запис у файл")
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(contentIntent);

        mBuilder.setAutoCancel(true);
        return mBuilder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public class WriteBinder extends Binder {
        public WriteService getService() {
            return WriteService.this;
        }
    }
}
