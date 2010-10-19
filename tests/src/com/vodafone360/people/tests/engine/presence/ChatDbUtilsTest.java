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

package com.vodafone360.people.tests.engine.presence;

import android.test.ApplicationTestCase;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.ChatMessage;
import com.vodafone360.people.engine.presence.ChatDbUtils;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;

/***
 * Tests for the ChatDbUtils class.
 */
public class ChatDbUtilsTest extends ApplicationTestCase<MainApplication> {

    /** Test network ID value, which should never be returned by the code. **/
    private static final int NETWORK_ID = -2;
    /** Test local contact ID value, which can  be returned by the code. **/
    private static final Long LOCAL_CONTACT_ID = -1L;
    /** Reference to database. **/
    private DatabaseHelper mDatabaseHelper;

    /***
     * Constructor.
     */
    public ChatDbUtilsTest() {
        super(MainApplication.class);
    }

    /***
     * Set up the DatabaseHelper.
     */
    public final void setUp() {
        createApplication();
        MainApplication mainApplication = getApplication();
        if (mainApplication == null) {
            throw(new RuntimeException("ChatDbUtilsTest.setUp() "
                    + "Unable to create main application"));
        }
        mDatabaseHelper = mainApplication.getDatabase();
    }

    /***
     * Close the DatabaseHelper.
     */
    public final void tearDown() {
        mDatabaseHelper.getReadableDatabase().close();
    }

    /***
     * JUnit test for the ChatDbUtils.convertUserIds() method.
     *
     * @see com.vodafone360.people.tests.engine.presence.ChatDbUtils
     *  #convertUserIds(ChatMessage msg, DatabaseHelper databaseHelper)
     */
    public final void testConvertUserIds() {

        /** SNS test. **/
        testConvertUserId(
                "facebook.com::123456", NETWORK_ID, LOCAL_CONTACT_ID,
                "123456", SocialNetwork.FACEBOOK_COM, -1L);
        testConvertUserId(
                "hyves.nl::123456", NETWORK_ID, LOCAL_CONTACT_ID,
                "123456", SocialNetwork.HYVES_NL, -1L);
        testConvertUserId(
                "google::123456", NETWORK_ID, LOCAL_CONTACT_ID,
                "123456", SocialNetwork.GOOGLE, -1L);
        testConvertUserId(
                "microsoft::123456", NETWORK_ID, LOCAL_CONTACT_ID,
                "123456", SocialNetwork.MICROSOFT, -1L);

        /** Parsing test. **/
        testConvertUserId(
                "facebook.com::123456", NETWORK_ID, LOCAL_CONTACT_ID,
                "123456", SocialNetwork.FACEBOOK_COM, -1L);
        testConvertUserId(
                "facebook_com::", NETWORK_ID, LOCAL_CONTACT_ID,
                "", SocialNetwork.FACEBOOK_COM, -1L);

        /** Issue seen in PAND-2356. **/
        testConvertUserId(
                "facebook_com::-100001020578165@chat.facebook.com",
                NETWORK_ID, LOCAL_CONTACT_ID,
                "-100001020578165@chat.facebook.com",
                SocialNetwork.FACEBOOK_COM, -1L);

        /** Issue seen in PAND-2408. **/
        testConvertUserId(
                "google::cassandra21love@googlemail.com",
                NETWORK_ID, LOCAL_CONTACT_ID,
                "cassandra21love@googlemail.com", SocialNetwork.GOOGLE,
                -1L);

        /** Invalid tests. **/
        testConvertUserId(
                "unknown_network::123456", NETWORK_ID, LOCAL_CONTACT_ID,
                "123456", SocialNetwork.INVALID, -1L);
        testConvertUserId(
                "::123456", NETWORK_ID, LOCAL_CONTACT_ID,
                "123456", SocialNetwork.INVALID, -1L);
        testConvertUserId(
                "unknown_network::", NETWORK_ID, LOCAL_CONTACT_ID,
                "", SocialNetwork.INVALID, -1L);
        testConvertUserId(
                "::", NETWORK_ID, LOCAL_CONTACT_ID,
                "", SocialNetwork.INVALID, -1L);
    }

    /***
     * Test ChatDbUtils.convertUserIds() method with a given parameter
     * combination.
     *
     * @param userId User ID.
     * @param networkId Network ID.
     * @param localContactId Local Contact ID.
     * @param expectedUserId Expected User ID.
     * @param expectedNetwork Expected Network ID.
     * @param expectedLocalContactId Expected Local Contact ID.
     */
    private void testConvertUserId(final String userId,
            final int networkId, final Long localContactId,
            final String expectedUserId,
            final SocialNetwork expectedNetwork,
            final Long expectedLocalContactId) {

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setNetworkId(networkId);
        chatMessage.setLocalContactId(localContactId);

        ChatDbUtils.convertUserIds(chatMessage, mDatabaseHelper);

        assertEquals("ChatDbUtilsTest.checkChatMessage() Unexpected user ID",
                expectedUserId, chatMessage.getUserId());
        assertEquals("ChatDbUtilsTest.checkChatMessage() Unexpected network ID ["
                + expectedNetwork + "]",
                expectedNetwork.ordinal(), chatMessage.getNetworkId());
        assertEquals("ChatDbUtilsTest.checkChatMessage() Unexpected local contact ID",
                expectedLocalContactId, chatMessage.getLocalContactId());
    }
}
