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

package com.vodafone360.people.service.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.rpg.RpgMessage;
import com.vodafone360.people.service.io.rpg.RpgMessageTypes;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.AuthUtils;
import com.vodafone360.people.service.utils.hessian.HessianEncoder;
import com.vodafone360.people.utils.CloseUtils;
import java.io.ByteArrayOutputStream;
import android.util.Log;
import com.caucho.hessian.micro.MicroHessianOutput;

/**
 * Container class for Requests issued from client to People server via the
 * transport layer. A request consists of a payload, an identifier for the
 * engine responsible for handling this request, a request type and a request id
 * which is generated on creation of the request.
 */
public class Request {

    /**
     * Request types, these are based on the content requested from, or
     * delivered to, the server, and whether the returned response contains a
     * type identifier or not.
     */
    public enum Type {
        COMMON, // Strongly typed
        ADD_CONTACT,
        TEXT_RESPONSE_ONLY,
        CONTACT_CHANGES_OR_UPDATES,
        CONTACT_DELETE,
        CONTACT_DETAIL_DELETE,
        FRIENDSHIP_REQUEST,
        USER_INVITATION,
        SIGN_UP,
        SIGN_IN,
        RETRIEVE_PUBLIC_KEY,
        EXPECTING_STATUS_ONLY,
        GROUP_LIST,
        STATUS_LIST,
        STATUS,
        CONTACT_GROUP_RELATION_LIST,
        CONTACT_GROUP_RELATIONS,
        ITEM_LIST_OF_LONGS,
        PRESENCE_LIST,
        AVAILABILITY,
        CREATE_CONVERSATION,
        SEND_CHAT_MESSAGE,
        PUSH_MSG,
        EXTERNAL_RPG_RESPONSE,
        // response to external RPG request
        /**
         * For uploading the photo.
         */
          UPLOAD_PHOTO, //UPLOADFILE,
          /**
           * For getting the group id of shared album.
           */
          GET_DEFAULT_ALBUM360,
          /**
           * For sharing of photo with album.
           */
          SHARE_PHOTO_WITH_ALBUM,
          /**
           * For sharing albums with groups.
           */
          SHARE_ALBUMS,
          /**
           * For adding albums on server.
           */
          ADD_ALBUMS

    }

    /*
     * List of parameters which will be used to generate the AUTH parameter.
     */
    private Hashtable<String, Object> mParameters = new Hashtable<String, Object>();

    /**
     * Name of method on the backend. E.g. identities/getavailableidentities.
     */
    private String mApiMethodName;

    /** RPG message payload (usually Hessian encoded message body). */
    // private byte[] mPayload;
    /** RPG Message type - as defined above. */
    public Type mType;

    /** Handle of Engine associated with this RPG message. */
    public EngineId mEngineId = EngineId.UNDEFINED; // to be used to map request
                                                    // to appropriate engine

    /** Whether we use RPG for this message. */
    // public boolean mUseRpg = true;
    /** active flag - set to true once we have actually issued a request */
    private boolean mIsActive = false;

    /**
     * The timeout set for the request. -1 if not set.
     */
    private long mTimeout = -1;

    /**
     * The expiry date calculated when the request gets executed.
     */
    private long mExpiryDate = -1;

    /** true if the request has expired */
    public boolean expired;

    /**
     * The timestamp when the request's auth was calculated.
     */
    private long mAuthTimestamp;

    /**
     * The timestamp when this request was created.
     */
    private long mCreationTimestamp;

    /**
     * <p>
     * Represents the authentication type that needs to be taken into account
     * when executing this specific request. There are 3 types of authentication
     * types:
     * </p>
     * <ul>
     * <li>USE_API: needs the API. These requests are usually requests like
     * getSessionByCredentials that use application authentication.</li>
     * <li>USE_RPG: these requests should use the RPG and some of them MUST use
     * the RPG. Usually, all requests after the auth requests should use the
     * RPG.</li>
     * <li>USE_BOTH: some requests like the requests for getting terms and
     * conditions or privacy statements need to be able to use the RPG or API at
     * any time of the application lifecycle.</li>
     * </ul>
     */
    private byte mAuthenticationType;

    public static final byte USE_API = 1, USE_RPG = 2, USE_BOTH = 3;

    /**
     * If true, this method is a fire and forget method and will not expect any
     * responses to come in. This fact can be used for removing requests from
     * the queue as soon as they have been sent.
     */
    private boolean mIsFireAndForget;

