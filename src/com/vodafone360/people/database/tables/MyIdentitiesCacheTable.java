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

package com.vodafone360.people.database.tables;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.IdentityCapability;
import com.vodafone360.people.datatypes.IdentityCapability.CapabilityID;

/**
 * The MyIdentitiesCacheTable class implements a persistent cache of the identities that were
 * registered by the user. Using it allows to display these identities if the device is restarted
 * and no network connection is available.
 */
public class MyIdentitiesCacheTable {

    /**
     * The identities cache table name.
     */
    public final static String TABLE_NAME = "MyIndentitiesCache";
    
    /**
     * Capability flag: no capability.
     */
    public final static int FLAG_CAPABILITY_NONE = 0x0;
    
    /**
     * Capability flag: chat capability.
     */
    public final static int FLAG_CAPABILITY_CHAT = 0x1;
    
    /**
     * Capability flag: mail capability.
     */
    public final static int FLAG_CAPABILITY_MAIL = 0x2;
    
    
    /**
     * Table fields.
     */
    private static enum Fields {
        _ID("_id"),
        NAME("Name"),
        NETWORK_ID("NetworkId"),
        USER_NAME("UserName"),
        CAPABILITIES("Capabilities");

        /**
         * The name of the field as it appears in the database
         */
        private String mField;

        /**
         * Constructor
         * 
         * @param field - The name of the field (see list above)
         */
        private Fields(String field) {
            mField = field;
        }

        /**
         * @return the name of the field as it appears in the database.
         */
        public String toString() {
            return mField;
        }
    }
    
    /**
     * Creates the table in the database.
     * 
     * @param database the database where to create the table
     * @return true if creation is successful, false otherwise
     */
    public static boolean create(SQLiteDatabase database) {
        
        DatabaseHelper.trace(true, "MyIdentitiesCacheTable.create()");
        
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("CREATE TABLE IF NOT EXISTS ");
        buffer.append(TABLE_NAME);
        buffer.append(" (");
        buffer.append(Fields._ID);
        buffer.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        buffer.append(Fields.NAME);
        buffer.append(" TEXT, ");
        buffer.append(Fields.USER_NAME);
        buffer.append(" TEXT, ");
        buffer.append(Fields.NETWORK_ID);
        buffer.append(" TEXT, ");
        buffer.append(Fields.CAPABILITIES);
        buffer.append(" INTEGER);");
        
        try {
            
            database.execSQL(buffer.toString());
            return true;
        } catch(Exception e) {
            
            DatabaseHelper.trace(true, "MyIdentitiesCacheTable.create() - Exception: "+e);
        }
        
        return false;
    }
    
    /**
     * Populates the provided ContentValues object with the Identity data.
     * 
     * Note: the ContentValues object is first cleared before being filled.
     * 
     * @param values the ContentValues object to populate
     * @param identity the identity to extract the data from
     * @return the provided ContentValues object
     */
    private static ContentValues getContentValues(ContentValues values, Identity identity) {
        
        int capabilities = FLAG_CAPABILITY_NONE;
        
        values.clear();
        
        values.put(Fields.NAME.toString(), identity.mName);
        values.put(Fields.USER_NAME.toString(), identity.mUserName);
        values.put(Fields.NETWORK_ID.toString(), identity.mNetwork);
        
        if (identity.mCapabilities != null) {
            
            for (IdentityCapability cap : identity.mCapabilities) {
                
                if (cap.mCapability != null) {
                    
                    switch(cap.mCapability) {
                        case chat:
                            if (cap.mValue) capabilities |= FLAG_CAPABILITY_CHAT;
                            break;
                        case mail:
                            if (cap.mValue) capabilities |= FLAG_CAPABILITY_MAIL;
                            break;
                        // TODO: add the other cases when needed
                    }
                }
            }
        }
        
        values.put(Fields.CAPABILITIES.toString(), capabilities);
        
        return values;
    }
    
    /**
     * Persists the provided identities into the client database.
     * 
     * @param database the database where to save the values
     * @param identities the identities to save
     */
    public static void setCachedIdentities(SQLiteDatabase database, ArrayList<Identity> identities) {
        
        final ContentValues contentValues = new ContentValues();
        
        database.delete(TABLE_NAME, null, null);
        
        for (Identity identity : identities) {
            
            database.insert(TABLE_NAME, null, getContentValues(contentValues, identity));
        }
    }
    
    /**
     * Retrieves the saved identities.
     * 
     * @param database the database where to read the values
     * @param identities the identities array to fill
     */
    public static void getCachedIdentities(SQLiteDatabase database, ArrayList<Identity> identities) {
        
        final String[] COLUMNS = { /* 0 */Fields.NAME.toString(), /* 1 */Fields.USER_NAME.toString(),
                                   /* 2 */Fields.NETWORK_ID.toString(), /* 3 */Fields.CAPABILITIES.toString() };
        Cursor cursor = null;
        
        try {
            
            cursor = database.query(TABLE_NAME, COLUMNS, null, null, null, null, null);
            
            while (cursor.moveToNext()) {
                
                final Identity identity = new Identity();
                final int capabilities = cursor.getInt(3);
                
                identity.mName = cursor.getString(0);
                identity.mUserName = cursor.getString(1);
                identity.mNetwork = cursor.getString(2);
                identity.mCapabilities = getCapabilities(capabilities);
                identities.add(identity);
            }
            
        } catch(Exception e) {
            DatabaseHelper.trace(true, "MyIdentitiesCacheTable.getCachedIdentities() - Exception: "+e);
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
    }
    
    /**
     * Gets an array of IdentityCapability from capability flags.
     * 
     * @param capabilities the capability flags
     * @return an array of IdentityCapability
     */
    private static ArrayList<IdentityCapability> getCapabilities(int capabilities) {
        
        ArrayList<IdentityCapability> capabilitiesList = null;
        
        if (capabilities != FLAG_CAPABILITY_NONE) {
            
            IdentityCapability cap;
            capabilitiesList = new ArrayList<IdentityCapability>();
            
            if ((capabilities & FLAG_CAPABILITY_CHAT) == FLAG_CAPABILITY_CHAT) {
                cap = new IdentityCapability();
                cap.mCapability = CapabilityID.chat;
                cap.mValue = true;
                capabilitiesList.add(cap);
            }
            if ((capabilities & FLAG_CAPABILITY_MAIL) == FLAG_CAPABILITY_MAIL) {
                cap = new IdentityCapability();
                cap.mCapability = CapabilityID.chat;
                cap.mValue = true;
                capabilitiesList.add(cap);
            }
        }
        
        return capabilitiesList;
    }
}
