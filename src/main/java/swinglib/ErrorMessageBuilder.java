package swinglib;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.ui.SwingAction;


/**
 * Swing builder for an error message pop-up (Uses JOptionPane). Example usages:
 * <code><pre>
 *   JOptionPaneBuilder.builder()
 *     .title("Something Crashed")
 *     .message("Error Details")
 *     .option("ok", () -> {})
 *     .option("quit", () -> System.exit(0))
 *     .buildAndShow();
 * </pre></code>
 * Default error dialog with an 'ok' button:
 * <code><pre>
 *   JOptionPaneBuilder.builder()
 *     .message("error details")
 *     .buildAndShow();
 * </pre></code>
 */
public class ErrorMessageBuilder {

  private String title = "Error";
  private String message;
  private final List<CloseButton> actions = new ArrayList<>();

  private static class CloseButton {
    private final String buttonText;
    private final Runnable closeAction;

    private CloseButton(final String buttonText, final Runnable closeAction) {
      this.buttonText = buttonText;
      this.closeAction = closeAction;
    }
  }

  private ErrorMessageBuilder() {}

  public static ErrorMessageBuilder builder() {
    return new ErrorMessageBuilder();
  }

  public ErrorMessageBuilder message(final String message) {
    this.message = message;
    return this;
  }

  /**
   * Constructs a Swing JDialog using current builder values.
   * Values that must be set: title, contents
   */
  public void buildAndShow() {
    SwingAction.invokeAndWait(this::buildAndShowSwingSafe);
  }

  private void buildAndShowSwingSafe() {
    checkState(!GraphicsEnvironment.isHeadless(), "JOptionPane throws an exception in headless environments");
    checkNotNull(message);

    if (actions.isEmpty()) {
      actions.add(new CloseButton("OK", () -> {
      }));
    }

    final String[] buttonOptions = actions.stream()
        .map(button -> button.buttonText)
        .toArray(String[]::new);

    final int value = JOptionPane.showOptionDialog(
        null,
        message,
        title,
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.ERROR_MESSAGE,
        null,
        buttonOptions,
        0);

    actions.get(value).closeAction.run();
  }

  /**
   * Adds another 'option' button to error message dialog window. Buttons are added in left to right order.
   *
   * @param buttonText Text of the option button.
   * @param buttonAction Action to be performed when the button is clicked.
   */
  public ErrorMessageBuilder option(final String buttonText, final Runnable buttonAction) {
    // Swing adds elements right to left, but we want clients to add components left to right.
    // To fix this we always insert the next component at the front of the list instead of tail.
    actions.add(0, new CloseButton(buttonText, buttonAction));
    return this;
  }

  /**
   * Sets the value that will be displayed in the dialog window title bar.
   */
  public ErrorMessageBuilder title(final String title) {
    Preconditions.checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
    this.title = title;
    return this;
  }
}
