package com.megster.cordova.hce;
 
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
 
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.HostApduService;
import android.util.Log;
 
public class HCEPlugin extends CordovaPlugin {
 
    private static final String TAG = "CordovaHCE";
 
    public interface CommandCallback {
        void onCommandReceived(byte[] apdu);
    }
 
    public static CommandCallback commandCallback;
 
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
 
        if (action.equals("registerCommandCallback")) {
            registerCallback(callbackContext);
            return true;
 
        } else if (action.equals("sendResponse")) {
            byte[] response = toByteArray(args.getString(0));
            sendResponseApdu(response);
            callbackContext.success();
            return true;
        }
 
        return false;
    }
 
    private void registerCallback(final CallbackContext callbackContext) {
        commandCallback = new CommandCallback() {
            @Override
            public void onCommandReceived(byte[] apdu) {
                try {
                    JSONArray json = new JSONArray();
                    json.put(bytesToHex(apdu));
 
                    callbackContext.success(json);
 
                } catch (Exception e) {
                    Log.e(TAG, "Callback error", e);
                }
            }
        };
    }
 
    public static void sendResponseApdu(byte[] response) {
        try {
            if (CordovaApduService.class != null && response != null) {
                HostApduService service = CordovaApduService.class.newInstance();
                service.sendResponseApdu(response);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send APDU response", e);
        }
    }
 
    private static byte[] toByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
 
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
 
        return data;
    }
 
    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
 
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
 
        return new String(hexChars);
    }
}
