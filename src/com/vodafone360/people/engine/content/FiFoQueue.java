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

package com.vodafone360.people.engine.content;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Simple first in first out queue. Basically same as a LinkedList only without
 * allowing null objects.
 */
public class FiFoQueue implements Queue<ContentObject> {

    /**
     * LinkedList to perform all the operations. Most methods in this class just
     * call methods on this list.
     */
    private LinkedList<ContentObject> list = new LinkedList<ContentObject>();

    /**
     * @see LinkedList.element()
     */
    @Override
    public final synchronized ContentObject element() {
        return list.element();
    }

    /**
     * Calls LinkedList.offer(Object) if content is not null.
     * 
     * @param content ContentObject to put into the list
     */
    @Override
    public final synchronized boolean offer(final ContentObject content) {
        if (content == null) {
            throw new RuntimeException("null objects not supported");
        }
        return list.offer(content);
    }

    /**
     * @see LinkedList.peek()
     */
    @Override
    public final synchronized ContentObject peek() {
        return list.peek();
    }

    /**
     * @see LinkedList.poll()
     */
    @Override
    public final synchronized ContentObject poll() {
        return list.poll();
    }

    /**
     * @see LinkedList.remove()
     */
    @Override
    public final synchronized ContentObject remove() {
        return list.remove();
    }

    /**
     * Calls LinkedList.add(Object) if content is not null.
     * 
     * @param content The ContentObject to put into the list
     * @return true if content was not null and the insertion was successful,
     *         false otherwise
     */
    @Override
    public final synchronized boolean add(final ContentObject content) {
        if (content == null) {
            return false;
        }
        return list.add(content);
    }

    /**
     * Iterates over a collection and tries putting all objects into the list.
     * If one of the object is null the method will exit with false.
     * 
     * @param contentCollection to be added into the list
     * @return true if every object of the provided collection could be put into
     *         the list, false otherwise
     */
    @SuppressWarnings("unchecked")
    @Override
    public final synchronized boolean addAll(
            final Collection<? extends ContentObject> contentCollection) {
        Iterator<ContentObject> iter = (Iterator<ContentObject>)contentCollection.iterator();
        while (iter.hasNext()) {
            if (!add(iter.next())) {
                return false;
            }
        }
        return true;

    }

    /**
     * @see LinkedList.clear()
     */
    @Override
    public final synchronized void clear() {
        list.clear();

    }

    /**
     * @see LinkedList.contains(Object)
     */
    @Override
    public final synchronized boolean contains(Object content) {
        return list.contains(content);
    }

    /**
     * @see LinkedList.containsAll(Collection<?>)
     */
    @Override
    public final synchronized boolean containsAll(Collection<?> contentCollection) {
        return list.containsAll(contentCollection);
    }

    /**
     * @see LinkedList.isEmpty();
     */
    @Override
    public final synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * @see LinkedList.iterator()
     */
    @Override
    public final synchronized Iterator<ContentObject> iterator() {
        return list.listIterator();
    }

    /**
     * @see LinkedList.remove(Object)
     */
    @Override
    public final synchronized boolean remove(final Object content) {
        return list.remove(content);
    }

    /**
     * @see LinkedList.removeAll(Collection<?>)
     */
    @Override
    public final synchronized boolean removeAll(final Collection<?> contentCollection) {
        return list.removeAll(contentCollection);
    }

    /**
     * @see LinkedList.retainAll(Collection<?>)
     */
    @Override
    public final synchronized boolean retainAll(final Collection<?> contentCollection) {
        return list.retainAll(contentCollection);
    }

    /**
     * @see LinkedList.size();
     */
    @Override
    public final synchronized int size() {
        return list.size();
    }

    /**
     * @see LinekdList.toArray();
     */
    @Override
    public final synchronized Object[] toArray() {
        return list.toArray();
    }

    /**
     * @see LinkedList.toArray(T[] type);
     */
    @Override
    public final synchronized <T> T[] toArray(final T[] type) {
        return list.toArray(type);
    }

}
