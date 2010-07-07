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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import com.vodafone360.people.Settings;
import com.vodafone360.people.service.io.rpg.RpgHeader;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.hessian.HessianUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Reads responses from a TCP sockets and adds them to the response decoder.
 * Errors are called and passed back to the RpgTcpConnectionThread.
 * 
 * @author Rudy Norff (rudy.norff@vodafone.com)
 */
public class ResponseReaderThread implements Runnable {

    /**
     * Sleep time (in milliseconds) of the thread in between reads.
     */
    private static final long THREAD_SLEEP_TIME = 300; // ms

    /**
     * The ResponseReaderThread that was created by the TcpConnectionThread. It
     * will be compared to this thread. If they differ this thread must be a
     * lock thread that got stuck when the user changed APNs or switched from
     * WiFi to another data connection type.
     */
    protected static ResponseReaderThread mCurrentThread;

    /**
     * Decodes all responses coming in.
     */
    private DecoderThread mDecoder;

    /**
     * The main connection thread to call back when an error occured.
     */
    private TcpConnectionThread mConnThread;

    /**
     * The input stream to read responses from.
     */
    protected DataInputStream mIs;

    /**
     * Indicates that the thread is active and connected.
     */
    protected boolean mIsConnectionRunning;

    /**
     * Represents the thread that reads responses.
     */
    protected Thread mThread;

    /**
     * The socket to read from.
     */
    private Socket mSocket;

    /**
     * Constructs a new response reader used for reading bytes from a socket
     * connection.
     * 
     * @param connThread The connection thread that manages this and the
     *            heartbeat sender thread. It is called back whenever an error
     *            occured reading from the socket input stream.
     * @param decoder Used to decode all incoming responses.
     */
    public ResponseReaderThread(TcpConnectionThread connThread, DecoderThread decoder, Socket socket) {
        mConnThread = connThread;
        mDecoder = decoder;
        mSocket = socket;

        if (null != mSocket) {
            try {
                mSocket.setSoTimeout(Settings.TCP_SOCKET_READ_TIMEOUT);
            } catch (SocketException se) {
                HttpConnectionThread.logE("ResponseReaderThread()", "Could not set socket to!", se);
            }
        }
    }

    /**
     * Starts the connection by setting the connection flag and spawning a new
     * thread in which responses can be read without interfering with the rest
     * of the client.
     */
    public void startConnection() {
        HttpConnectionThread.logI("RpgTcpResponseReader.startConnection()",
                "STARTING Response Reader!");

        mIsConnectionRunning = true;
        mThread = new Thread(this, "RpgTcpResponseReader");
        mThread.start();
    }

    /**
     * Stops the connection by closing the input stream and setting it to null.
     * Attempts to stop the thread and sets it to null.
     */
    public void stopConnection() {
        mIsConnectionRunning = false;

        if (null != mSocket) {
            try {
                mSocket.shutdownInput();
            } catch (IOException ioe) {
                HttpConnectionThread.logE("RpgTcpHeartbeatSender.stopConnection()",
                        "Could not shutdown InputStream", ioe);
            } finally {
                mSocket = null;
            }
        }

        if (null != mIs) {
            try {
                mIs.close();
            } catch (IOException ioe) {
                HttpConnectionThread.logE("RpgTcpResponseReader.stopConnection()",
                        "Could not close InputStream", ioe);
            } finally {
                mIs = null;
            }
        }

        try {
            mThread.join(60); // give it 60 millis max
        } catch (InterruptedException ie) {
        }
    }

    /**
     * Sets the input stream that will be used to read responses from.
     * 
     * @param inputStream The input stream to read from. Should not be null as
     *            this would trigger a reconnect of the whole socket and cause
     *            all transport-threads to be restarted.
     */
    public void setInputStream(BufferedInputStream inputStream) {
        HttpConnectionThread.logI("RpgTcpResponseReader.setInputStream()", "Setting new IS: "
                + ((null != inputStream) ? "not null" : "null"));
        if (null != inputStream) {
            mIs = new DataInputStream(inputStream);
        } else {
            mIs = null;
        }
    }

    /**
     * Overrides the Thread.run() method and continuously calls the
     * readResponses() method which blocks and reads responses or throws an
     * exception that needs to be handled by reconnecting the sockets.
     */
    public void run() {
        while (mIsConnectionRunning) {
            checkForDuplicateThreads();

            try {
                byte[] response = readNextResponse();

                if ((null != response) && (response.length >= RpgHeader.HEADER_LENGTH)) {
                    mDecoder.handleResponse(response);
                }
            } catch (Throwable t) {
                HttpConnectionThread.logE("RpgTcpResponseReader.run()",
                        "Could not read Response. Unknown: ", t);

                if ((null != mConnThread) && (mIsConnectionRunning)) {
                    mIsConnectionRunning = false;
                    mConnThread.notifyOfNetworkProblems();
                }
            }

            try {
                Thread.sleep(THREAD_SLEEP_TIME);
            } catch (InterruptedException ie) {
            }
        }
    }

