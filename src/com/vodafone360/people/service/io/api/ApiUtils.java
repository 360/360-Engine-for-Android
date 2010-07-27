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

package com.vodafone360.people.service.io.api;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.datatypes.Album;
import com.vodafone360.people.datatypes.Content;
import com.vodafone360.people.datatypes.EntityKey;
/**
 * Class which provides helper functions for assembling Vectors of data prior to
 * Hessian encoding.
 */
public class ApiUtils {

    /**
     * Create a Hash table from supplied Map, this is passed to Hessian encoder
     * to create Hessian encoded request body.
     * 
     * @param map The source map.
     * @return Hash table from supplied Map.
     */
    protected static Hashtable<String, Object> createHashTable(Map<String, List<String>> map) {
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();

        Set<Map.Entry<String, List<String>>> set = map.entrySet();
        for (Map.Entry<String, List<String>> obj : set) {
            hashtable.put(obj.getKey(), new Vector<String>(obj.getValue()));
        }
        return hashtable;
    }

    /**
     * Create vector from list of ContactDetails. This vector is passed to the
     * Hessian encoder for generation of Hessian encoded message body.
     * 
     * @param list List of contact details.
     * @return New vector.
     */
    protected static Vector<Object> createVectorOfContactDetail(List<ContactDetail> list) {
        Vector<Object> vector = new Vector<Object>();
        for (int i = 0; i < list.size(); i++) {
            vector.add(list.get(i).createHashtable());
        }
        return vector;
    }

    /**
     * Create Vector of of Hash table items from a list of Contacts (each
     * Contact being represented by a Hash table). This is supplied to Hessian
     * encoder.
     * 
     * @param list List-array of Contacts.
     * @return Vector of Hash-tables representing Contact list.
     */
    protected static Vector<Object> createVectorOfContact(List<Contact> list) {
        Vector<Object> vector = new Vector<Object>();
        for (int i = 0; i < list.size(); i++) {
            vector.add(list.get(i).createHashtable());
        }
        return vector;
    }

    /**
     * Create Vector of of Hash table items from a list of GroupItems (each
     * GroupItems being represented by a Hash table). This is supplied to
     * Hessian encoder.
     *
     * @param list List-array of Contacts.
     * @return Vector of Hash-tables representing GroupItems list.
     */
    protected static Vector<Object> createVectorOfGroup(List<GroupItem> list) {
        Vector<Object> vector = new Vector<Object>();
        for (int i = 0; i < list.size(); i++) {
            vector.add(list.get(i).createHashtable());
        }
        return vector;
    }

    /**
     * Create Vector of of Hash table items from.
     *  a list of ContentIPDataType (each.
     * ContentIP being represented by a Hash table). This is supplied to Hessian
     * encoder.
     *
     * @param list List-array of ContentIPDatatype.
     * @return Vector of Hash-tables representing Contact list.
     */
    protected static Vector<Object> createVectorOfContentIPDataType(
                                         final List<Content> list) {
        Vector<Object> vector = new Vector<Object>();
        for (int i = 0; i < list.size(); i++) {
            vector.add(list.get(i).createHashtable());
        }
        return vector;
    }
    /**
     * Create Vector of of Hash table items from a list of GroupItems (each
     * GroupItems being represented by a Hash table). This is supplied to
     * Hessian encoder.
     *
     * @param list List-array of albums.
     * @return Vector of Hash-tables representing GroupItems list.
     */
    public static Vector<Object> createVectorOfAlbumType(
                               final List<Album> list) {
        Vector<Object> vector = new Vector<Object>();
        for (int i = 0; i < list.size(); i++) {
            vector.add(list.get(i).createHashtable());
        }
        return vector;

    }

    /**
     *
     * @param list list of entity keys to be converted into hashtable.
     * @return Vector of objects
     */
    public static Vector<Object>
             createVectorOfEntity(final List<EntityKey> list) {
        Vector<Object> vector = new Vector<Object>();
        for (int i = 0; i < list.size(); i++) {
            vector.add(list.get(i).createHashtable());
        }
        return vector;
    }

}
