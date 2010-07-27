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

package com.vodafone360.people.service.utils.hessian;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import com.caucho.hessian.micro.MicroHessianInput;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetailDeletion;
import com.vodafone360.people.datatypes.ContactListResponse;
import com.vodafone360.people.datatypes.Conversation;
import com.vodafone360.people.datatypes.ExternalResponseObject;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.ItemList;
import com.vodafone360.people.datatypes.PresenceList;
import com.vodafone360.people.datatypes.PublicKeyDetails;
import com.vodafone360.people.datatypes.PushAvailabilityEvent;
import com.vodafone360.people.datatypes.PushChatConversationEvent;
import com.vodafone360.people.datatypes.PushChatMessageEvent;
import com.vodafone360.people.datatypes.PushClosedConversationEvent;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SimpleText;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.datatypes.UserProfile;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.rpg.PushMessageTypes;
import com.vodafone360.people.service.io.rpg.RpgPushMessage;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.datatypes.AddContentResult;
import com.vodafone360.people.datatypes.AlbumList;
import com.vodafone360.people.datatypes.ListEntityKeyResultShareAlbums;
import com.vodafone360.people.datatypes.ResultAddAlbums;

import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SharePhotoResult;

/**
 * Hessian decoding . TODO: Currently casting every response to a Map, losing
 * for example push events "c0" which only contains a string. This may need a
 * fix.
 */
public class HessianDecoder {

    private static final String KEY_ACTIVITY_LIST = "activitylist";

    private static final String KEY_AVAILABLE_IDENTITY_LIST = "availableidentitylist";

    private static final String KEY_CONTACT_ID_LIST = "contactidlist";

    private static final String KEY_CONTACT_LIST = "contactlist";

    private static final String KEY_IDENTITY_LIST = "identitylist";

    private static final String KEY_SESSION = "session";

    private static final String KEY_USER_PROFILE = "userprofile";

    private static final String KEY_USER_PROFILE_LIST = "userprofilelist";

    /**
     * The MicroHessianInput is here declared as member and will be reused
     * instead of making new instances on every need
     */
    private MicroHessianInput mMicroHessianInput = new MicroHessianInput();

    /**
     * Parse Hessian encoded byte array placing parsed contents into List.
     * 
     * @param data byte array containing Hessian encoded data
     * @param type Event type
     * @return List of BaseDataType items parsed from Hessian data
     * @throws IOException
     */
    public List<BaseDataType> decodeHessianByteArray(byte[] data, Request.Type type,
            boolean isZipped) throws IOException {
        InputStream is = null;
        InputStream bis = null;

        if (isZipped == true) {
            LogUtils.logV("HessianDecoder.decodeHessianByteArray() Handle zipped data");
            bis = new ByteArrayInputStream(data);
            is = new GZIPInputStream(bis, data.length);

        } else {
            LogUtils.logV("HessianDecoder.decodeHessianByteArray() Handle non-zipped data");
            is = new ByteArrayInputStream(data);
        }

        List<BaseDataType> mBaseDataTypeList = null;
        mMicroHessianInput.init(is);

        LogUtils.logV("HessianDecoder.decodeHessianByteArray() Begin Hessian decode");
        try {
            mBaseDataTypeList = decodeResponse(is, type);
        } catch (IOException e) {
            LogUtils.logE("HessianDecoder.decodeHessianByteArray() "
                    + "IOException during decodeResponse", e);
        }

        CloseUtils.close(bis);
        CloseUtils.close(is);
        return mBaseDataTypeList;
    }

    @SuppressWarnings("unchecked")
    public Hashtable<String, Object> decodeHessianByteArrayToHashtable(byte[] data)
            throws IOException {
        InputStream is = new ByteArrayInputStream(data);
        mMicroHessianInput.init(is);

        Object obj = null;
        obj = mMicroHessianInput.decodeTag();

        if (obj instanceof Hashtable) {
            return (Hashtable<String, Object>)obj;
        } else {
            return null;
        }
    }

