// Cordova HCE Plugin
// Updated for OutSystems NFC HCE Integration
 
package com.megster.cordova.hce;
 
import android.util.Log;
 
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
 
import java.util.Arrays;
 
public class HCEPlugin extends CordovaPlugin {
 
    private static final String REGISTER_COMMAND_CALLBACK = "registerCommandCallback";
    private static final String SEND_RESPONSE = "sendResponse";
    private static final String REGISTER_DEACTIVATED_CALLBACK = "registerDeactivatedCallback";
    private static final String TAG = "HCEPlugin";
 
    private CallbackContext onCommandCallback;
    private CallbackContext onDeactivatedCallback;
 
    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
 
        Log.d(TAG, "Action: " + action);
 
        switch (action) {
 
            case REGISTER_COMMAND_CALLBACK:
                CordovaApduService.setHCEPlugin(this);
 
                onCommandCallback = callbackContext;
                PluginResult cmdResult = new PluginResult(PluginResult.Status.NO_RESULT);
                cmdResult.setKeepCallback(true);
                callbackContext.sendPluginResult(cmdResult);
                return true;
 
            case SEND_RESPONSE:
                byte[] responseBytes = args.getArrayBuffer(0);
 
                if (CordovaApduService.sendResponse(responseBytes)) {
                    callbackContext.success();
                } else {
                    callbackContext.error("CordovaApduService not connected.");
                }
                return true;
 
            case REGISTER_DEACTIVATED_CALLBACK:
                onDeactivatedCallback = callbackContext;
                PluginResult deactResult = new PluginResult(PluginResult.Status.NO_RESULT);
                deactResult.setKeepCallback(true);
                callbackContext.sendPluginResult(deactResult);
                return true;
 
            default:
                return false;
        }
    }
 
    // Called from CordovaApduService when terminal disconnects
    public void deactivated(int reason) {
        Log.d(TAG, "Deactivated: " + reason);
 
        if (onDeactivatedCallback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, reason);
            result.setKeepCallback(true);
            onDeactivatedCallback.sendPluginResult(result);
        }
    }
 
    // Called from CordovaApduService when APDU command received
    public void sendCommand(byte[] command) {
        Log.d(TAG, "APDU Command: " + Arrays.toString(command));
 
        if (onCommandCallback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, command);
            result.setKeepCallback(true);
            onCommandCallback.sendPluginResult(result);
        }
    }
}
