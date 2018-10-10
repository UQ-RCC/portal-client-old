package au.org.massive.strudel_web;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.jwt.JWT;
import org.apache.oltu.oauth2.jwt.io.JWTReader;

import au.org.massive.strudel_web.cache.OauthToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Provides methods for the OAuth2 auth flow
 *
 * @author jrigby
 */
public class OAuthService {

    private static final Settings settings = Settings.getInstance();
    private final static Logger logger = LogManager.getLogger(OAuthService.class);
    
    private OAuthService() {

    }

    /**
     * Generates an authorization code request
     *
     * @param afterAuthRedirect {@link URI} to be redirected to after authorisation
     * @return an authorization code request
     * @throws OAuthSystemException thrown if errors with with the oauth system occur
     */
    public static OAuthClientRequest getAuthCodeRequest(URI afterAuthRedirect, String authServiceId) throws OAuthSystemException {
        String oauthAuthorizationEndpoint = settings.getOidConfigValue(authServiceId, "oidc-endpoint");
        String oauthClientId = settings.getOidConfigValue(authServiceId, "oidc-client-id");
        String oauthRedirect = settings.getOidConfigValue(authServiceId, "oidc-redirect");
    	return OAuthClientRequest.authorizationLocation(oauthAuthorizationEndpoint)
                .setClientId(oauthClientId)
                .setResponseType("code")
                .setRedirectURI(oauthRedirect)
                .setState(afterAuthRedirect.toString())
                .buildQueryMessage();
    }

