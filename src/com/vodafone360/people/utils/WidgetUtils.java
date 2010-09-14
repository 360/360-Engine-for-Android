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

import com.vodafone360.people.Intents;

import android.content.Context;
import android.content.Intent;

/*
 * Widget utility Class.
 */
public class WidgetUtils {

    protected static final String URI_SCHEME = "people_widget";

    protected static final String URI_DATA = "://widget/id/";

    /**
     * Sends an update event to the widget.
     * 
     * @param context - Android context.
     */
    public static void kickWidgetUpdateNow(Context context) {
        
    	context.sendBroadcast(new Intent(Intents.UPDATE_WIDGET));
    }
}
