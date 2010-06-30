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

package com.vodafone360.people.service.transport.tcp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;

import android.content.Context;
import android.os.PowerManager;

import com.vodafone360.people.Settings;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.transport.IWakeupListener;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.AuthUtils;
import com.vodafone360.people.service.utils.hessian.HessianEncoder;
import com.vodafone360.people.service.utils.hessian.HessianUtils;
import com.vodafone360.people.service.io.rpg.RpgHeader;
import com.vodafone360.people.service.io.rpg.RpgMessageTypes;

/**
 * Sends heartbeats to the RPG in order to keep the connection alive.
 * 
 * @author Rudy Norff (rudy.norff@vodafone.com)
 */
public class HeartbeatSenderThread implements Runnable, IWakeupListener {
    /**
     * The HeartbeatSenderThread that was created by the TcpConnectionThread. It
     * will be compared to this thread. If they differ this thread must be a
     * lock thread that got stuck when the user changed APNs or switched from
     * WiFi to another data connection type.
     */
    protected static HeartbeatSenderThread mCurrentThread;

    /**
     * The milliseconds in a second.
     */
    private static final long MILLIS_PER_SEC = 1000;

    /**
     * The service under which context the hb sender runs in. Used for setting
     * alarms that wake up the CPU for sending out heartbeats.
     */
    private RemoteService mService;

    /**
     * The managing thread that needs to be called back if an IOException occurs
     * sending a heartbeat.
     */
    private TcpConnectionThread mConnThread;

    /**
     * The thread continuously sending the heartbeats.
     */
    protected Thread mThread;

    /**
     * The output stream to write the heartbeat to.
     */
    protected OutputStream mOs;

    /**
     * This is the interval at which the heartbeat gets sent. The problem with
     * this is that this timeout interval is only proven in the Vodafone
     * network. In other networks we will do an autodetect for the correct
     * interval settings.
     */
    protected long mHeartbeatInterval;

    /**
     * Indicates that the connection is running if true.
     */
    private boolean mIsConnectionRunning;

    /**
     * Keeps a partial wake lock on the CPU that will prevent it from sleeping
     * and allow the Sender to send off heartbeats.
     */
    private PowerManager.WakeLock mWakeLock;

    /**
     * The socket to write to.
     */
    private Socket mSocket;

