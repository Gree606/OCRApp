package android.example.ocrapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    Button clickButton;
    PreviewView previewView;
    Response response;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    Spinner spinner_in;
    Spinner spinner_out;
    ImageButton playButton;
    ImageButton replayButton;
    ProgressBar tracker;
    MediaPlayer mp;
    Boolean mpStat=false;
    String fileName;
    Boolean isProcessing=false;

    private static final int REQUEST_CAMERA_PERMISSION= 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clickButton=(Button) findViewById(R.id.Capture);
        previewView=(PreviewView) findViewById(R.id.previewView);
        playButton=(ImageButton)findViewById(R.id.PlayButton);
//        replayButton=(ImageButton)findViewById(R.id.ReplayButton);
        tracker=(ProgressBar)findViewById(R.id.progressBar);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSION);

        fileName=getApplicationContext().getExternalCacheDir().getAbsolutePath()+"OCRaudio.3gp";

        spinner_in=findViewById(R.id.spinner_input_languages);
        ArrayAdapter<String> myInAdapter=new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.inlangs));
        myInAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner_in.setAdapter(myInAdapter);

        spinner_out=findViewById(R.id.spinner_output_languages);
        ArrayAdapter<String> myOutAdapter=new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.outlangs));
        myOutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner_out.setAdapter(myOutAdapter);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((mpStat==false)&&!isProcessing) {
                    playButton.setImageResource(R.drawable.stopbutton);
                    playAudio(fileName);
                }
                else if ((mpStat==true)&&!isProcessing){
                    playButton.setImageResource(R.drawable.playbutton);
                        if (mp.isPlaying()) {
                            mp.stop();
                            mp.release();
//                                player = new MediaPlayer();
                            mpStat = false;
                        }
                    }
                else{
                    Toast.makeText(MainActivity.this, "Hold up! getting results", Toast.LENGTH_SHORT).show();
                }

            }
        });

        cameraProviderFuture= ProcessCameraProvider.getInstance(this);
        String photoFilePath= getApplicationContext().getExternalFilesDir(null)+ File.separator+"/EspCheckPhoto.jpg";
        Log.e("filePath",photoFilePath);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider=cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        },getExecuter());

        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if(isProcessing==false) {
                  tracker.setVisibility(View.VISIBLE);
                  isProcessing = true;
                  playButton.setImageResource(R.drawable.playbutton);
                  if (mpStat == true) {
                      if (mp.isPlaying()) {
                          mp.stop();
                          mp.release();
//                                player = new MediaPlayer();
                          mpStat = false;
                      }
                  }
                  capturePhoto();
              }
              else {
                  Toast.makeText(MainActivity.this, "Hold up! getting results", Toast.LENGTH_SHORT).show();
              }
            }
        });
    }

    private Executor getExecuter() {
        return ContextCompat.getMainExecutor(this);
    }

    public void startCameraX(ProcessCameraProvider cameraProvider){
        cameraProvider.unbindAll();

        CameraSelector cameraSelector=new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        Preview preview=new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageCapture);
    }

    public void capturePhoto() {

        String photoFilePath = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/EspCheckPhoto.jpg";
        Log.e("filePath", photoFilePath);
        File photoFile = new File(photoFilePath);


        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(), getExecuter(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "Photo saved successfully in:" + photoFilePath, Toast.LENGTH_SHORT).show();
                        Log.e("File Saved in", photoFilePath);
                        String inputLangSelected = spinner_in.getSelectedItem().toString().toLowerCase();
                        String outLangSelected=spinner_out.getSelectedItem().toString().toLowerCase();

                        String reqSend="{\"src_lang\":\""+inputLangSelected+"\",\"targ_lang\":\""+outLangSelected+"\"}";
                        Log.e("RequestSend:",reqSend);

                        if (photoFile.exists()) {
                            Thread thread=new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                                    builder.connectTimeout(30, TimeUnit.SECONDS); // Connection timeout
                                    builder.readTimeout(30, TimeUnit.SECONDS);    // Read timeout
                                    builder.writeTimeout(30, TimeUnit.SECONDS);   // Write timeout
                                    OkHttpClient client = builder.build();
                                    MediaType mediaType = MediaType.parse("text/plain");
                                    RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                                            .addFormDataPart("image","/C:/Users/Greeshma/OneDrive/Pictures/707 desktop wallpaper.jpg",
                                                    RequestBody.create(new File(photoFilePath),MediaType.parse("application/octet-stream")))
                                            .addFormDataPart("data", null,
                                                    RequestBody.create(reqSend.getBytes(), MediaType.parse("application/json"))).build();
                                    Request request = new Request.Builder()
                                            .url(OCRUrl)
                                            .method("POST", body)
                                            .build();
                                    try {

                                        response = client.newCall(request).execute();
                                        String responseData = response.body().string();
                                        Log.e("Response of TTS",responseData);
                                        JSONObject jObj = null;
                                        try {
                                            jObj = new JSONObject(responseData);
                                            String respStat=jObj.getString("status");
                                            if(respStat.equalsIgnoreCase("success")) {
                                                String resAudio = jObj.getString("audio");
                                                Log.e("Response success", resAudio);
                                                byte[] decodedBytes = Base64.getDecoder().decode(resAudio);
                                                saveAudio(decodedBytes);
                                            }
                                            else{
                                                Log.e("ResponseFail", "Check if API is returning proper value");
                                                tracker.setVisibility(View.INVISIBLE);
                                                isProcessing=false;
                                            }

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            Log.e("ResponseFail", "Check if API is returning value");
                                            tracker.setVisibility(View.INVISIBLE);
                                            isProcessing=false;
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        tracker.setVisibility(View.INVISIBLE);
                                        isProcessing=false;
                                    }

                                }

                            });
                            thread.start();

                         }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Photo not saved" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void saveAudio(byte[] audioFile) {


        OutputStream os;
        try {
            os = new FileOutputStream(new File(fileName));
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);
            outFile.write(audioFile);
            outFile.flush();
            outFile.close();
            playAudio(fileName);
            tracker.setVisibility(View.INVISIBLE);
            isProcessing=false;
        } catch (IOException e) {
            //Toast.makeText(context, "Add audio file", Toast.LENGTH_LONG).show();
            Log.e("TAG", "failed to get audio output");
        }

    }

    //////////////////////////////Play the audio file//////////////////////
    public void playAudio(String outAudFile) {
        try {
            if (mpStat == true) {
                if (mp.isPlaying()) {
                    mp.stop();
                    mp.release();
                }
            }
            mp = new MediaPlayer();
            mp.setDataSource(outAudFile);
            Log.e("save path", outAudFile);
            mp.prepare();
            while (!mp.isPlaying()) {
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playButton.setImageResource(R.drawable.playbutton);
                            mpStat=false;
                        }
                    });
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playButton.setImageResource(R.drawable.stopbutton);
                    }
                });

                mpStat = true;
//            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mediaPlayer) {
//                        mp.stop();
//                        mp.release();
//                    }
//                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "prepare() failed for output audio");
        }
    }
}