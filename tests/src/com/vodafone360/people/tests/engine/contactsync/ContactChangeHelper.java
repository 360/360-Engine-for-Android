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

package com.vodafone360.people.tests.engine.contactsync;

import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.text.TextUtils;
import android.util.Log;

import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.datatypes.VCardHelper.Name;
import com.vodafone360.people.datatypes.VCardHelper.Organisation;
import com.vodafone360.people.datatypes.VCardHelper.PostalAddress;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.utils.VersionUtils;

public class ContactChangeHelper {

	private static final int MAX_PHONE_NUMBERS_PER_CONTACT = 5;
	private static final int MAX_EMAILS_PER_CONTACT = 5;
	private static final int MAX_ADDRESSES_PER_CONTACT = 3;
	private static final int MAX_ORGS_PER_CONTACT = 1;
	private static final int MAX_TITLES_PER_CONTACT = 1;
	private static final int MAX_WEBSITES_PER_CONTACT = 1;
	private static final int MAX_BIRTHDAYS_PER_CONTACT = 1;
	private static final int MAX_NOTES_PER_CONTACT = 1;
	private static final int MAX_NAMES_PER_CONTACT = 1;
	private static final int MAX_NICKNAMES_PER_CONTACT = 1;
	
	private static final int MAX_NUM_DETAILS_ADD = 10;
	
	private static final HashMap<Integer, Integer> sKeyMaxNumMap;
	
	private static final int[] sKeyArray;
	
