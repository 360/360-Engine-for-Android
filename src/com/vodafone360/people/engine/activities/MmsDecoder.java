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

package com.vodafone360.people.engine.activities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineNativeTypes;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;

/**
 * Helper class to 'decode' MMS messages to provide suitable content for
 * Timeline summary.
 */
public class MmsDecoder {
    private static final int ANY_CHARSET = 0x00;

    private static final int US_ASCII = 0x03;

    private static final int ISO_8859_1 = 0x04;

    private static final int ISO_8859_2 = 0x05;

    private static final int ISO_8859_3 = 0x06;

    private static final int ISO_8859_4 = 0x07;

    private static final int ISO_8859_5 = 0x08;

    private static final int ISO_8859_6 = 0x09;

    private static final int ISO_8859_7 = 0x0A;

    private static final int ISO_8859_8 = 0x0B;

    private static final int ISO_8859_9 = 0x0C;

    private static final int SHIFT_JIS = 0x11;

    private static final int UTF_8 = 0x6A;

    private static final int BIG5 = 0x07EA;

    private static final int UCS2 = 0x03E8;

    private static final int UTF_16 = 0x03F7;

    private static final String MIMENAME_ANY_CHARSET = "*";

    private static final String MIMENAME_US_ASCII = "us-ascii";

    private static final String MIMENAME_ISO_8859_1 = "iso-8859-1";

    private static final String MIMENAME_ISO_8859_2 = "iso-8859-2";

    private static final String MIMENAME_ISO_8859_3 = "iso-8859-3";

    private static final String MIMENAME_ISO_8859_4 = "iso-8859-4";

    private static final String MIMENAME_ISO_8859_5 = "iso-8859-5";

    private static final String MIMENAME_ISO_8859_6 = "iso-8859-6";

    private static final String MIMENAME_ISO_8859_7 = "iso-8859-7";

    private static final String MIMENAME_ISO_8859_8 = "iso-8859-8";

    private static final String MIMENAME_ISO_8859_9 = "iso-8859-9";

    private static final String MIMENAME_SHIFT_JIS = "shift_JIS";

    private static final String MIMENAME_UTF_8 = "utf-8";

    private static final String MIMENAME_BIG5 = "big5";

    private static final String MIMENAME_UCS2 = "iso-10646-ucs-2";

    private static final String MIMENAME_UTF_16 = "utf-16";

    protected static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");

    private static final int PDU_FROM_FIELD = 0x89;

    private static final int PDU_TO_FIELD = 0x97;

    private static final String THREAD_ID = "thread_id";

    private static final String ID = "_id";

    private static final String ELLIPSIZE = "...";

    /**
     * Fields to be returned from MMS query.
     */
    private static final String[] MMS_STATUS_PROJECTION = new String[] {
            THREAD_ID, "date", ID, "sub", "sub_cs", "msg_box", THREAD_ID
    };

    private static final int COLUMN_DATE = 1;

    private static final int COLUMN_MMS_ID = 2;

    private static final int COLUMN_SUBJECT = 3;

    private static final int COLUMN_SUBJECT_CS = 4;

    private static final int COLUMN_MSG_BOX = 5;

    private static final int COLUMN_MSG_THREAD_ID = 6;

    private static final String[] PART_PROJECTION = new String[] {
            ID, "chset", "cd", "cid", "cl", "ct", "fn", "name"
    };

    private static final int PART_COLUMN_ID = 0;

    private static final int PART_COLUMN_CONTENT_TYPE = 5;

    private static final int MESSAGE_BOX_INBOX = 1;

    private static final int MESSAGE_BOX_SENT = 2;

    private static final String MMS_SORT_ORDER = "date ASC";

    private static final String PART = "part";

    private static final String TEXT_PLAIN = "text/plain";

    private static final int MS_IN_SECONDS = 1000;

    protected static long getTimestamp(Cursor mmsCursor) {
        return mmsCursor.getLong(COLUMN_DATE) * MS_IN_SECONDS;
    }

    /**
     * Get a Cursor for MMS message item from native message log.
     * 
     * @param cr ContentResolver
     * @param timestamp Timestamp to search against (may be null)
     * @return Cursor to item in native message log (may be null)
     */
    protected static Cursor fetchMmsListCursor(ContentResolver cr, boolean refresh,
            long oldestTimestamp, long newestTimestamp) {
        String whereClause = refresh ? "date > " + newestTimestamp : "date < " + oldestTimestamp;
        return cr.query(MmsDecoder.MMS_CONTENT_URI, MMS_STATUS_PROJECTION, whereClause, null,
                MMS_SORT_ORDER);
    }

