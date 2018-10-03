package swinglib;

import com.google.common.annotations.VisibleForTesting;

/**
 * Class that is a wrapper around the 'test constant'. This is a flag that is set during testing to override
 * any UI dependent logic in swing. Using this is not considered good as we reduce our test coverage, but on the
 * other hand if we can't test because Swing will always open a dialog then we'll lose even more test coverage.
 */
public class TestConstant {
  private static boolean testMode;

  @VisibleForTesting
  public static void setTestConstant() {
    testMode = true;
  }

  /**
   * Checks if the test flag has been triggered.
   *
   * @return True if we are in a test setting and should override any behavior to allow a headless mode.
   */
  static boolean isSet() {
    return testMode;
  }
}