	static {
		sKeyArray = new int[9];
		sKeyArray[0] = ContactChange.KEY_VCARD_NAME;
		sKeyArray[1] = ContactChange.KEY_VCARD_NICKNAME;
		sKeyArray[2] = ContactChange.KEY_VCARD_PHONE;
		sKeyArray[3] = ContactChange.KEY_VCARD_EMAIL;
		sKeyArray[4] = ContactChange.KEY_VCARD_ADDRESS;
		sKeyArray[5] = ContactChange.KEY_VCARD_ORG;
		sKeyArray[6] = ContactChange.KEY_VCARD_TITLE;
		sKeyArray[7] = ContactChange.KEY_VCARD_URL;
		sKeyArray[8] = ContactChange.KEY_VCARD_NOTE;
		
		sKeyMaxNumMap = new HashMap<Integer, Integer>();
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_NAME, MAX_NAMES_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_NICKNAME, MAX_NICKNAMES_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_PHONE, MAX_PHONE_NUMBERS_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_EMAIL, MAX_EMAILS_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_ADDRESS, MAX_ADDRESSES_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_ORG, MAX_ORGS_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_TITLE, MAX_TITLES_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_URL, MAX_WEBSITES_PER_CONTACT);
		sKeyMaxNumMap.put(ContactChange.KEY_VCARD_NOTE, MAX_NOTES_PER_CONTACT);
	}
	
    private static Random sRn = new Random();
    
    
    private static final String[] mNameTitleList = {
    	"Mr.",
    	"Mrs.",
    	"Miss",
    	"Prof.",
    	"Dr.",
    	"Eng."
    };
    
    public static void printContactChange(ContactChange cc) {
    	Log.i("CC", "--------------- START ---------------");
    	Log.d("CC",						   
		"Key:"+cc.getKeyToString()+"("+cc.getKey()+")\n"+
		"Value:"+cc.getValue()+"\n"+
		"Flags:"+cc.getFlags()+"\n"+
		"Type:"+cc.getType()+"\n"+
		"IDS(IntCntID:"+cc.getInternalContactId()+
			", IntDtlID:"+cc.getInternalDetailId()+
			", BckndCntID:"+cc.getBackendContactId()+
			", BckndDtlID:"+cc.getBackendDetailId()+
			", NABCntID:"+cc.getNabContactId()+
			", NABDtlID:"+cc.getNabDetailId()+")");
    	Log.i("CC", "---------------  END  --------------");
    }
    
	public static void printContactChangeList(ContactChange[] ccList) {
		final int length = ccList.length;
		Log.i("CCLIST", "############### START ###############");
		for(int i = 0; i < length; i++) {
			final ContactChange cc = ccList[i];
			printContactChange(cc);
		}
		
		Log.i("CCLIST", "###############  END  ###############");
	}
	
	public static boolean areChangesEqual(ContactChange a, ContactChange b, boolean compareAll) {
		if(a.getKey() != b.getKey()) {
			return false;
		}
		
		// just for convenience
		final int key = a.getKey();
		
		if(!VersionUtils.is2XPlatform() && key == ContactChange.KEY_VCARD_NAME) {
			// Need to convert to raw string or else we can't compare them because of NAB 1.X limitations
			final Name nameA = VCardHelper.getName(a.getValue());
			final Name nameB = VCardHelper.getName(b.getValue());
			final String nameAStr = nameA.toString();
			final String nameBStr = nameB.toString();
			if(!TextUtils.equals(nameAStr, nameBStr)) {
				return false;
			}
		} else if(!VersionUtils.is2XPlatform() && key == ContactChange.KEY_VCARD_ADDRESS) {
			// Need to convert to raw string or else we can't compare them because of NAB 1.X limitations
			final PostalAddress addressA = VCardHelper.getPostalAddress(a.getValue());
			final PostalAddress addressB = VCardHelper.getPostalAddress(b.getValue());
			// Need to also remove \n's that were put there by .toString
			final String addressAStr = addressA.toString().replaceAll("\n", "");
			final String addressBStr = addressB.toString().replaceAll("\n", "");
			
			if(!TextUtils.equals(addressAStr, addressBStr)) {
				return false;
			}			
		} else if(!VersionUtils.is2XPlatform() && key == ContactChange.KEY_VCARD_ORG) {
			// Org on 1.X does not support department, need to NOT compare that
			final Organisation orgA = VCardHelper.getOrg(a.getValue());
			final Organisation orgB = VCardHelper.getOrg(b.getValue());
			if(!TextUtils.equals(orgA.name, orgB.name)) {
				return false;
			}
		} else if(!TextUtils.equals(a.getValue(), b.getValue())) {
			return false;
		}


		
		switch(key) {
			case ContactChange.KEY_VCARD_ORG:
			case ContactChange.KEY_VCARD_TITLE:
			case ContactChange.KEY_VCARD_PHONE:
			case ContactChange.KEY_VCARD_EMAIL:
			case ContactChange.KEY_VCARD_ADDRESS:
				// NAB may have changed these fields to preferred even though they were inserted as not preferred! 
				final int flagsWithoutPreferredA = a.getFlags() & ~ContactChange.FLAG_PREFERRED;
				final int flagsWithoutPreferredB = b.getFlags() & ~ContactChange.FLAG_PREFERRED;		
				if(flagsWithoutPreferredA != flagsWithoutPreferredB) {
					return false;
				}
				break;
			default:
				if(a.getFlags() != b.getFlags()) {
					return false;
				}
				break;
		}
		
		if(VersionUtils.is2XPlatform() && a.getFlags() != b.getFlags()) {
			return false;
		} else if(!VersionUtils.is2XPlatform()) {
		}
		
		if(compareAll) {
			if(a.getType() != b.getType()) {
				return false;
			}
			
			if(a.getInternalContactId() != b.getInternalContactId()) {
				return false;
			}
			
			if(a.getInternalDetailId() != b.getInternalDetailId()) {
				return false;
			}
			
			if(a.getBackendContactId() != b.getBackendContactId()) {
				return false;
			}
			
			if(a.getBackendDetailId() != b.getBackendDetailId()) {
				return false;
			}
			
			if(a.getNabContactId() != b.getNabContactId()) {
				return false;
			}
			
			if(a.getNabDetailId() != b.getNabDetailId()) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean areChangeListsEqual(ContactChange[] a, 
			ContactChange[] b, boolean compareAll) {
		final int aLength = a.length;
		final int bLength = b.length;
		
		if(aLength != bLength) {
			printContactChangeList(a);
			printContactChangeList(b);
			return false;
		}
		for(int i = 0; i < aLength; i++) {
			final ContactChange ccA = a[i];
			final ContactChange ccB = b[i];
			if(!areChangesEqual(ccA, ccB, compareAll)) {
				Log.e("CC COMPARISON", "Contact Change comparison mismatch, ContactChange A to follow:");
				printContactChange(ccA);
				Log.e("CC COMPARISON", "Contact Change comparison mismatch, ContactChange B to follow:");
				printContactChange(ccB);
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean areUnsortedChangeListsEqual(ContactChange[] a, ContactChange[] b, boolean compareAll) {
		final int aLength = a.length;
		final int bLength = b.length;
		
		if(aLength != bLength) {
			printContactChangeList(a);
			printContactChangeList(b);
			return false;
		}
		
		for(int i = 0; i < aLength; i++) {
			final ContactChange ccA = a[i];
			boolean matchFound = false;
			for(int j = 0; j < bLength; j++) {
				final ContactChange ccB = a[j];
				if(ccA.getKey() == ccB.getKey() && ccA.getNabDetailId() == ccB.getNabDetailId()) {
					if(areChangesEqual(ccA, ccB, compareAll)) {
						matchFound = true;
						break;
					}
				}
			}
			
			if(!matchFound) {
				Log.e("CONTACT COMPARISON", "Contact List Unsorted comparison because of mismatch, List A to follow:");
				printContactChangeList(a);
				Log.e("CONTACT COMPARISON", "Contact List Unsorted comparison because of mismatch, List B to follow:");
				printContactChangeList(b);
				return false;				
			}
		}
		
		return true;
	}
	
	public static boolean isUniqueKeyForContact(int key) {
		final Integer maxNum = sKeyMaxNumMap.get(key);
		return maxNum != null && maxNum == 1;
	}
    
    public static ContactChange[] randomContact(long internalContactId, long backendContactId, long nabContactId) {
    	final List<ContactChange> ccList = new ArrayList<ContactChange>();
    	int numNames = 0, 
    	numNicknames = 0, 
    	numEmails = 0, 
    	numPhones = 0, 
    	numAddresses = 0, 
    	numTitles = 0, 
    	numWebsites = 0, 
    	numNotes = 0, 
    	numOrgs = 0,
    	numBirthdays = 0;
    	
    	numNames = valueForLikelihoodPercentage(85, MAX_NAMES_PER_CONTACT);  // 85% likely
    	if(VersionUtils.is2XPlatform()) {
    		numNicknames = valueForLikelihoodPercentage(30, MAX_NICKNAMES_PER_CONTACT); // 30% likely
    		numBirthdays = valueForLikelihoodPercentage(25, MAX_BIRTHDAYS_PER_CONTACT); // 25% likely
    	}
    	numEmails = randomPositiveInt(MAX_EMAILS_PER_CONTACT);
    	// Always create at least a phone number so that an empty cc list is not possible
    	numPhones = 1 + randomPositiveInt(MAX_PHONE_NUMBERS_PER_CONTACT -1);
    	numAddresses = randomPositiveInt(MAX_ADDRESSES_PER_CONTACT);
    	numTitles = valueForLikelihoodPercentage(40, MAX_TITLES_PER_CONTACT);
    	if(VersionUtils.is2XPlatform()) {
    		numWebsites = valueForLikelihoodPercentage(35, MAX_WEBSITES_PER_CONTACT);
    	}
    	
    	numNotes = valueForLikelihoodPercentage(35, MAX_NOTES_PER_CONTACT); // 35% likely
    	numOrgs = valueForLikelihoodPercentage(40, MAX_ORGS_PER_CONTACT);

    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_NAME, 
    			internalContactId, backendContactId, nabContactId, numNames);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_NOTE, 
    			internalContactId, backendContactId, nabContactId, numNotes);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_NICKNAME, 
    			internalContactId, backendContactId, nabContactId, numNicknames);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_PHONE, 
    			internalContactId, backendContactId, nabContactId, numPhones);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_EMAIL, 
    			internalContactId, backendContactId, nabContactId, numEmails);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_DATE, 
    			internalContactId, backendContactId, nabContactId, numBirthdays);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_ADDRESS, 
    			internalContactId, backendContactId, nabContactId, numAddresses);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_URL, 
    			internalContactId, backendContactId, nabContactId, numWebsites);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_ORG, 
    			internalContactId, backendContactId, nabContactId, numOrgs);
    	generateRandomChangesForKey(ccList, ContactChange.KEY_VCARD_TITLE, 
    			internalContactId, backendContactId, nabContactId, numTitles);
    	
    	final int numOrgTitlePairs = Math.min(numOrgs, numTitles); 
    	if(numOrgTitlePairs > 0) {
    		final int ccListSize = ccList.size();
    		// Have to make the flags for each pair have the same values so that comparison is safe later
    		for(int i = 0; i < numOrgTitlePairs; i++) {
    			// get org flags
    			final int orgFlags =  ccList.get((ccListSize - (numOrgs + numTitles)) + i).getFlags();
    			// copy over flags from org to title
    			ccList.get(ccListSize - numOrgTitlePairs + i).setFlags(orgFlags);
    		}
    	}
    	
    	return ccList.toArray(new ContactChange[ccList.size()]);
    }
    
    public static ContactChange[] randomContactUpdate(ContactChange[] contact) {
    	final int originalDetailsNum = contact.length;
    	
    	final long internalCntId = contact[0].getInternalContactId();
    	final long backendCntId = contact[0].getBackendContactId();
    	final long nabCntId = contact[0].getNabContactId();
    	
    	int numDeletedDetails = 0,
    		curNumDeletedDetails = 0,
    		numAddedDetails = 0,
    		curNumAddedDetails = 0,
    		numChangedDetails = 0,
    		curNumChangedDetails = 0;
    	
    	// At best we can only delete all but one detail
    	numDeletedDetails = randomPositiveInt(originalDetailsNum - 1);
    	// At best we can only change all details minus the ones we will delete
    	numChangedDetails = randomPositiveInt(originalDetailsNum - numDeletedDetails);
    	// Reasonable maximum of details we can add
    	numAddedDetails = randomPositiveInt(MAX_NUM_DETAILS_ADD);
    	
    	final int updateCcListSize = numAddedDetails + numChangedDetails + numDeletedDetails;
    	ContactChange[] updateCcList = 
    		new ContactChange[updateCcListSize];
    	
    	for(int i = 0; i < updateCcListSize; i++) {
    		if(curNumChangedDetails < numChangedDetails) {
    			updateCcList[i] = generateUpdateDetailChange(contact[i]);
    			curNumChangedDetails++;
    			continue;
			}    			
    		if (curNumDeletedDetails < numDeletedDetails) {
    			updateCcList[i] = generateDeleteDetailChange(contact[i]);
    			curNumDeletedDetails++;
    			continue;
    		}
    		
    		if(curNumAddedDetails < numAddedDetails) {
    			int key;
    			do {
    				// get a random key and don't let it be a unique key to simplify things
    				key = randomKey();
    				
    				if(isKeyPlatformSupported(key)) {
    					if(!isUniqueKeyForContact(key) || 
    						(isUniqueKeyForContact(key) &&
    						!isKeyPresent(key, updateCcList) && 
    						!isKeyPresent(key, contact))) {
    						break;
    					}
    				}
    				
    			} while(true);
    			// The new detail, preferred flag not allowed to simplify things a little
    			final ContactChange addDetailCc = randomContactChange(key, internalCntId, backendCntId, nabCntId, false);
        		//addDetailCc.setInternalDetailId(randomPositiveLong());
    			addDetailCc.setType(ContactChange.TYPE_ADD_DETAIL);
    			updateCcList[i] = addDetailCc;

    			curNumAddedDetails++;			
    		}
    	}
    	    	
    	return updateCcList;
    }
    
    public static ContactChange[] generatedUpdatedContact(ContactChange[] contact, ContactChange[] update) {
    	if(update == null || update.length == 0) {
    		return contact;
    	}
    	
    	final List<ContactChange> updatedContact = new ArrayList<ContactChange>();
    	    	
    	final int contactSize = contact.length;
    	final int updateSize = update.length;
    	
    	for(int i = 0; i < contactSize; i++) {
    		ContactChange cc = contact[i];
    		int updateType = ContactChange.TYPE_UNKNOWN; 
    		if(updateSize > i) {
    			updateType = update[i].getType();
    		}
    		
    		if(updateType == ContactChange.TYPE_UPDATE_DETAIL) {
    			cc = update[i];
    		} else if(updateType == ContactChange.TYPE_DELETE_DETAIL) {
    			continue;
    		}
    		
    		updatedContact.add(cc);
		}
    	
    	for(int i = 0; i < updateSize; i++) {
    		final ContactChange cc = update[i];
    		//final int key = cc.getKey();
    		if(cc.getType() == ContactChange.TYPE_ADD_DETAIL) {
    			//updatedContact.add(lastIndexForKeyInCcList(key, updatedContact) + 1, cc);
    			// Since we are using areUnsortedChangeListsEqual to compare we can just add instead of above
    			updatedContact.add(cc);
    		}
    	}
    	
    	return updatedContact.toArray(new ContactChange[updatedContact.size()]);
    }

    /**
     * Finds the last index for the specified key.
     * This method uses recursion.
     */
    private static int lastIndexForKeyInCcList(int key, List<ContactChange> ccList) {
    	if(ccList == null || ccList.size() == 0) {
    		return -1;
    	}
    	
    	final int listSize = ccList.size();
    	for(int i = listSize - 1; i > -1; i--) {
    		if(ccList.get(i).getKey() == key) {
    			return i;
    		}
    	}
    	
    	// Yes, this is recursion...
    	switch(key) {
    		case ContactChange.KEY_VCARD_TITLE:
    			return lastIndexForKeyInCcList(ContactChange.KEY_VCARD_ORG, ccList);
    		case ContactChange.KEY_VCARD_ORG:
    			return lastIndexForKeyInCcList(ContactChange.KEY_VCARD_ADDRESS, ccList);
    		case ContactChange.KEY_VCARD_ADDRESS:
    			return lastIndexForKeyInCcList(ContactChange.KEY_VCARD_EMAIL, ccList);
    		case ContactChange.KEY_VCARD_EMAIL:
    			return lastIndexForKeyInCcList(ContactChange.KEY_VCARD_PHONE, ccList);
    		case ContactChange.KEY_VCARD_PHONE:
    			return lastIndexForKeyInCcList(ContactChange.KEY_VCARD_NICKNAME, ccList);
    		case ContactChange.KEY_VCARD_NICKNAME:
    			return lastIndexForKeyInCcList(ContactChange.KEY_VCARD_NOTE, ccList);
			case ContactChange.KEY_VCARD_NOTE:
				return lastIndexForKeyInCcList(ContactChange.KEY_VCARD_NAME, ccList);
    	}
    	
    	return -1;
    }
    
    public static boolean isKeyPresent(int key, ContactChange[] ccList) {
    	if(ccList == null || ccList.length == 0) {
    		return false;
    	}
    	
    	final int listSize = ccList.length;
    	for(int i = listSize - 1; i > -1; i--) {
    		if(ccList[i] != null && ccList[i].getKey() == key) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    private static boolean isKeyPlatformSupported(int key) {
    	switch (key) {
			case ContactChange.KEY_VCARD_TITLE:
			case ContactChange.KEY_VCARD_ORG:
			case ContactChange.KEY_VCARD_ADDRESS:
			case ContactChange.KEY_VCARD_EMAIL:
			case ContactChange.KEY_VCARD_PHONE:
			case ContactChange.KEY_VCARD_NOTE:
			case ContactChange.KEY_VCARD_NAME:
				return true;
			case ContactChange.KEY_VCARD_URL:
			case ContactChange.KEY_VCARD_NICKNAME:
				return VersionUtils.is2XPlatform();  	
    	}
    	
    	return false;
    }
    
    private static ContactChange generateUpdateDetailChange(ContactChange cc) {
    	if(cc == null) {
    		return null;
    	}
    		
    	final int key = cc.getKey();
    	boolean changed = false;
    	String value = cc.getValue();
    	int flags = cc.getFlags();
    	int newFlags = flags;
    	if(randomTrue()) {
    		value = randomValue(key);
    		changed = true;
    	}
    	
    	if(randomTrue()) {
    		flags = randomFlags(key, false);
    		if(newFlags != flags) {
    			changed = true;
    		}
    	}
    	
    	if(!changed) {
    		value = randomValue(key);
    	}
    	
    	final ContactChange updateDetailCc = new ContactChange(key, value, newFlags);
    	updateDetailCc.setType(ContactChange.TYPE_UPDATE_DETAIL);
    	updateDetailCc.setInternalContactId(cc.getInternalContactId());
    	updateDetailCc.setInternalDetailId(cc.getInternalDetailId());
    	updateDetailCc.setBackendContactId(cc.getBackendContactId());
    	updateDetailCc.setBackendDetailId(cc.getBackendDetailId());
    	updateDetailCc.setNabContactId(cc.getNabContactId());
    	updateDetailCc.setNabDetailId(cc.getNabDetailId());

    	return updateDetailCc;
    }
    
    private static ContactChange generateDeleteDetailChange(ContactChange cc) {
    	if(cc == null) {
    		return null;
    	}
    	final ContactChange deleteDetailCc = new ContactChange();
    	deleteDetailCc.setKey(cc.getKey());
    	deleteDetailCc.setType(ContactChange.TYPE_DELETE_DETAIL);
    	deleteDetailCc.setInternalContactId(cc.getInternalContactId());
    	deleteDetailCc.setInternalDetailId(cc.getInternalDetailId());
    	deleteDetailCc.setBackendContactId(cc.getBackendContactId());
    	deleteDetailCc.setBackendDetailId(cc.getBackendDetailId());
    	deleteDetailCc.setNabContactId(cc.getNabContactId());
    	deleteDetailCc.setNabDetailId(cc.getNabDetailId());
    	return deleteDetailCc;
    }
    
    private static void generateRandomChangesForKey(
    		List<ContactChange> ccList, int key, 
    		long internalContactId, long backendContactId, 
    		long nabContactId, int numChanges) {
    	boolean preferredSelected = false;
    	for(int i = 0; i < numChanges; i++) {
    		ContactChange cc = randomContactChange(key, internalContactId, backendContactId, nabContactId, !preferredSelected);
    		if(!preferredSelected) {
    			preferredSelected = 
    				(cc.getFlags() & ContactChange.FLAG_PREFERRED) == ContactChange.FLAG_PREFERRED; 
    		}
    		ccList.add(cc);
    	}
    }
    
    private static ContactChange randomContactChange(int key, 
    		long internalContactId, long backendContactId, long nabContactId, boolean allowPreferred) {
    	ContactChange cc = randomContactChange(key, allowPreferred);
    	cc.setInternalContactId(internalContactId);
    	cc.setBackendContactId(backendContactId);
    	cc.setNabContactId(nabContactId);
    	return cc;
    }
    
    private static ContactChange randomContactChange(int key, boolean allowPreferred) {
    	return new ContactChange(key, randomValue(key), randomFlags(key, allowPreferred));
    }
    
    private static int randomKey() {
    	return sKeyArray[randomPositiveInt(sKeyArray.length - 1)];
    }
    
    private static Name randomName() {
    	String firstname = null, 
    	surname = null, 
    	midname = null, 
    	suffix = null, 
    	title = null;
    	
    	if(randomTrue()) {
    		firstname = randomString();
    	}
    	
    	if(randomTrue()) {
    		surname = randomString();
    	}
    	
    	if(randomTrue()) {
    		midname = randomString();
    	}
    	
    	if(randomTrue()) {
    		if(randomTrue()) {
    			title = randomString();
    		} else {
    			title = mNameTitleList[randomPositiveInt(mNameTitleList.length)];
    		}
    	}
    	
    	if(randomTrue()) {
    		suffix = randomString();
    	}
    	
    	if(randomTrue()) {
    		firstname = randomString();
    	}
    	
		Name name = new Name();
		
		if(firstname == null && 
				surname == null) {
    		/* So that we have one part not null!
    		 * Note that we ignored middle name, suffix and title in the above check
    		 * The reason for doing so is that 2.X NAB cannot work with just middle name, prefix and suffix fields!
    		 */
			firstname = randomString();
		}

		name.firstname = firstname;
		name.midname = midname;
		name.surname = surname;
		name.title = title;
		name.suffixes = suffix;
	
    	return name;
	}
    
    
    private static PostalAddress randomAddress() {
    	String addressLine1 = null, 
    	addressLine2 = null,
    	city = null,
    	county = null,
    	pobox = null,
    	postcode = null,
    	country = null;
    	
    	if(randomTrue()) {
    		pobox = randomString();
    	}
    	
    	if(randomTrue()) {
    		addressLine1 = randomString();
    	}
    	
    	if(randomTrue()) {
    		addressLine2 = randomString();
    	}
    	
    	if(randomTrue()) {
    		city = randomString();
    	}
    	
    	if(randomTrue()) {
    		county = randomString();
    	}
    	
    	if(randomTrue()) {
    		postcode = randomNumbersString();
    	}
    	
    	if(randomTrue()) {
    		country = randomString();
    	}
    	
		PostalAddress address = new PostalAddress();
    	if(	pobox != null		
    			|| addressLine1 != null 
    			|| addressLine2 != null
    			|| city != null 
    			|| county != null
    			|| postcode != null
    			|| country != null) {
    		
    		address.addressLine1 = addressLine1;
    		address.addressLine2 = addressLine2;
    		address.postOfficeBox = pobox;
    		address.postCode = postcode;
    		address.city = city;
    		address.county = county;
    		address.country = country;
    	} else {
    		// So that we have one part not null
    		address.addressLine1 = randomString();
    	}
    	
    	return address;
	}

    private static Organisation randomOrganization() {
    	String company = null,
    		department = null;
    	
    	if(randomTrue()) {
    		company = randomString();
    	}
    	
    	if(randomTrue()) {
        	// Department only matters in 2.X
    		department = randomString();
    	}
    	
    	Organisation org = new Organisation();
    	    	
    	if(company == null && department == null ||
    		(!VersionUtils.is2XPlatform() && company == null)) {
    		// So that we have one part not null
    		// Also in 1.X we should have a company because department doesn't matter
    		company = randomString();    		
    	}
    	
		org.name = company;
		org.unitNames.add(department);
    	
    	return org;
    }
    
    private static String randomValue(int key) {
    	switch (key) {
    		case ContactChange.KEY_VCARD_NAME:
    			return VCardHelper.makeName(randomName());
    		case ContactChange.KEY_VCARD_PHONE:
    			return randomPhoneNumber();
    		case ContactChange.KEY_VCARD_EMAIL:
    			return randomEmail();
    		case ContactChange.KEY_VCARD_ADDRESS:
    			return VCardHelper.makePostalAddress(randomAddress());
    		case ContactChange.KEY_VCARD_ORG:
    			return VCardHelper.makeOrg(randomOrganization());
    		case ContactChange.KEY_VCARD_DATE:
    			return randomBirthday(); // Only birthday supported currently
    	}
    	
    	return randomString();
    }
    
//    private static String randomUpdatedValue(ContactChange cc) {
//    	final int key = cc.getKey();
//    	switch(key) {
//    		case ContactChange.KEY_VCARD_NAME:
//    			// get old value first
//    			final Name name = VCardHelper.getName(cc.getValue());
//    			final Name randomName = randomName();
//    			if(!TextUtils.isEmpty(randomName.firstname)) {
//    				name.firstname = randomName.firstname;
//    			}
//    			
//    			if(!TextUtils.isEmpty(randomName.midname)) {
//    				name.firstname = randomName.midname;
//    			}
//    			
//    			if(!TextUtils.isEmpty(randomName.surname)) {
//    				name.firstname = randomName.surname;
//    			}
//    			
//    			if(!TextUtils.isEmpty(randomName.title)) {
//    				name.firstname = randomName.title;
//    			}
//    			
//    			if(!TextUtils.isEmpty(randomName.suffixes)) {
//    				name.firstname = randomName.suffixes;
//    			}
//    			
//    			return VCardHelper.makeName(name);
//    		case ContactChange.KEY_VCARD_ADDRESS:
//    			final PostalAddress address = VCardHelper.getPostalAddress(cc.getValue());
//    			final PostalAddress randomAddress = 
//    			break;
//    		case ContactChange.KEY_VCARD_ORG:
//    			break;
//    			
//    	}
//    	
//    	return randomValue(key);
//    }
    
    private static int randomPhoneFlags() {
		if(randomTrue()) {
			return ContactChange.FLAG_CELL;
		}
		if(randomTrue()) {
			return ContactChange.FLAG_HOME;
		}
		if(randomTrue()) {
			return ContactChange.FLAG_WORK;
		}
		if(VersionUtils.is2XPlatform() && randomTrue()) {
			return ContactChange.FLAGS_WORK_CELL;
		}
		
		if(VersionUtils.is2XPlatform() &&  randomTrue()) {
			return ContactChange.FLAG_FAX;
		}
		if(randomTrue()) {
			return ContactChange.FLAGS_WORK_FAX;
		}
		
		if(randomTrue()) {
			return ContactChange.FLAGS_HOME_FAX;
		}
		
		return ContactChange.FLAG_NONE;	
    }
    
    private static int randomBasicFlags() {
		if(randomTrue()) {
			return ContactChange.FLAG_HOME;
		}
		if(randomTrue()) {
			return ContactChange.FLAG_WORK;
		}
		
		return ContactChange.FLAG_NONE;
    }
    
    private static int randomFlags(int key, boolean allowPreferred) {
    	int flags = ContactChange.FLAG_NONE;
    	switch(key) {
    		case ContactChange.KEY_VCARD_PHONE:
    			flags = randomPhoneFlags();
    			break;
    		case ContactChange.KEY_VCARD_URL:
    		case ContactChange.KEY_VCARD_ADDRESS:
    		case ContactChange.KEY_VCARD_EMAIL:
    			flags = randomBasicFlags();
    			break;
    		case ContactChange.KEY_VCARD_TITLE:
    		case ContactChange.KEY_VCARD_ORG:
    			if(randomTrue()) {
    				flags = ContactChange.FLAG_WORK;
    			}
    			break;
    		case ContactChange.KEY_VCARD_DATE:
    			// Only birthday currently supported
    			flags = ContactChange.FLAG_BIRTHDAY;
    			// fall through
    		default:
    			allowPreferred = false;
    			break;
    	}

		if(allowPreferred && randomTrue()) {
			flags|= ContactChange.FLAG_PREFERRED; 
		}
		return flags;
    }

    /**
     * Returns provided value if the probability Threshold given is met
     * Otherwise returns zero
     * @param treshold
     * @param value
     * @return value or zero
     */
    private static int valueForLikelihoodPercentage(int percentage, int value) {
    	return randomPositiveInt(100) < percentage ? value : 0;
    }
    
    private static int randomPositiveInt(int limit) {
    	return Math.abs(sRn.nextInt() % limit);
    }
    
    private static long randomPositiveLong() {
    	return Math.abs(sRn.nextLong());
    }

    private static int rand(int lo, int hi) {
            int n = hi - lo + 1;
            int i = sRn.nextInt() % n;
            if (i < 0)
                    i = -i;
            return lo + i;
    }

    private static String randomString(int lo, int hi, char loChar, char hiChar) {
            int n = rand(lo, hi);
            byte b[] = new byte[n];
            for (int i = 0; i < n; i++)
                    b[i] = (byte)rand(loChar, hiChar);
            return new String(b, 0);
    }
    
    private static String randomString() {
    	return randomString(4, 20, 'a', 'z');
    }

    private static String randomPhoneNumber() {
    	return "+"+randomString(4, 12, '0', '9');
    }
    
    private static String randomNumbersString() {
    	return randomString(4, 12, '0', '9');
    }
    
    private static String randomEmail() {
    	return randomString(4, 15, 'a', 'z')+"@"+randomString(4, 10, 'a', 'z')+".com";
    }
    
    private static Calendar randomDate() {
    	Date date = new Date(sRn.nextLong());
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(date);
    	return calendar;
    }
    private static String randomBirthday() {
    	final Calendar calendar = Calendar.getInstance();
    	Calendar randomDate = null;
    	do {
	    	randomDate = randomDate();
	    	final int currentYear = calendar.get(Calendar.YEAR);
	    	final int birthdayYear = currentYear - randomPositiveInt(100); // Maximum 100 years old
	    	randomDate.set(Calendar.YEAR, birthdayYear);
    	} while(calendar.compareTo(randomDate) >= 0);
    	final StringBuilder sb = new StringBuilder();
    	sb.append(randomDate.get(Calendar.DAY_OF_MONTH));
    	sb.append('-');
    	sb.append((randomDate.get(Calendar.MONTH)+1));
    	sb.append('-');
    	sb.append(randomDate.get(Calendar.YEAR));
    	return sb.toString();
    }
    
    private static boolean randomTrue() {
    	return sRn.nextBoolean();
    }
    
}