    /** RPG message request id. */
    private int mRequestId;

    /**
     * Customised for file Upload-content-upload.
     * filled directly from contents.java.
     */
    public Long mfileSize = null;

    /**
     * Customised for file Upload-content-upload.
     * filled directly from contents.java
     */
    public String mfileName = null;

     /**
     * Customised for file Upload-content-upload.
     * Chunk Size in which file will be uploaded.
     */
   // public Integer chunkSize = 24576;
    public Integer chunkSize = 30720;
    /**
     * Function for uploading file in chunks.
     * Startupload.
     */
    static final String FUNCTION_STARTUPLOAD = "upload/uploadstart";
    /**
     * Function for uploading file in chunks.
     */
     static final String FUNCTION_UPLOADCHUNK = "upload/uploadchunk";

    /**
     *Function for ending the chunking.
     */

     static final String FUNCTION_ENDCHUNK = "upload/uploadend";
    /**
     *data used in upload chunks.
     */
     
	static final String DATA = "data";
	/**
	 * Chunk Number used for uploading the chunk.
	 */
    static final String CHUNKNUMB =  "chunknum";
    /**
     * Uploadid for each chunk.
     */
    static final String UPLOADID = "uploadid";
    /**
     * CHunk Size to be uplaoded.
     */
    static final String CHUNKSIZE = "chunksize";
    /**
     * Total size of file.
     */
    static final String TOTALSIZE = "totalsize";
    //mParameters.put("chunksize",chunkSize);
    //mParameters.put("totalsize",mfileSize);
    
    /**
     * Constructor used for constructing internal (RPG/API) requests.
     * 
     * @param apiMethodName The method name of the call, e.g.
     *            "identities/getavailableidentities".
     * @param type RPG message type.
     * @param engId The engine ID. Will be used for routing the response to this
     *            request back to the engine that can process it.
     * @param needsUserAuthentication If true we need to authenticate this
     *            request by providing the session in the auth. This requires
     *            the user to be logged in.
     * @param isFireAndForget True if the request is a fire and forget request.
     * @param timeout the timeout in milliseconds before the request throws a
     *            timeout exception
     */
    public Request(String apiMethodName, Type type, EngineId engineId, boolean isFireAndForget,
            long timeout) {
        mType = type; // TODO find out a type yourself?
        mEngineId = engineId;
        mApiMethodName = apiMethodName;
        mIsFireAndForget = isFireAndForget;
        mCreationTimestamp = System.currentTimeMillis();
        mTimeout = timeout;

        if ((type == Type.RETRIEVE_PUBLIC_KEY) || (type == Type.SIGN_UP)
        		|| (type == Type.STATUS)
                || (type == Type.SIGN_IN)
                || type == Type.UPLOAD_PHOTO) {
            // we need to register, sign in, get t&c's etc. so the request needs
            // to happen without
            // user auth
            mAuthenticationType = USE_API;
        } else if (type == Type.TEXT_RESPONSE_ONLY) { // t&c or privacy
            mAuthenticationType = USE_BOTH;
        } else { // all other requests should use the RPG by default
            mAuthenticationType = USE_RPG;
        }
    }

    /**
     * Constructor used for constructing an external request used for fetching
     * e.g. images.
     * 
     * @param externalUrl The external URL of the object to fetch.
     * @param urlParams THe parameters to add to the URL of this request.
     * @param engineId The ID of the engine that will be called back once the
     *            response for this request comes in.
     */
    public Request(String externalUrl, String urlParams, EngineId engineId) {
        mType = Type.EXTERNAL_RPG_RESPONSE;
        mEngineId = engineId;
        mApiMethodName = "";
        mIsFireAndForget = false;
        mCreationTimestamp = System.currentTimeMillis();

        mParameters = new Hashtable<String, Object>();
        mParameters.put("method", "GET");
        mParameters.put("url", externalUrl + urlParams);

        mAuthenticationType = USE_RPG;
    }

    /**
     * Is request active - i.e has it been issued
     * 
     * @return true if request is active
     */
    public boolean isActive() {
        return mIsActive;
    }

    /**
     * <p>
     * Sets whether this request is active or not. An active request is a
     * request that is currently being sent to the server and awaiting a
     * response.
     * </p>
     * <p>
     * The reason is that an active request should not be sent twice.
     * </p>
     * 
     * @param isActive True if the request is active, false otherwise.
     */
    public void setActive(boolean isActive) {
        mIsActive = isActive;
    }

