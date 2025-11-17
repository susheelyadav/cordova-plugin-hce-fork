// Cordova HCE Plugin
// (c) 2015 Don Coleman
 
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
 
    // tight binding between the service and plugin
    private static HCEPlugin hcePlugin;
    private static CordovaApduService cordovaApduService;
 
    static void setHCEPlugin(HCEPlugin _hcePlugin) {
        hcePlugin = _hcePlugin;
    }
 
    static boolean sendResponse(byte[] data) {
        if (cordovaApduService != null) {
            cordovaApduService.sendResponseApdu(data);
            return true;
        } else {
            return false;
        }
    }
 
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
 
        // This message will appear on the receiving phone (no app required)
        String msg = "Hello from OutSystems";
 
        try {
            NdefRecord textRecord = createTextRecord(msg);
            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{textRecord});
            return wrapMessageInSelectOk(ndefMessage.toByteArray());
        } catch (Exception e) {
            Log.e(TAG, "Error building NDEF: " + e.getMessage());
            return new byte[]{};
        }
    }
 
    @Override
    public void onCreate() {
        super.onCreate();
        cordovaApduService = this;
    }
 
    @Override
    public void onDeactivated(int reason) {
 
        if (hcePlugin != null) {
            hcePlugin.deactivated(reason);
        } else {
            Log.e(TAG, "No reference to HCE Plugin.");
        }
    }
 
    // -------------------------------
    // NDEF TEXT RECORD CREATION
    // -------------------------------
    private NdefRecord createTextRecord(String text) throws Uns
