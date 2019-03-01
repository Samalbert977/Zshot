package com.camera.zshot.zshot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Sam on 1/30/2018.
 */

public class CustomView extends SurfaceView {
    SurfaceHolder surfaceHolder;
    Paint paint;
    Canvas canvas;
    public CustomView(Context context) {
        super(context);
        surfaceHolder = this.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
    }

    public void DrawView(float x , float y , boolean FOCUS , boolean IsFrontCamera)
    {
        paint.setStrokeWidth(2);
        this.invalidate();
        if(surfaceHolder.getSurface().isValid())
        {
            canvas = surfaceHolder.lockCanvas();
            if(canvas != null) {
                if (FOCUS || IsFrontCamera) {
                    if (IsFrontCamera) {
                        paint.setColor(Color.BLUE);
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        canvas.drawRect(x - 50, y + 50, x + 50, y - 50, paint);
                        surfaceHolder.unlockCanvasAndPost(canvas);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ClearCanvas();
                            }
                        }, 1000);
                    }
                    else
                    {
                        paint.setColor(Color.GREEN);
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        canvas.drawRect(x - 50, y + 50, x + 50, y - 50, paint);
                        surfaceHolder.unlockCanvasAndPost(canvas);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ClearCanvas();
                            }
                        },1000);
                    }
                }
                else
                {
                    paint.setColor(Color.RED);
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    canvas.drawRect(x - 50, y + 50, x + 50, y - 50, paint);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void ClearCanvas()
    {
        Canvas canvas1 = surfaceHolder.lockCanvas();
        if (canvas1 != null) {
            canvas1.drawColor(0, PorterDuff.Mode.CLEAR);
            surfaceHolder.unlockCanvasAndPost(canvas1);
        }
    }
}
