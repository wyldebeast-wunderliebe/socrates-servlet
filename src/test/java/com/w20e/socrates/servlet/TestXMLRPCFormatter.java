package com.w20e.socrates.servlet;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Locale;

import com.w20e.socrates.model.InstanceImpl;
import com.w20e.socrates.model.ModelImpl;
import com.w20e.socrates.model.NodeImpl;
import com.w20e.socrates.process.RunnerContextImpl;
import com.w20e.socrates.rendering.ControlImpl;
import com.w20e.socrates.rendering.Input;
import com.w20e.socrates.rendering.Renderable;
import com.w20e.socrates.servlet.XMLRPCFormatter;

import junit.framework.TestCase;

public class TestXMLRPCFormatter extends TestCase {

	private XMLRPCFormatter formatter;

	protected void setUp() throws Exception {
		super.setUp();

		this.formatter = new XMLRPCFormatter();
	}

	public void testFormat() {

		InstanceImpl inst = new InstanceImpl();
		ModelImpl model = new ModelImpl();

		ArrayList<Renderable> testItems = new ArrayList<Renderable>();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			inst.addNode(new NodeImpl("A01", "SOME VALUE"));

			ControlImpl item = new Input("c0");
			item.setBind("A01");
			item.setType("input");
			item.setLabel("Yo dude");
			item.setHint("Modda");

			ControlImpl item2 = new Input("c1");
			item2.setBind("A02");
			item2.setType("input");
			item2.setLabel("Yo dude2");
			item2.setHint("Modda2");

			testItems.add(item);
			testItems.add(item2);

			RunnerContextImpl ctx = new RunnerContextImpl(out, null, null,
					model, inst, null);
			ctx.setLocale(new Locale("en", "GB"));

			this.formatter.format(testItems, out, ctx);
			// System.out.println(out.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
