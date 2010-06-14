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

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.PresenceTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;

public class PresenceDbUtils {

    private static Long sMeProfileUserId = -1L;

    private static Long sMeProfileLocalContactId = -1L;

    public static void resetMeProfileIds() {
        sMeProfileUserId = -1L;
        sMeProfileLocalContactId = -1L;
    }

    /**
     * Parses user data before storing it to database
     * 
     * @param user
     * @param databaseHelper
     * @return
     */
    private static boolean convertUserIds(User user, DatabaseHelper databaseHelper) {
        if (!user.getPayload().isEmpty()) {
            long localContactId = -1;
            ArrayList<NetworkPresence> payload = user.getPayload();
            boolean resetOverallPresenceStatus = false;
            String userId = null;
            for (NetworkPresence presence : payload) {
                userId = presence.getUserId();
                if (notNullOrBlank(userId)) {
                    int networkId = presence.getNetworkId();
                    if (userId.equals(String.valueOf(getMeProfileUserId(databaseHelper)))) {
                        localContactId = sMeProfileLocalContactId;
                        // remove the PC presence
                        if (networkId == SocialNetwork.PC.ordinal()) {
                            user.getPayload().remove(presence);
                            resetOverallPresenceStatus = true;
                            break;
                        }
                    } else {
                        if (networkId == SocialNetwork.MOBILE.ordinal()
                                || (networkId == SocialNetwork.PC.ordinal())) {
                            localContactId = ContactsTable.fetchLocalIdFromUserId(Long
                                    .valueOf(userId), databaseHelper.getReadableDatabase());
                        } else {
                            localContactId = ContactDetailsTable.findLocalContactIdByKey(
                                    SocialNetwork.getPresenceValue(networkId).toString(), userId,
                                    ContactDetail.DetailKeys.VCARD_IMADDRESS, databaseHelper
                                            .getReadableDatabase());
                        }
                        if (localContactId != -1) {
                            break;
                        }
                    }
                }
            }
            if (resetOverallPresenceStatus) {
                int max = OnlineStatus.OFFLINE.ordinal();
                for (NetworkPresence presence : user.getPayload()) {
                    if (presence.getOnlineStatusId() > max)
                        max = presence.getOnlineStatusId();
                }
                user.setOverallOnline(max);
            }
            user.setLocalContactId(localContactId);
            return true;
        }
        LogUtils.logE("presence data can't be parsed!!");
        return false;
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
        User user = PresenceTable.getUserPresenceByLocaContactId(
                getMeProfileUserId(databaseHelper), databaseHelper.getWritableDatabase());
        if (user == null || (user.getPayload() == null)) { // the table is
            // empty, need to set
            // the status for the
            // 1st time
            // TODO: this hard code needs change, must filter the identities
            // info by VCARD.IMADRESS
            Hashtable<String, String> status = new Hashtable<String, String>();
            status.put("google", "online");
            status.put("microsoft", "online");
            status.put("mobile", "online");
            status.put("facebook.com", "online");
            status.put("hyves.nl", "online");
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
        User user = PresenceTable.getUserPresenceByLocaContactId(localContactId, databaseHelper
                .getWritableDatabase());
        LogUtils.logW("UI  called getUserPresenceStatusByLocalContactId: " + user);
        return user;
    }

    /**
     * Here we update the PresenceTable, and the ContactSummaryTable afterwards
     * the HandlerAgent receives the notification of presence states changes
     * 
     * @param users
     */
    protected static boolean updateDatabase(List<User> users, long idListeningTo,
            DatabaseHelper dbHelper) {
        LogUtils.logE("PresenceDBUtils updatedatabase in:");
        boolean contactsChanged = false;
        for (User user : users) {
            if (convertUserIds(user, dbHelper)) {
                SQLiteDatabase writableDb = dbHelper.getWritableDatabase();

                int status = PresenceTable.updateUser(user, writableDb);

                if (status != PresenceTable.USER_NOTADDED) {
                    user = PresenceTable.getUserPresenceByLocaContactId(user.getLocalContactId(), writableDb);
                    if (user != null) {
                        ContactSummaryTable.updateOnlineStatus(user);
                        if (user.getLocalContactId() == idListeningTo)
                            contactsChanged = true;    
                    }
                } else {
                    LogUtils.logE("PresenceDbUtils.updateDatabase():... USER WAS NOT ADDED");
                }
            }
        }
        if (!contactsChanged) {
            contactsChanged = (idListeningTo == -1);
        }
        LogUtils.logE("PresenceDBUtils updatedatabase out:");

        return contactsChanged;
    }

    protected static boolean updateMyPresence(User user, DatabaseHelper dbHelper) {
        boolean contactsChanged = false;
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        if (PresenceTable.updateUser(user, writableDb) != PresenceTable.USER_NOTADDED) {
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

    /**
     * @param input
     * @return
     */
    public static boolean notNullOrBlank(String input) {
        return (input != null) && input.length() > 0;
    }

}
