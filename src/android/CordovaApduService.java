package com.megster.cordova.hce;
 
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
 
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
 
public class CordovaApduService extends HostApduService {
 
    private static final String TAG = "CordovaApduService";
 
    // Reference to the plugin so we can send events back to JS
    private static HCEPlugin hcePlugin;
    private static CordovaApduService instance;
 
    static void setHCEPlugin(HCEPlugin plugin) {
        hcePlugin = plugin;
    }
 
    static boolean sendResponse(byte[] data) {
        if (instance != null) {
            instance.sendResponseApdu(data);
            return true;
        }
        return false;
    }
 
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
 
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.d(TAG, "APDU received: " + ByteArrayToHexString(commandApdu));
 
        // If JavaScript has registered a callback → send command to plugin
        if (hcePlugin != null) {
            hcePlugin.sendCommand(commandApdu);
        }
 
        // DEFAULT NDEF RESPONSE
        String message = "Hello from OutSystems";
 
        try {
            NdefRecord record = createTextRecord(message);
            NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
            return wrapInSuccess(msg.toByteArray());
        } catch (Exception e) {
            Log.e(TAG, "Error creating NDEF", e);
            return new byte[]{};
        }
    }
 
    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Deactivated with reason: " + reason);
        if (hcePlugin != null) {
            hcePlugin.deactivated(reason);
        }
    }
 
    // Create a standard NDE