    /**
     * Get the MMS data for the message at current Cursor position and use it to
     * populate a TimelineSummaryItem. We initially check if the MMS is an Inbox
     * or sent item returning false if this is not the case.
     * 
     * @param context Context.
     * @param cr ContentResolver.
     * @param mmsCursor Cursor pointing to MMS message entry in native message
     *            log.
     * @param item TimeLineSummaryItem to populate using MMS message details
     * @param db Handle to People database.
     * @param maxDescLength maximum length of the description.
     * @return true if we have created the TimelineSummaryItem false if we
     *         haven't (because the MMS is not of a valid type).
     */
    protected static boolean getMmsData(Context context, ContentResolver cr, Cursor mmsCursor,
            TimelineSummaryItem item, DatabaseHelper db, int maxDescLength) {
        int msgId = mmsCursor.getInt(COLUMN_MMS_ID);
        Uri msgUri = MMS_CONTENT_URI.buildUpon().appendPath(Long.toString(msgId)).build();
        ActivityItem.Type type = nativeToNpMessageType(mmsCursor.getInt(COLUMN_MSG_BOX));
        if (type == null) {
            return false;
        }
        String address = getToOrFrom(cr, type, msgUri);
        String sub = mmsCursor.getString(COLUMN_SUBJECT);
        int subcs = mmsCursor.getInt(COLUMN_SUBJECT_CS);

        String subject = TextUtils.isEmpty(sub) ? "" : decodeString(subcs, getMmsBytes(sub));
        item.mTimestamp = mmsCursor.getLong(COLUMN_DATE) * MS_IN_SECONDS;
        item.mNativeItemId = msgId;
        item.mNativeItemType = TimelineNativeTypes.MmsLog.ordinal();
        item.mType = type;
        item.mNativeThreadId = mmsCursor.getInt(COLUMN_MSG_THREAD_ID);
        item.mContactAddress = address;
        if (subject.length() > 0) {
            if (subject.length() <= maxDescLength) {
                item.mDescription = subject;
            } else {
                item.mDescription = subject.substring(0, maxDescLength) + ELLIPSIZE;
            }
        } else {
            item.mDescription = getMmsText(cr, msgId, maxDescLength);
        }
        item.mTitle = DateFormat.getDateInstance().format(new Date(item.mTimestamp));
        Contact c = new Contact();
        ContactDetail phoneDetail = new ContactDetail();
        ServiceStatus status = db.fetchContactInfo(address, c, phoneDetail);
        if (ServiceStatus.SUCCESS == status) {
            item.mContactId = c.contactID;
            item.mLocalContactId = c.localContactID;
            item.mUserId = c.userID;
            item.mContactName = null;
            for (ContactDetail d : c.details) {
                switch (d.key) {
                    case VCARD_NAME:
                        final VCardHelper.Name name = d.getName();
                        if (name != null) {
                            item.mContactName = d.getName().toString();
                        }
                        break;
                    case VCARD_IMADDRESS:
                        item.mContactNetwork = d.alt;
                        break;
                    default:
                        // do nothing.
                        break;
                }
            }
        } else {
            item.mContactName = address;
        }
        return true;
    }

    /**
     * Return the MIME-type for specified character-set.
     * 
     * @param charset Character set.
     * @return String containing MIME-type.
     * @throws UnsupportedEncodingException if un-supported character set
     *             specified.
     */
    private static String getMimeName(int charset) throws UnsupportedEncodingException {
        switch (charset) {
            case ANY_CHARSET:
                return MIMENAME_ANY_CHARSET;
            case US_ASCII:
                return MIMENAME_US_ASCII;
            case ISO_8859_1:
                return MIMENAME_ISO_8859_1;
            case ISO_8859_2:
                return MIMENAME_ISO_8859_2;
            case ISO_8859_3:
                return MIMENAME_ISO_8859_3;
            case ISO_8859_4:
                return MIMENAME_ISO_8859_4;
            case ISO_8859_5:
                return MIMENAME_ISO_8859_5;
            case ISO_8859_6:
                return MIMENAME_ISO_8859_6;
            case ISO_8859_7:
                return MIMENAME_ISO_8859_7;
            case ISO_8859_8:
                return MIMENAME_ISO_8859_8;
            case ISO_8859_9:
                return MIMENAME_ISO_8859_9;
            case SHIFT_JIS:
                return MIMENAME_SHIFT_JIS;
            case UTF_8:
                return MIMENAME_UTF_8;
            case BIG5:
                return MIMENAME_BIG5;
            case UCS2:
                return MIMENAME_UCS2;
            case UTF_16:
                return MIMENAME_UTF_16;
            default:
                throw new UnsupportedEncodingException();
        }
    }

