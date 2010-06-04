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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotations for declaring persistable Classes and a marker interface.
 * <p>
 * A Class which should be persisted in DB using the persistenceHelper must
 * implement the persistable interface and declare its columns with the
 * annotations
 * <p>
 * 
 * @version %I%, %G%
 */
public interface Persistable {

    /**
     * Annotation for binding a field to a column.
     */
    public @Retention(RetentionPolicy.RUNTIME)
    @interface Column {
        /** column name. **/
        String name();
    }

    /**
     * Annotation for binding a class to a table.
     */
    public @Retention(RetentionPolicy.RUNTIME)
    @interface Table {
        /** table name. **/
        String name();
    }

    /**
     * Annotation for declaring an id field.
     */
    public @Retention(RetentionPolicy.RUNTIME)
    @interface Id {

    }

    /**
     * Annotation for declaring an entity.
     */
    public @Retention(RetentionPolicy.RUNTIME)
    @interface Entity {

    }

}
