package com.example.markers;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.Marker;

public class MarkerRoute {
	private @NonNull Marker source;
	private Marker destination;
	private String route_mode;
	private String distance;
	private String time;
	
	public MarkerRoute(@NonNull Marker source){
		this.source = source;
	}
	
	@NonNull
	public Marker getSource() {
		return source;
	}
	
	public void setSource(@NonNull Marker source) {
		this.source = source;
	}
	
	public Marker getDestination() {
		return destination;
	}
	
	public void setDestination(Marker destination) {
		this.destination = destination;
	}
	
	public String getRoute_mode() {
		return route_mode;
	}
	
	public void setRoute_mode(String route_mode) {
		this.route_mode = route_mode;
	}
	
	public String getDistance() {
		return distance;
	}
	
	public void setDistance(String distance) {
		this.distance = distance;
	}
	
	public String getTime() {
		return time;
	}
	
	public void setTime(String time) {
		this.time = time;
	}
}
