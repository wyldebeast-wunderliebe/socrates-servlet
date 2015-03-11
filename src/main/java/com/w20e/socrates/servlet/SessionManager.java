/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * You should have received a copy of the GNU General Public License
 * (for example /usr/src/linux/COPYING); if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * File      : SessionManager.java
 * Classname :
 * Author    : Duco Dokter
 * Created   : Sun Jan 30 15:08:55 2005
 * Version   : $Revision: 1.1.1.1 $
 * Copyright : Wyldebeast & Wunderliebe
 * License   : GPL
 */

package com.w20e.socrates.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.w20e.socrates.data.Instance;
import com.w20e.socrates.model.util.InstanceXMLSerializer;
import com.w20e.socrates.process.RunnerContext;

/**
 * Basic implementation of session management using cookies. For the websurvey,
 * we don't cater for situations where cookies are not accepted.
 */
public class SessionManager {

	/**
	 * Hold number of seconds for week.
	 */
	private static final int WEEK = 604800;

	/**
	 * Initialize this class' logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(SessionManager.class
			.getName());

	/**
	 * Marker for invalid long sessions.
	 */
	public static final String LONG_SESSION_INVALID = "INVALID";

	/**
	 * Separator regex for long session id.
	 */
	public static final String LONG_SESSION_SEPARATOR = "\\|\\|";

	/**
	 * Hold references to existing sessions.
	 */
	private HashMap<String, HttpSession> sessionRefs = new HashMap<String, HttpSession>();

	/**
	 * Check for existence of a valid Session.
	 * 
	 * @param req
	 *            Request
	 * @return boolean indicating whether a Session exists.
	 */
	public final boolean hasSession(final HttpServletRequest req) {

		if (req.getSession(false) != null) {

			return true;
		}

		return false;
	}

	/**
	 * Create a fresh Session, destroying the old one if necessary.
	 * 
	 * @param req
	 *            Request
	 * @return a fresh Session.
	 */
	public final HttpSession createSession(final HttpServletRequest req) {

		if (hasSession(req) && req.isRequestedSessionIdValid()) {
			invalidateSession(req);
		}

		HttpSession session = req.getSession();
		session.setAttribute("runnerCtx", null);

		// Add reference of session to self
		storeSessionReference(session.getId(), session);

		return session;
	}

	/**
	 * Get the session for this request. Create if it's not there yet, but only
	 * if the 'id' parameter is available.
	 * 
	 * @param req
	 *            a <code>HttpServletRequest</code> value
	 * @return a <code>HttpSession</code> value
	 */
	public final HttpSession getSession(final HttpServletRequest req) {

		// No session yet and id parameter available?
		if ((!hasSession(req))
				&& (req.getParameter("id") != null || req
						.getParameter("regkey") != null)) {
			LOGGER.fine("Creating new session");
			return createSession(req);

		} else if (req.getParameter("id") != null
				|| req.getParameter("regkey") != null) {

			LOGGER.fine("Creating new session because of new id param");
			return createSession(req);
		}

		return req.getSession(false);
	}

	/**
	 * Create a cookie that will live for a week, thereby allowing a user to
	 * return to an existing instance.
	 * 
	 * @param filename
	 * @return
	 */
	public void createLongLivedSession(final String id, final String value,
			final HttpServletResponse res) {

		Cookie bastogne = new Cookie(id, value);

		bastogne.setMaxAge(SessionManager.WEEK);

		res.addCookie(bastogne);
	}

	/**
	 * Do we have a long session? This only returns true if the session exists,
	 * and is not invalid. It is invalid if it has value 'SUBMITTED'
	 * 
	 * @param req
	 * @return whether we have it or not...
	 */
	public boolean hasLongSession(final HttpServletRequest req,
			final String surveyId) {

		Cookie cookie = ServletHelper.getCookie(req, surveyId);

		if (cookie == null) {
			return false;
		}

		if (SessionManager.LONG_SESSION_INVALID.equals(cookie.getValue())) {
			return false;
		}

		return true;
	}

	/**
	 * Retrieve value of long session cookie.
	 * 
	 * @return
	 */
	private String getInstanceFileFromLongSession(final String surveyId,
			final HttpServletRequest req) {

		Cookie cookie = ServletHelper.getCookie(req, surveyId);

		if (cookie == null) {
			return null;
		}

		try {
			return cookie.getValue().split(
					SessionManager.LONG_SESSION_SEPARATOR)[0];
		} catch (Exception e) {
			LOGGER.severe("Couldn't extract instance file from cookie with value "
					+ cookie.getValue());
		}

		return null;
	}

	/**
	 * Invalidate user session after submit. This involves setting long session
	 * to invalid.
	 */
	public void invalidateSession(final HttpServletRequest req) {
		LOGGER.info("Destroying user session");

		try {
			destroySessionReference(req.getSession().getId());
			req.getSession().invalidate();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Couldn't invalidate session: " + e.getMessage());
		}
	}

