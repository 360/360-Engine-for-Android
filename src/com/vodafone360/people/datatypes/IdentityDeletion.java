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

package com.vodafone360.people.datatypes;

import java.util.Enumeration;
import java.util.Hashtable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * BaseDataType representing Identity deletion information retrieved from server.
 */
public class IdentityDeletion extends BaseDataType implements Parcelable {

	/**
	 * Enumeration of Tags for StatusMsg item.
	 */
	private enum Tags {
		STATUS("status"),
		DRYRUN("dryrun");

		private final String tag;

		/**
		 * Constructor creating Tags item for specified String.
		 *
		 * @param s
		 *            String value for Tags item.
		 */
		private Tags(final String s) {
			tag = s;
		}

		/**
		 * String value associated with Tags item.
		 *
		 * @return String value for Tags item.
		 */
		private String tag() {
			return tag;
		}

		/**
		 * Find Tags item for specified String.
		 *
		 * @param tag
		 *            String value to find Tags item for
		 * @return Tags item for specified String, null otherwise
		 */
		private static Tags findTag(final String tag) {
			for (Tags tags : Tags.values()) {
				if (tag.compareTo(tags.tag()) == 0) {
					return tags;
				}
			}
			return null;
		}
	}

	private Boolean mStatus = null;

	private Boolean mDryRun = null;

	/** {@inheritDoc} */
	@Override
	public int getType() {
		return IDENTITY_DELETION_DATA_TYPE;
	}

	/**
	 * Populate Identity from supplied Hashtable.
	 *
	 * @param hash
	 * Hashtable containing identity details.
	 * @return Identity instance
	 */
	public final IdentityDeletion createFromHashtable(final Hashtable<String, Object> hash) {
		Enumeration<String> e = hash.keys();
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			Tags tag = Tags.findTag(key);
			if (tag != null) {
				setValue(tag, hash.get(key));
			}
		}
		return this;
	}

	/**
	 * Sets the value of the member data item associated with the specified tag.
	 *
	 * @param tag
	 *            Current tag
	 * @param val
	 *            Value associated with the tag
	 */
	private void setValue(final Tags tag, final Object val) {
		switch (tag) {
		case STATUS:
			mStatus = (Boolean) val;
			break;

		case DRYRUN:
			mDryRun = (Boolean) val;
			break;

		default:
			// Do nothing.
			break;
		}
	}

	/** {@inheritDoc} */
	@Override
	public final int describeContents() {
		return 1;
	}

	/** {@inheritDoc} */
	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		Object[] ba = new Object[2];
		ba[0] = mStatus;
		ba[1] = mDryRun;
		dest.writeArray(ba);

	}
}
