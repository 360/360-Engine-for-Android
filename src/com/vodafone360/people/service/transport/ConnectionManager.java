/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 * src/com/vodafone360/people/VODAFONE.LICENSE.txt or
 * http://github.com/360/360-Engine-for-Android
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at src/com/vodafone360/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2010 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.service.transport;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.vodafone360.people.R;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.login.LoginEngine.ILoginEventsListener;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.transport.http.authentication.AuthenticationManager;
import com.vodafone360.people.service.transport.tcp.ITcpConnectionListener;
import com.vodafone360.people.service.transport.tcp.TcpConnectionThread;

/**
 * ConnectionManager - responsible for controlling the current connection
 * Connects and disconnects based on network availability etc and start or stops
 * RPG polling. Currently handles HTTP connections - this will be extended to
 * support TCP sockets.
 */
public class ConnectionManager implements ILoginEventsListener, IQueueListener {

    private static ConnectionManager mInstance;

    private IConnection mConnection;

    private RemoteService mService;

    private DecoderThread mDecoder;

    /**
     * ApplicationContext
     */
    private Context mContext;

    /**
     * the class member variable to keep the connection state.
     * 
     * @see ITcpConnectionListener
     */
    private int mConnectionState;

    /**
     * the list of connection state change listeners, e.g. PresenceEngine.
     */
    private ArrayList<ITcpConnectionListener> mListeners = new ArrayList<ITcpConnectionListener>();

    public static synchronized ConnectionManager getInstance() {
        if (mInstance == null) {
            mInstance = new ConnectionManager();
        }

        return mInstance;
    }

    /**
     * Shows a Toast ruinning on a UI Thread. There can be a crash if the
     * connectionmanager is called outside of an UI Thread and then treis to
     * show a toast. Using this approach with the AsyncTask Toasts will allways
     * be shown inside an UI Thread.
     * 
     * @param messageResId
     */
    private void showToast(int messageResId) {
        new AsyncTask<Integer, Void, Boolean>() {
            protected Boolean doInBackground(Integer... messageResId) {
                try {
                    Toast toast = Toast.makeText(mContext, messageResId[0], Toast.LENGTH_LONG);
                    toast.show();
                } catch (Exception exc) {
                    return false;
                }
                return true;
            }

        }.execute(messageResId);
    }

    private ConnectionManager() {
        EngineManager.getInstance().getLoginEngine().addListener(this);

    }

    public void connect(RemoteService service) {
        HttpConnectionThread.logI("ConnectionManager.connect()", "CONNECT CALLED BY NETWORKAGENT");

        mService = service;
        mContext = mService.getApplicationContext();
        // start decoder
        if (mDecoder == null) {
            mDecoder = new DecoderThread();
        }
        if (!mDecoder.getIsRunning()) {
            mDecoder.startThread();
        }

        boolean isCurrentlyLoggedIn = EngineManager.getInstance().getLoginEngine().isLoggedIn();
        onLoginStateChanged(isCurrentlyLoggedIn);

        HttpConnectionThread.logI("ConnectionManager.connect()",
                (isCurrentlyLoggedIn) ? "We are logged in!" : "We are not logged in!");
    }

    /**
     * Returns an autodetected connection. Where available, TCP will be used.
     * HTTP is used as a fallback alternative.
     * 
     * @return Returns the correct implmentation of the connection. If TCP is
     *         available it will be used preferably. Otherwise, HTTP is used as
     *         a fallback.
     */
    private IConnection getAutodetectedConnection(RemoteService service) {
        return new TcpConnectionThread(mDecoder, service);
    }

    public void disconnect() {
        HttpConnectionThread.logI("ConnectionManager.disconnect()",
                "DISCONNECT CALLED BY NETWORKAGENT");

        if (null != mDecoder) {
            mDecoder.stopThread();
        }

        if (null != mConnection) {
            mConnection.stopThread();
            mConnection = null;
            unsubscribeFromQueueEvents();

            // TODO remove as soon as the network agent is out. this is a hack!
            onConnectionStateChanged(ITcpConnectionListener.STATE_DISCONNECTED);
        }
    }

    @Override
    public synchronized void onLoginStateChanged(boolean loggedIn) {
        HttpConnectionThread.logI("ConnectionManager.onLoginStateChanged()", "Is logged in: "
                + loggedIn);

        if (null != mConnection) {
            mConnection.stopThread();
            mConnection = null;

            unsubscribeFromQueueEvents();
        }

        if (loggedIn) {
            mConnection = getAutodetectedConnection(mService);
        } else {
            mConnection = new AuthenticationManager(mDecoder);
        }

        QueueManager.getInstance().addQueueListener(mConnection);
        mConnection.startThread();
    }

    @Override
    public synchronized void notifyOfItemInRequestQueue() {
        mConnection.notifyOfItemInRequestQueue();
    }

    public void notifyOfUiActivity() {
        if (null != mConnection) {
            mConnection.notifyOfUiActivity();
        }
    }

    private void unsubscribeFromQueueEvents() {
        QueueManager queue = QueueManager.getInstance();

        queue.removeQueueListener(mConnection);
        queue.clearRequestTimeouts();
    }

    /**
     * TODO: remove this singleton model and call
     */
    /*
     * public boolean isRPGEnabled(){ if (null != mConnection) { return
     * mConnection.getIsRpgConnectionActive(); } return false; }
     */

    /***
     * Note: only called from tests.
     */
    public void free() {
        EngineManager.getInstance().getLoginEngine().removeListener(this);

        disconnect();
        mConnection = null;
        mInstance = null;
    }

    /**
     * Enable test connection (for Unit testing purposes)
     * 
     * @param testConn handle to test connection
     */
    public void setTestConnection(IConnection testConn) {
        mConnection = testConn;
        QueueManager.getInstance().addQueueListener(mConnection);
    }

    /**
     * This method is called by protocol to signal the connection state change.
     * 
     * @param state int - the new connection state, @see ITcpConnectionListener.
     */
    public void onConnectionStateChanged(int state) {
        if (state == ITcpConnectionListener.STATE_DISCONNECTED) {
            // showToast(R.string.ContactProfile_no_connection);	// TODO show toast only if activity in foreground
        }

        mConnectionState = state;
        for (ITcpConnectionListener listener : mListeners) {
            listener.onConnectionStateChanged(state);
        }
    }

    /**
     * This method adds a listener for the connection state changes to the list,
     * if it is not there yet.
     * 
     * @param listener ITcpConnectionListener - listener.
     */
    public void addConnectionListener(ITcpConnectionListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * This method removes a listener from the connection state changes
     * listeners list.
     * 
     * @param listener ITcpConnectionListener - listener.
     */
    public void removeConnectionListener(ITcpConnectionListener listener) {
        mListeners.remove(listener);
    }

    /**
     * This method returns the current connection state of the application.
     * 
     * @see ITcpConnectionListener
     * @return int - the connection state @see ITcpConnectionListener.
     */
    public int getConnectionState() {
        return mConnectionState;
    }
}
