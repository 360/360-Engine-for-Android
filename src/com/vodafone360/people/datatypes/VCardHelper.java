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

package com.vodafone360.people.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import android.text.TextUtils;

import android.text.format.Time;
import android.util.TimeFormatException;

/**
 * Helper class for handling VCard information associated with Contacts either
 * from People server/client or from Native database. Also used to help with
 * handling of Call log/Message log events displayed in Timeline.
 */
public class VCardHelper {
    public static final char LIST_SEPARATOR = ';';

    public static final char SUB_LIST_SEPARATOR = ',';

    /**
     * Item class representing vCard item.
     */
    private static class Item {
        Item(String value, boolean isSubValue) {
            mValue = value;
            mIsSubValue = isSubValue;
        }

        String mValue;

        boolean mIsSubValue;
    }

    /**
     * Handle VCard name fields.
     */
    public static class Name {
        public String firstname = null;

        public String midname = null;

        public String surname = null;

        public String title = null;

        public String suffixes = null;

        /**
         * Default constructor.
         */
        public Name() {
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            
            if(!TextUtils.isEmpty(title)) {
                sb.append(title);
                sb.append(' ');
            }
            
            if(!TextUtils.isEmpty(firstname)) {
                sb.append(firstname);
                sb.append(' ');
            }
            
            if(!TextUtils.isEmpty(midname)) {
                sb.append(midname);
                sb.append(' ');
            }
            
            if(!TextUtils.isEmpty(surname)) {
                sb.append(surname);
                sb.append(' ');
            }
            
            if(!TextUtils.isEmpty(suffixes)) {
                sb.append(suffixes);
            }
            
            return sb.toString().trim();
        }
    }

    /**
     * Handle VCard organisation fields.
     */
    public static class Organisation {
        public String name = null;

