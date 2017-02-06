package com.unlessquit.friggr;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static java.util.Arrays.asList;

/**
 * Created by dezider.mesko on 06/02/2017.
 */

public class UploadLogProcessor {

    public static UploadLog createLogItem(String pictureName, String uploadResult) {
        UploadLog ul = new UploadLog();
        ul.fileName = pictureName;

        //http://stackoverflow.com/questions/8745297/want-current-date-and-time-in-dd-mm-yyyy-hhmmss-ss-format
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String strDate = sdf.format(cal.getTime());

        ul.uploadDate = strDate;

        ul.uploadStatus = uploadResult;
        return ul;
    }

    public static void writeUploadLogToJSON(String pictureName, Boolean success, Context ctx) {
        String FILENAME = "filesUpload.log";
        String uploadResult = "FAILED";
        if (success) {
            uploadResult = "SUCCESS";
        }
        ArrayList currentLog = readUploadLogFromJSON(ctx);
        File file = new File(pictureName);
        currentLog.add(0, createLogItem(file.getName(), uploadResult));

        try {
            FileOutputStream fos = ctx.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

            Gson gson = new Gson();
            // convert list to array
            ArrayList al = new ArrayList<UploadLog>(currentLog);
            gson.toJson(al.toArray(), writer);
            writer.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    public static ArrayList<UploadLog> readUploadLogFromJSON(Context ctx) {
        String FILENAME = "filesUpload.log";
        ArrayList<UploadLog> emptyList = new ArrayList();
        emptyList.add(0, createLogItem("No image uploaded yet", ""));


        try {
            FileInputStream fis = ctx.openFileInput(FILENAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            Gson gson = new Gson();
            UploadLog[] log = gson.fromJson(reader, UploadLog[].class);
            reader.close();
            fis.close();
            ArrayList<UploadLog> logList = new ArrayList<UploadLog>(asList(log));

            if(logList.size() == 0) {
                return emptyList;
            }

            return logList;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return emptyList;
    }

}
