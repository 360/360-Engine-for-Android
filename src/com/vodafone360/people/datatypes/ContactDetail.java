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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType representing ContactDetails retrieved from, or issued to,
 * server.
 * <p>
 * Contains a specific contact detail such as name, address, phone number, etc.
 */
public class ContactDetail extends BaseDataType implements Parcelable {

    private static final String LOCATION_PROVIDER = "Now+ Sevice";

    private static final String LOCATION_DELIMITER = ":";

    public static final String UNKNOWN_NAME = "Unknown";

    private static final String TYPE_PREFERRED = "preferred";

    public static final int ORDER_PREFERRED = 0;

    public static final int ORDER_NORMAL = 50;

    /**
     * Definitions of KEY types for Contact-Details.
     */
    public enum DetailKeyTypes {
        HOME("home"),
        WORK("work"),
        MOBILE("mobile"), // Type not recognised by NOW+ server - use CELL
        // instead
        BIRTHDAY("birthday"),
        CELL("cell"),
        FAX("fax"),
        UNKNOWN("unknown");

        private final String typeName;

        /**
         * Constructor for detailKeyTypes item.
         * 
         * @param n String value associated with detailKeyTypes item.
         */
        private DetailKeyTypes(String n) {
            typeName = n;
        }

        /**
         * String value associated with detailKeyTypes item.
         * 
         * @return String value for detailKeyTypes item.
         */
        public String tag() {
            return typeName;
        }

