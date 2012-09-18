/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy.unittest;

public class SomeClass {
	
	//public int i = 0;
	
	public long waitTime = 1L;
	
	public int finCounter = 0;
	public int errCounter = 0;
	public int runCounter = 0;

    public int getTstCount() {
        return 123+runCounter;
    }

	public void someMethod() {
		TestUtil.sleep(waitTime);
		runCounter++;
	}
	
	public void errorMethod() throws TestException {
		TestUtil.sleep(waitTime);
		throw new TestException("bang!");
	}
	
	public void indirectErrorMethod() throws TestException {
		TestUtil.sleep(waitTime);
		errorMethod();
	}
	
	public void singleArgMethod(String arg1) {
		TestUtil.sleep(waitTime);
	}
	
	public void twoArgMethod(String arg1, String arg2) {
		TestUtil.sleep(waitTime);
	}

	public void threeArgMethod(String arg1, String arg2, String arg3) {
		TestUtil.sleep(waitTime);
	}
	
	public void tryCatchFinallyMethod(String arg) {
		try {
			TestUtil.sleep(1);
			if (arg.startsWith("ERR"))
				throw new Exception(arg);
			runCounter++;
		} catch (Exception e) {
			errCounter++;
		} finally {
			finCounter++;
		}
	}

    public String methodWithStringRetVal(int i) {
        TestUtil.sleep(waitTime);
        return "Returning: " + i;
    }


    protected boolean subsequentPre(String input) {
        return "aaa".equals(input);
    }

    protected boolean subsequentDo(String input) {
        return "aaa".equals(input);
    }

    protected boolean subsequentPost(String input, boolean bo) {
        return "aaa".equals(input) && bo;
    }

    public boolean testWithSubsequentCall(String input) throws Exception {
        if (!subsequentPre(input))
            return false;

        final boolean ret = subsequentDo(input);

        return subsequentPost(input, ret);
    }

    public static int someStaticInt = 123;

    public static int count() {
        return someStaticInt;
    }
}
