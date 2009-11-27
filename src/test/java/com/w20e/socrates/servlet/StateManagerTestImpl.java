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
 * File      : WoliWebStateManager.java
 * Classname : WoliWebStateManager
 * Author    : Wietze Helmantel
 * Date      : 3 feb 2005
 * Version   : $Revision: 1.3 $
 * Copyright : Wyldebeast & Wunderliebe
 * License   : GPL
 */

package com.w20e.socrates.servlet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.w20e.socrates.data.Instance;
import com.w20e.socrates.data.Node;
import com.w20e.socrates.model.InvalidPathExpression;
import com.w20e.socrates.model.ItemProperties;
import com.w20e.socrates.model.Model;
import com.w20e.socrates.model.NodeValidator;
import com.w20e.socrates.rendering.CascadedSelect;
import com.w20e.socrates.rendering.Control;
import com.w20e.socrates.rendering.Group;
import com.w20e.socrates.rendering.OptionList;
import com.w20e.socrates.rendering.RenderConfig;
import com.w20e.socrates.rendering.RenderState;
import com.w20e.socrates.rendering.RenderStateImpl;
import com.w20e.socrates.rendering.Renderable;
import com.w20e.socrates.rendering.StateManager;

/**
 * @author helmantel
 * 
 * Specific WoliWeb State Manager. This guy should take care of the WoliWeb
 * requirement of rendering only one item per go.
 */
public final class StateManagerTestImpl implements StateManager {

	/**
	 * The index of the <b>next</b> item, -1 if unset or empty model.
	 */
	private int itemIndex = -1;

	/**
	 * Holds all possible states.
	 */
	ArrayList<Renderable> states = new ArrayList<Renderable>();

	/**
	 * Enable jump to specific state, by maintaining a mapping based on the id
	 * of the first control in a given state.
	 */
	ArrayList<String> stateIndex = new ArrayList<String>();

	/**
	 * Hold instance for this state manager.
	 */
	private Instance instance;

	/**
	 * Hold the model for this state manager.
	 */
	private Model model;

	/**
	 * Hold current state.
	 */
	private RenderState currentState;

	/**
	 * Construct a manager by using a reference to a <code>Model</code>.
	 * 
	 * @param cfg
	 *            Configuration for this manager
	 * @param m
	 *            the reference to a Model.
	 * @param inst
	 *            The instance to use.
	 */
	public void init(final RenderConfig cfg, final Model m, final Instance inst) {

		this.instance = inst;
		this.model = m;

		// Let's create a map of level 2 thingies... We stuff 'm in a linked
		// hashmap, so as to
		// maintain order, and enable fast determination of the next state.
		//
		Renderable rItem;

		for (Iterator<Renderable> i = cfg.getItems().iterator(); i.hasNext();) {

			rItem = i.next();

			this.states.add(rItem);
			this.stateIndex.add(rItem.getId());
		}
	}

	/**
	 * Reset the managr to the initial state.
	 */
	public final void reset() {

		this.itemIndex = -1;
	}

	/**
	 * Calculate the next state, and return it.
	 * 
	 * @see com.w20e.socrates.process.RenderState
	 * @return a a State (essentially being a List) or null if no State found
	 */
	public final RenderState next() {

		// create a fresh List to be used as a container for
		// Items that belong to a certain State.
		//
		List<Renderable> stateItems = new ArrayList<Renderable>();

		// Ok, looks like we're going to have to calculate next state.

		// If we are already at the end, return null.
		if (this.itemIndex >= this.states.size()) {
			return null;
		}

		// We need at least -1...
		if (this.itemIndex < -1) {

			this.itemIndex = -1;
		}

		this.itemIndex++;

		// Let's find the first state that holds actually items.

		Renderable r = null;

		while (this.itemIndex < this.states.size()) {

			r = this.states.get(this.itemIndex);

			if (isRelevant(r)) {
				stateItems.add(r);
				break;
			}

			this.itemIndex++;
		}

		if (stateItems.size() == 0) {
			// no items recorded for state, return a null State.

			return null;
		}

		this.currentState = new RenderStateImpl("foo", stateItems);

		return current();
	}

