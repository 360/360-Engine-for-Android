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

package com.vodafone360.people;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import com.vodafone360.people.utils.LRUHashMap;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.ThumbnailUtils;


/***
 * Unified thumbnail cache with asynchronous file reading, intended to be
 * utilised by multiple activities.
 */
public class ThumbnailCache {

    /** Thread name.  **/
    private static final String THREAD_NAME = "ThumbnailCacheThread";
    /** Request queue size (number of possible on screen items). **/
    private static final int REQUEST_QUEUE_SIZE = 9;
    /**
     * Thumbnails cache size (REQUEST_QUEUE_SIZE or greater, but larger values
     * are OK due to garbage collection).
     */
    private static final int THUMBNAILS_CACHE_SIZE = 20;
    /**
     * Keep the Thumbnail Cache thread in the background by making it wait a
     * tiny bit between loading files.
     */
    private static final long THREAD_WAIT = 5L;

    /** Thumbnail cache. **/
    private final LRUHashMap<Long, SoftReference<Bitmap>> mThumbnailCache;
    /** Request queue (must be synchronised). **/
    private final List<Item> mRequestQueue;
    /** List of invalid Thumbnails. **/
    private final List<Long> mInvalidatedThumbnails;
    /**
     * Instance of the background thread (must always be accessed by the main
     * thread only).
     */
    private BackgroundThread mBackgroundThread;
    /** Background Thread sync object. **/
    private Object mBackgroundThreadSync = new Object();
    /** Reference to activity which is currently using this item. **/
    private Activity mActivity;
    /** True if the background thread should not be doing any work.  **/
    private boolean mPaused = false;

    /** Item in the request queue. **/
    private class Item {
        /** Contact ID. **/
        private final long mContactId;
        /** ImageView to populate. **/
        private final ImageView mImageView;

        /***
         * Item constructor.
         *
         * @param contactId Contact ID.
         * @param imageView ImageView to populate.
         */
        public Item(final long contactId, final ImageView imageView) {
            mContactId = contactId;
            mImageView = imageView;
        }

        /***
         * Contact ID.
         *
         * @return Contact ID.
         */
        public long getContactId() {
            return mContactId;
        }

        /***
         * ImageView to populate.
         *
         * @return ImageView to populate.
         */
        public ImageView getImageView() {
            return mImageView;
        }
    }

    /***
     * Create the Cache.
     */
    public ThumbnailCache() {
        mThumbnailCache = new LRUHashMap<Long, SoftReference<Bitmap>>(THUMBNAILS_CACHE_SIZE);
        mRequestQueue = new ArrayList<Item>(REQUEST_QUEUE_SIZE);
        mInvalidatedThumbnails = new ArrayList<Long>();
    }

    /***
     * Subscribe this Activity to the cache.
     *
     * @param activity Current Activity for posting to a UI thread.
     */
    public final void subscribe(final Activity activity) {
        synchronized (mBackgroundThreadSync) {
            if (mBackgroundThread == null) {
                mBackgroundThread = new BackgroundThread();
            }
        }
        mActivity = activity;
        pauseThread(false);
    }

    /***
     * Removed the background thread and clear all pending work.
     */
    public final void unsubscribe() {
        synchronized (mBackgroundThreadSync) {
            if (mBackgroundThread != null) {
                mBackgroundThread.killThread();
            }
            mBackgroundThread = null;
        }
        mPaused = true;
        mActivity = null;
        synchronized (mRequestQueue) {
            mRequestQueue.clear();
        }

    }

    /***
     * Pause the Background Thread, while keeping the work queue.
     *
     * @param pause TRUE if the thread should be paused, FALSE will immediately
     *      resume the thread.
     */
    public final void pauseThread(final boolean pause) {
        mPaused = pause;
        synchronized (mBackgroundThreadSync) {
            if (!pause && mBackgroundThread != null) {
                mBackgroundThread.doWork();
            }
        }
    }

