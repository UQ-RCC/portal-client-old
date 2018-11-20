package au.org.massive.strudel_web;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class OAuthToken implements Serializable {

	private static final long serialVersionUID = 1L;
	private String accessToken = "";
	private String refreshToken = "";
	private Long accessTokenExpiry;
	private Instant lastSetAccessTokenExpiry;
	private String uid = "";
	
	public OAuthToken(String aToken, String rToken, Long accessTokenExpiry, String uid) {
		this.accessToken = aToken;
		this.refreshToken = rToken;
		this.accessTokenExpiry = accessTokenExpiry;
		this.uid = uid;
	}
	
	public String getAccessToken() {
		return this.accessToken;
	}
	
	public void setAccessToken(String aToken) {
		this.accessToken = aToken;
	}
	
	public String getRefreshToken() {
		return this.refreshToken;
	}
	
	public void setRefreshToken(String rToken) {
		this.refreshToken = rToken;
	}
	
	public void setAccessTokenExpiry(Long expiryTimeInSeconds) {
		this.accessTokenExpiry = expiryTimeInSeconds;
		this.lastSetAccessTokenExpiry = Instant.now();
	}
	
	public Long getAccessTokenExpiry() {
		return this.accessTokenExpiry;
	}
	
	public boolean isAccessTokenExpired() {
		if(this.lastSetAccessTokenExpiry ==null)
    		return false;
    	Instant now = Instant.now();
		return (Duration.between(now, this.lastSetAccessTokenExpiry).toMillis() >= this.accessTokenExpiry*1000);
	}
	
	public String getUid() {
		return this.uid;
	}
	
	public void setUid(String uid) {
		this.uid = uid;
	}
	
	public Map<String, Object> getMap() {
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("accessToken", accessToken);
		values.put("refreshToken", refreshToken);
		values.put("accessTokenExpiry", accessTokenExpiry);
		values.put("lastSetAccessTokenExpiry", lastSetAccessTokenExpiry);
		values.put("uid", uid);
		return values;
	}
	
}