    /**
     * Constructs a heartbeat-sender and passes the connection thread to call
     * back to in case of errors.
     * 
     * @param connThread The connection thread to call back to in case of
     *            networking issues.
     * @param service The remote service that we register with once we have set
     *            the heartbeat alarm.
     */
    public HeartbeatSenderThread(TcpConnectionThread connThread, RemoteService service,
            Socket socket) {
        mConnThread = connThread;
        mHeartbeatInterval = Settings.TCP_VF_HEARTBEAT_INTERVAL;

        mService = service;
        mSocket = socket;

        if (null != mService) {
            mService.registerCpuWakeupListener(this);
            PowerManager pm = (PowerManager)mService.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "HeartbeatSenderThread.HeartbeatSenderThread()");
        }
    }

    /**
     * Sets the state of the connection to run and spawns a thread to run it in.
     */
    public synchronized void startConnection() {
        HttpConnectionThread.logI("RpgTcpHeartbeatSender.startConnection()", "STARTING HB Sender!");

        mIsConnectionRunning = true;

        mThread = null; // if we are restarting let's clear the last traces
        mThread = new Thread(this, "RpgTcpHeartbeatSender");
        mThread.start();
    }

    /**
     * Stops the heartbeat-senders connection and closes the output-stream to
     * the socket-connection.
     */
    public synchronized void stopConnection() {
        mIsConnectionRunning = false;
        synchronized (this) {
            notify();
        }

        mService.setAlarm(false, 0);

        if (null != mSocket) {
            try {
                mSocket.shutdownOutput();
            } catch (IOException ioe) {
                HttpConnectionThread.logE("RpgTcpHeartbeatSender.stopConnection()",
                        "Could not shutdown OutputStream", ioe);
            } finally {
                mSocket = null;
            }
        }

        if (null != mOs) {
            try {
                mOs.close();
            } catch (IOException ioe) {
                HttpConnectionThread.logE("RpgTcpHeartbeatSender.stopConnection()",
                        "Could not close OutputStream", ioe);
            } finally {
                mOs = null;
            }
        }

        if (null != mThread) {
            try {
                mThread.join(60); // give it 60 millis max
            } catch (InterruptedException ie) {
            }
        }
    }

    /**
     * Sets the output-stream so that the heartbeat-sender can send its
     * heartbeats to the RPG.
     * 
     * @param outputStream The open output-stream that the heartbeats shall be
     *            sent to. Avoid passing null as this will result in an
     *            error-callback to RpgTcpConnectionThread which will completely
     *            reestablish the socket connection.
     */
    public void setOutputStream(OutputStream outputStream) {
        HttpConnectionThread.logI("RpgTcpHeartbeatSender.setOutputStream()", "Setting new OS.");
        mOs = outputStream;
    }

    /**
     * The run-method overriding Thread.run(). The method continuously sends out
     * heartbeats by calling sendHeartbeat() and then waits for a
     * Thread.interrupt(), a notify or for the wait timer to time out after a
     * timer-value defined in Settings.TCP_VF_HEARTBEAT_INTERVAL.
     */
    public void run() {
        // sets the wakeup alarm at XX minutes. This is important as the CPU
        // gets woken
        // by this call if it should be in standby mode.

        while (mIsConnectionRunning) {
            try {
                if (!this.equals(mCurrentThread)) {
                    // The current thread created by the TcpConnectionThread is
                    // not equal to this thread.
                    // This thread must be old (locked due to the user changing
                    // his network settings.
                    HttpConnectionThread.logE("HeartbeatSenderThread.run()", "This thread is a "
                            + "locked thread caused by the connection settings being switched.",
                            null);
                    stopConnection(); // exit this thread
                    break;
                }

                mWakeLock.acquire();
                sendHeartbeat();

                mService.setAlarm(true, (System.currentTimeMillis() + mHeartbeatInterval));
            } catch (IOException ioe) {
                if ((null != mConnThread) && (mIsConnectionRunning)) {
                    mConnThread.notifyOfNetworkProblems();
                }
                HttpConnectionThread.logE("RpgTcpHeartbeatSender.run()", "Could not send HB!", ioe);
            } catch (Throwable t) {
                if ((null != mConnThread) && (mIsConnectionRunning)) {
                    mConnThread.notifyOfNetworkProblems();
                }
                HttpConnectionThread.logE("RpgTcpHeartbeatSender.run()",
                        "Could not send HB! Unknown: ", t);
            } finally {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }

            synchronized (this) {
                try {
                    wait(mHeartbeatInterval + (mHeartbeatInterval / 5));
                } catch (InterruptedException e) {
                    HttpConnectionThread.logE("RpgTcpHeartbeatSender.run()", "Failed sleeping", e);
                }
            }
        }
    }

    /**
     * Prepares the necessary Hessian payload and writes it directly to the open
     * output-stream of the socket.
     * 
     * @throws Exception Thrown if there was an unknown problem writing to the
     *             output-stream.
     * @throws IOException Thrown if there was a problem regarding IO while
     *             writing to the output-stream.
     */
    public void sendHeartbeat() throws IOException, Exception {
        byte[] rpgMsg = getHeartbeatHessianPayload();

        try {
            // Try and issue the request
            if (Settings.ENABLED_TRANSPORT_TRACE) {
                Long userID = null;
                AuthSessionHolder auth = LoginEngine.getSession();
                if (auth != null) {
                    userID = auth.userID;
                }
                
                HttpConnectionThread.logI("RpgTcpHeartbeatSender.sendHeartbeat()",
                        "\n  * Sending a heartbeat for user ID "
                                + userID + "----------------------------------------"
                                + HessianUtils.getInHessian(new ByteArrayInputStream(rpgMsg), true)
                                + "\n");
            }

            if (null != mOs) {
                synchronized (mOs) {
                    mOs.write(rpgMsg);
                    mOs.flush();
                }
            }
        } catch (IOException ioe) {
            HttpConnectionThread.logE("RpgTcpHeartbeatSender.sendHeartbeat()",
                    "Could not write HB to OS!", ioe);
            throw ioe;
        } catch (Exception e) {
            HttpConnectionThread.logE("RpgTcpHeartbeatSender.sendHeartbeat()",
                    "Could not send HB to OS! Unknown: ", e);
            throw e;
        } finally {
            rpgMsg = null;
        }
    }

    /**
     * Returns a byte-array containing the data needed for sending a heartbeat
     * to the RPG.
     * 
     * @throws IOException If there was an exception serializing the hash map to
     *             a hessian byte array.
     * @return A byte array representing the heartbeat.
     */
    private byte[] getHeartbeatHessianPayload() throws IOException {
        // hash table for parameters to Hessian encode
        final Hashtable<String, Object> ht = new Hashtable<String, Object>();

        final String timestamp = "" + ((long)System.currentTimeMillis() / MILLIS_PER_SEC);
        final AuthSessionHolder auth = LoginEngine.getSession();
        ht.put("auth", AuthUtils
                .calculateAuth("", new Hashtable<String, Object>(), timestamp, auth));
        ht.put("userid", auth.userID);

        // do Hessian encoding
        final byte[] payload = HessianEncoder.createHessianByteArray("", ht);
        payload[1] = (byte)1;
        payload[2] = (byte)0;

        final int reqLength = RpgHeader.HEADER_LENGTH + payload.length;

        final RpgHeader rpgHeader = new RpgHeader();
        rpgHeader.setPayloadLength(payload.length);
        rpgHeader.setReqType(RpgMessageTypes.RPG_TCP_HEARTBEAT);

        final byte[] rpgMsg = new byte[reqLength];
        System.arraycopy(rpgHeader.createHeader(), 0, rpgMsg, 0, RpgHeader.HEADER_LENGTH);

        if (null != payload) {
            System.arraycopy(payload, 0, rpgMsg, RpgHeader.HEADER_LENGTH, payload.length);
        }

        return rpgMsg;
    }

    /**
     * <p>
     * Returns true if the heartbeat thread is currently running/active.
     * </p>
     * <p>
     * Calling startConnection() will make this method return as the heartbeat
     * thread gets started.
     * </p>
     * <p>
     * stopConnection() will stop the connection again and make this method
     * return false.
     * </p>
     * 
     * @return True if the connection is running, false otherwise.
     */
    public boolean getIsActive() {
        return mIsConnectionRunning;
    }

    @Override
    public void notifyOfWakeupAlarm() {
        if (Settings.ENABLED_TRANSPORT_TRACE) {
            HttpConnectionThread.logI("HeartbeatSenderThread.notifyOfWakeupAlarm()",
                    "Waking up for a heartbeat!");
        }

        synchronized (this) {
            notify(); // this will try to send another heartbeat
        }
    }
}
