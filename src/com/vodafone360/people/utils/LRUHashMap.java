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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
    * LRUHashMap is an extension of {@link HashMap}. 
    * Cloning and serialization are not implemented, i.e. 
    * methods of the parent HashMap will be invoked.
    * The purpose of the class is to provide a functionality of HashMap
    * maintaining its size not bigger than a defined limit. 
    * For this purpose "Removing Least Recently Used (RLU) Item" algorithm is
    * applied. I.e. if adding an key/value pair to the LRUHashMap would
    * cause its size to overcome the defined size limit the oldest key/value pair
    * in this LRUHashMap will be deleted before the new one is added. 
    * So LRUHashMap is a hash map of latest added (size limit) elements.
    *       
    * <p>All elements are permitted as keys or values, including null.
    * 
    * <p>Note that the iteration order for LRUHashMap is non-deterministic. If you want
    * deterministic iteration, use {@link LinkedHashMap}.
    * 
    * <p>Note: the implementation of {@code LRUHashMap} is synchronized.
    *
    * @param <K> the type of keys maintained by this map
    * @param <V> the type of mapped values
    */
public class LRUHashMap<K, V> extends HashMap<K, V> {

    /**
     *  Id required for serialization (auto-generated here)
     */
    private static final long serialVersionUID = 12345L;
    
    /**
     *  Default size limit for the SizedLinkedHashMap 
     *  (when the map is created by a constructor with no parameters)
     *  which ensures that SizedLinkedHashMap behaves like a normal HashMap,
     *  i.e. doesn't maintain any size limit.  
     */
    private static final int NO_LIMIT = -1;

    /**
     * This ArrayList saves the order the keys of the LRUHashMap were added to it.
     */
    private ArrayList<K> mCacheOrder;
    
    /**
     * The maximum size of LRUHashMap. If not defined, no constraint is applied. 
     */
    private int mSizeLimit = NO_LIMIT;
    
    /**
     * Default constructor. Using  this constructor makes no sense
     * as it initializes the LRUHashMap to behave as a HashMap,
     * so using HashMap is preferable then.
     */
    public LRUHashMap() {
        super();
        mCacheOrder = new ArrayList<K>();
    }
    
    /**
     * Constructs a new {@code LRUHashMap} instance with the specified
     * size limit and load factor.
     * @param capacity the initial size limit of this hash map.
     * If zero is passed as capacity no maximum size 
     * constraint is applied to the LRUHashMap size,
     * using HashMap is preferable then.
     * @param loadFactor  the initial load factor.
     * @throws IllegalArgumentException
     *        when the capacity is less than zero or the load factor is
     *        less or equal to zero or NaN.
     */
    public LRUHashMap(int capacity, float loadFactor) {
        super(capacity, loadFactor);
        if (capacity > 0) {
            mSizeLimit = capacity;
        }
        mCacheOrder = new ArrayList<K>();
    }

    /**
      * Constructs a new {@code LRUHashMap} instance
      * with the specified size limit.
      * @param capacity the initial size limit of this hash map. 
      * If zero is passed as capacity no maximum size 
      * constraint is applied to the LRUHashMap size,
      * using HashMap is preferable then.       
      * @throws IllegalArgumentException when the capacity 
      * is less than zero.
      */
    public LRUHashMap(int capacity) {
        super(capacity);
        if (capacity > 0) {
            mSizeLimit = capacity;
        }
        mCacheOrder = new ArrayList<K>();
    }

    /**
      * Constructs a new {@code LRUHashMap} instance containing the mappings from
      * the specified map. If the passed map is zero size no maximum size 
      * constraint is applied to the LRUHashMap size,
      * using HashMap is preferable then.        
      * @param map the mappings to add.
      */
    public LRUHashMap(Map<? extends K, ? extends V> map) {
        super(map);
        mSizeLimit = map.size();
        mCacheOrder = new ArrayList<K>();
    }
    
    /**
     * Maps the specified key to the specified value. If adding
     * a key/value pair to the LRUHashMap would cause its size to overcome 
     * the defined size limit the oldest key/value pair in this LRUHashMap
     * will be deleted before the new one is added. 
     * 
     * @param key - The key. If the key is already in the map it will be added as the latest.
     * 
     * @param value - the value.
     * @return the value of any previous mapping with the specified key, or the removed mapping
     *   to the oldest key in this hash (the one that was removed) or {@code null} if there was no such mapping.
     */
   synchronized public V put(K key, V value) {
       if (mSizeLimit != NO_LIMIT) {
           if (mCacheOrder.size() < mSizeLimit) {
               if (mCacheOrder.contains(key)) {
                   mCacheOrder.remove(key);
               }
               mCacheOrder.add(key);
               // return the previous mapping (or null if no such mapping)
               return super.put(key, value);
           } else  {
               if (mCacheOrder.contains(key)) {
                   mCacheOrder.remove(key);
                   mCacheOrder.add(key);
                   // return the previous mapping
                   return super.put(key, value);
               } else {
                   Object oldestKey = mCacheOrder.remove(0);
                   //return the oldest value (the one that is removed from the hash)
                   V ret = super.remove(oldestKey);
                   mCacheOrder.add(key);
                   super.put(key, value);
                   return ret;
               }
           }
       } else {
           return super.put(key, value);
       }
   }

    /**
     * This method returns this LRUHashMap maximum size constraint value.
     * @return long maximum size constraint value.
     */
    public long getSizeLimit() {
        return mSizeLimit;
    }
    
    @Override
    synchronized public V remove(Object key) {
        if (mSizeLimit != NO_LIMIT) {
            if (mCacheOrder.contains(key)) {
                mCacheOrder.remove(key);
            }
        }
        return super.remove(key);
    }
    
    @Override
    synchronized public void clear() {
        mCacheOrder.clear();
        super.clear();   
    }
    
    @Override
    public String toString() {
        return "SizedLinkedHashMap:" + super.toString();    
    }

}