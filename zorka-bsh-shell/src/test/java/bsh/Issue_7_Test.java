package bsh;

import java.io.File;

//@RunWith(FilteredTestRunner.class)
public class Issue_7_Test {

	//@Test TODO test temporarily disabled
	//@Category(KnownIssue.class)
	public void run_script_class13() throws Exception {
		new OldScriptsTest.TestBshScript( new File("tests/test-scripts/class13.bsh")).runTest();
	}

}
