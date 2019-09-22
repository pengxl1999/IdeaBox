package com.pengxl.to;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGR;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class PhotoActivity extends AppCompatActivity {

    private JavaCameraView cameraView;
    private LinearLayout takePhoto;
    private Bitmap photoBitmap;
    private String orderNumber, medId;
    private Timer timer;
    private boolean isTouched;
    private boolean isShot;
    private Socket socket;

    static {
        OpenCVLoader.initDebug();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        }, 15000, 15000);
        if (!OpenCVLoader.initDebug()) {
        } else {
            cameraView.enableView();
        }
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
    }

    /**
     * 屏幕触摸检测
     * @param ev MotionEvent
     * @return boolean
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        isTouched = true;
        return super.dispatchTouchEvent(ev);
    }

    private void init() {
        /*初始化OpenCV相机*/
        cameraView = (JavaCameraView) findViewById(R.id.photo_camera);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.enableView();
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);      //是否前置摄像
        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener() {
            @Override
            public void onCameraViewStarted(int width, int height) {

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(final Mat inputFrame) {
                if(isShot) {
                    isShot = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showAlertDialog(inputFrame);
                        }
                    });
                    //takePhoto.setEnabled(true);
                }
                return inputFrame;
            }
        });

        isShot = false;
        isTouched = false;

        takePhoto = (LinearLayout) findViewById(R.id.take_photo);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShot = true;
                //takePhoto.setEnabled(false);
            }
        });

        orderNumber = getIntent().getStringExtra("orderNumber");
        medId = getIntent().getStringExtra("medId");
    }

    private void showAlertDialog(final Mat photo) {
        LayoutInflater inflater = LayoutInflater.from(PhotoActivity.this);
        ImageView view = (ImageView) inflater.inflate(R.layout.photo, null);
        photoBitmap = Bitmap.createBitmap(photo.width(), photo.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(photo, photoBitmap);
        view.setImageBitmap(photoBitmap);
        AlertDialog.Builder builder = new AlertDialog.Builder(PhotoActivity.this)
                .setView(view)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendData(photo);
                        Intent intent = new Intent(PhotoActivity.this, BuyMedicineActivity.class);
                        intent.putExtra("orderNumber", orderNumber);
                        intent.putExtra("medId", medId);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }).setNegativeButton("重拍", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.setCancelable(false);
        builder.create();
        builder.show();
    }

    private void sendData(final Mat photo) {
        MatOfByte matOfByte = new MatOfByte();
        cvtColor(photo, photo, COLOR_RGB2BGR);
        imencode(".jpg", photo, matOfByte);
        final byte[] data = matOfByte.toArray();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("39.106.219.88", 8088);
                } catch (Exception e) {
                    e.printStackTrace();
                    Looper.prepare();
                    Toast.makeText(PhotoActivity.this, "上传失败！", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                    return;
                }
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", 1)
                            .put("type", 1)
                            .put("order", orderNumber)
                            .put("size", data.length);
                    String jsonString = jsonObject.toString();
                    Log.i("pengxl1999", jsonString);
                    outputStream.write(jsonString.getBytes(), 0, jsonString.getBytes().length);
                    Thread.sleep(500);
                    outputStream.write(data);
                    socket.shutdownOutput();
                    outputStream.close();
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Looper.prepare();
                    Toast.makeText(PhotoActivity.this, "上传失败！", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();
    }
}
