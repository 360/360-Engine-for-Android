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

package com.vodafone360.people.engine.contactsync;

import java.util.HashMap;


/**
 * Class describing a Contact Change.
 * A Contact Change is meant to represent the smallest chunk of data 
 * associated with a Contact whilst being able to map and track its properties:
 * - Origin (via IDs and type);
 * - Purpose (destination); 
 * - Attributes (IDs, Key, Value and Flags)
 */
public class ContactChange {       
    /**
     * UNKNOWN Type
     */
    public static final int TYPE_UNKNOWN = 0;
    /**
     * ADD_CONTACT Type
     */
    public static final int TYPE_ADD_CONTACT = 1;
    /**
     * UPDATE_CONTACT Type
     */
    public static final int TYPE_UPDATE_CONTACT = 2;
    /**
     * DELETE_CONTACT Type
     */
    public static final int TYPE_DELETE_CONTACT = 3;
    /**
     * ADD_DETAIL Type
     */
    public static final int TYPE_ADD_DETAIL = 4;
    /**
     * UPDATE_DETAIL Type
     */
    public static final int TYPE_UPDATE_DETAIL = 5;
    /**
     * UPDATE_DELETE_DETAIL Type
     */
    public static final int TYPE_DELETE_DETAIL = 6;
    /**
     * UPDATE_BACKEND_CONTACT_ID Type
     */
    public static final int TYPE_UPDATE_BACKEND_CONTACT_ID = 7;
    /**
     * UPDATE_NAB_CONTACT_ID Type
     */
    public static final int TYPE_UPDATE_NAB_CONTACT_ID = 8;
    /**
     * UPDATE_BACKEND_DETAIL_ID Type
     */
    public static final int TYPE_UPDATE_BACKEND_DETAIL_ID = 9;
    /**
     * UPDATE_NAB_DETAIL_ID Type
     */
    public static final int TYPE_UPDATE_NAB_DETAIL_ID = 10;
    
    /**
     * None (null) flag
     */
    public static final int FLAG_NONE       = 0x0;
    /**
     * TODO: Needed?!
     * PREFERRED literal attribute
     */
    public static final String ATTRIBUTE_PREFERRED  = "preferred";
    /**
     * Preferred Flag
     */
    public static final int FLAG_PREFERRED  = 0x01;
    /**
     * TODO: Needed?!
     * HOME literal attribute
     */
    public static final String ATTRIBUTE_HOME       = "home";
    /**
     * HOME Flag
     */
    public static final int FLAG_HOME       = 0x02;
    /**
     * TODO: Needed?!
     * CELL literal attribute
     */
    public static final String ATTRIBUTE_CELL       = "cell";
    /**
     * CELL Flag
     */
    public static final int FLAG_CELL       = 0x04;
    /**
     * TODO: Needed?!
     * WORK literal attribute
     */
    public static final String ATTRIBUTE_WORK       = "work";
    /**
     * WORK Flag
     */
    public static final int FLAG_WORK       = 0x08;
    /**
     * BIRTHDAY FLAG
     */
    public static final String ATTRIBUTE_BIRTHDAY   = "birthday";
    /**
     * TODO: Needed?!
     * BIRTHDAY Flag
     */
    public static final int FLAG_BIRTHDAY   = 0x10;
    /**
     * TODO: Needed?!
     * FAX literal attribute
     */
    public static final String ATTRIBUTE_FAX        = "fax";
    /**
     * FAX flag
     */
    public static final int FLAG_FAX        = 0x20;

    /**
     * HOME and CELL Flag combination for convenience purposes
     */
    public static final int FLAGS_HOME_CELL = FLAG_HOME | FLAG_CELL;
    /**
     * WORK and CELL Flag combination for convenience purposes
     */
    public static final int FLAGS_WORK_CELL = FLAG_WORK | FLAG_CELL;
    
    /**
     * HOME and FAX Flag combination for convenience purposes
     */
    public static final int FLAGS_HOME_FAX = FLAG_HOME | FLAG_FAX;
    /**
     * WORK and FAX Flag combination for convenience purposes
     */
    public static final int FLAGS_WORK_FAX = FLAG_WORK | FLAG_FAX;
    
    /**
     * UNKNOWN Key
     */
    public static final int KEY_UNKNOWN = 0;
    
