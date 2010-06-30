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

import com.vodafone360.people.R;
import com.vodafone360.people.datatypes.ContactDetail;

import android.content.res.Resources;

/**
 * Collection of utility functions provided by the Service Engine.
 */
public class ServiceUtils {

    /**
     * Gets user friendly name for the type of contact detail.
     * 
     * @param res - Resource from which to get the string.
     * @param keyType - Detail type.
     * @return User friendly string.
     */
    public static String getDetailTypeString(Resources res, ContactDetail.DetailKeyTypes keyType) {
        if (keyType == ContactDetail.DetailKeyTypes.HOME) {
            return res.getString(R.string.UiUtils_detail_home);

        } else if (keyType == ContactDetail.DetailKeyTypes.WORK) {
            return res.getString(R.string.UiUtils_detail_work);

        } else if (keyType == ContactDetail.DetailKeyTypes.MOBILE) {
            return res.getString(R.string.UiUtils_detail_mobile);

        } else if (keyType == ContactDetail.DetailKeyTypes.CELL) {
            return res.getString(R.string.UiUtils_detail_cell);

        } else if (keyType == ContactDetail.DetailKeyTypes.FAX) {
            return res.getString(R.string.UiUtils_detail_fax);

        } else {
            return res.getString(R.string.UiUtils_detail_other);
        }
    }

}
