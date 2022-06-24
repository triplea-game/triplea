package games.strategy.engine.framework.network.ui;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import java.awt.Component;
import java.io.IOException;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;

/** A class for changing game options across all network nodes from a client node. */
@Slf4j
public class ChangeGameOptionsClientAction {

  public static void run(
      final Component parent, final byte[] oldBytes, final IServerStartupRemote serverRemote) {
    if (oldBytes.length == 0) {
      return;
    }
    try {
      final List<IEditableProperty<?>> properties = GameProperties.readEditableProperties(oldBytes);
      final PropertiesUi pui = new PropertiesUi(properties, true);
      final JScrollPane scroll = new JScrollPane(pui);
      scroll.setBorder(null);
      scroll.getViewport().setBorder(null);
      final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
      final String ok = "OK";
      final String cancel = "Cancel";
      pane.setOptions(new Object[] {ok, cancel});
      final JDialog window =
          pane.createDialog(JOptionPane.getFrameForComponent(parent), "Game Options");
      window.setVisible(true);
      final Object buttonPressed = pane.getValue();
      if (buttonPressed != null && !buttonPressed.equals(cancel)) {
        // ok was clicked. changing them in the ui changes the underlying properties,
        // but it doesn't change the hosts, so we need to send it back to the host.
        ThreadRunner.runInNewThread(
            () -> {
              try {
                serverRemote.changeToGameOptions(
                    GameProperties.writeEditableProperties(properties));
              } catch (final IOException ex) {
                log.error("Failed to write game properties", ex);
              }
            });
      }
    } catch (final IOException | ClassCastException ex) {
      log.error("Failed to read game properties", ex);
    }
  }
}
