package com.example.mapnavigator;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.example.tracecallbacks.TaskCallback;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RouteParser extends AsyncTask<String, Integer, List<List<ConcurrentHashMap<String, String>>>> implements APIResponseParser {
    private TaskCallback taskCallback;
    
    public RouteParser(){
        taskCallback = null;
    }
    
    public RouteParser(Context context){
        taskCallback = (TaskCallback) context;
    }
    
    @Override
    public void startExecution(Context context, String response){
        taskCallback = (TaskCallback)context;
        
        this.execute(response);
    }

    @Override
    protected void onPostExecute(List<List<ConcurrentHashMap<String, String>>> result){
        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;

        for(int i = 0; i < result.size(); ++i){
            points = new ArrayList<>();
            lineOptions = new PolylineOptions();

            List<ConcurrentHashMap<String, String>> path = result.get(i);

            for(int j = 0; j < path.size(); ++j){
                ConcurrentHashMap<String, String> point = path.get(j);
                LatLng coordinate = new LatLng(Double.parseDouble(point.get("lat")), Double.parseDouble(point.get("long")));

                points.add(coordinate);
            }

            lineOptions.addAll(points);
            lineOptions.width(20);
            lineOptions.color(Color.BLUE);
        }

        if(lineOptions != null){
            taskCallback.onRouteDone(lineOptions);
        }
    }

    @Override
    protected List<List<ConcurrentHashMap<String, String>>> doInBackground(String... strings) {
        JSONObject response;
        List<List<ConcurrentHashMap<String, String>>> routes = null;

        try {
            response = new JSONObject(strings[0]);
            routes = parseRoutes(response);
        }
        catch(Exception e){
            Log.d("RouteParser::doInBackground", e.toString());
        }

        return routes;
    }

    private List<List<ConcurrentHashMap<String, String>>> parseRoutes(JSONObject response){
        List<List<ConcurrentHashMap<String, String>>> routes = new ArrayList<>();

        JSONArray jsonRoutes, jsonLegs, jsonSteps;

        try {
            jsonRoutes = response.getJSONArray("routes");

            for(int i = 0; i < jsonRoutes.length(); ++i){
                jsonLegs = ((JSONObject)jsonRoutes.get(i)).getJSONArray("legs");
                List<ConcurrentHashMap<String, String>> path = new ArrayList<>();

                for(int j = 0; j < jsonLegs.length(); ++j){
                    jsonSteps = ((JSONObject)jsonLegs.get(j)).getJSONArray("steps");

                    for(int k = 0; k < jsonSteps.length(); ++k){
                        String polyline = ((JSONObject)((JSONObject)jsonSteps.get(k)).get("polyline")).get("points").toString();
                        List<LatLng> polyLines = decodePolyline(polyline);

                        for(int l = 0; l < polyLines.size(); ++l){
                            ConcurrentHashMap<String, String> coordinateMap = new ConcurrentHashMap<>();

                            coordinateMap.put("lat", Double.toString(polyLines.get(l).latitude));
                            coordinateMap.put("long", Double.toString(polyLines.get(l).longitude));

                            path.add(coordinateMap);
                        }
                    }

                    routes.add(path);
                }
            }
        }
        catch(Exception e){
            Log.d("RouteParser::parseRoutes", e.toString());
        }

        return routes;
    }

    private List<LatLng> decodePolyline(String path){
        return PolyUtil.decode(path);
    }
}
