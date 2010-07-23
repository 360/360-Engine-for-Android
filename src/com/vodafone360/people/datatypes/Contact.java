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

import com.vodafone360.people.database.persistenceHelper.Persistable;
import com.vodafone360.people.database.persistenceHelper.Persistable.Entity;
import com.vodafone360.people.database.persistenceHelper.Persistable.Table;
import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType encapsulating a Contact retrieved from, or sent to, Now+ server.
 * <p>
 * Represents a contact in the people client database with all the associated
 * information. Information stored in a contact is fetched from multiple
 * sub-tables. For example a contact is made up of multiple details which are
 * fetched from the contact details table.
 * <p>
 * The lighter weight {@link ContactSummary} object should be used if only the
 * contact name and essential details are needed.
 */
@Entity
@Table(name = "Contacts")
public class Contact extends BaseDataType implements Parcelable, Persistable {

    /**
     * Tags for fields associated with Contact items.
     */
    private enum Tags {
        CONTACT_ID("contactid"),
        GENDER("gender"),
        DELETED("deleted"),
        USER_ID("userid"),
        ABOUT_ME("aboutme"),
        UPDATED("updated"),
        SOURCES("sources"),
        DETAIL_LIST("detaillist"),
        DETAIL("detail"),
        GROUP_LIST("groupidlist"),
        FRIEND("friend"),
        PROFILE_PATH("profilepath"), // only for user profile
        SYNCTOPHONE("synctophone"); // used for 'Phonebook' group

        private final String tag;

        /**
         * Constructor for Tags item.
         * 
         * @param s String value associated with Tag.
         */
        private Tags(String s) {
            tag = s;
        }

        /**
         * String value associated with Tags item.
         * 
         * @return String value associated with Tags item.
         */
        private String tag() {
            return tag;
        }

    }

    /**
     * Primary key in the database
     */
    @Id
    @Column(name = "LocalId")
    public Long localContactID = null;

    /**
     * Contains the about me string provided by the server
     */
    @Column(name = "AboutMe")
    public String aboutMe = null;

    /**
     * The server ID (or null if the contact has not yet been synchronised)
     */
    @Column(name = "ServerId")
    public Long contactID = null;

    /**
     * The user ID if the contact has been sychronised with the server and the
     * contact is associated with a user
     */
    @Column(name = "UserId")
    public Long userID = null;

    /**
     * A list of sources which has been fetched from the sources sub-table
     */
    public List<String> sources = null;

    /**
     * The timestamp when the contact was last updated on the server
     */
    @Column(name = "Updated")
    public Long updated = null;

    /**
     * The path of the contact thumbnail/avatar on the server
     */
    public String profilePath = null; // only for user profile

    /**
     * The gender of the contact received from the server
     */
    @Column(name = "Gender")
    public Integer gender = null; // only for user profile

    /**
     * Contains true if the contacts is marked as a friend
     */
    @Column(name = "Friend")
    public Boolean friendOfMine = null;

    /**
     * Contains true if the contact has been deleted on the server
     */
    public Boolean deleted = null;

    /**
     * Internal value which is used to store the primary key of the contact
     * stored in the native Android addressbook.
     */
    @Column(name = "NativeContactId")
    public Integer nativeContactId = null;

    /**
     * A list of contact details (this contains the main information stored in
     * the contact such as name, phone number, email, etc.)
     */
    public final List<ContactDetail> details = new ArrayList<ContactDetail>();

    /**
     * A list groups (server IDs) which the contact is a member of
     */
    public List<Long> groupList = null;

    /**
     * Set to true if this contact should be synchronised with the native
     * address book.
     * <p>
     * Also determines which contacts are shown in the phonebook group in the
     * UI.
     */
    @Column(name = "Synctophone")
    public Boolean synctophone = null;

