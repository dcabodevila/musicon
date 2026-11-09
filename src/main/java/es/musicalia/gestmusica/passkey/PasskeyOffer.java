package es.musicalia.gestmusica.passkey;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * One-shot offer, after a password login, to create a passkey on the current device.
 * The login success handler marks the session; the next HTML page consumes the mark.
 */
public final class PasskeyOffer {

    public static final String SESSION_ATTRIBUTE = PasskeyOffer.class.getName() + ".PENDING";

    private PasskeyOffer() {
    }

    public static void mark(HttpServletRequest request) {
        request.getSession().setAttribute(SESSION_ATTRIBUTE, Boolean.TRUE);
    }

    /**
     * Returns whether the offer is pending and clears it, only for full HTML page requests
     * (AJAX calls made by a page must not swallow it).
     */
    public static boolean consume(HttpServletRequest request) {
        if (request == null || !"GET".equals(request.getMethod())) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (accept == null || !accept.contains("text/html")) {
            return false;
        }
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SESSION_ATTRIBUTE) == null) {
            return false;
        }
        session.removeAttribute(SESSION_ATTRIBUTE);
        return true;
    }
}
