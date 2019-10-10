package org.triplea.java;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class to complement guava "PreConditions" and assert "post" state, for example verifying the
 * state of a return value when a method exits. Java assertions normally would serve this function,
 * but they are not enabled by default. This class serves as an 'always-on' alternative to assertion
 * and provides room for nicer APIs and additional functionality when asserted states fail. <br>
 * Example Usage:
 *
 * <pre><code>
 * int methodCall() {
 *    int returnValue = doCalculation();
 *    Postconditions.assertState(returnValue > 0);
 *    return returnValue;
 * }
 * </code></pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Postconditions {

  /**
   * Checks value of a boolean state value to be true, if not throws an exception with message.
   *
   * @param state State to be asserted true.
   */
  public static void assertState(final boolean state) {
    if (!state) {
      throw new AssertionError("Post condition failed!");
    }
  }

  /**
   * Checks value of a boolean state value to be true, if not throws an exception with message.
   *
   * @param state State to be asserted true.
   * @param message An error message to be included with a thrown exception if state is false.
   */
  public static void assertState(final boolean state, final String message) {
    if (!state) {
      throw new AssertionError("Post condition failed! Details: " + message);
    }
  }

  /**
   * Checks an object to be non-null, if null throws an exception with message.
   *
   * @param objectToCheck Object to check for nullity, if null an exception is thrown.
   * @param message An error message to be included if objectToCheck is null.
   */
  public static void assertNotNull(final Object objectToCheck, final String message) {
    if (objectToCheck == null) {
      throw new AssertionError("Post condition failed, null object, message: " + message);
    }
  }
}