    /***
     * Sets the given ImageView with the cached thumbnail for this contact ID,
     * using the default thumbnail if it is not cached.  If the thumbnail is
     * not cached and "queue" is set to TRUE, then a background thread will try
     * and populate the ImageView later.
     *
     * @param imageView ImageView to set now (from cache) or later (in
     *          background thread).
     * @param localContactId ID of the contact thumbnail.
     * @param defaultThumbnailId ID of default thumbnail resource.
     */
    public final void setThumbnail(final ImageView imageView,
            final long localContactId, final int defaultThumbnailId) {
        if (imageView == null) {
            throw new InvalidParameterException("ThumbnailCache.setThumbnail() "
                    + "ImageView should not be NULL");
        }

        boolean imageSet = false;

        /** Associate the ImageView with the contact ID. **/
        imageView.setTag(localContactId);

        /** Check the thumbnail cache. **/
        final SoftReference<Bitmap> bitmapRef
            = mThumbnailCache.get(localContactId);
        if (bitmapRef != null) {
            final Bitmap thumbnail = bitmapRef.get();
            if (thumbnail != null) {
                /** Instantly return a cached reference. **/
                imageView.setImageBitmap(thumbnail);
                imageSet = true;

                if (!mInvalidatedThumbnails.contains(localContactId)) {
                    /** This image is valid, so don't try and reload. **/
                    return;
                }

            } else {
                /** Remove any faulty references from the cache. **/
                LogUtils.logW("ThumbnailCache.setThumbnail() "
                        + "Bad reference removed id[" + localContactId + "]");
                mThumbnailCache.remove(localContactId);
            }
        }

        /** Add this thumbnail to the request queue. **/
        synchronized (mRequestQueue) {
            if (!mRequestQueue.contains(localContactId)) {
                /** Remove oldest item in the list. **/
                if (mRequestQueue.size() > REQUEST_QUEUE_SIZE) {
                    mRequestQueue.remove(0);
                }
                mRequestQueue.add(new Item(localContactId, imageView));
                synchronized (mBackgroundThreadSync) {
                    if (mBackgroundThread != null) {
                        mBackgroundThread.doWork();
                    }
                }
            }
        }

        if (!imageSet) {
            /** Thumbnail was not found, so use default. **/
            imageView.setImageResource(defaultThumbnailId);
        }
    }

    /***
     * Clear the entire thumbnail cache to save memory.
     */
    private final void clearThumbnailCache() {
        synchronized (mRequestQueue) {
            mRequestQueue.clear();
        }
        mThumbnailCache.clear();
        mInvalidatedThumbnails.clear();
        System.gc();
    }

    /***
     * Invalidate the entire thumbnail cache, that way a thumbnail that has
     * been changed in the file system will be updated the next time it is
     * requested by the UI.
     */

    public final void invalidateThumbnailCache() {
        mInvalidatedThumbnails.clear();
        Set<Long> x = mThumbnailCache.keySet();
        Long[] thumbnailCacheArray = (Long[]) x.toArray(new Long[x.size()]);
        for (Long contactId : thumbnailCacheArray) {
            mInvalidatedThumbnails.add(contactId);
        }
    }

    /***
     * Perform work in a background thread.
     */
    private class BackgroundThread extends Thread {
        /**
         * Object is used to indicate that this thread has started, and for
         * pausing and notifying the Thread.  Thread will wait on this object.
         */
        private Object mRunning;
        /** Set to TRUE to indicate that this thread should roll to a stop. **/
        private boolean mKillThread = false;

        /***
         * Start or bring the thread out of wait state to do some background
         * work.
         */
        public void doWork() {
            if (mKillThread) {
                /** Exit now. **/
                return;

            } else if (mRunning == null) {
                /** First time start. **/
                mRunning = new Object();
                start();

            } else if (mRequestQueue.size() != 0 && !mPaused) {
                /** Notify the running thread. **/
                synchronized (mRunning) {
                    mRunning.notify();
                }
            }
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Thread.currentThread().setName(THREAD_NAME);

            while (!mKillThread) {
                loadThumbnails();
                threadWait(null);
            }
        }

