package games.strategy.engine.framework.network.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;

/**
 * An action for changing game options across all network nodes from a client node.
 */
public class ChangeGameOptionsClientAction extends AbstractAction {
  private static final long serialVersionUID = -6419002646689952824L;
  private final Component parent;
  private final IServerStartupRemote serverRemote;

  public ChangeGameOptionsClientAction(final Component parent, final IServerStartupRemote serverRemote) {
    super("Edit Game Options");
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.serverRemote = serverRemote;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final byte[] oldBytes = serverRemote.getGameOptions();
    if ((oldBytes == null) || (oldBytes.length == 0)) {
      return;
    }
    try {
      final List<IEditableProperty> properties = GameProperties.readEditableProperties(oldBytes);
      final PropertiesUi pui = new PropertiesUi(properties, true);
      final JScrollPane scroll = new JScrollPane(pui);
      scroll.setBorder(null);
      scroll.getViewport().setBorder(null);
      final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
      final String ok = "OK";
      final String cancel = "Cancel";
      pane.setOptions(new Object[] {ok, cancel});
      final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(parent), "Map Options");
      window.setVisible(true);
      final Object buttonPressed = pane.getValue();
      if ((buttonPressed != null) && !buttonPressed.equals(cancel)) {
        // ok was clicked. changing them in the ui changes the underlying properties,
        // but it doesn't change the hosts, so we need to send it back to the host.
        try {
          serverRemote.changeToGameOptions(GameProperties.writeEditableProperties(properties));
        } catch (final IOException ex) {
          ClientLogger.logQuietly("Failed to write game properties", ex);
        }
      }
    } catch (final IOException | ClassCastException ex) {
      ClientLogger.logQuietly("Failed to read game properties", ex);
    }
  }
}
