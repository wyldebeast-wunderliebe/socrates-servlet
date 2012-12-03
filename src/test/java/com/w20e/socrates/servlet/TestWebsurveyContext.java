/*
 * Created on Apr 25, 2005
 *
 * @todo To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.w20e.socrates.servlet;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import junit.framework.TestCase;

import com.w20e.socrates.expression.XBoolean;
import com.w20e.socrates.model.InstanceImpl;
import com.w20e.socrates.model.ItemProperties;
import com.w20e.socrates.model.ItemPropertiesImpl;
import com.w20e.socrates.model.ModelImpl;
import com.w20e.socrates.data.Node;
import com.w20e.socrates.model.NodeImpl;
import com.w20e.socrates.model.SubmissionImpl;
import com.w20e.socrates.process.RunnerContext;
import com.w20e.socrates.process.RunnerContextImpl;
import com.w20e.socrates.servlet.WebsurveyContext;

/**
 * @author dokter
 * 
 * @todo To change the template for this generated type comment go to Window -
 *       Preferences - Java - Code Style - Code Templates
 */
public class TestWebsurveyContext extends TestCase {

	private WebsurveyContext ctx;

	private InstanceImpl instance;

	public void setUp() {

		this.instance = new InstanceImpl();

		// Let's create a model, so as to test some complex
		// requirements.
		Node node0 = new NodeImpl("/a");
		Node node1 = new NodeImpl("/a/b");
		Node node2 = new NodeImpl("/a/b/c1");
		Node node3 = new NodeImpl("/a/b/c2");

		this.instance.addNode(node0);
		this.instance.addNode(node1);
		this.instance.addNode(node2);
		this.instance.addNode(node3);

		ModelImpl model = new ModelImpl();

		ItemProperties props0 = new ItemPropertiesImpl("a");
		ItemProperties props1 = new ItemPropertiesImpl("b");

		props0.setRequired(new XBoolean(true));
		props1.setRelevant(new XBoolean(false));

		model.addItemProperties(props0);
		model.addItemProperties(props1);

		SubmissionImpl sub = new SubmissionImpl();
		try {
			sub.setAction(new URI("file:///./testrunnerimpl-out.xml"));
		} catch (URISyntaxException e) {
			// Will fail later on.
		}

		model.setSubmission(sub);

		RunnerContext rCtx = new RunnerContextImpl(new ByteArrayOutputStream(),
				null, null, model, this.instance, null);

		this.ctx = new WebsurveyContext(rCtx, "pipo", new Locale("nl", "NL"));
	}

	public void testSerialize() {

		String filename = "./target/context.ser";
		WebsurveyContext restoredCtx = null;

		// do the serialization...
		try {
			this.ctx.getInstance().getNode("/a").setValue("Mamaloe");
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(this.ctx);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			FileInputStream fin = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(fin);
			restoredCtx = (WebsurveyContext) in.readObject();
			in.close();

			assertNotNull(restoredCtx);

			assertEquals("Mamaloe", restoredCtx.getInstance().getNode("/a")
					.getValue());

			assertEquals("pipo", restoredCtx.getModelId());
			assertEquals(new Locale("nl", "NL"), restoredCtx.getLocale());
			assertTrue(restoredCtx.isInvalid());

		} catch (Exception e) {
			fail(e.getMessage());
		}

	}
}
