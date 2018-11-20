package au.org.massive.strudel_web.storage;

import java.util.HashMap;
import java.util.Map;
import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

import au.org.massive.strudel_web.OAuthToken;
import au.org.massive.strudel_web.Settings;

public class TokenCache{
	private static TokenCache instance;
	private MongoCollection<Document> tokenStorage = null;
	public TokenCache() {
		tokenStorage = Settings.getInstance().getStorage().getCollection("token");
	}
	public static TokenCache getInstance(){
        if (instance == null) {
            instance = new TokenCache();
        }
        return instance;
    }
	public Map<String, OAuthToken> getUserTokens(String useremail) {
		FindIterable<Document> findIterable = tokenStorage.find(Filters.eq("email", useremail));
		MongoCursor<Document> iterator = findIterable.iterator();
		Map<String, OAuthToken> tokens = new HashMap<String, OAuthToken>(); 
		while(iterator.hasNext()) {
			Document doc = iterator.next();
			String accessToken = doc.getString("accessToken");
			String refreshToken = doc.getString("refreshToken");
			String uid = doc.getString("uid");
			Long accessTokenExpiry = doc.getLong("accessTokenExpiry");
			String provider = doc.getString("provider");
			OAuthToken token = new OAuthToken(accessToken, refreshToken, accessTokenExpiry, uid);
			tokens.put(provider, token);
		}
		return tokens;
	}

	public void addToken(String useremail, String provider, OAuthToken token) {
		System.out.println("Add token:" + token.toString());
		Document tokenDoc = new Document(token.getMap());
		tokenDoc.put("email", useremail);
		tokenDoc.put("provider", provider);
		UpdateResult updateResult = tokenStorage.updateOne(
				Filters.and(Filters.eq("email", useremail), 
							Filters.eq("provider", provider)),
				tokenDoc);
		if(updateResult.getModifiedCount() == 0 ) {
			tokenStorage.insertOne(tokenDoc);
		}
	}

	public void removeToken(String useremail, String provider) {
		System.out.println("Removing token: useremail" + useremail + "||provider:" + provider);
		tokenStorage.deleteMany(
								Filters.and(Filters.eq("email", useremail), 
											Filters.eq("provider", provider)));
	}

}
