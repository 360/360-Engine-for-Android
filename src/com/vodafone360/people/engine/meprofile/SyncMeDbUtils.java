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

package com.vodafone360.people.engine.meprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.graphics.BitmapFactory;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactChangeLogTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.database.tables.ContactChangeLogTable.ContactChangeInfo;
import com.vodafone360.people.database.tables.ContactChangeLogTable.ContactChangeType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.UserProfile;
import com.vodafone360.people.datatypes.ContactDetail.DetailKeys;
import com.vodafone360.people.engine.presence.PresenceDbUtils;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.ThirdPartyAccount;
import com.vodafone360.people.utils.ThumbnailUtils;
/**
 * This class is a set of utility methods called by
 * SyncMeEngine to save/read data to/from the database. 
 *
 */
public class SyncMeDbUtils {

    /**
     * Me profile local contact id.
     */
    private static Long sMeProfileLocalContactId;

    /**
     * Mime type for the uploaded thumbnail picture of the me profile.
     */
    private static final String PHOTO_MIME_TYPE = "image/png";

    /**
     * This method create a Me Profile contact in the database.
     * @param dbHelper DatabaseHelper - the database.
     * @param meProfile Contact - the Me Profile contact
     * @return ServiceStatus - ServiceStatus.SUCCESS when the new contact is
     *         successfully created.
     */
    public static ServiceStatus setMeProfile(final DatabaseHelper dbHelper, Contact meProfile) {
        ServiceStatus status = ServiceStatus.ERROR_DATABASE_CORRUPT;
        // the contact didn't exist before
        if (sMeProfileLocalContactId == null) {
            List<Contact> contactList = new ArrayList<Contact>();
            contactList.add(meProfile);
            status = dbHelper.syncAddContactList(contactList, false, false);
            if (ServiceStatus.SUCCESS == status) {
                sMeProfileLocalContactId = meProfile.localContactID;
                status = StateTable.modifyMeProfileID(sMeProfileLocalContactId, dbHelper.getWritableDatabase());
                PresenceDbUtils.resetMeProfileIds();
                if (ServiceStatus.SUCCESS != status) {
                    List<ContactsTable.ContactIdInfo> idList = new ArrayList<ContactsTable.ContactIdInfo>();
                    ContactsTable.ContactIdInfo contactIdInfo = new ContactsTable.ContactIdInfo();
                    contactIdInfo.localId = meProfile.localContactID;
                    contactIdInfo.serverId = meProfile.contactID;
                    contactIdInfo.nativeId = meProfile.nativeContactId;
                    idList.add(contactIdInfo);
                    dbHelper.syncDeleteContactList(idList, false, false);
                }
            }
        }
        return status;
    }

    /**
     * This method reads Me Profile contact from the database.
     * @param dbHelper DatabaseHelper - the database
     * @param contact Contact - the empty (stub) contact to read into
     * @return ServiceStatus - ServiceStatus.SUCCESS when the contact is
     * successfully filled, ServiceStatus.ERROR_NOT_FOUND - if the Me
     * Profile needs to be created first.
     */
    public static ServiceStatus fetchMeProfile(final DatabaseHelper dbHelper, Contact contact) {
        if (sMeProfileLocalContactId == null) {
            return ServiceStatus.ERROR_NOT_FOUND;
        }
        return dbHelper.fetchContact(sMeProfileLocalContactId, contact);
    }

    /**
     * This method returns the Me Profile localContactId...
     * @param dbHelper DatabaseHelper - the database
     * @return Long - Me Profile localContactId.
     */
    public static Long getMeProfileLocalContactId(DatabaseHelper dbHelper) {
        if (dbHelper == null)
            return null;
        if (sMeProfileLocalContactId == null) {
            sMeProfileLocalContactId = StateTable.fetchMeProfileId(dbHelper.getReadableDatabase());
        }
        return sMeProfileLocalContactId;
    }

    /**
     * This method sets Me Profile localContactId...
     * @param meProfileId Long - localContactID
     */
    public static void setMeProfileId(final Long meProfileId) {
        sMeProfileLocalContactId = meProfileId;
    }

    /**
     * This method updates current Me Profile with changes from user profile.
     * @param dbHelper DatabaseHelper - database
     * @param currentMeProfile Contact - current me profile, from DB
     * @param profileChanges - the changes to the current Me Profile
     * @return String - the profile avatar picture url, null if no picture can
     *  be found.
     */
    public static String updateMeProfile(final DatabaseHelper dbHelper, final Contact currentMeProfile,
            final UserProfile profileChanges) {
        if (processMyContactChanges(dbHelper, currentMeProfile, profileChanges) == ServiceStatus.SUCCESS) {
            return processMyContactDetailsChanges(dbHelper, currentMeProfile, profileChanges);
        }
        return null;
    }

