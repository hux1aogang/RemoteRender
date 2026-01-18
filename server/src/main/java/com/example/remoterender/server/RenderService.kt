package com.example.remoterender.server

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.SurfaceControlViewHost
import android.widget.ImageView
import androidx.core.view.isVisible

class RenderService : Service() {
    companion object {
        const val TAG = "RenderService"
    }

    private var scvh: SurfaceControlViewHost? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder {
        return object : IRenderService.Stub() {
            override fun addRemoteView(
                width: Int,
                height: Int,
                hostToken: IBinder?,
                callback: IRenderCallback?
            ) {
                Log.d(TAG, "addRemoteView: ")
                handler.post {
                    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                    val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                    scvh = SurfaceControlViewHost(
                        this@RenderService,
                        display,
                        hostToken
                    )
                    val imageView = ImageView(applicationContext).apply {
                        isVisible = false
                        setImageResource(R.drawable.cat)
                        scaleType = ImageView.ScaleType.FIT_XY
                    }
                    scvh!!.setView(imageView, width, height)
                    callback?.onRemoteViewAdded(scvh!!.surfacePackage)
                }
            }

            override fun setRemoteViewVisible(visible: Boolean) {
                Log.d(TAG, "setRemoteViewVisible: $visible" + scvh?.view)
                handler.post {
                    scvh?.view?.isVisible = visible
                }
            }
        }
    }

}