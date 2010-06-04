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
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * BaseDataType encapsulating a list of Items associated with data decoded from
 * Hessian response data received from People server. Specifically associated
 * with contact group relation, status message and group privacy responses.
 */
public class ItemList extends BaseDataType {

    /**
     * Enumeration of data-types items that can be returned as an ItemList.
     */
    public enum Type {
        group_privacy,
        status_msg,
        contact_group_relation,
        contact_group_relations,
        long_value;
    }

    private Integer mItemsSize = null;

    public Type mType = null;

    /** List-array of BaseDataType. */
    public final List<BaseDataType> mItemList = new ArrayList<BaseDataType>();

    public ItemList(Type t) {
        mType = t;
    }

    /** {@inheritDoc} */
    @Override
    public String name() {
        return "ItemList";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append(name() + "(" + mItemsSize + ")");
        for (int i = 0; i < mItemList.size(); i++) {
            ret.append("\n  " + mItemList.get(i).toString());
        }
        return ret.toString();
    }

    /**
     * Create ItemList from Hashtable generate by Hessian-decoder.
     * 
     * @param hash Hashtable containing ItemList parameters.
     */
    public void populateFromHashtable(Hashtable<String, Object> hash) {
        final String itemList = "itemlist";

        @SuppressWarnings("unchecked")
        Vector<Hashtable<String, Object>> vect = (Vector<Hashtable<String, Object>>)hash
                .get(itemList);
        
        mItemsSize = vect.size();
        switch (mType) {
            case group_privacy:
                for (Hashtable<String, Object> msghash : vect) {
                    GroupItem group = new GroupItem();
                    mItemList.add(group.createFromHashtable(msghash));
                }
                break;

            case status_msg:
                for (Hashtable<String, Object> msghash : vect) {
                    StatusMsg msg = new StatusMsg();
                    mItemList.add(msg.createFromHashtable(msghash));
                }
                break;
                
            default:
                // Do nothing.
                break;
        }
    }
}
