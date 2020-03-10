package org.triplea.swing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.triplea.java.Interruptibles;

/**
 * Performs a validation while a user is entering data into a text field. The validation result is
 * then sent to a consumer with the result of the validation. Validation is done in a background
 * thread to not interrupt user input and has a back-off so that user input is slightly buffered
 * before validation is re-executed. This can be useful for example to enable or disable a submit
 * button depending on the value of a text-field.
 */
public class KeyTypeValidator {
  private AtomicBoolean validationIsInFlight = new AtomicBoolean(false);

  public void attachKeyTypeValidator(
      final JTextComponent textComponent,
      final Predicate<String> dataValidation,
      final Consumer<Boolean> action) {
    new DocumentListenerBuilder(
            () ->
                new Thread(
                        () -> {
                          if (validationIsInFlight.compareAndSet(false, true)) {
                            // sleep to delay our current check and allow
                            // the check to be done once at the end of any
                            // further input that might still be in-flight.
                            if (!Interruptibles.sleep(200)) {
                              validationIsInFlight.set(false);
                              return;
                            }
                            // release the boolean lock, if we get another validation
                            // request, allow it to enter queue. This way even if 'getText()'
                            // returns us stale data, that new request will have a chance to
                            // operate on the most recent.
                            validationIsInFlight.set(false);

                            final String textData = textComponent.getText().trim();
                            final boolean valid = dataValidation.test(textData);

                            SwingUtilities.invokeLater(() -> action.accept(valid));
                          }
                        })
                    .start())
        .attachTo(textComponent);
  }
}
