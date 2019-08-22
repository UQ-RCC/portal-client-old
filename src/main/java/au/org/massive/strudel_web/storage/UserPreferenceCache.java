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
	
	
	public Document getPreference(String service, String email) {
		MongoCollection<Document> storage = 
				Settings.getInstance().getStorage().getCollection(service+"_preference");
		FindIterable<Document> findIterable = storage.find(Filters.eq("email", email));
		// the very first one
		Document prefDocument = findIterable.first();
		if(prefDocument == null) {
			prefDocument = new Document(new HashMap<String,Object>());
			prefDocument.put("email", email);
			storage.insertOne(prefDocument);
		}
		return prefDocument;
	}
	
	public void addUpdatePreference(String service, String email, Map<String, Object> preference) {
		MongoCollection<Document> storage = 
				Settings.getInstance().getStorage().getCollection(service+"_preference");
		FindIterable<Document> findIterable = storage.find(Filters.eq("email", email));
		Document prefDoc = findIterable.first();
		if(prefDoc == null) {
			// should not happen
			prefDoc = new Document(new HashMap<String,Object>());
			storage.insertOne(prefDoc);
		}
		for(String key: preference.keySet()) {
			prefDoc.put(key, preference.get(key));
		}
		//apparent if it has _id, it will complain
		storage.replaceOne(Filters.eq("email", email), prefDoc);
	}
}
