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

package com.vodafone360.people.database.utils;

import android.database.Cursor;

import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.ActivityItem.Type;

/***
 * SQLite utility methods.
 */
public final class SqlUtils {

    /**
     * Constant for ","
     */
    public final static String COMMA = ",";
    /**
     * Empty string ("") constant.
     */
    public static final String EMPTY = "";

    /***
     * Private constructor to prevent instantiation.
     */
    private SqlUtils() {
        // Do nothing.
    }

    /***
     * Return the value of the given field, or NULL if the value is not
     * present.
     *
     * @param cursor SQLite cursor.
     * @param field Column identifier.
     * @param defaultValue Value returned if value not found in Cursor.
     * @return Value or NULL if not present.
     */
    public static Long setLong(final Cursor cursor, final String field,
            final Long defaultValue) {
        final int index = cursor.getColumnIndex(field);
        if (index != -1 && !cursor.isNull(index)) {
            return cursor.getLong(index);
        } else {
            return defaultValue;
        }
    }

    /***
     * Return the value of the given field, or NULL if the value is not present.
     *
     * @param cursor SQLite cursor.
     * @param field Column identifier.
     * @return Value or NULL if not present.
     */
    public static String setString(final Cursor cursor, final String field) {
        final int index = cursor.getColumnIndex(field);
        if (index != -1 && !cursor.isNull(index)) {
            return cursor.getString(index);
        } else {
            return null;
        }
    }

    /***
     * Return the value of the given field, or NULL if the value is not present.
     *
     * @param cursor SQLite cursor.
     * @param field Column identifier.
     * @param defaultValue Value returned if value not found in Cursor.
     * @return Value or NULL if not present.
     */
    public static Integer setInt(final Cursor cursor, final String field,
            final Integer defaultValue) {
        final int index = cursor.getColumnIndex(field);
        if (index != -1 && !cursor.isNull(index)) {
            return cursor.getInt(index);
        } else {
            return defaultValue;
        }
    }

    /***
     * Return the value of the given field, or defaultValue if the value is not
     * present.
     *
     * @param cursor SQLite cursor.
     * @param field Column identifier.
     * @param defaultValue Value returned if value not found in Cursor.
     * @return Value or NULL if not present.
     */
    public static Boolean setBoolean(final Cursor cursor, final String field,
            final Boolean defaultValue) {
        final int index = cursor.getColumnIndex(field);
        if (index != -1 && !cursor.isNull(index)) {
            return cursor.getInt(index) != 0;
        } else {
            return defaultValue;
        }
    }

    /***
     * Return the value of the given field, or NULL if the value is not present.
     *
     * @param cursor SQLite cursor.
     * @param field Column identifier.
     * @return Value or NULL if not present.
     */
    public static Type setActivityItemType(final Cursor cursor,
            final String field) {
        final int index = cursor.getColumnIndex(field);
        if (index != -1 && !cursor.isNull(index)) {
            return ActivityItem.Type.findType(cursor.getString(index));
        } else {
            return ActivityItem.Type.ACTIVITY_EVENT_UNKNOWN;
        }
    }

    /***
     * Return the value of the given field, or NULL if the value is not present.
     *
     * @param cursor SQLite cursor.
     * @param field Column identifier.
     * @return Value or NULL if not present.
     */
    public static TimelineSummaryItem.Type setTimelineSummaryItemType(
            final Cursor cursor, final String field) {
        final int index = cursor.getColumnIndex(field);
        if (index != -1 && !cursor.isNull(index)) {
            return TimelineSummaryItem.getType(cursor.getInt(index));
        } else {
            return TimelineSummaryItem.Type.UNKNOWN;
        }
    }

    /***
     * Return the value of the given field, or NULL if the value is not present.
     *
     * @param cursor SQLite cursor.
     * @param field Column identifier.
     * @return Value or NULL if not present.
     */
    public static byte[] setBlob(final Cursor cursor, final String field) {
        final int index = cursor.getColumnIndex(field);
        if (index != -1 && !cursor.isNull(index)) {
            return cursor.getBlob(index);
        } else {
            return null;
        }
    }
}