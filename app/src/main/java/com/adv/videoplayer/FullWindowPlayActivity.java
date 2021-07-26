package com.adv.videoplayer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.adv.localmqtt.MQTTWrapper;
import com.adv.videoplayerlib.FileUtil;
import com.adv.videoplayerlib.NiceVideoPlayer;
import com.adv.videoplayerlib.NiceVideoPlayerManager;
import com.adv.videoplayerlib.TxVideoPlayerController;
import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FullWindowPlayActivity extends AppCompatActivity {

    private static NiceVideoPlayer mNiceVideoPlayer;
    private static String TAG = "FullWindowPlayActivity";
    private MQTTWrapper mqttWrapper = null;
    private String mqttClientId = "com.adv.videoplayer";
    private final int REQ_RECORD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_window_play);
        if (isStoragePermissionGranted()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            //String command = "ps |grep mosquitto";
                            String command = "ps";
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                String mosquittoIsRunning = getSystemStringProperties(FullWindowPlayActivity.this, "adv.mosquittoIsRunning", "false");
                                if (mosquittoIsRunning != null && !mosquittoIsRunning.isEmpty() && mosquittoIsRunning.equals("true")) {
                                    break;
                                }
                                Log.d(TAG, "isServiceRunning ...");
                            } else {
                                if (commandIsRuning(command)) {
                                    break;
                                }
                                Log.d(TAG, "ps command ...");
                            }
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
        }
        //MainActivity.fullWindowActivity = this;
    }

    private void connectMqttBroker() {
        Log.d(TAG, "--> onCreate");
        mqttWrapper = new MQTTWrapper(mqttClientId);
        boolean ret = mqttWrapper.connect(new MqttMessageReceiver(mqttWrapper, mqttClientId));
        if (ret) {
            Log.d(TAG, "--> mqtt connected");
        } else {
            Log.e(TAG, "--> mqtt no connect, return");
        }
    }

    public static void hideBottomUIMenu(View v) {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            v.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideBottomUIMenu(getWindow().getDecorView());
        }
    }

    private void init() {
        hideBottomUIMenu(getWindow().getDecorView());
        mNiceVideoPlayer = (NiceVideoPlayer) findViewById(R.id.nice_video_player);
        mNiceVideoPlayer.setPlayerType(NiceVideoPlayer.TYPE_IJK); // IjkPlayer or MediaPlayer
        //String videoUrl = "http://8537.vod.myqcloud.com/8537_a58316ea86df11e6a76c67b1ff29c014.f30.mp4?sign=5493ad997e153e8092de307e338b1774&t=5822a346";
        String videoUrl = "local";
        mNiceVideoPlayer.setUp(videoUrl, null);
        final TxVideoPlayerController controller = new TxVideoPlayerController(this);
        controller.setTitle("");
        //controller.setLenght(0);
        Glide.with(this)
                .load(R.drawable.img_default)
                .placeholder(R.drawable.img_default)
                .crossFade()
                .into(controller.imageView());//static png for interface
        mNiceVideoPlayer.setController(controller);
        mNiceVideoPlayer.enterFullScreen();

        NiceVideoPlayer.mVideoList = TxVideoPlayerController.getFilesAllName(FileUtil.DEFAULT_VIDEO_PATH);
        if (NiceVideoPlayer.mVideoList != null && NiceVideoPlayer.mVideoList.size() != 0) {
            NiceVideoPlayer.mPlayVideoList = NiceVideoPlayer.mVideoList;
            if (mNiceVideoPlayer.isIdle()) {
                mNiceVideoPlayer.start();
            }
        } else {
            String str = "No mp4 file found in " + FileUtil.DEFAULT_VIDEO_PATH + " directory!";
            Toast.makeText(FullWindowPlayActivity.this, str, Toast.LENGTH_LONG).show();
        }

    }

    public static NiceVideoPlayer getNiceVideoPlayerInstance() {

        return mNiceVideoPlayer;
    }

    /*    public void enterTinyWindow(View view) {
            if (mNiceVideoPlayer.isIdle()) {
                Toast.makeText(this, "要点击播放后才能进入小窗口", Toast.LENGTH_SHORT).show();
            } else {
                mNiceVideoPlayer.enterTinyWindow();
            }
        }*/


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
        if (mqttWrapper != null) {
            mqttWrapper.destroy();
        }
        NiceVideoPlayerManager.instance().releaseNiceVideoPlayer();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        if (mqttWrapper != null) {
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
                Log.e(TAG, "Command exit value = " + proc.exitValue());
                return false;
            }
            //Log.d(TAG,"StringBuilder: " + sb);
            if (sb.toString().contains("mosquitto")) {
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getSystemStringProperties(Context context, String key, String def) throws IllegalArgumentException {
        String ret = def;
        try {
            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = String.class;
            Method get = SystemProperties.getMethod("get", paramTypes);
            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new String(def);
            ret = (String) get.invoke(SystemProperties, params);
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (Exception e) {
            ret = def;
        }
        return ret;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            final Context context = getApplicationContext();
            int readPermissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (readPermissionCheck == PackageManager.PERMISSION_GRANTED
                    && writePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_RECORD);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD) {
            Log.v(TAG, "onRequestPermissionsResult requestCode ： " + requestCode
                    + " Permission: " + permissions[0] + " was " + grantResults[0]
                    + " Permission: " + permissions[1] + " was " + grantResults[1]
            );

            if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && permissions[1].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                //String command = "ps |grep mosquitto";
                                String command = "ps";
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    String mosquittoIsRunning = getSystemStringProperties(FullWindowPlayActivity.this, "adv.mosquittoIsRunning", "false");
                                    if (mosquittoIsRunning != null && !mosquittoIsRunning.isEmpty() && mosquittoIsRunning.equals("true")) {
                                        break;
                                    }
                                    Log.d(TAG, "isServiceRunning ...");
                                } else {
                                    if (commandIsRuning(command)) {
                                        break;
                                    }
                                    Log.d(TAG, "ps command ...");
                                }
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
            } else {
                //用户不同意，自行处理即可
                finish();
            }
        }
    }

}
