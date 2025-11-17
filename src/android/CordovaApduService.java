package com.megster.cordova.hce;
 
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
 
import java.util.Arrays;
 
public class CordovaApduService extends HostApduService {
 
    private static final String TAG = "CordovaHCE";
    private static byte[] lastCommand;
 
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        try {
            Log.d(TAG, "APDU Received: " + Arrays.toString(commandApdu));
            lastCommand = commandApdu;
 
            if (HCEPlugin.commandCallback != null) {
                HCEPlugin.commandCallback.onCommandReceived(commandApdu);
            }
 
        } catch (Exception e) {
            Log.e(TAG, "Error processing APDU", e);
        }
 
        return null;  // Response will be sent via sendResponseApdu
    }
 
    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCE service deactivated: " + reason);
    }
}
