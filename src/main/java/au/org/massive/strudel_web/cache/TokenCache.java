package au.org.massive.strudel_web.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TokenCache extends DiskCache{
	private Map<String, Map<String, OauthToken>> tokens;
	private final static Logger logger = LogManager.getLogger(TokenCache.class);
	
	public TokenCache(String cacheFile) {
    	super(cacheFile);
    	tokens = getCache("tokens", null);
    }
	
	public Map<String, OauthToken> getUserTokens(String userid) {
		if(!tokens.containsKey(userid)) {
			Map<String, OauthToken> tokenList = new HashMap<String, OauthToken>();
			tokens.put(userid, tokenList);
			commit();
		}
		return tokens.get(userid);
	}
	
	public void addToken(String userid, String provider, OauthToken token) throws Exception {
		Map<String, OauthToken> tokenList = tokens.get(userid);
		if(tokenList == null) {
			throw new Exception("Cannot find userid");
		}
		tokenList.put(provider, token);
		tokens.put(userid, tokenList);
		commit();
	}
	
	public void removeToken(String userid, String provider) {
		Map<String, OauthToken> tokenList = tokens.get(userid);
		if(tokenList != null) {
			tokenList.remove(provider);
			tokens.put(userid, tokenList);
			commit();
		}
		
	}
	
}
