/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 *
 * The contents of this file are subject to the
 * terms of the Common Development and Distribution
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 *  src/com/vodafone/people/VODAFONE.LICENSE.txt or
 * ###TODO:URL_PLACEHOLDER###
 * See the License for the specific language governing
 * permissions and limitations under the 
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER
 * in each file and include the License 
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 *  with the fields enclosed by brackets 
 * "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.service.transport.http.photouploadmanager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import com.caucho.hessian.micro.MicroHessianInput;

import android.util.Log;

import com.caucho.hessian.micro.MicroHessianInput;
import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.Request.Type;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.service.transport.DecoderThread.RawResponse;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.hessian.HessianDecoder;
import com.vodafone360.people.service.utils.hessian.HessianUtils;
import com.vodafone360.people.utils.LogUtils;

/*
 * PhotoUploadManager uploads the file to the server.
 * There are 2 usecases
 * a)File size is less than 600k 
 * b) file size > 600k.
 * For file size less than 600k,directly payload is made and sent to server using direct httpclient.
 * For file size greater than 600k,chunking is bieng done .
 * The request is sent to server in following three steps.
 * 1)Tell the server that file is to be uplaoded in chunks-using FUNCTION-StartUpload
 * .(Server return uploadid,which is used in subsequent uploading of chunks
 * 2)Read chunks from file and send chunks to server in size of 30k,
 * as mentioned by vodaphone,using function UploadChunk.
 * 3)End the chunking,telling the server that chunking is finished and this is last chunk-using function-UploadEnd.
 *Server return fileuuid.
 *The total request in case of chunking would be like
 * |----StartUpload----|--ReadFromFileInChunks--send chunksto server---|---EndChunking---uploadend--|
 * In the second and third step,uploadid is used,which is returned by server,
 * when startupload request is sent to server as firstpayload.
 *     
 */

public class PhotoUploadManager extends Thread implements IConnection {
    
    
    /**
     * Clinet for execution of the httppost
     */
    private volatile HttpClient mHttpClient;

    /**
     * Decoder thread thats going to be same
     */
    private DecoderThread mdecoder = null;
    /**
     * Chunk Number of the payload to be send
     */
    Integer mchunkNumber = 0;
    /**
     * URL to add the content
     */
    private URI mApiUrl;
    /**
     * singleton instance.
     */
    private static PhotoUploadManager m_instance = null;
    /**
     * ref counting for singleton.
     */
    private static int refCount = 0;
    /**
     * Checks whether its runnig.
     */
    Boolean mRunning = true;
    /**
     * Max size to be sent in one go.600k.
     */
    Long MAX_FILE_SEND_IN_ONE_CHUNK = new Long(614400);
    /**
     * Singleton pattern.
     * @param decode
     * @return Instance singleton
     */

    public static PhotoUploadManager getInstanceContentUpload(
            final DecoderThread decode) {

        if (m_instance != null) {
            refCount++;

        } else {
            m_instance = new PhotoUploadManager(decode);
        }
        return m_instance;
    }

    /**
     * Constructor.
     * @param decode decoder thread.
     */
    PhotoUploadManager(final DecoderThread decode) {
        super();
        mdecoder = decode;
        try {
            mApiUrl = (new URL(SettingsManager
                    .getProperty(Settings.SERVER_URL_HESSIAN_KEY))).toURI();
            int connectionTimeout = Settings.HTTP_CONNECTION_TIMEOUT;
            HttpParams myHttpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(myHttpParams,
                    connectionTimeout);

            HttpConnectionParams.setSoTimeout(myHttpParams, connectionTimeout);
            mHttpClient = new DefaultHttpClient(myHttpParams); // get http
        } catch (MalformedURLException e) {
            LogUtils.logE("HttpContentUpload-Error defining URL");
        } catch (URISyntaxException e) {
            LogUtils.logE("HttpContentUpload-Error defining URI");
        }

    }

    /**
     * Printing the response.
     * @param response inoput.
     * @return integer.
     */
    final int  print(final HttpResponse response) {
        byte[] ret = null;
        int respCode = response.getStatusLine().getStatusCode();
        try {
            HttpEntity entity = response.getEntity();
            if (null != entity) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                InputStream is = entity.getContent();
                if (null != is) {
                    int nextByte = 0;
                    while ((nextByte = is.read()) != -1) {
                        baos.write(nextByte);
                    }
                    baos.flush();
                    ret = baos.toByteArray();
                    baos.close();
                    baos = null;
                }
                entity.consumeContent();
            }
            if (Settings.ENABLED_TRANSPORT_TRACE) {
                int length = 0;
                if (ret != null) {
                    length = ret.length;
                }
                Log.v("HttpContentUpload-handleApiResponse()", "\n \n \n"
                        + "Response with length "
                        + length
                        + " bytes received "
                        + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
                        + (length == 0 ? "" : HessianUtils.getInHessian(
                                new ByteArrayInputStream(ret), false)));

            }
        } catch (IOException e) {
            Log.v("HttpContentUpload Exception", "e" + e);
        }

