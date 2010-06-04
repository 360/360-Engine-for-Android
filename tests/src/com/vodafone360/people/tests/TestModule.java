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

package com.vodafone360.people.tests;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Contacts;
import android.text.format.Time;
import android.util.Log;

import com.vodafone360.people.database.tables.ActivitiesTable.TimelineNativeTypes;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.database.tables.ContactDetailsTable.Field;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.datatypes.ActivityItem.Flag;
import com.vodafone360.people.datatypes.ActivityItem.Type;
import com.vodafone360.people.datatypes.ActivityItem.Visibility;
import com.vodafone360.people.datatypes.VCardHelper.Organisation;

@SuppressWarnings("deprecation")
public class TestModule {
	private static final Random RANDOM = new Random();
	private static final int TABLE_SIZE = 25;
	public static final int CONTACT_METHODS_KIND_EMAIL = 1;
	public static final int CONTACT_METHODS_KIND_ADDRESS = 2;

	public enum NativeNameType {
		NO_NAME,
		SINGLE_NAME,
		DOUBLE_NAME,
		FULL_NAME_NO_TITLE,
		FULL_NAME
	}

	public static class NativeDetail {
		public String mValue1;
		public String mValue2;
		public String mValue3;
		public boolean mIsPrimary;
		public Integer mId;
	}
	
	public static class NativeContactDetails {
		public Integer mId;
		public VCardHelper.Name mName;
		public String mNote;
		public final List<NativeDetail> mPhoneList = new ArrayList<NativeDetail>();
		public final List<NativeDetail> mEmailList = new ArrayList<NativeDetail>();
		public final List<NativeDetail> mAddressList = new ArrayList<NativeDetail>();
		public final List<NativeDetail> mOrgList = new ArrayList<NativeDetail>();
		public final List<NativeDetail> mTitleList = new ArrayList<NativeDetail>();
	}
	
	public TestModule() {
	}
	
	public ContactDetail createDummyDetailsName() {
		ContactDetail detail = new ContactDetail();
		VCardHelper.Name name = createDummyName();
		detail.setName(name);
		detail.key = ContactDetail.DetailKeys.VCARD_NAME;
		detail.keyType = null;
		return detail;
	}

	private VCardHelper.Name createDummyName() {
		VCardHelper.Name name = new VCardHelper.Name();
		switch (generateRandomInt() % 6)
		{
		case 0:
			name.title = "Mr";
			break;
		case 1:
			name.title = "Miss";
			break;
		case 2:
			name.title = "Mrs";
			break;
		case 3:
			name.title = "Ms";
			break;
		case 4:
			name.title = "Dr";
			break;
		case 5:
			name.title = null;
		}
		name.firstname = generateRandomString();
		name.surname = generateRandomString();
		return name;
	}

	public ContactDetail createDummyDetailsNickname(ContactDetail name) {
		ContactDetail nickname = new ContactDetail();
		nickname.setValue(name.getName().toString(), ContactDetail.DetailKeys.VCARD_NICKNAME, null);
		return nickname;
	}
	
	final private ContactDetail.DetailKeys[] keysList = {ContactDetail.DetailKeys.VCARD_DATE,
			ContactDetail.DetailKeys.PRESENCE_TEXT,
			ContactDetail.DetailKeys.VCARD_EMAIL,
			ContactDetail.DetailKeys.VCARD_PHONE,
			ContactDetail.DetailKeys.VCARD_BUSINESS,
			ContactDetail.DetailKeys.VCARD_ADDRESS,
			ContactDetail.DetailKeys.VCARD_URL,
			ContactDetail.DetailKeys.VCARD_ROLE,
			ContactDetail.DetailKeys.VCARD_NOTE
			};
	
	public void createDummyDetailsData(ContactDetail detail) {
		ContactDetail.DetailKeys key = keysList[generateRandomInt() % keysList.length];
		detail.key = key;
		detail.keyType = null;
		
		switch (key)
		{
		case PRESENCE_TEXT:
			detail.setValue(generateRandomString(), key, null);
			detail.alt="";
			break;
		case VCARD_DATE:
			Time time = new Time();
			time.parse("20080605");
			detail.setDate(time, ContactDetail.DetailKeyTypes.BIRTHDAY);
			break;
		case VCARD_IMADDRESS:
			detail.setValue(generateRandomString() + "@mail.co.uk", key, null);
			break;
		case VCARD_EMAIL:
			detail.setEmail(generateRandomString() + "@mail.co.uk", ContactDetail.DetailKeyTypes.HOME);
			break;
		case VCARD_PHONE:
			detail.setTel("07967 123456", ContactDetail.DetailKeyTypes.CELL);
			break;
		case VCARD_BUSINESS:
		case VCARD_ADDRESS:
			VCardHelper.PostalAddress address = new VCardHelper.PostalAddress();
			address.addressLine1 = "123 Any road";
			address.addressLine2 = "Any location";
			address.city = "Any City";
			address.county = "Any County";
			address.postCode = "M6 2AY";
			address.country = "United Kingdom";
			detail.setPostalAddress(address, ContactDetail.DetailKeyTypes.HOME);
			break;
		case VCARD_URL:
		case VCARD_INTERNET_ADDRESS:
			detail.setValue("www." + generateRandomString() + "anyaddress.co.uk", key, null);
			break;
		case VCARD_ROLE:
			detail.setValue(generateRandomString(), key, null);
			break;
		case VCARD_NOTE:
			{
				String randomString = new String();
				for (int i = 0 ; i < generateRandomInt() % 10 ; i++) {
					randomString += generateRandomString() + " ";
				}
				detail.setValue(randomString, key, null);
				break;
			}
		}
	}