    /**
     * This method stores the getMyChanges() response to database - contacts part.
     * @param dbHelper DatabaseHelper - database.
     * @param currentMeProfile Contact - me profile contact.
     * @param profileChanges UserProfile - the contact changes.
     * @return ServiceStatus - SUCCESS if the contact changes have been successfully processed stored. 
     */
    private static ServiceStatus processMyContactChanges(final DatabaseHelper dbHelper,
            final Contact currentMeProfile, final UserProfile profileChanges) {

        boolean profileChanged = false;
        if (profileChanges.userID != null) {
            currentMeProfile.userID = profileChanges.userID;
            profileChanged = true;
        }
        if (profileChanges.aboutMe != null) {
            currentMeProfile.aboutMe = profileChanges.aboutMe;
            profileChanged = true;
        }
        if (profileChanges.contactID != null) {
            currentMeProfile.contactID = profileChanges.contactID;
            profileChanged = true;
        }
        if (profileChanges.gender != null) {
            currentMeProfile.gender = profileChanges.gender;
            profileChanged = true;
        }
        if (profileChanges.profilePath != null) {
            currentMeProfile.profilePath = profileChanges.profilePath;
            profileChanged = true;
        }
        if (profileChanges.sources != null) {
            currentMeProfile.sources.clear();
            currentMeProfile.sources.addAll(profileChanges.sources);
            profileChanged = true;
        }
        if (profileChanges.updated != null) {
            currentMeProfile.updated = profileChanges.updated;
            profileChanged = true;
        }
        if (profileChanged) {
            ArrayList<Contact> contactList = new ArrayList<Contact>();
            contactList.add(currentMeProfile);
            return dbHelper.syncModifyContactList(contactList, false, false);
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * This method stores the getMyChanges() response to database - details part.
     * @param dbHelper DatabaseHelper - database.
     * @param currentMeProfile Contact - me profile contact.
     * @param profileChanges UserProfile - the contact changes.
     * @return ServiceStatus - SUCCESS if the contact changes have been successfully processed stored. 
     */
    private static String processMyContactDetailsChanges(final DatabaseHelper dbHelper,
            final Contact currentMeProfile, final UserProfile profileChanges) {
        String ret = null;
        final ArrayList<ContactDetail> modifiedDetailList = new ArrayList<ContactDetail>();
        final ArrayList<ContactDetail> addedDetailList = new ArrayList<ContactDetail>();
        final ArrayList<ContactDetail> deletedDetailList = new ArrayList<ContactDetail>();

        for (ContactDetail newDetail : profileChanges.details) {
            boolean found = false;
            for (int i = 0; i < currentMeProfile.details.size(); i++) {
                ContactDetail oldDetail = currentMeProfile.details.get(i);
                if (DatabaseHelper.doDetailsMatch(newDetail, oldDetail)) {
                    found = true;
                    if (newDetail.deleted != null && newDetail.deleted.booleanValue()) {
                        deletedDetailList.add(oldDetail);
                    } else if (DatabaseHelper.hasDetailChanged(oldDetail, newDetail)) {
                        newDetail.localDetailID = oldDetail.localDetailID;
                        newDetail.localContactID = oldDetail.localContactID;
                        newDetail.nativeContactId = oldDetail.nativeContactId;
                        newDetail.nativeDetailId = oldDetail.nativeDetailId;
                        modifiedDetailList.add(newDetail);

                        if (newDetail.key == DetailKeys.PHOTO) {
                            dbHelper.markMeProfileAvatarChanged();
                            ret = newDetail.value;
                        }
                    }
                    break;
                }
            }
            if (!found) {
                newDetail.localContactID = currentMeProfile.localContactID;
                newDetail.nativeContactId = currentMeProfile.nativeContactId;
                
                if (newDetail.key == DetailKeys.PHOTO) {
                    dbHelper.markMeProfileAvatarChanged();
                    ret = newDetail.value;
                }
                addedDetailList.add(newDetail);
            }
        }
        if (!addedDetailList.isEmpty()) {
            dbHelper.syncAddContactDetailList(addedDetailList, false, false);
        }
        if (!modifiedDetailList.isEmpty()) {
            dbHelper.syncModifyContactDetailList(modifiedDetailList, false, false);
        }
        if (!deletedDetailList.isEmpty()) {
            dbHelper.syncDeleteContactDetailList(deletedDetailList, false, false);
        }
        return ret;

    }

    /**
     * The utility method to save Contacts/setMe() response to the database...
     * @param dbHelper Database - database
     * @param uploadedMeProfile Contact - me profile which has been uploaded in
     *            Contacts/setMe() call
     * @param result ContactChanges - the contents of response Contacts/setMe().
     *            The contact details in response need to be in the same order
     *            as they were in setMe() request
     */
    public static void updateMeProfileDbDetailIds(final DatabaseHelper dbHelper,
            final ArrayList<ContactDetail> uploadedDetails, final ContactChanges result) {

        Contact uploadedMeProfile = new Contact();
        SyncMeDbUtils.fetchMeProfile(dbHelper, uploadedMeProfile);
        boolean changed = false;
        if (result.mUserProfile.userID != null
                && !result.mUserProfile.userID.equals(uploadedMeProfile.userID)) {
            uploadedMeProfile.userID = result.mUserProfile.userID;
            changed = true;
        }
        if (result.mUserProfile.contactID != null
                && !result.mUserProfile.contactID.equals(uploadedMeProfile.contactID)) {
            uploadedMeProfile.contactID = result.mUserProfile.contactID;
            changed = true;
        }
        if (changed) {
            dbHelper.modifyContactServerId(uploadedMeProfile.localContactID,
                    uploadedMeProfile.contactID, uploadedMeProfile.userID);
        }
        ListIterator<ContactDetail> destIt = uploadedDetails.listIterator();

        for (ContactDetail srcDetail : result.mUserProfile.details) {
            if (!destIt.hasNext()) {
                LogUtils
                        .logE("SyncMeDbUtils updateProfileDbDetailsId() - # of details in response > # in request");
                return;
            }
            final ContactDetail destDetail = destIt.next();
            if (srcDetail.key == null || !srcDetail.key.equals(destDetail.key)) {
                LogUtils.logE("SyncMeDbUtils updateProfileDbDetailsId() - details order is wrong");
                break;
            }
            
            destDetail.unique_id = srcDetail.unique_id;
            dbHelper.syncContactDetail(destDetail.localDetailID, destDetail.unique_id);
        }
    }

    /**
     * The utility method to save the status text change to the database...
     * @param dbHelper DatabaseHelper - database
     * @param statusText String - status text
     * @return ContactDetail - the modified or created ContactDetail with key
     *         ContactDetail.DetailKeys.PRESENCE_TEXT
     */
    public static ContactDetail updateStatus(final DatabaseHelper dbHelper, final String statusText) {
        Contact meContact = new Contact();
        if (fetchMeProfile(dbHelper, meContact) == ServiceStatus.SUCCESS) {
            // try to modify an existing detail
            for (ContactDetail detail : meContact.details) {
                if (detail.key == ContactDetail.DetailKeys.PRESENCE_TEXT) {
                    detail.value = statusText;
                    // Currently it's only possible to post a status on
                    // Vodafone sns
                    detail.alt = ThirdPartyAccount.SNS_TYPE_VODAFONE;
                    if (ServiceStatus.SUCCESS == dbHelper.modifyContactDetail(detail)) {
                        return detail;
                    }
                }
            }
            // create a new detail instead
            ContactDetail contactDetail = new ContactDetail();
            contactDetail.setValue(statusText, ContactDetail.DetailKeys.PRESENCE_TEXT, null);
            contactDetail.alt = ThirdPartyAccount.SNS_TYPE_VODAFONE;
            contactDetail.localContactID = meContact.localContactID;
            if (ServiceStatus.SUCCESS == dbHelper.addContactDetail(contactDetail)) {
                return contactDetail;
            }
        }
        return null;
    }

    /**
     * The utility method to save Contacts/setMe() response for the status text
     * change to the database...
     * @param dbHelper DatabaseHelper - database.
     * @param ContactChanges result - status text change.
     */
    public static void savePresenceStatusResponse(final DatabaseHelper dbHelper, ContactChanges result) {
        Contact currentMeProfile = new Contact();
        if (ServiceStatus.SUCCESS == SyncMeDbUtils.fetchMeProfile(dbHelper, currentMeProfile)) {
            boolean changed = false;
            if (result.mUserProfile.userID != null
                    && (!result.mUserProfile.userID.equals(currentMeProfile.userID))) {
                currentMeProfile.userID = result.mUserProfile.userID;
                changed = true;
            }
            if (result.mUserProfile.contactID != null
                    && (!result.mUserProfile.contactID.equals(currentMeProfile.contactID))) {
                currentMeProfile.contactID = result.mUserProfile.contactID;
                changed = true;
            }
            if (changed) {
                dbHelper.modifyContactServerId(currentMeProfile.localContactID,
                        currentMeProfile.contactID, currentMeProfile.userID);
            }
            
            for (ContactDetail oldStatus : currentMeProfile.details) {
                if (oldStatus.key == ContactDetail.DetailKeys.PRESENCE_TEXT) {
                    for (ContactDetail newStatus : result.mUserProfile.details) {
                        if (newStatus.key == ContactDetail.DetailKeys.PRESENCE_TEXT) {
                            oldStatus.unique_id = newStatus.unique_id;
                            dbHelper.syncContactDetail(oldStatus.localDetailID, oldStatus.unique_id);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * A utility method to save the Me Profile contact before sending the
     * updates to backend
     * 
     * @param dbHelper DataBaseHelper - database
     * @param meProfile - the new me Profile to push to server
     * @return - ArrayList of ContactDetails to be pushed to server
     */
    public static ArrayList<ContactDetail> saveContactDetailChanges(final DatabaseHelper dbHelper,
            final Contact meProfile) {
        ArrayList<ContactDetail> updates = new ArrayList<ContactDetail>();
        populateWithModifiedDetails(dbHelper, updates, meProfile);

        // add the deleted details from the change log table to the contact
        // details
        populateWithDeletedContactDetails(dbHelper, updates, meProfile.contactID);
        return updates;
    }

    private static void populateWithModifiedDetails(final DatabaseHelper dbHelper,
            final ArrayList<ContactDetail> updates, Contact meProfile) {
        boolean avatarChanged = dbHelper.isMeProfileAvatarChanged();
        for (ContactDetail detail : meProfile.details) {
            // LogUtils.logV("meProfile.details:" + detail);
            if (avatarChanged && detail.key == ContactDetail.DetailKeys.PHOTO) {
                populatePhotoDetail(dbHelper, meProfile, detail);
                updates.add(detail);

            } else if (detail.key != ContactDetail.DetailKeys.VCARD_INTERNET_ADDRESS
                    && detail.key != ContactDetail.DetailKeys.VCARD_IMADDRESS
                    && detail.key != ContactDetail.DetailKeys.PRESENCE_TEXT) {
                // fix for bug 16029 - it's a server issue (getMe() returns
                // broken details) but the workaround on the client side is
                // just
                // not to add the extra details to setMe() request
                detail.updated = null;
                // LogUtils.logV("meProfile.details: put");
                updates.add(detail);
            }
        }
    }

    /**
     * This method reads a photo data from a file into the ContactDetail...
     * @param dbHelper DatabaseHelper - database 
     * @param meProfile Contact - me profile contact
     * @param detail ContactDetail - the detail to write the photo into.
     */
    private static void populatePhotoDetail(final DatabaseHelper dbHelper, final Contact meProfile,
            final ContactDetail detail) {
        String path = ThumbnailUtils.thumbnailPath(meProfile.localContactID);
        detail.photo = BitmapFactory.decodeFile(path);
        if (detail.photo == null) {
            LogUtils.logE("SyncMeDbUtils saveContactDetailChanges: " + "Unable to decode avatar");
        }
        detail.photo_mime_type = PHOTO_MIME_TYPE;
        // when sending the "bytes" the "val" (photoDetail.value)has to
        // be null, otherwise the the picture on the website is not
        // updated
        detail.value = null;
        detail.updated = null;
        detail.order = 0;
    }

    /**
     * This method adds the deleted details to the detail list sent to server...
     * @param dbHelper DatabaseHelper - database 
     * @param contactDetails List<ContactDetail> - the deleted details list 
     * @param contactId Long - Me Profile local contact id.
     */
    private static void populateWithDeletedContactDetails(final DatabaseHelper dbHelper,
            final List<ContactDetail> contactDetails, final Long contactId) {
        List<ContactChangeInfo> deletedDetails = new ArrayList<ContactChangeInfo>();
        if (!ContactChangeLogTable.fetchMeProfileChangeLog(deletedDetails,
                ContactChangeType.DELETE_DETAIL, dbHelper.getReadableDatabase(), contactId)) {
            LogUtils.logE("UploadServerContacts populateWithDeletedContactDetails -"
                    + " Unable to fetch contact changes from database");
            return;
        }
        for (int i = 0; i < deletedDetails.size(); i++) {
            ContactChangeInfo info = deletedDetails.get(i);
            final ContactDetail detail = new ContactDetail();

            detail.localDetailID = info.mLocalDetailId;
            detail.key = info.mServerDetailKey;
            detail.unique_id = info.mServerDetailId;
            detail.deleted = true;

            contactDetails.add(detail);
        }
        dbHelper.deleteContactChanges(deletedDetails);
    }

}
