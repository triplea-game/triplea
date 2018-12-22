package games.strategy.debug;

/**
 * A 'console' window to display log messages to users.
 */
public interface Console {
  /**
   * Shows or hides the console window.
   */
  void setVisible(boolean visible);

  /**
   * Appends the specified string to the end of the console window without a trailing newline.
   */
  void append(String s);

  /**
   * Appends the specified string to the end of the console window with a trailing newline.
   */
  default void appendLn(final String s) {
    append(s + "\n");
  }

  static Console newInstance() {
    return new DefaultConsole();
  }
}