    /**
     * Returns a description of the contents of this object
     * 
     * @return The description
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Request [mEngineId=");
        sb.append(mEngineId); 
        sb.append(", mIsActive="); sb.append(mIsActive); 
        sb.append(", mTimeout="); sb.append(mTimeout); 
        sb.append(", mReqId="); sb.append(mRequestId); 
        sb.append(", mType="); sb.append(mType);
        sb.append(", mNeedsUserAuthentication="); sb.append(mAuthenticationType); 
        sb.append(", mExpired="); sb.append(expired);
        sb.append(", mDate="); sb.append(mExpiryDate); sb.append("]\n");
        sb.append(mParameters);
        
        return sb.toString();
    }

    /**
     * Adds element to list of params
     * 
     * @param name key name for object to be added to parameter list
     * @param nv object which will be added
     */
    public void addData(String name, Hashtable<String, ?> nv) {
        mParameters.put(name, nv);
    }

    /**
     * Adds element to list of params
     * 
     * @param name key name for object to be added to parameter list
     * @param value object which will be added
     */
    public void addData(String name, Vector<Object> value) {
        mParameters.put(name, value);
    }

    /**
     * Adds element to list of params
     * 
     * @param name key name for object to be added to parameter list
     * @param value object which will be added
     */
    public void addData(String name, List<String> value) {
        mParameters.put(name, value);
    }

    /**
     * Adds byte array to parameter list
     * 
     * @param varName name
     * @param value byte[] array to be added
     */
    public void addData(String varName, byte[] value) {
        mParameters.put(varName, value);
    }

    /**
     * Create new NameValue and adds it to list of params
     * 
     * @param varName name
     * @param value string value
     */
    public void addData(String varName, String value) {
        mParameters.put(varName, value);
    }

    /**
     * Create new NameValue and adds it to list of params
     * 
     * @param varName name
     * @param value Long value
     */
    public void addData(String varName, Long value) {
        mParameters.put(varName, value);
    }

    /**
     * Create new NameValue and adds it to list of params
     * 
     * @param varName name
     * @param value Integer value
     */
    public void addData(String varName, Integer value) {
        mParameters.put(varName, value);
    }

    /**
     * Create new NameValue and adds it to list of params
     * 
     * @param varName name
     * @param value Boolean value
     */
    public void addData(String varName, Boolean value) {
        mParameters.put(varName, value);
    }

    /**
     * Returns a Hashtable containing current request parameter list.
     * 
     * @return params The parameters that were added to this backend request.
     */
    /*
     * public Hashtable<String, Object> getParameters() { return mParameters; }
     */

    /**
     * Returns the authentication type of this request. This can be one of the
     * following: USE_API, which must be used for authentication requests that
     * need application authentication, USE_RPG, useful for requests against the
     * API that need user authentication and want to profit from the mobile
     * enhancements of the RPG, or USE_BOTH for requests that need to be able to
     * be used on the API or RPG. These requests are for example the Terms and
     * Conditions requests which need to be accessible from anywhere in the
     * client.
     * 
     * @return True if this method requires user authentication or a valid
     *         session to be more precise. False is returned if the method only
     *         needs application authentication.
     */
    public byte getAuthenticationType() {
        return mAuthenticationType;
    }

    /**
     * Gets the request ID of this request.
     * 
     * @return The unique request ID for this request.
     */
    public int getRequestId() {
        return mRequestId;
    }

    /**
     * Sets the request ID for this request.
     * 
     * @param requestId The request ID to set.
     */
    public void setRequestId(int requestId) {
        mRequestId = requestId;
    }

    /**
     * Gets the time stamp when this request's hash was calculated.
     * 
     * @return The time stamp representing the creation date of this request.
     */
    public long getAuthTimestamp() {
        return mAuthTimestamp;
    }

    /**
     * Gets the time stamp when this request was created.
     * 
     * @return The time stamp representing the creation date of this request.
     */
    public long getCreationTimestamp() {
        return mCreationTimestamp;
    }

    /**
     * Gets the set timeout.
     * 
     * @return the timeout in milliseconds, -1 if not set.
     */
    public long getTimeout() {
        return mTimeout;
    }

    /**
     * Gets the calculated expiry date.
     * 
     * @return the expiry date in milliseconds, -1 if not set.
     */
    public long getExpiryDate() {
        return mExpiryDate;
    }

