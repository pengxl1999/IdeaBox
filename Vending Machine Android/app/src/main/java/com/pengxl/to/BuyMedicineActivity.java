/*
 *  购买药品页面，利用web实现
 */
package com.pengxl.to;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.UnderstanderResult;
import com.pengxl.to.util.StaticMember;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class BuyMedicineActivity extends AppCompatActivity {

    private static int REQUEST_CODE = 3423;
    private WebView webView;
    private SpeechSynthesizer synthesizer;      //语音提示
    private SpeechUnderstander understander;    //语义听写
    private InitListener initListener;
    private SynthesizerListener synthesizerListener;
    private SpeechUnderstanderListener understanderListener;
    private Timer timer, enter;    //超过60s无响应返回
    private boolean isTouched = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_medicine);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SpeechUtility.createUtility(BuyMedicineActivity.this, SpeechConstant.APPID +"=5cd3c090");

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isTouched = false;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isTouched) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                isTouched = false;
            }
        }, 60000, 60000);

        synthesizer = SpeechSynthesizer.createSynthesizer(BuyMedicineActivity.this, initListener);
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
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
                case RESULT_OK:
                    String orderNumber = data.getStringExtra("orderNumber");
                    String medId = data.getStringExtra("medId");
                    if(orderNumber == null) {
                        finish();
                    }
                    webView.loadUrl("http://to-group.top/machine/web/index.php?r=buy/checked&orderNumber=" + orderNumber + "&medId=" + medId);
                default:
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 屏幕触摸检测
     * @param ev MotionEvent
     * @return boolean
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        isTouched = true;
        if(enter != null) {
            enter.cancel();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void init() {
        webView = (WebView) findViewById(R.id.web);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.i("pengxl1999", url);
                view.loadUrl(url);
                return true;
            }
        });
        webViewSetting();
        webView.addJavascriptInterface(BuyMedicineActivity.this, "android");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(BuyMedicineActivity.this)
                        .setTitle("警告").setMessage(message).setNegativeButton("知道了", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        });
                builder.setCancelable(false);
                builder.create();
                builder.show();
                return true;
            }
        });
        webView.loadUrl("http://to-group.top/machine/web/index.php?r=site/index&machine=1");
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
                        webView.loadUrl("javascript:searchMedicineByVoice('" + med + "')");
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
        enter = new Timer();
        enter.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl("http://to-group.top/machine/web/index.php?r=buy%2Findex");
                        askForMedicineName();
                    }
                });
            }
        }, 5000);
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

    /**
     * 返回选择功能菜单
     */
    @JavascriptInterface
    public void backToMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BuyMedicineActivity.this)
                .setTitle("确定返回菜单吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.setCancelable(false);
        builder.create();
        builder.show();
    }

    /**
     * 网页端调用该方法，获取处方图片。
     */
    @JavascriptInterface
    public void getImageForBuying(String orderNumber, String medId) {
        timer.cancel();
        Intent intent = new Intent(BuyMedicineActivity.this, PhotoActivity.class);
        intent.putExtra("orderNumber", orderNumber);
        intent.putExtra("medId", medId);
        startActivityForResult(intent, REQUEST_CODE);
    }


    /**
     * 网页端调用该方法，当进入购买时询问
     */
    @JavascriptInterface
    public void askForMedicineName() {
        Log.i("pengxl1999", "111");
        new Thread(new Runnable() {
            @Override
            public void run() {
                synthesizer.startSpeaking("请问您需要购买哪种药品呢？", synthesizerListener);
                try {
                    Thread.sleep(3500);     //说话后阻塞3.5s避免扬声器声音被录入
                } catch (Exception e) {
                    e.printStackTrace();
                }
                understander = SpeechUnderstander.createUnderstander(BuyMedicineActivity.this, initListener);
                understander.setParameter(SpeechConstant.VAD_BOS, "2000");
                understander.startUnderstanding(understanderListener);
            }
        }).start();
//        synthesizer.startSpeaking("请问您需要购买哪种药品呢？", synthesizerListener);
//        understander = SpeechUnderstander.createUnderstander(BuyMedicineActivity.this, initListener);
//        understander.setParameter(SpeechConstant.VAD_BOS, "3000");
//        understander.startUnderstanding(understanderListener);
    }

    @JavascriptInterface
    public void voiceInput() {
        understander = SpeechUnderstander.createUnderstander(BuyMedicineActivity.this, initListener);
        understander.setParameter(SpeechConstant.VAD_BOS, "2000");
        understander.startUnderstanding(understanderListener);
    }

    @JavascriptInterface
    public void dropMedicine(String order, String medId) {
        timer.cancel();
        Intent intent = new Intent(BuyMedicineActivity.this, DropActivity.class);
        String s = order + '_' + medId;
        intent.putExtra("result", s);
        startActivityForResult(intent, REQUEST_CODE);
    }
}
