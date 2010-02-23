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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.helpers.AttributesImpl;

import com.w20e.socrates.process.RunnerContext;

/**
 * Velocity formatter for the Socrates engine. The formatter is configured with
 * a mapping from classes to templates. This formatter is implemented as a
 * singleton.
 */
public final class XMLRPCFormatter {

	/**
	 * Initialize this class' logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(XMLRPCFormatter.class.getName());

	/**
	 * Format list of items.
	 * 
	 * @param items
	 *            List of items to use.
	 * @param out
	 *            OutputStream to use
	 * @param pContext
	 *            Processing context
	 * @throws Exception
	 *             in case of Velocity errors, or output stream errors.
	 */
	public void format(final Map<String, Map<String, String>> items,
			final OutputStream out, final RunnerContext pContext)
			throws Exception {

		Writer writer = new OutputStreamWriter(out, "UTF-8");

		LOGGER.finest("Formatting " + items.size() + " items");

		StreamResult streamResult = new StreamResult(writer);
		SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory
				.newInstance();
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();

		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		// serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"users.dtd");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		hd.setResult(streamResult);
		hd.startDocument();

		AttributesImpl atts = new AttributesImpl();

		hd.startElement("", "", "validation", atts);
		hd.startElement("", "", "commands", atts);

		// Let's loop over renderable items.
		//
		for (String rItem: items.keySet()) {

			addItem(rItem, items.get(rItem), hd);
		}

		hd.endElement("", "", "commands");
		hd.endElement("", "", "validation");
		hd.endDocument();

		writer.flush();
	}

	/**
	 * Add single item to the stream or, if it's a group, add it's controls.
	 * 
	 * @param rItem
	 * @param context
	 * @param pContext
	 * @param bundle
	 */
	private void addItem(String rItem, Map<String, String> itemProps, TransformerHandler hd) {

		AttributesImpl atts = new AttributesImpl();

		try {

			atts.clear();
			atts.addAttribute("", "", "selector", "CDATA", "#field-" + rItem);
			atts.addAttribute("", "", "selectorType", "CDATA", "");

			// Is the item required?
			if ("true".equals(itemProps.get("required"))) {
				atts.addAttribute("", "", "name", "CDATA", "addClass");
			} else {
				atts.addAttribute("", "", "name", "CDATA", "removeClass");
			}

			hd.startElement("", "", "command", atts);
			atts.clear();
			atts.addAttribute("", "", "name", "CDATA", "value");
			hd.startElement("", "", "param", atts);
			hd.characters("required".toCharArray(), 0, 8);
			hd.endElement("", "", "param");
			hd.endElement("", "", "command");

			// Relevance
			atts.clear();
			atts.addAttribute("", "", "selector", "CDATA", "#field-" + rItem);
			atts.addAttribute("", "", "selectorType", "CDATA", "");

            if ("true".equals(itemProps.get("relevant"))) {
				atts.addAttribute("", "", "name", "CDATA", "addClass");
			} else {
				atts.addAttribute("", "", "name", "CDATA", "removeClass");
			}

			hd.startElement("", "", "command", atts);
			atts.clear();
			atts.addAttribute("", "", "name", "CDATA", "value");
			hd.startElement("", "", "param", atts);
			hd.characters("relevant".toCharArray(), 0, 8);
			hd.endElement("", "", "param");
			hd.endElement("", "", "command");

			// Readonly-ness
			atts.clear();
			atts.addAttribute("", "", "selector", "CDATA", "#field-" + rItem);
			atts.addAttribute("", "", "selectorType", "CDATA", "");

            if ("true".equals(itemProps.get("readonly"))) {
				atts.addAttribute("", "", "name", "CDATA", "addClass");
			} else {
				atts.addAttribute("", "", "name", "CDATA", "removeClass");
			}

			hd.startElement("", "", "command", atts);
			atts.clear();
			atts.addAttribute("", "", "name", "CDATA", "value");
			hd.startElement("", "", "param", atts);
			hd.characters("readonly".toCharArray(), 0, 8);
			hd.endElement("", "", "param");
			hd.endElement("", "", "command");

			// New values we might have
			if (itemProps.get("value") != null) {
				atts.clear();
				atts.addAttribute("", "", "selector", "CDATA", "#field-"
						+ rItem + " div.readonly-value");
				atts.addAttribute("", "", "selectorType", "CDATA", "");
				atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");

				hd.startElement("", "", "command", atts);
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "html");
				hd.startElement("", "", "param", atts);
				hd.characters(itemProps.get("value").toCharArray(), 0, itemProps.get("value")
				        .length());
				hd.endElement("", "", "param");
				hd.endElement("", "", "command");
			}

			// Redo label if necessary
			if (itemProps.get("label") != null) {
				atts.clear();
				atts.addAttribute("", "", "selector", "CDATA", "#field-"
						+ rItem + " label.field-label");
				atts.addAttribute("", "", "selectorType", "CDATA", "");
				atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");

				hd.startElement("", "", "command", atts);
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "html");
				hd.startElement("", "", "param", atts);
				hd.characters(itemProps.get("label").toCharArray(), 0, itemProps.get("label").length());
				hd.endElement("", "", "param");
				hd.endElement("", "", "command");
			}

	         // Redo hint if necessary
            if (itemProps.get("hint") != null) {
                atts.clear();
                atts.addAttribute("", "", "selector", "CDATA", "#field-"
                        + rItem + " div.hint");
                atts.addAttribute("", "", "selectorType", "CDATA", "");
                atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");

                hd.startElement("", "", "command", atts);
                atts.clear();
                atts.addAttribute("", "", "name", "CDATA", "html");
                hd.startElement("", "", "param", atts);
                hd.characters(itemProps.get("hint").toCharArray(), 0, itemProps.get("hint").length());
                hd.endElement("", "", "param");
                hd.endElement("", "", "command");
            }
			
			if (itemProps.get("alert") != null) {
				atts.clear();
				atts.addAttribute("", "", "selector", "CDATA", "#alert-" + rItem);
				atts.addAttribute("", "", "selectorType", "CDATA", "");
				atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");
				hd.startElement("", "", "command", atts);
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "html");
				hd.startElement("", "", "param", atts);
				hd.characters(itemProps.get("alert").toCharArray(), 0, itemProps.get("alert").length());
				hd.endElement("", "", "param");
				hd.endElement("", "", "command");
			}
		} catch (Exception e) {
			// @todo Well, let's come up with something...
		}
	}
}
