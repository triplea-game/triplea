package org.triplea.test.common;

public enum TestType {
  /** A test that verifies multiple modules are configured correctly to work together. */
  INTEGRATION,

  /** A non-unit test that verifies a target functionality. */
  ACCEPTANCE,

  /** A test that verifies test code. */
  TEST_CODE_VERIFICATION
}