	public void modifyDummyDetailsData(ContactDetail detail) {
		switch (detail.key)
		{
		case VCARD_NAME:
			VCardHelper.Name name = createDummyName();
			detail.setName(name);
			break;
		case PRESENCE_TEXT:
			detail.setValue(generateRandomString(), detail.key, null);
			break;
		case VCARD_DATE:
			Time time = new Time();
			time.parse("20080605");
			detail.setDate(time, ContactDetail.DetailKeyTypes.BIRTHDAY);
			break;
		case VCARD_IMADDRESS:
			detail.setValue(generateRandomString() + "@mail.co.uk", detail.key, null);
			break;
		case VCARD_EMAIL:
			detail.setEmail(generateRandomString() + "@mail.co.uk", ContactDetail.DetailKeyTypes.HOME);
			break;
		case VCARD_PHONE:
			detail.setTel("07967 123456", ContactDetail.DetailKeyTypes.CELL);
			break;
		case VCARD_BUSINESS:
		case VCARD_ADDRESS:
			VCardHelper.PostalAddress address = new VCardHelper.PostalAddress();
			address.addressLine1 = "123 Any road";
			address.addressLine2 = "Any location";
			address.city = "Any City";
			address.county = "Any County";
			address.postCode = "M6 2AY";
			address.country = "United Kingdom";
			detail.setPostalAddress(address, ContactDetail.DetailKeyTypes.HOME);
			break;
		case VCARD_URL:
		case VCARD_INTERNET_ADDRESS:
			detail.setValue("www." + generateRandomString() + "anyaddress.co.uk", detail.key, null);
			break;
		case VCARD_ROLE:
			detail.setValue(generateRandomString(), detail.key, null);
			break;
		case VCARD_ORG:
			Organisation org = new Organisation();
			org.name = generateRandomString();
			detail.setOrg(org, null);
			break;
		case VCARD_TITLE:
			detail.setValue(generateRandomString(), detail.key, null);
			break;
		case VCARD_NOTE:
			{
				String randomString = new String();
				for (int i = 0 ; i < generateRandomInt() % 10 ; i++) {
					randomString += generateRandomString() + " ";
				}
				detail.setValue(randomString, detail.key, null);
				break;
			}
		}
	}
	
	public void addRandomGroup(List<Long> groupList) {
		long[] groupChoiceList = {1,2, 860909, 860910, 860911, 860912};
		for (int i = 0 ; i < 20 ; i++) {
			int groupIdIdx = generateRandomInt() % groupChoiceList.length;
			Long groupId = groupChoiceList[groupIdIdx];
			boolean used = false;
			for (int j = 0 ; j < groupList.size() ; j++) {
				if (groupList.get(j).equals(groupId)) {
					used = true;
					break;
				}
			}
			if (!used) {
				groupList.add(groupId);
				return;
			}
		}
	}
	
