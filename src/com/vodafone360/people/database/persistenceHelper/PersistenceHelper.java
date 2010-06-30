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

package com.vodafone360.people.database.persistenceHelper;

import java.lang.reflect.Field;
import java.util.List;

import android.database.Cursor;

import com.vodafone360.people.database.persistenceHelper.Persistable.Column;
import com.vodafone360.people.database.persistenceHelper.Persistable.Table;

/**
 * Contains Methods for persisting classes to a DB and for reading values from
 * DB and creating objects out of it
 * <p>
 * <p>
 * 
 * @version %I%, %G%
 */
public class PersistenceHelper {

    /**
     * Determines if an class implements the persistable interface.
     * 
     * @param c Class whih is to be checked
     * @return true if the given class implements the needed interface, false
     *         otherwise
     */
    public static boolean isPersistable(final Class<?> c) {
        Class<?> implementedInterfaces[] = c.getInterfaces();
        for (Class<?> implementedInterface : implementedInterfaces) {
            if (implementedInterface == Persistable.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * NOT READY NOW - DO NOT USE.
     * 
     * @param o Object to be saved
     */
    public static void saveObject(Object o) {
        if (!isPersistable(o.getClass())) {
            throw new NotPersistableException(
                    "Class must implement Persistable interface to be mapped");
        }

        Table table = o.getClass().getAnnotation(Table.class);
        @SuppressWarnings("unused")
        String tablename = table.name();
    }

    /**
     * NOT READY NOW - DO NOT USE.
     * 
     * @param o Object to be inserted into the database
     */
    public static void insertObject(final Object o) {
        if (!isPersistable(o.getClass())) {
            throw new NotPersistableException(
                    "Class must implement Persistable interface to be mapped");
        }

        Table table = o.getClass().getAnnotation(Table.class);
        @SuppressWarnings("unused")
        String tablename = table.name();

    }

    /**
     * NOT READY NOW - DO NOT USE.
     * 
     * @param list List to be saved in the database
     */
    public static void saveObjects(final List<?> list) {
        for (Object o : list) {
            if (!isPersistable(o.getClass())) {
                throw new NotPersistableException(
                        "Class must implement Persistable interface to be mapped");
            }

            Table table = o.getClass().getAnnotation(Table.class);
            @SuppressWarnings("unused")
            String tablename = table.name();
        }
    }

    /**
     * NOT READY NOW - DO NOT USE.
     * 
     * @param list List to be inserted into database
     */
    public static void insertObjects(final List<?> list) {
        for (Object o : list) {
            if (!isPersistable(o.getClass())) {
                throw new NotPersistableException(
                        "Class must implement Persistable interface to be mapped");
            }

            Table table = o.getClass().getAnnotation(Table.class);
            @SuppressWarnings("unused")
            String tablename = table.name();

        }
    }

    /**
     * Populates an Object with values from Database. This is very low level
     * because you have to open the cursor(make the select) by yourself and also
     * create the right object to take the values
     * 
     * @param object Object from an Class which implements persistable interface
     * @param cursor Cursor to a valid resultset
     * @throws Exception If given object is not persistable
     */
    public static void mapCursorToObject(final Object object, final Cursor cursor) throws Exception {

        if (!isPersistable(object.getClass())) {
            throw new NotPersistableException(
                    "Class must implement Persistable interface to be mapped");
        }

        Field[] fields = object.getClass().getFields();
        for (Field field : fields) {
            Column annotation = field.getAnnotation(Column.class);
            if (annotation != null) {
                String columnName = annotation.name();
                int columnIndex = cursor.getColumnIndex(columnName);

                if (field.getType() == String.class) {
                    field.set(object, cursor.getString(columnIndex));
                }

                if (field.getType() == Short.class) {
                    field.set(object, cursor.getShort(columnIndex));
                }

                if (field.getType() == Integer.class) {
                    field.set(object, cursor.getInt(columnIndex));
                }

                if (field.getType() == Long.class) {
                    field.set(object, cursor.getLong(columnIndex));
                }

                if (field.getType() == Float.class) {
                    field.set(object, cursor.getFloat(columnIndex));
                }

                if (field.getType() == Double.class) {
                    field.set(object, cursor.getDouble(columnIndex));
                }

                if (field.getType() == Boolean.class) {
                    field.set(object, cursor.getShort(columnIndex) == 1);
                }

            }
        }

    }

}
