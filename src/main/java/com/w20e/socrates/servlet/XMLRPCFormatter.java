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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.helpers.AttributesImpl;

import com.w20e.socrates.data.Instance;
import com.w20e.socrates.data.Node;
import com.w20e.socrates.model.ConstraintViolation;
import com.w20e.socrates.model.InvalidPathExpression;
import com.w20e.socrates.model.ItemProperties;
import com.w20e.socrates.model.ItemPropertiesImpl;
import com.w20e.socrates.model.Model;
import com.w20e.socrates.model.NodeValidator;
import com.w20e.socrates.process.RunnerContext;
import com.w20e.socrates.rendering.Control;
import com.w20e.socrates.rendering.Group;
import com.w20e.socrates.rendering.RenderConfig;
import com.w20e.socrates.rendering.Renderable;
import com.w20e.socrates.util.FillProcessor;
import com.w20e.socrates.util.UTF8ResourceBundle;
import com.w20e.socrates.util.UTF8ResourceBundleImpl;

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
	public void format(final Collection<Renderable> items,
			final OutputStream out, final RunnerContext pContext)
			throws Exception {

		Writer writer = new OutputStreamWriter(out, "UTF-8");

		Locale locale = pContext.getLocale();

		// @todo: the locale prefix should be configurable
		UTF8ResourceBundle bundle = UTF8ResourceBundleImpl.getBundle(
				"websurvey", locale);

		LOGGER.fine("Using locale " + pContext.getLocale());
		LOGGER.finest("Formatting " + items.size() + " items");
		LOGGER.fine("Resource locale: " + bundle.getLocale());

		// Let's declare vars outside of loop
		Renderable rItem = null;

		List<Renderable> fItems = new ArrayList<Renderable>();

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

		atts.addAttribute("", "", "xmlns", "CDATA",
				"http://www.kukit.org/commands/1.1");
		hd.startElement("", "", "kukit", atts);
		atts.clear();
		hd.startElement("", "", "commands", atts);

		// Let's loop over renderable items.
		//
		for (Iterator<Renderable> i = items.iterator(); i.hasNext();) {

			rItem = i.next();

			addItem(rItem, hd, pContext.getInstance(), pContext.getModel(),
					pContext.getRenderConfig(), bundle, locale);

			fItems.add(rItem);
		}

		hd.endElement("", "", "commands");
		hd.endElement("", "", "kukit");
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
	private void addItem(Renderable rItem, final TransformerHandler hd,
			final Instance inst, Model model, RenderConfig cfg,
			final UTF8ResourceBundle bundle, final Locale locale) {

		/**
		 * If it's a group, just add it's controls to the context.
		 */
		if (rItem instanceof Group) {

			addGroup((Group) rItem, hd, inst, model);

			for (Iterator<Renderable> i = ((Group) rItem).getItems().iterator(); i
					.hasNext();) {

				addItem(i.next(), hd, inst, model, cfg, bundle, locale);
			}
			return;
		}

		if (!(rItem instanceof Control)) {
			return;
		}

		Control control = (Control) rItem;
		String bind = control.getBind();
		Node n;
		AttributesImpl atts = new AttributesImpl();

		try {
			n = inst.getNode(bind);
		} catch (InvalidPathExpression e1) {
			return;
		}

		ItemProperties props = model.getItemProperties(bind);

		if (props == null) {
			props = new ItemPropertiesImpl(bind);
		}

		try {

			atts.clear();
			atts.addAttribute("", "", "selector", "CDATA", "#field-"
					+ control.getId());
			atts.addAttribute("", "", "selectorType", "CDATA", "");

			// Is the item required?
			if (NodeValidator.isRequired(props, inst, model)) {
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
			atts.addAttribute("", "", "selector", "CDATA", "#field-"
					+ control.getId());
			atts.addAttribute("", "", "selectorType", "CDATA", "");

			if (NodeValidator.isRelevant(props, inst, model)) {
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
			atts.addAttribute("", "", "selector", "CDATA", "#field-"
					+ control.getId());
			atts.addAttribute("", "", "selectorType", "CDATA", "");

			if (NodeValidator.isReadOnly(props, inst, model)) {
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
			if (props.getCalculate() != null) {
				atts.clear();
				atts.addAttribute("", "", "selector", "CDATA", "#field-"
						+ control.getId() + " div.readonly-value");
				atts.addAttribute("", "", "selectorType", "CDATA", "");
				atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");

				hd.startElement("", "", "command", atts);
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "html");
				hd.startElement("", "", "param", atts);
				try {
				    Object val = control.getDisplayValue(NodeValidator.getValue(n,
				            props, model, inst), props.getType(), locale);
				    hd.characters(val.toString().toCharArray(), 0, val.toString()
				            .length());
				} catch (Exception  e) {
				    LOGGER.severe("Exception in resolve of value: " + e.getMessage());
				}
				hd.endElement("", "", "param");
				hd.endElement("", "", "command");
			}

			// Redo label if necessary
			if (control.getLabel().indexOf("${") != -1) {
				atts.clear();
				atts.addAttribute("", "", "selector", "CDATA", "#field-"
						+ control.getId() + " label.field-label");
				atts.addAttribute("", "", "selectorType", "CDATA", "");
				atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");

				hd.startElement("", "", "command", atts);
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "html");
				hd.startElement("", "", "param", atts);
				String label = FillProcessor.processFills(control.getLabel(),
						inst, model, cfg, locale);
				hd.characters(label.toCharArray(), 0, label.length());
				hd.endElement("", "", "param");
				hd.endElement("", "", "command");
			}

	         // Redo hint if necessary
            if (control.getHint().indexOf("${") != -1) {
                atts.clear();
                atts.addAttribute("", "", "selector", "CDATA", "#field-"
                        + control.getId() + " div.hint");
                atts.addAttribute("", "", "selectorType", "CDATA", "");
                atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");

                hd.startElement("", "", "command", atts);
                atts.clear();
                atts.addAttribute("", "", "name", "CDATA", "html");
                hd.startElement("", "", "param", atts);
                String hint = FillProcessor.processFills(control.getHint(),
                        inst, model, cfg, locale);
                hd.characters(hint.toCharArray(), 0, hint.length());
                hd.endElement("", "", "param");
                hd.endElement("", "", "command");
            }
			
			if (n.getValue() != null) {
				atts.clear();
				atts.addAttribute("", "", "selector", "CDATA", "#alert-"
						+ control.getId());
				atts.addAttribute("", "", "selectorType", "CDATA", "");
				atts.addAttribute("", "", "name", "CDATA", "replaceInnerHTML");
				hd.startElement("", "", "command", atts);
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "html");
				hd.startElement("", "", "param", atts);

				try {
					NodeValidator.validate(n, props, inst, model);
					hd.characters("".toCharArray(), 0, 0);

				} catch (Exception cv) {
					String msg = "";

					if ("".equals(((Control) rItem).getAlert())) {
						msg = translateError(cv.getMessage(), bundle);
					} else {
						msg = ((Control) rItem).getAlert();
					}
					hd.characters(msg.toCharArray(), 0, msg.length());
				}

				hd.endElement("", "", "param");
				hd.endElement("", "", "command");
			}
		} catch (Exception e) {
			// @todo Well, let's come up with something...
		}
	}

	private void addGroup(Group group, final TransformerHandler hd,
			final Instance inst, Model model) {

		AttributesImpl atts = new AttributesImpl();

		try {
			// Relevance for group
			atts.clear();
			atts.addAttribute("", "", "selector", "CDATA", "#fieldset-"
					+ group.getId());
			atts.addAttribute("", "", "selectorType", "CDATA", "");

			if (isRelevant(group, model, inst)) {
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
		} catch (Exception e) {
			// @todo Well, let's come up with something...
		}
	}

	/**
	 * Determine whether group should actually be shown or not.
	 * 
	 * @param group
	 * @param pContext
	 * @return
	 */
	private boolean isRelevant(final Group group, final Model model,
			final Instance inst) {

		for (Renderable r : group.getItems()) {

			if (r instanceof Control) {
				Control control = (Control) r;
				String bind = control.getBind();
				ItemProperties props = model.getItemProperties(bind);

				if (props == null) {
					props = new ItemPropertiesImpl(bind);
				}

				if (NodeValidator.isRelevant(props, inst, model)) {
					return true;
				}
			} else if (r instanceof Group) {
				if (isRelevant((Group) r, model, inst)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Return the translated alert message.
	 * 
	 * @param msg
	 *            original message.
	 * @param bundle
	 *            locale bindle
	 * @return the translated message.
	 */
	private String translateError(final String msg,
			final UTF8ResourceBundle bundle) {

		try {
			if (ConstraintViolation.REQUIRED.equals(msg)) {
				return bundle.getString("alert.required");
			} else if (ConstraintViolation.TYPE.equals(msg)) {
				return bundle.getString("alert.type");
			} else if (ConstraintViolation.FALSE.equals(msg)) {
				return bundle.getString("alert.constraint");
			} else {
				return bundle.getString("alert.unknown");
			}
		} catch (Exception e) {
			return "Erroneous input";
		}
	}
}
