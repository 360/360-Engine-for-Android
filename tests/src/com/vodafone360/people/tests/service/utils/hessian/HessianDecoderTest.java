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

package com.vodafone360.people.tests.service.utils.hessian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.datatypes.UserProfile;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.io.Request.Type;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.utils.hessian.HessianDecoder;

public class HessianDecoderTest extends AndroidTestCase {

	private byte testSessionData[] = {114, 0, 0 , 'M', 't', 0, 0, 'S', 0, 7, 's','e','s','s','i','o','n', 'M',
			't', 0, 0, 'S', 0, 6, 'u','s','e','r','i','d','L', 0, 0, 0, 0, 1, 2, 3, 4, 'S', 0, 13,
			's','e','s','s','i','o','n','s','e','c','r','e','t', 'S', 0, 5, 's','e','s','h','y',
			'S', 0, 8, 'u','s','e','r','n','a','m','e', 'S', 0, 4, 'u','s','e','r','S', 0, 9,
			's','e','s','s','i','o','n','i','d', 'S', 0, 5, 's','s','s','s','s','z','z','z'};

	private byte testUserProfileData[] = {114,1,0,'M','t',0,0,'S',0,5,'i','t','e','m','s','I',0,0,0,1,
			'S',0,15, 'u','s','e','r','p','r','o','f','i','l','e','l','i','s','t','V','t',0,0,'l',0,0,0,1,
			'M','t',0,0,'S',0,9,'c','o','n','t','a','c','t','i','d','L',0,0,0,0,0,0,0,23,'S',0,6,
			'u','s','e','r','i','d', 'L',0,0,0,0,0,0,0,3,'S',0,6,'f','r','i','e','n','d','F','S',0,10,'d','e','t','a','i','l','l','i','s','t','V',
			'l',0,0,0,2,'M','S',0,3,'k','e','y','S',0,10,'v','c','a','r','d','.','n','a','m','e','S',0,3,'v','a','l',
			'S',0,16,'m','a','n',';','a','r','t','h','u','r',';','b','i','l','l','y','z','M','S',0,3,'k','e','y','S',0,13,
			'p','r','e','s','e','n','c','e','.','t','e','x','t','S',0,3, 'v','a','l','S',0,5,'T','h','i','s',' ',
			'S',0,4,'t','y','p','e','S',0,7,'n','o','w','p','l','u','s','S',0,7,'u','p','d','a','t','e','d',
			'L',0,0,0,0,0,1,2,3,'z','z','z','z','z','z',};

