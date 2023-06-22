package com.qualcomm.qcrilmsgtunnel;

interface IQcrilMsgTunnel {

    /**
     * Sends a OEM request to the RIL and returns the response back to the
     * Caller. The returnValue is negative on failure. 0 or length of response on SUCCESS
     */
    int sendOemRilRequestRaw(in byte[] request, out byte[] response, in int sub);

}
