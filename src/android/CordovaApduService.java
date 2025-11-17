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
    public void onCreate() {
        super.onCreate();
        cordovaApduService = this;
    }
 
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
 
        String msg = "Hello from OutSystems";
 
        try {
            NdefRecord textRecord = createTextRecord(msg);
            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{textRecord});
            return wrapMessageInSelectOk(ndefMessage.toByteArray());
        } catch (Exception e) {
            Log.e(TAG, "Error creating NDEF: " + e.getMessage());
            return new byte[]{};
        }
    }
 
    @Override
    public void onDeactivated(int reason) {
        if (hcePlugin != null) {
            hcePlugin.deactivated(reason);
        } else {
            Log.e(TAG, "No reference to HCE Plugin.");
        }
    }
 
    private NdefRecord createTextRecord(String text) throws UnsupportedEncodingException {
 
        byte[] langBytes = "en".getBytes("US-ASCII");
        byte[] textBytes = text.getBytes("UTF-8");
 
        int langLength = langBytes.length;
        int textLength = textBytes.length;
 
        byte[] payload = new byte[1 + langLength + textLength];
 
        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
 
        return new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,
                new byte[0],
                payload
        );
    }
 
    private byte[] wrapMessageInSelectOk(byte[] msg) {
        byte[] response = Arrays.copyOf(msg, msg.length + 2);
        response[msg.length]     = (byte) 0x90;  // Success SW1
        response[msg.length + 1] = (byte) 0x00;  // Success SW2
        return response;
    }
 
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {
            '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
        };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2]     = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
