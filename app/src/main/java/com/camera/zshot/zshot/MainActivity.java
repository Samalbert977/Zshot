package com.camera.zshot.zshot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageButton;


public class MainActivity extends AppCompatActivity implements SensorEventListener  {
        public static int ORIENTATION = 0;
        public static final int CAMERA_PERMISSION = 1;
        public static final int STORAGE_PERMISSION = 2;
        public static final int LOCATION_PERMISSION = 3;
        public static final int VIBRATOR_PERMISSION = 4;
        private boolean IS_PREVIEW_SURFACE_AVAILABLE = false;
        private static final int FLASH_AUTO = 0;
        private static final int FLASH_ON = 1;
        private static final int FLASH_OFF = 2;
        private static final int FLASH_TORCH_MODE = 3;
        private static final int HDR_OFF = -6;
        private static final int HDR_ON = -7;
        private final int LOAD_PREFERENCE =-5;
        private Camera camera = null;
        private String RearCamera,FrontCamera ,OpenedCamera;
        private Handler responseHandler;
        private float X,Y;
        private CustomView customView;
        private Surface CameraPreviewSurface;
        private FrameLayout drawingSurface;
        private int rotation = 0;
        private SensorManager sensorManager = null;
        private Sensor Accelerometer = null;
        private SharedPreferences sharedPreferences;
        private SharedPreferences.Editor editor;
        private final Keys keys = new Keys();
        private static CameraLogger logger;

