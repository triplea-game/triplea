package games.strategy.engine.framework.systemcheck;

import java.util.Optional;


/**
 * Class that performs a 'check' of some sort (executes an arbitrary Runnable).
 * If the runnable executes without exception then the check has passed, otherwise
 * a failure is marked and we remember the exception.
 *
 */
public class SystemCheck {
  private final boolean result;
  private final String msg;
  private final Optional<Exception> exception;

//  /** Constructor for test case usages, use the (String,Runnable) constructor instead */
//  protected SystemCheck(Runnable r) {
//    this("dummy msg test", r);
//  }

  /**
   * @param msg Message that is printed along with success/fail, should describe what the system check did.
   * @param r The runnable that represents the system check, should verify that an action can be performed.
   */
  protected SystemCheck(String msg, Runnable r) {
    this.msg = msg;
    try {
      r.run();
    } catch (Exception e) {
      exception = Optional.of(e);
      result = false;
      return;
    }
    result = true;
    exception = Optional.empty();
  }

  /**
   * @return True if the system check (Runnable constructor arg) completed without exception.
   */
  public boolean wasSuccess() {
    return result;
  }

  /**
   * @return A status message indicating if the system check passed or succeeded.
   */
  public String getResultMessage() {
    return msg + ": " + result;
  }

  /**
   * @return Any exceptions that may have happened while executing the system check.
   */
  public Optional<Exception> getException() {
    return exception;
  }

}
