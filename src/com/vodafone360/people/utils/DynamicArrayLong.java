/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution 
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at src/com/vodafone/people/VODAFONE.LICENSE.txt or 
 * ###TODO:URL_PLACEHOLDER###
 * See the License for the specific language governing permissions and limitations under the 
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License 
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the fields enclosed by brackets 
 * "[]" replaced with your own identifying information: Portions Copyright [yyyy] [name of 
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.utils;

/**
 * This class wraps the long primitive array into a dynamic array that
 * can grows if necessary. 
 */
public class DynamicArrayLong {

    /**
     * The initial capacity if not given via the constructor. 
     */
    private final static int INITIAL_CAPACITY = 50;
    
    /**
     * The wrapped long array.
     */
    private long[] mArray;
    
    /**
     * The current index where the next item will be added in the array.
     */
    private int mIndex = 0;
    
    /**
     * Constructor.
     */
    public DynamicArrayLong() {
        
        this(INITIAL_CAPACITY);
    }
    
    /**
     * Constructor.
     * 
     * @param capacity the initial capacity of the array
     */
    public DynamicArrayLong(int capacity) throws IllegalArgumentException {
        
        if (capacity <= 0) throw new IllegalArgumentException("The capacity has to be > 0!");
        mArray = new long[capacity];
    }
    
    /**
     * Appends a value to the array.
     * The values are added from index=0 and after each other.
     * If the array is full, its size is increased so that the value can be added.
     * 
     * @param value the value to add
     * @return the position in the array where the value was set
     */
    public int add(long value) {
        
        if (mIndex == mArray.length) {
            long[] newArray = new long[mArray.length * 2];
            System.arraycopy(mArray, 0, newArray, 0, mArray.length);
            mArray = newArray;
        }
        
        mArray[mIndex++] = value;
        
        return mIndex - 1;
    }
    
    /**
     * Adds the provided array.
     *
     * @param values the array to be added
     * @return the index of the last added value or -1 if nothing was added
     */
    public int add(long[] values) {
        
        if (values == null || values.length == 0) return -1;
        
        if (mIndex + values.length > mArray.length) {
            
            long[] newArray = new long[Math.max(mArray.length * 2, mIndex + values.length)];
            System.arraycopy(mArray, 0, newArray, 0, mArray.length);
            mArray = newArray;
        }
        
        System.arraycopy(values, 0, mArray, mIndex, values.length);
        mIndex += values.length;
        
        return mIndex - 1;
    }
    
    /**
     * Gets the value from its given position in the array.
     * 
     * @param position the position of the value in the array
     * @return the value at the provided position
     * @throws ArrayIndexOutOfBoundsException if position not >=0 and < size()
     */
    public long get(int position) throws ArrayIndexOutOfBoundsException {
        
        if ((position < 0) || (position >= mIndex)) {
            throw new ArrayIndexOutOfBoundsException("The position has to be >= 0 and < size()!");
        }
        
        return mArray[position];
    }
    
    /**
     * Gets the size of the array (i.e. the count of added values)
     * 
     * @return the array size
     */
    public int size() {
        
        return mIndex;
    }
    
    /**
     * Gets an array of long containing all the values inside the DynamicArrayLong.
     *
     * @return an array with all the values or null if empty
     */
    public long[] toArray() {
        
        if (mIndex > 0) {
            
            long[] array = new long[mIndex];
            System.arraycopy(mArray, 0, array, 0, mIndex);
            
            return array;
        }
        
        return null;
    }
}