    /**
     * Find Tags item for specified String
     * 
     * @param tag String value to find Tags item for
     * @return Tags item for specified String, null otherwise
     */
    private Tags findTag(String tag) {
        for (Tags tags : Tags.values()) {
            if (tag.compareTo(tags.tag()) == 0) {
                return tags;
            }
        }
        LogUtils.logE("Contact.findTag - Unsupported contact tag: " + tag);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return CONTACT_DATA_TYPE;
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
        StringBuffer sb = new StringBuffer("--\nContact data:");
        sb.append("\n\tLocal Contact ID: "); sb.append(localContactID);
        sb.append("\n\tContact ID: "); sb.append(contactID); 
        sb.append("\n\tUser ID: "); sb.append(userID); 
        sb.append("\n\tAbout me: "); sb.append(aboutMe); 
        sb.append("\n\tFriend of mine: "); sb.append(friendOfMine); 
        sb.append("\n\tDeleted: ");  sb.append(deleted);
        sb.append("\n\tGender: ");  sb.append(gender); 
        sb.append("\n\tSynctophone: "); 
        sb.append(synctophone);
        sb.append("\n\tNative Contact ID: "); sb.append(nativeContactId); 
        sb.append("\n\tupdated: "); sb.append(updated);
        sb.append(" Date: "); sb.append(time.toGMTString()); sb.append("\n");

        if (sources != null) {
            sb.append("Sources ("); sb.append(sources.size()); sb.append("): ");
            for (int i = 0; i < sources.size(); i++) {
                sb.append(sources.get(i)); sb.append(",");
            }
            sb.append("\n");
        }

        if (groupList != null) {
            sb.append("Group id list = [");
            for (int i = 0; i < groupList.size(); i++) {
                sb.append(groupList.get(i));
                if (i < groupList.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("\n");
        }

        sb.append("Contact details ("); 
        sb.append(details.size()); sb.append("):\n");
        for (int i = 0; i < details.size(); i++) {
            sb.append(details.get(i).toString()); 
            sb.append("\n");
        }
        sb.append("\n--------------------------------------------------");
        return sb.toString();
    }

    /**
     * Create Hashtable representing Contact parameters. This is used to create
     * Hessian encoded payload for Contacts upload.
     * 
     * @return Hashtable containing contact parameters.
     */
    public Hashtable<String, Object> createHashtable() {
        Hashtable<String, Object> htab = new Hashtable<String, Object>();

        if (aboutMe != null) {
            htab.put(Tags.ABOUT_ME.tag(), aboutMe);
        }
        if (contactID != null) {
            htab.put(Tags.CONTACT_ID.tag(), contactID);
        }
        if (userID != null) {
            htab.put(Tags.USER_ID.tag(), userID);
        }
        if (updated != null && updated != 0) {
            htab.put(Tags.UPDATED.tag(), updated);
        }
        if (profilePath != null && profilePath.length() > 0) {
            htab.put(Tags.PROFILE_PATH.tag(), profilePath);
        }
        if (friendOfMine != null) {
            htab.put(Tags.FRIEND.tag(), friendOfMine);
        }
        if (deleted != null) {
            htab.put(Tags.DELETED.tag(), deleted);
        }
        if (synctophone != null) {
            htab.put(Tags.SYNCTOPHONE.tag(), synctophone);
        }
        if (groupList != null) {
            Vector<Long> vL = new Vector<Long>();
            for (Long l : groupList) {
                vL.add(l);
            }
            htab.put(Tags.GROUP_LIST.tag(), vL);
        }

        if (details != null && details.size() > 0) {
            Vector<Object> v = new Vector<Object>();
            for (int i = 0; i < details.size(); i++) {
                v.add(details.get(i).createHashtable());
            }
            htab.put(Tags.DETAIL_LIST.tag(), v);
        }

        return htab;
    }

    /**
     * Create Contact item from Hashtable generated by Hessian-decoder
     * 
     * @param hash Hashtable containing Contact parameters
     * @return Contact item created from hashtable or null if hash was corrupted
     */
    public static Contact createFromHashtable(Hashtable<String, Object> hash) {
        Contact cont = new Contact();
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = cont.findTag(key);
            cont.setValue(tag, value);
        }
        return cont;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param val Value associated with the tag
     */
    private void setValue(Tags tag, Object value) {
        if (tag == null) {
            LogUtils.logE("Contact setValue tag is null");
            return;
        }
        switch (tag) {
            case ABOUT_ME:
                aboutMe = (String)value;
                break;
            case CONTACT_ID:
                contactID = (Long)value;
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
                        
                        LogUtils.logE("Contact.setValue(), the error [" + e + "] occured while adding a detail to the following contact:" + toString());
                    }
                }
                break;
            case DETAIL:
                break;
            case GROUP_LIST:
                @SuppressWarnings("unchecked")
                Vector<Long> gL = (Vector<Long>)value;
                groupList = new ArrayList<Long>();
                for (Long l : gL) {
                    groupList.add(l);
                }
                break;
            case UPDATED:
                updated = (Long)value;
                break;
            case USER_ID:
                userID = (Long)value;
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
            case DELETED:
                deleted = (Boolean)value;
                break;
            case FRIEND:
                friendOfMine = (Boolean)value;
                break;
            case GENDER:
                if (gender == null) {
                    gender = (Integer)value;
                }
                break;
            case SYNCTOPHONE:
                synctophone = (Boolean)value;
                break;
            default:
                // Do nothing.
                break;
        }
    }

