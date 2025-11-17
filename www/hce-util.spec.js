// hce-util.spec.js

// NOTE: This file is for Node/Mocha testing only.

// Do NOT include this file inside OutSystems mobile build.
 
var assert = require('assert');

var util = require('./hce-util.js');
 
describe('HCE Util', function () {
 
    it('should convert hex strings to byte arrays', function () {

        var test = util.hexStringToByteArray('656667');

        var expected = new Uint8Array([0x65, 0x66, 0x67]);

        assert.strictEqual(expected.toString(), test.toString());

    });
 
    it('should convert byte arrays to hex strings', function () {

        var data = new Uint8Array([0x65, 0x66, 0x67]);

        assert.strictEqual('656667', util.byteArrayToHexString(data));

    });
 
    it('should convert strings to byte arrays', function () {

        var test = util.stringToBytes('hello');

        var expected = new Uint8Array([104, 101, 108, 108, 111]);

        assert.strictEqual(expected.toString(), test.toString());

    });
 
    it('should combine buffers', function () {

        var a = new Uint8Array([63, 64]);

        var b = new Uint8Array([65, 66]);
 
        var combinedExpected = new Uint8Array([63, 64, 65, 66]).toString();
 
        var test = util.concatenateBuffers(a, b);

        assert.strictEqual(combinedExpected, new Uint8Array(test).toString());
 
        var test2 = util.concatenateBuffers(a.buffer, b.buffer);

        assert.strictEqual(combinedExpected, new Uint8Array(test2).toString());
 
        var test3 = util.concatenateBuffers(a.buffer, b);

        assert.strictEqual(combinedExpected, new Uint8Array(test3).toString());

    });
 
});

 
