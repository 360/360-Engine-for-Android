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

package com.vodafone360.people.datatypes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * BaseDataType encapsulating an ActivityItem retrieved from, or to be issued
 * to, Now + server
 */
public class ActivityItem extends BaseDataType {

    /*
     * Constant values for misc. flag items - see flag definitions.
     */
    public final static int ALREADY_READ = 0x01; // Applied to chat messages as well.

    public final static int TIMELINE_ITEM = 0x02; // Applied to chat messages as well.

    public final static int STATUS_ITEM = 0x04;

    //public final static int UNSENT_ITEM = 0x08; // Chat message was not sent.

    /*
     * Visibility flag values - see visibility type definition.
     */
    private final static int ORIGINATOR = 0x01;

    private final static int RECIPIENT = 0x02;

    private final static int ADDRESSBOOK = 0x04;

    private final static int BUSINESSCONTACTS = 0x08;

    private final static int KNOWUSER = 0x10;

    private final static int CONNECTED_FRIENDS = 0x20;

    /**
     * Tags associated with ActivityItem representing data items associated with
     * Activities returned from server.
     */
    private enum Tags {
        ACTIVITY_ID("activityid"),
        TIME("time"),
        TYPE("type"),
        URI("uri"),
        TITLE("title"),
        DESCRIPTION("description"),
        PREVIEW("preview"),
        PREVIEW_MIME("previewmime"),
        PREVIEW_URL("previewurl"),
        STORE("store"),
        FLAG_LIST("flaglist"),
        FLAG("flag"),
        PARENT_ACTIVITY("parentactivity"),
        HAS_CHILDREN("haschildren"),
        VISIBILITY("visibility"),
        CONTACT_LIST("contactlist"),
        CONTACT("contact"),
        MORE_INFO("moreinfo");

        private final String tag;

        /**
         * Constructor creating Tags item for specified String.
         * 
         * @param s String value for Tags item.
         */
        private Tags(String s) {
            tag = s;
        }

        /**
         * String value associated with Tags item.
         * 
         * @return String value for Tags item.
         */
        private String tag() {
            return tag;
        }

        /**
         * Find Tags item for specified String.
         * 
         * @param tag String value to find Tags item for
         * @return Tags item for specified String, null otherwise
         */
        private static Tags findTag(String tag) {
            for (Tags tags : Tags.values()) {
                if (tag.compareTo(tags.tag()) == 0) {
                    return tags;
                }
            }
            return null;
        }
    }

