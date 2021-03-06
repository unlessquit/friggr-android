package com.unlessquit.friggr;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import okhttp3.ConnectionSpec;
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

    private Context ctx = null;
    private View snackParentView = null;
    private AppCompatActivity app = null;
    private SharedPreferences settings = null;


    public SendImageListener(AppCompatActivity app) {
        this.app = app;
        this.ctx = app.getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        snackParentView = app.findViewById(android.R.id.content);
    }


    @Override
    public void onClick(View view) {
        Uri imageUri = HelperFunctions.getUriFromIntent(app.getIntent());
        Log.d("FRIGGR", "Sending image");
        if (imageUri == null) {
            Snackbar.make(snackParentView, "Choose image to send", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            String userId = ((TextView) app.findViewById(R.id.user_id)).getText().toString();
            String caption = ((TextView) app.findViewById(R.id.captionText)).getText().toString();
            Log.d("FRIGGR", "Saving userId to settings: " + userId);
            settings.edit().putString("userId", userId).commit();
            Log.d("FRIGGR", "Image Uri: " + imageUri.toString());
            UploadImageTask uploadTask = new UploadImageTask();
            String filePath = HelperFunctions.getPath(imageUri, ctx);
            if (filePath.equals("")) {
                Snackbar.make(snackParentView, "I have problem to take over the image, please try it again or share it from different application.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
            uploadTask.execute(userId, filePath, caption);
        }
    }

    private class UploadImageTask extends AsyncTask<String, Integer, Integer> {
        private final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");
        private final OkHttpClient client;

        UploadImageTask() {
            ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
            client = new OkHttpClient.Builder()
                    .connectionSpecs(Collections.singletonList(spec))
                    .build();
        }

        protected Integer doInBackground(String... params) {
            Log.d("FRIGGR", "Building multipart POST request");
            if (params[0].equals("") || params[1].equals("")) {
                Log.d("FRIGGR", "File or userId is empty string: '" + params[1] + "', '" + params[0] + "'");

                return 1;
            }

            File sourceFile = new File(params[1]);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("photoFile", sourceFile.getName(),
                            RequestBody.create(MEDIA_TYPE_JPG, sourceFile))
                    .addFormDataPart("userId", params[0])
                    .addFormDataPart("caption", params[2])
                    .build();

            Request request = new Request.Builder()
                    .url("https://friggr.unlessquit.com/inbox")
                    .post(requestBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                UploadLogProcessor.writeUploadLogToJSON(params[1], true, ctx);
                Log.d("FRIGGR", "Photo has been uploaded");
                return 0;

            } catch (IOException e) {
                UploadLogProcessor.writeUploadLogToJSON(params[1], false, ctx);
                e.printStackTrace();
                return 1;
            }
        }


        protected void onPreExecute() {
            Snackbar.make(snackParentView, "Uploading photo...", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null).show();
        }

        protected void onPostExecute(Integer result) {
            if (result != 0) {
                Snackbar.make(snackParentView, "Photo has NOT been uploaded", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                ((ImageView) app.findViewById(R.id.status_Image)).setImageResource(R.drawable.image_sent_failed);
                return;
            }

            Snackbar.make(snackParentView, "Photo was successfully uploaded!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            ((ImageView) app.findViewById(R.id.status_Image)).setImageResource(R.drawable.image_sent);
        }
    }

}


