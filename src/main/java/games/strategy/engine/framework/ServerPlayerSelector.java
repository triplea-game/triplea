package games.strategy.engine.framework;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import games.strategy.debug.ClientLogger;

public class ServerPlayerSelector extends JFrame {
  private static final long serialVersionUID = 8902742935005060145L;
  private final Collection<PlayerChoice> m_playerChoices;
  private final Object m_lock = new Object();
  private Collection<String> m_remote;
  private final JTextField m_nameField;

  /** Creates a new instance of PlayerSelecter */
  public ServerPlayerSelector(final String[] players) {
    super("Choose players");
    final JPanel namePanel = new JPanel();
    namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    namePanel.add(new JLabel("Name:"));
    m_nameField = new JTextField();
    m_nameField.setColumns(10);
    namePanel.add(m_nameField);
    getContentPane().add(namePanel);
    getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getContentPane().add(new JLabel("Choose player location."));
    m_playerChoices = new ArrayList<>();
    for (final String player : players) {
      final PlayerChoice current = new PlayerChoice(player);
      m_playerChoices.add(current);
      getContentPane().add(current);
    }
    final WindowListener m_doneOnClose = new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        m_done.actionPerformed(null);
      }
    };
    this.addWindowListener(m_doneOnClose);
    getContentPane().add(new JButton(m_done));
    this.pack();
  }

  /**
   * Returns a collection of player names that are to be remote.
   * This method blocks the current thread, and should
   * not be called from the swing event thread.
   */
  public Collection<String> getRemotePlayers() {
    try {
      synchronized (m_lock) {
        this.setVisible(true);
        m_lock.wait();
      }
      if (m_remote == null) {
        return getRemotePlayers();
      } else {
        return m_remote;
      }
    } catch (final InterruptedException e) {
      ClientLogger.logQuietly(e);
      return getRemotePlayers();
    }
  }

  @Override
  public String getName() {
    return m_nameField.getText();
  }

  public AbstractAction m_done = new AbstractAction("done") {
    private static final long serialVersionUID = -6315754845837738689L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (!isVisible()) {
        return;
      }
      m_remote = new ArrayList<>();
      for (final PlayerChoice choice : m_playerChoices) {
        if (choice.isRemote()) {
          m_remote.add(choice.getPlayerName());
        }
      }
      synchronized (m_lock) {
        m_lock.notifyAll();
      }
      setVisible(false);
      dispose();
    }
  };
}


class PlayerChoice extends JPanel {
  private static final long serialVersionUID = 8059707233260441012L;
  private static final String REMOTE = "Remote";
  private static final String LOCAL = "Local";
  private static final String[] s_choices = {LOCAL, REMOTE};
  private final String m_name;
  private final JComboBox<String> m_choice;

  public PlayerChoice(final String name) {
    setLayout(new FlowLayout(FlowLayout.LEFT));
    m_name = name;
    add(new JLabel(name));
    m_choice = new JComboBox<>(s_choices);
    m_choice.setSelectedIndex(0);
    m_choice.setEditable(false);
    add(m_choice);
  }

  public String getPlayerName() {
    return m_name;
  }

  public boolean isRemote() {
    return s_choices[m_choice.getSelectedIndex()].equals(REMOTE);
  }
}
