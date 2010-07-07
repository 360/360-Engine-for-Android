package com.vodafone360.people.tests.testutils;

import java.lang.reflect.Field;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.UiAgent;

/***
 * Testing framework utilities.
 */
public final class FrameworkUtils {

    /***
     * Private constructor to prevent instantiation.
     */
    private FrameworkUtils() {
        // Do nothing.
    }

    /***
     * Dummy IEngineEventCallback.
     *
     * @return Dummy IEngineEventCallback object.
     */
    public static IEngineEventCallback createDummyIEngineEventCallback() {
        return new IEngineEventCallback() {
            @Override
            public ApplicationCache getApplicationCache() {
                return null;
            }

            @Override
            public UiAgent getUiAgent() {
                return null;
            }

            @Override
            public void kickWorkerThread() {
                // Do nothing.
            }

            @Override
            public void onUiEvent(final ServiceUiRequest event,
                    final int request, final int status, final Object data) {
                // Do nothing.
            }
        };
    }

    /***
     * Set a specific field in the class via reflection.
     *
     * @param remoteService Instance of the class.
     * @param fieldName Name of the field to set via reflection.
     * @param value Value to set the field via reflection.
     * @throws Exception Any kind of mapping exception.
     */
    public static void set(final Object remoteService,
            final String fieldName, final Object value) throws Exception {
        Field field = remoteService.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(remoteService, value);
    }

    /***
     * Get the value of a specific field in the class via reflection.
     *
     * @param remoteService Instance of the class.
     * @param fieldName Name of the field to set via reflection.
     * @throws Exception Any kind of mapping exception.
     * @return Value of object via reflection.
     */
    public static Object get(final Object remoteService,
            final String fieldName) throws Exception {
        Field field = remoteService.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(remoteService);
    }
}