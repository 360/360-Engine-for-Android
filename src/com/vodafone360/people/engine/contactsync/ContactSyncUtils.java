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

import java.util.List;

import com.vodafone360.people.database.tables.NativeChangeLogTable.ContactChangeInfo;

public class ContactSyncUtils {

    /**
     * Utility function to search the given ContactChangeInfo list for an item
     * with the specified Native Contact ID. This is an optimised binary search
     * which assumes that the list is already sorted by Native contact ID in the
     * Database query.
     * 
     * @param nativeContactId Native contact ID to search for in given list.
     * @param list List of deleted contacts taken from the NativeChangeLog
     *            table.
     * @return Index of the change in the list, if the nativeContactId is found,
     *         -1 if not.
     */
    public static int findIdInOrderedList(int nativeContactId, List<ContactChangeInfo> list) {
        if (list == null || list.size() == 0) {
            return -1;
        }

        /*
         * Binary search through list.
         */
        int searchMin = 0;
        int searchMax = list.size() - 1;
        while (searchMin < searchMax) {
            int searchMid = (searchMin + searchMax) >> 1; // Divide the sum by
                                                          // two to get the
                                                          // middle
            if (list.get(searchMid).mNativeContactId != null) {
                if (nativeContactId > list.get(searchMid).mNativeContactId.intValue()) { // TODO:
                                                                                         // Potential
                                                                                         // NullPointerException
                    searchMin = searchMid + 1;
                } else {
                    searchMax = searchMid;
                }
            } else {
                /*
                 * We have found a NULL value, so as values with storage class
                 * NULL come first, followed by INTEGER, we should raise the Min
                 * value.
                 */
                searchMin = searchMid + 1;
            }
        }

        if (list.get(searchMin).mNativeContactId != null
                && nativeContactId == list.get(searchMin).mNativeContactId.intValue()) {

            /*
             * Search downwards until we find the given nativeContactId. Return
             * -1 if we reach some NULL values.
             */
            for (; searchMin >= 0; searchMin--) {
                if (list.get(searchMin).mNativeContactId == null) {
                    return -1;
                } else if (nativeContactId != list.get(searchMin).mNativeContactId.intValue()) {
                    return searchMin + 1;
                }
            }
            return 0;
        }
        return -1;
    }
}
