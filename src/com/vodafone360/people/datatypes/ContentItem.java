package com.vodafone360.people.datatypes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class ContentItem extends BaseDataType implements Parcelable {
	public Long		m_localId	=	null;
	public Long		m_contentId	=	null;
	public String	m_fileName	=	null;
	public Short m_shared = null;
	public ContentItem() {
		m_localId	=	null;
		m_contentId	=	null;
		m_fileName	=	null;
		m_shared 	= null;
	}
	
	public ContentItem(long id, String string, short shared) {
		m_contentId	=	id;
		m_fileName	=	string;
		m_shared		= shared;
	}

	public void clear(){
		m_contentId = null;
		m_fileName = null;
		m_shared = null;
	}

	@Override
	public int getType() {
		return	CONTENT_ITEM;
	}

	public String	toString() {
		String	string	=	"ContentItem : localID = ";
		if (m_localId == null)
			string	+=	"null";
		else
			string	+=	m_localId;
		string	+=	", Content ID = " + m_contentId;
		string	+=	", Filename = " + m_fileName;
		
		return	string;
	}
	@Override
	public int		describeContents() {
Log.e("DENNIS -- ContentItem::writeToParcel() -- ", "TODO ??");
		return	0;
	}

	@Override
	public void	writeToParcel(Parcel dest, int flags) {
Log.e("DENNIS -- ContentItem::writeToParcel() -- ", "TODO ??");
	}
}