	/**
	 * Calculate the previous state, and return it.
	 * 
	 * @see com.w20e.socrates.process.RenderState
	 * @return a a State (essentially being a List) or null if no State found
	 */
	public final RenderState previous() {

		// create a fresh List to be used as a container for
		// Items that belong to a certain State.
		//
		List<Renderable> stateItems = new ArrayList<Renderable>();

		// Ok, looks like we're going to have to calculate next state.

		// If we are already at the beginning, return null.
		if (this.itemIndex < 0) {
			return null;
		}

		// We need at most the size of all possible states.
		if (this.itemIndex > this.states.size()) {

			this.itemIndex = this.states.size();
		}

		this.itemIndex--;

		// Let's find the first state that holds actually items.

		Renderable r = null;

		while (this.itemIndex > -1) {

			r = this.states.get(this.itemIndex);

			if (isRelevant(r)) {
				stateItems.add(r);
				break;
			}

			// Huh? Is it a bird then? A plane..? Or just irrelevant...
			this.itemIndex--;
		}

		if (stateItems.size() == 0) {
			// no items recorded for state, return a null State.

			return null;
		}

		this.currentState = new RenderStateImpl("foo", stateItems);

		return current();
	}

	/**
	 * Return the state last calculated by either next or previous. If none
	 * calculated yet, return null.
	 * 
	 * @return the calculated state.
	 */
	public RenderState current() {

		return this.currentState;
	}

	private boolean isRelevant(final Renderable r) {

		if (r instanceof Group) {
			return isRelevant((Group) r);
		} else if (r instanceof Control) {
			return isRelevant((Control) r);
		}

		return true;
	}

	/**
	 * Determine whether this group is relevant. This is true iff one of the
	 * contained controls is relevant.
	 * 
	 * @param grp
	 * @return whether relevant or not.
	 */
	private boolean isRelevant(final Group grp) {

		Renderable r;

		for (Iterator<Renderable> i = grp.getItems().iterator(); i.hasNext();) {

			r = i.next();

			if (isRelevant(r)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Is the item relevant, and useable?
	 * 
	 * @param node
	 *            Node to check
	 * @return whether to use this item or no
	 */
	private boolean isRelevant(final Control c) {

		Node node;

		try {
			node = this.instance.getNode(c.getBind());
		} catch (InvalidPathExpression e1) {
			// If it doesn't have a node, it is relevant...
			return true;
		}

		// No proper binding...
		if (node == null) {
			return true;
		}

		ItemProperties props = this.model.getItemProperties(node.getName());


		if (!NodeValidator.isRelevant(props, this.instance, this.model)) {
			return false;
		}

		if ("cascadedselect".equals(c.getType())) {
			String ref = ((CascadedSelect) c).getNodeRef();

			if (ref == null) {
				return false;
			}

			try {
				String refvalue = this.instance.getNode(ref).getValue()
						.toString();

				OptionList options = ((CascadedSelect) c).getOptions(refvalue);
				
				if (options.size() == 0) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Set the current state to the state id given. If not found, return false.
	 * 
	 * @param stateId
	 * @return
	 */
	public boolean setState(RenderState state) {

		int idx = this.stateIndex.indexOf(((RenderStateImpl) state).getId());

		if (idx != -1) {
			this.itemIndex = idx;
			return true;
		}
		return false;
	}

	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasPrevious() {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public boolean setStateById(String stateId) {

        int idx = this.stateIndex.indexOf(stateId);

        if (idx != -1) {
            this.itemIndex = idx;
            return true;
        }
        return false;
    }

    @Override
    public int getProgressPercentage() {

        return 0;
    }
}
