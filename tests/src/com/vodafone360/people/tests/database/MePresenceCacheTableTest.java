package com.vodafone360.people.tests.database;

import java.util.ArrayList;

import android.util.Log;

import com.vodafone360.people.database.tables.MePresenceCacheTable;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.presence.NetworkPresence;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;

public class MePresenceCacheTableTest extends NowPlusTableTestCase {

    
    private boolean createTable() {
        try {
            MePresenceCacheTable.create(mTestDatabase.getWritableDatabase());
        } catch(Exception ex) {
            return false;
        }
        
        return true;
    }
    
    private boolean checkCacheEquals(ArrayList<NetworkPresence> presenceList) {
        final ArrayList<NetworkPresence> cache = 
            MePresenceCacheTable.getCache(mTestDatabase.getReadableDatabase());
        if(presenceList == null && cache == null) {
            return true;
        } else if(presenceList == null || cache == null) {
            return false;
        }
        
        final int cacheSize = cache.size();
        
        if(presenceList.size() != cacheSize) {
            return false;
        }
        
        for(int i = 0; i < cacheSize; i++) {
            NetworkPresence cacheValue = cache.get(i);
            NetworkPresence listValue = presenceList.get(i);
            if(cacheValue.getNetworkId() != listValue.getNetworkId()) {
                return false;
            }
            
            if(!(cacheValue.getUserId().equals(listValue.getUserId()))) {
                return false;
            }
            
            if(cacheValue.getOnlineStatusId() != listValue.getOnlineStatusId()) {
                return false;
            }
        }
        
        return true;
    }
    
    public void testCreate() {
        Log.i(LOG_TAG, "***** testCreateTable *****");
        assertTrue(createTable());
        assertTrue(checkCacheEquals(null));
        Log.i(LOG_TAG, "***** testCreateTable SUCCEEDED*****");
    }
    
    public void testGetUpdateCache() {
        final String userId = "1234";
        assertTrue(createTable());
        final ArrayList<NetworkPresence> presenceList = new ArrayList<NetworkPresence>();
        
        int onlineStatus = OnlineStatus.ONLINE.ordinal();
        for(int i = SocialNetwork.FACEBOOK_COM.ordinal(); 
            i < SocialNetwork.VODAFONE.ordinal(); 
            i++) {
            NetworkPresence networkPresence = new NetworkPresence(userId, i, onlineStatus);
            presenceList.add(networkPresence);
            MePresenceCacheTable.updateCache(networkPresence, mTestDatabase.getWritableDatabase());
            assertTrue(checkCacheEquals(presenceList));
        }
        
        onlineStatus = OnlineStatus.OFFLINE.ordinal();
        for(int i = SocialNetwork.FACEBOOK_COM.ordinal(); 
            i < SocialNetwork.VODAFONE.ordinal(); 
            i++) {
            NetworkPresence networkPresence = new NetworkPresence(userId, i, onlineStatus);
            presenceList.set(i, networkPresence);
            MePresenceCacheTable.updateCache(networkPresence, mTestDatabase.getWritableDatabase());
            assertTrue(checkCacheEquals(presenceList));            
        }
    }
    
}
