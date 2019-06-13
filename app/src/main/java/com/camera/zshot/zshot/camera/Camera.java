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
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import com.camera.zshot.zshot.BuildConfig;
import com.camera.zshot.zshot.CameraLogger;
import com.camera.zshot.zshot.ImageSaver;
import com.camera.zshot.zshot.keys.Keys;
import com.camera.zshot.zshot.ui.MainActivity;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.content.Context.CAMERA_SERVICE;
import static android.hardware.camera2.CameraDevice.StateCallback;

public class Camera {
    private static Size PreviewSize = null;
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
    private Size CurrentResolution;
    private final String TAG = "Camera.java";


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
                Logger.Log(TAG,"Camera Opened");
                cameraDevice = camera;
                CreateCameraPreviewSession();
                onCameraFocusListener.OnCameraOpened(true);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                if(BuildConfig.DEBUG)
                    Logger.Log(TAG,"Camera Disconnected");
                camera.close();
                cameraDevice =null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                if(BuildConfig.DEBUG)
                    Logger.Log(TAG,"Error on opening camera");
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
        if(BuildConfig.DEBUG)
            Logger.Log(TAG,"Opening Camera");
        this.CurrentCamera = CameraID;
        OpenBgThread();
        cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        try {
            assert cameraManager != null;
            String res = PreferenceManager.getDefaultSharedPreferences(context).getString(Keys.ImageResolutionKey,null);
            if(res!=null) {
                String[] buff = res.split("_");
                if (buff.length != 0) {
                    CurrentResolution = new Size(Integer.parseInt(buff[0]), Integer.parseInt(buff[1]));
                }
            }
            else {
                CurrentResolution = GetMaxCameraResolution();
            }
            if (BuildConfig.DEBUG) {
                Logger.Log(TAG,"Resolution is : " + CurrentResolution.getWidth()+" x "+CurrentResolution.getHeight());
            }
            cameraManager.openCamera(CameraID,stateCallback,handler);
            ProcessCameraOutputSizes();
        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
        }
        if(BuildConfig.DEBUG)
            Logger.Log(TAG,"Camera Opened");
    }

    public void CloseCamera() {
        if(BuildConfig.DEBUG)
            Logger.Log(TAG,"Closing Camera");
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
        if(BuildConfig.DEBUG)
            Logger.Log(TAG,"Camera Closed");
    }

    @Deprecated
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

    public void FocusOnTap(float x ,float y,int width,int height)
    {
        FocusState = STATE_WAIT_FOCUS_LOCK;
        PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        PreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, CalculateFocusRect(x,y,width,height));
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
            imageReader = ImageReader.newInstance(CurrentResolution.getWidth(),CurrentResolution.getHeight(),ImageFormat.JPEG,2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener,null);
            PreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            ImageCaptureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            PreviewRequestBuilder.addTarget(PreviewSurface);
            ImageCaptureBuilder.addTarget(imageReader.getSurface());
            if(isOISSupported())
                ImageCaptureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);

            cameraDevice.createCaptureSession(Arrays.asList(PreviewSurface,imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    PreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
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
                        Logger.Log(TAG,"Image saved");
                    ResumePreview();
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Toast.makeText(context,"Failed",Toast.LENGTH_LONG).show();
                    if(BuildConfig.DEBUG)
                        Logger.Log(TAG,"Image save failed");
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
            ImageCaptureBuilder.build();
            mcameraCaptureSession.setRepeatingRequest(PreviewRequestBuilder.build(), captureCallback, handler);
        }
        catch (CameraAccessException e){e.printStackTrace();}
    }

    private @Nullable MeteringRectangle[] CalculateFocusRect(float x , float y, int width, int height)
    {
        MeteringRectangle[] meteringRectangles = null;
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(CurrentCamera);
            final Rect sensorArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            y = (int)((x / (float)width)  * (float)sensorArraySize.height());
            x = (int)((y / (float)height) * (float)sensorArraySize.width());

            x = x > sensorArraySize.width()-1 ? sensorArraySize.width()-1 : x;
            y = y > sensorArraySize.height()-1 ? sensorArraySize.height()-1 : y;

            MeteringRectangle meteringRectangle = new MeteringRectangle(0 ,0, (int)x, (int)y, MeteringRectangle.METERING_WEIGHT_MAX);
            meteringRectangles = new MeteringRectangle[]{meteringRectangle};
            if (BuildConfig.DEBUG) {
                Logger.Log(TAG,"x = "+x+" y = "+y);
            }
            return new MeteringRectangle[]{meteringRectangle};
        }
        catch(Exception e)
        {
            if(BuildConfig.DEBUG)
                Logger.Log(TAG,e.getMessage());
        }
        return meteringRectangles;
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
        if(BuildConfig.DEBUG)
        {
            Logger.Log(TAG,"OIS is supported : "+OIS);
        }
            return OIS;
    }

    public void ApplyContrastFilter(float R , float G, float B)
    {
        if(ImageCaptureBuilder == null || PreviewRequestBuilder == null) {
            Logger.Log(TAG,"ImageCaptureBuilder is null or PreviewRequestBuilder is null , returning...");
            return;
        }
        float offset;
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(CurrentCamera);
        }catch (Exception e)
        {
            if(BuildConfig.DEBUG)
                Logger.Log(TAG,e.getMessage());
        }
        if(cameraCharacteristics == null)
        {
            if(BuildConfig.DEBUG)
                Logger.Log(TAG,"cameraCharacteristics is  null, returning from API ApplyContrastFilter()");
            return;
        }
        TonemapCurve tonemapCurve = PreviewRequestBuilder.get(CaptureRequest.TONEMAP_CURVE);
        if(tonemapCurve!=null)
        {
            float[][] channels = new float[3][];
            for(int channel = TonemapCurve.CHANNEL_RED; channel <= TonemapCurve.CHANNEL_BLUE;channel++)
            {
                float array[] = new float[tonemapCurve.getPointCount(channel)*2];
                switch (channel)
                {
                    case TonemapCurve.CHANNEL_RED:
                        offset = R;
                        break;
                    case TonemapCurve.CHANNEL_GREEN:
                        offset = G;
                        break;
                    case TonemapCurve.CHANNEL_BLUE:
                        offset = B;
                        break;
                    default:
                        offset = 0;
                }
//                for(int i = 0 ; i < array.length ; i++)
//                    array[i] = 1*offset;
               channels[channel] = array;

                tonemapCurve.copyColorCurve(channel,array,(int)offset);
            }
            if(BuildConfig.DEBUG)
            {
                Logger.Log(TAG,"RGB Values applied by camera are : ["+R+"] ["+G+"] ["+B+"]");
            }
            TonemapCurve tc = new TonemapCurve(channels[TonemapCurve.CHANNEL_RED],channels[TonemapCurve.CHANNEL_GREEN],channels[TonemapCurve.CHANNEL_BLUE]);
            ImageCaptureBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
            ImageCaptureBuilder.set(CaptureRequest.TONEMAP_CURVE,tc);
            PreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
            PreviewRequestBuilder.set(CaptureRequest.TONEMAP_CURVE,tc);
            ImageCaptureBuilder.build();
            ResumePreview();
        }
        else
        {
            Logger.Log(TAG,"ToneMap curve is null , not applying RGB curves");
        }
    }

    public void ApplyHighQualityFilter()
    {
        if(ImageCaptureBuilder == null || PreviewRequestBuilder == null) {
            Logger.Log(TAG,"Image Builder is null or Preview Builder is null , not applying high quality filter");
            return;
        }
        ImageCaptureBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
        PreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
        ImageCaptureBuilder.build();
        ResumePreview();
    }

    private void ProcessCameraOutputSizes()
    {
        if(BuildConfig.DEBUG)
        {
            Logger.Log(TAG,"ProcessCameraOutputSizes called");
        }

        if(CurrentCamera == null || cameraManager == null) {
            if(BuildConfig.DEBUG)
            {
                Logger.Log(TAG,"Camera Device is null , returning");
            }
            return;
        }
       Resolutions = null;

        try {
            CameraCharacteristics cameraCharacteristics =cameraManager.getCameraCharacteristics(CurrentCamera);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map!=null;
            Resolutions = map.getOutputSizes(ImageFormat.JPEG);
            if(BuildConfig.DEBUG) {
                Logger.Log(TAG,Arrays.toString(Resolutions));
            }
            } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static @Nullable Size[] getCameraResolutions()
    {
        return Resolutions;
    }



}


