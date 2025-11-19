package com.megster.cordova.hce;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

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
                            HCEPlugin.notifyPersistentCallback(apdu);
                            return SW_OK;
                        } else {
                            HCEPlugin.notifyPersistentCallback(apdu);
                            return SW_FILE_NOT_FOUND;
                        }
