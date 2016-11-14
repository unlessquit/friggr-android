package com.unlessquit.friggr;

import android.Manifest;
import android.app.Activity;
import android.content.CursorLoader;
import android.content.Intent;
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

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendToActivity extends AppCompatActivity {
    private Uri imageUri = null;
    private View snackParentView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_to);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        snackParentView = this.findViewById(android.R.id.content);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("FRIGGR", "Sending image");
                if (imageUri == null) {
                    Snackbar.make(view, "Choose image to send", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    TextView userIdInput = (TextView) findViewById(R.id.user_id);
                    String userId = userIdInput.getText().toString();
                    Log.d("FRIGGR", "Image Uri: " + imageUri.toString());
                    UploadImageTask uploadTask = new UploadImageTask();
                    uploadTask.execute(userId, getPath(imageUri));
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
                    .build();

            Request request = new Request.Builder()
                    .url("http://uq.maio.cz/inbox")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                Log.d("FRIGGR", "Photo has been uploaded");
                return 0;

            } catch (IOException e) {
                e.printStackTrace();
                return 1;
            }

        }

        protected void onPreExecute() {
            Snackbar.make(snackParentView, "Initiating image upload...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

        protected void onPostExecute(Integer result) {
            if (result != 0) {
                Snackbar.make(snackParentView, "Photo has NOT been uploaded", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }

            Snackbar.make(snackParentView, "Photo was successfully uploaded!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
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
