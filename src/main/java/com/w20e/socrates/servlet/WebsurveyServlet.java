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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.configuration.Configuration;

import com.w20e.socrates.config.ConfigurationResource;
import com.w20e.socrates.data.Instance;
import com.w20e.socrates.data.Node;
import com.w20e.socrates.process.Runner;
import com.w20e.socrates.process.RunnerContext;
import com.w20e.socrates.process.RunnerContextImpl;
import com.w20e.socrates.process.RunnerFactoryImpl;
import com.w20e.socrates.process.UnsupportedMediumException;
import com.w20e.socrates.process.ValidationException;
import com.w20e.socrates.rendering.Control;
import com.w20e.socrates.rendering.RenderState;
import com.w20e.socrates.rendering.Renderable;
import com.w20e.socrates.submission.HandlerManager;
import com.w20e.socrates.submission.NoneSubmissionHandler;
import com.w20e.socrates.submission.SubmissionException;
import com.w20e.socrates.submission.XMLFileSubmissionHandler;
import com.w20e.socrates.util.LocaleUtility;
import com.w20e.socrates.workflow.ActionResultImpl;
import com.w20e.socrates.workflow.Failure;

/**
 * Servlet for WoliWeb questionnaire. The servlet takes just one initialization
 * parameter: the location of the questionnaire config files. This can be
 * specified by either adding a java environment variable woliweb.cfg.root to
 * the JRE arguments, or by specifying an initial parameter in the web.xml.
 * Default is <code>/var/lib/woliweb/etc</code>. The servlet contains some logic
 * to be able to go forwards and backwards, and, in case of browser 'back'
 * actions, picking up the correct state of the questionnaire. The WoliWeb
 * servlet expects a 'state' parameter always, indicating the unique state for
 * the questionnaire. This enables the questionnaire runner to reset it's state
 * if someone happens to jump back and forward by means of the browser buttons.
 * 
 * Necessities: 'stateId' parameter. If none, just go forward.
 * 
 * @todo This class is way to large. We should cut it up into little pieces...
 */
public class WebsurveyServlet extends HttpServlet {

    /**
     * Version ID.
     */
    private static final long serialVersionUID = -7126084126224585784L;

