package com.w20e.socrates.servlet;

import java.io.File;

import junit.framework.TestCase;

import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import com.w20e.socrates.submission.HandlerManager;
import com.w20e.socrates.submission.XMLFileSubmissionHandler;

public class TestWebsurveyServlet extends TestCase {

	ServletUnitClient client;

	public TestWebsurveyServlet(String name) {
		super(name);
	}

	public void setUp() {

		try {
			ServletRunner sr = new ServletRunner(new File(
					"./target/test-classes/web-test.xml"));
			this.client = sr.newClient();
		} catch (Exception e) {
			fail();
		}

		HttpUnitOptions.setScriptingEnabled(false);
	}

	public void testDoPost() {
		
		WebResponse response = null;

		try {
			//Start with JS enabled, just for fun...
			response = this.client
			.getResponse("http://localhost/Survey?id=websurvey-test-config&locale=en_GB");
					
			// register handler here, to make sure proper config is done.
			HandlerManager.getInstance().register("file",
					new XMLFileSubmissionHandler());
			
			// System.out.println(response.getText());
			WebForm form = response.getFormWithID("survey");
			assertNotNull("No form found", form);
			assertEquals("Form method", "POST", form.getMethod());
			assertEquals("Form action", "Survey", form.getAction());
			
			assertTrue("Input field", form.isTextParameter("A1"));
			assertTrue("Input field", form.isTextParameter("stateId"));
						
			form.setParameter("A1", "");

			// Let's not fill in any data, and see
			response = form.submit();

			// Should have received same page
			form = response.getFormWithID("survey");

			assertNotNull("No form found", form);
			assertEquals("Form method", "POST", form.getMethod());
			assertEquals("Form action", "Survey", form.getAction());

			assertTrue("Input field", form.isTextParameter("A1"));

			// fill in field value, since it is required...
			form.setParameter("A1", "请在这里写下来");
			response = form.submit();

			form = response.getFormWithID("survey");

			assertNotNull("No form found", form);
			assertEquals("Form method", "POST", form.getMethod());
			assertEquals("Form action", "Survey", form.getAction());
			
/*			System.out.println(response.getText());

			assertEquals("A02 options", Arrays.asList(new String[] { "-10",
					"-11", "-12" }), Arrays.asList(form.getOptionValues("A02")));

			// let's go one step back...

			response = form.submit(form.getSubmitButton("previous", "previous"));

			form = response.getFormWithID("survey");

			assertTrue("Input field", form.isTextParameter("A01"));
			
			response = form.submit();

			form = response.getFormWithID("survey");

			assertNotNull("No form found", form);
			assertEquals("Form method", "POST", form.getMethod());
			assertEquals("Form action", "Survey", form.getAction());
			assertEquals("A02 options", Arrays.asList(new String[] { "-10",
					"-11", "-12" }), Arrays.asList(form.getOptionValues("A02")));
			
			form.setParameter("A02", "-11");
			response = form.submit();

			form = response.getFormWithID("survey");
			assertNotNull("No form found", form);
			assertEquals("Form method", "POST", form.getMethod());
			assertEquals("Form action", "Survey", form.getAction());

			assertEquals("A03 options", Arrays.asList(new String[] { "",
					"refopt0", "refopt1", "refopt2" }), Arrays.asList(form
					.getOptionValues("A03")));

			// required stuff, so should have same form
			response = form.submit();
			form = response.getFormWithID("survey");
			assertNotNull("No form found", form);

			assertTrue("Input field", form.isTextParameter("A04"));

			// fill in field value, since it is required...
			form.setParameter("A03", "refopt0");
			form.setParameter("A04", "tadaa");

			response = form.submit();

			form = response.getFormWithID("survey");
			assertNotNull("No form found", form);

			assertTrue(form.hasParameterNamed("A05"));

			assertEquals("A05 options", Arrays.asList(new String[] { "opt0",
					"opt1", "opt2" }), Arrays.asList(form
					.getOptionValues("A05")));

			form.setParameter("A05", "opt1");

			response = form.submit();

			form = response.getFormWithID("survey");

			assertNotNull("No form found", form);
			
			assertTrue(form.hasParameterNamed("A06"));

			assertEquals("A06 options", Arrays.asList(new String[] { "opt10",
					"opt11", "opt12" }), Arrays.asList(form
					.getOptionValues("A06")));

			form.setParameter("A06", "opt10");

			response = form.submit();

			form = response.getFormWithID("survey");
			
			assertTrue(form.hasParameterNamed("A06a"));

			assertEquals("A06a options", Arrays.asList(new String[] { "opt100",
					"opt101", "opt102" }), Arrays.asList(form
					.getOptionValues("A06a")));

			response = form.submit();

			form = response.getFormWithID("survey");

			assertTrue(form.hasParameterNamed("A07"));

			// Would be nice to test this, however, junit seems to f*ck things
			// up after POST
			form.setParameter("A07", "10:30");
			response = form.submit();

			response = form.submit();

			form = response.getFormWithID("survey");
			
			assertTrue(form.hasParameterNamed("B01"));

			response = form.submit();
			
			form = response.getFormWithID("survey");
			assertTrue(form.hasParameterNamed("M1"));
			
			// Submission, or so I hope...
			//response = form.submit();
*/			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}
}
