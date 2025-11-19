package com.megster.cordova.hce;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal HCEPlugin with dynamic NDEF text setter and getters used by CordovaApduService.
 * If your existing HCEPlugin already has other actions, merge the setNdefText block into execute().
 */
public class HCEPlugin extends CordovaPlugin {

    // JS callback interface for notifications (kept for compatibility)
    public static CommandCallback commandCallback = null;

    // AtomicReference to store dynamic NDEF text (thread-safe)
    private static final AtomicReference<String> ndefTextRef = new AtomicReference<>("Hello from card");

    /**
     * Simple interface used by CordovaApduService to notify JS.
     * You may already have a commandCallback in your plugin; adapt if needed.
     */
    public interface CommandCallback {
        void onCommandReceived(byte[] apdu);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("setNdefText".equals(action)) {
            String text = args.getString(0);
            setNdefText(text);
            callbackContext.success();
            return true;
        }

        // If you have other actions, handle them here or call super
        return super.execute(action, args, callbackContext);
    }

    /**
     * Called from JS to update the NDEF text that will be served to readers.
     * Must be set before the tag is polled.
     */
    public static void setNdefText(String text) {
        if (text == null) text = "";
        ndefTextRef.set(text);
    }

    /**
     * Build and return the current NDEF file bytes (NLEN + NDEF message)
     * based on the latest value set via setNdefText().
     */
    public static byte[] getNdefFile() {
        String text = ndefTextRef.get();
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] lang = "en".getBytes(StandardCharsets.US_ASCII);
        byte status = (byte)(lang.length & 0x3F);
        byte[] payload = new byte[1 + lang.length + textBytes.length];
        payload[0] = status;
        System.arraycopy(lang, 0, payload, 1, lang.length);
        System.arraycopy(textBytes, 0, payload, 1 + lang.length, textBytes.length);

        // NDEF record header: MB=1 ME=1 SR=1 TNF=0x01
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

    /**
     * Build and return a minimal CC file for the current NDEF length.
     */
    public static byte[] getCcFile() {
        byte[] ndefFile = getNdefFile();
        int maxNdef = Math.min(ndefFile.length, 0xFFFF);
        return new byte[] {
            0x00, 0x0F, // CCLEN = 15
            0x20,       // mapping version 2.0
            0x00, 0x00, // MLe
            0x00, 0x00, // MLc
            0x04, 0x06, // NDEF File Control TLV (tag 0x04) length 6
            (byte)0xE1, (byte)0x04, // file id E1 04
            (byte)((maxNdef >> 8) & 0xFF), (byte)(maxNdef & 0xFF),
            0x00, 0x00 // read/write access
        };
    }
}
