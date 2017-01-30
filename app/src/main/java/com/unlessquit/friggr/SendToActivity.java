package com.unlessquit.friggr;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.util.Arrays.*;

public class SendToActivity extends AppCompatActivity {
    private Uri imageUri = null;
    private View snackParentView = null;
    private SharedPreferences settings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_to);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        snackParentView = this.findViewById(android.R.id.content);

        settings = getPreferences(MODE_PRIVATE);
        String userId = settings.getString("userId", "test");
        Log.d("FRIGGR", "userId settings value: " + userId);
        ((TextView) findViewById(R.id.user_id)).setText(userId);


        //((TextView) findViewById(R.id.recentLogItem)).setText(readUploadLogFromJSON().get(0).toString());



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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
                    uploadTask.execute(userId, getPath(imageUri), description);
                }
            }
        });

        // https://developer.android.com/training/sharing/receive.html
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        imageUri = getUriFromIntent(intent);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            verifyStoragePermissions(this);
            if (type.startsWith("image/")) {
                setImageView(intent);
            }
        }
    }

    private void setImageView(Intent intent) {
        Uri image = getUriFromIntent(intent);
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);
        Log.d("FRIGGR", "Image path: " + image.getPath());
        ImageView mImageView;
        mImageView = (ImageView) findViewById(R.id.imagePreview);
        mImageView.setImageURI(image);
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public String getPath(Uri uri) {
        Log.d("FRIGGR", uri.toString());
        String[] projection = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), uri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    Uri getUriFromIntent(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            return imageUri;
        }
        String stringUri = intent.getStringExtra("stringUri");
        if (stringUri == null) return null;
        Uri testImageUri = Uri.parse(stringUri); //just for adb parameter support, -e stringUri "content://media/external/images/media/12"
        return testImageUri;
    }

    private UploadLog createLogItem(String pictureName, String uploadResult) {
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

    private void writeUploadLogToJSON(String pictureName, Boolean success) {
        String FILENAME = "filesUpload.log";
        String uploadResult = "FAILED";
        if (success) {
            uploadResult = "SUCCESS";
        }
        ArrayList currentLog = readUploadLogFromJSON();
        File file = new File(pictureName);
        currentLog.add(0, createLogItem(file.getName(), uploadResult));

        try {
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
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

    private ArrayList<UploadLog> readUploadLogFromJSON() {
        String FILENAME = "filesUpload.log";
        ArrayList<UploadLog> emptyList = new ArrayList();
        emptyList.add(0, createLogItem("No image uploaded yet", ""));


        try {
            FileInputStream fis = openFileInput(FILENAME);
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
                writeUploadLogToJSON(params[1], true);
                Log.d("FRIGGR", "Photo has been uploaded");


                return 0;

            } catch (IOException e) {
                writeUploadLogToJSON(params[1], false);
                e.printStackTrace();
                return 1;
            }

        }


        protected void onPreExecute() {
            Snackbar.make(snackParentView, "Uploading photo...", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null).show();
        }

        protected void onPostExecute(Integer result) {
            //((TextView) findViewById(R.id.recentLogItem)).setText(readUploadLogFromJSON().get(0).toString());
            if (result != 0) {
                Snackbar.make(snackParentView, "Photo has NOT been uploaded", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                ((ImageView)findViewById(R.id.status_Image)).setImageResource(R.drawable.image_sent_failed);
                return;
            }

            Snackbar.make(snackParentView, "Photo was successfully uploaded!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            ((ImageView)findViewById(R.id.status_Image)).setImageResource(R.drawable.image_sent);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_send_to, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