    /**
     * Formatting Dates.
     */
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
            "yyyyMMddHHmmss");

    /**
     * This servlet's session manager.
     */
    private SessionManager sessionMgr;

    /**
     * Initialize this class' logging.
     */
    private static final Logger LOGGER = Logger
            .getLogger(WebsurveyServlet.class.getName());

    /**
     * Hold the runner factory.
     */
    private RunnerFactoryImpl runnerFactory;

    /**
     * Hold config rootdir.
     */
    private String rootDir;

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

        LOGGER.info("Initializing the Websurvey servlet");

        this.sessionMgr = new SessionManager();

        // Adding sessionmanager to servlet context, so individual sessions can
        // reach their manager.
        //
        getServletContext().setAttribute("socrates.sessionmanager",
                this.sessionMgr);

        if (System.getProperty("socrates.cfg.root") != null) {
            this.rootDir = System.getProperty("socrates.cfg.root");
        } else if (c.getInitParameter("socrates.cfg.root") != null) {
            this.rootDir = c.getInitParameter("socrates.cfg.root");
        } else {
            this.rootDir = ".";
        }

        LOGGER.info("Setting config root to " + this.rootDir);

        this.runnerFactory = new RunnerFactoryImpl(this.rootDir);

        // Register handlers
        HandlerManager.getInstance().register("file",
                new XMLFileSubmissionHandler());
        HandlerManager.getInstance().register("none",
                new NoneSubmissionHandler());
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
        res.setContentType("text/html;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");
        
        // P3P header necessary for IE cookie policy
        res.addHeader("P3P", "CP='CAO PSA CONi OUR DEM ONL'");

        // Thou shalst not cache...
        res.addHeader("Expires", "-1");
        res.addHeader("Cache-Control", "no-cache");
        res.addHeader("Pragma", "No-Cache");

        HttpSession session = this.sessionMgr.getSession(req);

        // If we don't have a session now, we might as well call it a day...
        if (session == null) {

            if (ServletHelper.getCookie(req, "JSESSIONID") != null) {
                LOGGER.warning("Session timeout");
                res.sendRedirect("session-timeout.html");
                res.getOutputStream().flush();
                return;
            } else {
                LOGGER.severe("No session created");
                res.sendRedirect("session-creation-error.html");
                res.getOutputStream().flush();
                return;
            }
        }

        // Hold all enable/disable options
        //
        Map<String, String> options = ServletHelper.determineOptions(req);

        // If no runner yet for this session, create one. We should have
        // startup param's for the runner, like the questionnaire to run, and
        // the locale. If these are not available, all fails.
        //
        if (session.getAttribute("runnerCtx") == null) {
            
            LOGGER.finer("Session instantiated with id " + session.getId());
            LOGGER.fine("No runner context available in session; creating one");
          
            if (req.getParameter("id") == null) {
                LOGGER.severe("No id parameter in request");
                try {
                    res.sendRedirect("session-creation-error.html");
                    this.sessionMgr.invalidateSession(req);
                    res.getOutputStream().flush();
                } catch (IOException e) {
                    LOGGER.severe("Couldn't even send error message..."
                            + e.getMessage());
                }
                return;
            }

            if (!initializeRunner(req, res, session, options)) {
                LOGGER.severe("Could not create runner context. Bye for now.");
                return;
            }
        }

        // Okido, by now we should have a session, and a valid runner context
        // stored in the session.
        //
        try {
            WebsurveyContext wwCtx = (WebsurveyContext) session
                    .getAttribute("runnerCtx");

            // Now let's see whether this session was deserialized.
            //
            if (wwCtx.isInvalid()) {
                LOGGER.info("Serialized session found!");
                // Re-create the context, and attach to WoliWeb context.
                LOGGER.finer("Model id: " + wwCtx.getModelId());
                LOGGER.finer("State id: " + wwCtx.getStateId());
                LOGGER.finer("Locale: " + wwCtx.getLocale());

                URI qUri = QuestionnaireURIFactory.getInstance().determineURI(
                        this.rootDir, wwCtx.getModelId(), wwCtx.getLocale());

                RunnerContextImpl ctx = this.runnerFactory.createContext(qUri,
                        null);
                ctx.setLocale(wwCtx.getLocale());
                ctx.setQuestionnaireId(qUri);
                ctx.getStateManager().setStateById(wwCtx.getStateId());
                ctx.setInstance(wwCtx.getInstance());
                wwCtx.setRunnerContext(ctx);
            }

            RunnerContextImpl ctx = (RunnerContextImpl) wwCtx
                    .getRunnerContext();

            LOGGER.finer("Session id " + session.getId());
            LOGGER.finer("Context id "
                    + ctx.getInstance().getMetaData().get("key"));

            // Add specific options
            // @todo This should move to the runner creation options.
            if (ctx.getProperty("renderOptions") == null) {
                ctx.setProperty("renderOptions", options);
            } else {
                ((Map<String, String>) ctx.getProperty("renderOptions")).putAll(options);
            }

            Map<String, Object> params = ParameterParser.parseParams(req);
            
            ctx.setData(params);

            // Do we have initial data already?
            if ("true".equals(options.get("enable_preload_params"))) {
                Node node;
                for (String key: params.keySet()) {
                    node = ctx.getInstance().getNode(key);
                    if (node != null) {
                        LOGGER.fine("Preloading node value " + params.get(key) + " for node " + node.getName());
                        node.setValue(params.get(key));
                    }
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            ctx.setOutputStream(output);

            // @todo: I really don't see why we should re-create the runner for
            // every post. Actually, the factory holds a reference to existing
            // runners, so it is not really bad, but I reckon the context should
            // hold the runner?
            //
            URI qUri = QuestionnaireURIFactory.getInstance().determineURI(
                    this.rootDir, wwCtx.getModelId(), wwCtx.getLocale());

            Runner runner = this.runnerFactory.createRunner(qUri);

            if (req.getParameter("previous") == null) {
                Map<String, Object> meta = ctx.getInstance().getMetaData();
                meta.put("time_" + req.getParameter("stateId"), new Date());
            }

            // Always store stateId in instance, for retrieval of state after
            // serialization.
            //
            if (req.getParameter("stateId") != null) {
                LOGGER.fine("Setting state id to " + req.getParameter("stateId"));
                ctx.getInstance().getMetaData().put("stateId",
                        req.getParameter("stateId"));
                if (!ctx.getStateManager().setStateById(req.getParameter("stateId"))) {
                    LOGGER.warning("Couldn't set stateId to " + req.getParameter("stateId"));
                }
            }

            // Go two states back if 'previous' request, and simply execute
            // 'next'.
            if (req.getParameter("previous") != null) {
                ctx.getStateManager().previous();
                RenderState state = ctx.getStateManager().previous();

                LOGGER.finest("Fill data from instance");

                ctx.setProperty("previous", "true");

                if (state != null) {

                    // Make sure to fill in existing data, otherwise we'll get
                    // an error
                    //
                    for (Iterator<Renderable> i = state.getItems().iterator(); i
                            .hasNext();) {
                        Renderable r = i.next();
                        if (r instanceof Control) {
                            String name = ((Control) r).getBind();
                            params.put(name, ctx.getInstance().getNode(name)
                                    .getValue());
                            LOGGER.finest("Set node " + name + " to "
                                    + params.get(name));
                        }
                    }
                }
            } else {
                ctx.setProperty("previous", "false");
            }

            next(ctx, runner);

            LOGGER.fine("Are we stored yet? "
                    + ctx.getInstance().getMetaData().get("storage-type"));

            // If we submitted, destroy long session
            if ("submit".equals(ctx.getInstance().getMetaData().get(
                    "storage-type"))) {
                LOGGER.fine("Invalidating long session");

                String surveyId = ServletHelper.generateSurveyId(ctx
                        .getInstance().getMetaData().get("qId").toString(), ctx
                        .getLocale().toString());

                this.sessionMgr.invalidateLongSession(surveyId, req, res);
            }

            // If this was the last action, destroy session.
            if (!runner.hasNext(ctx)) {
                this.sessionMgr.invalidateSession(req);
            }

            res.getOutputStream().write(output.toByteArray());
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

    /**
     * Get next page.
     * 
     * @param ctx
     *            a runner context value
     * @param runner
     *            runner to use
     * @exception Exception
     *                if an error occurs
     */
    private void next(final RunnerContext ctx, final Runner runner)
            throws Exception {

        LOGGER.finest("Next state asked");

        // get next action till we receive the wait status. This
        // indicates that something is hanging out for user input.
        //
        while (runner.hasNext(ctx)) {

            LOGGER.finest("Doing next thing");
            LOGGER.fine("Current action before next call: " + ctx.getCurrentAction());
            runner.next(ctx);
            LOGGER.fine("Current action after next call: " + ctx.getCurrentAction());
            LOGGER.finest("Last result: " + ctx.getResult());

            // Failure may be due to validation, in which case we should
            // provide an error message, or due to a submission error, in which
            // case we're unhappy...
            //
            if (ActionResultImpl.FAIL.equals(ctx.getResult().toString())) {

                // Is it the data?
                Exception e = ((Failure) ctx.getResult()).getException();

                if (e != null && e instanceof ValidationException) {

                    for (Iterator<Entry<String, Exception>> i = ((ValidationException) e)
                            .getErrors().entrySet().iterator(); i.hasNext();) {
                        LOGGER.fine("Error: " + i.next());
                    }
                } else if (e != null && e instanceof SubmissionException) {
                    LOGGER.log(Level.SEVERE, "SubmissionException"
                            + e.getMessage());
                } else {
                    LOGGER.info("Failure in workflow (usually expected)");
                    if (e != null) {
                        LOGGER.info(e.getMessage());
                    }
                }
            } else if (ActionResultImpl.WAIT.equals(ctx.getResult().toString())) {
                break;
            }
        }
    }

    /**
     * Initialize the runner for a given questionnaire.
     * 
     * @param req
     * @param res
     * @param session
     */
    private boolean initializeRunner(HttpServletRequest req,
            HttpServletResponse res, HttpSession session,
            Map<String, String> options) {

        String id = req.getParameter("id");
        
        LOGGER.finest("Parameter id is " + id);
        LOGGER.finest("Parameter locale is " + req.getParameter("locale"));
        Locale locale = LocaleUtility.getLocale(req.getParameter("locale"),
                true);
        LOGGER.finest("Using locale " + locale);

        // Unique id for survey, defined by id and locale
        //
        String surveyId = ServletHelper.generateSurveyId(id, locale.toString());

        URI qUri = QuestionnaireURIFactory.getInstance().determineURI(
                this.rootDir, id, locale);

        /**
         * Get global config.
         */
        Configuration cfg = null;

        try {
            cfg = ConfigurationResource.getInstance().getConfiguration(
                    qUri.toURL());
        } catch (Exception e1) {
            return false;
        }

        LOGGER.fine("Using config URI " + qUri.toString());

        try {
            RunnerContextImpl ctx = this.runnerFactory.createContext(qUri,
                    options);
            ctx.setLocale(locale);
            ctx.setQuestionnaireId(qUri);

            /*
             * We may need to reread an existing data set. We do this if the
             * request didn't explicitly forbid it, and we do have either an
             * existing session or a stored instance file.
             */
            if ("true".equals(cfg.getString("enablelongsessions", "true"))) {

                LOGGER.info("Has long session? "
                        + this.sessionMgr.hasLongSession(req, surveyId));

                if (this.sessionMgr.hasLongSession(req, surveyId)
                        && !"true".equals(options.get("disable_reload"))) {

                    Instance inst = this.sessionMgr.salvageInstance(surveyId,
                            req, ctx);

                    if (inst != null) {
                        ctx.setInstance(inst);
                        LOGGER.fine("Setting state to "
                                + (String) inst.getMetaData().get("stateId"));
                        ctx.getStateManager().setStateById(
                                (String) inst.getMetaData().get("stateId"));
                    } else {
                        LOGGER.warning("Unable to restore instance");
                    }
                }
            }

            Map<String, Object> meta = ctx.getInstance().getMetaData();

            meta.put("qId", id);
            meta.put("locale", locale);

            ServletHelper.setMetaData(req, meta);

            // Store runner context in session
            //
            session.setAttribute("runnerCtx", new WebsurveyContext(ctx, id,
                    locale));

            // Output filename. If unset, default to overwritable file.
            //
            if (!meta.containsKey("filename") || meta.get("filename") == null) {

                meta.put("filename",
                        id
                                + (ctx.getModel().getMetaData().containsKey(
                                        "Version") ? "-"
                                        + ctx.getModel().getMetaData().get(
                                                "Version") : "")
                                + "_"
                                + locale
                                + "_"
                                + WebsurveyServlet.FORMATTER.format(Calendar
                                        .getInstance().getTime()) + "_"
                                + meta.get("key"));
            }

            if ("true".equals(cfg.getString("enablelongsessions", "true"))) {

                // Finally, add cookie that holds info on user data, if we
                // don't
                // already have it, and set output
                // file name.
                //
                if (!this.sessionMgr.hasLongSession(req, surveyId)) {
                    this.sessionMgr.createLongLivedSession(surveyId, meta.get(
                            "filename").toString()
                            + "||" + session.getId(), res);
                }
            }
        } catch (UnsupportedMediumException e) {

            this.sessionMgr.invalidateSession(req);
            LOGGER.log(Level.SEVERE, "Error in creating runner context", e);
            return false;
        }

        return true;
    }
}
