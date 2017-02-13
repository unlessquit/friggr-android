package com.unlessquit.friggr;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by dezider.mesko on 06/02/2017.
 */

public class HelperFunctions {
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

    /** Get absolute path from android resource URI **/
    public static String getPath(Uri uri, Context ctx) {
        Log.d("FRIGGR", uri.toString());
        String[] projection = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(ctx.getApplicationContext(), uri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    /** Extract URI from intent from different application,
     * as ADB cann't add extra_stream parameter,
     * to test it from console, there is a parameter stringUri **/
    @Nullable
    public static Uri getUriFromIntent(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            return imageUri;
        }
        String stringUri = intent.getStringExtra("stringUri");
        if (stringUri == null) return null;
        Uri testImageUri = Uri.parse(stringUri); //just for adb parameter support, -e stringUri "content://media/external/images/media/12"
        return testImageUri;
    }


}
