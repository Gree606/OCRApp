package android.example.ocrapp;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class PlayAudio {
    private String fileName;

    public void playAudio(MediaPlayer mp, String fName){
        try{
            fileName=fName;
            if(mp.isPlaying()) {mp.stop();
                mp.release();}
            mp.setDataSource(fileName);
            Log.e("save path", fileName);
            mp.prepare();
            while(!mp.isPlaying()){
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mp.stop();
                        mp.release();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "prepare() failed for output audio");
        }
    }
}
