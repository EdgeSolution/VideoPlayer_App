package com.adv.videoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.adv.localmqtt.MQTTWrapper;
import com.bumptech.glide.Glide;
import com.adv.videoplayerlib.NiceVideoPlayer;
import com.adv.videoplayerlib.NiceVideoPlayerManager;
import com.adv.videoplayerlib.TxVideoPlayerController;
import com.adv.videoplayer.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FullWindowPlayActivity extends AppCompatActivity {

    private static NiceVideoPlayer mNiceVideoPlayer;
    private static String TAG = "FullWindowPlayActivity";
    private MQTTWrapper mqttWrapper = null;
    private String mqttClientId = "com.adv.videoplayer";
    public static String videoPath = "/data/AndroidDM/video/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_window_play);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //String command = "ps |grep mosquitto";
                        String command = "ps";
                        if (commandIsRuning(command)) {
                            break;
                        }
                        Log.d(TAG, "ps command ...");
                        Thread.sleep(3000);
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "connectMqttBroker ...");
                connectMqttBroker();
            }
        }).start();

        init();

        //MainActivity.fullWindowActivity = this;
    }

    private void connectMqttBroker() {
        Log.d(TAG, "--> onCreate");
        mqttWrapper = new MQTTWrapper(mqttClientId);
        boolean ret = mqttWrapper.connect(new MqttMessageReceiver(mqttWrapper,mqttClientId));
        if (ret) {
            Log.d(TAG, "--> mqtt connected");
        } else {
            Log.e(TAG, "--> mqtt no connect, return");
        }
    }

    private void init() {

        mNiceVideoPlayer = (NiceVideoPlayer) findViewById(R.id.nice_video_player);
        mNiceVideoPlayer.setPlayerType(NiceVideoPlayer.TYPE_IJK); // IjkPlayer or MediaPlayer
        //String videoUrl = "http://8537.vod.myqcloud.com/8537_a58316ea86df11e6a76c67b1ff29c014.f30.mp4?sign=5493ad997e153e8092de307e338b1774&t=5822a346";
        String videoUrl = "local";
        mNiceVideoPlayer.setUp(videoUrl, null);
        TxVideoPlayerController controller = new TxVideoPlayerController(this);
        controller.setTitle("Advantech");
        //controller.setLenght(0);
        Glide.with(this)
                .load(R.drawable.img_default)
                .placeholder(R.drawable.img_default)
                .crossFade()
                .into(controller.imageView());//static png for interface
        mNiceVideoPlayer.setController(controller);
        mNiceVideoPlayer.enterFullScreen();

        //MqttV3MessageReceiver.nvp = mNiceVideoPlayer;
        //mNiceVideoPlayer.isPlaying();
    }

    public  static NiceVideoPlayer getNiceVideoPlayerInstance(){

        return mNiceVideoPlayer;
    }

    /*    public void enterTinyWindow(View view) {
            if (mNiceVideoPlayer.isIdle()) {
                Toast.makeText(this, "要点击播放后才能进入小窗口", Toast.LENGTH_SHORT).show();
            } else {
                mNiceVideoPlayer.enterTinyWindow();
            }
        }*/

    public static String getVideoPath() {
        return videoPath;
    }

    public static List<String> getFilesAllName(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            Log.e("error", "empty dir!");
            return null;
        }
        List<String> s = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains(".mp4")) {
                s.add(files[i].getName());
            }
        }

        return s;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        //NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
        //NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "onRestart");
        init();
        //NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        //NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
        NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed");
        //if (NiceVideoPlayerManager.instance().onBackPressd()) return;
        if(mqttWrapper!=null) {
            mqttWrapper.destroy();
        }
        NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        if(mqttWrapper!=null) {
            mqttWrapper.destroy();
        }
        NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
        super.onDestroy();
    }

    private boolean commandIsRuning(String command) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(command);
        InputStream inputstream = proc.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
        String line = "";
        StringBuilder sb = new StringBuilder(line);
        while ((line = bufferedreader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        try {
            if (proc.waitFor() != 0) {
                Log.e(TAG,"Command exit value = " + proc.exitValue());
                return false;
            }
            //Log.d(TAG,"StringBuilder: " + sb);
            if(sb.toString().contains("mosquitto")){
                return true;
            }
            return false;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
