package com.camera.zshot.zshot.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.TonemapCurve;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import com.camera.zshot.zshot.BuildConfig;
import com.camera.zshot.zshot.CameraLogger;
import com.camera.zshot.zshot.ImageSaver;
import com.camera.zshot.zshot.ui.MainActivity;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;
import static android.hardware.camera2.CameraDevice.StateCallback;

public class Camera {
    static Size PreviewSize = null;
    public static final int STATE_FOCUSING = -1;
    public static final int STATE_FOCUSED = 0;
    private static final int STATE_PREVIEWING = 0;
    private static final int STATE_WAIT_FOCUS_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static OnCameraFocusListener onCameraFocusListener;
    private String CurrentCamera;
    private int FocusState;
    private Context context ;
    private Surface PreviewSurface;
    private CameraManager cameraManager = null;
    private CameraDevice cameraDevice = null;
    private CameraDevice.StateCallback stateCallback = null;
    private CaptureRequest mCaptureRequest = null;
    private CaptureRequest.Builder PreviewRequestBuilder = null;
    private CaptureRequest.Builder ImageCaptureBuilder;
    private CameraCaptureSession mcameraCaptureSession = null;
    private CameraCaptureSession.CaptureCallback captureCallback = null;
    private Handler handler = null;
    private HandlerThread handlerThread = null;
    private ImageReader imageReader = null;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = null;
    private static final File ImageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/", "Zshot");
    private CameraLogger Logger;
    private static Size[] Resolutions = null;


