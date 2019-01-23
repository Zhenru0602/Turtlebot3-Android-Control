package com.example.zhenru.wafflecontroller;

import android.os.Environment;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    //create socket for connection
    Socket socket = null;

    // 语音听写对象
    private SpeechRecognizer mIat;
    int ret = 0; // 函数调用返回值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //allow network run on main thread, dangerous but easy to write (ง •̀_•́)ง┻━┻掀桌
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //connect to socket to control waffle turtle
        try {
            socket = new Socket("192.168.0.136", 9999);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //initialize the voice recognizer
        SpeechUtility.createUtility(MainActivity.this, "appid=" + "5c2e861b");
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);

        Button voiceButton = findViewById(R.id.speak);
        voiceButton.setOnTouchListener(new View.OnTouchListener() {
            //animation layout while recording
            FrameLayout recordingLayout = findViewById(R.id.recordingLayout);
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        recordingLayout.setVisibility(View.VISIBLE);
                        // 移动数据分析，收集开始听写事件
                        FlowerCollector.onEvent(MainActivity.this, "iat_recognize");
                        // 设置参数
                        setParam();
                        ret = mIat.startListening(mRecognizerListener);
                        break;
                    case MotionEvent.ACTION_UP:
                        recordingLayout.setVisibility(View.GONE);
                        //handle click motion
                        if (event.getEventTime() - event.getDownTime() < 200){
                            Toast.makeText(getApplicationContext(), "请长按！",
                                    Toast.LENGTH_SHORT).show();
                            mIat.cancel();
                        }
                        else{
                            mIat.stopListening();
                        }
                        break;
                }
                return false;
            }
        });
    }

    public void forward(View view){
        try {
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            out.write('w');
            out.flush();
            Log.v("SocketTest", "Send!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void backward(View view){
        try {
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            out.write('x');
            out.flush();
            Log.v("SocketTest", "Send!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void left(View view){
        try {
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            out.write('a');
            out.flush();
            Log.v("SocketTest", "Send!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void right(View view){
        try {
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            out.write('d');
            out.flush();
            Log.v("SocketTest", "Send!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop(View view){
        try {
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            out.write('s');
            out.flush();
            Log.v("SocketTest", "Send!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("Voice Control", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Toast.makeText(getApplicationContext(), "初始化失败！",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        //设置语法ID和 SUBJECT 为空，以免因之前有语法调用而设置了此参数；或直接清空所有参数，具体可参考 DEMO 的示例。
        mIat.setParameter( SpeechConstant.CLOUD_GRAMMAR, null );
        mIat.setParameter( SpeechConstant.SUBJECT, null );
        //设置返回结果格式，目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        //此处engineType为“cloud”
        mIat.setParameter( SpeechConstant.ENGINE_TYPE, "cloud");
        //设置语音输入语言，zh_cn为简体中文
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        //设置结果返回语言
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        // 设置语音前端点:静音超时时间，单位ms，即用户多长时间不说话则当做超时处理
        //取值范围{1000～10000}
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        //设置语音后端点:后端点静音检测时间，单位ms，即用户停止说话多长时间内即认为不再输入，
        //自动停止录音，范围{0~10000}
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        //设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT,"1");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, mediaStorageDir.getPath() + File.separator +
                "voice"+".wav");
    }

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            try {
                printResult(results);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private void printResult(RecognizerResult results) throws JSONException {
        String text = results.getResultString();
        String words = "";
        JSONObject jsonResult = new JSONObject(text);

        if(jsonResult.getInt("sn") == 1) {
            JSONArray ws = jsonResult.getJSONArray("ws");

            for (int i = 0; i < ws.length(); i++) {
                JSONObject temp = ws.getJSONObject(i);
                words += temp.getJSONArray("cw").getJSONObject(0).getString("w");
            }

            Toast.makeText(getApplicationContext(), words,
                    Toast.LENGTH_SHORT).show();

            try {
                OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
                if(words.contains("加")){
                    out.write("vw");
                }
                else if(words.contains("减")){
                    out.write("vx");
                }
                else if(words.contains("左")){
                    out.write("va");
                }
                else if(words.contains("右")){
                    out.write("vd");
                }
                else{
                    out.write('s');
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
