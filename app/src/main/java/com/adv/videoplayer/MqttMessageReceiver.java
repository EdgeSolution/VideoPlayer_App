package com.adv.videoplayer;

import android.util.Log;

import com.adv.localmqtt.MQTTWrapper;
import com.adv.localmqtt.MqttV3MessageReceiver;
import com.adv.localmqtt.Payload;
import com.adv.videoplayerlib.NiceVideoPlayer;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.adv.videoplayerlib.NiceVideoPlayer.numberLock;
import static com.adv.videoplayer.FullWindowPlayActivity.getNiceVideoPlayerInstance;
import static com.adv.videoplayer.FullWindowPlayActivity.videoPath;

public class MqttMessageReceiver extends MqttV3MessageReceiver {
    private final String TAG = "MqttMessageReceiver";
    private static final Logger LOG = LoggerFactory.getLogger(MqttMessageReceiver.class);
    private NiceVideoPlayer nvp;

    MqttMessageReceiver(MQTTWrapper mqttWrapper, String mqttClientId) {
        super(mqttWrapper, mqttClientId);
    }

    @Override
    public void handleMessage(String topic, String message) {
        final int SUCCEED = 0;
        final int UNKNOWN_REASON = 1;
        final int NO_VIDEOS_FOUND = 2;
        final int WRONG_FUNCID = 3;
        final int PLAYLIST_IS_EMPTY = 4;
        final int PLAY_ERROR = 5;

        try {
            LOG.info(TAG + " recv:  { " + topic + " [" + message + "]}");
            if (topic.startsWith(REQUEST_TOPIC_STARTER)) { // request from peer
                String[] parms = message.split(";", 6);
                Payload response = null;
                if (parms.length == 6) {
                    Long messageId = Long.parseLong(parms[0]);
                    String appName = parms[1];
                    String funcId = parms[2];
                    String option = parms[3];
                    String type = parms[4];
                    String param = parms[5];

                    String jsonValue = null;
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("pkgname", appName);
                    jsonObj.put("funcid", funcId);
                    JSONObject subJsonObj = new JSONObject();


                    nvp = getNiceVideoPlayerInstance();
                    if(nvp == null){
                        jsonObj.put("result",1);
                        jsonObj.put("errcode",NO_VIDEOS_FOUND);
                        jsonObj.put("data","");
                    }else {
                        switch (option) {
                            case "1": //get
                                switch (funcId) {
                                    case "get_volume": //查询当前音量和最大音量
                                        int maxvol = nvp.getMaxVolume();
                                        int curvol = nvp.getVolume();
                                        if(maxvol == -1 || curvol == -1) {
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", UNKNOWN_REASON);
                                        }else{
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                            subJsonObj.put("maxvol", maxvol);
                                            subJsonObj.put("curvol", curvol);
                                        }
                                        Log.d(TAG, "#get_volume#  maxVolume :" + maxvol + "currentVolume" + curvol);
                                        break;
                                    case "get_local_video_list": //查询本地影片列表
                                        List<String> videolist = FullWindowPlayActivity.getFilesAllName(videoPath);
                                        if(videolist == null || videolist.size() == 0) {
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                            subJsonObj.put("videolist", "");
                                        }else{
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                            subJsonObj.put("videolist", videolist);
                                        }
                                        Log.d(TAG, "#get_local_list#  Local videolists: " + videolist);
                                        break;
                                    case "get_playlist_status": //查询本地影片列表
                                        if (NiceVideoPlayer.mPlayVideoList == null || NiceVideoPlayer.mPlayVideoList.size() == 0) {
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", NO_VIDEOS_FOUND);
                                        } else {
                                            List<String>  list = new ArrayList<>();
                                            numberLock.readLock().lock();
                                            try {
                                                for(int i = 0; i< NiceVideoPlayer.mPlayVideoList.size();i++) {
                                                    if(isFileExists(NiceVideoPlayer.mPlayVideoList.get(i).replace(videoPath, ""))) {
                                                        //只把在本地视频源和播放列表都存在的视频上报上去，不从播放列表中清除本地列表不存在的视频
                                                        list.add(NiceVideoPlayer.mPlayVideoList.get(i).replace(videoPath, ""));
                                                    }
                                                }
                                            } finally {
                                                numberLock.readLock().unlock();
                                            }
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                            subJsonObj.put("playlist", list);
                                            subJsonObj.put("playstatus", nvp.getCurrentState());
                                            Log.d(TAG, "#get_playlist_status#  Local playlists: " + list);
                                        }
                                        Log.d(TAG, "#get_playlist_status#  Real local playlists: " + NiceVideoPlayer.mPlayVideoList);                                        break;
                                    case "get_video_info": //获取当前video信息
                                        Log.d(TAG, "Get current video info");
                                        if(NiceVideoPlayer.mPlayVideoList == null || NiceVideoPlayer.mPlayVideoList.size() == 0){
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", PLAYLIST_IS_EMPTY);
                                        }else {
                                            String videoName = "";
                                            numberLock.readLock().lock();
                                            try {
                                                videoName = NiceVideoPlayer.mPlayVideoList.get(nvp.getVideoNumber()).replace(videoPath, "");
                                            } finally {
                                                numberLock.readLock().unlock();
                                            }
                                            Long duration = nvp.getDuration();
                                            Long curPosition = nvp.getCurrentPosition();
                                            if(duration == -1 || curPosition == -1) {
                                                jsonObj.put("result", 1);
                                                jsonObj.put("errcode", UNKNOWN_REASON);
                                            }else if(duration == 0 || curPosition == 0) {
                                                Log.d(TAG,"status: " + nvp.getCurrentState());
                                                if(nvp.getCurrentState() == NiceVideoPlayer.STATE_ERROR) {
                                                    jsonObj.put("result", 1);
                                                    jsonObj.put("errcode", PLAY_ERROR);
                                                    subJsonObj.put("videoname", videoName);
                                                    subJsonObj.put("duration", duration);
                                                    subJsonObj.put("curposition", curPosition);
                                                }
                                            }else{
                                                jsonObj.put("result", 0);
                                                jsonObj.put("errcode", SUCCEED);
                                                subJsonObj.put("videoname", videoName);
                                                subJsonObj.put("duration", duration);
                                                subJsonObj.put("curposition", curPosition);
                                            }
                                            Log.d(TAG, "#get_video_info#  Current video name: " + videoName + " duration:" + duration + " curPosition:" + curPosition);
                                        }
                                        break;
                                    default:
                                        jsonObj.put("result", 1);
                                        jsonObj.put("errcode", WRONG_FUNCID);
                                        break;
                                }
                                jsonObj.put("data",subJsonObj);
                                break;
                            case "2": //set
                                switch (funcId) {
                                    case "set_start": //开始播放影片
                                        Log.e(TAG, "#set_start#  Start play");
                                        String[] strs = param.split(",");
                                        List<String> tmpList = new ArrayList<String>();
                                        Collections.addAll(tmpList, strs);
                                        for (String s : tmpList) {
                                            Log.e(TAG, "tmpList    " + s);
                                            if(!isFileExists(s)) {
                                                Log.e(TAG, "File \"" + videoPath + s + "\"" + " does not exist!");
                                                jsonObj.put("result", 1);
                                                jsonObj.put("errcode", NO_VIDEOS_FOUND);

                                                jsonObj.put("data","");
                                                response = new Payload(messageId, appName, funcId, Integer.parseInt(option), 2, jsonObj.toString());
                                                String pubTopic = genRespTopic();
                                                String pubContent = response.genContent();
                                                getMQTTWrapper().publish(pubTopic, pubContent);
                                                return;
                                            }
                                        }
                                        numberLock.writeLock().lock();
                                        try {
                                            if (NiceVideoPlayer.mPlayVideoList != null) {
                                                NiceVideoPlayer.mPlayVideoList.clear();
                                            } else {
                                                NiceVideoPlayer.mPlayVideoList = new ArrayList<>();
                                            }
                                            NiceVideoPlayer.setVideoNumber(0);
                                            for (int i = 0; i < tmpList.size(); i++) {
                                                System.out.println(videoPath + tmpList.get(i));
                                                NiceVideoPlayer.mPlayVideoList.add(i, videoPath + tmpList.get(i));
                                            }
                                        } finally {
                                            numberLock.writeLock().unlock();
                                        }

                                        for (String s : NiceVideoPlayer.mPlayVideoList) {
                                            Log.e(TAG, "mPlayVideoList    " + s);
                                        }
                                        if (nvp.isPlaying()) {
                                            Thread t = new Thread() {
                                                public void run() {
                                                    nvp.post(rePlay);
                                                }
                                            };
                                            t.start();
                                        } else if (nvp.isPaused() || nvp.isError()) {
                                            Thread t = new Thread() {
                                                public void run() {
                                                    nvp.post(reStart);
                                                }
                                            };
                                            t.start();
                                        } else {
                                            Thread t = new Thread() {
                                                public void run() {
                                                    nvp.post(playVideo);
                                                }
                                            };
                                            t.start();
                                        }
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Log.d(TAG,"status: " + nvp.getCurrentState());
                                        if(nvp.getCurrentState() != NiceVideoPlayer.STATE_PLAYING) {
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", UNKNOWN_REASON);
                                        }else{
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                        }
                                        break;
                                    case "set_restart": //开始播放影片
                                        Log.e(TAG, "#set_restart#  Restart play");
                                        String[] strs1 = param.split(",");
                                        List<String> tmpList1 = new ArrayList<String>();
                                        Collections.addAll(tmpList1, strs1);
                                        for (String s : tmpList1) {
                                            Log.e(TAG, "tmpList1    " + s);
                                            if(!isFileExists(s)) {
                                                Log.e(TAG, "File \"" + videoPath + s + "\"" + " does not exist!");
                                                jsonObj.put("result", 1);
                                                jsonObj.put("errcode", NO_VIDEOS_FOUND);

                                                jsonObj.put("data","");
                                                response = new Payload(messageId, appName, funcId, Integer.parseInt(option), 2, jsonObj.toString());
                                                String pubTopic = genRespTopic();
                                                String pubContent = response.genContent();
                                                getMQTTWrapper().publish(pubTopic, pubContent);
                                                return;
                                            }
                                        }
                                        numberLock.writeLock().lock();
                                        try {
                                            if (NiceVideoPlayer.mPlayVideoList != null) {
                                                NiceVideoPlayer.mPlayVideoList.clear();
                                            } else {
                                                NiceVideoPlayer.mPlayVideoList = new ArrayList<>();
                                            }
                                            NiceVideoPlayer.setVideoNumber(0);
                                            for (int i = 0; i < tmpList1.size(); i++) {
                                                System.out.println(videoPath + tmpList1.get(i));
                                                NiceVideoPlayer.mPlayVideoList.add(i, videoPath + tmpList1.get(i));
                                            }
                                        } finally {
                                            numberLock.writeLock().unlock();
                                        }

                                        for (String s : NiceVideoPlayer.mPlayVideoList) {
                                            Log.e(TAG, "mPlayVideoList    " + s);
                                        }

                                        if (nvp.isPlaying() || nvp.isPaused() || nvp.isError()) {
                                            Thread t = new Thread() {
                                                public void run() {
                                                    nvp.post(rePlay);
                                                }
                                            };
                                            t.start();
                                        } else {
                                            Thread t = new Thread() {
                                                public void run() {
                                                    nvp.post(playVideo);
                                                }
                                            };
                                            t.start();
                                        }
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Log.d(TAG,"status: " + nvp.getCurrentState());
                                        if(nvp.getCurrentState() != NiceVideoPlayer.STATE_PLAYING) {
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", UNKNOWN_REASON);
                                        }else{
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                        }
                                        break;
                                    case "set_pause": //暂停播放影片
                                        Log.e(TAG, "#set_pause#  Pause play");
                                        nvp.pause();
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Log.d(TAG,"status: " + nvp.getCurrentState());
                                        if(nvp.getCurrentState() != NiceVideoPlayer.STATE_PAUSED) {
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", UNKNOWN_REASON);
                                        }else{
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                        }
                                        break;
                                    case "set_volume": //设置音量
                                        if (nvp.setVolume(Integer.parseInt(param))) {
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                        } else {
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", UNKNOWN_REASON);
                                        }
                                        Log.d(TAG, "#set_volume#  Set the volume to " + Integer.parseInt(param));
                                        break;
                                    case "del_local_video": //删除本地视频列表
                                        String[] strs2 = param.split(",");
                                        List<String> tmpList2 = new ArrayList<String>();
                                        Collections.addAll(tmpList2, strs2);
                                        for (String s : tmpList2) {
                                            Log.e(TAG, "tmpList2    " + s);
                                            if(!delFile(s)) {
                                                jsonObj.put("result", 1);
                                                jsonObj.put("errcode", UNKNOWN_REASON);
                                            }else{
                                                jsonObj.put("result", 0);
                                                jsonObj.put("errcode", SUCCEED);
                                            }
                                        }
                                        break;
                                    default:
                                        jsonObj.put("result", 1);
                                        jsonObj.put("errcode", WRONG_FUNCID);
                                        break;
                                }
                                jsonObj.put("data","");
                                break;
                            default:
                                break;
                        }
                    }

                    jsonValue = jsonObj.toString();
                    response = new Payload();
                    response.setMessageID(messageId);
                    response.setFuncID(funcId);
                    response.setOption(Integer.parseInt(option));
                    response.setType(2);//response
                    response.setContent(jsonValue);
                    response.setAppName(appName);
                    String pubTopic = genRespTopic();
                    String pubContent = response.genContent();
                    getMQTTWrapper().publish(pubTopic, pubContent);
                } else {
                    LOG.error(TAG + " receive an invalid package");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    final Runnable playVideo = new Runnable() {
        public void run() {
            nvp.start();
        }
    };

    final Runnable rePlay = new Runnable() {
        public void run() {
            nvp.jumpToCompleted();
            nvp.restart();
        }
    };

    final Runnable reStart = new Runnable() {
        public void run() {
            nvp.restart();
        }
    };

    private static boolean isFileExists(String fileName) {
        File file = new File(videoPath);
        File[] files = file.listFiles();
        if (files == null) {
            Log.e("error", "empty dir!");
            return false;
        }
        for (File file1 : files) {
            if (file1.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean delFile(String fileName) {
        File file = new File(videoPath);
        File[] files = file.listFiles();
        if (files == null) {
            Log.e("error", "empty dir!");
            return false;
        }
        for (File file1 : files) {
            if (file1.getName().equals(fileName)) {
                return file1.delete();
            }
        }
        return false;
    }
}
