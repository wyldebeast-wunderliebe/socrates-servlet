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
 * File      : SessionTimeoutNotifier.java
 * Classname : SessionTimeoutNotifier
 * Author    : Wietze Helmantel
 * Date      : Mar 22, 2005
 * Version   : $Revision: 1.1.1.1 $
 * Copyright : Wyldebeast & Wunderliebe
 */

package com.w20e.socrates.servlet;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.w20e.socrates.data.Instance;
import com.w20e.socrates.data.Node;
import com.w20e.socrates.submission.HandlerManager;

/**
 * Use this class to create a listener to http sessions. This is especially
 * useful when destroying woliweb sessions. We also need to store them even if
 * not fully filled.
 * 
 * @author Wietze Helmantel
 */
public final class SessionTimeoutNotifier implements HttpSessionListener {

    /**
     * Initialize this class' logging.
     */
    private static final Logger LOGGER = Logger
            .getLogger(SessionTimeoutNotifier.class.getName());

    /**
     * Do something upon session initiation.
     * 
     * @param event
     *            The binding event
     */
    public void sessionCreated(final HttpSessionEvent event) {

        LOGGER.info("The session has started: " + event.getSession().getId());

        /*
         * Nothing yet
         */
    }

    /**
     * Do some session cleanup. In this case we can use it to do some file
     * storage.
     * 
     * @param event
     *            The binding event
     */
    public void sessionDestroyed(final HttpSessionEvent event) {

        LOGGER.info("Cleanup session " + event.getSession().getId());

        // Whatever happens, we need to destroy the session managers reference...
        // Otherwise, we create a nasty garbage collection problem.
        //
        try {
            SessionManager mgr = (SessionManager) event.getSession()
                    .getServletContext()
                    .getAttribute("socrates.sessionmanager");
            if (mgr != null) {
                LOGGER.info("Session manager holds " + mgr.getNrOfSessionRefs() + " sessions");
                
                mgr.destroySessionReference(event.getSession().getId());
            } else {
                LOGGER
                        .severe("No sessionmanager in global context. This is BAD(tm)");
            }
        } catch (Exception e) {
            LOGGER
                    .severe("Error in retrieving session manager. This is BAD(tm)");
        }

        WebsurveyContext ctx = null;

        try {
            ctx = (WebsurveyContext) event.getSession().getAttribute(
                    "runnerCtx");
        } catch (Exception e) {
        }

        // If the context is null, we can really return, otherwise clean up mess.
        if (ctx == null) {
            LOGGER.severe("No runner context could be retrieved");
            return;
        }

        Instance inst = ctx.getInstance();
        
        if (inst == null) {
            LOGGER.severe("Runner context has no instance. This is not good");
            return;
        }
        
        if (inst.getMetaData().get("stored") != null) {
            LOGGER.info("Instance data already stored for this session.");
            return;
        }

        if ("none".equals(inst.getMetaData().get("storage-type"))) {
            LOGGER.info("Instance data not to be stored.");
            return;
        }

        inst.getMetaData().put("storage-type", "timeout");

        try {
            // Do we need to submit any data at all?
            if (ctx.getSubmission() != null
                    && ctx.getSubmission().getAction() != null) {
                LOGGER.info("Storing timed out session");

                if (hasData(inst)) {

                    HandlerManager.getInstance().submit(inst,
                            ctx.getRunnerContext().getModel(),
                            ctx.getSubmission());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Submitting the data upon session destruction failed for session: "
                            + event.getSession().getId(), e);
        }
    }

    /**
     * Do we have any data at all?
     * 
     * @param instance
     *            instance to check for data
     * @return whether data is available or not
     */
    private boolean hasData(final Instance instance) {

        Node node = null;

        for (Iterator<Node> i = instance.getAllNodes().iterator(); i.hasNext();) {

            node = i.next();

            if (node.getValue() != null) {
                return true;
            }
        }
        return false;
    }
}