    /**
     * NAME Key
     */
    public static final int KEY_VCARD_NAME = 1;
    /**
     * NICKNAME Key
     */
    public static final int KEY_VCARD_NICKNAME = 2;
    /**
     * DATE Key
     */
    public static final int KEY_VCARD_DATE = 3;
    /**
     * EMAIL Key
     */
    public static final int KEY_VCARD_EMAIL = 4;
    /**
     * PHONE Key
     */
    public static final int KEY_VCARD_PHONE = 5;
    /**
     * ADDRESS Key
     */
    public static final int KEY_VCARD_ADDRESS = 6;
    /**
     * URL Key
     */
    public static final int KEY_VCARD_URL = 7;
    /**
     * INTERNET_ADDRESS Key
     */
    public static final int KEY_VCARD_INTERNET_ADDRESS = 8; // TODO needed?!
    /**
     * IM_ADDRESS Key
     */
    public static final int KEY_VCARD_IMADDRESS = 9;
    /**
     * ROLE Key
     */
    public static final int KEY_VCARD_ROLE = 10;     // TODO needed?!
    /**
     * ORG Key
     */
    public static final int KEY_VCARD_ORG = 11;
    /**
     * TITLE Key
     */
    public static final int KEY_VCARD_TITLE = 12;
    /**
     * NOTE Key
     */
    public static final int KEY_VCARD_NOTE = 13;
    /**
     * BUSINESS Key
     */
    public static final int KEY_VCARD_BUSINESS = 14; // TODO needed?!
    /**
     * PRESENCE_TEXT Key
     */
    public static final int KEY_PRESENCE_TEXT = 15;
    /**
     * PHOTO Key
     */
    public static final int KEY_PHOTO = 16;
    /**
     * LOCATION Key
     */
    public static final int KEY_LOCATION = 17;      // TODO needed?!
    /**
     * GENDER Key
     */
    public static final int KEY_GENDER = 18;        // TODO needed?!
    /**
     * RELATION Key
     */
    public static final int KEY_RELATION = 19;      // TODO needed?!
    /**
     * BOOKMARK Key
     */
    public static final int KEY_BOOKMARK = 20;      // TODO needed?!
    /**
     * INTEREST Key
     */
    public static final int KEY_INTEREST = 21;      // TODO needed?!
    /**
     * FOLDER Key
     */
    public static final int KEY_FOLDER = 22;        // TODO needed?!
    /**
     * GROUP Key
     */
    public static final int KEY_GROUP = 23;         // TODO needed?!
    /**
     * LINK Key
     */
    public static final int KEY_LINK = 24;          // TODO needed?!
    /**
     * EXTERNAL Key
     */
    public static final int KEY_EXTERNAL = 25;      // TODO needed?!

    /**
     * None (null) destination
     */
    public static final int DESTINATION_FLAG_NONE  = 0;
    /**
     * NAB destination
     */
    public static final int DESTINATION_FLAG_NAB = 0x01;
    /**
     * CAB destination
     */
    public static final int DESTINATION_FLAG_CAB = 0x02;
    /**
     * RPG destination
     */
    public static final int DESTINATION_FLAG_RPG = 0x04;
    
    /**
     * Incoming via RPG. Goes to CAB first.
     */
    public static final int DESTINATIONS_CAB_NAB = 
        DESTINATION_FLAG_CAB | DESTINATION_FLAG_NAB;
        
    /**
     * Incoming via NAB. Goes to CAB first.
     */
    public static final int DESTINATIONS_CAB_RPG = 
        DESTINATION_FLAG_CAB | DESTINATION_FLAG_RPG; 
    
    /**
     * Incoming via client. Goes to CAB first.
     */
    public static final int DESTINATIONS_CAB_NAB_RPG = 
        DESTINATION_FLAG_CAB | DESTINATION_FLAG_NAB | DESTINATION_FLAG_RPG;
           
  
    /**
     * Lookup Table from Key integer to String values
     */
    private static final String[] sIntToKeyLookupTable;

    /**
     * Hashmap from Key String to integer values
     */
    private static final HashMap<String, Integer> sKeyToIntMap;
    