    /**
     * Overwrites the timestamp if we need to wait one more second due to an
     * issue on the backend.
     * 
     * @param timestamp The timestamp in milliseconds(!) to overwrite with.
     */
    /*
    public void overwriteTimetampBecauseOfBadSessionErrorOnBackend(long timestamp) {
        mAuthTimestamp = timestamp;

        if (null != mParameters) {
            if (null != mParameters.get("timestamp")) {
                String ts = "" + (timestamp / 1000);
                mParameters.put("timestamp", ts);
            }
        }
    }
*/
    
    /**
     * Returns the API call this request will use.
     * 
     * @return The API method name this request will call.
     */
    public String getApiMethodName() {
        return mApiMethodName;
    }

    /**
     * Returns true if the method is a fire and forget method. Theses methods
     * can be removed from the request queue as soon as they have been sent out.
     * 
     * @return True if the request is fire and forget, false otherwise.
     */
    public boolean isFireAndForget() {
        return mIsFireAndForget;
    }

    /**
     * Serializes the request's data structure to the passed output stream
     * enabling the connection to easily prepare one or multiple) requests.
     * 
     * @param os The output stream to serialise this request to.
     * @param writeRpgHeader If true the RPG header is written.
     */
    public void writeToOutputStream(OutputStream os, boolean writeRpgHeader) {
        if (null == os) {
            return;
        }

        byte[] body;
        calculateAuth();
        try {
            body = makeBody();
            if (!writeRpgHeader) {
                os.write(body); // writing to the api directly
                return;
            }
        } catch (IOException ioe) {
            HttpConnectionThread.logE("Request.writeToOutputStream()",
                    "Failed writing standard API request: " + mRequestId, ioe);
            return;
        }

        int requestType = 0;
        if (mType == Request.Type.PRESENCE_LIST) {
            requestType = RpgMessageTypes.RPG_GET_PRESENCE;
        } else if (mType == Request.Type.AVAILABILITY) {
            requestType = RpgMessageTypes.RPG_SET_AVAILABILITY;
        } else if (mType == Request.Type.CREATE_CONVERSATION) {
            requestType = RpgMessageTypes.RPG_CREATE_CONV;
        } else if (mType == Request.Type.SEND_CHAT_MESSAGE) {
            requestType = RpgMessageTypes.RPG_SEND_IM;
        } else if (mType == Request.Type.EXTERNAL_RPG_RESPONSE) {
            requestType = RpgMessageTypes.RPG_EXT_REQ;
        } else {
            requestType = RpgMessageTypes.RPG_INT_REQ;
        }

        byte[] message = RpgMessage.createRpgMessage(body, requestType, mRequestId);
        

        try {
            os.write(message);
        } catch (IOException ioe) {
            HttpConnectionThread.logE("Request.writeToOutputStream()",
                    "Failed writing RPG request: " + mRequestId, ioe);
        }
    }

    /**
     * Creates the body of the request using the parameter list.
     * 
     * @return payload The hessian encoded payload of this request body.
     * @throws IOException Thrown if anything goes wrong using the hessian
     *             encoder.
     */
    private byte[] makeBody() throws IOException {
        // XXX this whole method needs to go or at least be changed into a
        // bytearray outputstream

        byte[] payload = HessianEncoder.createHessianByteArray(mApiMethodName, mParameters);
        if (payload == null) {
            return null;
        }
        payload[1] = (byte)1; // TODO we need to change this if we want to use a
                              // baos
        payload[2] = (byte)0;

        return payload;
    }

    /**
     * Gets the auth of this request. Prior to the the
     * writeToOutputStream()-method must have been called.
     * 
     * @return The auth of this request or null if it was not calculated before.
     */
    public String getAuth() {
        return (String)mParameters.get("auth");
    }

    /**
     * Calculate the Auth value for this Requester. TODO: Throttle by
     * timestamp/function to prevent automatic backend log out
     */
    private void calculateAuth() {
        String ts = null;
        if (null != mParameters) {
            ts = (String)mParameters.get("timestamp");
        }

        if (null == ts) {
            ts = "" + (System.currentTimeMillis() / 1000);
        }

        AuthSessionHolder session = LoginEngine.getSession();
        if (session != null) {
            addData("auth", SettingsManager.getProperty(Settings.APP_KEY_ID) + "::"
                    + session.sessionID + "::" + ts);
        } else {
            addData("auth", SettingsManager.getProperty(Settings.APP_KEY_ID) + "::" + ts);
        }

        addData("auth", AuthUtils.calculateAuth(mApiMethodName, mParameters, ts, session));
        /**
         * if (mNeedsUserAuthentication) { addData("auth",
         * AuthUtils.calculateAuth(mApiMethodName, mParameters, ts, session)); }
         * else { // create the final auth without the session addData("auth",
         * AuthUtils.calculateAuth(mApiMethodName, mParameters, ts, null)); }
         */
    }


