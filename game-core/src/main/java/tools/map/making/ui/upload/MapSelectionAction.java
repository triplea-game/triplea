package tools.map.making.ui.upload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MapSelectionAction implements ActionListener {
  private final JFrame parentWindow;
  private final UploadPanelState uploadPanelState;
  private final JTextArea statusArea;

  @Override
  public void actionPerformed(final ActionEvent actionEvent) {
    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select map folder to be uploaded");
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int option = fileChooser.showOpenDialog(parentWindow);
    if (option == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile().isDirectory()) {
      uploadPanelState.setMapDirectory(fileChooser.getSelectedFile().toPath());
      statusArea.setText(fileChooser.getSelectedFile().getAbsolutePath());
    } else {
      uploadPanelState.unsetMapDirectory();
      statusArea.setText("");
    }
  }
}