	public Contact createDummyContactData() {
		Contact contact = new Contact();
		//contact.localContactID = 0L;
		contact.synctophone = generateRandomBoolean();
		contact.details.clear();
		contact.aboutMe = generateRandomString();
		contact.friendOfMine = ((generateRandomInt() & 1) == 0);
		contact.gender = generateRandomInt() & 1;
		
		if (contact.groupList == null) {
			contact.groupList = new ArrayList<Long>();
		}
		for (int i = 0 ; i < (generateRandomInt() & 3) ; i++) {
			addRandomGroup(contact.groupList);
		}
		
		if (contact.sources == null) {
			contact.sources = new ArrayList<String>();
		}
		for (int i = 0 ; i < (generateRandomInt() & 3) ; i++) {
			contact.sources.add(generateRandomString());
		}
		
		ContactDetail nameDetail = createDummyDetailsName();
		ContactDetail nicknameDetail = createDummyDetailsNickname(nameDetail);
		contact.details.add(nameDetail);
		contact.details.add(nicknameDetail);

		final int noOfDetails = (generateRandomInt() % 10);
		for (int i = 0 ; i <  noOfDetails ; i++ ) {
			ContactDetail detail = new ContactDetail();
			createDummyDetailsData(detail);
			contact.details.add(detail);
		}
		if ((generateRandomInt() & 1) == 0) {
			ContactDetail detail = new ContactDetail();
			Organisation org = new Organisation();
			org.name = generateRandomString();
			detail.setOrg(org, null);
			contact.details.add(detail);
		}
		if ((generateRandomInt() & 1) == 0) {
			ContactDetail detail = new ContactDetail();
			detail.setValue(generateRandomString(), ContactDetail.DetailKeys.VCARD_TITLE, null);
			contact.details.add(detail);
		}
		fixPreferred(contact);
		return contact;
	}
	
	
	public Contact createDummyNativeContactData() {
		Contact contact = createDummyContactData();
		for (ContactDetail cd : contact.details) {
			cd.nativeContactId = generateRandomInt();
			cd.nativeDetailId = generateRandomInt();
			cd.nativeVal1 = generateRandomString();
			cd.nativeVal2 = generateRandomString();
			cd.nativeVal3 = generateRandomString();
		}
		return contact;
	}
	
	public static boolean generateRandomBoolean() {
        return RANDOM.nextBoolean();
    }
	
	/**
	 * TODO: fill in the method properly
	 * @return
	 */
	public List<ActivityItem> createFakeActivitiesList() {
		
		List<ActivityItem> activityList = new ArrayList<ActivityItem>();
		for (int i = 0; i < TABLE_SIZE; i++) {
			ActivityItem activityItem = new ActivityItem();
			
			/** Unique identifier for the activity. This can be empty when setting 
			 * a new activity (the id is generated on the server side) */
			activityItem.mActivityId = System.currentTimeMillis();

			/** Timestamp representing the time of the activity. 
			 * This may not be related to creation/updated time. */
			activityItem.mTime = System.currentTimeMillis();
			
			/** local id for db */
//			activityItem.mLocalId; set by DB insertion
			
//			activityItem.mMoreInfo; //new Hashtable<ActivityItem, String>
			
			/** The parent activity for 'grouped' or aggregated activities. This must be empty 
			 * for normal activities that can be retrieved normally. Normally, a GetActivities 
			 * without filter will not yield any 'grouped' or 'child' activities. 
			 * To get activities that have a mParentActivity set, the 'children' filter must 
			 * be used with a value of the parent Activity's id.*/
//			activityItem.mParentActivity; // null
			
			/** Indicates wether this activity 'groups' several child activities. When set, 
			 * there must be child activities set that refer the main activity. Normally, 
			 * a GetActivities without filter will not yield any 'grouped' or 'child' activities. 
			 * To get activities that have a parentactivity set, the 'children' filter 
			 * must be used with a value of the parent Activity's id.*/
//			activityItem.mHasChildren = false;
			
			/** Defines a binary preview for the activity. The preview can be a small thumbnail 
			 * of the activity. The type of the binary data is defined into the previewmime field.*/
//			keep null
//			activityItem.mPreview = ByteBuffer.allocate(bytes.length);
//			activityItem.mPreviewMime;
			
			/** Defines an http url that the client can use to retrieve preview binary data. 
			 * Can be used to embed the url into an IMG HTML tag.*/
//			activityItem.mPreviewUrl
			
			/** Name of the store type for this message. This field contains information about the 
			 * originator network (local or external community activity). 
			 * By default, should be set to local*/		
			activityItem.mStore = "local";
			
			activityItem.mTitle = generateRandomString(); 
			
			activityItem.mDescription = activityItem.mDescription + activityItem.mStore;
			
			/** Defines the type of the activity. */
			activityItem.mType = Type.CONTACT_FRIEND_INVITATION_SENT;
			
			/** Defines an internal reference (if any) to the source of the activity. 
			 * The format for the uri is "module:identifier".Some examples of valid uri are:
			 * contact:2737b322c9f6476ca152aa6cf3e5ac12 The activity is linked to some 
			 * changes on a contact identified by id=2737b322c9f6476ca152aa6cf3e5ac12.
			 * file:virtual/flickr/2590004126 The activity is linked to some actions 
			 * on a file identified by id=virtual/flickr/2590004126.
			 * message:9efd255359074dd9bd04cc1c8c4743e5 The activity is linked to a message 
			 * identified by id=9efd255359074dd9bd04cc1c8c4743e5 */
			activityItem.mUri = "virtual/flickr/2590004126";
			
	//can be 0		activityItem.mActivityFlags;

			/** Miscellaneous flags.*/
			activityItem.mFlagList = new ArrayList<Flag>();
			activityItem.mFlagList.add(Flag.ALREADY_READ);
			
			/** Defines the contact information of the counter-parties in the activity. 
			 * This field is not mandatory, because some activity types 
			 * are not related to contacts, but required if known.. */
	//keep it simple - empty		activityItem.mContactList = ;
			activityItem.mVisibility = new ArrayList<Visibility>();
			activityItem.mVisibility.add(Visibility.ORIGINATOR);
			
	//keep it 0		activityItem.mVisibilityFlags = 0;
			activityList.add(activityItem);
		} 
		return activityList;
	}
	
