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
 * File      : WoliWebServlet.java
 * Classname : WoliWebServlet
 * Author    : Duco Dokter, Wietze Helmantel
 * Created   : Sun Jan 30 14:46:19 2005
 * Version   : $Revision: 1.2 $
 * Copyright : Wyldebeast & Wunderliebe
 */

package com.w20e.socrates.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.w20e.socrates.model.Model;
import com.w20e.socrates.model.Submission;
import com.w20e.socrates.model.SubmissionImpl;
import com.w20e.socrates.process.RunnerContextImpl;
import com.w20e.socrates.submission.HandlerManager;

/**
 * Servlet for Ajax style validation.
 */
public class SessionStorageServlet extends HttpServlet {

    /**
     * Make serializable.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this class' logging.
     */
    private static final Logger LOGGER = Logger
            .getLogger(SessionStorageServlet.class.getName());

    /**
     * The init method creates an instance of the Socrates class, and allocates
     * initial resources. This includes compiling of XSL style sheets and
     * allocation of the database connections.
     * 
     * @param c
     *            Servlet configuration
     * @throws ServletException
     *             when the servlet fails.
     */
    public final void init(final ServletConfig c) throws ServletException {

        super.init(c);

        LOGGER.info("Initializing the session storage servlet");
    }

    /**
     * Do the thing...
     * @param req
     *            The request
     * @param res
     *            The response
     * @throws IOException
     *             when some io error occurs
     * @throws ServletException
     *             when the servlet fails
     */
    public final void doPost(final HttpServletRequest req,
            final HttpServletResponse res) throws IOException, ServletException {

        // Always use UTF!
        res.setContentType("text/xml;charset=UTF-8");

        // P3P header necessary for IE cookie policy
        res.addHeader("P3P", "CP=\"CAO DSP COR CURa ADMa DEVa OUR IND PHY ONL "
                + "UNI COM NAV INT DEM PRE\"");
        res.addHeader("Expires", "-1");
        res.addHeader("Cache-Control", "no-cache");
        res.addHeader("Pragma", "No-Cache");

        // We might as well return...
        if (req.getSession(false) == null) {

            // return error xml.
            res.getOutputStream()
                    .print("<validation>Nasty Error!</validation>");
            res.getOutputStream().flush();
            return;
        }

        HttpSession session = req.getSession();

        try {
            WebsurveyContext wwCtx = (WebsurveyContext) session
                    .getAttribute("runnerCtx");

            RunnerContextImpl ctx = (RunnerContextImpl) wwCtx
                    .getRunnerContext();

			Model model = ctx.getModel();
			Submission submission = new SubmissionImpl();
			
			submission.setAction(new URI(model.getSubmission().getAction() + "/stored_sessions/"));
			
			// Copy metadata from model into instance
			ctx.getInstance().getMetaData().putAll(model.getMetaData());
			ctx.getInstance().getMetaData().put("storage-type", "stored");
			
			HandlerManager.getInstance().submit(
					ctx.getInstance(),
					model,
					submission);
		} catch (Exception e) {
			LOGGER.severe("Couldn't submit instance for temporary storage");
		}
    }

            
            /**
     * Just forward to doPost.
     * 
     * @param req
     *            The request
     * @param res
     *            The response
     * @throws IOException
     *             when some io error occurs
     * @throws ServletException
     *             when the servlet fails
     */
    public final void doGet(final HttpServletRequest req,
            final HttpServletResponse res) throws IOException, ServletException {

        doPost(req, res);
    }

}