	private byte testContactListData[] = { 114, 1,0, 'M', 'S', 0, 5,'i','t','e','m','s','I',0,0,0,3,'S',0,11,'c','o','n','t','a','c','t','l','i','s','t','V','l',0,0,0,3,
			'M','S',0,9,'c','o','n','t','a','c','t','i','d','L',0,0,0,0,0,0,0,'+','S',0,7,'d','e','l','e','t','e','d','F','S',0,6,'u','s','e','r','i','d','L',0,0,0,0,0,0,0,0,
			'S',0,6,'f','r','i','e','n','d','F','S',0,10,'d','e','t','a','i','l','l','i','s','t','V',
			'l',0,0,0,3,'M','S',0,3,'k','e','y','S',0,10,'v','c','a','r','d','.','n','a','m','e','S',0,3,'v','a','l','S',0,16,'m','a','n',';','a','r','t','h','u','r',';','b','i','l','l','y','z','M','S',0,3,'k','e','y','S',0,14,'v','c','a','r','d','.','n','i','c','k','n','a','m','e','S',0,3,'v','a','l',
			'S',0,10,'a','r','t','h','u','r',' ','m','a','n','z','M','S',0,3,'k','e','y','S',0,11,'v','c','a','r','d','.','p','h','o','n','e','S',0,3,'v','a','l','S',0,9,'1','3','3','3','5','r','4','3','5','S',0,4,'t','y','p','e','S',0,4,'c','e','l','l','S',0,8,'d','e','t','a','i','l','i','d','L',0,0,0,0,23,0,0,0,'S',0,5,'o','r','d','e','r','I',0,0,0,2,'z','z','z',
			'M','S',0,9,'c','o','n','t','a','c','t','i','d','L',0,0,0,0,0,65,53,3,'S',0,7,'d','e','l','e','t','e','d','F','S',0,6,'u','s','e','r','i','d','L',0,0,0,0,0,0,0,0,'S',0,6,'f','r','i','e','n','d','F',
			'S',0,10,'d','e','t','a','i','l','l','i','s','t','V','l',0,0,0,2,'M','S',0,3,'k','e','y','S',0,10,'v','c','a','r','d','.','n','a','m','e','S',0,3,'v','a','l','S',0,7,'U','n','k','n','o','w','n','z','M','S',0,3,'k','e','y','S',0,14,
			'v','c','a','r','d','.','n','i','c','k','n','a','m','e','S',0,3,'v','a','l','S',0,7,'U','n','k','n','o','w','n','z','z','z','M','S',0,9,'c','o','n','t','a','c','t','i','d','L',0,0,0,0,0,0,0,0,'S',0,7,'d','e','l','e','t','e','d','F','S',0,6,'u','s','e','r','i','d','L',0,0,0,0,0,0,0,0,'S',0,6,'f','r','i','e','n','d','F','S',0,10,
			'd','e','t','a','i','l','l','i','s','t','V','l',0,0,0,7,'M','S',0,3,'k','e','y','S',0,10,
			'v','c','a','r','d','.','n','a','m','e','S',0,3,'v','a','l','S',0,3,'Z','Y','B','z','M','S',0,3,'k','e','y','S',0,14,'v','c','a','r','d','.','n','i','c','k','n','a','m','e','S',0,3,'v','a','l','S',0,5,'B','i','l','l','y','z','M','S',0,3,'k','e','y','S',0,11,'v','c','a','r','d','.','p','h','o','n','e','S',0,3,'v','a','l','S',0,10,
			'+','4','4','1','2','3','4','5','5','6','S',0,4,'t','y','p','e','S',0,4,'c','e','l','l','S',0,8,'d','e','t','a','i','l','i','d','L',0,0,0,0,23,23,0,0,'S',0,5,'o','r','d','e','r','I',0,0,0,2,'z','M','S',0,3,'k','e','y','S',0,11,'v','c','a','r','d','.','p','h','o','n','e','S',0,3,'v','a','l','S',0,
			10,'w','i','b','c','h','e','s','t','e','r','S',0,4,'t','y','p','e','S',0,4,'h','o','m','e','S',0,8,'d','e','t','a','i','l','i','d','L',0,0,0,0,0,0,0,12,'S',0,5,'o','r','d','e','r','I',0,0,0,2,'z','M','S',0,3,'k','e','y','S',0,11,'v','c','a','r','d','.','e','m','a','i','l','S',0,3,'v','a','l','S',0,20,'m','i','k','e','b','y','r','n','e','@','m','y','.','z','y','b','.','c','o','m','S',0,4,'t','y','p','e','S',0,4,
			'h','o','m','e','S',0,8,'d','e','t','a','i','l','i','d','L',0,0,0,0,0,0,24,3,'S',0,5,'o','r','d','e','r','I',0,0,0,2,'z','M','S',0,3,'k','e','y','S',0,10,
			'v','c','a','r','d','.','d','a','t','e','S',0,3,'v','a','l','S',0,10,
			'1','9','9','3','-','0','1','-','0','2','S',0,4,'t','y','p','e','S',0,8,'b','i','r','t','h','d','a','y','z','M','S',0,3,'k','e','y','S',0,10,
			'v','c','a','r','d','.','n','o','t','e','S',0,3,'v','a','l','S',0,5,'?','S','e','n','d','S',0,8,'d','e','t','a','i','l','i','d','L',0,0,0,0,127,65,53,3,'z','z','z'};