    /**
     * Copy parameters from UserProfile object.
     * 
     * @param source UserProfile object to populate Contact with.
     */
    public void copy(UserProfile source) {
        userID = source.userID;
        profilePath = source.profilePath;
        contactID = source.contactID;
        if (source.sources != null && source.sources.size() > 0) {
            if (sources == null) {
                sources = new ArrayList<String>();
            } else {
                sources.clear();
            }
            sources.addAll(source.sources);
        } else {
            sources = null;
        }
        gender = source.gender;
        details.clear();
        details.addAll(source.details);
        aboutMe = source.aboutMe;
        friendOfMine = source.friendOfMine;
        updated = source.updated;

        localContactID = null;
        deleted = null;
        nativeContactId = null;
        groupList = null;
        synctophone = null;
    }

    /**
     * Member data definitions used when reading and writing Contact from/to
     * Parcels.
     */
    private enum MemberData {
        LOCALID,
        NAME,
        ABOUTME,
        CONTACTID,
        USERID,
        SOURCES,
        GENDER,
        UPDATED,
        PROFILEPATH,
        FRIENDOFMINE,
        DELETED,
        NATIVE_CONTACT_ID,
        SYNCTOPHONE;
    }

    /**
     * Read Contact from Parcel.
     * Note: only called from tests.
     *
     * @param in Parcel containing Contact item.
     */
    public void readFromParcel(Parcel in) {
        aboutMe = null;
        contactID = null;
        userID = null;
        sources = null;
        gender = null;
        updated = null;
        profilePath = null;
        friendOfMine = null;
        deleted = null;
        nativeContactId = null;
        groupList = null;
        synctophone = null;

        boolean[] validDataList = new boolean[MemberData.values().length];
        in.readBooleanArray(validDataList);

        if (validDataList[MemberData.LOCALID.ordinal()]) {
            localContactID = in.readLong();
        }
        if (validDataList[MemberData.ABOUTME.ordinal()]) {
            aboutMe = in.readString();
        }
        if (validDataList[MemberData.CONTACTID.ordinal()]) {
            contactID = in.readLong();
        }
        if (validDataList[MemberData.USERID.ordinal()]) {
            userID = in.readLong();
        }
        if (validDataList[MemberData.SOURCES.ordinal()]) {
            sources = new ArrayList<String>();
            in.readStringList(sources);
        }
        if (validDataList[MemberData.GENDER.ordinal()]) {
            gender = in.readInt();
        }
        if (validDataList[MemberData.SYNCTOPHONE.ordinal()]) {
            synctophone = (in.readByte() == 0 ? false : true);
        }
        if (validDataList[MemberData.UPDATED.ordinal()]) {
            updated = in.readLong();
        }
        if (validDataList[MemberData.PROFILEPATH.ordinal()]) {
            profilePath = in.readString();
        }
        if (validDataList[MemberData.FRIENDOFMINE.ordinal()]) {
            friendOfMine = (in.readByte() == 0 ? false : true);
        }
        if (validDataList[MemberData.DELETED.ordinal()]) {
            deleted = (in.readByte() == 0 ? false : true);
        }
        if (validDataList[MemberData.NATIVE_CONTACT_ID.ordinal()]) {
            nativeContactId = in.readInt();
        }
        int noOfDetails = in.readInt();
        details.clear();
        for (int i = 0; i < noOfDetails; i++) {
            ContactDetail detail = ContactDetail.CREATOR.createFromParcel(in);
            details.add(detail);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 1;
    }

    /** {@inheritDoc}
     *
     * Note: only called from tests.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        boolean[] validDataList = new boolean[MemberData.values().length];
        int validDataPos = dest.dataPosition();
        dest.writeBooleanArray(validDataList); // placeholder for real array
        if (localContactID != null) {
            validDataList[MemberData.LOCALID.ordinal()] = true;
            dest.writeLong(localContactID);
        }
        if (aboutMe != null) {
            validDataList[MemberData.ABOUTME.ordinal()] = true;
            dest.writeString(aboutMe);
        }
        if (contactID != null) {
            validDataList[MemberData.CONTACTID.ordinal()] = true;
            dest.writeLong(contactID);
        }
        if (userID != null) {
            validDataList[MemberData.USERID.ordinal()] = true;
            dest.writeLong(userID);
        }
        if (sources != null && sources.size() > 0) {
            validDataList[MemberData.SOURCES.ordinal()] = true;
            dest.writeStringList(sources);
        }
        if (gender != null) {
            validDataList[MemberData.GENDER.ordinal()] = true;
            dest.writeInt(gender);
        }
        if (updated != null) {
            validDataList[MemberData.UPDATED.ordinal()] = true;
            dest.writeLong(updated);
        }
        if (profilePath != null) {
            validDataList[MemberData.PROFILEPATH.ordinal()] = true;
            dest.writeString(profilePath);
        }
        if (friendOfMine != null) {
            validDataList[MemberData.FRIENDOFMINE.ordinal()] = true;
            dest.writeByte((byte)(friendOfMine ? 1 : 0));
        }
        if (deleted != null) {
            validDataList[MemberData.DELETED.ordinal()] = true;
            dest.writeByte((byte)(deleted ? 1 : 0));
        }
        if (synctophone != null) {
            validDataList[MemberData.SYNCTOPHONE.ordinal()] = true;
            dest.writeByte((byte)(synctophone ? 1 : 0));
        }
        if (nativeContactId != null) {
            validDataList[MemberData.NATIVE_CONTACT_ID.ordinal()] = true;
            dest.writeInt(nativeContactId);
        }

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // real array
        dest.setDataPosition(currentPos);

        dest.writeInt(details.size());
        for (ContactDetail detail : details) {
            detail.writeToParcel(dest, 0);
        }
    }

    /**
     * Fetches the preffered ContactDetail for this Contact for example the
     * preffered phone number or email address If no such is found, an
     * unpreffered contact will be taken
     * 
     * @param detailKey The type of the Detail (PHONE, EMAIL_ADDRESS)
     * @return preffered ContactDetail, any ContactDetail if no preferred is
     *         found or null if no detail at all is found
     */
    public ContactDetail getContactDetail(ContactDetail.DetailKeys detailKey) {
        ContactDetail preffered = getPrefferedContactDetail(detailKey);
        if (preffered != null)
            return preffered;
        for (ContactDetail detail : details) {
            if (detail != null && detail.key == detailKey)
                return detail;
        }
        return null;
    }

    /**
     * Fetches the preffered ContactDetail for this Contact and a DetailKey for
     * example the preffered phone number or email address
     * 
     * @param detailKey The type of the Detail (PHONE, EMAIL_ADDRESS)
     * @return preffered ContactDetail or null if no such is found
     */
    private ContactDetail getPrefferedContactDetail(ContactDetail.DetailKeys detailKey) {
        for (ContactDetail detail : details) {
            if (detail != null && detail.key == detailKey
                    && detail.order == ContactDetail.ORDER_PREFERRED)
                return detail;
        }
        return null;
    }

    public String getDetailString() {
        StringBuilder sb = new StringBuilder();
        for (ContactDetail detail : details) {
            sb.append(detail.getValue());
            sb.append("|");
        }
        return sb.toString();
    }
}