	/**
	 * TODO: fill in the method properly
	 * @return
	 */
	public static ArrayList<TimelineSummaryItem> generateFakeTimeLinesList() {

		ArrayList<TimelineSummaryItem> timeList = new ArrayList<TimelineSummaryItem>();
		ArrayList<String> uniqueContactNames = new ArrayList<String>();
		ArrayList<Long> uniqueLocalContactIds = new ArrayList<Long>();
		
		for (int i = 0; i < TABLE_SIZE; i++) {
			TimelineSummaryItem item = new TimelineSummaryItem();
			
	        item.mTimestamp = System.currentTimeMillis();
	        item.mNativeItemId = generateRandomInt();
	        item.mNativeItemType = TimelineNativeTypes.SmsLog.ordinal(); // the same as in fetch
	        item.mType = Type.MESSAGE_SMS_RECEIVED;
	        item.mNativeThreadId = generateRandomInt();
	        item.mContactAddress = "some local address";
	      	item.mDescription = generateRandomString();
	      	item.mTitle = DateFormat.getDateInstance().format(new Date (item.mTimestamp));
			
	      	//item.mLocalActivityId = i; // Set on databae insert.
	      	//the below fields are originally set under condition
	      	item.mContactId = generateRandomLong();
	    	item.mLocalContactId = generateRandomLong();
			item.mUserId = generateRandomLong();
			item.mContactName = generateRandomString()+ i;
			item.mContactNetwork = generateRandomString();
			item.mIncoming = TimelineSummaryItem.Type.INCOMING;
//			public boolean mHasAvatar;
			// Name and localContactId has to be unique, if localContactId is the same 
			// then contact isn't included in ActivitiesTable.fetchTimelineEventList
			if (!uniqueContactNames.contains(item.mContactName)
					&& !uniqueLocalContactIds.contains(item.mLocalContactId)) {
				uniqueContactNames.add(item.mContactName);
				uniqueLocalContactIds.add(item.mLocalContactId);
				timeList.add(item);
			}
		}
		
		Log.e("UNIQUE TIMELINE NAMES:", uniqueContactNames.toString());
		return timeList;
	}

	
	/**
	 * TODO: fill in the method properly
	 * @return
	 */
	public List<ActivityItem> createFakeStatusEventList() {

		List<ActivityItem> activityList = new ArrayList<ActivityItem>();
		for (int i = 0; i < TABLE_SIZE; i++) {
			ActivityItem activityItem = new ActivityItem();
			
			/** Unique identifier for the activity. This can be empty when setting 
			 * a new activity (the id is generated on the server side) */
			activityItem.mActivityId = System.currentTimeMillis();

			/** Timestamp representing the time of the activity. 
			 * This may not be related to creation/updated time. */
			activityItem.mTime = System.currentTimeMillis();
			
			/** local id for db */
//			activityItem.mLocalId; set by DB insertion
			
//			activityItem.mMoreInfo; //new Hashtable<ActivityItem, String>
			
			/** The parent activity for 'grouped' or aggregated activities. This must be empty 
			 * for normal activities that can be retrieved normally. Normally, a GetActivities 
			 * without filter will not yield any 'grouped' or 'child' activities. 
			 * To get activities that have a mParentActivity set, the 'children' filter must 
			 * be used with a value of the parent Activity's id.*/
//			activityItem.mParentActivity; // null
			
			/** Indicates wether this activity 'groups' several child activities. When set, 
			 * there must be child activities set that refer the main activity. Normally, 
			 * a GetActivities without filter will not yield any 'grouped' or 'child' activities. 
			 * To get activities that have a parentactivity set, the 'children' filter 
			 * must be used with a value of the parent Activity's id.*/
//			activityItem.mHasChildren = false;
			
			/** Defines a binary preview for the activity. The preview can be a small thumbnail 
			 * of the activity. The type of the binary data is defined into the previewmime field.*/
//			keep null
//			activityItem.mPreview = ByteBuffer.allocate(bytes.length);
//			activityItem.mPreviewMime;
			
			/** Defines an http url that the client can use to retrieve preview binary data. 
			 * Can be used to embed the url into an IMG HTML tag.*/
//			activityItem.mPreviewUrl
			
			/** Name of the store type for this message. This field contains information about the 
			 * originator network (local or external community activity). 
			 * By default, should be set to local*/		
			activityItem.mStore = "local";
			
			activityItem.mTitle = generateRandomString(); 
			
			activityItem.mDescription = activityItem.mDescription + activityItem.mStore;
			
			/** Defines the type of the activity. */
			activityItem.mType = Type.CONTACT_RECEIVED_STATUS_UPDATE;
			
			/** Defines an internal reference (if any) to the source of the activity. 
			 * The format for the uri is "module:identifier".Some examples of valid uri are:
			 * contact:2737b322c9f6476ca152aa6cf3e5ac12 The activity is linked to some 
			 * changes on a contact identified by id=2737b322c9f6476ca152aa6cf3e5ac12.
			 * file:virtual/flickr/2590004126 The activity is linked to some actions 
			 * on a file identified by id=virtual/flickr/2590004126.
			 * message:9efd255359074dd9bd04cc1c8c4743e5 The activity is linked to a message 
			 * identified by id=9efd255359074dd9bd04cc1c8c4743e5 */
			activityItem.mUri = "virtual/flickr/2590004126";
			
	//can be 0		activityItem.mActivityFlags;

			/** Miscellaneous flags.*/
			activityItem.mFlagList = new ArrayList<Flag>();
			activityItem.mFlagList.add(Flag.STATUS);
			activityItem.mActivityFlags = 0x04;
			
			/** Defines the contact information of the counter-parties in the activity. 
			 * This field is not mandatory, because some activity types 
			 * are not related to contacts, but required if known.. */
	//keep it simple - empty		activityItem.mContactList = ;
			activityItem.mVisibility = new ArrayList<Visibility>();
			activityItem.mVisibility.add(Visibility.ORIGINATOR);
			
	//keep it 0		activityItem.mVisibilityFlags = 0;
			activityList.add(activityItem);
		} 
		return activityList;
	}
	
