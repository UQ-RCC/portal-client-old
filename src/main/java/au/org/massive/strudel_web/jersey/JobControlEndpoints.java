package au.org.massive.strudel_web.jersey;
import au.org.massive.strudel_web.*;
import au.org.massive.strudel_web.storage.TokenCache;
import au.org.massive.strudel_web.storage.UserPreferenceCache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.json.JSONObject;
//import org.keycloak.common.util.Base64Url;
//import org.keycloak.common.util.KeycloakUriBuilder;

import com.google.gson.Gson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jrigby
 * @modified hoangnguyen177
 */
@Path("/")
public class JobControlEndpoints extends Endpoint {

    private static final Logger logger = LogManager.getLogger(JobControlEndpoints.class);

    /**
     * Gets (and creates, if necessary) a session and returns the id and whether the session
     * currently has a certificate associated with it.
     *
     * @param request the {@link HttpServletRequest} object injected from the {@link Context}
     * @return a json object with some session info
     */
    @GET
    @Path("session_info")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSessionInfo(@Context HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String authServiceId;
        if ((authServiceId = request.getParameter("service")) == null) {
            authServiceId = Settings.getInstance().getDefaultProvider();
        } 
        String sessionId = request.getSession(true).getId();
        Session session;
        try {
            session = new Session(sessionId);
        } catch (NoSuchSessionException e1) {
            // If the server restarts and a client has a stale session, a new one is created
            request.getSession().invalidate();
            return getSessionInfo(request);
        }
        response.put("session_id", sessionId);
        response.put("uname", session.getUsername(authServiceId));
        response.put("has_oauth_access_token", String.valueOf(session.hasAccessToken(authServiceId)));
        // all the providers that this one has access to
        List<String> providers = new LinkedList<String>();
        for(String provider: Settings.getInstance().getNonDefaultProvider()) {
        	if(session.hasAccessToken(provider))
        		providers.add(provider);
        }
        response.put("providers", providers);
        if (session.hasUserEmail()) {
            response.put("email", session.getUserEmail());
        } else {
            response.put("email", "");
        }
        return (new JSONObject(response)).toString();
    }

    /**
     * Get access token
     * @param request
     * @return
     */
    @GET
    @Path("access_token")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAccessToken(@Context HttpServletRequest request) {
    	String authServiceId;
        if ((authServiceId = request.getParameter("service")) == null) {
            authServiceId = Settings.getInstance().getDefaultProvider();
        }
    	Map<String, String> response = new HashMap<>();
        Session session = this.getSession(request);
        if(session.accessTokenExpired(authServiceId)) {
        	logger.info("Token expired, get anew one for:" + authServiceId);
        	try {
				OAuthService.doRequestNewAccessToken(session, authServiceId);
			} catch (Exception e) {
				logger.info("Something wrong with acquiring new access token from refresh token.");
				logger.error(e.getMessage());
				logger.info("Removing the token");
				// remove this token
				session.clearToken(authServiceId);
				// remove from the cache
				TokenCache.getInstance().removeToken(session.getUserEmail(), authServiceId);
				return (new JSONObject(response)).toString();
			} 
        }
        else
        	logger.info("Token not expired for:" + authServiceId);
        response.put("access_token", session.getAccessToken(authServiceId));
        response.put("uid", session.getUserID(authServiceId));
        response.put("uname", session.getUsername(authServiceId));
        logger.info(session.toString());
        return (new JSONObject(response)).toString();
    }

    
    /**
     * Triggers a session logout
     *
     * @param request  the {@link HttpServletRequest} object injected from the {@link Context}
     * @param response the {@link HttpServletResponse} object injected from the {@link Context}
     * @return a status message
     * @throws IOException thrown on network IO errors
     */
    @GET
    @Path("end_session")
    @Produces(MediaType.APPLICATION_JSON)
    public String invalidateSession(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
    	String authServiceId;
        if ((authServiceId = request.getParameter("service")) == null) {
            authServiceId = Settings.getInstance().getDefaultProvider();
        }
    	Session session = getSession(request);
        session.clearAccessToken(authServiceId);
        session.clearRefreshToken(authServiceId);
        SessionManager.endSession(session);
        Map<String, String> responseMessage = new HashMap<>();
        responseMessage.put("message", "Session " + session.getSessionId() + " invalidated");
        return (new JSONObject(responseMessage)).toString();
    }
    

    @GET
    @Path("configuration")
    @Produces(MediaType.APPLICATION_JSON)
    public String getConfigurationName() {
    	Map<String, String> responseMessage = new HashMap<>();
    	responseMessage.put("configuration", Settings.getInstance().getConfigurationName());
    	return (new JSONObject(responseMessage)).toString();
    }
    
    /************************preference**********************************/
    private String getUserEmail(HttpServletRequest request){
    	String sessionId = request.getSession(true).getId();
        Session session;
        try {
            session = new Session(sessionId);
        } catch (NoSuchSessionException e1) {
            // If the server restarts and a client has a stale session, a new one is created
            request.getSession().invalidate();
            return getSessionInfo(request);
        }
        return session.getUserEmail();
    }
    
    @GET
    @Path("preference")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPreference(@Context HttpServletRequest request) {
        String email = this.getUserEmail(request);
        Document preference= UserPreferenceCache.getInstance().getPreference(email);        
        return preference.toJson();
    }
    
    @PUT
    @Path("preference")
    @Consumes({MediaType.APPLICATION_JSON})
    public void addPreference(@Context HttpServletRequest request, 
    							String pref) {
    	Gson gson = new Gson();
        Map<String, Object> prefMap = new HashMap<String, Object>();
        prefMap = (Map<String, Object>)gson.fromJson(pref, prefMap.getClass());
        String email = this.getUserEmail(request);
    	UserPreferenceCache.getInstance().addUpdatePreference(email, prefMap); 
    }
    
    
   
}