    static {
        sIntToKeyLookupTable = new String[26];
        sIntToKeyLookupTable [KEY_UNKNOWN] = "unknown";      // TODO needed?!
        sIntToKeyLookupTable [KEY_VCARD_NAME] = "vcard.name";
        sIntToKeyLookupTable [KEY_VCARD_NICKNAME] = "vcard.nickname";
        sIntToKeyLookupTable [KEY_VCARD_DATE] = "vcard.date";
        sIntToKeyLookupTable [KEY_VCARD_EMAIL] = "vcard.email";
        sIntToKeyLookupTable [KEY_VCARD_PHONE] = "vcard.phone";
        sIntToKeyLookupTable [KEY_VCARD_ADDRESS] = "vcard.address";
        sIntToKeyLookupTable [KEY_VCARD_URL] = "vcard.url";
        sIntToKeyLookupTable [KEY_VCARD_INTERNET_ADDRESS] = "vcard.internetaddress"; // TODO needed?!
        sIntToKeyLookupTable [KEY_VCARD_IMADDRESS] = "vcard.imaddress"; // TODO needed?!
        sIntToKeyLookupTable [KEY_VCARD_ROLE] = "vcard.role";         // TODO needed?!
        sIntToKeyLookupTable [KEY_VCARD_ORG] = "vcard.org";
        sIntToKeyLookupTable [KEY_VCARD_TITLE] = "vcard.title";
        sIntToKeyLookupTable [KEY_VCARD_NOTE] = "vcard.note";
        sIntToKeyLookupTable [KEY_VCARD_BUSINESS] = "vcard.business"; // TODO needed?! 
        sIntToKeyLookupTable [KEY_PRESENCE_TEXT] = "presence.text";
        sIntToKeyLookupTable [KEY_PHOTO] = "photo";
        sIntToKeyLookupTable [KEY_LOCATION] = "location";
        sIntToKeyLookupTable [KEY_GENDER] = "gender";        // TODO needed?!
        sIntToKeyLookupTable [KEY_RELATION] = "relation";    // TODO needed?!
        sIntToKeyLookupTable [KEY_BOOKMARK] = "bookmark";    // TODO needed?!
        sIntToKeyLookupTable [KEY_INTEREST] = "interest";    // TODO needed?!
        sIntToKeyLookupTable [KEY_FOLDER] = "folder";        // TODO needed?!
        sIntToKeyLookupTable [KEY_GROUP] = "group";          // TODO needed?!
        sIntToKeyLookupTable [KEY_LINK] = "link";            // TODO needed?!
        sIntToKeyLookupTable [KEY_EXTERNAL] = "external";    // TODO needed?!
        
        sKeyToIntMap = new HashMap<String, Integer>(26, 1);
        sKeyToIntMap.put("unknown", KEY_UNKNOWN);                  // TODO needed?!
        sKeyToIntMap.put("vcard.name", KEY_VCARD_NAME);
        sKeyToIntMap.put("vcard.nickname", KEY_VCARD_NICKNAME);
        sKeyToIntMap.put("vcard.date", KEY_VCARD_DATE);
        sKeyToIntMap.put("vcard.email", KEY_VCARD_EMAIL);
        sKeyToIntMap.put("vcard.phone", KEY_VCARD_PHONE);
        sKeyToIntMap.put("vcard.address", KEY_VCARD_ADDRESS);
        sKeyToIntMap.put("vcard.url", KEY_VCARD_URL);
        sKeyToIntMap.put("vcard.internetaddress", KEY_VCARD_INTERNET_ADDRESS); // TODO needed?!
        sKeyToIntMap.put("vcard.imaddress", KEY_VCARD_IMADDRESS);   // TODO needed?!
        sKeyToIntMap.put("vcard.role", KEY_VCARD_ROLE);             // TODO needed?!
        sKeyToIntMap.put("vcard.org", KEY_VCARD_ORG);
        sKeyToIntMap.put("vcard.title", KEY_VCARD_TITLE);
        sKeyToIntMap.put("vcard.note", KEY_VCARD_NOTE);
        sKeyToIntMap.put("vcard.business", KEY_VCARD_BUSINESS);     // TODO needed?!
        sKeyToIntMap.put("presence.text", KEY_PRESENCE_TEXT);
        sKeyToIntMap.put("photo", KEY_PHOTO);
        sKeyToIntMap.put("location", KEY_LOCATION);
        sKeyToIntMap.put("gender", KEY_GENDER);                    // TODO needed?!
        sKeyToIntMap.put("relation", KEY_RELATION);                // TODO needed?!
        sKeyToIntMap.put("bookmark", KEY_BOOKMARK);                // TODO needed?!
        sKeyToIntMap.put("interest", KEY_INTEREST);                // TODO needed?!
        sKeyToIntMap.put("folder", KEY_FOLDER);                    // TODO needed?!
        sKeyToIntMap.put("group", KEY_GROUP);                      // TODO needed?!
        sKeyToIntMap.put("link", KEY_LINK);                        // TODO needed?!
        sKeyToIntMap.put("external", KEY_EXTERNAL);                // TODO needed?!
        
    }
    
