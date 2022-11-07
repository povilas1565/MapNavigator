package com.example.mapnavigator;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.tracecallbacks.TaskCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DistanceMatrixParser extends AsyncTask<String, Void, Map<String, String>> implements APIResponseParser {
	private TaskCallback taskCallback;
	
	public DistanceMatrixParser(){
		taskCallback = null;
	}
	
	public DistanceMatrixParser(Context context){
		taskCallback = (TaskCallback) context;
	}
	
	@Override
	public void startExecution(Context context, String response) {
		taskCallback = (TaskCallback) context;
		
		this.execute(response);
	}
	
	@Override
	protected void onPostExecute(Map<String, String> distance_duration){
		if(distance_duration != null){
			taskCallback.onDistanceMatrixDone(distance_duration);
		}
	}
	
	@Override
	protected Map<String, String> doInBackground(String... strings) {
		Map<String, String> distance_duration = null;
		JSONObject response;
		
		try {
			response = new JSONObject(strings[0]);
			distance_duration = parseRequest(response);
		}
		catch(Exception e){
			Log.d("DistanceMatrixParser::doInBackground", e.toString());
		}
		
		return distance_duration;
	}
	
	
	private Map<String, String> parseRequest(JSONObject response){
		Map<String, String> distance_duration = new HashMap<>();
		JSONArray rows;
		JSONObject elements;
		JSONObject distance, duration;
		
		try {
			rows = response.getJSONArray("rows");
			elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
			
			distance = elements.getJSONObject("distance");
			duration = elements.getJSONObject("duration");
			
			distance_duration.put("distance", distance.get("text").toString());
			distance_duration.put("duration", duration.get("text").toString());
		}
		catch(Exception e){
			Log.d("DistanceMatrixParser::parseRequest", e.toString());
		}
		
		return distance_duration;
	}
	
}
