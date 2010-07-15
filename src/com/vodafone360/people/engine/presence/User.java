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

package com.vodafone360.people.engine.presence;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;

/**
 * User is a class encapsulating the information about a user's presence state.
 */
public class User {

    private static final String COLUMNS = "::";

    private long mLocalContactId; // the database id of the contact, which
                                  // corresponds to, e.g. "userid@gmail.com"

    private int mOverallOnline; // the overall presence state displayed in the
                                // common contact list

    private ArrayList<NetworkPresence> mPayload; // communities presence status
                                                 // {google:online, pc:online,
                                                 // mobile:online}

    /**
     * Default Constructor.
     */
    public User() {
    }

    /**
     * Constructor.
     * 
     * @param userId - user id in the contact list, e.g.
     *            "google::userid@gmail.com" or "882339"
     * @param payload - communities presence status {google:online, pc:online,
     *            mobile:online}
     */
    public User(String userId, Hashtable<String, String> payload) {
        mOverallOnline = isOverallOnline(payload);
        mPayload = createPayload(userId, payload);
    }

    /**
     * This method returns the localContactId for this contact in DB across the
     * application .
     * 
     * @return the localContactId for this contact in DB
     */
    public long getLocalContactId() {
        return mLocalContactId;
    }

    public void setLocalContactId(long mLocalContactId) {
        this.mLocalContactId = mLocalContactId;
    }

    /**
     * Returns communities presence status
     * 
     * @return communities presence status, e.g. {google:online, pc:online,
     *         mobile:online}
     */
    public ArrayList<NetworkPresence> getPayload() {
        return mPayload;
    }

    public OnlineStatus getStatusForNetwork(SocialNetwork network) {
        if (network == null) {
            return null;
        }
        OnlineStatus os = OnlineStatus.OFFLINE;
        if (mPayload != null) {
            if (network == SocialNetwork.VODAFONE) {
                int aggregated = 0; // aggregated state for "mobile" and "pc"
                for (NetworkPresence np : mPayload) {
                    if (np.getNetworkId() == SocialNetwork.MOBILE.ordinal()
                            || (np.getNetworkId() == SocialNetwork.PC.ordinal())) {
                        if (aggregated < np.getOnlineStatusId()) {
                            aggregated += np.getOnlineStatusId();
                        }
                    }
                }
                os = OnlineStatus.getValue(aggregated);
            } else {
                for (NetworkPresence np : mPayload) {
                    if (np.getNetworkId() == network.ordinal()) {
                        os = OnlineStatus.getValue(np.getOnlineStatusId());
                        break;
                    }
                }
            }
        }
        return os;
    }

    /**
     * Returns communities presence status
     * 
     * @return communities presence status, e.g. {google:online, pc:online,
     *         mobile:online}
     */
    public void setPayload(ArrayList<NetworkPresence> payload) {
        mPayload = payload;
    }

    /**
     * Returns the overall user presence status
     * 
     * @return true if user is online at least at one community, e.g. true if
     *         {google:offline, pc:offline, mobile:online}
     */
    private int isOverallOnline(Hashtable<String, String> payload) {
        if (payload != null) {
            if (payload.values().contains(ContactSummary.OnlineStatus.ONLINE.toString()))
                return ContactSummary.OnlineStatus.ONLINE.ordinal();
            if (payload.values().contains(ContactSummary.OnlineStatus.INVISIBLE.toString()))
                return ContactSummary.OnlineStatus.INVISIBLE.ordinal();
            if (payload.values().contains(ContactSummary.OnlineStatus.IDLE.toString()))
                return ContactSummary.OnlineStatus.IDLE.ordinal();
        }

        return ContactSummary.OnlineStatus.OFFLINE.ordinal();
    }

    /**
     * Returns the overall user presence status: in fact the one from the below
     * status states first encountered for all known user accounts next:
     * INVISIBLE, ONLINE, IDLE, OFFLINE
     * 
     * @return presence state
     */
    public int isOnline() {
        return mOverallOnline;
    }

    /**
     * @param payload
     * @return
     */
    private ArrayList<NetworkPresence> createPayload(String userId,
            Hashtable<String, String> payload) {
        ArrayList<NetworkPresence> presenceList = new ArrayList<NetworkPresence>(payload.size());
        String parsedUserId = parseUserName(userId);
        String key = null;
        SocialNetwork network = null;
        String value = null;
        OnlineStatus status = null;
        for (Enumeration<String> en = payload.keys(); en.hasMoreElements();) {
            key = en.nextElement();
            network = SocialNetwork.getValue(key);
            if (network != null) {
                int keyIdx = network.ordinal();
                value = payload.get(key);
                if (value != null) {
                    status = OnlineStatus.getValue(value);
                    if (status != null) {
                        int valueIdx = status.ordinal();
                        presenceList.add(new NetworkPresence(parsedUserId, keyIdx, valueIdx));
                    }
                }
            }
        }
        return presenceList;
    }

    /**
     * @param user
     * @return
     */
    private static String parseUserName(String userId) {
        if (userId != null) {
            int columnsIndex = userId.indexOf(COLUMNS);
            if (columnsIndex > -1) {
                return userId.substring(columnsIndex + COLUMNS.length());
            } else {
                return userId;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mOverallOnline;
        result = prime * result + ((mPayload == null) ? 0 : mPayload.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User)obj;
        if (mOverallOnline != other.mOverallOnline)
            return false;
        if (mPayload == null) {
            if (other.mPayload != null)
                return false;
        } else if (!mPayload.equals(other.mPayload))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("User [mLocalContactId=");
        sb.append(mLocalContactId);
        sb.append(", mOverallOnline="); sb.append(mOverallOnline);
        sb.append(", mPayload="); sb.append(mPayload); sb.append("]");
        return sb.toString();
    }

    /**
     * This method sets the overall user presence status,
     * @parameter the online status id - the ordinal, see @OnlineStatus  
     */
    public void setOverallOnline(int overallOnline) {
        this.mOverallOnline = overallOnline;
    }

    /**
     * This method removes the network presence information with the given presence id from the User.
     * @param ordinal - the network id, ordinal in @see SocialNetworks
     */
    public void removeNetwork(int ordinal) {
        Iterator<NetworkPresence> itr = mPayload.iterator();
        NetworkPresence presence = null;
        while (itr.hasNext()) {
            presence = itr.next();
            if (presence.getNetworkId() == ordinal) {
                itr.remove();
                break;
            }
        }
    }

}
