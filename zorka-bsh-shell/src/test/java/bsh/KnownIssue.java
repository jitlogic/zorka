package bsh;

public class KnownIssue implements TestFilter {

	static final boolean SKIP_KOWN_ISSUES = "yes".equalsIgnoreCase(System.getProperties().getProperty("skip_known_issues", "yes"));

	public boolean skip() {
		return SKIP_KOWN_ISSUES;
	}

}
