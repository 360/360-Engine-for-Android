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

package com.vodafone360.people.tests.datatypes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.vodafone360.people.datatypes.ActivityContact;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactDetailDeletion;
import com.vodafone360.people.datatypes.ContactListResponse;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.IdentityCapability;
import com.vodafone360.people.datatypes.ItemList;
import com.vodafone360.people.datatypes.PublicKeyDetails;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.datatypes.UserProfile;
import com.vodafone360.people.datatypes.IdentityCapability.CapabilityID;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.io.rpg.PushMessageTypes;
import com.vodafone360.people.service.io.rpg.RpgPushMessage;

import android.test.AndroidTestCase;

public class NowPlusDatatypesTests extends AndroidTestCase {
	
	public void testActivityContact() {
		ActivityContact input = new ActivityContact();
		input.mAddress = "foo";
		input.mAvatarUrl = "foo";
		input.mContactId = 1L;
		input.mName = "bar";
		input.mNetwork = "mob";
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("address", input.mAddress);
		hash.put("avatarurl", input.mAvatarUrl);
		hash.put("contactid", input.mContactId);
		hash.put("name", input.mName);
		hash.put("network", input.mNetwork);
		
		ActivityContact output = ActivityContact.createFromHashTable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.mAddress, output.mAddress);
	    assertEquals(input.mAvatarUrl, output.mAvatarUrl);
	    assertEquals(input.mContactId, output.mContactId);
	    assertEquals(input.mName, output.mName);
	    assertEquals(input.mNetwork, output.mNetwork);
	}

	public void testContactChanges() {
		List<Contact> contacts = new ArrayList<Contact>();
		long currentServerVersion = 1;
		long versionAnchor = 2;
		int numberOfPages = 3;
		long serverRevisionBefore = 4;
		long serverRevisionAfter = 5;
		Hashtable<String, Object> hashUserProfile = new Hashtable<String, Object>();
		
		ContactChanges input = new ContactChanges();
		input.mContacts = contacts;
		input.mCurrentServerVersion = ((Long) currentServerVersion).intValue();
		input.mVersionAnchor = ((Long) versionAnchor).intValue();
		input.mNumberOfPages = numberOfPages;
		input.mServerRevisionBefore = ((Long) serverRevisionBefore).intValue();
		input.mServerRevisionAfter = ((Long) serverRevisionAfter).intValue();
		input.mUserProfile = UserProfile.createFromHashtable(hashUserProfile);
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("contact", contacts);
		hash.put("currentserverrevision", currentServerVersion);
		hash.put("serverrevisionanchor", versionAnchor);
		hash.put("numpages", numberOfPages);
		hash.put("serverrevisionbefore", serverRevisionBefore);
		hash.put("serverrevisionafter", serverRevisionAfter);
		hash.put("userprofile", hashUserProfile);
		
		ContactChanges helper = new ContactChanges();
		ContactChanges output = helper.createFromHashtable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.mContacts, output.mContacts);
	    assertEquals(input.mCurrentServerVersion, output.mCurrentServerVersion);
	    assertEquals(input.mNumberOfPages, output.mNumberOfPages);
	    assertEquals(input.mServerRevisionBefore, output.mServerRevisionBefore);
	    assertEquals(input.mServerRevisionAfter, output.mServerRevisionAfter);
	}

	public void testContactDetailDeletion() {
		long serverVersionBefore = 1;
		long serverVersionAfter = 2;
		long contactId = 3;
		
		ContactDetailDeletion input = new ContactDetailDeletion();
		input.mServerVersionBefore = ((Long) serverVersionBefore).intValue();
		input.mServerVersionAfter = ((Long) serverVersionAfter).intValue();
		input.mContactId = ((Long) contactId).intValue();
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("serverrevisionbefore", serverVersionBefore);
		hash.put("serverrevisionafter", serverVersionAfter);
		hash.put("contactid", contactId);
		
		ContactDetailDeletion helper = new ContactDetailDeletion();
		ContactDetailDeletion output = helper.createFromHashtable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.mServerVersionBefore, output.mServerVersionBefore);
	    assertEquals(input.mServerVersionAfter, output.mServerVersionAfter);
	    assertEquals(input.mContactId, output.mContactId);
	}
	
	public void testContactListResponse() {
		long serverRevisionBefore = 1;
		long serverRevisionAfter = 2;
		List<Integer> contactIdList = new ArrayList<Integer>();
		Integer contactId = 3;
		
		ContactListResponse input = new ContactListResponse();
		input.mServerRevisionBefore = ((Long) serverRevisionBefore).intValue();
		input.mServerRevisionAfter = ((Long) serverRevisionAfter).intValue();
		input.mContactIdList = contactIdList;
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("serverrevisionbefore", serverRevisionBefore);
		hash.put("serverrevisionafter", serverRevisionAfter);
		hash.put("contactidlist", contactIdList);
		hash.put("contactid", contactId);
		
		ContactListResponse helper = new ContactListResponse();
		ContactListResponse output = helper.createFromHashTable(hash);	// createFromHashTable should be static
		
		input.mContactIdList.add(contactId);
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
		assertEquals(input.mServerRevisionBefore, output.mServerRevisionBefore);
		assertEquals(input.mServerRevisionAfter, output.mServerRevisionAfter);
		assertEquals(input.mContactIdList, output.mContactIdList);
	}
	
	public void testGroupItem() {
		int groupType = 1;
		boolean isReadOnly = true;
		boolean requiresLocalisation = true;
		boolean isSystemGroup = true;
		boolean isSmartGroup = true;
		long id = 3;
		long userId = 4;
		String name = "foo";
		
		GroupItem input = new GroupItem();
		input.mGroupType = (Integer) groupType;
		input.mIsReadOnly = (Boolean) isReadOnly;
		input.mRequiresLocalisation = (Boolean) requiresLocalisation;
		input.mIsSystemGroup = (Boolean) isSystemGroup;
		input.mIsSmartGroup = (Boolean) isSmartGroup;
		input.mId = (Long) id; 
		input.mUserId = (Long) userId;
		input.mName = name;
	
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("grouptype", groupType);
		hash.put("isreadonly", isReadOnly);
		hash.put("requireslocalisation", requiresLocalisation);
		hash.put("issystemgroup", isSystemGroup);
		hash.put("issmartgroup", isSmartGroup);
		hash.put("id", id);
		hash.put("userid", userId);
		hash.put("name", name);
		
		GroupItem helper = new GroupItem();
		GroupItem output = helper.createFromHashtable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.mGroupType, output.mGroupType);
	    assertEquals(input.mIsReadOnly, output.mIsReadOnly);
	    assertEquals(input.mRequiresLocalisation, output.mRequiresLocalisation);
	    assertEquals(input.mIsSystemGroup, output.mIsSystemGroup);
	    assertEquals(input.mIsSmartGroup, output.mIsSmartGroup);
	    assertEquals(input.mId, output.mId);
	    assertEquals(input.mUserId, output.mUserId);
	    assertEquals(input.mName, output.mName);
	}

	public void testIdentityCapability() {
		IdentityCapability input = new IdentityCapability();
		input.mCapability = CapabilityID.share_media;
		input.mDescription = "des";
		input.mName = "name";
		input.mValue = true;
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("capabilityid", input.mCapability.name());
		hash.put("description", input.mDescription);
		hash.put("name", input.mName);
		hash.put("value", input.mValue);

		IdentityCapability helper = new IdentityCapability();
		IdentityCapability output = helper.createFromHashtable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.describeContents(), output.describeContents());
	    assertEquals(input.mCapability, output.mCapability);
	    assertEquals(input.mDescription, output.mDescription);
	    assertEquals(input.mName, output.mName);
	    assertEquals(input.mValue, output.mValue);
	}
	
	public void testIdentity() {
		Identity input = new Identity();
		input.mPluginId = "pluginid";
		input.mNetwork = "network";
		input.mIdentityId = "identityId";
		input.mDisplayName = "displayname";
		input.mCreated = new Long(12);
		input.mUpdated = new Long(23);
		input.mActive = true;
		input.mAuthType = "none";
		input.mIdentityType = "chat";
		input.mUserId = new Integer(1234);
		input.mUserName = "bob";
		input.mCountryList = new ArrayList<String>();
		
		String urlString = "http://www.mobica.com/";
		try {
			input.mNetworkUrl = new URL(urlString);
		} catch (MalformedURLException e) {
			input.mNetworkUrl = null;
		}
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("pluginid", input.mPluginId);
		hash.put("network", input.mNetwork);
		hash.put("identityid", input.mIdentityId);
		hash.put("displayname", input.mDisplayName);
		hash.put("networkurl", urlString);
		hash.put("created", input.mCreated);
		hash.put("updated", input.mUpdated);
		hash.put("active", true);
		hash.put("authtype",input.mAuthType);
		hash.put("identitytype",input.mIdentityType);
		hash.put("userid",new Long(1234));
		hash.put("username",input.mUserName);
		hash.put("countrylist",input.mCountryList);
		
		Identity helper = new Identity();
		Identity output = helper.createFromHashtable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertTrue(input.isSameAs(output));
	}	
	
	public void testItemList() {
		ItemList groupPriv = new ItemList(ItemList.Type.group_privacy);
		int groupType = 1;
		boolean isReadOnly = true;
		boolean requiresLocalisation = true;
		boolean isSystemGroup = true;
		boolean isSmartGroup = true;
		long id = 3;
		long userId = 4;
		String name = "foo";
		Hashtable<String, Object> hashGroup = new Hashtable<String, Object>();
		hashGroup.put("grouptype", groupType);
		hashGroup.put("isreadonly", isReadOnly);
		hashGroup.put("requireslocalisation", requiresLocalisation);
		hashGroup.put("issystemgroup", isSystemGroup);
		hashGroup.put("issmartgroup", isSmartGroup);
		hashGroup.put("id", id);
		hashGroup.put("userid", userId);
		hashGroup.put("name", name);
		
		Vector<Hashtable<String, Object>> vect = new Vector<Hashtable<String, Object>>();
		vect.add(hashGroup);
		Hashtable<String, Object> hashItemListGroup = new Hashtable<String, Object>();
		hashItemListGroup.put("itemlist", vect);
		
		groupPriv.populateFromHashtable(hashItemListGroup);
		GroupItem helper = new GroupItem();
		
		GroupItem input = helper.createFromHashtable(hashGroup);
		GroupItem output = (GroupItem) groupPriv.mItemList.get(0);
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.mGroupType, output.mGroupType);
	    assertEquals(input.mIsReadOnly, output.mIsReadOnly);
	    assertEquals(input.mRequiresLocalisation, output.mRequiresLocalisation);
	    assertEquals(input.mIsSystemGroup, output.mIsSystemGroup);
	    assertEquals(input.mIsSmartGroup, output.mIsSmartGroup);
	    assertEquals(input.mId, output.mId);
	    assertEquals(input.mUserId, output.mUserId);
	    assertEquals(input.mName, output.mName);
	}

	public void testPublicKeyDetails() {  
	    byte[] modulo = new byte[] {0, 0};
	    byte[] exponential = new byte[] {0, 1};
	    byte[] key = new byte[] {1, 1};
	    String keyBase64 = "64";
	    
	    PublicKeyDetails input = new PublicKeyDetails();
	    input.mModulus = modulo;
	    input.mExponential = exponential;
	    input.mKeyX509 = key;
	    input.mKeyBase64 = keyBase64;
	    
	    Hashtable<String, Object> hash = new Hashtable<String, Object>();
	    hash.put("modulo", modulo);
	    hash.put("exponential", exponential);
	    hash.put("key", key);
	    hash.put("keybase64", keyBase64);
	    
	    PublicKeyDetails output = PublicKeyDetails.createFromHashtable(hash);
	    
	    assertEquals(input.describeContents(), output.describeContents());
	    assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.mModulus, output.mModulus);
	    assertEquals(input.mExponential, output.mExponential);
	    assertEquals(input.mKeyX509, output.mKeyX509);
	    assertEquals(input.mKeyBase64, output.mKeyBase64);
	}

	public void testCreatePushEvent() {
		RpgPushMessage msg = new RpgPushMessage();
		msg.mType = PushMessageTypes.CONTACTS_CHANGE;
		EngineId engId = EngineId.ACTIVITIES_ENGINE;
		
		PushEvent input = (PushEvent) PushEvent.createPushEvent(msg, engId);
		
		assertEquals("PushEvent", input.name());
		assertEquals(msg.mType, input.mMessageType);
		assertEquals(engId, input.mEngineId);
	}

	public void testStatusMsg() {
		boolean status = true;
		boolean dryRun = true;

		StatusMsg input = new StatusMsg();
		input.mStatus = (Boolean) status;
		input.mDryRun = (Boolean) dryRun;
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("status", status);
		hash.put("dryrun", dryRun);

		StatusMsg helper = new StatusMsg();
		StatusMsg output = helper.createFromHashtable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.mStatus, output.mStatus);
	    assertEquals(input.mDryRun, output.mDryRun);
	}

	public void testUserProfile() {
		UserProfile input = new UserProfile();
		input.userID = 50L;
		input.aboutMe = "newAboutMe";
		input.contactID = 10L;
		input.gender = 1;
		input.profilePath = "foo";
		input.updated = 2L;
		ContactDetail contactDetail = new ContactDetail();
		contactDetail.value = "00000000";
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("userid", input.userID);
		hash.put("aboutme", input.aboutMe);
		hash.put("contactid", input.contactID);
		hash.put("gender", input.gender);
		hash.put("profilepath", input.profilePath);
		hash.put("updated", input.updated);
		
		UserProfile output = UserProfile.createFromHashtable(hash);
		
		assertEquals(input.name(), output.name());
	    assertEquals(input.toString(), output.toString());
	    assertEquals(input.userID, output.userID);
	    assertEquals(input.aboutMe, output.aboutMe);
	    assertEquals(input.contactID, output.contactID);
	    assertEquals(input.gender, output.gender);
	    assertEquals(input.profilePath, output.profilePath);
	    assertEquals(input.updated, output.updated);
	}    
}