    /**
     * TODO: we cast every response to a Map, losing e.g. push event "c0" which
     * only contains a string - to fix
     * 
     * @param is
     * @param type
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private List<BaseDataType> decodeResponse(InputStream is, Request.Type type) throws IOException {
        boolean usesReplyTag = false;
        List<BaseDataType> mResultList = new ArrayList<BaseDataType>();
        mMicroHessianInput.init(is);

        // skip start
        int tag = is.read(); // initial map tag or fail

        if (tag == 'r') { // reply / response
            is.read(); // read major and minor
            is.read();

            tag = is.read(); // read next tag
            usesReplyTag = true;
        }

        if (tag == -1) {
            return null;
        }

        // check for fail
        // read reason string and throw exception
        if (tag == 'f') {
            ServerError zybErr = 
                new ServerError(mMicroHessianInput.readFault().errString());
            mResultList.add(zybErr);
            return mResultList;
        }

        // handle external response
        // this is not wrapped up in a hashtable
        if (type == Request.Type.EXTERNAL_RPG_RESPONSE) {
            LogUtils.logV("HessianDecoder.decodeResponse() EXTERNAL_RPG_RESPONSE");
            if (tag != 'I') {
                LogUtils.logE("HessianDecoder.decodeResponse() "
                        + "tag!='I' Unexpected Hessian type:" + tag);
            }

            parseExternalResponse(mResultList, is, tag);
            return mResultList;
        }

        // internal response: should contain a Map type - i.e. Hashtable
        if (tag != 'M') {
            LogUtils
                    .logE("HessianDecoder.decodeResponse() tag!='M' Unexpected Hessian type:" + tag);
            throw new IOException("Unexpected Hessian type");

        } else if ((type != Request.Type.COMMON) && (type != Request.Type.SIGN_IN)) {
            // get initial hash table
            // TODO: we cast every response to a Map, losing e.g. push event
            // "c0" which only contains a string - to fix
            Hashtable<String, Object> hash = (Hashtable<String, Object>)mMicroHessianInput
                    .decodeType(tag);
            decodeResponseByRequestType(mResultList, hash, type);
        } else { // if we have a common request
            Hashtable<String, Object> map = (Hashtable<String, Object>)mMicroHessianInput
                    .readHashMap(tag);

            if (null == map) {
                return null;
            }

            if (map.containsKey(KEY_SESSION)) {
                AuthSessionHolder auth = new AuthSessionHolder();
                Hashtable<String, Object> authHash = (Hashtable<String, Object>)map
                        .get(KEY_SESSION);

                mResultList.add(auth.createFromHashtable(authHash));
            } else if (map.containsKey(KEY_CONTACT_LIST)) {
                // contact list
                getContacts(mResultList, ((Vector<?>)map.get(KEY_CONTACT_LIST)));
            } else if (map.containsKey(KEY_USER_PROFILE_LIST)) {
                Vector<Hashtable<String, Object>> upVect = (Vector<Hashtable<String, Object>>)map
                        .get(KEY_USER_PROFILE_LIST);

                for (Hashtable<String, Object> obj : upVect) {
                    mResultList.add(UserProfile.createFromHashtable(obj));
                }
            } else if (map.containsKey(KEY_USER_PROFILE)) {
                Hashtable<String, Object> userProfileHash = (Hashtable<String, Object>)map
                        .get(KEY_USER_PROFILE);
                mResultList.add(UserProfile.createFromHashtable(userProfileHash));
            } else if ((map.containsKey(KEY_IDENTITY_LIST))
                    || (map.containsKey(KEY_AVAILABLE_IDENTITY_LIST))) {
            	int identityType = 0;
                Vector<Hashtable<String, Object>> idcap = null;
                if (map.containsKey(KEY_IDENTITY_LIST)) {
                    idcap = (Vector<Hashtable<String, Object>>)map.get(KEY_IDENTITY_LIST);
                    identityType = BaseDataType.MY_IDENTITY_DATA_TYPE;
                } else {
                    idcap = (Vector<Hashtable<String, Object>>)map.get(KEY_AVAILABLE_IDENTITY_LIST);
                    identityType = BaseDataType.AVAILABLE_IDENTITY_DATA_TYPE;
                }

                for (Hashtable<String, Object> obj : idcap) {
                    Identity id = new Identity(identityType);
                    mResultList.add(id.createFromHashtable(obj));
                }

            } else if (map.containsKey(KEY_ACTIVITY_LIST)) {
                Vector<Hashtable<String, Object>> activityList = (Vector<Hashtable<String, Object>>)map
                        .get(KEY_ACTIVITY_LIST);

                for (Hashtable<String, Object> obj : activityList) {
                    mResultList.add(ActivityItem.createFromHashtable(obj));
                }
            }
        }

        if (usesReplyTag) {
            is.read(); // read the last 'z'
        }

        return mResultList;
    }

    private void parseExternalResponse(List<BaseDataType> clist, InputStream is, int tag)
            throws IOException {
        mMicroHessianInput.init(is);
        ExternalResponseObject resp = new ExternalResponseObject();
        // we already read the 'I' in the decodeResponse()-method
        // now we read and check the response code
        if (mMicroHessianInput.readInt(tag) != 200) {
            return;
        }

        try {
            resp.mMimeType = mMicroHessianInput.readString();
        } catch (IOException ioe) {
            LogUtils.logE("Failed to parse hessian string.");
            return;
        }

        // read data - could be gzipped
        try {
            resp.mBody = mMicroHessianInput.readBytes();
        } catch (IOException ioe) {
            LogUtils.logE("Failed to read bytes.");
            return;
        }

        LogUtils.logI("HessianDecoder.parseExternalResponse()"
                + " Parsed external object with length: " + resp.mBody.length);
        clist.add(resp);
    }

    private void decodeResponseByRequestType(List<BaseDataType> clist,
            Hashtable<String, Object> hash, Request.Type type) {
        switch (type) {
            case CONTACT_CHANGES_OR_UPDATES:

                // create ContactChanges
                ContactChanges contChanges = new ContactChanges();
                contChanges = contChanges.createFromHashtable(hash);
                clist.add(contChanges);
                break;

            case ADD_CONTACT:
            case SIGN_UP:
                clist.add(Contact.createFromHashtable(hash));
                break;
            case RETRIEVE_PUBLIC_KEY:
                // AA define new object type
                clist.add(PublicKeyDetails.createFromHashtable(hash));
                break;

            case FRIENDSHIP_REQUEST:
                break;

            case CONTACT_DELETE:
                ContactListResponse cresp = new ContactListResponse();
                cresp.createFromHashTable(hash);
                // add ids
                @SuppressWarnings("unchecked")
                Vector<Long> contactIds = (Vector<Long>)hash.get(KEY_CONTACT_ID_LIST);
                if (contactIds != null) {
                    for (Long cid : contactIds) {
                        cresp.mContactIdList.add((cid).intValue());
                    }
                }
                clist.add(cresp);
                break;
            case CONTACT_DETAIL_DELETE:
                ContactDetailDeletion cdel = new ContactDetailDeletion();
                clist.add(cdel.createFromHashtable(hash));
                break;

            case USER_INVITATION:
                // Not currently supported.
                break;

            case CONTACT_GROUP_RELATION_LIST:
                ItemList groupRelationList = new ItemList(ItemList.Type.contact_group_relation);
                groupRelationList.populateFromHashtable(hash);
                clist.add(groupRelationList);
                break;

            case CONTACT_GROUP_RELATIONS:
                ItemList groupRelationsList = new ItemList(ItemList.Type.contact_group_relations);
                groupRelationsList.populateFromHashtable(hash);
                clist.add(groupRelationsList);
                break;

            case GROUP_LIST:
                ItemList zyblist = new ItemList(ItemList.Type.group_privacy);
                zyblist.populateFromHashtable(hash);
                clist.add(zyblist);
                break;

            case ITEM_LIST_OF_LONGS:
                ItemList listOfLongs = new ItemList(ItemList.Type.long_value);
                listOfLongs.populateFromHashtable(hash);
                clist.add(listOfLongs);
                break;

            case STATUS_LIST:
                ItemList zybstatlist = new ItemList(ItemList.Type.status_msg);
                zybstatlist.populateFromHashtable(hash);
                clist.add(zybstatlist);
                break;

            case TEXT_RESPONSE_ONLY:

                Object val = hash.get("result");
                if (val != null && val instanceof String) {
                    SimpleText txt = new SimpleText();
                    txt.addText((String)val);
                    clist.add(txt);
                }
                break;

            case EXPECTING_STATUS_ONLY:
                StatusMsg statMsg = new StatusMsg();
                clist.add(statMsg.createFromHashtable(hash));

                break;

            case STATUS:
                StatusMsg s = new StatusMsg();
                s.mStatus = true;
                clist.add(s);
                break;

            case PRESENCE_LIST:
                PresenceList mPresenceList = new PresenceList();
                mPresenceList.createFromHashtable(hash);
                clist.add(mPresenceList);
                break;

            case PUSH_MSG:
                // parse content of RPG Push msg
                parsePushMessage(clist, hash);
                break;
            case CREATE_CONVERSATION:
                Conversation mConversation = new Conversation();
                mConversation.createFromHashtable(hash);
                clist.add(mConversation);
                break;
            case  UPLOAD_PHOTO:
            	AddContentResult addcontentresult = new AddContentResult();
            	addcontentresult.createFromHashtable(hash);
            	clist.add(addcontentresult);
                break;
            case GET_DEFAULT_ALBUM360:
            	AlbumList getalbum = new AlbumList();
            	getalbum.createFromHashtable(hash);
            	clist.add(getalbum);
            	break;
            case SHARE_PHOTO_WITH_ALBUM:
            	SharePhotoResult result = new SharePhotoResult();
            	result.createFromHashtable(hash);
            	clist.add(result);
            	break;
            case ADD_ALBUMS:
                ResultAddAlbums getalbumlist = new ResultAddAlbums();
                getalbumlist.createFromHashtable(hash);
                clist.add(getalbumlist);
                break;
            case SHARE_ALBUMS:
               ListEntityKeyResultShareAlbums obj =
                               new  ListEntityKeyResultShareAlbums();
               obj.createFromHashtable(hash);
               clist.add(obj);
               break;

            default:
                LogUtils.logE("HessianDecoder."
                                + "decodeResponseByRequestType()"
                                	+ " Unhandled type["
                        + type.name() + "]");
        }
    }

    private void getContacts(List<BaseDataType> clist, Vector<?> cont) {
        for (Object obj : cont) {
            @SuppressWarnings("unchecked")
            Hashtable<String, Object> hash = (Hashtable<String, Object>)obj;
            clist.add(Contact.createFromHashtable(hash));
        }
    }

    private void parsePushMessage(List<BaseDataType> list, Hashtable<String, Object> hash) {
        RpgPushMessage push = RpgPushMessage.createFromHashtable(hash);
        parsePushPayload(push, list);
    }

    private void parsePushPayload(RpgPushMessage msg, List<BaseDataType> list) {
        // convert push msg type string to PushMsgType
        PushMessageTypes type = msg.mType;
        EngineId engineId = EngineId.UNDEFINED;
        if (type != null) {
            switch (type) {
                case CHAT_MESSAGE:
                    LogUtils.logV("Parse incomming chat_message");
                    engineId = EngineId.PRESENCE_ENGINE;
                    list.add(new PushChatMessageEvent(msg, engineId));
                    return;
                case AVAILABILITY_STATE_CHANGE:
                    LogUtils.logV("Parse availability state change:");
                    engineId = EngineId.PRESENCE_ENGINE;
                    list.add(PushAvailabilityEvent.createPushEvent(msg, engineId));
                    return;
                case START_CONVERSATION:
                    LogUtils.logV("Parse new conversation event:");
                    engineId = EngineId.PRESENCE_ENGINE;
                    list.add(new PushChatConversationEvent(msg, engineId));
                    return;
                case CLOSED_CONVERSATION:
                    LogUtils.logV("Parse closed conversation event:");
                    engineId = EngineId.PRESENCE_ENGINE;
                    list.add(new PushClosedConversationEvent(msg, engineId));
                    return;
                case CONVERSATION_END:
                    break;
                // API events create push message type
                case PROFILE_CHANGE:
                    engineId = EngineId.SYNCME_ENGINE;
                    break;
                case CONTACTS_CHANGE:
                    engineId = EngineId.CONTACT_SYNC_ENGINE;
                    break;
                case TIMELINE_ACTIVITY_CHANGE:
                case STATUS_ACTIVITY_CHANGE:
                    engineId = EngineId.ACTIVITIES_ENGINE;
                    break;
                case FRIENDSHIP_REQUEST_RECEIVED:
                    break;
                case IDENTITY_CHANGE:
                    engineId = EngineId.IDENTITIES_ENGINE;
                    break;
                case IDENTITY_NETWORK_CHANGE:
                	engineId = EngineId.IDENTITIES_ENGINE;
                    break;
                case SYSTEM_NOTIFICATION:
                    LogUtils.logE("SYSTEM_NOTIFICATION push msg:" + msg.mHash);
                    list.add(SystemNotification.createFromHashtable(msg.mHash, engineId));
                    return;
                default:

            }

            list.add(PushEvent.createPushEvent(msg, engineId));
        }
    }
}
