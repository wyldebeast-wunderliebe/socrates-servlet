/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * You should have received a copy of the GNU General Public License
 * (for example /usr/src/linux/COPYING); if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.w20e.socrates.servlet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Locale;

import com.w20e.socrates.data.Instance;
import com.w20e.socrates.model.Submission;
import com.w20e.socrates.process.RunnerContext;

/**
 * @author dokter
 * 
 * @todo To change the template for this generated type comment go to Window -
 *       Preferences - Java - Code Style - Code Templates
 */
public class WebsurveyContext implements Serializable {

	/**
	 * Version ID.
	 */
	private static final long serialVersionUID = 8613064653864548295L;

	/**
	 * Hold the runner context. This will not be serialized.
	 */
	private transient RunnerContext ctx;

	/**
	 * Indicate status of context. This is invalid after deserialization.
	 */
	private transient boolean invalid;

	/**
	 * Hold serializable instance.
	 */
	private Instance inst;

	/**
	 * Hold submission info, so even without the model, things may be stored.
	 */
	private Submission submission;

	/**
	 * Hold model id for deserialization.
	 */
	private String modelId;

	/**
	 * Locale to use.
	 */
	private Locale locale;

	/**
	 * Create wrapper.
	 * 
	 * @param newCtx
	 *            Runner context.
	 * @param id
	 *            model id.
	 * @param newLocale
	 *            the locale for the context.
	 */
	public WebsurveyContext(final RunnerContext newCtx, final String id,
			final Locale newLocale) {

		this.ctx = newCtx;
		this.inst = newCtx.getInstance();
		this.submission = newCtx.getModel().getSubmission();
		this.modelId = id;
		this.locale = newLocale;
		this.invalid = false;
	}

	/**
	 * Override for serialization. This will make sure that the instance is
	 * deserialized, and that the model is restored.
	 * 
	 * @param in
	 *            object input stream.
	 * @throws IOException
	 *             when a deserialization error occurs.
	 * @throws ClassNotFoundException
	 *             when the stream doesn't actually hold this class.
	 */
	private void readObject(final ObjectInputStream in) throws IOException,
			ClassNotFoundException {

		in.defaultReadObject();
		this.invalid = true;
	}

	/**
	 * Set the runner context and set status to valid. This will also set the
	 * instance to the runner context's instance!
	 * 
	 * @param newCtx
	 *            runner context.
	 */
	public final void setRunnerContext(final RunnerContext newCtx) {

		this.ctx = newCtx;

		this.inst = newCtx.getInstance();
		this.invalid = false;
	}

	/**
	 * Return instance after serialization.
	 * 
	 * @return instance.
	 */
	public final Instance getInstance() {

		return this.inst;
	}

	/**
	 * Is the context valid?
	 * 
	 * @return whether the context is valid.
	 */
	public final boolean isInvalid() {

		return this.invalid;
	}

	/**
	 * Return the model id.
	 * 
	 * @return the model id
	 */
	public final String getModelId() {

		return this.modelId;
	}

	/**
	 * Return the state id.
	 * 
	 * @return the state id.
	 */
	public final String getStateId() {

		return (String) this.inst.getMetaData().get("stateId");
	}

	/**
	 * Get the context.
	 * 
	 * @return the runner context for this wrapper.
	 */
	public final RunnerContext getRunnerContext() {

		return this.ctx;
	}

	/**
	 * Return the context's locale.
	 * 
	 * @return the locale.
	 */
	public final Locale getLocale() {

		return this.locale;
	}

	/**
	 * Get submission info.
	 * 
	 * @return submission info.
	 */
	public final Submission getSubmission() {

		return this.submission;
	}
}