	public static int generateRandomInt() {
		return RANDOM.nextInt() & 0x7FFF;
	}
	
	public static long generateRandomLong() {
		return RANDOM.nextLong() & 0x7FFF;
	}

	public static String generateRandomString() {
		String[] stringList = {"Adult",
		"Aeroplane",
		"Air",
		"Aircraft Carrier",
		"Airforce",
		"Airport",
		"Album",
		"Alphabet",
		"Apple",
		"Arm",
		"Army",
		"Baby",
		"Baby",
		"Backpack",
		"Balloon",
		"Banana",
		"Bank",
		"Barbecue",
		"Bathroom",
		"Bathtub",
		"Bed",
		"Bed",
		"Bee",
		"Bible",
		"Bible",
		"Bird",
		"Book",
		"Boss",
		"Bottle",
		"Bowl",
		"Box",
		"Boy",
		"Brain",
		"Bridge",
		"Butterfly",
		"Button",
		"Cappuccino",
		"Car",
		"Car-race",
		"Carpet",
		"Carrot",
		"Cave",
		"Chair",
		"Chess Board",
		"Chief",
		"Child",
		"Chisel",
		"Chocolates",
		"Church",
		"Circle",
		"Circus",
		"Circus",
		"Clock",
		"Clown",
		"Coffee",
		"Coffee-shop",
		"Comet",
		"Compact Disc",
		"Compass",
		"Computer",
		"Crystal",
		"Cup",
		"Cycle",
		"Data Base",
		"Desk",
		"Diamond",
		"Dress",
		"Drill",
		"Drink",
		"Drum",
		"Dung",
		"Ears",
		"Earth",
		"Egg",
		"Electricity",
		"Elephant",
		"Eraser",
		"Eyes",
		"Family",
		"Fan",
		"Feather",
		"Festival",
		"Film",
		"Finger",
		"Fire",
		"Floodlight",
		"Flower",
		"Foot",
		"Fork",
		"Freeway",
		"Fruit",
		"Fungus",
		"Game",
		"Garden",
		"Gas",
		"Gate",
		"Gemstone",
		"Girl",
		"Gloves",
		"Grapes",
		"Guitar",
		"Hammer",
		"Hat",
		"Hieroglyph",
		"Highway",
		"Horoscope",
		"Horse",
		"Hose",
		"Ice",
		"Ice-cream",
		"Insect",
		"Jet fighter",
		"Junk",
		"Kaleidoscope",
		"Kitchen",
		"Knife",
		"Leather jacket",
		"Leg",
		"Library",
		"Liquid",
		"Magnet",
		"Man",
		"Map",
		"Maze",
		"Meat",
		"Meteor",
		"Microscope",
		"Milk",
		"Milkshake",
		"Mist",
		"Mojito",
		"Money $$$$",
		"Monster",
		"Mosquito",
		"Mouth",
		"Nail",
		"Navy",
		"Necklace",
		"Needle",
		"Onion",
		"PaintBrush",
		"Pants",
		"Parachute",
		"Passport",
		"Pebble",
		"Pendulum",
		"Pepper",
		"Perfume",
		"Pillow",
		"Plane",
		"Planet",
		"Pocket",
		"Post-office",
		"Potato",
		"Printer",
		"Prison",
		"Pyramid",
		"Radar",
		"Rainbow",
		"Record",
		"Restaurant",
		"Rifle",
		"Ring",
		"Robot",
		"Rock",
		"Rocket",
		"Roof",
		"Room",
		"Rope",
		"Saddle",
		"Salt",
		"Sandpaper",
		"Sandwich",
		"Satellite",
		"School",
		"Ship",
		"Shoes",
		"Shop",
		"Shower",
		"Signature",
		"Skeleton",
		"Slave",
		"Snail",
		"Software",
		"Solid",
		"Space Shuttle",
		"Spectrum",
		"Sphere",
		"Spice",
		"Spiral",
		"Spoon",
		"Sports-car",
		"Spot Light",
		"Square",
		"Staircase",
		"Star",
		"Stomach",
		"Sun",
		"Sunglasses",
		"Surveyor",
		"Swimming Pool",
		"Sword",
		"Table",
		"Tapestry",
		"Teeth",
		"Telescope",
		"Television",
		"Tennis racquet",
		"Thermometer",
		"Tiger",
		"Toilet",
		"Tongue",
		"Torch",
		"Torpedo",
		"Train",
		"Treadmill",
		"Triangle",
		"Tunnel",
		"Typewriter",
		"Umbrella",
		"Vacuum",
		"Vampire",
		"Videotape",
		"Vulture",
		"Water",
		"Weapon",
		"Web",
		"Wheelchair",
		"Window",
		"Woman",
		"Worm",
		"X-ray"};
		final int val = generateRandomInt() % stringList.length;
		return stringList[val];
	}

