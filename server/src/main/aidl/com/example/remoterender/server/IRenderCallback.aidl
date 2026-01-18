// IRenderCallback.aidl
package com.example.remoterender.server;

import android.view.SurfaceControlViewHost.SurfacePackage;

interface IRenderCallback {
    void onRemoteViewAdded(in SurfacePackage surfacePackage);
}