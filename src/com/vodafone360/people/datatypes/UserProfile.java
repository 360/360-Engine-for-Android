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

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.os.Parcel;
import android.os.Parcelable;

import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType encapsulating user profile information retrieved from People
 * server.
 */
public class UserProfile extends BaseDataType implements Parcelable {

    /**
     * Tags for fields associated with UserProfile items.
     */
    private enum Tags {
        USER_ID("userid"),
        PROFILE_PATH("profilepath"),
        CONTACT_ID("contactid"),
        SOURCES("sources"),
        GENDER("gender"),
        DETAIL_LIST("detaillist"),
        DETAIL("detail"),
        ME("me"),
        FRIEND_OF_FRIEND_LIST("foflist"),
        ABOUT_ME("aboutme"),
        FRIEND("friend"),
        UPDATED("updated");

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
    }

    public Long userID;

    public String profilePath;

    public Long contactID;

    public List<String> sources;

    public Integer gender;

    public final List<ContactDetail> details = new ArrayList<ContactDetail>();

    private Boolean isMe;

    private List<Long> fofList;

    public String aboutMe;

    protected Boolean friendOfMine;

    public Long updated;

    /**
     * Find Tags item for specified String.
     * 
     * @param tag String value to search for in Tag
     * @return Tags item for specified String, NULL otherwise.
     */
    private Tags findTag(String tag) {
        for (Tags tags : Tags.values()) {
            if (tag.compareTo(tags.tag()) == 0) {
                return tags;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return USER_PROFILE_DATA_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        Date time = null;
        if (updated != null) {
            time = new Date(updated * 1000);
        } else {
            time = new Date(0);
        }
        final StringBuffer sb = new StringBuffer("--\nUser Profile data:");
        sb.append("\n\tUser ID: "); sb.append(userID);
        sb.append("\n\tProfile Path: "); sb.append(profilePath);
        sb.append("\n\tContact ID: "); sb.append(contactID);
        sb.append("\n\tGender: "); sb.append(gender);
        sb.append("\n\tIs Me: "); sb.append(isMe); 
        sb.append("\n\tAbout me: "); sb.append(aboutMe);
        sb.append("\n\tFriend of mine: "); sb.append(friendOfMine);
        sb.append("\n\tupdated: "); sb.append(updated); 
        sb.append(" Date: "); sb.append(time.toGMTString()); 
        sb.append("\n");
        if (sources != null) {
            sb.append("Sources ("); sb.append(sources.size()); 
            sb.append("): ");
            for (int i = 0; i < sources.size(); i++) {
                sb.append(sources.get(i)); sb.append(",");
            }
        }

        if (fofList != null) {
            sb.append("Group id list = [");
            for (int i = 0; i < fofList.size(); i++) {
                sb.append(fofList.get(i));
                if (i < fofList.size() - 1) {
                    sb.append(",");
                }
            }
        }

        sb.append("Contact details ("); sb.append(details.size()); sb.append("):\n");
        for (int i = 0; i < details.size(); i++) {
            sb.append(details.get(i).toString() + "\n");
        }
        sb.append("\n--------------------------------------------------");
        return sb.toString();
    }

    /**
     * Create UserProfile from Hashtable.
     * 
     * @param hash Hashtable containing User Profile information.
     * @return UserProfile generated from Hashtable.
     */
    public static UserProfile createFromHashtable(Hashtable<String, Object> hash) {
        UserProfile profile = new UserProfile();
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = profile.findTag(key);
            profile.setValue(tag, value);
        }

        return profile;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param value Value associated with the tag
     */
    private void setValue(Tags tag, Object value) {
        switch (tag) {
            case USER_ID:
                userID = (Long)value;
                break;

            case PROFILE_PATH:
                profilePath = (String)value.toString();
                break;

            case CONTACT_ID:
                contactID = (Long)value;
                break;

            case SOURCES:
                if (sources == null) {
                    sources = new ArrayList<String>();
                }
                @SuppressWarnings("unchecked")
                Vector<String> vals = (Vector<String>)value;
                for (String source : vals) {
                    sources.add(source);
                }
                break;

            case GENDER:
                if (gender == null) {
                    gender = (Integer)value;
                }
                break;

            case DETAIL_LIST:
                @SuppressWarnings("unchecked")
                Vector<Hashtable<String, Object>> detailsList = (Vector<Hashtable<String, Object>>)value;

                for (Hashtable<String, Object> detailHashtable : detailsList) {
                    
                    try {
                        
                        // let's try to create the ContactDetail
                        // if failing, the detail will just be skipped
                        final ContactDetail detail = new ContactDetail();
                        detail.createFromHashtable(detailHashtable);
                        details.add(detail);
                        
                    } catch (Exception e) {
                        
                        LogUtils.logE("UserProfile.setValue(), the following error occured while adding a detail: "+e);
                    }
                }
                break;

            case DETAIL:
                break;

            case ME:
                isMe = (Boolean)value;
                break;

            case FRIEND_OF_FRIEND_LIST:
                @SuppressWarnings("unchecked")
                Vector<Long> gL = (Vector<Long>)value;
                if (fofList == null) {
                    fofList = new ArrayList<Long>();
                }
                for (Long l : gL) {
                    fofList.add(l);
                }
                break;

            case ABOUT_ME:
                aboutMe = (String)value;
                break;

            case FRIEND:
                friendOfMine = (Boolean)value;
                break;

            case UPDATED:
                updated = (Long)value;
                break;

            default:
                LogUtils.logW("setValue: Unknown tag - " + tag + "[" + value + "]");
        }
    }

    /**
     * Enumeration of items written to UserProfile Parcel.
     */
    private enum MemberData {
        USERID,
        PROFILEPATH,
        CONTACTID,
        SOURCES,
        GENDER,
        ISME,
        FOFLIST,
        ABOUTME,
        FRIENDOFMINE,
        UPDATED;
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        boolean[] validDataList = new boolean[MemberData.values().length];
        int validDataPos = dest.dataPosition();
        dest.writeBooleanArray(validDataList); // Placeholder for real array.

        if (userID != null) {
            validDataList[MemberData.USERID.ordinal()] = true;
            dest.writeLong(userID);
        }
        if (profilePath != null) {
            validDataList[MemberData.PROFILEPATH.ordinal()] = true;
            dest.writeString(profilePath);
        }
        if (contactID != null) {
            validDataList[MemberData.CONTACTID.ordinal()] = true;
            dest.writeLong(contactID);
        }
        if (sources != null && sources.size() > 0) {
            validDataList[MemberData.SOURCES.ordinal()] = true;
            dest.writeStringList(sources);
        }
        if (gender != null) {
            validDataList[MemberData.GENDER.ordinal()] = true;
            dest.writeInt(gender);
        }
        if (isMe != null) {
            validDataList[MemberData.ISME.ordinal()] = true;
            dest.writeByte((byte)(isMe ? 1 : 0));
        }
        if (fofList != null && fofList.size() > 0) {
            validDataList[MemberData.FOFLIST.ordinal()] = true;
            long[] fofListArray = new long[fofList.size()];
            for (int i = 0; i < fofList.size(); i++) {
                fofListArray[i] = fofList.get(i);
            }
            dest.writeLongArray(fofListArray);
        }
        if (aboutMe != null) {
            validDataList[MemberData.ABOUTME.ordinal()] = true;
            dest.writeString(aboutMe);
        }
        if (friendOfMine != null) {
            validDataList[MemberData.FRIENDOFMINE.ordinal()] = true;
            dest.writeByte((byte)(friendOfMine ? 1 : 0));
        }
        if (updated != null) {
            validDataList[MemberData.UPDATED.ordinal()] = true;
            dest.writeLong(updated);
        }

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // Real array.
        dest.setDataPosition(currentPos);

        dest.writeInt(details.size());
        for (ContactDetail detail : details) {
            detail.writeToParcel(dest, 0);
        }
    }
}
