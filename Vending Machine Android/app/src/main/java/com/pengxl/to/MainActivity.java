/*
 * 程序入口
 * 通过使用OpenCV进行人脸检测，当检测到人脸时设置成为工作状态，并进入SelectFunctionActivity
 * 利用计时器避免误检测
 */
package com.pengxl.to;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

import static com.pengxl.to.util.Machine.medicine;
import static com.pengxl.to.util.StaticMember.usbDriver;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGR;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.equalizeHist;

public class MainActivity extends AppCompatActivity {

    static String TAG = "pengxl1999";
    static final int REQUEST_PERMISSION = 1999;
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";       //usb权限
    private String[] permissions = { Manifest.permission.CAMERA };
    private JavaCameraView cameraView;
    private Button enter;
    private Bitmap srcBitmap, dstBitmap;
    private Size mSize = new Size(3, 3);
    private Mat image, originalImage;
    private MatOfRect faces;
    private Timer timer;
    private CascadeClassifier cascadeClassifier;
    private int framesWithFaces;      //有人脸的帧数，0.5s内总帧数
    private boolean timerIsSet;

    static {
        OpenCVLoader.initDebug();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_PERMISSION);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {

        } else {
            cameraView.enableView();
        }
        framesWithFaces = 0;
        timerIsSet = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
        usbDriver.CloseDevice();
    }

    /**
     * 相机权限请求回调函数
     * @param requestCode 请求码
     * @param permissions 权限
     * @param grantResults 请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSION) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
            else {
                Toast.makeText(MainActivity.this, "相机权限请求失败！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void init() {       //初始化控件
        initializeOpenCVDependencies();
        framesWithFaces = 0;
        timerIsSet = false;
        /*初始化药品信息*/
        initMedicine();
        enter = (Button) findViewById(R.id.main_enter);
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timer != null) {
                    timer.cancel();
                }
                Intent intent = new Intent(MainActivity.this, SelectFunctionActivity.class);
                startActivity(intent);
            }
        });
        /*初始化OpenCV相机*/
        cameraView = (JavaCameraView) findViewById(R.id.cameraView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.enableView();
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);      //是否前置摄像
        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                image = new Mat();
                originalImage = new Mat();
                Log.i(TAG, "onCameraViewStarted");
            }

            @Override
            public void onCameraViewStopped() {
                image.release();
                originalImage.release();
                Log.i(TAG, "onCameraViewStopped");
            }

            @Override
            public Mat onCameraFrame(Mat inputFrame) {
                //cvtColor(inputFrame, inputFrame, COLOR_RGB2BGR);
                originalImage = inputFrame.clone();
                processImage(inputFrame);
                //Log.i("pengxl1999", "frame");
                return originalImage;
            }
        });
        /*初始化USB*/
        usbDriver = new CH34xUARTDriver((UsbManager) getSystemService(Context.USB_SERVICE), this, ACTION_USB_PERMISSION);
        if(!usbDriver.UsbFeatureSupported()) {
            Toast.makeText(MainActivity.this, "No USB Service!", Toast.LENGTH_SHORT).show();
        }
        int retval = usbDriver.ResumeUsbList();
        switch (retval) {
            case -1:
                Toast.makeText(MainActivity.this, "打开设备失败！", Toast.LENGTH_SHORT).show();
                usbDriver.CloseDevice();
                break;
            case 0:
                if(!usbDriver.UartInit()) {
                    Toast.makeText(MainActivity.this, "设备初始化失败", Toast.LENGTH_SHORT).show();
                    break;
                }
                Toast.makeText(MainActivity.this, "设备初始化成功", Toast.LENGTH_SHORT).show();
                usbDriver.SetConfig(115200, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
                break;
            default:
                Toast.makeText(MainActivity.this, "没有权限！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化药品
     */
    private void initMedicine() {
        medicine = new SparseIntArray();
        medicine.append(1965, 1);     //复方珊瑚片
        medicine.append(4002, 2);       //甲硝唑
        medicine.append(258, 3);      //金振口服液
        medicine.append(2744, 4);       //头孢
    }

    /**
     * 处理视频流中的图像
     * @param inputFrame 输入图像矩阵
     */
    private void processImage(Mat inputFrame) {
        srcBitmap = Bitmap.createBitmap(inputFrame.width(), inputFrame.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputFrame, srcBitmap);
        dstBitmap = Bitmap.createScaledBitmap(srcBitmap, (int)(0.3*srcBitmap.getWidth()),
                (int)(0.3*srcBitmap.getHeight()), false);
        Utils.bitmapToMat(dstBitmap, image);
        cvtColor(image, image, COLOR_RGB2GRAY);
        equalizeHist(image, image);
        faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(image, faces, 1.2, 2, 2, new Size(10, 10));
        }
        Rect[] facesArray = faces.toArray();
        if (facesArray.length > 0){
            framesWithFaces++;
            Log.i(TAG, "1");
            if(!timerIsSet) {       //是否已设置
                timerIsSet = true;
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (framesWithFaces > 2) {
//                                synthesizer.startSpeaking("王凯，男，1990年生，出生于中国吉林省长春市。伟大的民主斗士，思想家，教育家，" +
//                                        "企业家，物联王，英特雷真公司首席技术官。于2019年被送了一个钟", synthesizerListener);
                            Intent intent = new Intent(MainActivity.this, SelectFunctionActivity.class);
                            startActivity(intent);
                        }
                        else {
                            timerIsSet = false;
                        }
                        framesWithFaces = 0;
                    }
                }, 500);       //0.5s内有人脸的帧数>2，则启动服务
            }
        }
    }

    /**
     * 加载OpenCV依赖，主要是人脸样本文件
     */
    private void initializeOpenCVDependencies() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2); //OpenCV的人脸模型文件： lbpcascade_frontalface_improved
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            // 加载cascadeClassifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error loading cascade", e);
        }
    }

}
