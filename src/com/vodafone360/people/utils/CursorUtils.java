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

package com.vodafone360.people.utils;

import android.database.Cursor;

public class CursorUtils {

    public static void closeCursor(Cursor cursor) {
        if(cursor != null) {
            cursor.close();
        }
    }
    
    public static String getString(Cursor cursor, String columnName) {
          return cursor.getString(cursor.getColumnIndex(columnName));
    }

    public static int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }
    
    public static long getLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }
    

    public static Integer getInteger(Cursor cursor, String columnName) {
        final int columnIndex = cursor.getColumnIndex(columnName);
        return cursor.getInt(columnIndex);
    }
    public static int getInt(Cursor cursor, String columnName, int missingValue) {
        final int columnIndex = cursor.getColumnIndex(columnName);
          return cursor.isNull(columnIndex) ? missingValue : cursor.getInt(columnIndex);
    }

    public static long getLong(Cursor cursor, String columnName, long missingValue) {
          final int columnIndex = cursor.getColumnIndex(columnName);
          return cursor.isNull(columnIndex) ? missingValue : cursor.getLong(columnIndex);
    }

    public static boolean isNull(Cursor cursor, String columnName) {
          return cursor.isNull(cursor.getColumnIndex(columnName));
    }  
}
