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

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

import org.apache.commons.configuration.Configuration;

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
 * This helper enables detection and setting of changes in Ajax validation.
 */
public final class ValidatorHelper {

	/**
	 * Initialize this class' logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(ValidatorHelper.class.getName());

	/**
	 * Determine UI properties for given list of renderables.
	 * 
	 * @param items
	 *            List of items to use.
	 * @throws Exception
	 *             in case of Velocity errors, or output stream errors.
	 */
	public static void getRenderableProperties(final Collection<Renderable> items,
			final Map<String, Map<String, String>> props, final RunnerContext pContext)
			throws Exception {

        Locale locale = pContext.getLocale();
        
        Configuration cfg = pContext.getConfiguration();
        
        String base = cfg.getString("formatter.locale.prefix");
        
        UTF8ResourceBundle bundle = UTF8ResourceBundleImpl.getBundle(base, locale);

        Stack<Group> parents = new Stack<Group>();
        
		// Let's loop over renderable items.
		//
		for (Renderable rItem: items) {

			addItem(rItem, parents, props, pContext.getInstance(), pContext.getModel(),
					pContext.getRenderConfig(), bundle, locale);

		}
	}

	/**
	 * Add single item to the stream or, if it's a group, add it's controls.
	 * 
	 * @param rItem
	 * @param context
	 * @param pContext
	 * @param bundle
	 */
	private static void addItem(Renderable rItem, Stack<Group> parents,
	        final Map<String, Map<String, String>> props,
			final Instance inst, Model model, RenderConfig cfg,
			final UTF8ResourceBundle bundle, final Locale locale) {

		/**
		 * If it's a group, just add it's controls to the context.
		 */
		if (rItem instanceof Group) {

		    parents.push((Group) rItem);
		    
			for (Renderable rSubItem: ((Group) rItem).getItems()) {

				addItem(rSubItem, parents, props, inst, model, cfg, bundle, locale);
			}
			
			parents.pop();
		}

		if (!(rItem instanceof Control)) {
			return;
		}

		Control control = (Control) rItem;
		String bind = control.getBind();
		Node n;
		Map<String, String> localProps = new HashMap<String, String>();
		
		try {
			n = inst.getNode(bind);
		} catch (InvalidPathExpression e1) {
			return;
		}

		ItemProperties itemProps = model.getItemProperties(bind);

		if (itemProps == null) {
			itemProps = new ItemPropertiesImpl(bind);
		}

		try {
			// Is the item required?
			if (NodeValidator.isRequired(itemProps, inst, model)) {
			    localProps.put("required", "true");
			} else {
                localProps.put("required", "false");
			}

			if (NodeValidator.isRelevant(itemProps, inst, model)) {
                localProps.put("relevant", "true");
                for (Group group: parents) {
                    LOGGER.fine("Adding relevant to parent " + group.getId());
                    Map<String, String> groupProps = new HashMap<String, String>();
                    groupProps.put("relevant", "true");
                    props.put("group:" + group.getId(), groupProps);
                }
			} else {
                localProps.put("relevant", "false");
                for (Group group: parents) {
                    if (!props.containsKey("group:" + group.getId())) {
                        LOGGER.fine("Removing relevant of parent " + group.getId());
                        Map<String, String> groupProps = new HashMap<String, String>();
                        groupProps.put("relevant", "false");
                        props.put("group:" + group.getId(), groupProps);
                    }
                }
			}

            if (NodeValidator.isReadOnly(itemProps, inst, model)) {
                localProps.put("readonly", "true");
            } else {
                localProps.put("readonly", "false");
            }

			// New values we might have
			if (itemProps.getCalculate() != null) {
				try {
				    Object val = control.getDisplayValue(NodeValidator.getValue(n,
				            itemProps, model, inst), itemProps.getDatatype(), locale);
				    localProps.put("value", val.toString());
				} catch (Exception  e) {
				    LOGGER.severe("Exception in resolve of value: " + e.getMessage());
				}
			}

			// Redo label if necessary
			if (control.getLabel().toString().indexOf("${") != -1) {
				String label = FillProcessor.processFills(control.getLabel().toString(),
						inst, model, cfg, locale);
				localProps.put("label", label);
			}

	         // Redo hint if necessary
            if (control.getHint().toString().indexOf("${") != -1) {
                String hint = FillProcessor.processFills(control.getHint().toString(),
                        inst, model, cfg, locale);
                localProps.put("hint", hint);
            }
			
			if (n.getValue() != null) {

				try {
					NodeValidator.validate(n, itemProps, inst, model);
                    localProps.put("alert", "");
				} catch (Exception cv) {
				    LOGGER.finest("Exception during validation" + cv.getMessage());
				    LOGGER.finest("Node value: " + n.getValue() +"; type " + itemProps.getDatatype());
					String msg = "";

					if ("".equals(((Control) rItem).getAlert())) {
						msg = translateError(cv.getMessage(), bundle);
					} else {
						msg = ((Control) rItem).getAlert().toString();
					}
					localProps.put("alert", msg);
				}
			} else {
                localProps.put("alert", "");
			}
		} catch (Exception e) {
		    LOGGER.severe("Couldn't resolve properties:" +  e.getMessage());
		}
		
        props.put(rItem.getId(), localProps);
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
	private static String translateError(final String msg,
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
