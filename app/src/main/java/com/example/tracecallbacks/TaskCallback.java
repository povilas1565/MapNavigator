package com.example.tracecallbacks;

public interface TaskCallback {
    void onRouteDone(Object... values);
    void onDistanceMatrixDone(Object... values);
}
