package com.pengxl.to;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.UnderstanderResult;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class AskQuestionActivity extends AppCompatActivity {

    private WebView webView;
    private TextView inputText, outputText;
    private Button search, voiceInput;
    private SpeechSynthesizer synthesizer;      //语音提示
    private SpeechUnderstander understander;    //语义听写
    private SynthesizerListener synthesizerListener;
    private SpeechUnderstanderListener understanderListener;
    private InitListener initListener;
    private Timer timer, interval;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_question);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if(!isTouched) {
//                    setResult(RESULT_CANCELED);
//                    finish();
//                }
//                isTouched = false;
//            }
//        }, 60000, 60000);
        synthesizer = SpeechSynthesizer.createSynthesizer(AskQuestionActivity.this, initListener);
        understander = SpeechUnderstander.createUnderstander(AskQuestionActivity.this, initListener);
    }


    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        if(synthesizer != null) {
            synthesizer.destroy();
        }
        if(understander != null) {
            understander.destroy();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(synthesizer != null) {
            synthesizer.destroy();
        }
        if(understander != null) {
            understander.destroy();
        }
    }

    @JavascriptInterface
    public void haha() {
        Log.i("pengxl1999", "dkkd");
//        //runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                a.setText("dkkd");
//            }
//        });
    }

    /**
     * 初始化
     */
    private void init() {
        //a = (TextView) findViewById(R.id.aaa);
        //a.setText("nihao!");
        inputText = (TextView) findViewById(R.id.ask_question_input_text);
        outputText = (TextView) findViewById(R.id.ask_question_output_text);
        search = (Button) findViewById(R.id.ask_question_search);
        voiceInput = (Button) findViewById(R.id.ask_question_voice_input);

        webView = new WebView(this);
        webViewSetting();
        webView.loadUrl("http://to-group.top/machine/web/index.php?r=question/qa");
        webView.addJavascriptInterface(AskQuestionActivity.this, "android");

        interval = new Timer();
        interval.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl("javascript:haveFun()");
                    }
                });
            }
        }, 1000);
        initListener = new InitListener() {
            @Override
            public void onInit(int i) {
            }
        };
        synthesizerListener = new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {

            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {

            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };
        understanderListener = new SpeechUnderstanderListener() {
            @Override
            public void onVolumeChanged(int i, byte[] bytes) {

            }

            @Override
            public void onBeginOfSpeech() {
                Log.i("pengxl1999", "begin");
            }

            @Override
            public void onEndOfSpeech() {
                Log.i("pengxl1999", "end");
            }

            @Override
            public void onResult(UnderstanderResult understanderResult) {
                try {
                    JSONObject jsonObject = new JSONObject(understanderResult.getResultString());
                    if(jsonObject.has("text")) {
                        String med = jsonObject.getString("text");
                        med = med.substring(0, med.length() - 1);
                        Log.i("pengxl1999", med);
                        webView.loadUrl("javascript:getInformation()");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(SpeechError speechError) {

            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };
    }

    /**
     * 设置webView
     */
    private void webViewSetting() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);                       //可执行js
        webSettings.setDefaultTextEncodingName("UTF-8");              //设置默认的文本编码名称，以便在解码html页面时使用
        webSettings.setAllowContentAccess(true);                      //启动或禁用WebView内的内容URL访问
        webSettings.setAppCacheEnabled(false);                        //设置是否应该启用应用程序缓存api
        webSettings.setBuiltInZoomControls(false);                    //设置WebView是否应该使用其内置的缩放机制
        webSettings.setUseWideViewPort(true);                         //设置WebView是否应该支持viewport
        webSettings.setLoadWithOverviewMode(true);                    //不管WebView是否在概述模式中载入页面，将内容放大适合屏幕宽度
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);          //重写缓存的使用方式
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);   //告知js自动打开窗口
        webSettings.setLoadsImagesAutomatically(true);                //设置WebView是否应该载入图像资源
        webSettings.setAllowFileAccess(true);                         //启用或禁用WebView内的文件访问
        webSettings.setDomStorageEnabled(true);                       //设置是否启用了DOM存储API,默认为false
    }
}