        /**
         * Find detailKeyTypes item for specified String
         * 
         * @param tag String value to find detailKeyTypes item for
         * @return detailKeyTypes item for specified String, null otherwise
         */
        protected static DetailKeyTypes findKey(String k) {
            for (DetailKeyTypes type : DetailKeyTypes.values()) {
                if (k.compareTo(type.tag()) == 0) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Contact Detail KEY definitions
     */
    public enum DetailKeys {
        VCARD_NAME("vcard.name"), // 0
        VCARD_NICKNAME("vcard.nickname"), // 1
        VCARD_DATE("vcard.date"), // 2
        VCARD_EMAIL("vcard.email"), // 3
        VCARD_PHONE("vcard.phone"), // 4
        VCARD_ADDRESS("vcard.address"), // 5
        VCARD_URL("vcard.url"), // 6
        VCARD_INTERNET_ADDRESS("vcard.internetaddress"), // 7
        VCARD_IMADDRESS("vcard.imaddress"), // 8
        VCARD_ROLE("vcard.role"), // 9haven't found short name for it
        VCARD_ORG("vcard.org"), // 10haven't found short name for it
        VCARD_TITLE("vcard.title"), // 11haven't found short name for it
        VCARD_NOTE("vcard.note"), // 12
        VCARD_BUSINESS("vcard.business"), // 13only in API doc
        PRESENCE_TEXT("presence.text"), // 14
        PHOTO("photo"), // 15
        LOCATION("location"), // 16
        GENDER("gender"), // 17only in API doc
        RELATION("relation"), // 18only in API doc
        BOOKMARK("bookmark"), // 19only in API doc
        INTEREST("interest"), // 20only in API doc
        FOLDER("folder"), // 21only in API doc
        GROUP("group"), // 22only in API doc
        LINK("link"), // 23only in API doc
        EXTERNAL("external"), // 24only in API doc
        UNKNOWN("unknown"); // 25only in API doc

        private final String keyName;

        /**
         * Constructor for detailKeys item.
         * 
         * @param n String value for DetailKeys item.
         */
        private DetailKeys(String n) {
            keyName = n;
        }

        /**
         * Return String value associated with detailKeys item.
         * 
         * @return String value associated with detailKeys item.
         */
        private String tag() {
            return keyName;
        }
    }

    /**
     * Tags associated with ContactDetail item.
     */
    private enum Tags {
        KEY("key"),
        VALUE("val"),
        DELETED("deleted"),
        ALT("alt"),
        UNIQUE_ID("detailid"), // previously rid
        ORDER("order"),
        UPDATED("updated"),
        TYPE("type"),
        BYTES("bytes"), // docs are inconsistent about those
        BYTES_MIME_TYPE("bytesmime"), // might be those 3 last tags
        BYTES_URL("bytesurl"); // are not possible in contact details
        // but they are inside Content structure.

        private final String tag;

        /**
         * Construct Tags item from supplied String
         * 
         * @param s String value for Tags item.
         */
        private Tags(String s) {
            tag = s;
        }

        /**
         * Return String value associated with Tags item.
         * 
         * @return String value associated with Tags item.
         */
        private String tag() {
            return tag;
        }

    }

    /**
     * Primary key in the ContactDetails table
     */
    public Long localDetailID = null; // Primary key in database

    /**
     * Secondary key which links the contact detail with a contact
     */
    public Long localContactID = null;

    /**
     * Determines which kind of contact detail this object refers to (name,
     * address, phone number, etc)
     */
    public DetailKeys key = DetailKeys.UNKNOWN;

    /**
     * Type of detail (home, business, work, etc.)
     */
    public DetailKeyTypes keyType = null;

    /**
     * Current value of the detail
     */
    public String value = null;

    /**
     * True if the contact detail has been deleted on the server
     */
    public Boolean deleted = null;

    /**
     * Contains the last time the contact detail was updated on the server
     */
    public Long updated = 0L;

    /**
     * Contains the server ID if the contact detail has been synchronised with
     * the server, null otherwise.
     */
    public Long unique_id = null;

    /**
     * Contains the order in which the contact detail should be displayed. The
     * lower the value, the higher in the list the contact detail should be.
     */
    public Integer order = 0;

    /**
     * An alternative value to display for the contact detail
     */
    public String alt = null;

    /**
     * The location associated with the contact detail obtained from the server
     */
    public Location location = null;

    /**
     * A photo associated with the detail. It is preferred that large objects
     * such as photos are stored in the file system rather than the database.
     * Hence this may never be used.
     */
    public Bitmap photo = null;

    /**
     * The mime type of the image pointed to in the {@link #photo_url} field.
     * 
     * @see photo_url
     */
    public String photo_mime_type = "";

    /**
     * Contains the remote URL on the server where the image is located.
     * 
     * @see #photo_mime_type
     */
    public String photo_url = "";

    /**
     * Internal field which is used to cache the contact server ID. This is not
     * stored in the database table.
     */
    public Long serverContactId;

    /**
     * Internal field which is used to cache the sync native contact ID. This is
     * not stored in the database table.
     */
    public Integer syncNativeContactId;

    /**
     * Internal field which is a secondary key linking the detail with a contact
     * in the native address book. Is null if this detail is not linked with the
     * native address book.
     */
    public Integer nativeContactId = null;

    /**
     * Internal field which is a secondary key linking the detail with a contact
     * detail in the native address book. Is null if this detail is not linked
     * with the native address book.
     */
    public Integer nativeDetailId = null;

    /**
     * Internal field which is a secondary key to link the detail with a change
     * in the change log table. The field is a temporary field used only during
     * contact sync and is not stored or parcelled.
     */
    public Long changeID = null;

    /**
     * A string copied from the native address book which can be used to
     * determine if this contact has changed.
     * 
     * @see #nativeVal2
     * @see #nativeVal3
     */
    public String nativeVal1 = null;

    /**
     * A string copied from the native address book which can be used to
     * determine if this contact has changed.
     * 
     * @see #nativeVal1
     * @see #nativeVal3
     */
    public String nativeVal2 = null;

    /**
     * A string copied from the native address book which can be used to
     * determine if this contact has changed.
     * 
     * @see #nativeVal1
     * @see #nativeVal2
     */
    public String nativeVal3 = null;

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
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String name() {
        return "ContactDetail";
    }

    /**
     * Create Hashtable containing ContactDetail parameters
     * 
     * @return Hashtable containing Contact detail parameters
     */
    public Hashtable<String, Object> createHastable() {
        Hashtable<String, Object> htab = new Hashtable<String, Object>();

        if (key != null) {
            htab.put("key", key.tag());
        }
        if (unique_id != null) {
            htab.put("detailid", unique_id);
        }

        if ((deleted != null) && (deleted.booleanValue())) {
            // if the detail is marked as deleted we return a detail with detail
            // id and key and
            // it will be deleted
            return htab;
        }

        if (keyType != null && keyType != DetailKeyTypes.UNKNOWN) {
            htab.put("type", keyType.tag());
        }
        if (value != null) {
            htab.put("val", value);
        }
        if (updated != null && updated != 0) {
            htab.put("updated", updated);
        }
        if (order != null) {
            htab.put("order", order);
        }
        if (location != null) {
            htab.put("location", location.getLatitude() + LOCATION_DELIMITER
                    + location.getLongitude());
        }
        if (photo_url != null && photo_url.length() > 0) {
            htab.put("photo_url", photo_url);
        }
        if (photo != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream(); // AA:
            // there's
            // no point
            // to set
            // the size
            // here new
            // ByteArrayOutputStream(1)
            photo.compress(CompressFormat.PNG, 100, os);
            byte[] bytes = os.toByteArray();
            htab.put(Tags.BYTES.tag(), bytes);
        }
        if (photo_mime_type != null && photo_mime_type.length() > 0) {
            htab.put(Tags.BYTES_MIME_TYPE.tag(), photo_mime_type);
        }
        return htab;
    }

    /**
     * Create ContactDetail from Hashtable generated by Hessian-decoder
     * 
     * @param hash Hashtable containing ContactDetail parameters
     * @return ContactDetail created from Hashtable
     */
    protected ContactDetail createFromHashtable(Hashtable<String, Object> hash) {
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = findTag(key);
            setValue(tag, value);
        }

        return this;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param val Value associated with the tag
     */
    private void setValue(Tags tag, Object obValue) {
        if (tag == null) {
            LogUtils.logE("ContactDetail setValue tag is null");
            return;
        }

        switch (tag) {
            case KEY:
                for (DetailKeys keys : DetailKeys.values()) {
                    if (((String)obValue).compareTo(keys.tag()) == 0) {
                        key = keys;
                        break;
                    }
                }
                break;
            case DELETED:
                deleted = (Boolean)obValue;
                break;
            case ALT:
                if (alt == null) {
                    String valStr = (String)obValue;
                    int delim = valStr.indexOf(LOCATION_DELIMITER);
                    if (delim < 0) {
                        LogUtils.logE("Location string has wrong format: " + valStr);
                        alt = valStr;
                    } else {
                        if (delim > 0 && valStr.length() > delim + 1) {
                            location = new Location(LOCATION_PROVIDER);
                            String lat = valStr.substring(0, delim - 1);
                            String lon = valStr.substring(delim + 1);
                            location.setLatitude(Double.valueOf(lat));
                            location.setLongitude(Double.valueOf(lon));
                            if (updated != null) {
                                location.setTime(updated);
                            }
                        }
                    }
                }
                break;
            case BYTES:
                byte[] data = (byte[])obValue;
                photo = BitmapFactory.decodeByteArray(data, 0, data.length);
                break;
            case BYTES_MIME_TYPE:
                photo_mime_type = (String)obValue;
                break;
            case BYTES_URL:
                photo_url = (String)obValue;
                break;
            case ORDER:
                order = (Integer)obValue;
                break;
            case TYPE:
                processTypeData((String)obValue);
                break;
            case UNIQUE_ID:
                unique_id = (Long)obValue;
                break;
            case UPDATED:
                updated = (Long)obValue;
                if (location != null) {
                    location.setTime(updated);
                }
                break;
            case VALUE:
                value = (String)obValue;
            default:
        }
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
        String ret = "\tContact detail:" + "\n\t\tLocal Detail ID: " + localDetailID
                + "\n\t\tLocal Contact ID: " + localContactID + "\n\t\tKey: " + key
                + "\n\t\tKey type: " + keyType + "\n\t\tValue: " + value + "\n\t\tDeleted: "
                + deleted + "\n\t\tOrder: " + order + "\n\t\tLocation: " + location + "\n\t\tAlt: "
                + alt + "\n\t\tContact ID: " + serverContactId + "\n\t\tNative Contact ID: "
                + nativeContactId + "\n\t\tNative Detail ID: " + nativeDetailId
                + "\n\t\tNative Val 1: " + nativeVal1 + "\n\t\tNative Val 2: " + nativeVal2
                + "\n\t\tNative Val 3: " + nativeVal3 + "\n\t\tPhoto Mime Type: " + photo_mime_type
                + "\n\t\tPhoto URL: " + photo_url + "\n\t\tUpdated: " + updated + " Date: "
                + time.toGMTString() + "\n\t\tUnique ID: " + unique_id
                + "\n\t\tserverContactId: " + serverContactId
                + "\n\t\tsyncNativeContactId: " + syncNativeContactId;
        if (location != null) {
            ret += "\n\t\tLocation: " + location.toString();
        }

        if (photo != null) {
            ret += "\n\t\tPhoto BYTE[] is present";
        }
        ret += "\n\t\tPhoto MIME type: " + photo_mime_type;
        ret += "\n\t\tPhoto URL: " + photo_url;

        return ret;
    }

    /**
     * Copy ContactDetail parameters from supplied ContactDetail item.
     * 
     * @param source ContactDetail to copy from.
     */
    public void copy(ContactDetail source) {
        android.os.Parcel _data = android.os.Parcel.obtain();
        source.writeToParcel(_data, 0);
        _data.setDataPosition(0);
        readFromParcel(_data);
    }

    /**
     * Default constructor for ContactDetail.
     */
    public ContactDetail() {
    }

    /**
     * Construct ContactDetail from supplied Parcel.
     * 
     * @param in Parcel containing ContactDetails.
     */
    private ContactDetail(Parcel in) {
        readFromParcel(in);
    }

    /**
     * @param rawVal
     * @param inKey
     * @param inType
     */
    public void setValue(String rawVal, DetailKeys inKey, DetailKeyTypes inType) {
        key = inKey;
        keyType = inType;
        value = VCardHelper.makeSingleTextValue(rawVal);
    }

    /**
     * Fetches single value from a VCard entry using VCardHelper class.
     * 
     * @return value as String.
     */
    public String getValue() {
        return VCardHelper.getSingleTextValue(value);
    }

    /**
     * Set VCard name field using VCardHelper.
     * 
     * @param name VCardHelper name item.
     */
    public void setName(VCardHelper.Name name) {
        key = DetailKeys.VCARD_NAME;
        keyType = null;
        value = VCardHelper.makeName(name);
    }

    /**
     * Return VCard name field
     * 
     * @return VCardHelper.Name containing name.
     */
    public VCardHelper.Name getName() {
        if (value != null && !value.equals(UNKNOWN_NAME)) {
            return VCardHelper.getName(value);
        }
        return null;
    }

    /**
     * Set VCard organisation field.
     * 
     * @param org VCardHelper.Organisation containing representation of
     *            organisation fields.
     * @param type detailKeyTypes (i.e. HOME/WORK).
     */
    public void setOrg(VCardHelper.Organisation org, DetailKeyTypes type) {
        key = DetailKeys.VCARD_ORG;
        keyType = type;
        value = VCardHelper.makeOrg(org);
    }

    /**
     * Return organisation field value.
     * 
     * @return VCardHelper.Organisation containing organisation.
     */
    public VCardHelper.Organisation getOrg() {
        return VCardHelper.getOrg(value);
    }

    /**
     * Set VCard date field.
     * 
     * @param time Time to set.
     * @param inType detailKeyTypes for item (i.e BIRTHDAY).
     */
    public void setDate(Time time, DetailKeyTypes inType) {
        key = DetailKeys.VCARD_DATE;
        keyType = inType;
        value = VCardHelper.makeDate(time);
    }

    /**
     * Get date value.
     * 
     * @return Time containing date value.
     */
    public Time getDate() {
        return VCardHelper.getDate(value);
    }

    /**
     * Set email address
     * 
     * @param emailAddress String containing email address
     * @param inType detailKeyTypes vale specifying address type.
     */
    public void setEmail(String emailAddress, DetailKeyTypes inType) {
        key = DetailKeys.VCARD_EMAIL;
        keyType = inType;
        value = VCardHelper.makeEmail(emailAddress);
    }

    /*
     * Get email address value with assistance of VCardHelper.
     * @return String containing email address.
     */
    public String getEmail() {
        return VCardHelper.getEmail(value);
    }

    /**
     * Set postal address.
     * 
     * @param address VCardHelper.PostalAddress containing postal address
     *            fields.
     * @param inType detailKeyTypes specifying address type.
     */
    public void setPostalAddress(VCardHelper.PostalAddress address, DetailKeyTypes inType) {
        key = DetailKeys.VCARD_ADDRESS;
        keyType = inType;
        value = VCardHelper.makePostalAddress(address);
    }

    /**
     * Get postal address
     * 
     * @return postal address placed in VCardHelper.PostalAddress.
     */
    public VCardHelper.PostalAddress getPostalAddress() {
        return VCardHelper.getPostalAddress(value);
    }

    /**
     * Set telephone number.
     * 
     * @param tel String containing telephone number.
     * @param inType detailKeyTypes identifying number type.
     */
    public void setTel(String tel, DetailKeyTypes inType) {
        key = DetailKeys.VCARD_PHONE;
        keyType = inType;
        value = VCardHelper.makeTel(tel);
    }

    /**
     * Return telephone number as String
     * 
     * @return String containing telephone number.
     */
    public String getTel() {
        return VCardHelper.getTel(value);
    }

    /**
     * Attempt to set type based on supplied data. If type can not be determined
     * the suppled data is used to populate 'alt;' field.
     * 
     * @param typeData String containing type information.
     */
    private void processTypeData(String typeData) {
        final int posIdx = typeData.indexOf(';');
        if (posIdx >= 0) {
            List<String> list = new ArrayList<String>();
            VCardHelper.getStringList(list, typeData);
            for (String type : list) {
                if (processType(type)) {
                    break;
                }
            }
        } else {
            processType(typeData);
        }
        if (keyType == null && ((String)typeData).trim().length() > 0) {
            alt = ((String)typeData);
        }
    }

    /**
     * Set key-type based on supplied key String.
     * 
     * @param typeString String containing type.
     * @return true if the type is supported, false otherwise.
     */
    private boolean processType(String typeString) {
        if (typeString.equals(TYPE_PREFERRED)) {
            return false;
        }
        for (DetailKeyTypes type : DetailKeyTypes.values()) {
            if (typeString.equals(type.tag())) {
                keyType = type;
                return true;
            }
        }
        return false;
    }

    /**
     * Enumeration consisting of fields written to/from Parcel containing
     * ContactDetail item.
     */
    private enum MemberData {
        LOCAL_DETAIL_ID,
        LOCAL_CONTACT_ID,
        KEY,
        KEY_TYPE,
        VALUE,
        DELETED,
        UPDATED,
        UNIQUE_ID,
        ORDER,
        LOCATION,
        ALT,
        PHOTO,
        PHOTO_MIME_TYPE,
        PHOTO_URL,
        SERVER_CONTACT_ID,
        NATIVE_CONTACT_ID,
        NATIVE_DETAIL_ID,
        NATIVE_VAL1,
        NATIVE_VAL2,
        NATIVE_VAL3;
    }

    /**
     * Read ContactDetail item from supplied Parcel.
     * 
     * @param in PArcel containing ContactDetail.
     */
    private void readFromParcel(Parcel in) {
        localDetailID = null; // Primary key in database
        localContactID = null;
        key = null;
        keyType = null;
        value = null;
        updated = null;
        unique_id = null;
        order = null;
        location = null;
        photo = null;
        photo_mime_type = null;
        photo_url = null;
        serverContactId = null;
        nativeContactId = null;
        nativeVal1 = null;
        nativeVal2 = null;
        nativeVal3 = null;

        boolean[] validDataList = new boolean[MemberData.values().length];
        in.readBooleanArray(validDataList);

        if (validDataList[MemberData.LOCAL_DETAIL_ID.ordinal()]) {
            localDetailID = in.readLong(); // Primary key in database
        }

        if (validDataList[MemberData.LOCAL_CONTACT_ID.ordinal()]) {
            localContactID = in.readLong();
        }

        if (validDataList[MemberData.KEY.ordinal()]) {
            key = DetailKeys.values()[in.readInt()];
        }

        if (validDataList[MemberData.KEY_TYPE.ordinal()]) {
            keyType = DetailKeyTypes.values()[in.readInt()];
        }

        if (validDataList[MemberData.VALUE.ordinal()]) {
            value = in.readString();
        }

        if (validDataList[MemberData.DELETED.ordinal()]) {
            deleted = (in.readByte() == 0 ? false : true);
        }

        if (validDataList[MemberData.UPDATED.ordinal()]) {
            updated = in.readLong();
        }

        if (validDataList[MemberData.UNIQUE_ID.ordinal()]) {
            unique_id = in.readLong();
        }

        if (validDataList[MemberData.ORDER.ordinal()]) {
            order = in.readInt();
        }

        if (validDataList[MemberData.LOCATION.ordinal()]) {
            location = Location.CREATOR.createFromParcel(in);
        }

        if (validDataList[MemberData.ALT.ordinal()]) {
            alt = in.readString();
        }

        if (validDataList[MemberData.PHOTO.ordinal()]) {
            photo = Bitmap.CREATOR.createFromParcel(in);
        }

        if (validDataList[MemberData.PHOTO_MIME_TYPE.ordinal()]) {
            photo_mime_type = in.readString();
        }

        if (validDataList[MemberData.PHOTO_URL.ordinal()]) {
            photo_url = in.readString();
        }

        if (validDataList[MemberData.SERVER_CONTACT_ID.ordinal()]) {
            serverContactId = in.readLong();
        }

        if (validDataList[MemberData.NATIVE_CONTACT_ID.ordinal()]) {
            nativeContactId = in.readInt();
        }

        if (validDataList[MemberData.NATIVE_DETAIL_ID.ordinal()]) {
            nativeDetailId = in.readInt();
        }

        if (validDataList[MemberData.NATIVE_VAL1.ordinal()]) {
            nativeVal1 = in.readString();
        }

        if (validDataList[MemberData.NATIVE_VAL2.ordinal()]) {
            nativeVal2 = in.readString();
        }

        if (validDataList[MemberData.NATIVE_VAL3.ordinal()]) {
            nativeVal3 = in.readString();
        }
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
        dest.writeBooleanArray(validDataList); // placeholder for real array

        if (localDetailID != null) {
            validDataList[MemberData.LOCAL_DETAIL_ID.ordinal()] = true;
            dest.writeLong(localDetailID); // Primary key in database
        }

        if (localContactID != null) {
            validDataList[MemberData.LOCAL_CONTACT_ID.ordinal()] = true;
            dest.writeLong(localContactID);
        }

        if (key != null) {
            validDataList[MemberData.KEY.ordinal()] = true;
            dest.writeInt(key.ordinal());
        }

        if (keyType != null) {
            validDataList[MemberData.KEY_TYPE.ordinal()] = true;
            dest.writeInt(keyType.ordinal());
        }

        if (value != null) {
            validDataList[MemberData.VALUE.ordinal()] = true;
            dest.writeString(value);
        }

        if (deleted != null) {
            validDataList[MemberData.DELETED.ordinal()] = true;
            dest.writeByte((byte)(deleted ? 1 : 0));
        }

        if (updated != null) {
            validDataList[MemberData.UPDATED.ordinal()] = true;
            dest.writeLong(updated);
        }

        if (unique_id != null) {
            validDataList[MemberData.UNIQUE_ID.ordinal()] = true;
            dest.writeLong(unique_id);
        }

        if (order != null) {
            validDataList[MemberData.ORDER.ordinal()] = true;
            dest.writeInt(order);
        }

        if (location != null) {
            validDataList[MemberData.LOCATION.ordinal()] = true;
            location.writeToParcel(dest, 0);
        }

        if (alt != null) {
            validDataList[MemberData.ALT.ordinal()] = true;
            dest.writeString(alt);
        }

        if (photo != null) {
            validDataList[MemberData.PHOTO.ordinal()] = true;
            photo.writeToParcel(dest, 0);
        }

        if (photo_mime_type != null) {
            validDataList[MemberData.PHOTO_MIME_TYPE.ordinal()] = true;
            dest.writeString(photo_mime_type);
        }

        if (photo_url != null) {
            validDataList[MemberData.PHOTO_URL.ordinal()] = true;
            dest.writeString(photo_url);
        }

        if (serverContactId != null) {
            validDataList[MemberData.SERVER_CONTACT_ID.ordinal()] = true;
            dest.writeLong(serverContactId);
        }

        if (nativeContactId != null) {
            validDataList[MemberData.NATIVE_CONTACT_ID.ordinal()] = true;
            dest.writeInt(nativeContactId);
        }

        if (nativeDetailId != null) {
            validDataList[MemberData.NATIVE_DETAIL_ID.ordinal()] = true;
            dest.writeInt(nativeDetailId);
        }

        if (nativeVal1 != null) {
            validDataList[MemberData.NATIVE_VAL1.ordinal()] = true;
            dest.writeString(nativeVal1);
        }

        if (nativeVal2 != null) {
            validDataList[MemberData.NATIVE_VAL2.ordinal()] = true;
            dest.writeString(nativeVal2);
        }

        if (nativeVal3 != null) {
            validDataList[MemberData.NATIVE_VAL3.ordinal()] = true;
            dest.writeString(nativeVal3);
        }

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // real array
        dest.setDataPosition(currentPos);
    }

    /**
     * Interface to allow ContactDetail to be written and restored from a
     * Parcel.
     */
    protected static final Parcelable.Creator<ContactDetail> CREATOR = new Parcelable.Creator<ContactDetail>() {
        public ContactDetail createFromParcel(Parcel in) {
            return new ContactDetail(in);
        }

        public ContactDetail[] newArray(int size) {
            return new ContactDetail[size];
        }
    };
}
