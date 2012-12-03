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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.w20e.socrates.process.DataHandler;
import com.w20e.socrates.process.RunnerContextImpl;
import com.w20e.socrates.process.ValidationException;
import com.w20e.socrates.rendering.Control;
import com.w20e.socrates.rendering.Group;
import com.w20e.socrates.rendering.RenderStateImpl;
import com.w20e.socrates.rendering.Renderable;

/**
 * Servlet for Ajax style validation.
 */
public class WebsurveyAjaxValidateServlet extends HttpServlet {

    /**
     * Make serializable.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this class' logging.
     */
    private static final Logger LOGGER = Logger
            .getLogger(WebsurveyAjaxValidateServlet.class.getName());

    /**
     * XML-RPX formatter
     */
    private XMLRPCFormatter formatter;

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

        LOGGER.info("Initializing the Websurvey Ajax validation servlet");
        this.formatter = new XMLRPCFormatter();
    }

    /**
     * Do the thing... If there is no runner (context) in the session, create a
     * new session based on the given id parameter. If there is also no id
     * parameter, it's an error. If the id parameter is given, create a new
     * runner context anyway. If a parameter called regkey is given, this
     * parameter is used for storage of the instance. This way, a user may
     * provide it's own key.
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

        LOGGER.fine("Incoming request, session id validity is \""
                + req.isRequestedSessionIdValid() + "\"");

        HttpSession session = req.getSession();
        LOGGER.fine("Session found with id " + session.getId());

        try {
            WebsurveyContext wwCtx = (WebsurveyContext) session
                    .getAttribute("runnerCtx");

            RunnerContextImpl ctx = (RunnerContextImpl) wwCtx
                    .getRunnerContext();

            LOGGER.finer("Context id "
                    + ctx.getInstance().getMetaData().get("key"));

            // Add all http params and output to runner's context. Sadly
            // for http params we need a hack, since the getParameterMap
            // method seems f**ked up.
            //
            Map<String, Object> params = ParameterParser.parseParams(req);

            Map<String, Map<String, String>> before = new HashMap<String, Map<String, String>>();
            Map<String, Map<String, String>> after = new HashMap<String, Map<String, String>>();
            Map<String, Map<String, String>> filtered = new HashMap<String, Map<String, String>>();
            
            ValidatorHelper.getRenderableProperties(ctx.getStateManager().current().getItems(), before, ctx);
            
            Collection<Renderable> found = new ArrayList<Renderable>();
            
            findRenderables(params.keySet(), ctx.getStateManager().current().getItems(), found);
            
            // Here we only set the data of the parameters we actually have...
            RenderStateImpl tmpState = new RenderStateImpl("TMP", found);
            
            try {
                DataHandler.setData(params, ctx.getModel(), ctx.getInstance(), tmpState);
            } catch (ValidationException ve) {
                // Not a problem here.
            }

            ValidatorHelper.getRenderableProperties(ctx.getStateManager().current().getItems(), after, ctx);

            for (String rItem: before.keySet()) {
                
                Map<String, String> localProps = new HashMap<String, String>();
                
                for (String key: before.get(rItem).keySet()) {
                    try {
                        LOGGER.fine("Before '" + rItem + "', " + key + ": " + before.get(rItem).get(key));
                        LOGGER.fine("After '" + rItem + "', " + key + ": " + after.get(rItem).get(key));
                        
                        if (!before.get(rItem).get(key).equals(after.get(rItem).get(key))) {
                            LOGGER.fine("Adding to filtered: " + key);
                            localProps.put(key, after.get(rItem).get(key));
                        }
                    } catch (Exception e) {
                        LOGGER.severe("Missing value in after properties for " + rItem + "; " + key + ": " + e.getMessage());
                    }
                }

                if (localProps.keySet().size() > 0) {
                    LOGGER.fine("Adding to filtered: " + rItem);
                    filtered.put(rItem, localProps);
                }
            }

            OutputStream out = res.getOutputStream();

            ctx.setOutputStream(out);
            
            this.formatter.format(filtered, out, ctx);

            res.getOutputStream().flush();

            // free resources...
            ctx.setOutputStream(null);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No runner created", e);
            throw new ServletException("Runner could not be created: "
                    + e.getMessage());
        }
    }

    
    /**
     * Recursively find renderables that are listed in keys.
     * @param keys
     * @param src
     * @param found
     */
    private void findRenderables(Set<String> keys, Collection<Renderable> src, Collection<Renderable> found) {
        
        for (Renderable r : src) {
            if (r instanceof Group) {
                findRenderables(keys, ((Group) r).getItems(), found);
            } else if (r instanceof Control) {
                if (keys.contains(r.getId())) {
                    found.add(r);
                }
            }
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
