package au.org.massive.strudel_web;
import au.org.massive.strudel_web.storage.TokenCache;
import au.org.massive.strudel_web.storage.UserPreferenceCache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.bson.Document;
import org.json.JSONObject;

import com.google.gson.Gson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jrigby
 * @modified hoangnguyen177
 */
@Path("/api")
public class JobControlEndpoints {

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
    public String getSessionInfo(@Context HttpServletRequest request, @PathParam("service") String authServiceId) {
        Map<String, Object> response = new HashMap<>();
		if(authServiceId == null) {
			authServiceId = Settings.getInstance().getDefaultProvider();
		}

        String sessionId = request.getSession(true).getId();
        Session session;
        try {
            session = new Session(sessionId);
        } catch (NoSuchSessionException e1) {
            // If the server restarts and a client has a stale session, a new one is created
			// NB: If this stack overflows, make sure SessionManager is registered.
            request.getSession().invalidate();
            return getSessionInfo(request, authServiceId);
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
    public String getAccessToken(@Context HttpServletRequest request, @PathParam("service") String authServiceId) {
        if(authServiceId == null) {
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
    private String getUsername(HttpServletRequest request, String service){
    	if (service ==null || service.isEmpty()) {
            service = Settings.getInstance().getDefaultProvider();
        }
    	Map<String, String> response = new HashMap<>();
        Session session = this.getSession(request);
        return session.getUsername(service);
    }
    
    @GET
    @Path("preference/{service}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPreference(@Context HttpServletRequest request,
    		@PathParam("service") String service) {
    	String uname = this.getUsername(request, service);
        Document preference= UserPreferenceCache.getInstance().getPreference(service, uname);        
        return preference.toJson();
    }
    
    @PUT
    @Path("preference/{service}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void addPreference(@Context HttpServletRequest request,
    		@PathParam("service") String service, String pref) {
    	Gson gson = new Gson();
        Map<String, Object> prefMap = new HashMap<String, Object>();
        prefMap = (Map<String, Object>)gson.fromJson(pref, prefMap.getClass());
        String uname = this.getUsername(request, service);
        UserPreferenceCache.getInstance().addUpdatePreference(service, uname, prefMap); 
    }

	@GET
	@Path("oauth/callback/{provider}")
	public Response authorize(@Context HttpServletRequest req,
							  @Context HttpServletResponse res,
							  @PathParam("provider") String provider) throws IOException {
		// Extract token from state parameter (i.e. redirect URL)
		try {
			Session session = new Session(req);
			OAuthService.doTokenRequest(req, session, provider);
			return Response.seeOther(OAuthService.getAfterAuthRedirect(req)).build();
		} catch (OAuthProblemException e) {
			e.printStackTrace();
			URI redirect = UriBuilder.fromUri(req.getContextPath()).path("login-failed.html").build();
			return Response.seeOther(redirect).build();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return Response.serverError().build();
	}

	private Session getSession(HttpServletRequest request) {
		return new Session(request);
	}

	private Session getSessionWithAccessTokenOrSendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Session session = getSession(request);
		if(!session.hasAccessToken(Settings.getInstance().getDefaultProvider())) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Session requires a valid access token.");
			return null;
		} else {
			return session;
		}
	}
}
