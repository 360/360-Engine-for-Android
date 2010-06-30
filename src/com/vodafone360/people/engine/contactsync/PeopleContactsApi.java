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

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.DatabaseHelper.DatabaseChangeType;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.NativeChangeLogTable;
import com.vodafone360.people.database.tables.ContactsTable.ContactIdInfo;
import com.vodafone360.people.database.tables.NativeChangeLogTable.ContactChangeType;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;

/**
 * The PeopleContactsApi wrapper class of the People contacts database.
 * 
 * Modifying the People database by adding, modifying and deleting contacts
 * should be done via this class to ensure that the database remain consistent
 * across all the tables.
 * 
 * Note: this class is an attempt to separate the internal People contacts persistence from
 *       other components that need to access it (i.e. hiding its database, SQL tables and internals).
 *       It is not yet used by all the code base that would need to. 
 */
public class PeopleContactsApi {
    
    /**
     * Handler to the DatabaseHelper class.
     */
    private DatabaseHelper mDbh;
    
    /**
     * Array of ContactIdInfo.
     * 
     * Used for compatibility reasons with DatabaseHelper method and kept as a class member to avoid
     * frequent allocation.
     * @see DatabaseHelper#syncDeleteContactList(List, boolean, boolean)
     */
    private List<ContactsTable.ContactIdInfo> mContactIdInfoList = new ArrayList<ContactsTable.ContactIdInfo>(1);
    
    /**
     * Array of added ContactDetail.
     * 
     * Used for compatibility reasons with DatabaseHelper method and kept as a class member to avoid
     * frequent allocation.
     * @see DatabaseHelper#syncAddContactDetailList(List, boolean, boolean)
     */
    private ArrayList<ContactDetail> mAddedDetails = new ArrayList<ContactDetail>();
    
    /**
     * Array of updated ContactDetail.
     * 
     * Used for compatibility reasons with DatabaseHelper method and kept as a class member to avoid
     * frequent allocation.
     * @see DatabaseHelper#syncModifyContactDetailList(List, boolean, boolean)
     */
    private ArrayList<ContactDetail> mUpdatedDetails = new ArrayList<ContactDetail>();
    
    /**
     * Array of deleted ContactDetail.
     * 
     * Used for compatibility reasons with DatabaseHelper method and kept as a class member to avoid
     * frequent allocation.
     * @see DatabaseHelper#syncDeletedContactDetailList(List, boolean, boolean)
     */
    private ArrayList<ContactDetail> mDeletedDetails = new ArrayList<ContactDetail>();
    
    /**
     * Constructor.
     * 
     * @param dbh the DatabaseHelper to access the people database
     */
    public PeopleContactsApi(DatabaseHelper dbh) {
        mDbh = dbh;
    }
    
    /**
     * Gets the list of native contacts ids stored in the people database.
     * Contacts being deleted but still in the database will also be returned.
     * 
     * @return an array of native contacts ids
     */
    public long[] getNativeContactsIds() {
        
        try {
            
            final SQLiteDatabase db = mDbh.getReadableDatabase();
            
            // get the native ids from the Contacts table
            final long[] existingIds = ContactsTable.getNativeContactsIds(db);
            // get the deleted native ids form the Change log table
            final long[] deletedIds = NativeChangeLogTable.getDeletedContactsNativeIds(db);
            
            // merge both arrays to get the full list of native ids on People database side
            return mergeSortedArrays(existingIds, deletedIds);
        } catch (Exception e) {
            
            LogUtils.logE("getNativeContactsIds(), error: "+e);
        }
        
        return null;
    }
    
    /**
     * Gets an array of contacts people ids that need to be synced back to native.
     *
     * @return
     */
    public long[] getNativeSyncableContactIds() {
        
        long[] ids = null;
        
        try { 
            
            // get the "syncable to native" contacts
            ids = mDbh.getNativeSyncableContactsLocalIds();
            
        } catch (Exception e) {
            
            LogUtils.logE("getNativeSyncableContactsIds(), error: "+e);
        }
        
        return ids;
    }
    
