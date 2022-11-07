package com.example.mapnavigator;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class DirectionsAPICaller extends AsyncTask<String, Void, String> {
    private Context context;
    private APIResponseParser task;
    
    public DirectionsAPICaller(Context context, APIResponseParser task){
        this.context = context;
        this.task = task;
    }

    @Override
    protected void onPostExecute(String str){
        super.onPostExecute(str);
        
        task.startExecution(context, str);
    }


    /**
     *
     * @param strings
     * @return
     */
    @Override
    protected String doInBackground(String... strings) {
        String data = "";
        URLConnection urlConnection = null;
        InputStream streamData = null;

        try {
            URL url = new URL(strings[0]);

            urlConnection = url.openConnection();
            urlConnection.connect();
            streamData = urlConnection.getInputStream();

            data = read(streamData);

            streamData.close();
        }
        catch(Exception e){
            Log.d("DirectionsApi::doInBackground", e.toString());
        }

        return data;
    }

    @NonNull
    private String read(InputStream streamData) throws Exception {
        InputStreamReader input = new InputStreamReader(streamData);
        BufferedReader bufferReader = new BufferedReader(input);
        StringBuffer stringBuffer = new StringBuffer();
        String iter = "";

        while((iter = bufferReader.readLine()) != null){
            stringBuffer.append(iter);
        }

        bufferReader.close();

        return stringBuffer.toString();
    }
}