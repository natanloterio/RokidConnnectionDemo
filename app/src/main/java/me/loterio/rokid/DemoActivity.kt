package me.loterio.rokid

import android.Manifest
import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oxsightglobal.R
import com.oxsightglobal.databinding.PreviewSingleBinding
import com.rokid.axr.phone.glasscamera.RKGlassCamera
import com.rokid.axr.phone.glasscamera.RKGlassCamera.RokidCameraCallback
import com.rokid.axr.phone.glasscamera.callback.OnGlassCameraConnectListener
import com.rokid.axr.phone.glassdevice.RKGlassDevice
import com.rokid.axr.phone.glassdevice.callback.OnGlassDeviceConnectListener
import com.rokid.axr.phone.glassdevice.hw.GlassConfig
import com.rokid.axr.phone.glassdevice.hw.RKKeyProcessor
import com.rokid.axr.phone.glassdevice.hw.listener.KeyEventType
import com.rokid.axr.phone.glassdevice.hw.listener.RKKeyListener
import com.rokid.logger.RKLogger


class DemoActivity: AppCompatActivity(), RKKeyListener {

    private var device: UsbDevice? = null
    private var presentation: Presentation? = null
    private var surface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo_activity)
        assurePermissions()

        //initSurface()

        attachListenerToButtonsEvents()

        connect()

        findViewById<TextureView>(R.id.preview).surfaceTextureListener = getSurfaceListener()
    }

    private fun assurePermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 666
        )
    }

    private fun initSurface() {
        //use the OnSurfaceListener provided by viewModel
        //to display on glass with presentation, on phone the view is black.
        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).let {
            presentation = object : Presentation(this, it.displays[it.displays.size.minus(1)]) {
                private lateinit var binding: PreviewSingleBinding
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    binding = PreviewSingleBinding.inflate(layoutInflater)
                    binding.lifecycleOwner = this@DemoActivity
                    setContentView(binding.root)
                    //do things when view is ready
                    binding.texture.surfaceTextureListener = getSurfaceListener()
                }
            }
            //presentation?.show()
        }
        findViewById<TextureView>(R.id.preview).surfaceTextureListener = getSurfaceListener()
    }

    fun getSurfaceListener(): TextureView.SurfaceTextureListener {
        return object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                    p0: SurfaceTexture,
                    p1: Int,
                    p2: Int
            ) {//make sure surface is created after texture is available
                surface = Surface(p0)
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                //in this test nothing to do
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                if (RKGlassCamera.getInstance().isRecording) {
                    RKGlassCamera.getInstance().stopRecord()
                }
                //If texture is destroyed stop preview.
                stopPreview()

                return true
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                //in this test nothing to do
            }

        }
    }

    private fun attachListenerToButtonsEvents() {
        RKGlassDevice.getInstance().setRkKeyListener(this)
    }

    private fun connect() {
        RKGlassDevice.getInstance().init(connectListener)
        val config = GlassConfig()
        config.clickDelayTime = 200
        config.longClickTime = 2000
        RKKeyProcessor.getInstance().setGlassConfig(config)
        RKGlassDevice.getInstance().setRKKeyProcessor(RKKeyProcessor.getInstance())
    }



    val connectListener: OnGlassDeviceConnectListener by lazy {
        object : OnGlassDeviceConnectListener {
            override fun onGlassDeviceConnected(usbDevice: UsbDevice?) {//call when glass is connected

                findViewById<View>(R.id.holder)?.background = resources.getDrawable(R.color.green_400)
                findViewById<TextView>(R.id.connectionStatus)?.text = resources.getText(R.string.connected)
                findViewById<TextView>(R.id.buttonStatus)?.text = resources.getText(R.string.press_glass_button)

                initSurface()
                startCamera()

            }

            override fun onGlassDeviceDisconnected() {//call when glass is disconnected
                device = null
                findViewById<View>(R.id.holder)?.background = resources.getDrawable(R.color.red_400)
                findViewById<TextView>(R.id.connectionStatus)?.text = resources.getText(R.string.disconnected)
                findViewById<TextView>(R.id.buttonStatus)?.text = resources.getText(R.string.plug_the_glasses_to_the_device)

                stopPreview()
            }
        }
    }

    //Camera Connection listener, to open camera immediately when camera is connected.
    private val cameraConnectionListener: OnGlassCameraConnectListener by lazy {
        object : OnGlassCameraConnectListener {
            override fun onGlassCameraConnected(p0: UsbDevice?) {
                RKGlassCamera.getInstance().openCamera()
            }

            override fun onGlassCameraDisconnected() {
                stopPreview()
            }

        }
    }
    private fun startCamera() {
        RKGlassCamera.getInstance().init(cameraConnectionListener)

        RKGlassCamera.getInstance().setCameraCallback(object : RKGlassCamera.RokidCameraCallback {

            override fun onOpen() {//when camera is opened
                startPreView()
            }

            override fun onClose() {
                //in this test nothing to do
            }

            override fun onStartPreview() {//when preview is started.

                RKGlassCamera.getInstance().supportedPreviewSizes?.forEach {
                    RKLogger.e("it = ${it.width}  *  ${it.height}")
                }

            }

            override fun onStopPreview() {//when stopped.

            }

            override fun onError(p0: Exception?) {//when some error.
                p0?.printStackTrace()
            }

            override fun onStartRecording() {
            }

            override fun onStopRecording() {
            }

        })

        RKGlassCamera.getInstance().addOnPreviewFrameListener { byteArrayValue, timeStamp ->
            //add this interface to use images provided by preview, also timestamp can be find.
        }

    }

    override fun onPowerKeyEvent(p0: Int) {
        runOnUiThread {
            if (p0 == KeyEventType.LONG_CLICK){
                findViewById<TextView>(R.id.buttonStatus)?.text = "Long click"
            } else {
                findViewById<TextView>(R.id.buttonStatus)?.text = "Short click"
            }

            blink()
        }
    }

    private fun blink() {

        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 50 //You can manage the blinking time with this parameter

        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = 0

        findViewById<TextView>(R.id.buttonStatus)?.startAnimation(anim)
    }

    private fun startPreView() {
        if (surface == null) {
            Handler(Looper.getMainLooper()).postDelayed({
                startPreView()
            },500)
        } else {
            RKGlassCamera.getInstance().startPreview(surface, 1920, 1080)
        }
    }

    private fun stopPreview() {
        try {
            RKGlassCamera.getInstance().stopPreview()
        } catch (e: Exception) {
        }
        //surface = null
    }

    override fun onBackKeyEvent(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onTouchKeyEvent(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onTouchSlideBack() {
        TODO("Not yet implemented")
    }

    override fun onTouchSlideForward() {
        TODO("Not yet implemented")
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            666 -> {//for this is a test so every permission is required by default.
                var all = true
                grantResults.forEach {
                    all = (it == PackageManager.PERMISSION_GRANTED) && all
                }
                if (all) {
                    startCamera()
                } else {//goto the application settings to request all permissions.
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = Uri.fromParts("package", packageName, null)
                    })
                    this.finish()
                }
            }
        }
    }

}