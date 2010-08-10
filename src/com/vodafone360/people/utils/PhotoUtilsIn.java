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
 * Class objects are received from UI and passed to engine from the interface.
 * General use of this class is to pass data from UI to contentEngine interface.
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
     * contentId.
     */
    public Long contentId = null;
    /**
     * remoteId.
     */
    public String remoteId = null;
    /**
     * bytesMime.
     */
    public String bytesMime = null;
    /**
     * extFid.
     */
    public String extFid = null;
    /**
     * title.
     */
    public String title = null;
    /**
     * fileName.
    */
    public String fileName = null;
    /**
     * filePath.
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
     * previewUrl.
     */
    public String previewUrl = null;
    /**
     * tagList.
     */
    public List<String> tagList = null;
    /**
     * maxAge.
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
     * tagsCount.
     */
    public Integer tagsCount = null;
    /**
     * commentsCount.
     */
    public Integer commentsCount = null;
    /**
     * uploadedViaAppid.
     */
    public String uploadedViaAppid = null;
    /**
     * uplaodedViaApptype.
     */
    public String uploadedViaApptype = null;
    /**
     * albumIdList.
     */
    public List<Long> albumIdList = null;

    /**
     * Identifies the class.
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
        string += "filename :" + fileName;
        string += ", bytesmime : " + bytesMime;

        return string;
    }
}