	public NativeContactDetails addNativeContact(ContentResolver cr, NativeNameType nameType, boolean withNote, int phones, int emails, int addresses, int orgs) {
		NativeContactDetails ncd = new NativeContactDetails();
		ncd.mName = createDummyName();
		ncd.mName.midname = generateRandomString();
		switch (nameType) {
			case NO_NAME:
				ncd.mName.firstname = null;
				// Fall through
			case SINGLE_NAME:
				ncd.mName.surname = null;
				// Fall through
			case DOUBLE_NAME:
				ncd.mName.midname = null;
				// Fall through
			case FULL_NAME_NO_TITLE:
				ncd.mName.title = null;
				// Fall through
			case FULL_NAME:
				break;
		}
		ContentValues cv = new ContentValues();
		cv.put(Contacts.People.NAME, ncd.mName.toString());
		if (withNote) {
			String randomString = new String();
			for (int i = 0 ; i < generateRandomInt() % 10 ; i++) {
				randomString += generateRandomString() + " ";
			}
			cv.put(Contacts.People.NOTES, randomString);
			ncd.mNote = randomString;
		}
		Uri peopleResult;
		try {
			peopleResult = cr.insert(Contacts.People.CONTENT_URI, cv);
			if (peopleResult == null) {
				return null;
			}
		} catch (SQLException e) {
			return null;
		}
		int id = (int)ContentUris.parseId(peopleResult);
		ncd.mId = id;
		for (int i = 0 ; i < phones ; i++) {
			NativeDetail nd = addNativePhone(cr, id, (i == 0));
			if (nd == null) {
				return null;
			}
			ncd.mPhoneList.add(nd);
		}
		for (int i = 0 ; i < emails ; i++) {
			NativeDetail nd = addNativeContactMethod(cr, id, CONTACT_METHODS_KIND_EMAIL, (i==0));
			if (nd == null) {
				return null;
			}
			ncd.mEmailList.add(nd);
		}
		for (int i = 0 ; i < addresses ; i++) {
			NativeDetail nd = addNativeContactMethod(cr, id, CONTACT_METHODS_KIND_ADDRESS, (i==0));
			if (nd == null) {
				return null;
			}
			ncd.mAddressList.add(nd);
		}
		for (int i = 0 ; i < orgs ; i++) {
			if (!addNativeOrg(cr, id, (i==0), ncd.mOrgList, ncd.mTitleList)) {
				return null;
			}
		}
		
		return ncd;
	}
	