    /**
     * <p>
     * Attempts to read all the bytes from the DataInputStream and writes them
     * to a byte array where they are processed for further use.
     * </p>
     * <p>
     * As this method uses InputStream.read() it blocks the execution until a
     * byte has been read, the socket was closed (at which point an IOException
     * will be thrown), or the end of the file has been reached (resulting in a
     * EOFException being thrown).
     * </p>
     * 
     * @throws IOException Thrown if something went wrong trying to read or
     *             write a byte from or to a stream.
     * @throws EOFException Thrown if the end of the stream has been reached
     *             unexpectedly.
     * @return A byte-array if the response was read successfully or null if the
     *         response could not be read.
     */
    private byte[] readNextResponse() throws IOException, EOFException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int offset = 0;

        for (int i = 0; i < 2; i++) { // read delimiter, this method blocks
            int tag = -1;

            try {
                tag = mIs.read();
            } catch (SocketTimeoutException ste) {
                HttpConnectionThread.logW("ResponseReaderThread.readNextResponse()",
                        "Socket timed out reading!");
                checkForDuplicateThreads();
                return null;
            }

            if (tag != RpgHeader.DELIMITER_BYTE) {
                if (tag == -1) { // we reached EOF. This is a network issue
                    throw new EOFException();
                }
                HttpConnectionThread.logI("RpgTcpResponseReader.readResponses()",
                        "Returning... Tag is " + tag + " (" + (char)tag + ")");
                return null;
            }
        }
        final byte msgType = mIs.readByte();
        final int reqId = mIs.readInt();
        final int other = mIs.readInt();
        final int payloadSize = mIs.readInt();
        final byte compression = mIs.readByte();
        dos.writeByte(RpgHeader.DELIMITER_BYTE);
        dos.writeByte(RpgHeader.DELIMITER_BYTE);
        dos.writeByte(msgType);
        dos.writeInt(reqId);
        dos.writeInt(other);
        dos.writeInt(payloadSize);
        dos.writeByte(compression);

        byte[] payload = new byte[payloadSize];

        // read the payload
        while (offset < payloadSize) {
            offset += mIs.read(payload, offset, payloadSize - offset);
        }

        dos.write(payload);
        dos.flush();
        dos.close();
        final byte[] response = baos.toByteArray();

        if (Settings.ENABLED_TRANSPORT_TRACE) {
            if (reqId != 0) { // regular response
                HttpConnectionThread.logI("ResponseReader.readResponses()", "\n"
                        + "  < Response for ID " + reqId + " with payload-length " + payloadSize
                        + " received <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
                        + HessianUtils.getInHessian(new ByteArrayInputStream(response), false)
                        + "\n  ");
            } else { // push response
                HttpConnectionThread.logI("ResponseReader.readResponses()", "\n"
                        + "  < Push response " + " with payload-length " + payloadSize
                        + " received <x<x<x<x<x<x<x<x<x<x<x<x<x<x<x<x<x<x<x<x<x<x"
                        + HessianUtils.getInHessian(new ByteArrayInputStream(response), false)
                        + "\n  ");
            }

            // log file containing response to SD card
            if (Settings.sEnableSuperExpensiveResponseFileLogging) {
                LogUtils.logE("XXXXXXYYYXXXXXX Do not Remove this!");
                LogUtils.logToFile(response, "people_" + reqId + "_" + System.currentTimeMillis()
                        + "_resp_" + ((int)msgType)
                        + ((compression == 1) ? ".gzip_w_rpg_header" : ".txt"));
            } // end log file containing response to SD card
        }

        return response;
    }

    /**
     * Checks whether the current thread is not the same as the thread that
     * should be running. If it is not the connection and the thread is stopped.
     */
    private void checkForDuplicateThreads() {
        if (!this.equals(mCurrentThread)) {
            // The current thread created by the TcpConnectionThread is not
            // equal to this thread.
            // This thread must be old (locked due to the user changing his
            // network settings.
            HttpConnectionThread.logE("ResponseReaderThread.run()", "This thread is a "
                    + "locked thread caused by the connection settings being switched.", null);
            stopConnection(); // exit this thread
        }
    }
}
