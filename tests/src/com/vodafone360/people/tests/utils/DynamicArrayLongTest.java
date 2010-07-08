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

package com.vodafone360.people.tests.utils;

import junit.framework.TestCase;

import com.vodafone360.people.utils.DynamicArrayLong;

/**
 * JUnit tests for the DynamicArrayLong class.
 * 
 * @see DynamicArrayLong
 */
public class DynamicArrayLongTest extends TestCase {

    /**
     * Tests the default constructor.
     */
    public void testDefaultContructor() {
        
        DynamicArrayLong array = new DynamicArrayLong();
        
        // check that the array is empty after creation
        assertEquals(0, array.size());
    }
    
    /**
     * Tests the constructor specifying the initial capacity.
     */
    public void testConstructorWithInitialCapacity() {
        
        DynamicArrayLong array = new DynamicArrayLong(10);
        boolean exceptionThrown;
        
        // check that the array is empty after creation
        assertEquals(0, array.size());
        
        // check that exceptions are thrown with capacity <= 0
        try {
            
            exceptionThrown = false;
            array = new DynamicArrayLong(0);
        } catch(IllegalArgumentException iae) {
            
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
        
        try {
            
            exceptionThrown = false;
            array = new DynamicArrayLong(-10);
        } catch(IllegalArgumentException iae) {
            
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
    }
    
    /**
     * Tests the add(), get() and size() methods.
     */
    public void testAddGetSize() {
        
        final int ADDS_COUNT = 20;
        final DynamicArrayLong array = new DynamicArrayLong();
        
        // check that the array is empty after creation
        assertEquals(0, array.size());
        
        // perform checks for add/get/size methods
        for (int i = 0; i < ADDS_COUNT; i++) {
            
            final int ret = array.add(i+10);
            assertEquals(i, ret);
            assertEquals(i+10, array.get(i));
            assertEquals(i+1, array.size());
        }
    }
    
    /**
     * Tests the add(), get() and size() methods when going over the initial capacity
     */
    public void testAddGetSizeOverInitialCapacity() {
        
        final int ADDS_COUNT = 100;
        final DynamicArrayLong array = new DynamicArrayLong(ADDS_COUNT / 2);
        
        // check that the array is empty after creation
        assertEquals(0, array.size());
        
        // perform checks for add/get/size methods
        for (int i = 0; i < ADDS_COUNT; i++) {
            
            final int ret = array.add(i+10);
            assertEquals(i, ret);
            assertEquals(i+10, array.get(i));
            assertEquals(i+1, array.size());
        }
        
        // re-check the values from the beginning
        for (int i = 0; i < ADDS_COUNT; i++) {

            assertEquals(i+10, array.get(i));
        }
        assertEquals(ADDS_COUNT, array.size());
    }
    
    /**
     * Tests the get() method.
     */
    public void testGet() {
        
        final int ADDS_COUNT = 100;
        final DynamicArrayLong array = new DynamicArrayLong(); 
        
        testGetExceptions(array);
        
        for (int i = 0; i < ADDS_COUNT; i++) {
            
            final int ret = array.add(i+10);
            assertEquals(i, ret);
            assertEquals(i+10, array.get(i));
            assertEquals(i+1, array.size());
            testGetExceptions(array);
        }
        
        testGetExceptions(array);
    }
    
    /**
     * Tests the add(long[]) method.
     */
    public void testAddArray() {
        
        final DynamicArrayLong array = new DynamicArrayLong(10);
        final long[] subArray = new long[5];
        
        assertEquals(0, array.size());
       
        // adds arrays containing increasing number to the DynamicArrayLong 
        for (int i = 0; i <= 4; i++) {
            
            for (int j = 0; j < subArray.length; j++) {
                subArray[j] = i * subArray.length + (j + 1);
            }
            final int index = array.add(subArray);
            assertEquals((i + 1) * subArray.length - 1, index);
            assertEquals((i + 1) * subArray.length, array.size());
        }
        
        // re-check the values from the beginning
        // it shall contain the values [1, 2, 3...19, 20] in the same order
        for (int i = 0; i < array.size(); i++) {

            assertEquals(i+1, array.get(i));
        }
    }
    
    /**
     * Tests the toArray() method.
     */
    public void testToArray() {
        
        final int ADDS_COUNT = 20;
        final DynamicArrayLong array = new DynamicArrayLong(ADDS_COUNT / 4);
        
        // check that the array is empty after creation
        assertEquals(0, array.size());
        
        // add the values [0..ADDS_COUNT] to the array
        for (int i = 0; i < ADDS_COUNT; i++) {
            
            array.add(i);
        }
        
        // get all the values in the DynamicArrayLong as a primitive long array
        final long[] toArray = array.toArray();
        assertEquals(ADDS_COUNT, toArray.length);
        
        // check that the values in the array are [0..ADDS_COUNT] 
        for (int i = 0; i < toArray.length; i++) {

            assertEquals(i, toArray[i]);
        }
    }
    
    /**
     * Helper method to test the get() exception cases to the provided DynamicArrayLong
     * 
     * @param array the DynamicArrayLong to check
     */
    private void testGetExceptions(DynamicArrayLong array) {

        boolean exceptionThrown;
        
        // check negative case
        try {
            
            exceptionThrown = false;
            array.get(-10);
        } catch(ArrayIndexOutOfBoundsException e) {
            
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        
        // check positive case
        try {
            
            exceptionThrown = false;
            array.get(array.size() + 10);
        } catch(ArrayIndexOutOfBoundsException e) {
            
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }
}
