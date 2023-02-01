package com.example.mediaplayproject.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * @author wm
 */
public class MusicPlayService extends Service {
    public MusicPlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}