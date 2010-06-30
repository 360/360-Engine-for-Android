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

package com.vodafone360.people.service.transport;

public interface IConnection extends IQueueListener {
    /**
     * Starts the main connection thread.
     */
    public void startThread();

    /**
     * Stops the current connection thread. this should also stop any running
     * sub-connection threads such as heartbeats or polls.
     */
    public void stopThread();

    /**
     * Called whenever the network coverage has been reestablished...
     */
    public void notifyOfRegainedNetworkCoverage();

    /**
     * Triggered by the ConnectionManager whenever the login engine has detected
     * a change in the currently held session. If the user has signed off for
     * example, false will be passed as a parameter.
     * 
     * @param isLoggedIn True if the user was just logged in, false if he was
     *            logged out.
     */
    public void onLoginStateChanged(boolean isLoggedIn);

    /**
     * Returns true if the current connection thread is connected.
     * 
     * @return True if the connection thread is connected to the backend.
     */
    public boolean getIsConnected();

    /**
     * Returns true if we have an open RPG connection or false if we do not have
     * one.
     * 
     * @return True if the RPG connection is currently active or false
     *         otherwise.
     */
    public boolean getIsRpgConnectionActive();

    /**
     * If the UI is currently being used by the user this method gets called.
     */
    public void notifyOfUiActivity();
}