    /**
     * Invalid ID value applicable to any ID within this class
     */
    public static final long INVALID_ID = -1L;
    
    /**
     * Destination flags for this ContactChange
     */
    private int mDestinations = DESTINATION_FLAG_NONE;
    /**
     * Key for this ContactChange
     */
    private int mKey = KEY_UNKNOWN;
    /**
     * Flags for this ContactChange
     */
    private int mFlags = FLAG_NONE;
    /**
     * Value for this ContactChange
     */
    final private String mValue;
    /**
     * Type for this ContactChange
     */
    private int mType = TYPE_UNKNOWN;
    
    /**
     * Internal Contact ID for this ContactChange
     */
    private long mInternalContactId = INVALID_ID;
    /**
     * Internal Detail ID for this ContactChange
     */
    private long mInternalDetailId = INVALID_ID;
    /**
     * Backend Contact ID for this ContactChange
     */
    private long mBackendContactId = INVALID_ID;
    /**
     * Backend Detail ID for this ContactChange
     */
    private long mBackendDetailId = INVALID_ID;
    /**
     * NAB Contact ID for this ContactChange
     */
    private long mNabContactId = INVALID_ID;
    /**
     * NAB Detail ID for this ContactChange
     */
    private long mNabDetailId = INVALID_ID;

    /**
     * Default constructor
     */
    public ContactChange() {
        mValue = null;
    }
    
    /**
     * Constructor taking the change type
     * @param type The Change Type
     */
    public ContactChange(int type) {
        this();
        mType = type;
    }
    
    /**
     * Constructor taking a nabContactId 
     * @param nabContactId Native Contact ID
     */
    public ContactChange(long nabContactId) {
        this();
        mNabContactId = nabContactId;
    }
    
    /**
     * Constructor taking a nabContactId and a nabDetailId
     * @param nabContactId Native Contact ID
     * @param nabDetailId Native Detail ID
     */
    public ContactChange(long nabContactId, long nabDetailId) {
        this();
        mNabContactId = nabContactId;
        mNabDetailId = nabDetailId;
    }
    
    /**
     * Constructor taking destination flags and a type
     * @param destinations The destination flags
     * @param type The change type
     */
    public ContactChange(int destinations, int type) {
        this();
        mDestinations = destinations;
        mType = type;
    }
    
    /**
     * Constructor taking key, value and flags
     * @param key The Key for the Change
     * @param value The Value for the Change
     * @param flags The Flags for the Change
     */
    public ContactChange(int key, String value, int flags) {
        mKey = key;
        mValue = value;
        mFlags = flags;
    }
       
    /**
     * Creates a shallow copy of a change.
     * The copy contains the Contact IDs of the original
     * and the supplied type
     * @param cc The original Contact Change
     * @param type The new type
     * @return The shallow copy containing the IDs and Change type
     */
    public static ContactChange createIdsChange(ContactChange cc, int type) {
        ContactChange idChange =  new ContactChange();
        idChange.mInternalContactId = cc.mInternalContactId;
        idChange.mInternalDetailId = cc.mInternalDetailId;
        idChange.mBackendContactId = cc.mBackendContactId;
        idChange.mBackendDetailId = cc.mBackendDetailId;
        idChange.mNabContactId = cc.mNabContactId;
        idChange.mNabDetailId = cc.mNabDetailId;
        idChange.mType = type;
        return idChange;
    }
    