        return respCode;
    }

    /**
     * first chunk.
     * @param resp
     * @return long 
     */
    Long getUploadIDFromUploadStart(HttpResponse resp) {

        byte[] ret = null;
        HttpEntity entity = resp.getEntity();
        Long uploadid = null;
        InputStream is = null;
        try {
            if (null != entity) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is = entity.getContent();
            }
            MicroHessianInput mhi = null;
            if (is != null) {
                mhi = new MicroHessianInput(is);
            }
            HessianDecoder hessianDecoder = new HessianDecoder();
            int tag = is.read(); // initial map tag or fail

            if (tag == 'r') { // reply / response
                is.read(); // read major and minor
                is.read();

                tag = is.read(); // read next tag
                // usesReplyTag = true;
            }
            Hashtable<String, Object> map = (Hashtable<String, Object>) mhi
                    .decodeType(tag);
            Enumeration<String> e = map.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                Object value = map.get(key);
                String keyObj = (String) key;
                String upload = new String("uploadid");
                if (keyObj.compareTo(upload) == 0) {
                    uploadid = (Long) value;
                    break;
                }
                String numOfChunk = new String("numofchunks");
                if (numOfChunk.compareTo(keyObj) == 0) {
                    mchunkNumber = (Integer) value;

                }
            }
            entity.consumeContent();
        } catch (IOException e) {

        }
        return uploadid;
    }

    /**
     * second chunk.
     * @param request Input.
     * @param reqIds INput.
     * @return hhresponse.
     */
    private HttpResponse UploadFirstPayload(final Request request, final List<Integer> reqIds) {
        byte[] payload1 = request.getEncodedUploadStartPayload();
        HttpResponse resp = null;
        Boolean merror = false;
        try {
            resp = postHTTPRequest(payload1, mApiUrl,
                    Settings.HTTP_HEADER_CONTENT_TYPE);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // handleApiResponse(resp, reqIds);
            } else {
                merror = true;
                addErrorToResponseQueue(reqIds);
                resp = null;
            }
        } catch (Exception e) {
            addErrorToResponseQueue(reqIds);
            resp = null;
        }
        return resp;
    }
    /**
     * Uploadfile in chnks.
     * @param request Input.
     * @param reqIds Input.
     * @param uploadid Input.
     * @return boolean for success.
     */
    Boolean UploadFileInChunks(final Request request, final List<Integer> reqIds,
            final Long uploadid) {
        HttpResponse resp = null;
        File mfilname = new File(request.mfileName);
        Boolean merror = false;
        try {
            FileInputStream fin = new FileInputStream(mfilname);
            byte[] arrayBuff = new byte[request.chunkSize];
            Long num = request.mfileSize / new Long(request.chunkSize);
            int offset = 0;
            Integer mchunkNumberToUpload = 1;

            while (mchunkNumber > 1) {
                int sizeread = fin.read(arrayBuff);
                mchunkNumber--;
                offset = offset + sizeread;
                byte[] payload2 = request.getEncodedUploadChunkPayload(
                        arrayBuff, uploadid, mchunkNumberToUpload);
                resp = postHTTPRequest(payload2, mApiUrl,
                        Settings.HTTP_HEADER_CONTENT_TYPE);
                if (print(resp) == HttpStatus.SC_OK) {
                    Log.v("http-contentuplaod", "Response-SC_OK");
                } else {
                    addErrorToResponseQueue(reqIds);
                    merror = true;
                    break;
                }
                mchunkNumberToUpload++;
            }
            if (merror == false) {
                Long left = request.mfileSize - offset;
                byte[] lastOfFile = new byte[left.intValue()];
                int sizeread = fin.read(lastOfFile);
                fin.close();
                byte[] payload3 = request.getEncodedUploadChunkPayload(
                        lastOfFile, uploadid, mchunkNumberToUpload);
                resp = postHTTPRequest(payload3, mApiUrl,
                        Settings.HTTP_HEADER_CONTENT_TYPE);
                if (print(resp) == HttpStatus.SC_OK) {
                    Log.v("HttpContentUpload", "Correct-Http-Response");
                } else {
                    addErrorToResponseQueue(reqIds);
                    merror = true;
                }
            }
        } catch (Exception e) {
            merror = true;
            addErrorToResponseQueue(reqIds);
        }
        return merror;
    }
    /**
     * Uploadfile end.
     * @param request input
     * @param reqIds  input.
     * @param uploadid input.
     * @return boolean.
     */
    private Boolean UploadFileEnd(final Request request, final List<Integer> reqIds, Long uploadid) {
        HttpResponse resp = null;
        Boolean merror = false;
        try {
            byte[] payload4 = request.getEncodedUploadEndPayload(uploadid);
            resp = postHTTPRequest(payload4, mApiUrl,
                    Settings.HTTP_HEADER_CONTENT_TYPE);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                handleApiResponse(resp, reqIds);
            } else {
                merror = true;
                addErrorToResponseQueue(reqIds);
            }
        } catch (Exception e) {
            merror = true;
            addErrorToResponseQueue(reqIds);
        }
        return merror;
    }
   /**
    * Upload file in 1 chunk.
    * @param request input
    * @param reqIds input.
    * @return boolean.
    */
    private Boolean UploadFileInOneChunk(final Request request, final List<Integer> reqIds) {
        Boolean merror = false;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            request.writeToOutputStream(baos, false);
            byte[] payload = baos.toByteArray();
            if (Settings.ENABLED_TRANSPORT_TRACE) {
                logI("AuthenticationManager.handleAuthRequests()",
                        "\n \n \nAUTHENTICATING: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                                + HessianUtils
                                        .getInHessian(new ByteArrayInputStream(
                                                payload), true));
            }
            HttpResponse resp = postHTTPRequest(payload, mApiUrl,
                    Settings.HTTP_HEADER_CONTENT_TYPE);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                handleApiResponse(resp, reqIds);
            } else {
                merror = true;
                addErrorToResponseQueue(reqIds);
            }

        } catch (Exception e) {
            merror = true;
            addErrorToResponseQueue(reqIds);
        }
        return merror;
    }

    /**
     * Uses the passed http connection to start a synchronous request against
     * the API. This method blocks until the request is made and the response is
     * retrieved.
     */
    public final void run() {

        while (mRunning) {
            List<Request> requests = QueueManager.getInstance()
                    .getApiRequests();
            List<Integer> reqIds = null;
            Boolean error = false;
            if (null == requests) {
                return;
            }
            for (int i = 0; i < requests.size(); i++) {
                Request request = requests.get(i);
                reqIds = new ArrayList<Integer>();
                reqIds.add(request.getRequestId());
                if (request.getAuthenticationType() == 1) { // USE_API==1
                    if (request.mType == Request.Type.UPLOAD_PHOTO) {
                        if (request.mfileSize == null
                                || request.mfileSize < MAX_FILE_SEND_IN_ONE_CHUNK) {
                            UploadFileInOneChunk(request, reqIds);
                        } else {
                            Long uploadid = null;
                            HttpResponse resp = null;
                            resp = UploadFirstPayload(request, reqIds);
                            if (resp != null
                                    && resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                uploadid = getUploadIDFromUploadStart(resp);
                                if (!UploadFileInChunks(request, reqIds,
                                        uploadid)) {
                                    if (!UploadFileEnd(request, reqIds,
                                            uploadid)) {
                                        Log.i("Photomanager", "Success Upload");
                                    }
                                }
                            }
                        }
                    }
                }
            } // end for
            synchronized (this) {
                try {
                    wait();
                } catch (Exception e) {
                }
            }
        } // end while

    }

    /**
     * Used to post the response to server.Its synchronous API.
     */
    final public HttpResponse postHTTPRequest(final byte[] postData,final URI uri,
            final String contentType) throws Exception {

        HttpResponse response = null;
        if (null == postData) {
            return response;
        }
        if (uri != null) {
            Log.v("httpcontentupload-postHTTPRequest()", "HTTP Requesting URI "
                    + uri.toString() + " " + contentType);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Content-Type", contentType);
            httpPost.addHeader("User-Agent", "PeopleRPGClient/1.0");
            httpPost.addHeader("Cache-Control", "no-cache");
            httpPost.setEntity(new ByteArrayEntity(postData));
            try {
                response = mHttpClient.execute(httpPost);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    @Override
    public final boolean getIsConnected() {
        return true;
    }

    @Override
    public final boolean getIsRpgConnectionActive() {
        return true;
    }

    @Override
    public void notifyOfRegainedNetworkCoverage() {
    }

    @Override
    public void notifyOfUiActivity() {
    }

    @Override
    public void onLoginStateChanged(final boolean isLoggedIn) {
    }

    @Override
    public final void startThread() {
        mRunning = true;
        start();
    }

    @Override
    public final void stopThread() {
        synchronized (this) {
            mRunning = false;
            notify();
        }
    }

    @Override
    public final void notifyOfItemInRequestQueue() {
        synchronized (this) {
            notify();
            // handleRequests();
        }
    }
    /**
     *
     * @param reqIds input.
     */
    final void addErrorToResponseQueue(final List<Integer> reqIds) {
        EngineId source = null;
        QueueManager requestQueue = QueueManager.getInstance();
        ResponseQueue responseQueue = ResponseQueue.getInstance();
        for (Integer reqId : reqIds) {
            // attempt to get type from request
            Request req = requestQueue.getRequest(reqId);
            if (req != null)
                source = req.mEngineId;
            responseQueue.addToResponseQueue(reqId, null, source);
        }

    }

    /**
     * Handles the synchronous responses for the authentication calls which go
     * against the API directly by adding it to the queue and checking if the
     * response code was a HTTP 200. TODO: this should be refactored into a
     * AuthenticationManager class.
     *
     * @param response
     *            The response to add to the decoder.
     * @param reqIds
     *            The request IDs the response is to be decoded for.
     * @throws Exception
     *             Thrown if the status line could not be read or the response
     *             is null.
     */
    final public void handleApiResponse(final HttpResponse response,final List<Integer> reqIds)
            throws Exception {
        byte[] ret = null;
        if (null != response) {
            if (null != response.getStatusLine()) {
                int respCode = response.getStatusLine().getStatusCode();

                switch (respCode) {
                case HttpStatus.SC_OK:
                case HttpStatus.SC_CONTINUE:
                case HttpStatus.SC_CREATED:
                case HttpStatus.SC_ACCEPTED:
                case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
                case HttpStatus.SC_NO_CONTENT:
                case HttpStatus.SC_RESET_CONTENT:
                case HttpStatus.SC_PARTIAL_CONTENT:
                case HttpStatus.SC_MULTI_STATUS:
                    HttpEntity entity = response.getEntity();
                    if (null != entity) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        InputStream is = entity.getContent();
                        if (null != is) {
                            int nextByte = 0;
                            while ((nextByte = is.read()) != -1) {
                                baos.write(nextByte);
                            }
                            baos.flush();
                            ret = baos.toByteArray();
                            baos.close();
                            baos = null;
                        }
                        entity.consumeContent();
                    }

                    if (Settings.ENABLED_TRANSPORT_TRACE) {
                        int length = 0;
                        if (ret != null) {
                            length = ret.length;
                        }
                        PhotoUploadManager
                                .logI(
                                        "ResponseReader.handleApiResponse()",
                                        "\n \n \n"
                                                + "Response with length "
                                                + length
                                                + " bytes received "
                                                + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
                                                + (length == 0 ? ""
                                                        : HessianUtils
                                                                .getInHessian(
                                                                        new ByteArrayInputStream(
                                                                                ret),
                                                                        false)));

                    }
                    addToDecoder(ret, reqIds);
                    break;
                default:
                    addErrorToResponseQueue(reqIds);
                }
            } else {
                throw new Exception("Status line of response was null.");
            }
        } else {
            throw new Exception("Response was null.");
        }
    }

    /**
     * Adds a response to the response decoder.
     *
     * @param input
     *            The data of the response.
     * @param reqIds
     *            The request IDs that a response was received for.
     */
    private void addToDecoder(final byte[] input, final List<Integer> reqIds) {
        if (input != null && mdecoder != null) {
            int reqId = reqIds.size() > 0 ? reqIds.get(0) : 0;
            mdecoder.addToDecode(new RawResponse(reqId, input, false, false));

        }
    }
    /**
     * logs.
     * @param tag input
     * @param message input
     */
    public static void logI(final String tag, final String message) {
        if (Settings.ENABLED_TRANSPORT_TRACE) {
            Thread t = Thread.currentThread();
            Log
                    .i("(PROTOCOL)", tag + "[" + t.getName() + "]" + " : "
                            + message);
        }
    }

}