	/**
	 * Invalidate user session after submit. This involves setting long session
	 * to invalid.
	 */
	public void invalidateLongSession(final String surveyId,
			final HttpServletRequest req, final HttpServletResponse res) {
		LOGGER.info("Destroying long session");

		Cookie bastogne = new Cookie(surveyId,
				SessionManager.LONG_SESSION_INVALID);
		bastogne.setMaxAge(0);

		res.addCookie(bastogne);
	}

	/**
	 * Store a reference to a session.
	 * 
	 * @param id
	 * @param session
	 */
	public void storeSessionReference(String id, HttpSession session) {

		this.sessionRefs.put(id, session);
	}

	/**
	 * Remove ref to session.
	 * 
	 * @param id
	 */
	public void destroySessionReference(String id) {

		this.sessionRefs.remove(id);
	}

	/**
	 * Fetch Instance from a session, or null if this is not possible.
	 * 
	 * @param id
	 * @return instance
	 */
	public Instance salvageInstanceFromRegkey(final String regKey,
			final HttpServletRequest req, final RunnerContext runnercontext) {

		WebsurveyContext ctx = null;

		try {
			ctx = (WebsurveyContext) this.sessionRefs.get(regKey).getAttribute(
					"runnerCtx");
			LOGGER.info("Found existing session in session manager.");

			destroySessionReference(regKey);

		} catch (Exception e) {
			LOGGER.info("No session left for id: " + regKey);
		}

		if (ctx != null) {
			return ctx.getInstance();
		}

		LOGGER.fine("Request for restoring instance file");

		try {
			String base = runnercontext.getModel().getSubmission().getAction()
					.getPath()
					+ "/stored_sessions/";

			if (base.startsWith("/.")) {
				base = base.substring(1);
			}

			File baseDir = new File(base);

			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.indexOf(regKey) > -1;
				}
			};

			File tgt = baseDir.listFiles(filter)[0];

			if (tgt.exists()) {
				try {
					return InstanceXMLSerializer.deserialize(new URI("file:"
							+ tgt.getAbsolutePath()));
				} catch (URISyntaxException e) {
					LOGGER.severe("Exception in resolving file. This is unusual...");
				}
			} else {
				LOGGER.severe("Instance file doesn't exist for session " + regKey + "! No data loaded");
			}
		} catch (Exception e) {
			LOGGER.warning("No instance file for this session.");
		}

		return null;
	}

	/**
	 * Fetch Instance from a session, or null if this is not possible.
	 * 
	 * @param id
	 * @return instance
	 */
	public Instance salvageInstance(final String surveyId,
			final HttpServletRequest req, final RunnerContext runnercontext) {

		final String instancefile = getInstanceFileFromLongSession(surveyId,
				req);
		final String id = getSessionIdFromLongSession(surveyId, req);

		if (instancefile == null || id == null) {
			LOGGER.severe("No instancefile nor original session id found in long session");
			return null;
		}

		WebsurveyContext ctx = null;

		try {
			ctx = (WebsurveyContext) this.sessionRefs.get(id).getAttribute(
					"runnerCtx");
			LOGGER.info("Found existing session in session manager.");

			destroySessionReference(id);

		} catch (Exception e) {
			LOGGER.info("No session left for id: " + id);
		}

		if (ctx != null) {
			return ctx.getInstance();
		}

		LOGGER.fine("Request for restoring instance file");

		if (instancefile != null) {

			try {
				String base = runnercontext.getModel().getSubmission()
						.getAction().getPath();

				if (base.startsWith("/.")) {
					base = base.substring(1);
				}

				String file = base + File.separator + instancefile + ".xml";

				LOGGER.fine("Restoring from " + file);

				if (new File(file).exists()) {
					try {
						return InstanceXMLSerializer.deserialize(new URI(
								"file:" + file));
					} catch (URISyntaxException e) {
						LOGGER.severe("Exception in resolving file. This is unusual...");
					}
				} else {
					LOGGER.severe("Instance file doesn't exist! No data loaded");
				}
			} catch (Exception e) {
				LOGGER.warning("No instance file for this session. This may or may not be a problem...");
			}
		}

		return null;
	}

	/*
	 * Retrieve session id of original session from long session, or return
	 * null;
	 */
	private String getSessionIdFromLongSession(final String surveyId,
			final HttpServletRequest req) {

		final Cookie cookie = ServletHelper.getCookie(req, surveyId);

		if (cookie == null) {
			return null;
		}

		try {
			return cookie.getValue().split(
					SessionManager.LONG_SESSION_SEPARATOR)[1];
		} catch (Exception e) {
			LOGGER.severe("Couldn't extract session id from cookie with value "
					+ cookie.getValue());
		}

		return null;
	}

	public int getNrOfSessionRefs() {

		return this.sessionRefs.size();
	}
}