    /**
     * Called as first payload sent to server by direct -http.
     * httpContentupload directly uses function to get the initial payload.
     * @return first byte load.
     */

    public final  byte[] getEncodedUploadStartPayload() {

        mParameters.clear();
         byte[] payload = null;
         try {
	         mApiMethodName = FUNCTION_STARTUPLOAD;
	         mParameters.put(CHUNKSIZE, chunkSize);
	         mParameters.put(TOTALSIZE, mfileSize);
	         calculateAuth();
	         ByteArrayOutputStream bos = new ByteArrayOutputStream();
	         MicroHessianOutput mho = new MicroHessianOutput(bos);
	         mho.startCall(mApiMethodName);
	         HessianEncoder.writeHashtable(mParameters, mho);
	         mho.completeCall();
	         CloseUtils.close(bos);
	         payload = bos.toByteArray();
	         payload[1] = (byte) 1;
	         // TODO we need to change this if we want to use a
	         // baos
	         payload[2] = (byte) 0;
          } catch (IOException e) {
             Log.v("Request-getUploadStart-exception", "error" + e);
         }
         return payload;
    }

    /**
     * Used to hessian encode byte array to hessian encoded array.
     * httpcontentupload directly read file.
     * and uses this function to convert array.
     * of bytes to hessian encoded byte array.
     * @param ar input array
     * @param uploadid uploadid for each upload.
     * @param chunkNumber chunknumber to be uploaded.
     * @return byte array for uploading hessian encoded.
     *
     */

      public final   byte[] getEncodedUploadChunkPayload(final byte[] ar,
    		        final Long uploadid,
                    final Integer chunkNumber) {

        mParameters.clear();
        mApiMethodName = FUNCTION_UPLOADCHUNK;
        byte[] payload = null;
        Long muploadID = uploadid;
        Integer muploadChunk = chunkNumber;
        mParameters.put(DATA, ar);
        mParameters.put(CHUNKNUMB, muploadChunk);
        mParameters.put(UPLOADID, muploadID);
        calculateAuth();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MicroHessianOutput mho = new MicroHessianOutput(bos);
        try {
	        mho.startCall(mApiMethodName);
	        HessianEncoder.writeHashtable(mParameters, mho);
	        mho.completeCall();
        } catch (IOException e) {
            Log.v("Request-Exception-request-getUploadChunk", "error" + e);
        }
        CloseUtils.close(bos);
        payload = bos.toByteArray();
        payload[1] = (byte) 1;
        // TODO we need to change this if we want to use a
        // baos
        payload[2] = (byte) 0;
        return payload;
    }

    /**
     * Ends the payload-This last chunk is sent to .
     * server to indicate that upload has ended.
     *
     * @param uploadid uploadid to be sent to server.
     * @return byte load last payload.
     *
     */

       public final byte[] getEncodedUploadEndPayload(final Long uploadid) {
        mParameters.clear();
        mApiMethodName = FUNCTION_ENDCHUNK;
        byte[] payload = null;
        Long muploadID = uploadid;
        mParameters.put(UPLOADID, muploadID);
        calculateAuth();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MicroHessianOutput mho = new MicroHessianOutput(bos);
        try {
            mho.startCall(mApiMethodName);
            HessianEncoder.writeHashtable(mParameters, mho);
            mho.completeCall();
        } catch (IOException e) {
            Log.v("Request Exception-request-getUploadEnd", "error" + e);
        }
        CloseUtils.close(bos);
        payload = bos.toByteArray();
        payload[1] = (byte) 1;
        // TODO we need to change this if we want to use a
        // baos
        payload[2] = (byte) 0;
        return payload;
     }


    /**
     * Calculates the expiry date based on the timeout.
     *  TODO: should have
     * instead an execute() method to call when performing the request. it would
     * set the request to active, calculates the expiry date, etc...
     */
    public void calculateExpiryDate() {
        if (mTimeout > 0) {
            mExpiryDate = System.currentTimeMillis() + mTimeout;
        }
    }
}