    /**
     * Decode MMS data based on character set.
     * 
     * @param charset Character set.
     * @param data MMS data.
     * @return Decoded MMS data as String.
     */
    private static String decodeString(int charset, byte[] data) {
        if (ANY_CHARSET == charset) {
            return new String(data); // system default encoding.
        } else {
            try {
                String name = getMimeName(charset);
                return new String(data, name);
            } catch (UnsupportedEncodingException e) {
                try {
                    return new String(data, MIMENAME_ISO_8859_1);
                } catch (UnsupportedEncodingException ex) {
                    return new String(data); // system default encoding.
                }
            }
        }
    }

    /**
     * Return byte array from supplied String.
     * 
     * @param data String containing MMS data.
     * @return byte array containing data.
     */
    private static byte[] getMmsBytes(String data) {
        try {
            return data.getBytes(MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            // Impossible to reach here!
            LogUtils.logE("MmsDecoder.getBytes() ISO_8859_1 must be supported - " + e);
            return new byte[0];
        }
    }

    /**
     * Retrieve the sender/recipient of MMS (based on whether received or sent
     * message).
     * 
     * @param cr ContentResolver.
     * @param msgType ActivityItem type (i.e. MESSAGE_MMS_SENT).
     * @param uri MMS URI based on MMS_CONTENT_URI.
     * @return String containing sender/recipient (can be null).
     */
    private static String getToOrFrom(ContentResolver cr, ActivityItem.Type msgType, Uri uri) {
        String msgId = uri.getLastPathSegment();
        Uri.Builder builder = MMS_CONTENT_URI.buildUpon();

        builder.appendPath(msgId).appendPath("addr");

        int type = PDU_FROM_FIELD;
        if (msgType == ActivityItem.Type.MESSAGE_MMS_SENT) {
            type = PDU_TO_FIELD;
        }
        Uri uriPart = builder.build();
        Cursor cursor = cr.query(uriPart, new String[] {
                "address", "charset"
        }, "type=" + type, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String from = cursor.getString(0);

                    if (!TextUtils.isEmpty(from)) {
                        byte[] bytes = getMmsBytes(from);
                        int charset = cursor.getInt(1);
                        return decodeString(charset, bytes);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Convert Native message type (Inbox, Sent) to corresponding ActivityItem
     * type.
     * 
     * @param type Native message type.
     * @return ActivityItem type.
     */
    private static ActivityItem.Type nativeToNpMessageType(int type) {
        switch (type) {
            case MESSAGE_BOX_SENT:
                return ActivityItem.Type.MESSAGE_MMS_SENT;
            case MESSAGE_BOX_INBOX:
                return ActivityItem.Type.MESSAGE_MMS_RECEIVED;
            default:
                // do nothing.
                break;
        }
        return null;
    }

    /**
     * Generate text required for Timeline entry from content of MMS message.
     * 
     * @param cr ContentResolver
     * @param msgId ID of MMS message as retrieved from message log.
     * @param maxLength maximum length for text.
     * @return String containing retrieved text, can be null.
     */
    private static String getMmsText(ContentResolver cr, long msgId, int maxLength) {
        String strText = null;
        Uri.Builder builder = MMS_CONTENT_URI.buildUpon();
        builder.appendPath(Long.toString(msgId)).appendPath(PART);
        Cursor partsCursor = cr.query(builder.build(), PART_PROJECTION, null, null, null);
        while (strText == null && partsCursor.moveToNext()) {
            long partId = partsCursor.getLong(PART_COLUMN_ID);
            if (!partsCursor.isNull(PART_COLUMN_CONTENT_TYPE)) {
                String ct;
                try {
                    ct = new String(partsCursor.getString(PART_COLUMN_CONTENT_TYPE).getBytes(),
                            MIMENAME_ISO_8859_1);
                    if (ct.equals(TEXT_PLAIN)) {
                        Uri.Builder builder2 = MMS_CONTENT_URI.buildUpon();
                        builder2.appendPath(PART).appendPath(Long.toString(partId));
                        Uri partURI = builder2.build();
                        InputStream is = cr.openInputStream(partURI);
                        byte[] buffer = new byte[maxLength];
                        int len = is.read(buffer);
                        strText = new String(buffer, 0, len, MIMENAME_ISO_8859_1);
                        if (len == maxLength) {
                            strText += ELLIPSIZE;
                        }
                        is.close();
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return strText;
    }
}