        public final List<String> unitNames = new ArrayList<String>();

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return name + ", " + unitNames;
        }
    }

    /**
     * Handle VCard postal address
     */
    public static class PostalAddress {
        public String postOfficeBox = null;

        public String addressLine1 = null;

        public String addressLine2 = null;

        public String city = null;

        public String postCode = null;

        public String county = null;

        public String country = null;

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            
            if(!TextUtils.isEmpty(postOfficeBox)) {
                sb.append(postOfficeBox);
                sb.append("\n");
            }
            
            if(!TextUtils.isEmpty(addressLine1)) {
                sb.append(addressLine1);
                sb.append("\n");
            }
            
            if(!TextUtils.isEmpty(addressLine2)) {
                sb.append(addressLine2);
                sb.append("\n");
            }
            
            if(!TextUtils.isEmpty(city)) {
                sb.append(city);
                sb.append("\n");
            }
            
            if(!TextUtils.isEmpty(county)) {
                sb.append(county);
                sb.append("\n");
            }
            
            if(!TextUtils.isEmpty(postCode)) {
                sb.append(postCode);
                sb.append("\n");
            }
            
            if(!TextUtils.isEmpty(country)) {
                sb.append(country);
                sb.append("\n");
            }

            if (sb.length() > 0) {
                return sb.toString().substring(0, sb.length() - 1);
            } else {
                return "";
            }
        }

        /**
         * Whether postal address item is empty.
         * 
         * @return false if address has any content, null otherwise
         */
        public boolean isEmpty() {
            return (TextUtils.isEmpty(postOfficeBox) && 
                    TextUtils.isEmpty(addressLine1) &&
                    TextUtils.isEmpty(addressLine2) && 
                    TextUtils.isEmpty(city) &&
                    TextUtils.isEmpty(county) &&
                    TextUtils.isEmpty(country) &&
                    TextUtils.isEmpty(postCode));
        }
    }

    /**
     * Create String from array of VCard items.
     * 
     * @param itemList List array of VCard items.
     * @return String constructed from array of Items.
     */
    private static String createVCardList(List<Item> itemList) {
        StringBuffer value = new StringBuffer();
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);
            if (item.mValue != null) {
                String val2 = item.mValue.replace(";", "\\;");
                String val3 = val2.replace(",", "\\,");
                /*
                 * (JT) TODO: removed the "escaping" of '/' character but my
                 * understanding of vCard format is that only ';' needs to be
                 * escaped but in the code we also escape ',' String val4 =
                 * val3.replace("/", "\\/");
                 */
                value.append(val3);
            }
            if (i < itemList.size() - 1) {
                if (item.mIsSubValue) {
                    value.append(SUB_LIST_SEPARATOR);
                } else {
                    value.append(LIST_SEPARATOR);
                }
            }
        }
        int idx = value.length() - 1;
        while (idx >= 0 && (value.charAt(idx) == ';' || value.charAt(idx) == ',')) {
            idx--;
        }
        return value.substring(0, idx + 1);
    }

    /**
     * Create array of VCard items from supplied String.
     * 
     * @param val String containing VCard items.
     * @param itemList List of VCard items to populate.
     */
    private static void getVCardList(String val, List<Item> itemList) {
        itemList.clear();
        boolean escaped = false;
        int i = 0;
        String currentItem = "";
        for (i = 0; i < val.length(); i++) {
            char ch = val.charAt(i);
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (escaped || (ch != LIST_SEPARATOR && ch != SUB_LIST_SEPARATOR)) {
                currentItem += ch;
                escaped = false;
                continue;
            }
            if (ch == SUB_LIST_SEPARATOR) {
                itemList.add(new Item(currentItem, true));
            } else {
                itemList.add(new Item(currentItem, false));
            }
            currentItem = "";
        }
        if (currentItem.length() > 0) {
            itemList.add(new Item(currentItem, false));
        }
    }

    /**
     * Return next complete Item from item ListIterator as String. Sub-items are
     * appended to the String.
     * 
     * @param it ListIterator to iterate through Items.
     * @return next Item.
     */
    private static String nextFullItem(ListIterator<Item> it) {
        StringBuffer result = new StringBuffer();
        while (it.hasNext()) {
            Item item = it.next();
            result.append(item.mValue + " ");
            if (!item.mIsSubValue) {
                return result.toString().trim();
            }
        }
        return result.toString();
    }

    /**
     * Return List of Strings made up of next full item and sub-items retrieved
     * from ListIterator.
     * 
     * @param it ListIterator to iterate through Items.
     * @return List of Strings generated from Item.
     */
    private static List<String> nextFullItemArray(ListIterator<Item> it) {
        List<String> stringList = new ArrayList<String>();
        while (it.hasNext()) {
            Item item = it.next();
            stringList.add(item.mValue);
            if (!item.mIsSubValue) {
                return stringList;
            }
        }
        return stringList;
    }

    /**
     * Create a VCard string from a single value Suitable for Url, Internet
     * Address, IM Address, Role, Title and Note
     * 
     * @param textValue The string value to use
     * @return A string in VCard format (does not include types)
     */
    protected static String makeSingleTextValue(String textValue) {
        List<Item> itemList = new ArrayList<Item>();
        itemList.add(new Item(textValue, false));
        return createVCardList(itemList);
    }

    /**
     * Fetch a single string value from a VCard string Suitable for Url,
     * Internet Address, IM Address, Role, Title and Note
     * 
     * @param val A string in VCard format.
     * @return the single text value.
     */
    protected static String getSingleTextValue(String val) {
        if (val == null) {
            return null;
        }
        List<Item> itemList = new ArrayList<Item>();
        getVCardList(val, itemList);
        ListIterator<Item> it = itemList.listIterator();
        return nextFullItem(it);
    }

    /**
     * Return VCard name string from supplied Name.
     * 
     * @param name Name item.
     * @return String generated from supplied Name.
     */
    public static String makeName(Name name) {
        List<Item> itemList = new ArrayList<Item>();
        itemList.add(new Item(name.surname, false));
        itemList.add(new Item(name.firstname, false));
        itemList.add(new Item(name.midname, false));
        itemList.add(new Item(name.title, false));
        itemList.add(new Item(name.suffixes, false));
        return createVCardList(itemList);
    }

    /**
     * Get Name object from supplied String.
     * 
     * @param val String containing name information.
     * @return Name item generated from supplied string.
     */
    public static Name getName(String val) {
        if (val == null) {
            return null;
        }
        Name name = new Name();
        List<Item> itemList = new ArrayList<Item>();
        getVCardList(val, itemList);
        ListIterator<Item> it = itemList.listIterator();
        name.surname = nextFullItem(it);
        name.firstname = nextFullItem(it);
        name.midname = nextFullItem(it);
        name.title = nextFullItem(it);
        name.suffixes = nextFullItem(it);
        return name;
    }

    /**
     * Create VCard date String from supplied Time item.
     * 
     * @param time Time item.
     * @return String generated from supplied Time item.
     */
    protected static String makeDate(Time time) {
        List<Item> itemList = new ArrayList<Item>();
        String dateString = null;
        try {
            dateString = time.format("%Y-%m-%d");
        } catch (TimeFormatException e) {
            return null;
        }
        itemList.add(new Item(dateString, false));
        return createVCardList(itemList);
    }

    /**
     * Get Time item from supplied String.
     * 
     * @param val String containing time information.
     * @return Time item generated from supplied String.
     */
    protected static Time getDate(String val) {
        if (val == null) {
            return null;
        }
        List<Item> itemList = new ArrayList<Item>();
        getVCardList(val, itemList);
        ListIterator<Item> it = itemList.listIterator();
        String dateString = nextFullItem(it);
        dateString = dateString.replace("-", "");
        Time time = new Time();
        try {
            time.parse(dateString);
        } catch (TimeFormatException e) {
            return null;
        }
        return time;
    }

    /**
     * Generate VCard email address from simple String representation.
     * 
     * @param emailAddress simple email address.
     * @return email address in VCard style String.
     */
    public static String makeEmail(String emailAddress) {
        return makeSingleTextValue(emailAddress);
    }

    /**
     * Create simple String representation of email address from supplied VCard
     * email String.
     * 
     * @param val VCard email String.
     * @return email address as simple String.
     */
    protected static String getEmail(String val) {
        return getSingleTextValue(val);
    }

    /**
     * Create VCard telephone number String.
     * 
     * @param telNumber String containing telephone number
     * @return VCatrd representation of telephone number.
     */
    protected static String makeTel(String telNumber) {
        return makeSingleTextValue(telNumber);
    }

    /**
     * Get telephone number as String from VCard representation.
     * 
     * @param val VCard representation of telephone number.
     * @return String containing telephone number.
     */
    protected static String getTel(String val) {
        return getSingleTextValue(val);
    }

    /**
     * Create VCard representation of postal address.
     * 
     * @param address PostalAddress item.
     * @return VCard representation of postal address.
     */
    public static String makePostalAddress(PostalAddress address) {
        List<Item> itemList = new ArrayList<Item>();
        itemList.add(new Item(address.postOfficeBox, false));
        itemList.add(new Item(address.addressLine1, false));
        itemList.add(new Item(address.addressLine2, false));
        itemList.add(new Item(address.city, false));
        itemList.add(new Item(address.county, false));
        itemList.add(new Item(address.postCode, false));
        itemList.add(new Item(address.country, false));
        return createVCardList(itemList);
    }

    /**
     * Get PostalAddress from supplied string.
     * 
     * @param val String containing address.
     * @return Postal address generated from supplied String.
     */
    public static PostalAddress getPostalAddress(String val) {
        HttpConnectionThread.logW("$$$$$$$$$$$$$$$$$$$$$$$$$", "Location: " + val);
        
        if (val == null) {
            return null;
        }
        PostalAddress address = new PostalAddress();
        List<Item> itemList = new ArrayList<Item>();
        getVCardList(val, itemList);
        
        HttpConnectionThread.logW("$$$$$$$$$$$$$$$$$$$$$$$$$", "List: " + itemList.toString());
        
        ListIterator<Item> it = itemList.listIterator();
        address.postOfficeBox = nextFullItem(it);
        address.addressLine1 = nextFullItem(it);
        address.addressLine2 = nextFullItem(it);
        address.city = nextFullItem(it);
        address.county = nextFullItem(it);
        address.postCode = nextFullItem(it);
        address.country = nextFullItem(it);
        return address;
    }

    /**
     * Create VCard Organisation item.
     * 
     * @param org Organisation item.
     * @return VCard organisation representation.
     */
    public static String makeOrg(Organisation org) {
        List<Item> itemList = new ArrayList<Item>();
        itemList.add(new Item(org.name, false));
        for (String s : org.unitNames) {
            itemList.add(new Item(s, false));
        }
        return createVCardList(itemList);
    }

    /**s
     * Create Organisation item from String containing VCArd organisation
     * representation.
     * 
     * @param val String containing VCard organisation.
     * @return Organisation item generated.
     */
    public static Organisation getOrg(String val) {
        if (val == null) {
            return null;
        }
        Organisation org = new Organisation();
        List<Item> itemList = new ArrayList<Item>();
        getVCardList(val, itemList);
        ListIterator<Item> it = itemList.listIterator();
        org.name = nextFullItem(it);
        org.unitNames.addAll(nextFullItemArray(it));
        return org;
    }
    
    /**
     * Parses the Company name from a VCard Organization String.
     * 
     * @param value the VCard Organization String to parse
     * @return the company name or null if not found
     */
    public static String parseCompanyFromOrganization(String value) {
        
        if (value == null) return null;
        
        int index = -1;
        char prevChar = '.'; // set it to whatever is different from '\\'
        for (int i = 0; i < value.length(); i++) {
            
            if (value.charAt(i) == ';' && prevChar != '\\') {
                index = i;
                break;
            }
            prevChar = value.charAt(i);
        }
        
        return value.substring(0, index != -1 ? index : value.length());
    }
    
    /**
     * Splits the VCard Organization value to return only the departments
     * 
     * Note: it will return the ';' separated departments
     * 
     * @param value the value to split
     * @return the VCard departments or empty String if none
     */
    public static String splitDepartmentsFromOrganization(String value) {
        
        if (value == null) return "";
        
        int index = -1;
        char prevChar = '.'; // set it to whatever is different from '\\'
        for (int i = 0; i < value.length(); i++) {
            
            if (value.charAt(i) == ';' && prevChar != '\\') {
                index = i;
                break;
            }
            prevChar = value.charAt(i);
        }
        
        if (index != -1) {
            return value.substring(index, value.length());
        } else {
            return "";
        }
    }
    
    /**
     * Tells whether or not the provided VCard value is empty.
     * 
     * Note: a VCard value is empty if
     *       - null
     *       - "" empty String
     *       - ";;;" contains only ';'
     * 
     * @return true if empty, false if not
     */
    public static boolean isEmptyVCardValue(String value) {
        
        if (value != null) {

            char prevChar = '.';
            for (int i = 0; i < value.length(); i++) {
                
                if (value.charAt(i) != ';' && prevChar != '\\') {
                    return false;
                }
                prevChar = value.charAt(i);
            }
        }
        
        return true;
    }

    /**
     * Generate List array of items (as Strings) from single String using
     * ListIterator.
     * 
     * @param list List array to be populated
     * @param val Single String containing items.
     */
    protected static void getStringList(List<String> list, String val) {
        if (val == null) {
            return;
        }
        List<Item> itemList = new ArrayList<Item>();
        getVCardList(val, itemList);
        ListIterator<Item> it = itemList.listIterator();
        while (it.hasNext()) {
            Item item = it.next();
            list.add(item.mValue);
        }
    }
}
