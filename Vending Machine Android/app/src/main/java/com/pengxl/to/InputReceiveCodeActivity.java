package com.pengxl.to;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.security.Key;
import java.util.Timer;
import java.util.TimerTask;

public class InputReceiveCodeActivity extends AppCompatActivity {

    //收货码输入框
    private EditText[] codes;
    //正在输入第几个收货码
    private int target;
    //计时器
    private Timer timer;
    private boolean isTouched;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_receive_code);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
            }
        }, 30000, 30000);
    }

    /**
     * 触摸屏幕检测
     * @param ev MotionEvent
     * @return boolean
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        isTouched = true;
        return super.dispatchTouchEvent(ev);
    }

    private void init() {
        codes = new EditText[7];
        codes[0] = (EditText) findViewById(R.id.receive_code_0);
        codes[1] = (EditText) findViewById(R.id.receive_code_1);
        codes[2] = (EditText) findViewById(R.id.receive_code_2);
        codes[3] = (EditText) findViewById(R.id.receive_code_3);
        codes[4] = (EditText) findViewById(R.id.receive_code_4);
        codes[5] = (EditText) findViewById(R.id.receive_code_5);
        codes[6] = (EditText) findViewById(R.id.receive_code_6);
        target = 0;

        for(EditText code : codes) {
            code.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(count == 1) {
                        target++;
                        codes[target].requestFocus();
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {
                    if(target == 6) {
                        //Test
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            });
            code.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    String s = "";
                    if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        if(target == 0) {
                            return false;
                        }
                        if(target == 6 && !s.equals(codes[target].getText().toString())) {
                            codes[target].setText(s);
                            return true;
                        }
                        target--;
                        codes[target].requestFocus();
                        codes[target].setText(s);
                        return true;
                    }
                    return false;
                }
            });
        }
    }
}
