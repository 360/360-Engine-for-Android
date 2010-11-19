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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.SQLKeys;
import com.vodafone360.people.engine.presence.NetworkPresence;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.StringBufferPool;

/**
 * MePresenceCacheTable
 * 
 * This table is used to store cached Availability/Presence for the Me Contact.
 * Storing this information ensures that the appropriate values are used when, for example,
 * regaining connectivity.
 * 
 * @throws SQLException is thrown when request to create the table fails with an
 *             SQLException
 * @throws NullPointerException if the passed in database instance is null
 */
public abstract class MePresenceCacheTable {

    /**
     * The name of the table as it appears in the database.
     */
    public static final String TABLE_NAME = "MePresenceCache";

    /**
     * The primary key column String value.
     */
    private static final String ID_STRING = "id";
    
    /**
     * The User ID column String value.
     */
    private static final String USER_ID_STRING = "UserId";
    
    /**
     * The Network ID column String value. 
     */
    private static final String NETWORK_ID_STRING = "NetworkId";
    
    /**
     * The Status column String value.
     */
    private static final String STATUS_STRING = "Status";

    /**
     * The User ID column index value.
     */
    private static final int USER_ID = 1;

    /**
     * The Network ID column index value.
     */
    private static final int NETWORK_ID = 2;

    /**
     * The Network ID column index value.
     */
    private static final int STATUS = 3;

    /**
     * The default message for the NullPointerException caused by the null
     * instance of database passed into PresenceCacheTable methods.
     */
    private static final String DEFAULT_ERROR_MESSAGE = 
        "MePresenceCacheTable: the passed in database is null!";
    
    /**
     * This method creates the PresenceTable.
     * 
     * @param writableDb - the writable database
     * @throws SQLException is thrown when request to create a table fails with
     *             an SQLException
     * @throws NullPointerException if the passed in database instance is null
     */
    public static void create(SQLiteDatabase writableDb) 
        throws SQLException, NullPointerException {
        DatabaseHelper.trace(true, "PresenceCacheTable.create()");
        if (writableDb == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        
        final StringBuffer sb = new StringBuffer();
        sb.append("CREATE TABLE IF NOT EXISTS "); 
        sb.append(TABLE_NAME); sb.append(" ("); sb.append(ID_STRING);
        sb.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sb.append(USER_ID_STRING); sb.append(" STRING, ");
        sb.append(NETWORK_ID_STRING); sb.append(" INT, ");
        sb.append(STATUS_STRING); sb.append(" INT);");
      
        writableDb.execSQL(sb.toString());
    }
    
    /**
     * Retrieves current NetworkPresence values in the cache.
     * @param readableDatabase Readable object to access db
     * @return Current cached values or null if there are none.
     * @throws SQLException if the database layer throws this exception.
     * @throws NullPointerException if the passed in database instance is null.
     */
    public static ArrayList<NetworkPresence> getCache(SQLiteDatabase readableDatabase) 
        throws SQLException, NullPointerException {
        Cursor c = null;
        if (readableDatabase == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        ArrayList<NetworkPresence> presences = null;
        try {            
            c = readableDatabase.rawQuery("SELECT * FROM " + TABLE_NAME, null);
            if(c.getCount() > 0) {
                presences = new ArrayList<NetworkPresence>();
            }
            while (c.moveToNext()) {
                String userId = c.getString(USER_ID);
                int networkId = c.getInt(NETWORK_ID);
                int statusId = c.getInt(STATUS);
                presences.add(new NetworkPresence(userId, networkId, statusId));
            }
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        
        return presences;
    }
    

    /**
     * Updates the cache using a lone NetworkPresence value.
     * The value is inserted if not already present in the table.
     * @param presence The Network Presence to update
     * @param writableDatabase writable object to access db
     * @throws SQLException if the database layer throws this exception.
     * @throws NullPointerException if the passed in database instance is null.
     */
    public static void updateCache(NetworkPresence presence, SQLiteDatabase writableDatabase) 
        throws SQLException, NullPointerException {
        if (writableDatabase == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        ContentValues values = new ContentValues();
        int networkId = presence.getNetworkId();
        values.put(USER_ID_STRING, presence.getUserId());
        values.put(STATUS_STRING, presence.getOnlineStatusId());
        StringBuffer where = StringBufferPool.getStringBuffer(NETWORK_ID_STRING);        
        where.append(SQLKeys.EQUALS).append(networkId);
        
        int numberOfAffectedRows = 
            writableDatabase.
                update( TABLE_NAME, 
                        values, 
                        StringBufferPool.toStringThenRelease(where),
                        null);
        
        if(numberOfAffectedRows == 0) {
            values.put(NETWORK_ID_STRING, networkId);
            writableDatabase.insert(TABLE_NAME, null, values);
        }
    }
    
    
    /**
     * Updates the cache using a NetworkPresence array.
     * The values are inserted if not already present in the table.
     * @param presences The Network Presences to update
     * @param writableDatabase writable object to access db
     * @throws SQLException if the database layer throws this exception.
     * @throws NullPointerException if the passed in database instance is null.
     */
    public static void updateCache(ArrayList<NetworkPresence> presences, 
            SQLiteDatabase writableDatabase) throws SQLException, NullPointerException {
        if (writableDatabase == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        if(presences == null) {
            return;
        }

        for(NetworkPresence presence : presences) {
            updateCache(presence, writableDatabase);
        }
    }
  
}

