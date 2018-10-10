package au.org.massive.strudel_web;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;

/**
 * An abstraction from the {@link HttpSession} object, mediating access to session attributes
 *
 * @author jrigby
 * @modified: hoangnguyen177
 */
public class Session {

    private final HttpSession session;
    // Session attribute keys
    private static final String USER_EMAIL = "user-email";
    
    public Session(String sessionId) throws NoSuchSessionException {
        session = SessionManager.getSessionById(sessionId);
    	if (session == null) {
            throw new NoSuchSessionException();
        }
    }

    public Session(HttpSession session) {
    	this.session = session;
    }

    public Session(HttpServletRequest request) {
    	this(request.getSession());
    }

    public HttpSession getHttpSession() {
        return session;
    }

    public String getSessionId() {
        return session.getId();
    }

    public void setUserEmail(String email) {
        session.setAttribute(USER_EMAIL, email);
    }

    public String getUserEmail() {
        return (String) session.getAttribute(USER_EMAIL);
    }

    public boolean hasUserEmail() {
        return getUserEmail() != null;
    }
    
    public void setExpiresIn(String provider, Long expiresIn) {
    	session.setAttribute(provider+"-lastset", Instant.now().toEpochMilli());
    	session.setAttribute(provider+"-expires", expiresIn);
    }
    
    public boolean accessTokenExpired(String provider) {
    	Long lastSetExpire = (Long)session.getAttribute(provider+"-lastset");
    	Long _expiresInSecond = (Long)session.getAttribute(provider+"-expires");
    	if(_expiresInSecond == null)
    		return true;
    	if(lastSetExpire == null) {
    		session.setAttribute(provider+"-lastset", Instant.now().toEpochMilli());
    		return false;
    	}
    	return (Math.abs(lastSetExpire - Instant.now().toEpochMilli()) >= _expiresInSecond*1000);
    }
    
    public void setUserID(String provider, String uid) {
    	String keyName = provider + "-uid";
    	session.setAttribute(keyName, uid);
    }
    
    public String getUserID(String provider) {
    	String keyName = provider + "-uid";
    	return (String)session.getAttribute(keyName);
    }
    
    public void setUsername(String provider, String uname) {
    	String keyName = provider + "-uname";
    	session.setAttribute(keyName, uname);
    }
    
    public String getUsername(String provider) {
    	String keyName = provider + "-uname";
    	return (String)session.getAttribute(keyName);
    }
    
    
    
    @Override
    public boolean equals(Object o) {
        return getSessionId().equals(o);
    }

    @Override
    public int hashCode() {
        return getSessionId().hashCode();
    }
    
    public void setAccessToken(String serviceName, String accessToken) {
    	String keyName = serviceName + "-access-token";
    	session.setAttribute(keyName, accessToken);
    }
    
    public String getAccessToken(String serviceName) {
    	String keyName = serviceName + "-access-token";
    	return (String)session.getAttribute(keyName);
    }
    
    public boolean hasAccessToken(String serviceName) {
    	return this.getAccessToken(serviceName)!=null;
    }
    
    public void setRefreshToken(String serviceName, String refreshToken) {
    	String keyName = serviceName + "-refresh-token";
    	session.setAttribute(keyName, refreshToken);
    }
    
    public boolean hasRefreshToken(String serviceName) {
    	return this.getRefreshToken(serviceName)!=null;
    }
    
    public void clearAccessToken(String serviceName) {
    	String keyName = serviceName + "-access-token";
    	session.removeAttribute(keyName);
    }
    
    public void clearRefreshToken(String serviceName) {
    	String keyName = serviceName + "-refresh-token";
    	session.removeAttribute(keyName);
    }
    
    public String getRefreshToken(String serviceName) {
    	String keyName = serviceName + "-refresh-token";
    	return (String)session.getAttribute(keyName);
    }
    
    public void clearToken(String serviceName) {
    	session.removeAttribute(serviceName + "-access-token");
    	session.removeAttribute(serviceName + "-refresh-token");
    	session.removeAttribute(serviceName + "-lastset");
    	session.removeAttribute(serviceName + "-expires");
    }
    
    
}
