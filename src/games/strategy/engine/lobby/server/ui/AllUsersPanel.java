package games.strategy.engine.lobby.server.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

public class AllUsersPanel extends JPanel {
  private static final long serialVersionUID = -9133556462653843231L;
  private final IMessenger m_messenger;
  private JList<INode> m_nodes;
  private DefaultListModel<INode> m_nodesModel;
  private LobbyAdminStatPanel m_statPane;
  private final List<INode> m_orderedNodes;

  public AllUsersPanel(final IMessenger messenger) {
    m_messenger = messenger;
    m_orderedNodes = new ArrayList<>();
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {
    m_nodesModel = new DefaultListModel<>();
    m_nodes = new JList<>(m_nodesModel);
    m_statPane = new LobbyAdminStatPanel(m_messenger);
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());
    add(new JScrollPane(m_nodes), BorderLayout.CENTER);
    add(m_statPane, BorderLayout.SOUTH);
  }

  private void setupListeners() {
    ((IServerMessenger) m_messenger).addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            m_orderedNodes.remove(to);
            refreshModel();
          }
        });
      }

      @Override
      public void connectionAdded(final INode to) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            m_orderedNodes.add(to);
            refreshModel();
          }
        });
      }
    });
  }

  private void refreshModel() {
    Collections.sort(m_orderedNodes);
    m_nodesModel.clear();
    for (final INode node : m_orderedNodes) {
      m_nodesModel.addElement(node);
    }
  }

  private void setWidgetActivation() {}
}
