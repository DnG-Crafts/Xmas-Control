package dngsoftware.xmascontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.exifinterface.media.ExifInterface;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class Functions {

    public static void showToast(Context context, String tMsg) {
        try {
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, tMsg, Toast.LENGTH_SHORT).show());
        } catch (Exception ignored) {
        }
    }

    public static void showToast(Context context, @StringRes int tMsg) {
        try {
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, tMsg, Toast.LENGTH_SHORT).show());
        } catch (Exception ignored) {
        }
    }

    public static String capFirst(String inStr) {
        return inStr.substring(0, 1).toUpperCase() + inStr.substring(1).toLowerCase();
    }

    @SuppressLint("DefaultLocale")
    public static String secToTime(int sec) {
        int seconds = sec % 60;
        int minutes = sec / 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            minutes %= 60;
            if (hours >= 24) {
                int days = hours / 24;
                return String.format("%d days %02d:%02d:%02d", days, hours % 24, minutes, seconds);
            }
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static Spanned fromHtml(String text) {
        Spanned result;
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(text);
        }
        return result;
    }


    public static List<String> convertJsonArrayToList(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }

        return list;
    }

    public static String secondsToTime(int seconds) {
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;
        return String.format(Locale.ENGLISH, "%02d:%02d", minutes, remainingSeconds);
    }

    public static int stepTimeToFPS(int StepTime) {
        return 1000 / StepTime;
    }

    public static int stepTimeToSeconds(int StepTime, int Frames) {
        int fps = stepTimeToFPS(StepTime);
        return Frames / fps;
    }

    public static String sendFalconCommand(String Host, String e,String t,String n,String s,String i, String r)
    {
        return postRESTCommand(Host,"/api","{\"T\":\"" + e + "\",\"M\":\"" + t + "\",\"B\":" + n + ",\"E\":" + s + ",\"I\":" + i + ",\"P\":" + r + "}");
    }

    public static String sendFalconCommand(String Host, String e,String t,String n,String s,String i)
    {
        return postRESTCommand(Host,"/api","{\"T\":\"" + e + "\",\"M\":\"" + t + "\",\"B\":" + n + ",\"E\":" + s + ",\"I\":" + i + ",\"P\":{}}");
    }
    public static String sendFalconCommand(String Host, String e,String t,String n,String s)
    {
        return postRESTCommand(Host,"/api","{\"T\":\"" + e + "\",\"M\":\"" + t + "\",\"B\":" + n + ",\"E\":" + s + ",\"I\":0,\"P\":{}}");
    }

    public static String sendFalconCommand(String Host, String e,String t,String n)
    {
        return postRESTCommand(Host,"/api","{\"T\":\"" + e + "\",\"M\":\"" + t + "\",\"B\":" + n + ",\"E\":0,\"I\":0,\"P\":{}}");
    }

    public static String sendFalconCommand(String Host, String e,String t)
    {
        return postRESTCommand(Host,"/api","{\"T\":\"" + e + "\",\"M\":\"" + t + "\",\"B\":0,\"E\":0,\"I\":0,\"P\":{}}");
    }

    public static String getRESTCommand(final String Host, String Command)
    {
        URL url;
        HttpURLConnection urlConnection;
        String server_response = "";
        try {
            if (!Command.startsWith("/")){
                Command = "/" + Command;
            }
            url = new URL( "http://" + Host +  Command);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestProperty("Accept-Language", "en-us");
            urlConnection.setRequestProperty("Accept", "*/*");
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:72.0) Gecko/20100101 Firefox");
            urlConnection.setRequestProperty("Connection", "close");
            final int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                server_response = readStream(urlConnection.getInputStream());
            } else {
                server_response = "[]";
            }
        }
        catch (Exception e)
        {
            server_response = "[]";
        }
        return server_response;
    }


    public static String postRESTCommand(final String Host, String Command, final String Content)
    {
        URL url;
        HttpURLConnection urlConnection;
        String server_response = "";
        try {
            if (!Command.startsWith("/")){
                Command = "/" + Command;
            }
            url = new URL("http://" + Host +  Command);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:72.0) Gecko/20100101 Firefox");
            urlConnection.setRequestProperty("Accept-Language", "en-us");
            urlConnection.setRequestProperty("Accept", "*/*");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Content-Length", Integer.toString(Content.getBytes().length));
            urlConnection.setRequestProperty("Connection", "keep-alive");

            if (!Content.isEmpty()) {
                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write(Content);
                writer.flush();
                writer.close();
                os.close();
            }

            urlConnection.connect();

            final int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED  || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                server_response = readStream(urlConnection.getInputStream());
            } else {
                server_response = "[]";
            }
        }
        catch (Exception e)
        {
            server_response = "[]";
        }
        return server_response;
    }

    public static boolean isAvailable(String urlString){
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.connect();
            urlConnection.disconnect();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static String readStream(InputStream in)
    {
        try {
            int len;
            byte[] buf = new byte[ 1024 ];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while((len = in.read(buf)) > 0)
            {
                outputStream.write(buf, 0, len);
            }
            in.close();
            return outputStream.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }


    public static void loadBrowser(Context context, String webURL)
    {
        if (!webURL.isEmpty()) {
            Intent intent = new Intent(context, WebActivity.class);
            intent.putExtra("URL", webURL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static void loadFppActivity(Context context, String rHost)
    {
        if (!rHost.isEmpty()) {
            Intent intent = new Intent(context, FppActivity.class);
            intent.putExtra("HOST", rHost);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static void loadFalconActivity(Context context, String rHost)
    {
        if (!rHost.isEmpty()) {
            Intent intent = new Intent(context, FalconActivity.class);
            intent.putExtra("HOST", rHost);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static Bitmap loadBitmap(String fileName) {
        Bitmap bitmap = null;
        if (!fileName.isEmpty()) {
            fileName = fileName.replace("//","/");
            try {
                FileInputStream inputStream = new FileInputStream(new File(fileName));
                if (inputStream.available() > 0) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                }
            } catch (Exception ignored) {}
        }
        return bitmap;
    }

    public static Bitmap scaleBitmap(Bitmap bitmapToScale, float newWidth, float newHeight) {
        try {
            if (bitmapToScale == null)
                return null;
            int width = bitmapToScale.getWidth();
            int height = bitmapToScale.getHeight();
            Matrix matrix = new Matrix();
            matrix.postScale(newWidth / width, newHeight / height);
            return Bitmap.createBitmap(bitmapToScale, 0, 0, bitmapToScale.getWidth(), bitmapToScale.getHeight(), matrix, true);
        } catch (Exception ignored) {
            return bitmapToScale;
        }
    }


    public static void copyBitmap(String sourcePath, String destinationPath) {
        try
        {
            FileInputStream inStream = new FileInputStream(sourcePath);
            FileOutputStream outStream = new FileOutputStream(destinationPath);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
        }
        catch (IOException ignored){}
    }


    public static int getCameraPhotoOrientation(String imagePath) {
        int rotate = 0;
        try {
            ExifInterface exif  = null;
            try {
                exif = new ExifInterface(imagePath);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 90;
                    break;
                default:
                    rotate = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }


    public static boolean checkReadImagePermission(Context context)
    {
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        }else
        {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ignored) {}
        return s;
    }


    @SuppressLint({"NewApi", "Recycle"})
    public static String getFilePath(Context context, final Uri uri) {
        Uri contentUri = null;
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String selection;
        String[] selectionArgs;
        // DocumentProvider
        if (isKitKat ) {
            // ExternalStorageProvider

            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");

                String fullPath = getPathFromExtSD(split);
                if (!fullPath.isEmpty()) {
                    return fullPath;
                } else {
                    return null;
                }
            }

            if (isDownloadsDocument(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String id;
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String fileName = cursor.getString(0);
                            String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            if (!TextUtils.isEmpty(path)) {
                                return path;
                            }
                        }
                    }
                    finally {
                        if (cursor != null)
                            cursor.close();
                    }
                    id = DocumentsContract.getDocumentId(uri);
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        String[] contentUriPrefixesToTry = new String[]{
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads"
                        };
                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));
                                return getDataColumn(context, contentUri, null, null);
                            } catch (NumberFormatException e) {
                                //In Android 8 and Android P the id is not a number
                                return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }
                    }
                }
                else {
                    final String id = DocumentsContract.getDocumentId(uri);

                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
                        contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (contentUri != null) {
                        return getDataColumn(context, contentUri, null, null);
                    }
                }
            }


            // MediaProvider
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(context, uri);
            }
            if(isWhatsAppFile(uri)){
                return getFilePathForWhatsApp(context, uri);
            }

            if ("content".equalsIgnoreCase(uri.getScheme())) {
                if (isGooglePhotosUri(uri)) {
                    return uri.getLastPathSegment();
                }
                if (isGoogleDriveUri(uri)) {
                    return getDriveFilePath(context, uri);
                }
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    // return getFilePathFromURI(context,uri);
                    return copyFileToInternalStorage(context,uri,"userfiles");
                    // return getRealPathFromURI(context,uri);
                }
                else
                {
                    return getDataColumn(context, uri, null, null);
                }
            }
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }
        else {
            if(isWhatsAppFile(uri)){
                return getFilePathForWhatsApp(context, uri);
            }
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {
                        MediaStore.Images.Media.DATA
                };
                try {
                    Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private static String getPathFromExtSD(String[] pathData) {
        final String type = pathData[0];
        final String relativePath = "/" + pathData[1];
        String fullPath;
        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }
        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }
        return fullPath;
    }

    @SuppressLint("Recycle")
    private static String getDriveFilePath(Context context, Uri uri) {
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        //   int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        // String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getCacheDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read;
            int maxBufferSize = (1024 * 1024);
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception ignored) {}
        return file.getPath();
    }


    @SuppressLint("Recycle")
    private static String copyFileToInternalStorage(Context context, Uri uri, String newDirName) {
        Cursor returnCursor = context.getContentResolver().query(uri, new String[]{
                OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE
        }, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));

        File output;
        if(!newDirName.equals("")) {
            File dir = new File(context.getFilesDir() + "/" + newDirName);
            if (!dir.exists()) {
                dir.mkdir();
            }
            output = new File(context.getFilesDir() + "/" + newDirName + "/" + name);
        }
        else{
            output = new File(context.getFilesDir() + "/" + name);
        }
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(output);
            int read = 0;
            int bufferSize = 1024;
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            inputStream.close();
            outputStream.close();
        }
        catch (Exception ignored) {}
        return output.getPath();
    }

    private static String getFilePathForWhatsApp(Context context, Uri uri){
        return  copyFileToInternalStorage(context,uri,"whatsapp");
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isWhatsAppFile(Uri uri){
        return "com.whatsapp.provider.media".equals(uri.getAuthority());
    }

    private static boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }

    public static boolean GetSetting(Context context, String sKey, boolean bDefault)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getBoolean(sKey, bDefault);
    }

    public static void SaveSetting(Context context, String sKey, boolean bValue)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(sKey, bValue);
        editor.apply();
    }

}
