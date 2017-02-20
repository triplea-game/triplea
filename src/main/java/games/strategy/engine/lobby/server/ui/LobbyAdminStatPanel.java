package games.strategy.engine.lobby.server.ui;

import java.awt.GridLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.triplea.ui.MemoryLabel;

public class LobbyAdminStatPanel extends JPanel {
  private static final long serialVersionUID = 3737079270721494810L;
  private JLabel m_upSince;
  private JLabel m_maxPlayersLabel;
  private JLabel m_totalLoginsLabel;
  private JLabel m_currentLoginsLabel;
  private int m_maxPlayers;
  private int m_totalLogins;
  private int m_currentLogins;
  private final IMessenger m_messenger;

  public LobbyAdminStatPanel(final IMessenger messenger) {
    m_messenger = messenger;
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {
    m_currentLoginsLabel = new JLabel("Current Players: -----");
    m_maxPlayersLabel = new JLabel("Max Concurrent Players : ----");
    m_totalLoginsLabel = new JLabel("Total Logins : ------");
    m_upSince = new JLabel("Up since " + new Date());
  }

  private void layoutComponents() {
    setLayout(new GridLayout(5, 1));
    add(m_currentLoginsLabel);
    add(m_totalLoginsLabel);
    add(m_maxPlayersLabel);
    add(m_upSince);
    add(new MemoryLabel());
  }

  private void setupListeners() {
    ((IServerMessenger) m_messenger).addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        SwingUtilities.invokeLater(() -> {
          m_currentLogins--;
          m_currentLoginsLabel.setText("Current Players: " + m_currentLogins);
        });
      }

      @Override
      public void connectionAdded(final INode to) {
        SwingUtilities.invokeLater(() -> {
          m_currentLogins++;
          m_currentLoginsLabel.setText("Current Players: " + m_currentLogins);
          if (m_currentLogins > m_maxPlayers) {
            m_maxPlayers = m_currentLogins;
            m_maxPlayersLabel.setText("Max Concurrent Players : " + m_maxPlayers);
          }
          m_totalLogins++;
          m_totalLoginsLabel.setText("Total Logins : " + m_totalLogins);
        });
      }
    });
  }

  private void setWidgetActivation() {}
}
