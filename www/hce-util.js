// Cordova HCE Plugin Utility Functions
// (c) 2015 Don Coleman – Cleaned & fixed for OutSystems
 
function hexStringToByteArray(hexString) {
    if (!hexString || hexString.length % 2 !== 0) {
        throw ("Hex string must have even number of characters");
    }
 
    var result = new Uint8Array(hexString.length / 2);
 
    for (var i = 0; i < hexString.length; i += 2) {
        result[i / 2] = parseInt(hexString.substring(i, i + 2), 16);
    }
 
    return result;
}
 
function byteArrayToHexString(bytes) {
    if (bytes instanceof ArrayBuffer) {
        bytes = new Uint8Array(bytes);
    }
 
    if (!(bytes instanceof Uint8Array)) {
        return "";
    }
 
    var hex = "";
    for (var i = 0; i < bytes.length; i++) {
        hex += toHex(bytes[i]);
    }
 
    return hex;
}
 
function toHex(value) {
    if (value < 0 || value > 255) {
        throw ("Value must be between 0–255. Got " + value);
    }
    return ("00" + value.toString(16)).slice(-2);
}
 
function stringToBytes(string) {
    var bytes = [];
 
    for (var i = 0; i < string.length; i++) {
        var c = string.charCodeAt(i);
 
        if (c < 128) {
            bytes.push(c);
 
        } else if (c < 2048) {
            bytes.push((c >> 6) | 192);
            bytes.push((c & 63) | 128);
 
        } else {
            bytes.push((c >> 12) | 224);
            bytes.push(((c >> 6) & 63) | 128);
            bytes.push((c & 63) | 128);
        }
    }
 
    return new Uint8Array(bytes);
}
 
function appendBuffer(buffer1, buffer2) {
    var tmp = new Uint8Array(buffer1.byteLength + buffer2.byteLength);
    tmp.set(new Uint8Array(buffer1), 0);
    tmp.set(new Uint8Array(buffer2), buffer1.byteLength);
    return tmp.buffer;
}
 
module.exports = {
    hexStringToByteArray: hexStringToByteArray,
    byteArrayToHexString: byteArrayToHexString,
    stringToBytes: stringToBytes,
    concatenateBuffers: appendBuffer
};