    /**
     * Activity types, a number of these types are not currently handled by
     * People client but will be as more activity events are supported. These
     * provide a mapping of all Activity types currently listed as returned by
     * Server.
     */
    public enum Type {
        CALL_DIALED("call_dialed"),
        CALL_RECEIVED("call_received"),
        CALL_MISSED("call_missed"),
        CONTACT_SENT_STATUS_UPDATE("contact_sent_status_update"),
        CONTACT_RECEIVED_STATUS_UPDATE("contact_received_status_update"), // TODO:
                                                                          // Only
                                                                          // called
                                                                          // from
                                                                          // tests!!
        CONTACT_JOINED("contact_joined"), // TODO: Only called from tests!!
        CONTACT_FRIEND_INVITATION_SENT("contact_friend_invitation_sent"), // TODO:
                                                                          // Only
                                                                          // called
                                                                          // from
                                                                          // tests!!
        CONTACT_FRIEND_INVITATION_RECEIVED("contact_friend_invitation_received"),
        CONTACT_NEW_FRIENDS("contact_new_friends"),
        CONTACT_WALL_POST_SENT("contact_wall_post_sent"),
        CONTACT_WALL_POST_RECEIVED("contact_wall_post_received"),
        CONTACT_PROFILE_EMAIL_UPDATED("contact_profile_email_updated"),
        CONTACT_PROFILE_PHONE_UPDATED("contact_profile_phone_updated"),
        CONTACT_PROFILE_ADDRESS_UPDATED("contact_profile_address_updated"),
        CONTACT_PROFILE_PICTURE_UPDATED("contact_profile_picture_updated"),
        STORE_APPLICATION_PURCHASED("store_application_purchased"),
        SN_ADDED("sn_added"),
        SN_ADDED_BY_FRIEND("sn_added_by_friend"),
        SN_WALL_POST_SENT("sn_wall_post_sent"),
        SN_WALL_POST_RECEIVED("sn_wall_post_received"),
        SN_MESSAGE_SENT("sn_message_sent"),
        SN_MESSAGE_RECEIVED("sn_message_received"),
        SN_PHOTOS_POSTED("sn_photos_posted"),
        SN_VIDEOS_POSTED("sn_videos_posted"),
        SN_STATUS_SENT("sn_status_sent"),
        SN_STATUS_RECEIVED("sn_status_received"),
        SN_CONTACT_PROFILE_EMAIL_UPDATED("sn_contact_profile_email_updated"),
        SN_CONTACT_PROFILE_PHONE_UPDATED("sn_contact_profile_phone_updated"),
        SN_CONTACT_PROFILE_ADDRESS_UPDATED("sn_contact_profile_address_updated"),
        MESSAGE_SMS_SENT("message_sms_sent"),
        MESSAGE_SMS_RECEIVED("message_sms_received"),
        MESSAGE_MMS_SENT("message_mms_sent"),
        MESSAGE_MMS_RECEIVED("message_mms_received"),
        MESSAGE_IM_CONVERSATION("message_im_conversation"),
        MESSAGE_EMAIL_SENT("message_email_sent"),
        MESSAGE_EMAIL_RECEIVED("message_email_received"),
        SHARE_ALBUM_SENT("share_album_sent"),
        SHARE_ALBUM_RECEIVED("share_album_received"),
        SHARE_PHOTO_SENT("share_photo_sent"),
        SHARE_PHOTO_RECEIVED("share_photo_received"),
        SHARE_VIDEO_SENT("share_video_sent"),
        SHARE_VIDEO_RECEIVED("share_video_received"),
        SHARE_PHOTO_COMMENT_SENT("share_photo_comment_sent"),
        SHARE_PHOTO_COMMENT_RECEIVED("share_photo_comment_received"),
        SHARE_PHOTO_MULTIPLE_SENT("share_photo_multiple_sent"),
        SHARE_PHOTO_MULTIPLE_RECEIVED("share_photo_multiple_received"),
        SHARE_VIDEO_MULTIPLE_SENT("share_video_multiple_sent"),
        SHARE_VIDEO_MULTIPLE_RECEIVED("share_video_multiple_received"),
        LOCATION_SENT("location_sent"),
        LOCATION_RECEIVED("location_received"),
        LOCATION_SHARED_PLACEMARK_CREATED("location_shared_placemark_created"),
        LOCATION_SHARED_PLACEMARK_RECEIVED("location_shared_placemark_received"),
        LOCATION_PLACEMARK_CREATED("location_placemark_created"),
        LOCATION_PLACEMARK_RECEIVED("location_placemark_received"),
        MUSIC_PURCHASED_SONG("music_purchased_song"),
        MUSIC_PURCHASED_ALBUM("music_purchased_album"),
        MUSIC_DOWNLOADED_SONG("music_downloaded_song"),
        MUSIC_DOWNLOADED_ALBUM("music_downloaded_album"),
        MUSIC_DOWNLOADED_PLAYLIST("music_downloaded_playlist"),
        MUSIC_RATED_SONG("music_rated_song"),
        MUSIC_RATED_ALBUM("music_rated_album"),
        MUSIC_RECOMMENDATION_SENT_TRACK("music_recommendation_sent_track"),
        MUSIC_RECOMMENDATION_SENT_ALBUM("music_recommendation_sent_album"),
        MUSIC_RECOMMENDATION_SENT_PLAYLIST("music_recommendation_sent_playlist"),
        MUSIC_RECOMMENDATION_SENT_TRACK_ANON("music_recommendation_sent_track_anon"),
        MUSIC_RECOMMENDATION_SENT_ALBUM_ANON("music_recommendation_sent_album_anon"),
        MUSIC_RECOMMENDATION_SENT_PLAYLIST_ANON("music_recommendation_sent_playlist_anon"),
        MUSIC_RECOMMENDATION_RECEIVED_TRACK("music_recommendation_received_track"),
        MUSIC_RECOMMENDATION_RECEIVED_ALBUM("music_recommendation_received_album"),
        MUSIC_RECOMMENDATION_RECEIVED_PLAYLIST("music_recommendation_received_playlist"),
        MUSIC_RECOMMENDATION_RECEIVED_TRACK_ANON("music_recommendation_received_track_anon"),
        MUSIC_RECOMMENDATION_RECEIVED_ALBUM_ANON("music_recommendation_received_album_anon"),
        MUSIC_RECOMMENDATION_RECEIVED_PLAYLIST_ANON("music_recommendation_received_playlist_anon"),
        ACTIVITY_EVENT_UNKNOWN("unknown_activity");

