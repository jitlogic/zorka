package com.jitlogic.zorka.agent.testutil;

import java.util.concurrent.Executor;

public class TestExecutor implements Executor {

	public void execute(Runnable command) {
		command.run();
	}

}
