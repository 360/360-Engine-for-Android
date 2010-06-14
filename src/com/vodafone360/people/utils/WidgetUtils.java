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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.utils.LogUtils;

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
        MainApplication mainApplication = (MainApplication)context.getApplicationContext();
        int[] list = mainApplication.getCache().getWidgetIdList(context.getApplicationContext());
        if (list != null && list.length > 0) {
            LogUtils.logD("WidgetUtils.kickWidgetUpdateNow() Updating widget list.length" + "["
                    + list.length + "]");

            Intent widgetUpdate = new Intent();
            widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, list);

            // Make this pending intent unique.
            widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(URI_SCHEME
                    + URI_DATA), String.valueOf(list[0])));

            // Schedule the new widget for updating.
            ((AlarmManager)mainApplication.getSystemService(Context.ALARM_SERVICE)).set(
                    AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), PendingIntent
                            .getBroadcast(context.getApplicationContext(), 0, widgetUpdate,
                                    PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            LogUtils.logE("WidgetUtils.kickWidgetUpdateNow() There are no widgets to update");
        }
    }
}