        private String mTypeCode;

        /**
         * Constructor creating type item for specified String.
         * 
         * @param s String value for type item.
         */
        private Type(String s) {
            mTypeCode = s;
        }

        /**
         * String value associated with type item.
         * 
         * @return String value for type item.
         */
        public String getTypeCode() {
            return mTypeCode;
        }

        /**
         * Find type item for specified String
         * 
         * @param tag String value to find type item for
         * @return type item for specified String, null otherwise
         */
        public static Type findType(String t) {
            for (Type types : Type.values()) {
                if (t.compareTo(types.getTypeCode()) == 0) {
                    return types;
                }
            }
            return null;
        }
    }

    /**
     * Flags identifying ActivityItems as Status or Timeline events and already
     * read
     */
    public enum Flag {
        /** User already read the activity before */
        ALREADY_READ("already_read"),
        /** This activity will be shown in the timeline view. */
        TIMELINE("timeline"),
        /** This activity will be shown in the status view. */
        STATUS("status");

        private String mFlagCode;

        /**
         * Constructor creating flag item for specified String.
         * 
         * @param s String value for flag item.
         */
        private Flag(String s) {
            mFlagCode = s;
        }

        /**
         * String value associated with flag item.
         * 
         * @return String value for flag item.
         */
        public String getFlagCode() {
            return mFlagCode;
        }

        /**
         * Find flag item for specified String
         * 
         * @param tag String value to find flag item for
         */
        private static Flag findFlag(String f) {
            for (Flag flags : Flag.values()) {
                if (f.compareTo(flags.getFlagCode()) == 0) {
                    return flags;
                }
            }
            return null;
        }
    }

    /**
     * Visibility flags for ActivityItem
     */
    public enum Visibility {
        /**
         * Do not extend visibility and just store the activity. This is assumed
         * default when no visibility is specified explicitly at all.
         */
        ORIGINATOR("originator"),
        /**
         * Send a 'received' activity to the recipient contact that is specified
         * on the contactlist / unknowncontacts.
         */
        RECIPIENT("recipient"),
        /**
         * Send a 'received' activity to all contacts in the user's address who
         * are interested in this activity type as indicated by their privacy
         * settings.
         */
        ADDRESS_BOOK("addressbook"),
        /**
         * Send a 'received' activity to all contacts in the user's address who
         * are interested in this activity type as indicated by their privacy
         * settings AND which are business contacts.
         */
        BUSINESS_CONTACTS("businesscontacts"),
        /**
         * Send a 'received' activity to all contacts in the user's address who
         * are interested in this activity type as indicated by their privacy
         * settings.
         */
        KNOW_USER("knowuser"),
        /**
         * Send a notification activity to all connected friends of user or
         * contact.
         */
        CONNECTED_FRIENDS("connected_friends");

        private String mVisibilityCode;

        /**
         * Constructor creating visibility item for specified String.
         * 
         * @param s String value for visibility item.
         */
        private Visibility(String s) {
            mVisibilityCode = s;
        }

        /**
         * String value associated with visibility item.
         * 
         * @return String value for visibility item.
         */
        public String getVisibilityCode() {
            return mVisibilityCode;
        }

        /**
         * Find visibility item for specified String
         * 
         * @param tag String value to find visibility item for
         * @return visibility item for specified String, null otherwise
         */
        private static Visibility findVisibility(String v) {
            for (Visibility vs : Visibility.values()) {
                if (v.compareTo(vs.getVisibilityCode()) == 0) {
                    return vs;
                }
            }
            return null;
        }
    }

    /** local id for db */
    public Long mLocalActivityId = null;

    /*
     * Unique identifier for the activity. This can be empty when setting a new
     * activity (the id is generated on the server side)
     */
    public Long mActivityId = null;

    /*
     * Timestamp representing the time of the activity. This may not be related
     * to creation/updated time.
     */
    public Long mTime = null;

    /** Defines the type of the activity. */
    public Type mType = null;

