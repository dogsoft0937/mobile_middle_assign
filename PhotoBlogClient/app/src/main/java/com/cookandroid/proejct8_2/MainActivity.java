package com.cookandroid.proejct8_2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    ImageView imgView;
    TextView textView;
    String site_url = "http://10.0.2.2:8000";
    JSONObject post_json;

    CloadImage taskDownload;
    String selectedImagePath; // 선택한 이미지 경로를 저장할 변수 추가

    // ActivityResultLauncher 선언
    ActivityResultLauncher<Intent> getContentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        imgView = findViewById(R.id.imageView); // 이미지 뷰 초기화
        checkPermissions(); // 권한 확인 메소드 호출
        checkDownloadFolder();

        // ActivityResultLauncher 초기화
        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        selectedImagePath = getRealPathFromURI(imageUri); // URI를 파일 경로로 변환

                        // 이미지 미리보기 추가
                        Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);
                        imgView.setImageBitmap(bitmap); // 이미지 미리보기
                    }
                }
        );
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_SHORT).show();
    }

    public void onClickUpload(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.upload_image_dialog, null);
        builder.setView(dialogView);

        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        Button buttonSelectImage = dialogView.findViewById(R.id.buttonSelectImage);
        Button buttonUpload = dialogView.findViewById(R.id.buttonUpload);

        AlertDialog dialog = builder.create();

        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadNewImage();
            }
        });

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTextTitle.getText().toString();
                if (!title.isEmpty() && selectedImagePath != null) {
                    // 업로드 처리 함수 호출 (이미지 파일 경로를 넘김)
                    String authorId = "사용자ID"; // 작성자 ID 설정 (예: 하드코딩 또는 동적 할당 필요)
                    new PutPost().execute(site_url + "/api_root/Post/", title, "", selectedImagePath, authorId);
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "제목과 이미지를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void checkDownloadFolder() {
        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadFolder.exists() && downloadFolder.isDirectory()) {
            File[] files = downloadFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    Log.d("DownloadFolder", "File: " + file.getName());
                }
            } else {
                Log.d("DownloadFolder", "No files found in the download folder.");
            }
        } else {
            Log.d("DownloadFolder", "Download folder does not exist.");
        }
    }

    private void uploadNewImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        getContentLauncher.launch(intent); // ActivityResultLauncher 사용
    }

    private String getRealPathFromURI(Uri uri) {
        String filePath = "";
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(column_index);
            cursor.close();
        } else {
            // 커서가 null인 경우는 Uri가 외부 스토리지가 아닐 때 발생할 수 있으므로
            filePath = uri.getPath(); // URI의 경로를 직접 반환
        }
        return filePath;
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);
                        String imageURL = post_json.getString("image");
                        if (!imageURL.equals("")) {
                            URL myImageURL = new URL(imageURL);
                            conn = (HttpURLConnection) myImageURL.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            bitmapList.add(imageBitmap);
                            imgStream.close();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images.isEmpty()) {
                textView.setText("No image");
            } else {
                textView.setText("이미지 로드 성공");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(images);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private class PutPost extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String apiUrl = params[0];  // API URL
            String title = params[1];    // 게시글 제목
            String text = params[2];     // 게시글 내용 (필요한 경우 빈 문자열로 전달)
            String imagePath = params[3]; // 이미지 경로
            String authorId = params[4];  // 작성자 ID (사용자 ID)
            String token = "your_token";  // 인증 토큰

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json");  // 추가된 헤더
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=--boundary");

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // 제목과 내용 작성
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n");
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n" + text + "\r\n");

                // 이미지 파일 업로드
                FileInputStream fileInputStream = new FileInputStream(new File(imagePath));
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + new File(imagePath).getName() + "\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fileInputStream.close();
                dos.writeBytes("\r\n");
                dos.writeBytes("--boundary--\r\n");

                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                Log.d("PutPost", "Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("PutPost", "Post uploaded successfully.");
                } else {
                    Log.d("PutPost", "Post upload failed.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