        //UI variable
        ImageButton ShutterButton , FlashButton , HDR_Button;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logger = new CameraLogger();
        StartApplication();
        }
        @Override
        protected void onResume() {
            super.onResume();
            if(ViewConfiguration.get(this).hasPermanentMenuKey())
                HideActionBar();
            else
                HideNavigationAndActionBar();
            if(camera==null && IS_PREVIEW_SURFACE_AVAILABLE){
                camera = new Camera(this,CameraPreviewSurface);
                camera.OpenCamera(OpenedCamera);}
                if(sensorManager!=null && Accelerometer!=null)
                    sensorManager.registerListener(this,Accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
            if(logger!=null)
                logger.Open();

        }
        @Override
        protected void onPause() {
        super.onPause();
        if(camera!=null) {
            camera.CloseCamera();
             camera = null;}
            if(sensorManager!=null)
                sensorManager.unregisterListener(this, Accelerometer);


            ImageButton Flash = findViewById(R.id.FlashButton);
            Flash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ControlFlash();
                }
            });

            if(logger!=null)
                logger.Close();
    }

    @SuppressLint({"CommitPrefEdits", "ClickableViewAccessibility"})
    private void StartApplication() {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            editor = sharedPreferences.edit();
        responseHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(BuildConfig.DEBUG)
                    logger.Log(msg.toString());
                if(Integer.parseInt(msg.obj.toString()) == Camera.STATE_FOCUSING)
                    customView.DrawView(X,Y,false,false);
                else if(Integer.parseInt(msg.obj.toString()) == Camera.STATE_FOCUSED)
                    customView.DrawView(X,Y,true,false);
                else if(Integer.parseInt(msg.obj.toString())==LOAD_PREFERENCE)
                    LoadPreferences();
                return false;
            }
        });
        FlashButton = findViewById(R.id.FlashButton);
        HDR_Button = findViewById(R.id.HDR_Button);
        drawingSurface = findViewById(R.id.DrawingSurface);
        customView = new CustomView(this);
        drawingSurface.addView(customView);
        SetupSurfaceView();
        //previewSurface1.addView(SetupSurfaceView(previewSurface1.getWidth(),previewSurface1.getHeight()));

        Camera.setOnFocusListener(new OnCameraFocusListener() {
            @Override
            public void OnFocus(int focus) {
                switch (focus)
                {
                    case Camera.STATE_FOCUSING:
                        Message message = new Message();
                        message.obj = Camera.STATE_FOCUSING;
                        responseHandler.sendMessage(message);
                        break;
                    case Camera.STATE_FOCUSED:
                        Message message1 = new Message();
                        message1.obj = Camera.STATE_FOCUSED;
                        responseHandler.sendMessage(message1);
                        break;
                }
            }

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void OnCameraOpened(boolean State) {
                if(State)
                {
                    drawingSurface.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            X = event.getX();
                            Y = event.getY();
                            camera.FocusOnTap(X,Y);
                            if(BuildConfig.DEBUG)
                                logger.Log("X = "+X+"Y = "+Y);
                            return false;
                        }
                    });
                    Message message = new Message();
                    message.obj = LOAD_PREFERENCE;
                    responseHandler.sendMessage(message);
                }
            }
        });
        ShutterButton = findViewById(R.id.ShutterButton);
        ShutterButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if(action==MotionEvent.ACTION_DOWN)
                    ShutterButton.setImageResource(R.mipmap.capturex);
                else if(action==MotionEvent.ACTION_UP)
                    ShutterButton.setImageResource(R.mipmap.capture2);
                return false;
            }
        });
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager!=null)
        {
            Accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        FlashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ControlFlash();
            }
        });
    }
    private void SetupSurfaceView()
    {
        SurfaceView surfaceView = findViewById(R.id.Preview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                IS_PREVIEW_SURFACE_AVAILABLE = true;
                CameraPreviewSurface = holder.getSurface();
                DetectCameras();
                if(!CheckSelfPermission(CAMERA_PERMISSION))
                    RequestPermission(CAMERA_PERMISSION);
                else{
                    camera = new Camera(MainActivity.this,CameraPreviewSurface);
                    camera.OpenCamera(OpenedCamera);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                IS_PREVIEW_SURFACE_AVAILABLE = false;
            }
        });
    }
    @Deprecated
    private SurfaceView SetupSurfaceView(int width,int height)
    {
        SurfaceView surfaceView = new SurfaceView(this);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setFixedSize(width,height);
        surfaceHolder.setKeepScreenOn(true);
        final SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                IS_PREVIEW_SURFACE_AVAILABLE = true;
                CameraPreviewSurface = holder.getSurface();
                DetectCameras();
                if(CheckSelfPermission(CAMERA_PERMISSION))
                    RequestPermission(CAMERA_PERMISSION);
                else{
                    camera = new Camera(MainActivity.this,CameraPreviewSurface);
                    camera.OpenCamera(OpenedCamera);
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                IS_PREVIEW_SURFACE_AVAILABLE =false;
            }
        };
        surfaceHolder.addCallback(callback);
        return surfaceView;
    }

    private void DetectCameras() {
        CameraManager cameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try {
            for(String  CameraID : cameraManager.getCameraIdList())
            {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(CameraID);
                //noinspection ConstantConditions
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK)
                    RearCamera = CameraID;
                else //noinspection ConstantConditions
                    if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING )==CameraCharacteristics.LENS_FACING_FRONT)
                        FrontCamera = CameraID;

                OpenedCamera = RearCamera;
            }
        }catch (CameraAccessException e){e.printStackTrace();}
    }


    public boolean CheckSelfPermission(int REQUEST_CODE)
    {
        switch (REQUEST_CODE)
        {
            case CAMERA_PERMISSION:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            case STORAGE_PERMISSION:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            case LOCATION_PERMISSION:
                return ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            case VIBRATOR_PERMISSION:
                return ContextCompat.checkSelfPermission(this,Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED;
            default:
                return true;

        }
    }


    public void RequestPermission(int REQUEST_CODE)
    {
        switch (REQUEST_CODE)
        {
            case CAMERA_PERMISSION:
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
                break;
            case STORAGE_PERMISSION:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        ,STORAGE_PERMISSION);
                break;
            case LOCATION_PERMISSION:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION}
                        ,LOCATION_PERMISSION);
                break;
            case VIBRATOR_PERMISSION:
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.VIBRATE},VIBRATOR_PERMISSION);

        }
    }


    private void HideNavigationAndActionBar() {
        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    private void HideActionBar() {
        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_FULLSCREEN|
                View.SYSTEM_UI_FLAG_IMMERSIVE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case CAMERA_PERMISSION :
                if(grantResults.length >0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    this.finish();
                }
                else
                {
                    if(camera!=null && IS_PREVIEW_SURFACE_AVAILABLE) {
                        try {
                            camera.OpenCamera(OpenedCamera);
                        }catch (NullPointerException e){e.printStackTrace();}
                    }
                }
                break;
        }
    }


    public void TakePicture(View view) {
        if(!CheckSelfPermission(STORAGE_PERMISSION))
            RequestPermission(STORAGE_PERMISSION);
        else{
            ORIENTATION = rotation;
            camera.CaptureImageNow();}
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float x_axis = event.values[0];
        float y_axis = event.values[1];

        // total = (x_axis+y_axis+z_axis+45)%360;
        if (x_axis <= -7.9 && x_axis >= -9.55) {
            rotation = 3;
        } else if (x_axis <= 8.1 && x_axis >= -7.8 && y_axis >= 4.7) {
            rotation = 0;
        } else if (x_axis >= 8.2 && x_axis <= 10.1) {
            rotation = 1;
        }
        CalculateUIRotation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void CalculateUIRotation()
    {
        float Degree;
        switch (rotation) {
            case 0:
                Degree = 270.0f;
                Rotate_UI_By(Degree);
                break;
            case 3:
                Degree = 180.0f;
                Rotate_UI_By(Degree);
                break;
            case 1:
                Degree = 0.0f;
                Rotate_UI_By(Degree);
                break;
        }
    }

    private void Rotate_UI_By(float Degree)
    {
        FlashButton.setRotation(Degree);
        HDR_Button.setRotation(Degree);
    }


    public void ControlFlash() {

           switch (sharedPreferences.getInt(keys.FLASH_KEY,FLASH_AUTO))
           {
               case FLASH_AUTO:
                   FlashButton.setImageResource(R.drawable.ic_flash_on_black_24dp);
                   camera.Set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                   camera.Set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_SINGLE);
                   editor.putInt(keys.FLASH_KEY,FLASH_ON);
                   break;
               case FLASH_ON:
                   FlashButton.setImageResource(R.drawable.ic_flash_off_black_24dp);
                   camera.Set(CaptureRequest.CONTROL_AE_MODE,CameraMetadata.CONTROL_AE_MODE_ON);
                   camera.Set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_OFF);
                   editor.putInt(keys.FLASH_KEY,FLASH_OFF);
                   break;
               case FLASH_OFF:
                   FlashButton.setImageResource(R.drawable.ic_flash_auto_black_24dp);
                   camera.Set(CaptureRequest.CONTROL_AE_MODE,CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                   //camera.Set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_);
                   editor.putInt(keys.FLASH_KEY,FLASH_AUTO);
                   break;
           }
        editor.apply();
    }

    private void LoadPreferences(){
        switch (sharedPreferences.getInt(keys.FLASH_KEY,FLASH_AUTO))
        {
            case FLASH_AUTO:
                FlashButton.setImageResource(R.drawable.ic_flash_auto_black_24dp);
                camera.Set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                break;
            case FLASH_OFF:
                FlashButton.setImageResource(R.drawable.ic_flash_off_black_24dp);
                camera.Set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
                break;
            case FLASH_ON:
                FlashButton.setImageResource(R.drawable.ic_flash_on_black_24dp);
                camera.Set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                break;
        }
        switch (sharedPreferences.getInt(keys.HDR_KEY,HDR_OFF))
        {
            case HDR_OFF:
                camera.Set(CaptureRequest.CONTROL_SCENE_MODE, CaptureResult.CONTROL_SCENE_MODE_DISABLED);
                HDR_Button.setImageResource(R.drawable.ic_hdr_off_black_24dp);
                break;
            case HDR_ON:
                camera.Set(CaptureRequest.CONTROL_SCENE_MODE,CaptureRequest.CONTROL_SCENE_MODE_HDR);
                HDR_Button.setImageResource(R.drawable.ic_hdr_on_black_24dp);
                break;
        }
    }

    public void ControlHDR(View view)  {
        switch (sharedPreferences.getInt(keys.HDR_KEY,HDR_OFF))
        {
            case HDR_OFF:
                camera.Set(CaptureRequest.CONTROL_SCENE_MODE,CaptureRequest.CONTROL_SCENE_MODE_HDR);
                HDR_Button.setImageResource(R.drawable.ic_hdr_on_black_24dp);
                editor.putInt(keys.HDR_KEY,HDR_ON);
                break;
            case HDR_ON:
                camera.Set(CaptureRequest.CONTROL_SCENE_MODE, CaptureResult.CONTROL_SCENE_MODE_DISABLED);
                HDR_Button.setImageResource(R.drawable.ic_hdr_off_black_24dp);
                editor.putInt(keys.HDR_KEY,HDR_OFF);
                break;
        }
        editor.apply();
    }

    static CameraLogger getLoggerInstance()
    {
        return logger;
    }


}