    /*
     * Defines an internal reference (if any) to the source of the activity. The
     * format for the uri is "module:identifier".Some examples of valid uri are:
     * contact:2737b322c9f6476ca152aa6cf3e5ac12 The activity is linked to some
     * changes on a contact identified by id=2737b322c9f6476ca152aa6cf3e5ac12.
     * file:virtual/flickr/2590004126 The activity is linked to some actions on
     * a file identified by id=virtual/flickr/2590004126.
     * message:9efd255359074dd9bd04cc1c8c4743e5 The activity is linked to a
     * message identified by id=9efd255359074dd9bd04cc1c8c4743e5
     */
    public String mUri = null;

    /** Short text description of the activity. */
    public String mTitle = null;

    /** Long text description of the activity. */
    public String mDescription = null;

    /**
     * Defines a binary preview for the activity. The preview can be a small
     * thumbnail of the activity. The type of the binary data is defined into
     * the previewmime field.
     */
    public ByteBuffer mPreview = null;

    /** Defines the MIME type of the preview binary data. */
    public String mPreviewMime = null;

    /**
     * Defines an http url that the client can use to retrieve preview binary
     * data. Can be used to embed the url into an IMG HTML tag.
     */
    public String mPreviewUrl = null;

    /**
     * Name of the store type for this message. This field contains information
     * about the originator network (local or external community activity). By
     * default, should be set to local
     */
    public String mStore = null;

    /** Miscellaneous flags. */
    public List<Flag> mFlagList = null;

    public Integer mActivityFlags = 0;

    /**
     * The parent activity for 'grouped' or aggregated activities. This must be
     * empty for normal activities that can be retrieved normally. Normally, a
     * GetActivities without filter will not yield any 'grouped' or 'child'
     * activities. To get activities that have a mParentActivity set, the
     * 'children' filter must be used with a value of the parent Activity's id.
     */
    public Long mParentActivity = null;

    /**
     * Indicates wether this activity 'groups' several child activities. When
     * set, there must be child activities set that refer the main activity.
     * Normally, a GetActivities without filter will not yield any 'grouped' or
     * 'child' activities. To get activities that have a parentactivity set, the
     * 'children' filter must be used with a value of the parent Activity's id.
     */
    public Boolean mHasChildren = null;

    /** Defines the visibility of the activity. */
    public List<Visibility> mVisibility = null;

    public Integer mVisibilityFlags = 0;

    /**
     * Defines the contact information of the counter-parties in the activity.
     * This field is not mandatory, because some activity types are not related
     * to contacts, but required if known..
     */
    public List<ActivityContact> mContactList = null;

    /** {@inheritDoc} */
    @Override
    public String name() {
        return "ActivityItem";
    }

    /**
     * Create ActivityItem from HashTable generated by Hessian-decoder
     * 
     * @param hash Hashtable representing ActivityItem
     * @return ActivityItem created from Hashtable
     */
    static public ActivityItem createFromHashtable(Hashtable<String, Object> hash) {
        ActivityItem act = new ActivityItem();
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = Tags.findTag(key);
            act.setValue(tag, value);
        }
        return act;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param val Value associated with the tag
     */
    private void setValue(Tags tag, Object value) {
        if (tag != null) {
            switch (tag) {
                case ACTIVITY_ID:
                    mActivityId = (Long)value;
                    break;

                case CONTACT:
                    // Do nothing.
                    break;

                case CONTACT_LIST:
                    mContactList = new ArrayList<ActivityContact>();
                    @SuppressWarnings("unchecked")
                    Vector<Hashtable<String, Object>> v = (Vector<Hashtable<String, Object>>)value;

                    for (Hashtable<String, Object> hash : v) {
                        mContactList.add(ActivityContact.createFromHashTable(hash));
                    }
                    break;

                case DESCRIPTION:
                    mDescription = (String)value;
                    break;

                case FLAG_LIST:
                    mFlagList = new ArrayList<Flag>();
                    if (value instanceof Vector<?>) {
                        @SuppressWarnings("unchecked")
                        Vector<String> flags = (Vector<String>)value;
                        int flagCount = flags.size();
                        for (int i = 0; i < flagCount; i++) {
                            updateMiscFlags(Flag.findFlag((String) flags.get(i)));
                        }
                    }
                    break;

                case HAS_CHILDREN:
                    mHasChildren = (Boolean)value;
                    break;

                case MORE_INFO:
                    // Not currently handled.
                    break;

                case PARENT_ACTIVITY:
                    mParentActivity = (Long)value;
                    break;

                case PREVIEW:
                    byte[] bytes = (byte[])value;
                    mPreview = ByteBuffer.allocate(bytes.length);
                    mPreview.put(bytes);
                    break;

                case PREVIEW_MIME:
                    mPreviewMime = (String)value;
                    break;

                case PREVIEW_URL:
                    mPreviewUrl = (String)value;
                    break;

                case STORE:
                    mStore = (String)value;
                    break;

                case TIME:
                    mTime = (Long)value * 1000; // mTime on server is given in
                                                // seconds.
                    break;

                case TITLE:
                    mTitle = (String)value;
                    break;

                case TYPE:
                    mType = Type.findType((String) value);
                    break;

                case URI:
                    mUri = (String)value;
                    break;

                case VISIBILITY:
                    mVisibility.add(Visibility.findVisibility((String) value));
                    updateVisibilityFlags(Visibility.findVisibility((String) value));
                    break;
                    
                default:
                    // Do nothing.
                    break;
            }
        }
    }

