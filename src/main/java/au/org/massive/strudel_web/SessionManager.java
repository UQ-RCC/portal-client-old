package au.org.massive.strudel_web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A session listener that cleans up any leftovers when a session ends, and keeps track of all active sessions
 *
 * @author jrigby
 */
public class SessionManager implements HttpSessionListener {


    private static final Map<String, HttpSession> sessionMap = new HashMap<>();

    public static HttpSession getSessionById(String id) {
        return sessionMap.get(id);
    }

    public static void endSession(String id) {
        HttpSession session = getSessionById(id);
        if (session != null) {
            endSession(session);
        }
    }

    public static void endSession(Session session) {
        endSession(session.getHttpSession());
    }

    public static void endSession(HttpSession session) {
        session.invalidate();
    }

    public static Set<Session> getActiveSessions() {

        Set<Session> activeSessions = new HashSet<>();
        for (HttpSession s : sessionMap.values()) {
            try {
                s.getCreationTime();
                activeSessions.add(new Session(s));
            } catch (IllegalStateException e) {
                // Don't add it; it's inactive
            }
        }
        return activeSessions;
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
    	// load access token, refresh token from database
    	//
        sessionMap.put(event.getSession().getId(), event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Session s = new Session(event.getSession());
        sessionMap.remove(s.getSessionId());
    }

}
