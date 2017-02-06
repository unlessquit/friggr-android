package com.unlessquit.friggr;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by dezider.mesko on 06/02/2017.
 */

public class SendImageListener implements View.OnClickListener {

    Context applicationContext;


    public SendImageListener(Context applicationContext) {
        this.applicationContext = applicationContext;

    }


    @Override
    public void onClick(View view) {
        Log.d("FRIGGR", "Sending image");
        if (imageUri == null) {
            Snackbar.make(view, "Choose image to send", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            String userId = ((TextView) findViewById(R.id.user_id)).getText().toString();
            String description = ((TextView) findViewById(R.id.captionText)).getText().toString();
            Log.d("FRIGGR", "Saving userId to settings: " + userId);
            settings.edit().putString("userId", userId).commit();
            Log.d("FRIGGR", "Image Uri: " + imageUri.toString());
            UploadImageTask uploadTask = new UploadImageTask();
            uploadTask.execute(userId, HelperFunctions.getPath(imageUri, applicationContext), description);
        }
    }

    private class UploadImageTask extends AsyncTask<String, Integer, Integer> {
        private final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");
        private final OkHttpClient client = new OkHttpClient();

        protected Integer doInBackground(String... params) {
            Log.d("FRIGGR", "Building multipart POST request");
            String userId = params[0];
            File sourceFile = new File(params[1]);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("photoFile", sourceFile.getName(),
                            RequestBody.create(MEDIA_TYPE_JPG, sourceFile))
                    .addFormDataPart("userId", userId)
                    .addFormDataPart("caption", params[2])
                    .build();

            Request request = new Request.Builder()
                    .url("http://uq.maio.cz/inbox")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                UploadLogProcessor.writeUploadLogToJSON(params[1], true, applicationContext);
                Log.d("FRIGGR", "Photo has been uploaded");
                return 0;

            } catch (IOException e) {
                UploadLogProcessor.writeUploadLogToJSON(params[1], false, applicationContext);
                e.printStackTrace();
                return 1;
            }
        }
    }

}