	public NativeDetail addNativePhone(ContentResolver cr, int id, boolean isPrimary) {
		ContentValues cv = new ContentValues();
		String prefix = "0";
		if (generateRandomInt() > 0x3FFF) {
			prefix = String.format("+%02d", generateRandomInt()%100);
		}
		String number = prefix + generateRandomInt() + " " + generateRandomInt();
		int type = generateRandomInt() & 7;
		if (type == 0) {
			cv.put(Contacts.Phones.LABEL, generateRandomString());
		}
		cv.put(Contacts.Phones.PERSON_ID, id);
		cv.put(Contacts.Phones.NUMBER, number);
		cv.put(Contacts.Phones.TYPE, type);
		cv.put(Contacts.Phones.ISPRIMARY, (isPrimary?1:0));
		Uri uriPhone;
		try {
			uriPhone = cr.insert(Contacts.Phones.CONTENT_URI, cv);
			if (uriPhone == null) {
				return null;
			}
		} catch (SQLException e) {
			return null;
		}
		NativeDetail nd = new NativeDetail();
		nd.mValue1 = number;
		nd.mValue2 = String.valueOf(type);
		nd.mIsPrimary = isPrimary;
		nd.mId = (int)ContentUris.parseId(uriPhone);
		return nd;
	}

	public NativeDetail addNativeContactMethod(ContentResolver cr, int id, int kind, boolean isPrimary) {
		ContentValues cv = new ContentValues();
		cv.put(Contacts.ContactMethods.PERSON_ID, id);
		String data = null;
		switch (kind) {
		case CONTACT_METHODS_KIND_EMAIL:
			data = "testtesttest@test.com";
			break;
		case CONTACT_METHODS_KIND_ADDRESS:
			data = generateNativeAddress();
			break;
		default:
			data = generateRandomString();
			break;
		}
		int type = generateRandomInt() & 3;
		if (type == 0) {
			cv.put(Contacts.ContactMethods.LABEL, generateRandomString());
		}
		cv.put(Contacts.ContactMethods.DATA, data);
		cv.put(Contacts.ContactMethods.TYPE, type);
		cv.put(Contacts.ContactMethods.KIND, kind);
		cv.put(Contacts.ContactMethods.ISPRIMARY, (isPrimary?1:0));
		Uri uriCm;
		try {
			uriCm = cr.insert(Contacts.ContactMethods.CONTENT_URI, cv);
			if (uriCm == null) {
				return null;
			}
		} catch (SQLException e) {
			return null;
		}
		NativeDetail nd = new NativeDetail();
		nd.mValue1 = data;
		nd.mValue2 = String.valueOf(kind);
		nd.mValue3 = String.valueOf(type);
		nd.mIsPrimary = isPrimary;
		nd.mId = (int)ContentUris.parseId(uriCm);
		return nd;
	}

	public boolean addNativeOrg(ContentResolver cr, int id, boolean isPrimary, List<NativeDetail> orgList, List<NativeDetail> titleList) {
		ContentValues cv = new ContentValues();
		String company = generateRandomString();
		String title = generateRandomString();
		int type = generateRandomInt() % 3;
		if (type == 0) {
			cv.put(Contacts.Organizations.LABEL, generateRandomString());
		}
		cv.put(Contacts.Organizations.PERSON_ID, id);
		cv.put(Contacts.Organizations.COMPANY, company);
		cv.put(Contacts.Organizations.TITLE, title);
		cv.put(Contacts.Organizations.TYPE, type);
		cv.put(Contacts.Organizations.ISPRIMARY, (isPrimary?1:0));
		Uri uriOrg;
		try {
			uriOrg = cr.insert(Contacts.Organizations.CONTENT_URI, cv);
			if (uriOrg == null) {
				return false;
			}
		} catch (SQLException e) {
			return false;
		}
		NativeDetail ndOrg = new NativeDetail();
		ndOrg.mValue1 = company;
		ndOrg.mValue2 = null;
		ndOrg.mValue3 = String.valueOf(type);
		ndOrg.mIsPrimary = isPrimary;
		ndOrg.mId = (int)ContentUris.parseId(uriOrg);
		orgList.add(ndOrg);
		NativeDetail ndTitle = new NativeDetail();
		ndTitle.mValue1 = title;
		ndTitle.mValue2 = null;
		ndTitle.mValue3 = null;
		ndTitle.mIsPrimary = false;
		ndTitle.mId = (int)ContentUris.parseId(uriOrg);
		titleList.add(ndTitle);
		return true;
	}
	
	public String generateNativeAddress() {
		String sep = ", ";
		if ((generateRandomInt() & 1) == 1) {
			sep = "\n";
		}
		switch (generateRandomInt() % 5) {
		case 0:
			return "Manchester";
		case 1:
			return "3 test road" + sep + "Manchester";
		case 2:
			return "3 test road" + sep + "Manchester" + sep + "M28 2AL";
		case 3:
			return "3 test road" + sep + "Manchester" + sep + "M28 2AL" + sep + "United Kingdom";
		default:
			return "3 test road" + sep + "Manchester" + sep + "Gtr Manchester" + sep + "M28 2AL" + sep + "United Kingdom";
		}
	}

