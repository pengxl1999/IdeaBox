/*
 * 用于选择功能：
 * 1、购买药品，进入web界面
 * 2、手机预约取药
 * 3、医学问题咨询
 */
package com.pengxl.to;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.UnderstanderResult;
import com.pengxl.to.util.ImageTextButton;
import com.pengxl.to.util.StaticMember;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.common.Constant;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class SelectFunctionActivity extends AppCompatActivity implements View.OnClickListener {

    private final int REQUEST_CODE = 4547;
    private ImageTextButton buyMedicine, getMedicine, askQuestions;
    private SpeechSynthesizer synthesizer;
    private InitListener initListener;
    private SynthesizerListener synthesizerListener;
    private SpeechUnderstander understander;    //语义听写
    private SpeechUnderstanderListener understanderListener;
    private Timer timer;    //超过30s无响应返回

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_function);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SpeechUtility.createUtility(SelectFunctionActivity.this, SpeechConstant.APPID +"=5cd3c090");

        init();

    }

    @Override
    protected void onResume() {
        super.onResume();
        understander = SpeechUnderstander.createUnderstander(SelectFunctionActivity.this, initListener);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synthesizer = SpeechSynthesizer.createSynthesizer(SelectFunctionActivity.this, initListener);
                synthesizer.startSpeaking("请问您需要什么帮助？", synthesizerListener);
//                //说话后阻塞3s避免扬声器声音被录入
//                try {
//                    Thread.sleep(2500);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        }).start();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                finish();
            }
        }, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_CANCELED:
                    finish();
                case RESULT_OK:
                default:
            }
        }
    }

    private void init() {   //初始化控件
        buyMedicine = (ImageTextButton) findViewById(R.id.buy_medicine_button);
        getMedicine = (ImageTextButton) findViewById(R.id.get_medicine_button);
        askQuestions = (ImageTextButton) findViewById(R.id.ask_question_button);

        buyMedicine.setText("购药");
        getMedicine.setText("取药");
        askQuestions.setText("健康咨询");

        buyMedicine.setImgResource(R.drawable.buy_medicine);
        getMedicine.setImgResource(R.drawable.get_medicine);
        askQuestions.setImgResource(R.drawable.ask_question);

        buyMedicine.setOnClickListener(this);
        getMedicine.setOnClickListener(this);
        askQuestions.setOnClickListener(this);

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
                understander.setParameter(SpeechConstant.VAD_BOS, "2000");
                understander.startUnderstanding(understanderListener);
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

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onResult(UnderstanderResult understanderResult) {
                try {
                    JSONObject jsonObject = new JSONObject(understanderResult.getResultString());
                    if(jsonObject.has("text")) {
                        String info = jsonObject.getString("text");
                        Log.i("pengxl1999", info);
                        if(info.contains("购") || info.contains("买")) {
                            Intent intent = new Intent(SelectFunctionActivity.this, BuyMedicineActivity.class);
                            timer.cancel();
                            startActivityForResult(intent, REQUEST_CODE);
                        }
                        if(info.contains("取")) {
                            Intent intent = new Intent(SelectFunctionActivity.this, CaptureActivity.class);
                            timer.cancel();
                            startActivityForResult(intent, REQUEST_CODE);
                        }
                        if(info.contains("咨询")) {
                            Intent intent = new Intent(SelectFunctionActivity.this, CaptureActivity.class);
                            timer.cancel();
                            startActivityForResult(intent, REQUEST_CODE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                understander.setParameter(SpeechConstant.VAD_BOS, "2000");
                understander.startUnderstanding(understanderListener);
            }

            @Override
            public void onError(SpeechError speechError) {

            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.buy_medicine_button) {
            Intent intent = new Intent(SelectFunctionActivity.this, BuyMedicineActivity.class);
            timer.cancel();
            startActivityForResult(intent, REQUEST_CODE);
        }
        if(v.getId() == R.id.get_medicine_button) {
            Intent intent = new Intent(SelectFunctionActivity.this, CaptureActivity.class);
            timer.cancel();
            startActivityForResult(intent, REQUEST_CODE);
        }
        if(v.getId() == R.id.ask_question_button) {
            Intent intent = new Intent(SelectFunctionActivity.this, AskQuestionActivity.class);
            timer.cancel();
            startActivityForResult(intent, REQUEST_CODE);
        }
    }
}
