package com.vodafone360.people.tests.testutils;

/***
 * Object used to store the current state of a running test.
 */
public class TestStatus {
    /** Set to TRUE when a prerequisite for passing occurs.  **/
    private boolean mPass = false;

    /***
     * Return TRUE if the test prerequisite has occurred, FALSE otherwise.
     *
     * @return TRUE if the test should pass.
     */
    public final boolean isPass() {
        return mPass;
    }

    /***
     * Specific if a test passing prerequisite has occurred.
     *
     * @param pass TRUE to pass the test.
     */
    public final void setPass(final boolean pass) {
        mPass = pass;
    }
}