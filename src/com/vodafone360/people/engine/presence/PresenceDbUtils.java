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

package com.vodafone360.people.engine.presence;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.PresenceTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.utils.HardcodedUtils;
import com.vodafone360.people.utils.LogUtils;

public class PresenceDbUtils {
    /**
     * the user id of the me profile contact
     */
    private static long sMeProfileUserId = -1L;
    /**
     * the local contact id of the me profile contact
     */
    private static long sMeProfileLocalContactId = -1L;

    public static void resetMeProfileIds() {
        sMeProfileUserId = -1L;
        sMeProfileLocalContactId = -1L;
    }
    
    /**
     * This method returns true if the provided user id matches the one of me profile.
     * @return TRUE if the provided user id matches the one of me profile.
     */
    private static boolean isMeProfile(String userId, DatabaseHelper databaseHelper) {
        return userId.equals(String.valueOf(getMeProfileUserId(databaseHelper)));
    }
    
    /**
     * @param databaseHelper
     * @return
     */
    protected static Long getMeProfileUserId(DatabaseHelper databaseHelper) {
        if (sMeProfileUserId == -1L) {
            Contact meProfile = new Contact();
            if (SyncMeDbUtils.fetchMeProfile(databaseHelper, meProfile) != ServiceStatus.ERROR_NOT_FOUND) {
                sMeProfileUserId = meProfile.userID;
                sMeProfileLocalContactId = meProfile.localContactID;
            }

        }
        return sMeProfileUserId;

    }

    /**
     * @param databaseHelper
     * @return
     */
    protected static User getMeProfilePresenceStatus(DatabaseHelper databaseHelper) {
        // if
        // (!sMeProfileLocalContactId.equals(databaseHelper.getMeProfileId())) {
        Long meProfileId = SyncMeDbUtils.getMeProfileLocalContactId(databaseHelper);
        sMeProfileLocalContactId = (meProfileId == null || meProfileId.intValue() == -1) ? -1
                : meProfileId;
        // LogUtils.logE("The DB Helper and Utils IDs are not synchronized");
        // }
        User user = PresenceTable.getUserPresenceByLocalContactId(
                getMeProfileUserId(databaseHelper), databaseHelper.getWritableDatabase());
        if (user == null || (user.getPayload() == null)) { // the table is
            // empty, need to set
            // the status for the
            // 1st time
            // Get presence list constructed from identities
            Hashtable<String, String> status =             
                EngineManager.getInstance().getPresenceEngine().getPresencesForStatus(OnlineStatus.ONLINE);
            user = new User(String.valueOf(getMeProfileUserId(databaseHelper)), status);
        }
        return user;
    }

    /**
     * This method returns wrapper with the presence information for all user
     * networks
     * 
     * @param localContactId - the localContactId of the contact we want to get
     *            presence states information for.
     * @param databaseHelper
     * @return User wrapper with the presence information for all user networks.
     *         If no information is on the database the payload is NULL
     */
    public static User getUserPresenceStatusByLocalContactId(long localContactId,
            DatabaseHelper databaseHelper) {
        User user = PresenceTable.getUserPresenceByLocalContactId(localContactId, databaseHelper
                .getWritableDatabase());
        LogUtils.logW("UI  called getUserPresenceStatusByLocalContactId: " + user);
        return user;
    }

    /**
     * Here we update the PresenceTable, and the ContactSummaryTable afterwards
     * the HandlerAgent receives the notification of presence states changes.
     *  
     * @param users List<User> - the list of user presence states
     * @param idListeningTo long - local contact id which this UI is watching, -1 is all contacts
     * @param dbHelper DatabaseHelper - the database.
     * @return TRUE if database has changed in result of the update.
     */
    protected static boolean updateDatabase(List<User> users, long idListeningTo,
            DatabaseHelper dbHelper) {
        boolean presenceChanged = false;
        boolean deleteNetworks = false;
         // list of network presence information we ignore - the networks where the user is offline.
        ArrayList<Integer> ignoredNetworks = new ArrayList<Integer>();
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();

        for (User user : users) {
            if (!user.getPayload().isEmpty()) {
                long localContactId = -1;
                ArrayList<NetworkPresence> payload = user.getPayload();
                // if it is the me profile User
                boolean meProfile = false;
                String userId = null;
                int networkId = 0;
                for (NetworkPresence presence : payload) {
                    userId = presence.getUserId();
                    if (!TextUtils.isEmpty(userId)) {
                        networkId = presence.getNetworkId();
                        // if this is me profile contact 
                        if (isMeProfile(userId, dbHelper)) { 
                            localContactId = sMeProfileLocalContactId;
                            meProfile = true;
                            // remove the PC presence, as we don't display it in me profile
                            if (networkId == SocialNetwork.PC.ordinal()) {
                                presenceChanged = true;
                            } 
                         } // 360 contact, PC or MOBILE network
                         else if (networkId == SocialNetwork.PC.ordinal() || networkId == SocialNetwork.MOBILE.ordinal()) {
                            localContactId = ContactsTable.fetchLocalIdFromUserId(Long
                                    .valueOf(userId), writableDb);
                            if (localContactId != -1) {
                                break;
                            }
                        } else { // 3rd party accounts
                             localContactId = ContactDetailsTable.findLocalContactIdByKey(
                                    SocialNetwork.getPresenceValue(networkId).toString(), userId,
                                    ContactDetail.DetailKeys.VCARD_IMADDRESS, writableDb);
                             if (localContactId != -1) {
                                 break;
                             }
                        }
                    }
                }
                // set the local contact id
                user.setLocalContactId(localContactId);
                if (meProfile) {
                    if (deleteNetworks = processMeProfile(presenceChanged, user, ignoredNetworks)) {
                        // delete the information about offline networks from PresenceTable 
                        PresenceTable.setTPCNetworksOffline(ignoredNetworks, writableDb);
                    }
                } 
                if (user.getLocalContactId() > -1) {
                    // will not save infos from the ignored networks
                    updateUserRecord(user, ignoredNetworks, writableDb);
                    if (user.getLocalContactId() == idListeningTo) {
                        presenceChanged = true;    
                    }
                }
            }
        }
        // if contact summary table needs extra refresh, to make sure no statuses are displayed for offline TPC networks users
        if (deleteNetworks) {
            ArrayList<Long> userIds = PresenceTable.getLocalContactIds(dbHelper.getWritableDatabase());
            ContactSummaryTable.setUsersOffline(userIds);
            presenceChanged = true;
        }
        if (idListeningTo == UiAgent.ALL_USERS) {
            presenceChanged = true;
        }
        return presenceChanged;
    }
    
