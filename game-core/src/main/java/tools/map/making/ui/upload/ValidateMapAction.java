package tools.map.making.ui.upload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JTextArea;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import tools.map.making.ui.upload.map.validators.MapValidator;
import tools.map.making.ui.upload.map.validators.Validators;

@Log
@AllArgsConstructor
class ValidateMapAction implements ActionListener {
  private final UploadPanelState uploadPanelState;

  private final JTextArea validationMessages;

  @Override
  public void actionPerformed(final ActionEvent actionEvent) {
    Path mapDirectory = uploadPanelState.getMapDirectory();

    final List<String> errors = new ArrayList<>();

    try {
      for (MapValidator mapValidator : Validators.getValidators()) {
        errors.addAll(mapValidator.validate(mapDirectory));
      }

      if (errors.isEmpty()) {
        validationMessages.setText("");
      } else {
        validationMessages.setText(String.join("\n", errors));
      }

      uploadPanelState.setValidationStatus(errors.isEmpty());
    } catch (IOException e) {
      log.log(Level.SEVERE, "Error validating map: " + mapDirectory + ", " + e.getMessage(), e);
    }
  }
}
