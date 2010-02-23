package com.w20e.socrates.servlet;

import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class ServletHelper {

    /**
     * Initialize this class' logging.
     */
    private static final Logger LOGGER = Logger.getLogger(ServletHelper.class
            .getName());

    /**
     * Fetch referer from headers, or null if unavailable.
     * 
     * @param req
     * @return
     */
    // public static URI storeReferer(final HttpServletRequest req) {
    //
    // }

    /**
     * Get user agent from request. This will yield a string of the form
     * agent[/version].
     * 
     * @param req
     * @return agent and optionally version.
     */
    public static String determineUserAgent(final HttpServletRequest req) {

        String agent = req.getHeader("User-Agent");

        if (agent == null) {
            return "";
        }

        return agent.split(" ")[0];
    }

    /**
     * Determine options used for specific client request. This will collect all
     * enable_ or disable_ options.
     * 
     * @param req
     * @return Map holding all options and their values.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> determineOptions(final HttpServletRequest req) {

        Map<String, String> options = new HashMap<String, String>();
        String val, key;

        for (Enumeration<String> e = req.getParameterNames(); e
                .hasMoreElements();) {
            key = (String) e.nextElement();

            if (key.startsWith("enable_") || key.startsWith("disable_")) {

                val = Boolean.valueOf(req.getParameter(key)).toString();

                LOGGER.finest("Option " + key + " set to " + val);

                options.put(key, val);
            }
        }

        return options;
    }

    /**
     * Extract regkey from query string.
     * 
     * @param qry
     *            query string from URL
     * @return regkey or null
     */
    public static String extractRegkey(final String qry) {

        String[] args = qry.split("&");

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("regkey")) {
                if (args[i].split("=").length > 1) {
                    return args[i].split("=")[1];
                }
            }
        }

        return null;
    }

    public static void setMetaData(final HttpServletRequest req,
            final Map<String, Object> meta) {

        HttpSession session = req.getSession(false);

        meta.put("userAgent", ServletHelper.determineUserAgent(req));
        meta.put("remoteAddress", req.getRemoteAddr());

        URI refererUrl = null;

        if (req.getHeader("Referer") == null) {
            meta.put("referer", "UNKNOWN_REFERER");
        } else {
            try {
                // The getHeader method is case insensitive, so no worries
                // here...
                refererUrl = new URI(req.getHeader("Referer"));
                meta
                        .put("referer", refererUrl.getHost()
                                + refererUrl.getPath());

            } catch (Exception e) {
                // Set as raw
                meta.put("referer", "RAW: " + req.getHeader("Referer"));
            }
        }

        // Try to get the key from the referer, if it's there
        if (refererUrl != null) {
            try {
                String key = ServletHelper.extractRegkey(refererUrl.getQuery());

                if (key == null) {
                    if (req.getParameter("regkey") != null) {
                        meta.put("key", req.getParameter("regkey"));
                        meta.put("reminder", req.getParameter("regkey"));
                    } else {
                        meta.put("key", session.getId());
                    }
                } else {
                    meta.put("key", key);
                }
            } catch (Exception e) {
                meta.put("key", session.getId());
            }
        } else {
            meta.put("key", session.getId());
        }

        // Do not store...
        if ("false".equals(req.getParameter("enable_storage"))) {
            meta.put("storage-type", "none");
        }

    }

    /**
     * Generate unique survey id of id and locale.
     */
    public static String generateSurveyId(final String id, final String locale) {

        return id + "_" + locale;
    }

    /**
     * Retrieve cookie from request, if it's there...
     * @param id
     * @param req
     * @return
     */
    public static Cookie getCookie(final HttpServletRequest req, final String id) {

        if (req.getCookies() == null || id == null) {
            return null;
        }

        for (Cookie cookie : req.getCookies()) {
            if (id.equals(cookie.getName())) {
                return cookie;            }
        }

        return null;
    }
}