    /**
     * This method writes the user presence status change from the passed User object
     *  to the database and then fills the same User object with updated information.  
     * @param user - the User presence change.
     * @param ignoredNetworks - the networks information from which must be ignored.
     * @param writableDb - database.
     */
    private static void updateUserRecord(User user , ArrayList<Integer> ignoredNetworks, SQLiteDatabase writableDb) {
//      write the user presence update into the database and read the complete wrapper
        PresenceTable.updateUser(user, ignoredNetworks, writableDb);
        PresenceTable.getUserPresence(user, writableDb);
        
//      update the user aggregated presence state in the ContactSummaryTable
        ContactSummaryTable.updateOnlineStatus(user);
    }

    /**
     * This method alters the User wrapper of me profile,
     *  and returns true if me profile information contains the ignored TPC networks information.
     * Based on the result this information may be deleted.
     * @param removePCPresence - if TRUE the PC network will be removed from the network list.
     * @param user - the me profile wrapper.
     * @param ignoredNetworks - the list if ignored integer network ids. 
     * @return
     */
    private static boolean processMeProfile(boolean removePCPresence, User user, ArrayList<Integer> ignoredNetworks){
        if (removePCPresence) {
            user.removeNetwork(SocialNetwork.PC.ordinal());
            int max = OnlineStatus.OFFLINE.ordinal();
            // calculate the new aggregated presence status
            for (NetworkPresence presence : user.getPayload()) {
                if (presence.getOnlineStatusId() > max) {
                    max = presence.getOnlineStatusId();
                }
            }    
            user.setOverallOnline(max);                        
        }
        // the list of chat network ids in this User wrapper                
        ArrayList<Integer> userNetworks = new ArrayList<Integer>();

        ArrayList<NetworkPresence> payload = user.getPayload();
        
        for (NetworkPresence presence : payload) {
            int networkId = presence.getNetworkId();
            userNetworks.add(networkId);
            // 1. ignore offline TPC networks 
            if (presence.getOnlineStatusId() == OnlineStatus.OFFLINE.ordinal()) {
                ignoredNetworks.add(networkId);
            }    
        }
        // 2. ignore the TPC networks presence state for which is unknown
        for (int networkId : HardcodedUtils.THIRD_PARTY_CHAT_ACCOUNTS) {
            if (!userNetworks.contains(networkId)) {
                ignoredNetworks.add(networkId);
            }
        }
         
        return !ignoredNetworks.isEmpty();
    }
    
    
    protected static boolean updateMyPresence(User user, DatabaseHelper dbHelper) {
        boolean contactsChanged = false;
        if (PresenceTable.updateUser(
        		user, null, dbHelper.getWritableDatabase()) != PresenceTable.USER_NOTADDED) {
            contactsChanged = (ContactSummaryTable.updateOnlineStatus(user) == ServiceStatus.SUCCESS);
        }
        return contactsChanged;
    }

    /**
     * Set all users to offline state
     * 
     * @param dbHelper
     */
    protected static void setPresenceOfflineInDatabase(DatabaseHelper dbHelper) {
        SQLiteDatabase writableDatabase = dbHelper.getWritableDatabase();
        PresenceTable.setAllUsersOffline(writableDatabase);
        ContactSummaryTable.setOfflineStatus();
    }

    /**
     * Removes all presence infos besides those related to MeProfile
     * 
     * @param dbHelper
     */
    protected static void resetPresenceStatesAcceptForMe(long localContactIdOfMe,
            DatabaseHelper dbHelper) {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        if (writableDb != null) {
            LogUtils.logW(" PresenceDBUtils.resetPresenceStatesAcceptForMe: "
                    + "#rows affected by delete method "
                    + PresenceTable.setAllUsersOfflineExceptForMe(localContactIdOfMe, writableDb));
            ContactSummaryTable.setOfflineStatusExceptForMe(localContactIdOfMe);
        }
    }
}