    /**
     * Set values for mActivityFlags flag. This actually stores whether the
     * ActivityItem is a Status or Timeline event, and whether item is unread.
     * 
     * @param flagValue Value to add to mActivityFlags item.
     */
    private void updateMiscFlags(Flag flagValue) {
        switch (flagValue) {
            case ALREADY_READ:
                mActivityFlags = mActivityFlags | ALREADY_READ;
                break;

            case TIMELINE:
                mActivityFlags = mActivityFlags | TIMELINE_ITEM;
                break;

            case STATUS:
                mActivityFlags = mActivityFlags | STATUS_ITEM;
                break;

            default:
                // Do nothing.
                break;
        }
    }

    /**
     * Update visibility flag. This can take on a number of values as defined by
     * the online API documentation associated with the getactivities API.
     * 
     * @param flagValue visibility value to add to mVisibilityFlags.
     */
    private void updateVisibilityFlags(Visibility flagValue) {
        switch (flagValue) {
            case ORIGINATOR:
                mVisibilityFlags = mVisibilityFlags | ORIGINATOR;
                break;

            case RECIPIENT:
                mVisibilityFlags = mVisibilityFlags | RECIPIENT;
                break;

            case ADDRESS_BOOK:
                mVisibilityFlags = mVisibilityFlags | ADDRESSBOOK;
                break;

            case BUSINESS_CONTACTS:
                mVisibilityFlags = mVisibilityFlags | BUSINESSCONTACTS;
                break;

            case KNOW_USER:
                mVisibilityFlags = mVisibilityFlags | KNOWUSER;
                break;

            case CONNECTED_FRIENDS:
                mVisibilityFlags = mVisibilityFlags | CONNECTED_FRIENDS;
                break;

            default:
                // Do nothing.
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append(name());
        ret.append("\n\tActivity Id = " + mActivityId);
        ret.append("\n\tTime = " + mTime);
        if (mType != null) {
            ret.append("\n\tType = " + mType.getTypeCode());
        }
        ret.append("\n\tUri = " + mUri);
        ret.append("\n\tTitle = " + mTitle);
        ret.append("\n\tDescription = " + mDescription);
        if (mPreview != null) {
            ret.append("\n\tPreview = " + String.valueOf(mPreview));
        }
        ret.append("\n\tPreview Mime =" + mPreviewMime);
        ret.append("\n\tPreview URL = " + mPreviewUrl);
        ret.append("\n\tStore = " + mStore);
        if (mFlagList != null) {
            ret.append("\n\tFlag list: [");
            for (int i = 0; i < mFlagList.size(); i++) {
                ret.append(mFlagList.get(i).getFlagCode());
                if (i < mFlagList.size() - 1) {
                    ret.append(", ");
                }
            }
            ret.append("]");
        }
        ret.append("\n\tParent activity = " + mParentActivity);
        ret.append("\n\tHas children = " + mHasChildren);
        if (mVisibility != null) {
            ret.append("\n\tVisibility: [");
            for (int i = 0; i < mVisibility.size(); i++) {
                ret.append(mVisibility.get(i).getVisibilityCode());
                if (i < mVisibility.size() - 1) {
                    ret.append(", ");
                }
            }
            ret.append("]");
        }

        if (mContactList != null) {
            ret.append("\n\tContact list: [");
            for (int i = 0; i < mContactList.size(); i++) {
                ret.append(mContactList.get(i).toString());
                if (i < mContactList.size() - 1) {
                    ret.append(", ");
                }
            }
            ret.append("]");
        }
        return ret.toString();
    }
}