	private byte testIdentityListData[] = {114,1,0,77,116,0,0,83,0,21,'a','v','a','i','l','a','b','l','e','i','d','e','n','t','i','t','y','l','i','s','t',
			'V', 't', 0,0, 'l', 0,0,0,1,77,116,0,0,83,0,22,'i','d','e','n','t','i','t','y','c','a','p','a','b','i','l','i','t','y','l','i','s','t', 
			'V','t',0,0,'l',0,0,0,2,77,116,0,0,83,0,5,'v','a','l','u','e','T','S',0,12,
			'c','a','p','a','b','i','l','i','t','y','i','d','S',0,4,'c','h','a','t','S',0,11,
			'd','e','s','c','r','i','p','t','i','o','n','S',0,3,'c','a','p','S',0,4,'n','a','m','e','S',0,4,'C','h','a','t','z','M',
			116,0,0,83,0,5,'v','a','l','u','e','T','S',0,12,'c','a','p','a','b','i','l','i','t','y','i','d','S',0,4,'m','a','i','l','S',0,11,
			'd','e','s','c','r','i','p','t','i','o','n','S',0,3,'y','e','s','S',0,4,
			'n','a','m','e','S',0,4,'M','a','i','l','z','z', 83,0,9,'i','c','o','n','2','m','i','m','e','S',0,9,
			'i','m','a','g','e','/','p','n','g','S',0,8,'p','l','u','g','i','n','i','d','S',0,14,
			'm','a','i','l','i','d','e','n','t','i','t','i','e','s','S',0,8,'i','c','o','n','2','u','r','l','S',0,19,
			'h','t','t','p',':','/','/','2','_','g','o','o','g','l','e','.','p','n','g','S',0,10,
			'n','e','t','w','o','r','k','u','r','l','S',0,21,'h','t','t','p',':','/','/','w','w','w','.',
			'g','o','o','g','l','e','.','c','o','m','S',0,8,'a','u','t','h','t','y','p','e','S',0,11,'c','r','e','d','e','n','t','i','a','l','s',
			'S',0,8,'i','c','o','n','m','i','m','e','S',0,9,'i','m','a','g','e','/','p','n','g',
			'S',0,5,'o','r','d','e','r','I',0,0,0,3,83,0,7,'i','c','o','n','u','r','l',
			'S',0,12,'1','_','g','o','o','g','l','e','.','p','n','g',83,0,4,'n','a','m','e',
			'S',0,6,'G','o','o','g','l','e','S',0,7,'n','e','t','w','o','r','k','S',0,6,'g','o','o','g','l','e','z',122};

	/*
	private byte testCountryList[] = {114,1,0,77,116,0,0,83,0,5,'i','t','e','m','s','I',0,0,0,1,83,0,11,
			'c','o','u','n','t','r','y','l','i','s','t','V','t',0,0,'l',0,0,0,2, 
			'M','t',0,0,'S',0,7,'c','o','u','n','t','r','y','S',0,3,'A','L','L','z',
			'M','t',0,0,'S',0,7,'c','o','u','n','t','r','y','S',0,2,'A','T',122,122,122,122};
*/

	private byte testActivityList[] = {114,1,0,77,116,0,0,83,0,12,'a','c','t','i','v','i','t','y','l','i','s','t', 
			'V',116,0,0,'l',0,0,0,2,77,116,0,0,83,0,7,'d','e','l','e','t','e','d','F',83,0,7,'c','r','e','a','t','e','d',
			'L',0,0,0,0,22,23,24,25,83,0,10,'a','c','t','i','v','i','t','y','i','d','L',0,0,0,0,26,27,28,29,
			83,0,8,'f','l','a','g','l','i','s','t','V',116,0,0,'l',0,0,0,1,83,0,6,'s','t','a','t','u','s',122,
			83,0,05,'s','t','o','r','e',83,0,6,'g','o','o','g','l','e',83,0,05,'t','i','t','l','e',
			83,0,5,'h','e','l','l','o',83,0,4,'t','y','p','e',83,0,8,'s','n','_','a','d','d','e','d',
			83,0,3,'u','r','l',83,0,10,'g','o','o','g','l','e','.','c','o','m',83,0,4,'t','i','m','e','L',0,0,0,0,33,32,31,30,
			83,0,11,'d','e','s','c','r','i','p','t','i','o','n',83,0,2,'o','k',
			'S',0,11,'c','o','n','t','a','c','t','l','i','s','t','V',116,0,0,'l',0,0,0,1,
			'M','S',0,9,'c','o','n','t','a','c','t','i','d','L',0,0,0,0,0,0,0,'+',
			83,0,6,'u','s','e','r','i','d','L',0,0,0,0,40,41,42,43,83,0,7,'a','d','d','r','e','s','s', 83,0,2,'n','o',
			83,0,4,'n','a','m','e',83,0,3,'b','o','b',83,0,7,'n','e','t','w','o','r','k',83,0,6,'g','o','o','g','l','e',122,122,
			83,0,7,'u','p','d','a','t','e','d','L',0,0,0,0,0,0,12,13,122,
			77,116,0,0,83,0,7,'d','e','l','e','t','e','d','F',83,0,7,'c','r','e','a','t','e','d',
			'L',0,0,0,0,22,23,24,25,83,0,10,'a','c','t','i','v','i','t','y','i','d','L',0,0,0,0,26,27,28,29,
			83,0,8,'f','l','a','g','l','i','s','t','V',116,0,0,'l',0,0,0,1,83,0,8,'t','i','m','e','l','i','n','e',122,
			83,0,05,'s','t','o','r','e',83,0,6,'g','o','o','g','l','e',83,0,05,'t','i','t','l','e',
			83,0,5,'h','e','l','l','o',83,0,4,'t','y','p','e',83,0,8,'s','n','_','a','d','d','e','d',
			83,0,3,'u','r','l',83,0,10,'g','o','o','g','l','e','.','c','o','m',83,0,4,'t','i','m','e','L',0,0,0,0,33,32,31,30,
			83,0,11,'d','e','s','c','r','i','p','t','i','o','n',83,0,2,'o','k',
			'S',0,11,'c','o','n','t','a','c','t','l','i','s','t','V',116,0,0,'l',0,0,0,1,
			'M','S',0,9,'c','o','n','t','a','c','t','i','d','L',0,0,0,0,0,0,0,'+',
			83,0,6,'u','s','e','r','i','d','L',0,0,0,0,40,41,42,43,83,0,7,'a','d','d','r','e','s','s', 83,0,2,'n','o',
			83,0,4,'n','a','m','e',83,0,3,'b','o','b',83,0,7,'n','e','t','w','o','r','k',83,0,6,'g','o','o','g','l','e',122,122,
			83,0,7,'u','p','d','a','t','e','d','L',0,0,0,0,0,0,12,13,122,122,83,0,5,'i','t','e','m','s','I',0,0,0,1,
			83,0,7,'u','p','d','a','t','e','d','L',0,0,0,0,0,0,12,13,122,122};