    /**
     * Creates a copy of the ContactChange with a modified value.
     * 
     * @param newValue the new value to set
     * @return a copy of the ContactChange with a new value
     */
    public ContactChange copyWithNewValue(String newValue) {
        
        ContactChange cc = new ContactChange(mKey, newValue, mFlags);
        cc.mInternalContactId = mInternalContactId;
        cc.mInternalDetailId = mInternalDetailId;
        cc.mBackendContactId = mBackendContactId;
        cc.mBackendDetailId = mBackendDetailId;
        cc.mNabContactId = mNabContactId;
        cc.mNabDetailId = mNabDetailId;
        cc.mType = mType;
        cc.mDestinations = mDestinations;
        
        return cc;
    }
    
    /**
     * Get the change destination flags.
     * @return The Destination flags
     */
    public int getDestinations() {
        return mDestinations;
    }

    /**
     * Set the change destination flags.
     * @param destinations The Destination flags to set 
     */
    public void setDestinations(int destinations) {
        mDestinations = destinations;
    }
    
    /**
     * Get the change Key.
     * @return The change Key
     */
    public int getKey() {
        return mKey;
    }

    /**
     * Get the change Key String value.
     * @return The Key String value for this change
     */
    public String getKeyToString() {
        if(mKey >=0) {
            return sIntToKeyLookupTable[mKey];
        }
        return "";
    }
    
    /**
     * Get the change value
     * @return The change value
     */
    public String getValue() {
        return mValue;
    }

    /**
     * Get the change type.
     * @return The change value.
     */
    public int getType() {
        return mType;
    }
    
    /**
     * Set the type of change.
     * @param type The type to set
     */
    public void setType(int type) {
        mType = type;
    }
    
    /**
     * Set the change flags.
     * @param flags The flags to set
     */
    public void setFlags(int flags) {
        mFlags = flags;
    }
    
    /**
     * Get the change flags.
     * @return The change flags
     */
    public int getFlags() {
        return mFlags;
    }
    
    /**
     * Sets the key.
     * @param key The key to set
     */
    public void setKey(int key) {
        mKey = key;
    }
    
    /**
     * Get the change's Internal Contact ID.
     * @return The Internal Contact ID
     */
    public long getInternalContactId() {
        return mInternalContactId;
    }

    /**
     * Set the change's Internal Contact ID.
     * @param The Internal Contact ID to set
     */
    public void setInternalContactId(long internalContactId) {
        mInternalContactId = internalContactId;
    }
    
    /**
     * Get the change's Internal Detail ID.
     * @return The Internal Detail ID
     */
    public long getInternalDetailId() {
        return mInternalDetailId;
    }

    /**
     * Set the change's Internal Detail ID.
     * @param The Internal Detail ID to set
     */
    public void setInternalDetailId(long internalDetailId) {
        mInternalDetailId = internalDetailId;
    }

    /**
     * Get the change's Backend Contact ID.
     * @return The Backend Contact ID
     */
    public long getBackendContactId() {
        return mBackendContactId;
    }

    /**
     * Set the change's Backend Contact ID.
     * @param The Backend Contact ID to set
     */
    public void setBackendContactId(long backendContactId) {
        mBackendContactId = backendContactId;
    }

    /**
     * Get the change's Backend Detail ID.
     * @return The Backend Detail ID
     */
    public long getBackendDetailId() {
        return mBackendDetailId;
    }

    /**
     * Set the change's Backend Detail ID.
     * @param The Backend Detail ID to set
     */
    public void setBackendDetailId(long backendDetailId) {
        mBackendDetailId = backendDetailId;
    }
    
    /**
     * Get the change's NAB Contact ID.
     * @return The NAB Contact ID
     */
    public long getNabContactId() {
        return mNabContactId;
    }

    /**
     * Set the change's NAB Contact ID.
     * @param The NAB Contact ID to set
     */
    public void setNabContactId(long nabContactId) {
        mNabContactId = nabContactId;
    }
    
    /**
     * Get the change's NAB Detail ID.
     * @return The NAB Detail ID
     */
    public long getNabDetailId() {
        return mNabDetailId;
    }

    /**
     * Set the change's NAB Detail ID.
     * @param The NAB Detail ID
     */
    public void setNabDetailId(long nabDetailId) {
        mNabDetailId = nabDetailId;
    }
}
