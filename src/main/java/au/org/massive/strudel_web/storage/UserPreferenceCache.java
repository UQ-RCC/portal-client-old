package au.org.massive.strudel_web.storage;


import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import au.org.massive.strudel_web.Settings;

import java.util.Map;
import java.util.HashMap;

public class UserPreferenceCache {
	private static UserPreferenceCache instance;
	
	public UserPreferenceCache() {
	}
	public static UserPreferenceCache getInstance(){
        if (instance == null) {
            instance = new UserPreferenceCache();
        }
        return instance;
    }
	
	
	public Document getPreference(String service, String uname) {
		MongoCollection<Document> storage = 
				Settings.getInstance().getStorage().getCollection(service+"_preference");
		FindIterable<Document> findIterable = storage.find(Filters.eq("username", uname));
		// the very first one
		Document prefDocument = findIterable.first();
		if(prefDocument == null) {
			prefDocument = new Document(new HashMap<String,Object>());
			prefDocument.put("username", uname);
			storage.insertOne(prefDocument);
		}
		return prefDocument;
	}
	
	public void addUpdatePreference(String service, String uname, Map<String, Object> preference) {
		if(uname == null || uname.isEmpty())
			return;
		MongoCollection<Document> storage = 
				Settings.getInstance().getStorage().getCollection(service+"_preference");
		FindIterable<Document> findIterable = storage.find(Filters.eq("username", uname));
		Document prefDoc = findIterable.first();
		if(prefDoc == null) {
			// should not happen
			prefDoc = new Document(new HashMap<String,Object>());
			prefDoc.put("username", uname);
			storage.insertOne(prefDoc);
		}
		for(String key: preference.keySet()) {
			prefDoc.put(key, preference.get(key));
		}
		//apparent if it has _id, it will complain
		storage.replaceOne(Filters.eq("username", uname), prefDoc);
	}
}
