package swinglib;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.google.common.annotations.VisibleForTesting;

import lombok.RequiredArgsConstructor;

/**
 * Type-safe builder to create a swing dialog confirmation runnable. When the runnable is executed a modal
 * yes/no confirmation dialog will be shown to the user, if yes is clicked the confirm action will be executed.
 */
public class ConfirmationDialogBuilder {
  private static boolean headlessMode;

  @VisibleForTesting
  public static void suppressDialog() {
    headlessMode = true;
  }

  public static TitleBuilder builder() {
    return new TitleBuilder();
  }

  /**
   * Type safe builder that adds the dialog title.
   */
  public static class TitleBuilder {
    private String title;

    public MessageBuilder title(final String title) {
      this.title = title;
      return new MessageBuilder(this);
    }
  }


  /**
   * Type safe builder that adds the dialog message.
   */
  @RequiredArgsConstructor
  public static class MessageBuilder {
    private final TitleBuilder titleBuilder;

    private String message;

    public ConfirmActionBuilder message(final String message) {
      this.message = message;
      return new ConfirmActionBuilder(this);
    }
  }


  /**
   * Type safe builder that adds the action to be run if the user confirms.
   */
  @RequiredArgsConstructor
  public static class ConfirmActionBuilder {
    private final MessageBuilder messageBuilder;
    private Runnable confirmAction;

    public DialogOptionalsBuilder confirmAction(final Runnable confirmAction) {
      this.confirmAction = confirmAction;
      return new DialogOptionalsBuilder(this);
    }

  }

  /**
   * Type safe builder, this is the final builder can construct the dialog or first add an optional properties.
   */
  @RequiredArgsConstructor
  public static class DialogOptionalsBuilder {
    private final ConfirmActionBuilder confirmActionBuilder;

    private Component parent;

    public DialogOptionalsBuilder parent(final Component parent) {
      this.parent = parent;
      return this;
    }

    public Runnable build() {
      return headlessMode
          ? confirmActionBuilder.confirmAction
          : this::showConfirmDialog;
    }

    private void showConfirmDialog() {
      final int result = JOptionPane.showConfirmDialog(
          parent,
          confirmActionBuilder.messageBuilder.message,
          confirmActionBuilder.messageBuilder.titleBuilder.title,
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        confirmActionBuilder.confirmAction.run();
      }
    }

  }
}
