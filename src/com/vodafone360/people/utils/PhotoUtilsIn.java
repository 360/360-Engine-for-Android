/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 *
 * The contents of this file are subject
 *  to the terms of the Common Development and Distribution
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 * src/com/vodafone/people/VODAFONE.LICENSE.txt or
 * See the License for the specific language
 * governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL
 *  HEADER in each file and include the License
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 * with the fields enclosed by brackets
 * "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

/**
 * Class objects are recieved from UI and passed to engine from the interface.
 * General use of this class is to pass data from UI to contentengine inetrface.
 */
package com.vodafone360.people.utils;

import java.util.List;

/**
 * Inputs from UI.
 * @author sagar
 *
 */
public class PhotoUtilsIn {
    /**
     * contentid.
     */
    public Long contentid = null;
    /**
     * remoteid.
     */
    public String remoteid = null;
    /**
     * bytesmime.
     */
    public String bytesmime = null;
    /**
     * extfd.
     */
    public String extfid = null;
    /**
     * title.
     */
    public String title = null;
    /**
     * filename.
    */
    public String filename = null;
    /**
     * filepath.
     */
    public String filePath = null;
    /**
     * description.
     */
    public String description = null;
    /**
     * system.
     */
    public String system = null;
    /**
     * store.
     */
    public String store = null;
    /**
     * time.
     */
    public Long time = null;
    /**
     * previewurl.
     */
    public String previewurl = null;
    /**
     * taglist.
     */
    public List<String> taglist = null;
    /**
     * maxage.
     */
    public Long maxage = null;
    /**
     * comments.
     */
    public List<String> comments = null;
    /**
     * albums.
     */
    public List<String> albums = null;
    /**
     * tagscount.
     */
    public Integer tagscount = null;
    /**
     * commentscount.
     */
    public Integer commentscount = null;
    /**
     * uploadedvisppid.
     */
    public String uploadedviaappid = null;
    /**
     * uplaodedviapptype.
     */
    public String uploadedviaapptype = null;
    /**
     * albumidlist.
     */
    public List<Long> albumidlist = null;

    /**
     * Identifues the class.
     * @return string
     */
    public final String name() {
        return "ContentUtilsIn";
    }
    /**
     * @return string.
     */
    public final String toString() {
        String string = name() + "=[";
        string += "filename :" + filename;
        string += ", bytesmime : " + bytesmime;

        return string;
    }
}