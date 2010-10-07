package com.vodafone360.people.utils;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * The SimCard class provides utility methods to access SIM card data.
 */
public class SimCard {

    /**
     * Digits beyond this count will be replaced by 0.
     */
    private static final int ANONYMISED_DIGITS = 4;
    
    /**
     * The Network.
     */
    public enum Network {

        CH("ch", 41), // Switzerland
        DE("de", 49), // Germany
        FR("fr", 33), // France
        GB("gb", 44), // Great Britain
        IE("ie", 353), // Ireland
        IT("it", 39), // Italy
        NL("nl", 31), // Netherlands
        SE("se", 46), // Sweden
        TR("tr", 90), // Turkey
        TW("tw", 886), // Taiwan
        US("us", 1), // United States
        ES("es", 34), // Spain
        // ZA("za", 260), //Zambia/VodaCom-SA??
        UNKNOWN("unknown", 0);

        private final String mIso;

        private final int mPrefix;

        private Network(String iso, int prefix) {
            mIso = iso;
            mPrefix = prefix;
        }

        private Network() {
            mIso = null;
            mPrefix = -1;
        }

        public String iso() {
            return mIso;
        }

        protected int prefix() {
            return mPrefix;
        }

        /***
         * Returns the SimNetwork of a given ISO if known
         *
         * @param iso Country ISO value.
         * @return SimNetwork or SimNetwork.UNKNOWN if not found.
         */
        public static Network getNetwork(String iso) {
            for (final Network network : Network.values()) {
                if (network.iso().equals(iso)) {
                    return network;
                }
            }
            return UNKNOWN;
        }
    }
    
    /**
     * SIM card absent state.
     * @see #getSubscriberId(Context)
     */
    public final static String SIM_CARD_ABSENT = "SimAbsent";
    
    /**
     * SIM card not readable state.
     * @see #getSubscriberId(Context)
     */
    public final static String SIM_CARD_NOT_READABLE = "SimNotReadable";
    
    /**
     * Gets the SIM card state.
     * 
     * @see TelephonyManager#getSimState()
     * 
     * @param context the application context
     * @return the SIM card state
     */
    public static int getState(Context context) {
       
        try {
            
            final TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            
            return telephonyManager.getSimState();  
            
        } catch (final Exception e) {
            LogUtils.logE("SimCard.getState() - Exception:", e);
        }
        
        return TelephonyManager.SIM_STATE_UNKNOWN;
    }
    
    /**
     * Tells whether or not the SIM card is present.
     * 
     * @param context the 
     * @return true if the SIM is ready, false otherwise (not ready or absent)
     */
    public static boolean isSimReady(Context context) {
        
        return getState(context) == TelephonyManager.SIM_STATE_READY;
    }
    
    /**
     * Gets the Subscriber Id.
     * 
     * @see SIM_CARD_NOT_READABLE
     * @see SIM_CARD_ABSENT
     * 
     * @param databaseHelper the DatabaseHelper
     * @return the Subscriber Id or SIM_CARD_ABSENT or SIM_CARD_NOT_READABLE
     */
    public static String getSubscriberId(Context context) {
        
        if (getState(context) == TelephonyManager.SIM_STATE_ABSENT) {
            
            return SIM_CARD_ABSENT;
            
        } else {
            
            try {
                
                final TelephonyManager mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                
                return mTelephonyManager.getSubscriberId(); 
    
            } catch (final Exception e) {
                
                LogUtils.logE("SimCard.getSubscriberId() - Exception: "+e);
            }
        }
        return SIM_CARD_NOT_READABLE;
    }
    
    /**
     * Logs a MSISDN validation failure event to Flurry.
     *
     * @param context Android context.
     * @param workflow Given work flow (either sign in or sign up).
     * @param status Current status string.
     * @param enteredMsisdn User provided MSISDN.
     */
    public static void logMsisdnValidationFail(Context context, String workflow, String status,
            String enteredMsisdn) {
        final TelephonyManager telephonyManager = (TelephonyManager)context
                .getSystemService(Context.TELEPHONY_SERVICE);
        final Map<String, String> map = new HashMap<String, String>();
        map.put("NetworkCountryIso", telephonyManager.getNetworkCountryIso());
        map.put("NetworkOperator", telephonyManager.getNetworkOperator());
        map.put("NetworkOperatorName", telephonyManager.getNetworkOperatorName());
        map.put("SimCountryIso", telephonyManager.getSimCountryIso());
        map.put("SimOperator", telephonyManager.getSimOperator());
        map.put("SimOperatorName", telephonyManager.getSimOperatorName());
        map.put("AnonymisedMsisdn", getAnonymisedMsisdn(telephonyManager.getLine1Number()));
        map.put("AnonymisedEntereddMsisdn", getAnonymisedMsisdn(enteredMsisdn));
        map.put("Workflow", workflow);
        map.put("Error", status);
        // FlurryAgent.onEvent("EnterMobileNumberActivity_FAIL", map);

        LogUtils.logV("SimUtils.logMsisdnValidationFail() enteredMsisdn[" + enteredMsisdn + "]");
    }

