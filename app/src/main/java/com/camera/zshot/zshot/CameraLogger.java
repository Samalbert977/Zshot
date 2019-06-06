package com.camera.zshot.zshot;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;

public class CameraLogger {

    private BufferedWriter bufferedWriter = null;
    private Calendar calendar = null;
    private File file;

    public CameraLogger()
    {
        String path = Environment.getExternalStorageDirectory().getPath();
        String logFileName = "Camera.log";
        file = new File(path.concat("/" + logFileName));
        try {
            if(!file.exists())
                if(!file.createNewFile())
                    throw new Exception("Could not create the Log File");
            bufferedWriter = new BufferedWriter(new FileWriter(file,true));
        } catch(Exception e){
            e.printStackTrace();
        }
        calendar = Calendar.getInstance();
    }

    public void Log(String Tag , String message)
    {
        try
        {
            String log = "Time:"+calendar.get(Calendar.HOUR)+":"+calendar.get(Calendar.MINUTE)+"Tag : "+Tag+" Message:"+message;
            bufferedWriter.write(log);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void Close()
    {
        try
        {
            if(bufferedWriter!=null)
            {
                bufferedWriter.close();
                bufferedWriter = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void Open()
    {
        try
        {
            if(bufferedWriter == null)
            {
                bufferedWriter = new BufferedWriter(new FileWriter(file,true));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


}