	private byte[] testErrorResponse = {114,1,0,'f',83,0,4,'c','o','d','e',83,0,14,'I','N','T','E','R','N','A','L','_','E','R','R','O','R',
			83,0,7,'m','e','s','s','a','g','e',83,0,5,'e','r','r','o','r',83,0,7,'d','e','t','a','i','l','s',77,116,0,0,83,0,5,'c','l','a','s','s',
			83,0,04,'n','o','n','e',122,122,122};

	
	private byte[] testPcPushMsgData = {114,1,0,77,116,0,0,83,0,6,'u','s','e','r','i','d','L',0,0,0,0,1,2,3,4,
			83, 0, 4, 't','y','p','e', 83, 0, 2, 'p','c',83,0,7,'p','a','y','l','o','a','d',77,116,0,0,122,122};
	
	private byte[] testCcPushMsgData = {114,1,0,77,116,0,0,83,0,6,'u','s','e','r','i','d','L',0,0,0,0,1,2,3,4,
			83, 0, 4, 't','y','p','e', 83, 0, 2, 'c','c',83,0,7,'p','a','y','l','o','a','d',77,116,0,0,122,122};
	
	private byte[] testSnPushMsgData = {114,1,0,77,116,0,0,83,0,6,'u','s','e','r','i','d','L',0,0,0,0,1,2,3,4,
			83, 0, 4, 't','y','p','e', 83, 0, 2, 's','n',83,0,7,'p','a','y','l','o','a','d',77,116,0,0,
			83,0,4,'c','o','d','e',83,0,4,'1','1','0','2',83,0,7,'m','e','s','s','a','g','e',83,0,3,'e','r','r', 122,122};
	
	private byte[] testSnPushMsgData2 = {114,1,0,77,116,0,0,83,0,6,'u','s','e','r','i','d','L',0,0,0,0,1,2,3,4,
			83, 0, 4, 't','y','p','e', 83, 0, 2, 's','n',83,0,7,'p','a','y','l','o','a','d',77,116,0,0,
			83,0,4,'c','o','d','e',83,0,4,'1','1','0','0',83,0,7,'m','e','s','s','a','g','e',83,0,3,'e','r','r', 122,122};
	
	private byte[] testUnknownPushMsgData = {114,1,0,77,116,0,0,83,0,6,'u','s','e','r','i','d','L',0,0,0,0,1,2,3,4,
			83, 0, 4, 't','y','p','e', 83, 0, 2, 'x','x',83,0,7,'p','a','y','l','o','a','d',77,116,0,0,122,122};/*
	 * 
	 * r{1}{0}fS{0}{4}codeS{0}{14}INTERNAL_ERRORS{0}{7}
	 * messageS{0}hError occured while getting contacts. Message was: Object reference not set to an instance of an object.S{0}{7}detailsMt{0}{0}S{0}{5}classS{0}Dcom.vodafone.next.api.common.APIStructures$APIInternalErrorExceptionzzz
	 */

	@MediumTest
	public void testSessionResponse(){
		//boolean testPassed = true;
		List<BaseDataType> slist = new ArrayList<BaseDataType>();

		HessianDecoder hess = new HessianDecoder();
		try {
			DecodedResponse resp = hess.decodeHessianByteArray(1, testSessionData, Type.COMMON, false, EngineId.UNDEFINED);
			slist = resp.mDataTypes;
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}

		int size = slist.size();
		assertTrue(size == 1);
		assertTrue(slist.get(0) instanceof AuthSessionHolder);
	}

