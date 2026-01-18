// IRenderService.aidl
package com.example.remoterender.server;

import com.example.remoterender.server.IRenderCallback;
import android.os.IBinder;

interface IRenderService {
    void addRemoteView(int width,int height,IBinder hostToken,IRenderCallback callback);
    void setRemoteViewVisible(boolean visible);
}