    /**
     * Performs the auth code redirect
     *
     * @param response {@link HttpServletResponse} object from a servlet
     * @param session the current http session
     * @param afterAuthRedirect {@link URI} to be redirected to after authorisation
     * @throws OAuthSystemException thrown if errors with with the oauth system occur
     */
    public static void doAuthCodeRedirect(HttpServletResponse response, Session session, URI afterAuthRedirect, String authServiceId) throws OAuthSystemException {
        OAuthClientRequest oauthReq = OAuthService.getAuthCodeRequest(afterAuthRedirect, authServiceId);
        try {
        	logger.info("oauth req:" + URI.create(oauthReq.getLocationUri()).toString());
            response.sendRedirect(URI.create(oauthReq.getLocationUri()).toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the auth code from the request
     *
     * @param req {@link HttpServletRequest} object from a servlet
     * @return the auth code
     * @throws OAuthProblemException thrown if there are errors with the OAuth request
     */
    public static String getAuthCodeFromRequest(HttpServletRequest req) throws OAuthProblemException {
    	return OAuthAuthzResponse.oauthCodeAuthzResponse(req).getCode();
    }

    /**
     * Extracts the location to redirect after successful auth using the state parameter of the
     * OAuth request
     *
     * @param req {@link HttpServletRequest} object from a servlet
     * @return the redirect URI
     * @throws OAuthProblemException thrown if there are errors with the OAuth request
     */
    public static URI getAfterAuthRedirect(HttpServletRequest req) throws OAuthProblemException {
        return URI.create(OAuthAuthzResponse.oauthCodeAuthzResponse(req).getState());
    }

    /**
     * Requests an access token
     *
     * @param req {@link HttpServletRequest} object from a servlet
     * @param session the current http session
     * @throws Exception 
     */
    public static void doTokenRequest(HttpServletRequest req, Session session, String provider) throws Exception {
        doTokenRequest(getAuthCodeFromRequest(req), session, provider);
    }

    /**
     * Requests an access token
     *
     * @param code the authorisation code
     * @param session the current http session
     * @throws Exception 
     */
    public static void doTokenRequest(String code, Session session, String provider) throws Exception {
    	logger.info("doTokenRequest for " + provider);
    	String oauthTokenEndpoint = settings.getOidConfigValue(provider, "oidc-token-endpoint");
    	String oauthRedirect = settings.getOidConfigValue(provider, "oidc-redirect");
        String oauthClientId = settings.getOidConfigValue(provider, "oidc-client-id");
        String oauthClientSecret = settings.getOidConfigValue(provider, "oidc-client-secret");
    	
    	String authHeader = "Basic " + new String(Base64.encodeBase64((oauthClientId + ":" + oauthClientSecret).getBytes()));
        OAuthClient client = new OAuthClient(new URLConnectionClient());
        OAuthClientRequest req = OAuthClientRequest.tokenLocation(oauthTokenEndpoint)
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setScope("openid")
                .setScope("offline_access")
                .setScope("email")
                .setScope("profile")
                .setScope("offline")
                .setRedirectURI(oauthRedirect)
                .setCode(code).buildBodyMessage();
        req.setHeader("Accept", "application/json");
        req.setHeader("Authorization", authHeader);        
        OAuthJSONAccessTokenResponse accessTokenResp = client.accessToken(req);
        String accessToken = accessTokenResp.getAccessToken();
        String refreshToken = accessTokenResp.getRefreshToken();
        logger.info("access token:" + accessToken);
        logger.info("refresh token:" + refreshToken);
        Long expiresIn = accessTokenResp.getExpiresIn();
        logger.info("Token expires in:::" + expiresIn);
        session.setAccessToken(provider, accessToken);
        session.setRefreshToken(provider, refreshToken);
        session.setExpiresIn(provider, expiresIn);    
        // if this is the auth and successful, load other tokens as well
        JWTReader jwtReader = new JWTReader();
        JWT jwtToken = jwtReader.read(accessToken);
        String email = jwtToken.getClaimsSet().getCustomField("email", String.class);
        String uid = jwtToken.getClaimsSet().getSubject();
        String uname = jwtToken.getClaimsSet().getCustomField("preferred_username", String.class);
        logger.info("JWT Fields");
        logger.info(jwtToken.getClaimsSet().toString());
        logger.info("JWT Custome Fields");
        logger.info(jwtToken.getClaimsSet().getCustomFields().toString());
        session.setUserID(provider, uid);
        session.setUsername(provider, uname);
        logger.info(session.getUserID(provider));
        if(settings.isDefaultOidcProvider(provider)) {
        	session.setUserEmail(email);
            Map<String, OauthToken> storedTokens = Settings.getInstance().getTokenCache().getUserTokens(email);
            for(String storedProvider: storedTokens.keySet()) {
            	session.setAccessToken(storedProvider, 
            			storedTokens.get(storedProvider).getAccessToken());
            	session.setRefreshToken(storedProvider,
            			storedTokens.get(storedProvider).getRefreshToken());
            	// by not setting Expires in, this access token loaded from database
            	// becomes invalid right away, forcing to get new one
            	session.setUserID(storedProvider,
            			storedTokens.get(storedProvider).getUid());
            }
        }
        else {
        	OauthToken _token = new OauthToken(accessToken, refreshToken, expiresIn, uid);
        	Settings.getInstance().getTokenCache().addToken(session.getUserEmail(), provider, _token);
        }
    }
    
    /**
     * Request new access token
     * @param session
     * @throws Exception 
     */
    public static void doRequestNewAccessToken(Session session, String provider) throws Exception {
    	logger.info("Request new accesstoken for:" + provider);
    	String oauthTokenEndpoint = settings.getOidConfigValue(provider, "oidc-token-endpoint");
    	String oauthClientId = settings.getOidConfigValue(provider, "oidc-client-id");
        String oauthClientSecret = settings.getOidConfigValue(provider, "oidc-client-secret");
    	
    	String authHeader = "Basic " + new String(Base64.encodeBase64(( oauthClientId + ":" 
    											+ oauthClientSecret).getBytes()));
        OAuthClient client = new OAuthClient(new URLConnectionClient());
        OAuthClientRequest req = OAuthClientRequest.tokenLocation(oauthTokenEndpoint)
                .setGrantType(GrantType.REFRESH_TOKEN)
                .setRefreshToken(session.getRefreshToken(provider))
                .buildBodyMessage();
        req.setHeader("Accept", "application/json");
        req.setHeader("Authorization", authHeader);
        OAuthJSONAccessTokenResponse accessTokenResp = client.accessToken(req);
        String accessToken = accessTokenResp.getAccessToken();
        String refreshToken = accessTokenResp.getRefreshToken();
        Long expiresIn = accessTokenResp.getExpiresIn();
        JWTReader jwtReader = new JWTReader();
        JWT jwtToken = jwtReader.read(accessToken);
        String uid = jwtToken.getClaimsSet().getSubject();
        session.setAccessToken(provider, accessToken);
        session.setRefreshToken(provider, refreshToken);
        session.setExpiresIn(provider, expiresIn);
        session.setUserID(provider, uid);
        logger.info("access token:" + accessToken);
        logger.info("refresh token:" + refreshToken);
        if(!settings.isDefaultOidcProvider(provider)) {
        	OauthToken _token = new OauthToken(accessToken, refreshToken, expiresIn, uid);
        	Settings.getInstance().getTokenCache().addToken(session.getUserEmail(), provider, _token);
        }
        
    }
    

}