	@MediumTest
	public void testUserProfileResponse(){
		//boolean testPassed = true;
		List<BaseDataType> ulist = new ArrayList<BaseDataType>();

		HessianDecoder hess = new HessianDecoder();
		try {
			DecodedResponse resp = hess.decodeHessianByteArray(2, testUserProfileData, Type.COMMON, false, EngineId.UNDEFINED);
			ulist = resp.mDataTypes;
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}

		int size = ulist.size();

		assertTrue(size == 1);
		assertTrue(ulist.get(0) instanceof UserProfile);
	}


	@MediumTest
	public void testContactListResponse(){
		//boolean testPassed = true;
		List<BaseDataType> clist = new ArrayList<BaseDataType>();

		HessianDecoder hess = new HessianDecoder();
		try {
			DecodedResponse resp = hess.decodeHessianByteArray(3, testContactListData, Type.COMMON, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}

		int size = clist.size();
		assertTrue(size == 3);
		assertTrue(clist.get(0) instanceof Contact);
	}

	@MediumTest
	public void testIdentityListResponse(){
		//boolean testPassed = true;
		List<BaseDataType> clist = new ArrayList<BaseDataType>();

		HessianDecoder hess = new HessianDecoder();
		try {
			DecodedResponse resp = hess.decodeHessianByteArray(4, testIdentityListData, Type.COMMON, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}

		int size = clist.size();
		assertTrue(size == 1);
		assertTrue(clist.get(0) instanceof Identity);
	}


	@MediumTest
	public void testActivityListResponse(){
		//boolean testPassed = true;
		List<BaseDataType> clist = new ArrayList<BaseDataType>();

		HessianDecoder hess = new HessianDecoder();
		try {
			DecodedResponse resp = hess.decodeHessianByteArray(5, testActivityList, Type.COMMON, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}

		int size = clist.size();
		assertTrue(size == 2);
		assertTrue(clist.get(0) instanceof ActivityItem);
	}

	@MediumTest
	public void testErrorResponse(){
		//boolean testPassed = true;
		List<BaseDataType> clist = new ArrayList<BaseDataType>();

		HessianDecoder hess = new HessianDecoder();
		try {
			DecodedResponse resp = hess.decodeHessianByteArray(6, testErrorResponse, Type.COMMON, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}

		int size = clist.size();
		assertTrue(size == 1);
		assertTrue(clist.get(0) instanceof ServerError);
	}
	/*
	@MediumTest
	public void testContactChangesResponse(){
		
	}
	*/
	@MediumTest
	public void testPushMessages(){
		List<BaseDataType> clist = new ArrayList<BaseDataType>();
		
		HessianDecoder hess = new HessianDecoder();
		try{
			DecodedResponse resp = hess.decodeHessianByteArray(7, testPcPushMsgData, Type.PUSH_MSG, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		}
		catch(IOException e){
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}
		
		int size = clist.size();
		assertTrue(size == 1);
		assertTrue(clist.get(0) instanceof PushEvent);
		
		try{
			DecodedResponse resp = hess.decodeHessianByteArray(8, testCcPushMsgData, Type.PUSH_MSG, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		}
		catch(IOException e){
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}
			
		size = clist.size();
		assertTrue(size == 1);
		assertTrue(clist.get(0) instanceof PushEvent);
		
		try{
			DecodedResponse resp = hess.decodeHessianByteArray(9, testSnPushMsgData, Type.PUSH_MSG, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		}
		catch(IOException e){
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}
		
		size = clist.size();
		assertTrue(size == 1);
		assertTrue(clist.get(0) instanceof SystemNotification);
		
		try{
			DecodedResponse resp = hess.decodeHessianByteArray(10, testSnPushMsgData2, Type.PUSH_MSG, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		}
		catch(IOException e){
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}
		
		size = clist.size();
		assertTrue(size == 1);
		assertTrue(clist.get(0) instanceof SystemNotification);
		
		try{
			DecodedResponse resp = hess.decodeHessianByteArray(11, testUnknownPushMsgData, Type.PUSH_MSG, false, EngineId.UNDEFINED);
			clist = resp.mDataTypes;
		}
		catch(IOException e){
			e.printStackTrace();
			assertTrue("IOException thrown", false);
		}
			
		size = clist.size();
		assertTrue(size == 0);
		//assertTrue(clist.get(0) instanceof PushEvent);
		
	}
}
