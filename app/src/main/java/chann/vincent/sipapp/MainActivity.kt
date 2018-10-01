package chann.vincent.sipapp

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.sip.SipAudioCall
import android.net.sip.SipManager
import android.net.sip.SipProfile
import android.net.sip.SipRegistrationListener
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.text.ParseException

class MainActivity : AppCompatActivity() {

    val mSipManager: SipManager? by lazy(LazyThreadSafetyMode.NONE) {
        SipManager.newInstance(this)
    }

    private var mSipProfile: SipProfile? = null

    // dev1
    val username = "8MTYpcU3H8"
    val password = "rkYMgVI6TL"
    val domain = "bowo.3cx.samcloud.fr"

    // call to vincent
    val sipAddress = "1001"

    var registrationSuccess = false
    var builder: SipProfile.Builder? = null
    var call: SipAudioCall? = null

    val PERMISSION_CODE_SIP: Int = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setPermissionButton()
        val buttonPermission = findViewById<Button>(R.id.permission)
        buttonPermission.setOnClickListener {
            askPermission()
        }

        setButtonProfile()
        val buttonProfile = findViewById<Button>(R.id.create_profile)
        buttonProfile.setOnClickListener {
            createProfile()
        }

        setButtonCall()
        val buttonCall = findViewById<Button>(R.id.call)
        buttonCall.setOnClickListener {
            initiateCall()
        }
        val buttonCloseProfile = findViewById<Button>(R.id.close_profile)
        buttonCloseProfile.setOnClickListener {
            closeLocalProfile()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        updateStatus("permission granted")
    }

    override fun onDestroy() {
        super.onDestroy()
        closeLocalProfile()
    }

    /*
    Methods
     */

    fun closeLocalProfile() {
        try {
            if (mSipProfile != null) {
                mSipManager?.close(mSipProfile?.uriString)
                updateStatus("profile closed")
            }
        } catch (ee: Exception) {
            updateStatus("Failed to close local profile")
        }
    }

    fun updateStatus(status: String?) {
        runOnUiThread {
            val textView = findViewById<TextView>(R.id.text)
            textView.movementMethod = ScrollingMovementMethod()
            textView.text = textView.text.toString() + "\n" + status
            setButtonCall()
            setButtonProfile()
            setPermissionButton()
        }
    }

    fun updateStatus(call: SipAudioCall) {
        var useName: String? = call.peerProfile.displayName
        if (useName == null) {
            useName = call.peerProfile.userName
        }
        updateStatus(useName + "@" + call.peerProfile.sipDomain)
    }

    fun askPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1) { Manifest.permission.USE_SIP }, PERMISSION_CODE_SIP)
            return false
        }
        else {
            return true
        }
    }

    fun setPermissionButton() {
        val buttonPermission = findViewById<Button>(R.id.permission)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP) != PackageManager.PERMISSION_GRANTED) {
            buttonPermission.isEnabled = true
            buttonPermission.text = "grant permission"
            updateStatus("permission granted")
        }
        else {
            buttonPermission.isEnabled = false
            buttonPermission.text = "permission already granted"
            // updateStatus("permission not granted")
        }
    }

    fun setButtonProfile() {
        val buttonCloseProfile = findViewById<Button>(R.id.close_profile)
        val buttonProfile = findViewById<Button>(R.id.create_profile)
        buttonProfile.isEnabled = (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP) == PackageManager.PERMISSION_GRANTED)
        buttonCloseProfile.isEnabled = (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP) == PackageManager.PERMISSION_GRANTED)
        buttonProfile.requestLayout()
        buttonCloseProfile.requestLayout()
    }

    fun setButtonCall() {
        val buttonCall = findViewById<Button>(R.id.call)
        buttonCall.isEnabled = registrationSuccess
        buttonCall.requestLayout()
    }

    fun createProfile() {
        try {
            // build your profile
            builder = SipProfile.Builder(username, domain).setPassword(password)
            mSipProfile = builder?.build()

            // register for incoming calls
            val intent = Intent("android.SipDemo.INCOMING_CALL")
            val pendingIntent: PendingIntent = PendingIntent.getBroadcast(this, PERMISSION_CODE_SIP, intent, Intent.FILL_IN_DATA)
            mSipManager?.open(mSipProfile, pendingIntent, null)

            // listen for registration status
            mSipManager?.setRegistrationListener(mSipProfile?.uriString, object : SipRegistrationListener {
                override fun onRegistering(p0: String?) {
                    registrationSuccess = false
                    updateStatus("Registering with SIP Server...")
                }

                override fun onRegistrationDone(p0: String?, p1: Long) {
                    registrationSuccess = true
                    updateStatus("Ready")
                }

                override fun onRegistrationFailed(p0: String?, p1: Int, p2: String?) {
                    registrationSuccess = false
                    updateStatus("Registration failed")
                }
            })
        }
        catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    fun initiateCall() {
        updateStatus("initiateCall : " + sipAddress)
        try {
            val listener = object : SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
                override fun onCallEstablished(call: SipAudioCall) {
                    call.startAudio()
                    call.setSpeakerMode(true)
                    call.toggleMute()
                    updateStatus(call)
                }

                override fun onCallEnded(call: SipAudioCall) {
                    updateStatus("Ready.")
                }
            }

            call = mSipManager?.makeAudioCall(mSipProfile?.uriString, sipAddress, listener, 30)

        } catch (e: Exception) {
            Log.e("MainActivity", "Error when trying to close manager.", e)
            if (mSipProfile != null) {
                try {
                    mSipManager?.close(mSipProfile?.uriString)
                } catch (ee: Exception) {
                    Log.e("MainActivity", "Error when trying to close manager.", ee)
                    ee.printStackTrace()
                }

            }
            if (call != null) {
                call?.close()
            }
        }

    }
}