    /**
     * Deletes a Contact in the people database from its native id.
     * 
     * Note: it assumes that the deletion comes from native as it sets flags
     *       to prevent syncing back to native
     * 
     * @param nativeId the native id of the contact to delete
     * @param syncToNative true if the deletion has to be propagated to native, false otherwise
     */
    public boolean deleteNativeContact(long nativeId, boolean syncToNative) {
        
        try {
            
            final SQLiteDatabase readableDb = mDbh.getReadableDatabase();
            
            ContactsTable.ContactIdInfo info = ContactsTable.fetchContactIdFromNative((int)nativeId, readableDb);
            
            if (info != null) {
                mContactIdInfoList.clear();
                mContactIdInfoList.add(info);
                info.nativeId = (int)nativeId;
                ServiceStatus status = mDbh.syncDeleteContactList(mContactIdInfoList, true, syncToNative);
                if (ServiceStatus.SUCCESS == status) {
                    
                    // TODO: Throttle the event
                    mDbh.fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, true);
                    return true;
                }
            }
        } catch (Exception e) {
            
            LogUtils.logE("deleteNativeContact("+nativeId+"), error: "+e);
        }
        
        return false;
    }
    
    /**
     * Adds a native contact to the people database.
     * 
     * Note: it assumes that the new contact comes from native as it sets flags
     *       to prevent syncing back to native
     * 
     * @param contact the ContactChange array representing the contact to add
     * @return true if successful, false otherwise
     */
    public boolean addNativeContact(ContactChange[] contact) {
        
        final boolean result = mDbh.addNativeContact(contact);
        
        // TODO: Throttle the event
        if (result) mDbh.fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, true);
        
        return result;
    }
    
    /**
     * Updates a native contact in the people database.
     * 
     * Note: it assumes that the changes come from native as it sets flags
     *       to prevent syncing back to native
     * 
     * @param contact the contact changes to apply to the contact
     */
    public void updateNativeContact(ContactChange[] contact) {
        
        mAddedDetails.clear();
        mDeletedDetails.clear();
        mUpdatedDetails.clear();
        
        for(int i = 0; i < contact.length; i++) {
            
            final ContactChange change = contact[i];
            // convert the ContactChange into a ContactDetail
            final ContactDetail detail = mDbh.convertContactChange(change);
            final int type = change.getType();
            
            switch(type) {
                case ContactChange.TYPE_ADD_DETAIL:
                    mAddedDetails.add(detail);
                    break;
                case ContactChange.TYPE_DELETE_DETAIL:
                    mDeletedDetails.add(detail);
                    break;
                case ContactChange.TYPE_UPDATE_DETAIL:
                    mUpdatedDetails.add(detail);
                    break;
            }
        }
        
        if (mAddedDetails.size() > 0) {
            mDbh.syncAddContactDetailList(mAddedDetails, true, false);
        }
        if (mDeletedDetails.size() > 0) {
            mDbh.syncDeleteContactDetailList(mDeletedDetails, true, false);
        }
        if (mUpdatedDetails.size() > 0) {
            mDbh.syncModifyContactDetailList(mUpdatedDetails, true, false);
        }
        
        // TODO: Throttle the event
        mDbh.fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, true);
    }
    
    /**
     * Gets a contact from its native id.
     * 
     * @param nativeId the native id of the contact
     * @return an array of ContactChange representing the contact, null if not found
     */
    public ContactChange[] getContact(long nativeId) {
        
        try {
            
            final SQLiteDatabase readableDb = mDbh.getReadableDatabase();
        
            if (NativeChangeLogTable.isContactChangeInList(nativeId, ContactChangeType.DELETE_CONTACT, readableDb)) {
                
                // the contact exists as a deleted contact in the NativeChangeLogTable
                // return one ContactChange with the deleted contact flag
                ContactChange[] changes = new ContactChange[1];
                changes[0] = new ContactChange(ContactChange.TYPE_DELETE_CONTACT);
                return changes;
            } else {
                
                // get the corresponding local contact id
                final ContactIdInfo info = ContactsTable.fetchContactIdFromNative((int)nativeId, readableDb);
                
                if (info != null) {
                    
                    // we found the contact on CAB, let's get the details
                    final ContactChange[] existingDetails = ContactDetailsTable.getContactChanges((long)info.localId, false, readableDb);
                    
                    // get also the deleted details if any
                    final ContactChange[] deletedDetails = NativeChangeLogTable.getDeletedDetails((long)info.localId, readableDb);
                    
                    if (existingDetails != null && deletedDetails != null) {
                        
                        // merge both arrays
                        ContactChange[] mergedDetails = new ContactChange[existingDetails.length + deletedDetails.length];
                        
                        System.arraycopy(existingDetails, 0, mergedDetails, 0, existingDetails.length);
                        System.arraycopy(deletedDetails, 0, mergedDetails, existingDetails.length, deletedDetails.length);
                        
                        return mergedDetails;
                    } else if (existingDetails != null) {
                        
                        return existingDetails;
                    } else {
                        
                        return deletedDetails;
                    }
                }
            }
        } catch (Exception e) {
            
            LogUtils.logE("getContact("+nativeId+"), error: "+e);
        }
        
        // no contact found
        return null;
    }
    
    /**
     * Gets the syncable changes for a contact (i.e. the details not synced yet to native).
     * 
     * @param localId the localId of the contact
     * @return an array of ContactChange that need to be synced to native
     */
    public ContactChange[] getNativeSyncableContactChanges(long localId) {
        
        // 3 types of changes: new contact, deleted contact, updated contact (new details / removed details / updated detail)
        ContactChange[] changes = null;
        
        try {
            
            final SQLiteDatabase readableDb = mDbh.getReadableDatabase();
            long nativeId;

            if ((nativeId = NativeChangeLogTable.getDeletedContactNativeId(localId, readableDb)) != -1) {  
                // the contact exists as a deleted contact in the NativeChangeLogTable
                // return one ContactChange with the deleted contact flag
                
                changes = new ContactChange[1];
                changes[0] = new ContactChange(ContactChange.TYPE_DELETE_CONTACT);
                changes[0].setNabContactId(nativeId);
                changes[0].setInternalContactId(localId);
            } else if ((nativeId = ContactsTable.getNativeContactId(localId, readableDb)) == -1) {
            
                // the contact is new on native side
                changes = ContactDetailsTable.getContactChanges(localId, false, readableDb);
                if (changes != null) {
                    
                    changes[0].setType(ContactChange.TYPE_ADD_CONTACT);
                }
            } else {
            
                // the contact needs to be updated
                final ContactChange[] newOrUpdated = ContactDetailsTable.getContactChanges(localId, true, readableDb);
                final ContactChange[] deleted = NativeChangeLogTable.getDeletedDetails(localId, readableDb);
                
                if (nativeId == ContactChange.INVALID_ID) {
                    LogUtils.logE("getNativeSyncableContactChanges(), the native contact id shall not be invalid!");
                }
                
                // set the native contact id to the new contact changes (only updated or deleted ones have an id already)
                setNativeContactId(newOrUpdated, nativeId);
                
                if (newOrUpdated != null && deleted != null) {
                    
                    // merge needed
                    changes = new ContactChange[newOrUpdated.length + deleted.length];
                    System.arraycopy(newOrUpdated, 0, changes, 0, newOrUpdated.length);
                    System.arraycopy(deleted, 0, changes, newOrUpdated.length, deleted.length);
                } else if (newOrUpdated != null) {
                    
                    changes = newOrUpdated;
                } else {
                    
                    changes = deleted;
                }
            }
        } catch(Exception e) {
            
            LogUtils.logE("getNativeSyncableContactChanges(), error: "+e);
        }
        
        return changes;
    }
    
    /**
     * Sets the native contact id to the new details.
     * 
     * @param changes the array of changes where to set missing native contact id
     * @param nativeContactId the native contact id to set
     */
    private void setNativeContactId(ContactChange[] changes, long nativeContactId) {
        
        if (changes != null) {
            
            final int count = changes.length; 
            
            for (int i = 0; i < count; i++) {
                
                final ContactChange change = changes[i];
                if (change.getNabContactId() == ContactChange.INVALID_ID) {
                    change.setNabContactId(nativeContactId);
                }
            }
        }
    }
    
    /**
     * Sets the native ids to the people database since the contact has been added to native.
     * 
     * @param contact the array of ContactChange representing the new contact the contact
     * @param nativeIds the array of ContactChange containing the native ids for the added contact
     * @return true if successful, false otherwise
     */
    public boolean syncBackNewNativeContact(ContactChange[] contact, ContactChange[] nativeIds) {
        
        // set the native ids to Contacts, ContactsSummary and ContactDetails tables
        
        if (nativeIds == null
         || (nativeIds.length != contact.length + 1)) {

            // we expect an array containing +1 elements as the first element contains the
            // new native contact id
            return false;
        }
        
        SQLiteDatabase writableDb = null;
        
        try {
            
            writableDb = mDbh.getWritableDatabase();
            writableDb.beginTransaction();
            
            if (nativeIds[0] != null) {
                
                // nativeIds[0] shall not be null
                final long localContactId = nativeIds[0].getInternalContactId();
                final long nativeContactId = nativeIds[0].getNabContactId();
                
                if (ContactsTable.setNativeContactId(localContactId, nativeContactId, writableDb)) {
                    
                    if (ContactSummaryTable.setNativeContactId(localContactId, nativeContactId, writableDb)) {
                        
                        final int length = contact.length;
                        for (int i = 0; i < length; i++) {
                            
                            final long localDetailId = contact[i].getInternalDetailId();
                            
                            // some details have no native ids as unique
                            // in that case, we set them with the native contact id
                            // the +1 is because we have to skip the first ContactChange that is here only for getting the nativeContactId
                            final long nativeDetailId;
                            
                            if (nativeIds[i+1] == null
                             || nativeIds[i+1].getNabDetailId() == ContactChange.INVALID_ID) {
                                
                                nativeDetailId = nativeContactId;
                            } else {
                                
                                nativeDetailId = nativeIds[i+1].getNabDetailId();
                            }
                            
                            if (!ContactDetailsTable.setDetailSyncedWithNative(localDetailId, nativeContactId, nativeDetailId, true, writableDb)) {
                                
                                return false;
                            }
                        }
                        
                        writableDb.setTransactionSuccessful();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            
            LogUtils.logE("PeopleContactApi.syncBackNewNativeContact() - Error: " + e);
        } finally {
            
            if (writableDb != null) {
                writableDb.endTransaction();
                writableDb = null;
            }
        }
        
        return false;
    }

    /**
     * Acknowledges the people database that the native side deleted the contact as requested.
     * 
     * @param deletedContact the ContactChange of the deleted contact
     */
    public boolean syncBackDeletedNativeContact(ContactChange deletedContact) {
        
        SQLiteDatabase writableDb = null;

        try {
            
            writableDb = mDbh.getWritableDatabase();
            writableDb.beginTransaction();
            
            if (NativeChangeLogTable.removeContactChanges(deletedContact.getInternalContactId(), writableDb)) {
                
                // the contact is completely removed on native side,
                // we can now remove it from the native change log table
                writableDb.setTransactionSuccessful();
                return true;
            }
            
        } catch (Exception e) {
            
            LogUtils.logE("PeopleContactApi.syncBackDeletedNativeContact() - Error: " + e);
        } finally {
            
            if (writableDb != null) {
                writableDb.endTransaction();
                writableDb = null;
            }
        }
        
        return false;
    }
    
    /**
     * Sets the native ids to the people database for the added details on native side and
     * removes the deleted details from the people database.
     * 
     * @param contact the array of ContactChange representing the updates that where performed on the contact
     * @param nativeIds the array of ContactChange containing the native ids for the added details
     * @return true if successful, false otherwise
     */
    public boolean syncBackUpdatedNativeContact(ContactChange[] contact, ContactChange[] nativeIds) {
        
        if (nativeIds == null
         || nativeIds.length != contact.length) {

            // we expect an array with exactly the same size
            return false;
        }
        
        SQLiteDatabase writableDb = null;
        
        try {
            
            writableDb = mDbh.getWritableDatabase();
            writableDb.beginTransaction();
            
            final int length = nativeIds.length;
            
            for (int i = 0; i < length; i++) {
                
                final ContactChange change = contact[i];
                final int type = change.getType();
                final long localDetailId = contact[i].getInternalDetailId();

                switch(type) {
                    case ContactChange.TYPE_ADD_DETAIL:
                        // some details have no native detail ids because unique or not supported
                        // in that case, we set them with the native contact id
                        final long nativeContactId = contact[0].getNabContactId();
                        final long nativeDetailId;
                        
                        if (nativeIds[i] == null
                         || nativeIds[i].getNabDetailId() == ContactChange.INVALID_ID) {
                            
                            nativeDetailId = nativeContactId;
                        } else {
                            
                            nativeDetailId = nativeIds[i].getNabDetailId();
                        }
                        
                        if (!ContactDetailsTable.setDetailSyncedWithNative(localDetailId, nativeContactId, nativeDetailId, true, writableDb)) {
                            LogUtils.logE("PeopleContactApi.syncBackUpdatedNativeContact() - error while adding a detail");
                            return false;
                        }
                        break;
                    case ContactChange.TYPE_UPDATE_DETAIL:
                        // we set the native ids as -1 because we don't need them in that case (update, not an add that generates new ids)
                        if (!ContactDetailsTable.setDetailSyncedWithNative(localDetailId, -1, -1, false, writableDb)) {
                            LogUtils.logE("PeopleContactApi.syncBackUpdatedNativeContact() - error while updating a detail");
                            return false;
                        }
                        break;
                    case ContactChange.TYPE_DELETE_DETAIL:
                        // detail removed on native side, finally remove it's deleted log from people database
                        if (!NativeChangeLogTable.removeContactDetailChanges(localDetailId, writableDb)) {
                            LogUtils.logE("PeopleContactApi.syncBackUpdatedNativeContact() - error while deleting a detail");
                            return false;
                        }
                        break;
                }
            }
            
            writableDb.setTransactionSuccessful();
            return true;

            
        } catch (Exception e) {
            
            LogUtils.logE("PeopleContactApi.syncBackUpdatedNativeContact() - Error: " + e);
        } finally {
            
            if (writableDb != null) {
                writableDb.endTransaction();
                writableDb = null;
            }
        }
        
        return false;
    }
    
    /**
     * Merges two sorted arrays in one sorted array.
     * 
     * @param array1 the first sorted array
     * @param array2 the second sorted array
     * @return a sorted array that contains array1 and array2
     */
    private long[] mergeSortedArrays(long[] array1, long[] array2) {
        
        // easy cases
        if ((array1 == null) && (array2 == null)) {
             
            return null;
        } else if (array1 == null) {
            
            return array2;
        } else if (array2 == null) {
            
            return array1;
        }
        
        // interesting case, perform the merge
        long[] merged = new long[array1.length + array2.length];
        int index1 = 0;
        int index2 = 0;
        
        for (int i = 0; i < merged.length; i++) {
            
            if (index1 == array1.length) {
                
                System.arraycopy(array2, index2, merged, i, array2.length - index2);
                break;
            } else if (index2 == array2.length) {
                
                System.arraycopy(array1, index1, merged, i, array1.length - index1);
                break;
            } else if (array1[index1] < array2[index2]) {
                
                merged[i] = array1[index1++];
            } else {
                
                merged[i] = array2[index2++];
            }
        }
        
        return merged;
    }
}
