package com.vodafone360.people.service.receivers;

import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.SimCard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * The SimStateReceiver is a BroadcastReceiver used to listen for SIM state changes.
 * 
 * @see INTENT_SIM_STATE_CHANGED
 */
public class SimStateReceiver extends BroadcastReceiver {

    /**
     * The Listener interface.
     */
    public interface Listener {
        
        /**
         * Callback method when SIM is ready.
         */
        void onSimReadyState();
    }
    
    /**
     * The Intent broadcasted by the Android platform when the SIM card state changes.
     */
    public final static String INTENT_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    
    /**
     * The registered Listener.
     */
    private final Listener mListener;
    
    /**
     * The SimStateReceiver constructor.
     * 
     * @param listener the registered listener
     */
    public SimStateReceiver(Listener listener) {
        
        LogUtils.logD("SimStateReceiver instance created.");
        mListener = listener;
    }
    
    /**
     * @see BroadcastReceiver#onReceive(Context, Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        
        final int simState = SimCard.getState(context);
        
        LogUtils.logD("SimStateReceiver.onReceive() - simState="+simState);
        
        if (simState == TelephonyManager.SIM_STATE_READY) {
            
            mListener.onSimReadyState();
        }
    }
}
