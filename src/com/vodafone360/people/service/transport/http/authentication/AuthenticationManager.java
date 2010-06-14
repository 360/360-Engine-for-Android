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

package com.vodafone360.people.service.transport.http.authentication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.hessian.HessianUtils;
import com.vodafone360.people.utils.LogUtils;

public class AuthenticationManager extends Thread implements IConnection {
    private URI mApiUrl;

    private HttpConnectionThread mHttpConnection;

    private DecoderThread mDecoder;

    private boolean mIsConnectionRunning;

    public AuthenticationManager(HttpConnectionThread httpConnection) {
        mHttpConnection = httpConnection;

        try {
            mApiUrl = (new URL(SettingsManager.getProperty(Settings.SERVER_URL_HESSIAN_KEY)))
                    .toURI();
        } catch (MalformedURLException e) {
            LogUtils.logE("Error defining URL");
        } catch (URISyntaxException e) {
            LogUtils.logE("Error defining URI");
        }
    }

    public AuthenticationManager(DecoderThread decoder) {
        mDecoder = decoder;

        mHttpConnection = new HttpConnectionThread(mDecoder);
        mHttpConnection.setHttpClient();

        try {
            mApiUrl = (new URL(SettingsManager.getProperty(Settings.SERVER_URL_HESSIAN_KEY)))
                    .toURI();
        } catch (MalformedURLException e) {
            LogUtils.logE("Error defining URL");
        } catch (URISyntaxException e) {
            LogUtils.logE("Error defining URI");
        }
    }

    public void run() {
        while (mIsConnectionRunning) {
            handleAuthRequests();
            synchronized (this) {
                try {
                    wait();

                } catch (InterruptedException e) {
                    // Do nothing.
                }
            }
        }
    }

    /**
     * Uses the passed http connection to start a synchronous request against
     * the API. This method blocks until the request is made and the response is
     * retrieved.
     */
    public void handleAuthRequests() {
        List<Request> requests = QueueManager.getInstance().getApiRequests();
        if (null == requests) {
            return;
        }

        HttpConnectionThread.logI("AuthenticationManager.handleAuthRequest()",
                "Looking for auth requests");

        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get(i);
            request.setActive(true);

            List<Integer> reqIds = new ArrayList<Integer>();
            reqIds.add(request.getRequestId());

            try {
                HttpConnectionThread.logI("AuthenticationManager.handleAuthRequest()", "Request: "
                        + request.getRequestId());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                request.writeToOutputStream(baos, false);
                byte[] payload = baos.toByteArray();

                if (Settings.ENABLED_TRANSPORT_TRACE) {
                    HttpConnectionThread.logI("AuthenticationManager.handleAuthRequests()",
                            "\n \n \nAUTHENTICATING: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                                    + HessianUtils.getInHessian(new ByteArrayInputStream(payload),
                                            true));
                }

                HttpResponse resp = mHttpConnection.postHTTPRequest(payload, mApiUrl,
                        Settings.HTTP_HEADER_CONTENT_TYPE);
                mHttpConnection.handleApiResponse(resp, reqIds);
            } catch (Exception e) {
                mHttpConnection.addErrorToResponseQueue(reqIds);
            }
        }
    }

    @Override
    public boolean getIsConnected() {
        return true;
    }

    @Override
    public boolean getIsRpgConnectionActive() {
        return true;
    }

    @Override
    public void notifyOfRegainedNetworkCoverage() {
    }

    @Override
    public void notifyOfUiActivity() {
    }

    @Override
    public void onLoginStateChanged(boolean isLoggedIn) {
    }

    @Override
    public void startThread() {
        mIsConnectionRunning = true;
        start();
    }

    @Override
    public void stopThread() {
        mIsConnectionRunning = false;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void notifyOfItemInRequestQueue() {
        synchronized (this) {
            notify();
        }
    }
}