        /***
         * Load thumbnails from the file IO, posting a cache updated
         * notification to the handled as long as one file has been loaded.
         */
        private final void loadThumbnails() {
            while (!mKillThread) {
                if (mPaused || mActivity == null) {
                    return;
                }

                final Item item;
                synchronized (mRequestQueue) {
                    if (mRequestQueue.size() < 1) {
                        /** Loading done. **/
                        return;
                    }
                    item = mRequestQueue.remove(0);
                }

                if (item.getContactId()
                        != getLocalContactId(item.getImageView())) {
                    /** ImageView has been Recycled. **/
                    continue;
                }

                /**
                 * Do all expensive File IO and Bitmap decoding work.
                 */
                final String path = ThumbnailUtils.thumbnailPath(
                        item.getContactId());
                if (path != null) {
                    try {
                        /* Using reflection to set inPurgeable flag as it is not available on 1.5 */
                        Class bitmapFactoryOptionsClass = BitmapFactory.Options.class;
                        BitmapFactory.Options bitmapFactoryOptionsInstance = new BitmapFactory.Options();
                        Field field;
                        try {
                            field = bitmapFactoryOptionsClass.getField("inPurgeable");
                            field.setBoolean(bitmapFactoryOptionsInstance, true);
                        } catch (SecurityException e) {
                            LogUtils.logW("ThumbnailCache.loadThumbnails() "
                                    + "Security Exception");
                        } catch (NoSuchFieldException e) {
                            LogUtils.logW("ThumbnailCache.loadThumbnails() "
                                    + "Field not found");
                        } catch (IllegalArgumentException e) {
                            LogUtils.logW("ThumbnailCache.loadThumbnails() "
                                    + "Illegal Argument");
                        } catch (IllegalAccessException e) {
                            LogUtils.logW("ThumbnailCache.loadThumbnails() "
                                    + "Illegal Access");
                        }

                        final Bitmap bitmap = BitmapFactory.decodeFile(path, bitmapFactoryOptionsInstance);
                        mThumbnailCache.put(item.getContactId(),
                                new SoftReference<Bitmap>(bitmap));

                        /** Thumbnail is now valid. **/
                        mInvalidatedThumbnails.remove(item.getContactId());

                        if (item.getContactId()
                                != getLocalContactId(item.getImageView())) {
                            /** ImageView has been Recycled. **/
                            continue;
                        }

                        if (mActivity == null) {
                            return;
                        }
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                /**
                                 * Check if the view is still the same, and
                                 * hasn't been re-used by the ListView.
                                 */
                                if (item.getContactId()
                                        == getLocalContactId(item.getImageView())) {
                                    item.getImageView().setImageBitmap(bitmap);
                                }
                            }});

                    } catch (NullPointerException e) {
                        LogUtils.logE("ThumbnailCache.loadThumbnails() "
                                + "Unexpected NullPointerException while "
                                + "loading thumbnails, clearing Thumbnail "
                                + "cache for safety.", e);
                        clearThumbnailCache();

                    } catch (OutOfMemoryError outOfMemoryError) {
                        LogUtils.logE("ThumbnailCache.loadThumbnails() "
                                + "Low on memory while decoding thumbnails",
                                outOfMemoryError);
                        clearThumbnailCache();
                    }
                }

                threadWait(THREAD_WAIT);
            }
        }

        /***
         * Stop the thread between cycles to stop it overloading the device and
         * possibly blocking the UI.
         *
         * @param wait Time to wait in milliseconds, or NULL to wait for a
         *      thread notify.
         */
        private void threadWait(final Long wait) {
            synchronized (mRunning) {
                try {
                    if (mKillThread) {
                        /** Never wait while thread is being killed. **/
                        return;
                    } else if (wait == null) {
                        mRunning.wait();
                    } else {
                        mRunning.wait(wait);
                    }

                } catch (InterruptedException e) {
                    // Do nothing.
                }
            }
        }

        /***
         * Kills the running background thread.
         */
        public void killThread() {
            mKillThread = true;
            synchronized (mRunning) {
                mRunning.notify();
            }
        }
    }

    /***
     * Return the local contact ID tag for the given View.
     *
     * @param view View to extract the list position information.
     * @return List position of the view.
     */
    private static long getLocalContactId(final View view) {
        final Object localContactIdObject = view.getTag();
        if (localContactIdObject == null) {
            LogUtils.logW("ThumbnailCache.getLocalContactId() "
                    + "ID for view should not be NULL");
            return -1L;
        } else {
            return (Long) localContactIdObject;
        }
    }

    /***
     * Return TRUE if the background thread is paused.
     *
     * @return TRUE if the background thread is paused.
     */
    public boolean isPaused() {
        return mPaused;
    }
}