	public void fixPreferred(Contact testContact) {
		boolean donePhone = false;
		boolean doneEmail = false;
		boolean doneAddress = false;
		boolean doneOrg = false;
		for (ContactDetail detail : testContact.details) {
			switch (detail.key) {
				case VCARD_PHONE:
					if (!donePhone) {
						donePhone = true;
						detail.order = ContactDetail.ORDER_PREFERRED;
					} else {
						detail.order = ContactDetail.ORDER_NORMAL;
					}
					break;
				case VCARD_EMAIL:
					if (!doneEmail) {
						doneEmail = true;
						detail.order = ContactDetail.ORDER_PREFERRED;
					} else {
						detail.order = ContactDetail.ORDER_NORMAL;
					}
					break;
				case VCARD_ADDRESS:
					if (!doneAddress) {
						doneAddress = true;
						detail.order = ContactDetail.ORDER_PREFERRED;
					} else {
						detail.order = ContactDetail.ORDER_NORMAL;
					}
					break;
				case VCARD_ORG:
					if (!doneOrg) {
						doneOrg = true;
						detail.order = ContactDetail.ORDER_PREFERRED;
					} else {
						detail.order = ContactDetail.ORDER_NORMAL;
					}
					break;
			}
		}
	}
	
	/***
	 * Compare two given contacts.
	 * @param firstContact contact to compare
	 * @param secondContact contact to compare
	 * @return identical true if given contacts are identical 
	 */
	public static boolean doContactsMatch(Contact firstContact, Contact secondContact) {
		boolean identical = doContactsFieldsMatch(firstContact, secondContact);

		if (firstContact.contactID != null && !firstContact.contactID.equals(secondContact.contactID)) {
			identical = false;
		}
		
		if (firstContact.sources != null && secondContact.sources != null) {
			if (firstContact.sources.size() != secondContact.sources.size()) {
				identical = false;
			} else {
				for (int i = 0 ; i < firstContact.sources.size() ; i++) {
					if (firstContact.sources.get(i) != null &&  
							!firstContact.sources.get(i).equals(secondContact.sources.get(i))) {
						identical = false;
						break;
					}
				}
			}
		} else if (firstContact.sources == null && secondContact.sources != null) {
			identical = false;
		} else if (firstContact.sources != null && secondContact.sources == null) {
			identical = false;
		}
		
		if (firstContact.groupList != null && secondContact.groupList != null) {
			if (firstContact.groupList.size() != secondContact.groupList.size()) {
				identical = false;
			} else {
				for (int i = 0 ; i < firstContact.groupList.size() ; i++) {
					if (firstContact.groupList.get(i) != null && 
							!firstContact.groupList.get(i).equals(secondContact.groupList.get(i))) {
						identical = false;
						break;
					}
				}
			}
		} else if (firstContact.groupList == null && secondContact.groupList != null) {
			identical = false;
		} else if (firstContact.groupList != null && secondContact.groupList == null) {
			identical = false;
		}
		return identical;
	}

	/**
	 * Check if ContactsTable entries match.
	 * @param firstContact contact to compare
	 * @param secondContact contact to compare
	 * @return identical 
	 */
	public static boolean doContactsFieldsMatch(Contact firstContact,
			Contact secondContact) {
		boolean identical = true;
		if (firstContact.localContactID != null && !firstContact.localContactID.equals(secondContact.localContactID)) {
			identical = false;
		}
		if (firstContact.userID != null && !firstContact.userID.equals(secondContact.userID)) {
			identical = false;
		}
		if (firstContact.aboutMe != null && !firstContact.aboutMe.equals(secondContact.aboutMe)) {
			identical = false;
		}
		if (firstContact.friendOfMine != null &&
				!firstContact.friendOfMine.equals(secondContact.friendOfMine)) {
			identical = false;
		}
		if (firstContact.deleted != null && !firstContact.deleted.equals(secondContact.deleted)) {
			identical = false;
		}
		if (firstContact.gender != null && !firstContact.gender.equals(secondContact.gender)) {
			identical = false;
		}
		if (firstContact.synctophone != null && !firstContact.synctophone.equals(secondContact.synctophone)) {
			identical = false;
		}
		if (firstContact.nativeContactId != null && !firstContact.nativeContactId.equals(secondContact.nativeContactId)) {
			identical = false;
		}
		if (firstContact.updated != null && !firstContact.updated.equals(secondContact.updated)) {
			identical = false;
		}
		return identical;
	}
	

	public static Long fetchSyncServerId(Long localDetailID,
			SQLiteDatabase readableDb) {
		Long value = null;
		try {
			Cursor c = readableDb.rawQuery("SELECT "
					+ Field.SERVERSYNCCONTACTID + " FROM ContactDetails WHERE "
					+ Field.DETAILLOCALID + "=" + localDetailID, null);
			if (c.moveToFirst()) {
				if (!c.isNull(0)) {
					value = c.getLong(0);
				}
			}
			c.close();
		} catch (SQLException e) {
		}
		return value;
	}
}