    public Camera(Context context,Surface surface)
    {
        this.context = context;
        this.PreviewSurface = surface;
        Logger = MainActivity.getLoggerInstance();
        OnCreate();
    }
    private void OnCreate()
    {
        stateCallback = new StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                if(BuildConfig.DEBUG)
                Logger.Log("Camera Opened");
                cameraDevice = camera;
                CreateCameraPreviewSession();
                onCameraFocusListener.OnCameraOpened(true);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                if(BuildConfig.DEBUG)
                    Logger.Log("Camera Disconnected");
                camera.close();
                cameraDevice =null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                if(BuildConfig.DEBUG)
                    Logger.Log("Error on opening camera");
                    camera.close();
                    cameraDevice =null;
            }
        };
        captureCallback = new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                ProcessFocusState(result);
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
            }
            private void ProcessFocusState(CaptureResult captureResult)
            {
                switch (FocusState)
                {
                    case STATE_PREVIEWING:
                        //do nothing:
                        break;
                    case STATE_WAIT_FOCUS_LOCK:
                        Integer AfState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                        //noinspection ConstantConditions
                        if(AfState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                AfState == null)
                        {
                            onCameraFocusListener.OnFocus(STATE_FOCUSED);
                            //RunPrecapture();
                            UnlockFocus();
                        }
                        break;
                }
            }
        };
        onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                handler.post(new ImageSaver(imageReader.acquireNextImage(),ImageFile));
            }
        };

        ProcessCameraoutputSizes();

    }

    @Deprecated
    private void RunPrecapture()
    {
        PreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        FocusState = STATE_WAITING_PRECAPTURE;
        try {
            mcameraCaptureSession.capture(PreviewRequestBuilder.build(),captureCallback,handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void OpenCamera(String CameraID)
    {
        this.CurrentCamera = CameraID;
        OpenBgThread();
        cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        try {
            assert cameraManager != null;
            cameraManager.openCamera(CameraID,stateCallback,handler);
        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public void CloseCamera() {
        if(mcameraCaptureSession != null)
        {
           mcameraCaptureSession.close();
            mcameraCaptureSession = null;
        }
        if(cameraDevice != null)
        {
            cameraDevice.close();
            cameraDevice = null;
        }
        if(imageReader!=null)
            imageReader.close();
        CloseBgThread();
    }

    public void LockFocus()
    {
        FocusState = STATE_WAIT_FOCUS_LOCK;
        PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        //PreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_TORCH);
        try {
            mcameraCaptureSession.capture(PreviewRequestBuilder.build(),captureCallback,handler);
            onCameraFocusListener.OnFocus(STATE_FOCUSING);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void FocusOnTap(float x ,float y)
    {
        FocusState = STATE_WAIT_FOCUS_LOCK;
        PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        PreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS,CalculateFocusRects(x,y));
        try {
            mcameraCaptureSession.capture(PreviewRequestBuilder.build(),captureCallback,handler);
            onCameraFocusListener.OnFocus(STATE_FOCUSING);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void UnlockFocus()
    {
        FocusState = STATE_PREVIEWING;
        PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        //PreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
        try {
            mcameraCaptureSession.capture(PreviewRequestBuilder.build(),captureCallback,handler);
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void CreateCameraPreviewSession() {

        try {
            imageReader = ImageReader.newInstance(GetMaxCameraResolution().getWidth(),GetMaxCameraResolution().getHeight(),ImageFormat.JPEG,2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener,null);
            PreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            ImageCaptureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            PreviewRequestBuilder.addTarget(PreviewSurface);
            ImageCaptureBuilder.addTarget(imageReader.getSurface());
            ApplyContrastFilter();
            if(isOISSupported())
                ImageCaptureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);

            cameraDevice.createCaptureSession(Arrays.asList(PreviewSurface,imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mCaptureRequest = PreviewRequestBuilder.build();
                    mcameraCaptureSession = cameraCaptureSession;
                    try {
                        mcameraCaptureSession.setRepeatingRequest(
                                mCaptureRequest,captureCallback,handler
                        );
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(context,"Failed to Create Session",Toast.LENGTH_LONG).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void CloseBgThread() {
        if(handlerThread!=null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join();
                handlerThread = null;
                handler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void OpenBgThread() {
        handlerThread = new HandlerThread("CameraThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public static void setOnFocusListener(OnCameraFocusListener onCameraFocusListener)
    {
        Camera.onCameraFocusListener = onCameraFocusListener;
    }

    private Size GetMaxCameraResolution()
    {
        Size Max = null;

        try {
           CameraCharacteristics cameraCharacteristics =cameraManager.getCameraCharacteristics(CurrentCamera);
           StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
           assert map!=null;
           Max = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizesByArea());
            PreviewSize = Max;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return Max;
    }

    private Size GetMiniCameraResolution()
    {
        Size Min = null;

        try {
            CameraCharacteristics cameraCharacteristics =cameraManager.getCameraCharacteristics(CurrentCamera);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map!=null;
            Min = Collections.min(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizesByArea());
            PreviewSize = Min;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return Min;
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


    public void CaptureImageNow()
    {
        try {
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(context,"ImageSaved",Toast.LENGTH_LONG).show();
                    if(BuildConfig.DEBUG)
                        Logger.Log("Image saved");
                    ResumePreview();
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Toast.makeText(context,"Failed",Toast.LENGTH_LONG).show();
                    if(BuildConfig.DEBUG)
                        Logger.Log("Image saved");
                }
            };
            mcameraCaptureSession.stopRepeating();
            mcameraCaptureSession.abortCaptures();
            mcameraCaptureSession.capture(ImageCaptureBuilder.build(),captureCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void ResumePreview(){
        try {
            mcameraCaptureSession.setRepeatingRequest(PreviewRequestBuilder.build(),captureCallback,handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void Set(CaptureRequest.Key key,Integer value){
        try {
            ImageCaptureBuilder.set(key, value);
            PreviewRequestBuilder.set(key, value);
            mcameraCaptureSession.setRepeatingRequest(PreviewRequestBuilder.build(), captureCallback, handler);
        }
        catch (CameraAccessException e){e.printStackTrace();}
    }

    private MeteringRectangle[] CalculateFocusRects(float x , float y)
    {
        Rect rect = new Rect(1,1,500,500);
        if(BuildConfig.DEBUG)
            Logger.Log(rect.toString());
        return new MeteringRectangle[]{new MeteringRectangle(rect,MeteringRectangle.METERING_WEIGHT_MAX)};
    }

    Object get(CaptureRequest.Key key)
    {
        return PreviewRequestBuilder.get(key);
    }

    private boolean isOISSupported()
    {
        boolean OIS = false;
        try {
            int length[] = cameraManager.getCameraCharacteristics(this.CurrentCamera).get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
            OIS = length !=null && length .length > 0;
        }
        catch(Exception e)
            {
                e.printStackTrace();
            }

            return OIS;
    }

    private void ApplyContrastFilter()
    {
        if(ImageCaptureBuilder == null || PreviewRequestBuilder == null)
            return;
        TonemapCurve tonemapCurve = ImageCaptureBuilder.get(CaptureRequest.TONEMAP_CURVE);
        if(tonemapCurve!=null)
        {
            float[][] channels = new float[3][];
            for(int channel = TonemapCurve.CHANNEL_RED; channel <= TonemapCurve.CHANNEL_BLUE;channel++)
            {
                float array[] = new float[tonemapCurve.getPointCount(channel)*2];
                for(int i = 0 ; i < array.length ; i++)
                    array[i] *= 0.5f;
                channels[channel] = array;
            }
            TonemapCurve tc = new TonemapCurve(channels[TonemapCurve.CHANNEL_RED],channels[TonemapCurve.CHANNEL_GREEN],channels[TonemapCurve.CHANNEL_BLUE]);
            ImageCaptureBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
            ImageCaptureBuilder.set(CaptureRequest.TONEMAP_CURVE,tc);
            PreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
            PreviewRequestBuilder.set(CaptureRequest.TONEMAP_CURVE,tc);
        }
    }

    private void ApplyHighQualityFilter()
    {
        if(ImageCaptureBuilder == null || PreviewRequestBuilder == null)
            return;
        ImageCaptureBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
        PreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
    }

    private void ProcessCameraoutputSizes()
    {
        if(CurrentCamera == null || cameraManager == null)
            return ;
       Resolutions = null;

        try {
            CameraCharacteristics cameraCharacteristics =cameraManager.getCameraCharacteristics(CurrentCamera);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map!=null;
            Resolutions = map.getOutputSizes(ImageFormat.JPEG);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static @Nullable Size[] getCameraresolutions()
    {
        return Resolutions;
    }


}


