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

/**
 * BaseDataType encapsulating a text response returned from server
 */
public class SimpleText extends BaseDataType {

    public StringBuffer mValue = null;

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return SIMPLE_TEXT_DATA_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Simple Text: \n" + mValue;
    }

    /**
     * Add test to SimpleText item
     * 
     * @param text String containing text to add.
     */
    public void addText(String text) {
        if (mValue == null) {
            mValue = new StringBuffer();
        }
        mValue.append(text);
    }
}
