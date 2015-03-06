package com.w20e.socrates.servlet;

import java.io.File;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Create URL's based on config paramters.
 * 
 * @author dokter
 * 
 */
public final class QuestionnaireURIFactory {

	private static QuestionnaireURIFactory factory;

	private static final Logger LOGGER = Logger
			.getLogger(QuestionnaireURIFactory.class.getName());

	/*
	 * Private constructor.
	 */
	private QuestionnaireURIFactory() {
		// Nothing to do...
	}

	/**
	 * Get an instance of the factory.
	 * 
	 * @return
	 */
	public static synchronized QuestionnaireURIFactory getInstance() {

		if (factory == null) {
			factory = new QuestionnaireURIFactory();
		}

		return factory;
	}

	/**
	 * Determine proper URL for loading configuration.
	 * 
	 * @param rootDir
	 * @param id
	 * @param locale
	 * @return
	 */
	public URI determineURI(final String rootDir, final String surveyId) {

		String cId = surveyId;

		File file = new File(rootDir
				+ System.getProperty("file.separator", "/") + cId + ".xml");

		if (!file.exists()) {
			file = new File(rootDir + System.getProperty("file.separator", "/")
					+ surveyId + ".xml");
		}

		LOGGER.fine("Using file " + file.toString());

		return file.toURI();
	}
}