package com.vodafone360.people.service.transport.tcp;

public interface ITcpConnectionListener {

    int STATE_CONNECTED = 0;
    
    int STATE_DISCONNECTED = 1;
    
    int STATE_CONNECTING = 2;
    
    void onConnectionStateChanged(int state);
    
}
