package chann.vincent.sipapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.sip.SipAudioCall
import android.net.sip.SipProfile

class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val wtActivity = context as MainActivity

        var incomingCall: SipAudioCall? = null
        try {
            incomingCall = wtActivity.mSipManager?.takeAudioCall(intent, listener)
            incomingCall?.apply {
                answerCall(30)
                startAudio()
                setSpeakerMode(true)
                if (isMuted) {
                    toggleMute()
                }
                wtActivity.call = this
                wtActivity.updateStatus("incoming call")
            }
        } catch (e: Exception) {
            incomingCall?.close()
        }
    }

    private val listener = object : SipAudioCall.Listener() {

        override fun onRinging(call: SipAudioCall, caller: SipProfile) {
            try {
                call.answerCall(30)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
