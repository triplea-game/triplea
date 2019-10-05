package org.triplea.swing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.awt.Component;
import javax.swing.JOptionPane;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Type-safe builder to create a swing dialog confirmation runnable. When the runnable is executed a
 * modal yes/no confirmation dialog will be shown to the user, if yes is clicked the confirm action
 * will be executed.
 */
public final class DialogBuilder {
  private static boolean uiEnabled = true;

  private DialogBuilder() {}

  /** Method to turn off UI notifications for cases when we are running tests. */
  @VisibleForTesting
  public static void disableUi() {
    uiEnabled = false;
  }

  public static WithParentBuilder builder() {
    return new WithParentBuilder();
  }

  /**
   * Type safe builder that adds a parent component, this centers the dialog message over its
   * parent.
   */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class WithParentBuilder {
    private Component parent;

    public TitleBuilder parent(final Component parent) {
      this.parent = parent;
      return new TitleBuilder(this);
    }
  }

  /** Type safe builder that adds the dialog title. */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class TitleBuilder {
    private final WithParentBuilder withParentBuilder;
    private String title;

    public WithMessageBuilder title(final String title) {
      this.title = Preconditions.checkNotNull(title);
      return new WithMessageBuilder(this);
    }
  }

  /** Type safe builder that adds the dialog message. */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class WithMessageBuilder {
    private final TitleBuilder withTitleBuilder;

    private String message;

    public WithInfoMessageBuilder infoMessage(final String infoMessage) {
      this.message = infoMessage;
      return new WithInfoMessageBuilder(this);
    }

    public WithErrorMessageBuilder errorMessage(final String errorMessage) {
      this.message = errorMessage;
      return new WithErrorMessageBuilder(this);
    }

    public WithConfirmActionBuilder confirmationQuestion(final String message) {
      this.message = message;
      return new WithConfirmActionBuilder(this);
    }
  }

  /** Builds an information dialog that the user can view and close. */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class WithInfoMessageBuilder {
    private final WithMessageBuilder withMessageBuilder;

    public void showDialog() {
      if (uiEnabled) {
        showMessage(withMessageBuilder, JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }

  private static void showMessage(
      final WithMessageBuilder withMessageBuilder, final int optionPanetype) {
    try {
      SwingAction.invokeAndWait(
          () ->
              JOptionPane.showConfirmDialog(
                  withMessageBuilder.withTitleBuilder.withParentBuilder.parent,
                  withMessageBuilder.message,
                  withMessageBuilder.withTitleBuilder.title,
                  JOptionPane.DEFAULT_OPTION,
                  optionPanetype));
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Builds an error message styled dailog, user can view it and close. */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class WithErrorMessageBuilder {
    private final WithMessageBuilder withMessageBuilder;

    public void showDialog() {
      if (uiEnabled) {
        showMessage(withMessageBuilder, JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /** Adds option to confirm/cancel. */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class WithConfirmActionBuilder {
    private final WithMessageBuilder withMessageBuilder;
    private Runnable confirmAction;

    public Builder confirmAction(final Runnable confirmAction) {
      this.confirmAction = confirmAction;
      return new Builder(this);
    }
  }

  /** Adds a yes/no confirmation dialog where user can cancel an action. */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final WithConfirmActionBuilder withConfirmActionBuilder;

    /**
     * Opens a yes/no confirmation dialog, executes the 'confirm' action if user selects 'yes',
     * selecting 'no' simply closes the dialog.
     */
    public void showDialog() {
      if (uiEnabled) {
        SwingAction.invokeNowOrLater(
            () -> {
              final int result =
                  JOptionPane.showConfirmDialog(
                      withConfirmActionBuilder
                          .withMessageBuilder
                          .withTitleBuilder
                          .withParentBuilder
                          .parent,
                      withConfirmActionBuilder.withMessageBuilder.message,
                      withConfirmActionBuilder.withMessageBuilder.withTitleBuilder.title,
                      JOptionPane.YES_NO_OPTION,
                      JOptionPane.QUESTION_MESSAGE);

              if (result == JOptionPane.YES_OPTION) {
                withConfirmActionBuilder.confirmAction.run();
              }
            });
      }
    }
  }
}
