// Cordova HCE Plugin
// (c) 2015 Don Coleman
 
module.exports = {
 
    // Register to receive APDU commands from the terminal.
    // Success callback receives Uint8Array commands
    registerCommandCallback: function (success, failure) {
        cordova.exec(success, failure, 'HCE', 'registerCommandCallback', []);
    },
 
    // Send APDU response back to terminal
    // responseApdu must be Uint8Array or ArrayBuffer
    sendResponse: function (responseApdu, success, failure) {
 
        // Fix for OutSystems & Cordova: ensure ArrayBuffer
        if (responseApdu instanceof Uint8Array) {
            responseApdu = responseApdu.buffer;
        }
 
        cordova.exec(success, failure, 'HCE', 'sendResponse', [responseApdu]);
    },
 
    // Receive "onDeactivated" callback
    registerDeactivatedCallback: function (success, failure) {
        cordova.exec(success, failure, 'HCE', 'registerDeactivatedCallback', []);
    }
};
