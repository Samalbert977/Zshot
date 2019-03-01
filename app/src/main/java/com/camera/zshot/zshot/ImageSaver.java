package com.camera.zshot.zshot;

import android.annotation.SuppressLint;
import android.media.Image;
import android.support.media.ExifInterface;

import com.camera.zshot.zshot.ui.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Sam on 2/1/2018.
 */

public class ImageSaver implements Runnable {

    private final Image mImage;
    private final File mFile;

    public ImageSaver(Image image, File file) {
        mImage = image;
        mFile = file;
    }

    @Override
    public void run() {
            if (!mFile.exists())
                if (!mFile.mkdirs())
                    return;
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            File Image = getFile();
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(Image);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        try {
            ExifInterface(Image.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getFile()
    {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "Zshot_" + timeStamp + ".jpg";
        return  new File(mFile.getPath() + File.separator + fileName);
    }

    private void ExifInterface(String path) throws IOException {
        ExifInterface exifInterface = new ExifInterface(path);
        if(MainActivity.ORIENTATION==0)
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION,String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
        else if(MainActivity.ORIENTATION==3)
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION,String.valueOf(ExifInterface.ORIENTATION_ROTATE_180));
        exifInterface.saveAttributes();
    }

}
