package bsh;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.script.ScriptException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(FilteredTestRunner.class)
public class Issue_55_Test {

	@Category( NotSuitedFor_Java5_OrLower.class )
	@Test
	public void check_BshScriptEngineFactory() throws Exception {
		final String script = "a = null; return \"a=\" + a;\n";
		final Object interpreterResult = new Interpreter().eval(script);
		final Object scriptEngineResult = new BshScriptEngineFactory().getScriptEngine().eval(script);
		assertEquals(interpreterResult, scriptEngineResult);
	}


	@Test
	public void check_ExternalNameSpace() throws Exception {
		final ExternalNameSpace externalNameSpace = new ExternalNameSpace();
		externalNameSpace.setVariable("a", Primitive.NULL, false);
		assertTrue("map should contain variable 'a'", externalNameSpace.getMap().containsKey("a"));
		assertNull("variable 'a' should have value <NULL>", externalNameSpace.getMap().get("a"));
	}

	@Category( NotSuitedFor_Java5_OrLower.class )
	@Test
	public void issue_67() throws Exception {
		final String script = "print(\"test\";";
		try {
			new BshScriptEngineFactory().getScriptEngine().eval(script);
			fail("expected script exception");
		} catch (ScriptException e) {
			assertEquals(1, e.getLineNumber());
		}
	}

}
