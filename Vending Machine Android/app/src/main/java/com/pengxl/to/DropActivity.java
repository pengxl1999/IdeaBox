/*
* 掉落药品
* 通过串口为电机发送信息
*
*/
package com.pengxl.to;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SynthesizerListener;
import com.pengxl.to.util.StaticMember;

import java.util.Timer;
import java.util.TimerTask;

import static com.pengxl.to.util.Machine.aisle1;
import static com.pengxl.to.util.Machine.aisle2;
import static com.pengxl.to.util.Machine.aisle3;
import static com.pengxl.to.util.Machine.aisle4;
import static com.pengxl.to.util.Machine.machineId;
import static com.pengxl.to.util.Machine.medicine;
import static com.pengxl.to.util.StaticMember.usbDriver;

public class DropActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 5437;
    //private TextView test;
    private String result;
    private Timer timer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();

    }

    @Override
    protected void onResume() {
        super.onResume();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                setResult(RESULT_CANCELED);
                finish();
            }
        }, 15000);
        sendData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == REQUEST_CODE) {
            setResult(RESULT_CANCELED);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init() {
        //test = (TextView) findViewById(R.id.test);

        result = getIntent().getStringExtra("result");
        //test.setText(result);
    }

    /**
     * USB发送数据
     */
    private void sendData() {
        Pair<Integer, Integer> decodeResult = decodeString(result);
        Log.i("pengxl1999", decodeResult.first + "");
        switch (decodeResult.first) {
            case 1:
                write(aisle1, decodeResult.second);
                break;
            case 2:
                //Log.i("pengxl1999", decodeResult.second + "");
                write(aisle2, decodeResult.second);
                break;
            case 3:
                write(aisle3, decodeResult.second);
                break;
            case 4:
                write(aisle4, decodeResult.second);
                break;
            case 5:     //验证错误
                timer.cancel();
                String s = "";
                switch (decodeResult.second) {
                    case 0:
                        s = "出货失败！请联系管理员！";
                        break;
                    case 1:
                        s = "扫码结果错误，请检查二维码！";
                        break;
                    case 2:
                        s = "取货地点错误，请重新确认订单！";
                        break;
                    case 3:
                        s = "售货机中没有该药品，请咨询店员！";
                        break;
                    default:
                        s = "Error!";
                        break;
                }
                Intent intent = new Intent(DropActivity.this, ErrorPageActivity.class);
                intent.putExtra("message", s);
                startActivityForResult(intent, REQUEST_CODE);
        }
    }

    /**
     * 解析扫码结果
     * @param result 扫码结果
     * @return 货道
     */
    private Pair<Integer, Integer> decodeString(String result) {
        switch (result.charAt(0)) {
            case 'A':
                String[] m = result.split("_");
                if(m.length != 2) {     //m[0]订单号, m[1]药品id
                    return new Pair<>(5, 0);
                }
                else if(medicine.get(Integer.parseInt(m[1]), -1) == -1) {       //药品不存在
                    return new Pair<>(5, 3);
                }
                return new Pair<>(medicine.get(Integer.parseInt(m[1])), 1);
            default:
                String[] s = result.split("_");
                if(s.length != 5) {     //s[0]验证位，s[1]订单号，s[2]药品id，s[3]药品数量，s[4]售货机id
                    return new Pair<>(5, 1);
                }
                else if(!s[0].equals("pxl")) {      //验证位不正确
                    return new Pair<>(5, 1);
                }
                else if(Integer.parseInt(s[4]) != machineId) {      //所选售货机和实际售货机不一致
                    return new Pair<>(5, 2);
                }
                else if(medicine.get(Integer.parseInt(s[2]), -1) == -1) {       //药品不存在
                    return new Pair<>(5, 3);
                }
                return new Pair<>(medicine.get(Integer.parseInt(s[2])), Integer.parseInt(s[3]));
        }
    }

    /**
     * 写入数据
     * @param aisle 货道号
     * @param times 转动次数
     */
    private void write(final byte[] aisle, final int times) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i < times; i++) {
                    usbDriver.WriteData(aisle, aisle.length);
                    try {
                        Thread.sleep(4000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
