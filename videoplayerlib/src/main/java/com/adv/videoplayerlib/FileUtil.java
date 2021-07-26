package com.adv.videoplayerlib;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ClassName:   FileUtil
 * Description: TODO
 * CreateDate   2021/07/23
 * Author:  Fengchao.dai
 */
public class FileUtil {
    private static final String TAG = "FileUtil";
    public static final String DEFAULT_ANDROIDMANAGER_PATH = Environment.getExternalStorageDirectory().getPath() + "/AndroidManager/";
    public static final String DEFAULT_VIDEO_PATH = Environment.getExternalStorageDirectory().getPath() + "/AndroidManager/video/";

    public static boolean createAndroidManagerPath() {
        File dir = new File(DEFAULT_ANDROIDMANAGER_PATH);
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }

        if (dir.exists()) {
            try {
                String command = "chmod 777 " + DEFAULT_ANDROIDMANAGER_PATH;
                execCommand(command);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static boolean createVideoPath() {
        File dir = new File(DEFAULT_VIDEO_PATH);
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }

        if (dir.exists()) {
            try {
                String command = "chmod 777 " + DEFAULT_VIDEO_PATH;
                execCommand(command);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static boolean recreateVideoPath() {
        File dir = new File(DEFAULT_VIDEO_PATH);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "recreateVideoPath mkdir failed");
                return false;
            }
        } else {
            if (!deleteFile(dir) || !dir.mkdirs()) {
                Log.e(TAG, "recreateVideoPath delete and mkdir failed");
                return false;
            }
        }

        try {
            String command = "chmod 777 " + DEFAULT_VIDEO_PATH;
            execCommand(command);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean deleteFile(File file) {
        if (file == null) {
            Log.e(TAG, "DeleteFile: null parameter");
            return false;
        }

        if (!file.canRead() && !file.canWrite()) {
            return false;
        }
        boolean directory = file.isDirectory();
        if (directory) {
            for (File child : file.listFiles()) {
                deleteFile(child);
            }
        }
        Log.v(TAG, "DeleteFile >>> " + file.getPath());
        return file.delete();
    }


    private static void execCommand(String command) throws IOException {
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
                Log.d(TAG, "exit value = " + proc.exitValue());
            }
            Log.d(TAG, "sb: " + sb);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }
}
