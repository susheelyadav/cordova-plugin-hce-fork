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

                // Get current CC and NDEF files (dynamic)
                byte[] currentCc = HCEPlugin.getCcFile();
                byte[] currentNdef = HCEPlugin.getNdefFile();

                // If offset inside CC file, serve CC slice
                if (offset < currentCc.length) {
                    int available = currentCc.length - offset;
                    int toSend = (le == 0) ? available : Math.min(le, available);
                    if (toSend <= 0) {
                        notifyJsIfPresent(apdu);
                        return SW_FILE_NOT_FOUND;
                    }
                    byte[] resp = new byte[toSend + SW_OK.length];
                    System.arraycopy(currentCc, offset, resp, 0, toSend);
                    resp[toSend] = SW_OK[0];
                    resp[toSend + 1] = SW_OK[1];
                    notifyJsIfPresent(apdu);
                    return resp;
                } else {
                    // else treat as read from NDEF file (offset measured after CC)
                    int ndefOffset = offset - currentCc.length;
                    if (ndefOffset < 0) {
                        notifyJsIfPresent(apdu);
                        return SW_FILE_NOT_FOUND;
                    }
                    int available = currentNdef.length - ndefOffset;
                    int toSend = (le == 0) ? available : Math.min(le, available);
                    if (toSend <= 0) {
                        notifyJsIfPresent(apdu);
                        return SW_FILE_NOT_FOUND;
                    }
                    byte[] resp = new byte[toSend + SW_OK.length];
                    System.arraycopy(currentNdef, ndefOffset, resp, 0, toSend);
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
                byte[] copy = Arrays.copyOf(apdu, apdu.length);
                HCEPlugin.commandCallback.onCommandReceived(copy);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to notify JS callback", e);
        }
    }
}
