package com.megster.cordova.hce;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class HCEPlugin extends CordovaPlugin {

    // Persistent callback for JS notifications (keepCallback = true)
    public static CallbackContext persistentCallback = null;

    // Thread-safe holder for dynamic NDEF text
    private static final AtomicReference<String> ndefTextRef = new AtomicReference<>("Hello from card");

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("registerCommandCallback".equals(action)) {
            persistentCallback = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.OK);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }

        if ("unregisterCommandCallback".equals(action)) {
            persistentCallback = null;
            callbackContext.success();
            return true;
        }

        if ("setNdefText".equals(action)) {
            String text = args.getString(0);
            setNdefText(text);
            callbackContext.success();
            return true;
        }

        // other actions (if any) fall back
        return super.execute(action, args, callbackContext);
    }

    public static void setNdefText(String text) {
        if (text == null) text = "";
        ndefTextRef.set(text);
    }

    public static byte[] getNdefFile() {
        String text = ndefTextRef.get();
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] lang = "en".getBytes(StandardCharsets.US_ASCII);
        byte status = (byte)(lang.length & 0x3F);
        byte[] payload = new byte[1 + lang.length + textBytes.length];
        payload[0] = status;
        System.arraycopy(lang, 0, payload, 1, lang.length);
        System.arraycopy(textBytes, 0, payload, 1 + lang.length, textBytes.length);

        byte header = (byte)0xD1;
        byte typeLength = 0x01;
        int payloadLen = payload.length;
        byte[] ndefRecord = new byte[4 + payloadLen];
        ndefRecord[0] = header;
        ndefRecord[1] = typeLength;
        ndefRecord[2] = (byte) payloadLen;
        ndefRecord[3] = 0x54; // 'T'
        System.arraycopy(payload, 0, ndefRecord, 4, payloadLen);

        int ndefLen = ndefRecord.length;
        byte[] ndefFile = new byte[2 + ndefLen];
        ndefFile[0] = (byte)((ndefLen >> 8) & 0xFF);
        ndefFile[1] = (byte)(ndefLen & 0xFF);
        System.arraycopy(ndefRecord, 0, ndefFile, 2, ndefLen);
        return ndefFile;
    }

    public static byte[] getCcFile() {
        byte[] ndefFile = getNdefFile();
        int maxNdef = Math.min(ndefFile.length, 0xFFFF);
        return new byte[] {
            0x00, 0x0F, // CCLEN = 15
            0x20,       // mapping version 2.0
            0x00, 0x00, // MLe
            0x00, 0x00, // MLc
            0x04, 0x06, // NDEF File Control TLV length 6
            (byte)0xE1, (byte)0x04, // file id E1 04
            (byte)((maxNdef >> 8) & 0xFF), (byte)(maxNdef & 0xFF),
            0x00, 0x00 // read/write access
        };
    }

    // Helper: send APDU notification (hex string) to JS persistent callback
    public static void notifyPersistentCallback(byte[] apdu) {
        try {
            if (persistentCallback != null) {
                String hex = bytesToHex(apdu);
                PluginResult pr = new PluginResult(PluginResult.Status.OK, hex);
                pr.setKeepCallback(true);
                persistentCallback.sendPluginResult(pr);
            }
        } catch (Exception e) {
            // best-effort: ignore
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
