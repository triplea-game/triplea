package tools.map.making.ui.upload.map.validators;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.map.making.ui.upload.UploadPanelState;

/**
 * Runs validations on a given map. If valid, notifies the panel state to enable additional buttons
 * and updates the validation text area with either validation errors or with an '[ok]' status.
 */
@Slf4j
@AllArgsConstructor
public class ValidateMapAction implements ActionListener {
  private final UploadPanelState uploadPanelState;

  private final JTextArea validationMessages;

  @Override
  public void actionPerformed(final ActionEvent actionEvent) {
    final Path mapDirectory = uploadPanelState.getMapDirectory();

    final List<String> errors = new ArrayList<>();

    try {
      for (final MapValidator mapValidator : Validators.getValidators()) {
        errors.addAll(mapValidator.validate(mapDirectory));
      }

      if (errors.isEmpty()) {
        validationMessages.setText("[OK] Map is valid");
        uploadPanelState.setValidationStatus(true);
      } else {
        validationMessages.setText(String.join("\n", errors));
        uploadPanelState.setValidationStatus(false);
      }
    } catch (final IOException e) {
      log.error("Error validating map: " + mapDirectory + ", " + e.getMessage(), e);
    }
  }
}
