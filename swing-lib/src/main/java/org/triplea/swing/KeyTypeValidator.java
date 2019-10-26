package org.triplea.swing;

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
  private boolean validationIsInFlight = false;

  public void attachKeyTypeValidator(
      final JTextComponent textComponent,
      final Predicate<String> dataValidation,
      final Consumer<Boolean> action) {
    DocumentListenerBuilder.attachDocumentListener(
        textComponent,
        () ->
            new Thread(
                    () -> {
                      if (!validationIsInFlight) {
                        validationIsInFlight = true;

                        if(!Interruptibles.sleep(200)) {
                          validationIsInFlight = false;
                          return;
                        }

                        final String textData = textComponent.getText().trim();
                        final boolean valid = dataValidation.test(textData);

                        SwingUtilities.invokeLater(
                            () -> {
                              action.accept(valid);
                              validationIsInFlight = false;
                            });
                      }
                    })
                .start());
  }
}
