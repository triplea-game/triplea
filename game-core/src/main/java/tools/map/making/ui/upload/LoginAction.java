package tools.map.making.ui.upload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextArea;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoginAction implements ActionListener {
  private final UploadPanelState uploadPanelState;
  private final JTextArea loginStatus;

  @Override
  public void actionPerformed(final ActionEvent actionEvent) {}
}
