package com.example.remoterender.client

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import android.window.SurfaceSyncGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.example.remoterender.server.IRenderCallback
import com.example.remoterender.server.IRenderService
import com.google.android.material.math.MathUtils


class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        val COLOR_START = Color.parseColor("#6AA36A")
        val COLOR_END = Color.parseColor("#5ab7d0")
    }

    private var remoteService: IBinder? = null
    private var render: IRenderService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnected: ")
            remoteService = service
            render = IRenderService.Stub.asInterface(service)
            showToast("服务连接成功")

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected: ")
            remoteService = null
            render = null
            showToast("服务断开")
        }
    }

    private val button: View
        get() = findViewById(R.id.btn_test)
    private var surfaceView: SurfaceView? = null

    private val selfRunnable = Runnable {
        Log.d(TAG, "self ready!")
    }

    private val remoteRunnable = Runnable {
        Log.d(TAG, "remote ready!")
        render?.setRemoteViewVisible(true)
    }

    private var animator: Animator? = null
    private var remoteViewContainer: ViewGroup? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindService()
        button.setOnClickListener {
            Log.d(TAG, "onClick: " + render)
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                setDuration(500)
                interpolator = AccelerateInterpolator()
                val width = resources.getDimensionPixelOffset(R.dimen.sv_width).toFloat()
                val height = resources.getDimensionPixelOffset(R.dimen.sv_height).toFloat()
                addUpdateListener { animation ->
                    val fraction = animation.animatedValue as Float
                    button.setBackgroundColor(
                        ArgbEvaluator.getInstance()
                            .evaluate(fraction, COLOR_START, COLOR_END) as Int
                    )
                    button.scaleX =
                        MathUtils.lerp(1f, width / button.width, fraction)
                    button.scaleY =
                        MathUtils.lerp(1f, height / button.height, fraction)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        prepareRemoteView()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        remoteViewContainer?.isVisible = true
                    }
                })
            }
            animator!!.start()
        }
    }


    override fun onResume() {
        super.onResume()
        if (remoteService == null || !remoteService?.isBinderAlive!!) {
            bindService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun bindService() {
        val intent = Intent("com.example.remoterender.server.RENDER_SERVICE")
        intent.`package` = "com.example.remoterender.server"
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun prepareRemoteView() {
        remoteViewContainer = FrameLayout(this)
        remoteViewContainer!!.visibility = View.INVISIBLE
        surfaceView = SurfaceView(this)
        surfaceView!!.setZOrderOnTop(true)
        val width = resources.getDimensionPixelOffset(R.dimen.sv_width)
        val height = resources.getDimensionPixelOffset(R.dimen.sv_height)
        val lp = FrameLayout.LayoutParams(width, height)
        lp.gravity = Gravity.CENTER
        remoteViewContainer!!.addView(surfaceView, lp)
        val dialog = AlertDialog.Builder(this).setView(remoteViewContainer).create()
        dialog.setOnShowListener {
            render?.addRemoteView(
                width,
                height,
                surfaceView?.hostToken,
                object : IRenderCallback.Stub() {
                    override fun onRemoteViewAdded(surfacePackage: SurfaceControlViewHost.SurfacePackage) {
                        Log.d(TAG, "onRemoteViewAdded: surfacePackage=${surfacePackage}")
                        runOnUiThread {
                            val syncGroup = SurfaceSyncGroup("remote_render")
                            syncGroup.add(button.rootView.rootSurfaceControl, selfRunnable)
                            syncGroup.add(surfacePackage, remoteRunnable)
                            surfaceView!!.setChildSurfacePackage(surfacePackage)
                            syncGroup.markSyncReady()
                        }
                    }
                })
        }
        dialog.setOnDismissListener {
            surfaceView = null
            remoteViewContainer = null
            resetButton()
        }
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0f)
        }
        dialog.show()
    }

    private fun resetButton() {
        button.setBackgroundColor(COLOR_START)
        button.scaleX = 1f
        button.scaleY = 1f
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }
}