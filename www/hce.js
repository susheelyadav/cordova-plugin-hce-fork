var exec = require('cordova/exec');
var util = require('./hce-util'); // ensure this exists in www/

module.exports = {
    // Register to receive APDU commands (persistent). onCommand gets Uint8Array
    registerCommandCallback: function(onCommand, success, failure) {
        if (typeof onCommand !== 'function') throw new Error('onCommand must be a function');

        function nativeCallback(result) {
            try {
                if (typeof result === 'string') {
                    var u8 = util.hexStringToByteArray(result);
                    onCommand(u8);
                } else if (Array.isArray(result)) {
                    onCommand(new Uint8Array(result));
                } else if (result && result instanceof ArrayBuffer) {
                    onCommand(new Uint8Array(result));
                } else {
                    try {
                        var parsed = JSON.parse(result);
                        if (Array.isArray(parsed)) onCommand(new Uint8Array(parsed));
                        else onCommand(parsed);
                    } catch (e) {
                        onCommand(result);
                    }
                }
            } catch (e) {
                console.error('HCE: error processing native callback', e);
            }
        }

        exec(nativeCallback, failure || function(){}, 'HCE', 'registerCommandCallback', []);
        if (success) success();
        return {
            unregister: function(cb) {
                exec(cb || function(){}, function(){}, 'HCE', 'unregisterCommandCallback', []);
            }
        };
    },

    // Send APDU response back to terminal (Uint8Array or ArrayBuffer)
    sendResponse: function (responseApdu, success, failure) {
        if (responseApdu instanceof Uint8Array) responseApdu = responseApdu.buffer;
        exec(success || function(){}, failure || function(){}, 'HCE', 'sendResponse', [responseApdu]);
    },

    // Set dynamic NDEF text (call before tap)
    setNdefText: function(text, success, failure) {
        if (typeof text !== 'string') text = String(text || '');
        exec(success || function(){}, failure || function(){}, 'HCE', 'setNdefText', [text]);
    },

    // Register onDeactivated callback
    registerDeactivatedCallback: function (success, failure) {
        exec(success || function(){}, failure || function(){}, 'HCE', 'registerDeactivatedCallback', []);
    },

    // Helpers (for tests)
    _hexToBytes: util.hexStringToByteArray,
    _bytesToHex: util.byteArrayToHexString,
    _stringToBytes: util.stringToBytes
};
