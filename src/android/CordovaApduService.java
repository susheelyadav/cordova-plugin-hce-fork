package com.megster.cordova.hce;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CordovaApduService extends HostApduService {

    private static final String TAG = "CordovaHCE";

    // Status words
    private static final byte[] SW_OK = {(byte)0x90, 0x00};
    private static final byte[] SW_FILE_NOT_FOUND = {(byte)0x6A, (byte)0x82};
    private static final byte[] SW_INS_NOT_SUPPORTED = {(byte)0x6D, 0x00};

    // FIDs
    private static final byte[] CC_FILE_ID = {(byte)0xE1, (byte)0x03};
    private static final byte[] NDEF_FILE_ID = {(byte)0xE1, (byte)0x04};

    // NDEF AID (Type-4 NDEF Application)
    private static final byte[] NDEF_AID = {
        (byte)0xD2, (byte)0x76, (byte)0x00, (byte)0x00, (byte)0x85, (byte)0x01, (byte)0x01
    };

    // NDEF text to present to the reader — change this string to whatever text you want
    private static final String TEXT_TO_SEND = "Hello from card";

    // Built files to respond to READ BINARY
    private final byte[] ccFile;
    private final byte[] ndefFile;

    public CordovaApduService() {
        // Build NDEF text record (one Well-known Text record)
        byte[] textBytes = TEXT_TO_SEND.getBytes(StandardCharsets.UTF_8);
        byte[] lang = "en".getBytes(StandardCharsets.US_ASCII);
        byte status = (byte)(lang.length & 0x3F); // UTF-8, language length
        byte[] payload = new byte[1 + lang.length + textBytes.length];
        payload[0] = status;
        System.arraycopy(lang, 0, payload, 1, lang.length);
        System.arraycopy(textBytes, 0, payload, 1 + lang.length, textBytes.length);

        // NDEF record header: MB=1 ME=1 SR=1 TNF=0x01 (well-known)
        byte header = (byte)0xD1;
        byte typeLength = 0x01;
        int payloadLen = payload.length;
        byte[] ndefRecord = new byte[4 + payloadLen];
        ndefRecord[0] = header;
        ndefRecord[1] = typeLength;
        ndefRecord[2] = (byte)payloadLen; // SR set
        ndefRecord[3] = 0x54; // 'T'
        System.arraycopy(payload, 0, ndefRecord, 4, payloadLen);

        // NLEN (2 bytes) + NDEF record
        int ndefLen = ndefRecord.length;
        ndefFile = new byte[2 + ndefLen];
        ndefFile[0] = (byte)((ndefLen >> 8) & 0xFF);
        ndefFile[1] = (byte)(ndefLen & 0xFF);
        System.arraycopy(ndefRecord, 0, ndefFile, 2, ndefLen);

        // Simple CC file per NFC Forum Type-4 minimal mapping:
        // CCLEN(2) = 0x000F, Mapping version = 0x20, MLe/MLc = 0x0000, one NDEF File Control TLV
        int maxNdef = Math.min(ndefFile.length, 0xFFFF);
        ccFile = new byte[] {
            0x00, 0x0F, // CCLEN = 15
            0x20,       // mapping version 2.0
            0x00, 0x00, // MLe (not critical)
            0x00, 0x00, // MLc (not critical)
            0x04, 0x06, // NDEF File Control TLV (tag 0x04) length 6
            NDEF_FILE_ID[0], NDEF_FILE_ID[1], // file id E1 04
            (byte)((maxNdef >> 8) & 0xFF), (byte)(maxNdef & 0xFF), // max NDEF size (2 bytes)
            0x00, 0x00 // read/write access (0x00 = allowed)
        };
    }

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        try {
            if (apdu == null || apdu.length < 4) {
                Log.w(TAG, "Received invalid/short APDU: " + (apdu == null ? "null" : Arrays.toString(apdu)));
                return SW_INS_NOT_SUPPORTED;
            }

            Log.d(TAG, "APDU Received: " + Arrays.toString(apdu));

            int cla = apdu[0] & 0xFF;
            int ins = apdu[1] & 0xFF;
            int p1 = apdu[2] & 0xFF;
            int p2 = apdu[3] & 0xFF;

            // SELECT by name -> 00 A4 04 00 <Lc> <AID>
            if (cla == 0x00 && ins == 0xA4 && p1 == 0x04) {
                if (apdu.length >= 5) {
                    int lc = apdu[4] & 0xFF;
                    if (apdu.length >= 5 + lc) {
                        byte[] aid = Arrays.copyOfRange(apdu, 5, 5 + lc);
                        Log.d(TAG, "SELECT AID: " + Arrays.toString(aid));
                        if (Arrays.equals(aid, NDEF_AID)) {
                            // Reader selected the NDEF application
                            notifyJsIfPresent(apdu);
                            return SW_OK;
                        } else {
                            notifyJsIfPresent(apdu);
                            return SW_FILE_NOT_FOUND;
                        }
                    }
                }
                notifyJsIfPresent(apdu);
                return SW_INS_NOT_SUPPORTED;
            }

            // SELECT by FID -> 00 A4 02 0C 02 <FID>
            if (cla == 0x00 && ins == 0xA4 && p1 == 0x02) {
                if (apdu.length >= 7) {
                    byte fid0 = apdu[5];
                    byte fid1 = apdu[6];
                    Log.d(TAG, String.format("SELECT FID: %02X %02X", fid0, fid1));
                    if (fid0 == CC_FILE_ID[0] && fid1 == CC_FILE_ID[1]) {
                        notifyJsIfPresent(apdu);
                        return SW_OK;
                    } else if (fid0 == NDEF_FILE_ID[0] && fid1 == NDEF_FILE_ID[1]) {
                        notifyJsIfPresent(apdu);
                        return SW_OK;
                    } else {
                        notifyJsIfPresent(apdu);
                        return SW_FILE_NOT_FOUND;
                    }
                }
                notifyJsIfPresent(apdu);
                return SW_INS_NOT_SUPPORTED;
            }

            // READ BINARY -> 00 B0 P1 P2 [Le]
            if (cla == 0x00 && ins == 0xB0) {
                int offset = (p1 << 8) | p2;
                int le = 0;
                if (apdu.length == 5) le = apdu[4] & 0xFF;
                Log.d(TAG, "READ BINARY offset=" + offset + " le=" + le);

                // If offset inside CC file, serve CC slice
                if (offset < ccFile.length) {
                    int available = ccFile.length - offset;
                    int toSend = (le == 0) ? available : Math.min(le, available);
                    if (toSend <= 0) {
                        notifyJsIfPresent(apdu);
                        return SW_FILE_NOT_FOUND;
                    }
                    byte[] resp = new byte[toSend + SW_OK.length];
                    System.arraycopy(ccFile, offset, resp, 0, toSend);
                    resp[toSend] = SW_OK[0];
                    resp[toSend + 1] = SW_OK[1];
                    notifyJsIfPresent(apdu);
                    return resp;
                } else {
                    // else treat as read from NDEF file (offset measured after CC)
                    int ndefOffset = offset - ccFile.length;
                    if (ndefOffset < 0) {
                        notifyJsIfPresent(apdu);
                        return SW_FILE_NOT_FOUND;
                    }
                    int available = ndefFile.length - ndefOffset;
                    int toSend = (le == 0) ? available : Math.min(le, available);
                    if (toSend <= 0) {
                        notifyJsIfPresent(apdu);
                        return SW_FILE_NOT_FOUND;
                    }
                    byte[] resp = new byte[toSend + SW_OK.length];
                    System.arraycopy(ndefFile, ndefOffset, resp, 0, toSend);
                    resp[toSend] = SW_OK[0];
                    resp[toSend + 1] = SW_OK[1];
                    notifyJsIfPresent(apdu);
                    return resp;
                }
            }

            // Unsupported instruction
            notifyJsIfPresent(apdu);
            return SW_INS_NOT_SUPPORTED;

        } catch (Exception e) {
            Log.e(TAG, "Error in processCommandApdu", e);
            return SW_INS_NOT_SUPPORTED;
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCE service deactivated: " + reason);
    }

    /**
     * Notify the JS plugin if a command callback is registered.
     * This is only a notification; JS will not be required to reply.
     */
    private void notifyJsIfPresent(byte[] apdu) {
        try {
            if (HCEPlugin != null && HCEPlugin.commandCallback != null) {
                // Defensive copy
                byte[] copy = Arrays.copyOf(apdu, apdu.length);
                HCEPlugin.commandCallback.onCommandReceived(copy);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to notify JS callback", e);
        }
    }
}