    /**
     * Retrieves the full international MSISDN number from the SIM or NULL if
     * unavailable or unsure. Note: This functionality is currently part of
     * SignupEnterMobileNumberActivity so anonymised data can be sent back to
     * Flurry, in order to verify this program logic on multiple SIMs.
     *
     * @param context Android Context
     * @return Full international MSISDN, or NULL if unsure of conversion.
     */
    public static String getFullMsisdn(Context context) {
        final TelephonyManager telephonyManager = (TelephonyManager)context
                .getSystemService(Context.TELEPHONY_SERVICE);
        final String rawMsisdn = telephonyManager.getLine1Number();
        final String verifyedMsisdn = getVerifyedMsisdn(rawMsisdn, telephonyManager
                .getNetworkCountryIso(), telephonyManager.getNetworkOperatorName(),
                telephonyManager.getNetworkOperator());
        final Map<String, String> map = new HashMap<String, String>();
        map.put("NetworkCountryIso", telephonyManager.getNetworkCountryIso());
        map.put("NetworkOperator", telephonyManager.getNetworkOperator());
        map.put("NetworkOperatorName", telephonyManager.getNetworkOperatorName());
        map.put("SimCountryIso", telephonyManager.getSimCountryIso());
        map.put("SimOperator", telephonyManager.getSimOperator());
        map.put("SimOperatorName", telephonyManager.getSimOperatorName());
        map.put("AnonymisedMsisdn", getAnonymisedMsisdn(rawMsisdn));
        map.put("AnonymisedVerifyedMsisdn", getAnonymisedMsisdn(verifyedMsisdn));
        // FlurryAgent.onEvent("EnterMobileNumberActivity", map);
        return verifyedMsisdn;
    }

    /***
     * Convert raw MSISDN to a verified MSISDN with country code.
     *
     * @param rawMsisdn Unverified MSISDN.
     * @param countryIso Country ISO value from the SIM.
     * @param networkOperatorName Network operator name value from the SIM.
     * @param networkOperator Network operator value from the SIM.
     * @return Verified MSISDN, or "" if NULL or unsure of country code.
     */
    public static String getVerifyedMsisdn(String rawMsisdn, String countryIso,
            String networkOperatorName, String networkOperator) {
        if (rawMsisdn == null || rawMsisdn.trim().equals("")) {
            // Reject any NULL or empty values.
            return "";

        } else if (rawMsisdn.substring(0, 1).equals("+")) {
            // Accept any values starting with "+".
            return rawMsisdn;
//            the MTS Russian SIM may have just "8" as a rawNsisdn string
        } else if (rawMsisdn.length() > 1 && (rawMsisdn.substring(0, 2).equals("00"))) {
            // Accept any values starting with "00", but at +.
            return "+" + rawMsisdn.substring(2);

        } else if (countryIso != null && !countryIso.trim().equals("")) {
            // Filter known values:
            try {
                final Network simNetwork = Network.getNetwork(countryIso);
                if (simNetwork != Network.UNKNOWN) {
                    return "+" + smartAdd(simNetwork.prefix() + "", dropZero(rawMsisdn));
                } else {
                    // Rejected
                    return "";
                }

            } catch (final NumberFormatException e) {
                // Rejected
                return "";
            }

        } else {
            // Rejected
            return "";
        }
    }

    /***
     * Concatenate the "start" value with the "end" value, except where the
     * "start" value is already in place.
     *
     * @param start First part of String.
     * @param end Last part of String.
     * @return "start" + "end".
     */
    private static String smartAdd(String start, String end) {
        if (end == null || end.trim().equals("")) {
            return "";
        } else if (end.startsWith(start)) {
            return end;
        } else {
            return start + end;
        }
    }

    /***
     * Remove the preceding zero, if present.
     *
     * @param rawMsisdn Number to alter.
     * @return Number without the zero.
     */
    private static String dropZero(String rawMsisdn) {
        if (rawMsisdn == null || rawMsisdn.trim().equals("")) {
            return "";
        } else if (rawMsisdn.startsWith("0")) {
            return rawMsisdn.substring(1);
        } else {
            return rawMsisdn;
        }
    }

    /***
     * Converts an identifiable MSISDN value to something that can be sent via a
     * third party. E.g. "+49123456789" becomes "+49100000000".
     *
     * @param input Private MSISDN.
     * @return Anonymous MSISDN.
     */
    public static String getAnonymisedMsisdn(String input) {
        if (input == null || input.trim().equals("")) {
            return "";
        } else {
            final int length = input.length();
            final StringBuffer result = new StringBuffer();
            for (int i = 0; i < length; i++) {
                if (i < ANONYMISED_DIGITS) {
                    result.append(input.charAt(i));
                } else {
                    result.append("0");
                }
            }
            return result.toString();
        }
    }
}
