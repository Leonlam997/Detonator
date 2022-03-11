package com.leon.detonator.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.util.Base64Util;
import com.google.gson.Gson;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.FaceIdentifyBean;
import com.leon.detonator.R;
import com.leon.detonator.Util.KeyUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CameraActivity extends BaseActivity {
    public static final String APP_ID = "16706105";
    public static final String API_KEY = "70s2Tb7TNOARiTxY8xjcI4nO";
    public static final String SECRET_KEY = "gHdWY3maEZKN7UkxKSpCC6H8d3vqFi4r";
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private BaseApplication myApp;
    private String mCameraId;
    private Size mPreviewSize;
    private Size mCaptureSize;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NotNull CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NotNull CameraCaptureSession session, @NotNull CaptureRequest request, @NotNull CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(@NotNull CameraCaptureSession session, @NotNull CaptureRequest request, @NotNull TotalCaptureResult result) {
            capture();
        }
    };
    private boolean detecting;
    private byte[] imageData;
    private int mWidth, mHeight;
    private final TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当SurfaceTexture可用的时候，设置相机参数并打开相机
            mWidth = width;
            mHeight = height;
            setupCamera(mWidth, mHeight);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        setTitle(R.string.camera_title);
        detecting = false;

        myApp = (BaseApplication) getApplication();
        mTextureView = findViewById(R.id.ttv_Camera);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            mTextureView.setOnClickListener(view -> {
                if (!detecting) {
                    setProgressVisibility(true);
                    detecting = true;
                    takePicture(mTextureView);
                }
            });
        } else {
            myApp.myToast(this, R.string.camera_not_installed);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraThread();
        if (!mTextureView.isAvailable()) {
            mTextureView.setSurfaceTextureListener(mTextureListener);

        } else {
            setupCamera(mWidth, mHeight);
            openCamera();
            //startPreview();
        }
    }

    private void startCameraThread() {
        HandlerThread mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (null != manager) {
            try {
                //遍历所有摄像头
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    //此处默认打开前置摄像头
                    if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK)
                        continue;
                    //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    assert map != null;
                    //根据TextureView的尺寸设置预览尺寸
                    mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    //获取相机支持的最大拍照尺寸
                    mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getHeight() * rhs.getWidth()));
                    //此ImageReader用于拍照所需
                    setupImageReader();
                    mCameraId = cameraId;
                    break;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight()));
        }
        return sizeMap[0];
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (null != manager) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    myApp.myToast(this, R.string.camera_permission);
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                    return;
                }
                manager.openCamera(mCameraId, mStateCallback, mCameraHandler);
            } catch (Exception e) {
                myApp.myToast(this, R.string.camera_open_fail);
                BaseApplication.writeErrorLog(e);
                finish();
            }
        }
    }

    private void startPreview() {
        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(mSurfaceTexture);
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 自动对焦
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            // 自动曝光
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY);
            mCaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture(View view) {
        lockFocus();
    }

    private void lockFocus() {
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void capture() {
        try {
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));

            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    //unLockFocus();
                }
            };
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(mCaptureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unLockFocus() {
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            //mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), null, mCameraHandler);
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

    }


    private void setupImageReader() {
        //2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();
            //mCameraHandler.post(new imageSaver(image));
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            imageData = new byte[buffer.remaining()];
            buffer.get(imageData);
            new FaceIdentifyThread().start();
        }, mCameraHandler);
    }

//    public static class imageSaver implements Runnable {
//
//        private final Image mImage;
//
//        public imageSaver(Image image) {
//            mImage = image;
//        }
//
//        @Override
//        public void run() {
//            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//            byte[] data = new byte[buffer.remaining()];
//            buffer.get(data);
//            File mImageFile = new File(FilePath.FILE_CAMERA_CAPTURE);
//            if (!mImageFile.exists() && !mImageFile.mkdir()) {
//                BaseApplication.writeFile("Create camera path error!");
//            }
//
//            String fileName = FilePath.FILE_CAMERA_CAPTURE + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".jpg";
//            FileOutputStream fos = null;
//            try {
//                fos = new FileOutputStream(fileName);
//                fos.write(data, 0, data.length);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                if (fos != null) {
//                    try {
//                        fos.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }

    private class FaceIdentifyThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                runOnUiThread(() -> myApp.myToast(CameraActivity.this, R.string.camera_detecting));
                AipFace client = new AipFace(APP_ID, API_KEY, SECRET_KEY);
                JSONObject res = client.search(Base64Util.encode(imageData), "BASE64", "zbgroup", null);
                FaceIdentifyBean bean = new Gson().fromJson(res.toString(), FaceIdentifyBean.class);
                if (bean.getResult() != null && bean.getResult().getUser_list() != null) {
                    int i;
                    for (i = 0; i < bean.getResult().getUser_list().size(); i++)
                        if (bean.getResult().getUser_list().get(i).getScore() > 80) {
                            detecting = false;
                            setResult(RESULT_OK, new Intent().putExtra(KeyUtils.KEY_USER_NAME, bean.getResult().getUser_list().get(i).getUser_id()));
                            finish();
                            break;
                        }
                    if (i == bean.getResult().getUser_list().size()) {
                        detecting = false;
                        runOnUiThread(() -> {
                            myApp.myToast(CameraActivity.this, R.string.camera_not_found);
                            setProgressVisibility(false);
                            unLockFocus();
                        });
                    }
                } else {
                    detecting = false;
                    runOnUiThread(() -> {
                        myApp.myToast(CameraActivity.this, R.string.camera_not_detected);
                        setProgressVisibility(false);
                        unLockFocus();
                    });
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }

}