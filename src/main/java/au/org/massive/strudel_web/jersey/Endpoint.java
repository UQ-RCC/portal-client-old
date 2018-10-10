package au.org.massive.strudel_web.jersey;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.org.massive.strudel_web.Session;
import au.org.massive.strudel_web.Settings;

/**
 * Provides some convenience methods for api endpoints to get sessions that are appropriately authorised.
 *
 * @author jrigby
 * @modified hoangnguyen177
 */
public abstract class Endpoint {

    protected Session getSession(HttpServletRequest request) {
        return new Session(request);
    }

    protected Session getSessionWithAccessTokenOrSendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Session session = getSession(request);
        if (session != null) {
            if (!session.hasAccessToken(Settings.getInstance().getDefaultProvider())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Session requires a valid access token.");
                return null;
            } else {
                return session;
            }
        }
        return null;
    }
}
