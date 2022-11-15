package me.loterio.rokid

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oxsightglobal.R
import com.rokid.axr.phone.glassdevice.RKGlassDevice
import com.rokid.axr.phone.glassdevice.callback.OnGlassDeviceConnectListener
import com.rokid.axr.phone.glassdevice.hw.GlassConfig
import com.rokid.axr.phone.glassdevice.hw.RKKeyProcessor
import com.rokid.axr.phone.glassdevice.hw.listener.KeyEventType
import com.rokid.axr.phone.glassdevice.hw.listener.RKKeyListener
import com.rokid.logger.Logger


class DemoActivity: AppCompatActivity(), RKKeyListener {

    private var device: UsbDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo_activity)

        connect()

        attachListenerToButtonsEvents()
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

            }

            override fun onGlassDeviceDisconnected() {//call when glass is disconnected
                device = null
                findViewById<View>(R.id.holder)?.background = resources.getDrawable(R.color.red_400)
                findViewById<TextView>(R.id.connectionStatus)?.text = resources.getText(R.string.disconnected)
                findViewById<TextView>(R.id.buttonStatus)?.text = resources.getText(R.string.plug_the_glasses_to_the_device)
            }
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
}