package com.jitlogic.zorka.mbeans;

import javax.management.j2ee.statistics.TimeStatistic;

public interface MethodCallStatistic extends TimeStatistic {

	public long getCalls();
	
	public long getErrors();
	
}
