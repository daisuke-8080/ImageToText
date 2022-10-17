package com.example.imagetotext;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.os.HandlerCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.microsoft.azure.cognitiveservices.vision.computervision.*;
import com.microsoft.azure.cognitiveservices.vision.computervision.implementation.ComputerVisionImpl;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String subscriptionKey = BuildConfig.apiKey;
    private static final String endpoint = BuildConfig.ENDPOINT;

    private static Button buttonFolder;
    private static Button buttonCamera;
    private static Button buttonReload;
    private static Button buttonPost;
    private static TextView textTop;

    private static Uri resultUri;
    private static SubsamplingScaleImageView imageSelected;
    private static Bitmap resultBmp;

    private static String resultString;

    private interface CogApi {
        @POST("vision/v3.2/read/analyze")
        Call<Void> readWithByte(@QueryMap Map<String, String> params,
                                @HeaderMap Map<String, String> headers,
                                @Body RequestBody targetByte);
    }

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(endpoint)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static CogApi cogApi = retrofit.create(CogApi.class);

    private static ComputerVisionClient getResultClient = ComputerVisionManager.authenticate(subscriptionKey).withEndpoint(endpoint);
    private static ComputerVisionImpl vision = (ComputerVisionImpl) getResultClient.computerVision();

    private ActivityResultLauncher<String> selectLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri resultSelected) {
                    if (resultSelected == null){
                        Intent intentMain = new Intent(getApplication(), MainActivity.class);
                        startActivity(intentMain);
                        return;
                    }
                    resultUri = resultSelected;

                    resultBmp = correctBitmapOrientation(resultUri);

                    imageSelected.setImage(ImageSource.bitmap(resultBmp));
                    buttonReload.setVisibility(View.VISIBLE);
                    buttonPost.setVisibility(View.VISIBLE);

                }
            });


    private ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean resultCamera) {
                    if (!resultCamera){
                        Intent intentMain = new Intent(getApplication(), MainActivity.class);
                        startActivity(intentMain);
                        return;
                    }

                    resultBmp = correctBitmapOrientation(resultUri);

                    imageSelected.setImage(ImageSource.bitmap(resultBmp));
                    buttonReload.setVisibility(View.VISIBLE);
                    buttonPost.setVisibility(View.VISIBLE);

                    Log.d("debug", "camera done!");
                }
            });

    private File createImageFile() throws IOException {
        String timeTemp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        String imageFileName = "JPEG_" + timeTemp + "_ImageToText";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    public static Bitmap correctBitmapOrientation(Uri inputUri){
        try {
            InputStream stream = ImagetoText.getContext().getContentResolver().openInputStream(inputUri);
            ExifInterface exifInterface = new ExifInterface(stream);
            int orientationID = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Bitmap originalBmp = BitmapFactory.decodeFileDescriptor(ImagetoText.getContext().getContentResolver().openFileDescriptor(inputUri, "r").getFileDescriptor());
            float resizeScale;
            float width = Math.min(originalBmp.getWidth(), 1920);
            Matrix matrix = new Matrix();
            matrix.reset();

            switch(orientationID) {
                case 2:  // ORIENTATION_FLIP_HORIZONTAL 左右反転
                    resizeScale = width / originalBmp.getWidth();
                    matrix.postScale(resizeScale, -resizeScale);
                    matrix.postTranslate(0, originalBmp.getHeight() * resizeScale);
                    break;
                case 3:  // ORIENTATION_ROTATE_180 180度回転
                    resizeScale = width / originalBmp.getWidth();
                    matrix.postRotate(180, originalBmp.getWidth() / 2f, originalBmp.getHeight() / 2f);
                    matrix.postScale(resizeScale, resizeScale);
                    break;
                case 4:  // ORIENTATION_FLIP_VERTICAL 上下反転
                    resizeScale = width / originalBmp.getWidth();
                    matrix.setScale(-resizeScale, resizeScale);
                    matrix.postTranslate(originalBmp.getWidth() * resizeScale,0);
                    break;
                case 5:  // ORIENTATION_TRANSPOSE 270度回転+上下反転
                    resizeScale = width / originalBmp.getHeight();
                    matrix.postRotate(270, 0, 0);
                    matrix.postScale(resizeScale, -resizeScale);
                    break;
                case 6:  // ORIENTATION_ROTATE_90 90度回転
                    resizeScale = width / originalBmp.getHeight();
                    matrix.postRotate(90, 0, 0);
                    matrix.postScale(resizeScale, resizeScale);
                    matrix.postTranslate(originalBmp.getHeight() * resizeScale, 0);
                    break;
                case 7:  // ORIENTATION_TRANSVERSE 90度回転+90度反転
                    resizeScale = width / originalBmp.getHeight();
                    matrix.postRotate(90, 0, 0);
                    matrix.postScale(resizeScale, -resizeScale);
                    matrix.postTranslate(originalBmp.getHeight() * resizeScale, originalBmp.getWidth() * resizeScale);
                    break;
                case 8:  // ORIENTATION_ROTATE_270 270度回転
                    resizeScale = width / originalBmp.getHeight();
                    matrix.postRotate(270, 0, 0);
                    matrix.postScale(resizeScale, resizeScale);
                    matrix.postTranslate(0, originalBmp.getWidth() * resizeScale);
                    break;
                default: // ORIENTATION_NORMAL 変更不要
                    resizeScale = width / originalBmp.getWidth();
                    matrix.preScale(resizeScale, resizeScale);
            }

            Bitmap returnBmp = Bitmap.createBitmap(originalBmp, 0, 0, originalBmp.getWidth(), originalBmp.getHeight(), matrix, true);

            return returnBmp;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private OnBackPressedCallback BackButtonCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            Intent intentMain = new Intent(getApplication(), MainActivity.class);
            startActivity(intentMain);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getOnBackPressedDispatcher().addCallback(this, BackButtonCallBack);

        buttonFolder = findViewById(R.id.buttonFolder);
        buttonFolder.setOnClickListener(this);

        buttonCamera = findViewById(R.id.buttonCamera);
        buttonCamera.setOnClickListener(this);

        buttonReload = findViewById(R.id.buttonReload);
        buttonReload.setOnClickListener(this);
        buttonReload.setVisibility(View.INVISIBLE);

        buttonPost = findViewById(R.id.buttonPost);
        buttonPost.setOnClickListener(this);
        buttonPost.setVisibility(View.INVISIBLE);

        imageSelected = findViewById(R.id.postImage);

        textTop = findViewById(R.id.textTop);
    }

    /**
     * Extracts the OperationId from a Operation-Location returned by the POST Read operation
     *
     * @param operationLocation
     * @return operationId
     */
    private static String extractOperationIdFromOpLocation(String operationLocation) {
        if (operationLocation != null && !operationLocation.isEmpty()) {
            String[] splits = operationLocation.split("/");

            if (splits != null && splits.length > 0) {
                return splits[splits.length - 1];
            }
        }
        resultString = "解析に失敗しました";
        throw new IllegalStateException("Something went wrong: Couldn't extract the operation id from the operation location");
    }


    private static void ReadFromByte() {
        Log.i("MainActivity.java", "-----------------------------------------------");


        Log.i("MainActivity.java", "ReadFromByte Started!!!");

        try {
            // Cast Computer Vision to its implementation to expose the required methods

            Map<String, String> params = new HashMap<>();
            params.put("language", "ja");
            params.put("pages", "1");
            params.put("readingOrder", "basic");
            params.put("model-version", "latest");

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/octet-stream");
            headers.put("Ocp-Apim-Subscription-Key", subscriptionKey);

            //File file = new File(context.getFilesDir(), "res/drawable-v24/funny.jpeg");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resultBmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] ImageByte = baos.toByteArray();
            RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), ImageByte);

            Response<Void> ReadResponse = cogApi.readWithByte(params, headers, body).execute();

            if (!ReadResponse.isSuccessful()) {
                String url = ReadResponse.raw().request().url().toString();
                System.out.println("Http response error!!");
            }

            String operationLocation = ReadResponse.headers().get("Operation-Location");


            // Read in remote image and response header

            String operationId = extractOperationIdFromOpLocation(operationLocation);

            System.out.println("Polling for Read results ...");

            boolean pollForResult = true;
            ReadOperationResult readResults = null;

            while (pollForResult) {
                // Poll for result every second
                try {
                    Thread.sleep(1000);
                    readResults = vision.getReadResult(UUID.fromString(operationId));
                    // The results will no longer be null when the service has finished processing the request.
                    if (readResults != null) {
                        // Get request status
                        OperationStatusCodes status = readResults.status();

                        if (status == OperationStatusCodes.FAILED || status == OperationStatusCodes.SUCCEEDED) {
                            pollForResult = false;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Print read results, page per page
            for (ReadResult pageResult : readResults.analyzeResult().readResults()) {
                System.out.println("");
                System.out.println("Printing Read results for page " + pageResult.page());
                StringBuilder builder = new StringBuilder();

                for (Line line : pageResult.lines()) {
                    builder.append(line.text());
                    builder.append("\n");
                }

                resultString = builder.toString();
                System.out.println(builder.toString());
            }

        } catch (Exception e) {
            Log.i("MainActivity.java", e.getMessage());
            e.printStackTrace();
        }
    }


    private class PostBackG implements Runnable {
        private final Handler handlerResult;

        private PostBackG(Handler handler_) {
            handlerResult = handler_;
        }

        @Override
        public void run() {
            ReadFromByte();
            ShowPostResult showPostResult = new ShowPostResult();
            handlerResult.post(showPostResult);
            Log.i("MainActivity.java", "finish!!!!!");
        }
    }

    private class ShowPostResult implements Runnable {
        @Override
        public void run() {
            Intent intentPost = new Intent(getApplication(), Result.class);
            intentPost.putExtra("resultString", resultString);
            intentPost.putExtra("resultUri", resultUri);
            startActivity(intentPost);
        }
    }


    private class selectImageBackG implements Runnable {
        @Override
        public void run() {
            selectLauncher.launch("image/*");
        }
    }

    private class CameraBackG implements Runnable {
        @Override
        public void run() {

            cameraLauncher.launch(resultUri);

        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonFolder:
                selectImageBackG selectImageBackG = new selectImageBackG();
                ExecutorService selectExecutor = Executors.newSingleThreadExecutor();
                selectExecutor.submit(selectImageBackG);

                buttonFolder.setVisibility(View.INVISIBLE);
                buttonCamera.setVisibility(View.INVISIBLE);
                break;

            case R.id.buttonCamera:
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                if (photoFile != null) {
                    resultUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                    // Continue only if the File was successfully created
                }
                Log.i("MainActivity", resultUri.toString());

                CameraBackG cameraBackG = new CameraBackG();
                ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
                cameraExecutor.submit(cameraBackG);

                buttonFolder.setVisibility(View.INVISIBLE);
                buttonCamera.setVisibility(View.INVISIBLE);
                break;

            case R.id.buttonReload:
                Intent intentReload = new Intent(getApplication(), MainActivity.class);
                startActivity(intentReload);
                break;

            case R.id.buttonPost:
                Looper mainLooper = Looper.getMainLooper();
                Handler postResultHandler = HandlerCompat.createAsync(mainLooper);
                PostBackG postBG = new PostBackG(postResultHandler);
                ExecutorService postExecutor = Executors.newSingleThreadExecutor();
                Log.i("MainActivity.java", "POST開始");
                postExecutor.submit(postBG);

                textTop.setText("解析中です。しばらくお待ちください");
                buttonReload.setVisibility(View.INVISIBLE);
                buttonPost.setVisibility(View.INVISIBLE);
                break;

        }
    }
}

