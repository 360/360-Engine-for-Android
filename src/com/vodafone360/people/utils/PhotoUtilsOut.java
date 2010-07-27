/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
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

/**
 * Class returns the contentid returned from server to the UI.
 * Class is used by content engine to fill in details like contentid and filename and send it back to UI
 * Genaral use of this class is to send the data from content engine to UI.
 */

package com.vodafone360.people.utils;

public class PhotoUtilsOut
{
	public String filename;
	public long contentid;
	public byte bytes[];
	public String filePathOnDevice;
	
}