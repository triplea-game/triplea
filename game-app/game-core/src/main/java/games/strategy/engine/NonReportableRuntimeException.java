package games.strategy.engine;

/// Thrown when no error reporting should be offered.
public class NonReportableRuntimeException extends RuntimeException {

  public NonReportableRuntimeException(final String string) {
    super(string);
  }
}
