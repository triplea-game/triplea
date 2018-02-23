package games.strategy.triplea.ai.proAI.logging;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.ui.SwingAction;
import games.strategy.util.Interruptibles;

/**
 * GUI class used to display logging window and logging settings.
 */
class ProLogWindow extends JDialog {
  private static final long serialVersionUID = -5989598624017028122L;

  private JTextArea currentLogTextArea = null;
  private JTextArea aiOutputLogArea;
  private JCheckBox enableAiLogging;
  private JCheckBox limitLogHistoryCheckBox;
  private JSpinner limitLogHistoryToSpinner;
  private JComboBox<String> logDepth;
  private JTabbedPane logHolderTabbedPane;
  private JTabbedPane tabPaneMain;

  /** Creates new form ProLogWindow. */
  ProLogWindow(final TripleAFrame frame) {
    super(frame);
    initComponents();
  }

  void clear() {
    this.dispose();
    tabPaneMain = null;
    logHolderTabbedPane = null;
  }

  private void initComponents() {
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    final JPanel panel7 = new JPanel();
    final JButton restoreDefaultsButton = new JButton();
    final JButton settingsDetailsButton = new JButton();
    final JPanel panel14 = new JPanel();
    final JPanel panel13 = new JPanel();
    final JButton cancelButton = new JButton();
    final JButton okButton = new JButton();
    tabPaneMain = new JTabbedPane();
    final JPanel panel8 = new JPanel();
    logHolderTabbedPane = new JTabbedPane();
    final JPanel panel9 = new JPanel();
    final JScrollPane aiOutputLogAreaScrollPane = new JScrollPane();
    aiOutputLogArea = new JTextArea();
    enableAiLogging = new JCheckBox();
    final JLabel label15 = new JLabel();
    logDepth = new JComboBox<>();
    limitLogHistoryToSpinner = new JSpinner();
    limitLogHistoryCheckBox = new JCheckBox();
    final JLabel label46 = new JLabel();
    final JPanel pauseAIs = new JPanel();
    setTitle("Hard AI Settings");
    setMinimumSize(new Dimension(775, 400));
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent evt) {
        formWindowClosing();
      }

      @Override
      public void windowOpened(final WindowEvent evt) {
        formWindowOpened();
      }
    });
    getContentPane().setLayout(new GridBagLayout());
    panel7.setName("panel7");
    panel7.setPreferredSize(new Dimension(600, 45));
    panel7.setLayout(new GridBagLayout());
    restoreDefaultsButton.setText("Restore Defaults");
    restoreDefaultsButton.setMinimumSize(new Dimension(118, 23));
    restoreDefaultsButton.setName("restoreDefaultsButton");
    restoreDefaultsButton.setPreferredSize(new Dimension(118, 23));
    restoreDefaultsButton.addActionListener(evt -> restoreDefaultsButtonActionPerformed());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(11, 0, 11, 0);
    panel7.add(restoreDefaultsButton, gridBagConstraints);
    settingsDetailsButton.setText("Settings Details");
    settingsDetailsButton.setMinimumSize(new Dimension(115, 23));
    settingsDetailsButton.setName("settingsDetailsButton");
    settingsDetailsButton.setPreferredSize(new Dimension(115, 23));
    settingsDetailsButton.addActionListener(evt -> settingsDetailsButtonActionPerformed());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.insets = new Insets(11, 6, 11, 0);
    panel7.add(settingsDetailsButton, gridBagConstraints);
    panel14.setName("panel14");
    panel14.setLayout(new GridBagLayout());
    panel13.setName("panel13");
    panel13.setLayout(new GridBagLayout());
    cancelButton.setText("Cancel");
    cancelButton.setName("cancelButton");
    cancelButton.addActionListener(evt -> cancelButtonActionPerformed());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(0, 10, 0, 0);
    panel13.add(cancelButton, gridBagConstraints);
    okButton.setText("OK");
    okButton.setName("okButton");
    okButton.addActionListener(evt -> okButtonActionPerformed());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    panel13.add(okButton, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    panel14.add(panel13, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.insets = new Insets(11, 6, 11, 0);
    panel7.add(panel14, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.insets = new Insets(0, 7, 0, 7);
    getContentPane().add(panel7, gridBagConstraints);
    tabPaneMain.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    tabPaneMain.setName("tabPaneMain");
    tabPaneMain.setPreferredSize(new Dimension(500, screenSize.height - 200));
    panel8.setName("panel8");
    panel8.setPreferredSize(new Dimension(500, 314));
    panel8.setLayout(new GridBagLayout());
    logHolderTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    logHolderTabbedPane.setFont(new Font("Segoe UI", 0, 10));
    logHolderTabbedPane.setName("logHolderTabbedPane");
    panel9.setName("panel9");
    panel9.setLayout(new GridLayout(1, 0));
    aiOutputLogAreaScrollPane.setName("aiOutputLogAreaScrollPane");
    aiOutputLogArea.setColumns(20);
    aiOutputLogArea.setEditable(false);
    aiOutputLogArea.setFont(new Font("Segoe UI", 0, 10));
    aiOutputLogArea.setRows(5);
    aiOutputLogArea.setName("aiOutputLogArea");
    aiOutputLogAreaScrollPane.setViewportView(aiOutputLogArea);
    panel9.add(aiOutputLogAreaScrollPane);
    logHolderTabbedPane.addTab("Pre-Game", panel9);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 7;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.weighty = 99.0;
    gridBagConstraints.insets = new Insets(7, 7, 7, 7);
    panel8.add(logHolderTabbedPane, gridBagConstraints);
    enableAiLogging.setSelected(true);
    enableAiLogging.setText("Enable AI Logging");
    enableAiLogging.setName("enableAiLogging");
    enableAiLogging.addChangeListener(evt -> enableAiLoggingStateChanged());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(7, 7, 0, 0);
    panel8.add(enableAiLogging, gridBagConstraints);
    label15.setText("Log Depth:");
    label15.setName("label15");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    panel8.add(label15, gridBagConstraints);
    logDepth.setModel(new DefaultComboBoxModel<>(new String[] {"Fine", "Finer", "Finest"}));
    logDepth.setSelectedItem(logDepth.getItemAt(2));
    logDepth.setName("logDepth");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(7, 5, 0, 0);
    panel8.add(logDepth, gridBagConstraints);
    limitLogHistoryToSpinner.setModel(new SpinnerNumberModel(5, 1, 100, 1));
    limitLogHistoryToSpinner.setMinimumSize(new Dimension(60, 20));
    limitLogHistoryToSpinner.setName("limitLogHistoryToSpinner");
    limitLogHistoryToSpinner.setPreferredSize(new Dimension(60, 20));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.ipadx = 10;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    panel8.add(limitLogHistoryToSpinner, gridBagConstraints);
    limitLogHistoryCheckBox.setSelected(true);
    limitLogHistoryCheckBox.setText("Limit Log History To:");
    limitLogHistoryCheckBox.setName("limitLogHistoryCheckBox");
    limitLogHistoryCheckBox.addChangeListener(evt -> limitLogHistoryCbStateChanged());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(7, 0, 0, 12);
    panel8.add(limitLogHistoryCheckBox, gridBagConstraints);
    label46.setText("rounds");
    label46.setName("label46");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(7, 5, 0, 7);
    panel8.add(label46, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    panel8.add(pauseAIs, gridBagConstraints);
    tabPaneMain.addTab("Debugging", panel8);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 99.0;
    gridBagConstraints.weighty = 99.0;
    gridBagConstraints.insets = new Insets(7, 7, 0, 7);
    getContentPane().add(tabPaneMain, gridBagConstraints);
    setBounds((screenSize.width - 800), 25, 775, 401);
  }

  private void formWindowOpened() {
    loadSettings(ProLogSettings.loadSettings());
    this.pack();
  }

  /**
   * Loads the settings provided and displays it in this settings window.
   */
  private void loadSettings(final ProLogSettings settings) {
    enableAiLogging.setSelected(settings.EnableAILogging);
    if (settings.AILoggingDepth.equals(Level.FINE)) {
      logDepth.setSelectedIndex(0);
    } else if (settings.AILoggingDepth.equals(Level.FINER)) {
      logDepth.setSelectedIndex(1);
    } else if (settings.AILoggingDepth.equals(Level.FINEST)) {
      logDepth.setSelectedIndex(2);
    }
    limitLogHistoryCheckBox.setSelected(settings.LimitLogHistory);
    limitLogHistoryToSpinner.setValue(settings.LimitLogHistoryTo);
  }

  ProLogSettings createSettings() {
    final ProLogSettings settings = new ProLogSettings();
    settings.EnableAILogging = enableAiLogging.isSelected();
    if (logDepth.getSelectedIndex() == 0) {
      settings.AILoggingDepth = Level.FINE;
    } else if (logDepth.getSelectedIndex() == 1) {
      settings.AILoggingDepth = Level.FINER;
    } else if (logDepth.getSelectedIndex() == 2) {
      settings.AILoggingDepth = Level.FINEST;
    }
    settings.LimitLogHistory = limitLogHistoryCheckBox.isSelected();
    settings.LimitLogHistoryTo = Integer.parseInt(limitLogHistoryToSpinner.getValue().toString());
    return settings;
  }

  private void restoreDefaultsButtonActionPerformed() {
    final int result = JOptionPane.showConfirmDialog(rootPane, "Are you sure you want to reset all AI settings?",
        "Reset Default Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
      // Default settings are already contained in a new DSettings instance
      final ProLogSettings defaultSettings = new ProLogSettings();
      loadSettings(defaultSettings);
      JOptionPane.showMessageDialog(rootPane,
          "Default settings restored.\r\n\r\n(If you don't want to keep these default settings, just hit cancel)",
          "Default Settings Restored", JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private void enableAiLoggingStateChanged() {
    logDepth.setEnabled(enableAiLogging.isSelected());
    limitLogHistoryCheckBox.setEnabled(enableAiLogging.isSelected());
  }

  private void limitLogHistoryCbStateChanged() {
    limitLogHistoryToSpinner.setEnabled(limitLogHistoryCheckBox.isSelected() && enableAiLogging.isSelected());
  }

  private void formWindowClosing() {
    cancelButtonActionPerformed();
  }

  private void okButtonActionPerformed() {
    final ProLogSettings settings = createSettings();
    ProLogSettings.saveSettings(settings);
    this.setVisible(false);
  }

  private void cancelButtonActionPerformed() {
    final ProLogSettings settings = ProLogSettings.loadSettings();
    loadSettings(settings);
    this.setVisible(false);
  }

  private void settingsDetailsButtonActionPerformed() {
    final JDialog dialog = new JDialog(this, "Pro AI - Settings Details");
    String message = "";
    if (tabPaneMain.getSelectedIndex() == 0) { // Debugging
      message = "Debugging\r\n" + "\r\n"
          + "AI Logging: When this is checked, the AI's will output their logs, as they come in, so you can see "
          + "exactly what the AI is thinking.\r\n"
          + "Note that if you check this on, you still have to press OK then reopen the settings window for the logs "
          + "to actually start displaying.\r\n"
          + "\r\n"
          + "Log Depth: This setting lets you choose how deep you want the AI logging to be. Fine only displays the "
          + "high-level events, like the start of a phase, etc.\r\n"
          + "Finer displays medium-level events, such as attacks, reinforcements, etc.\r\n"
          + "Finest displays all the AI logging available. Can be used for detailed ananlysis, but is a lot harder to "
          + "read through it.\r\n"
          + "\r\n"
          + "Pause AI's: This checkbox pauses all the AI's while it's checked, so you can look at the logs without the "
          + "AI's outputing floods of information.\r\n"
          + "\r\n"
          + "Limit Log History To X Rounds: If this is checked, the AI log information will be limited to X rounds of "
          + "information.\r\n";
    }
    final JTextArea label = new JTextArea(message);
    label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    label.setEditable(false);
    label.setAutoscrolls(true);
    label.setLineWrap(false);
    label.setFocusable(false);
    label.setWrapStyleWord(true);
    label.setLocation(0, 0);
    dialog.setBackground(label.getBackground());
    dialog.setLayout(new BorderLayout());
    final JScrollPane pane = new JScrollPane();
    pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    pane.setViewportView(label);
    dialog.add(pane, BorderLayout.CENTER);
    final JButton button = new JButton("Close");
    button.setMinimumSize(new Dimension(100, 30));
    button.addActionListener(e -> dialog.dispose());
    dialog.add(button, BorderLayout.SOUTH);
    dialog.setMinimumSize(new Dimension(500, 300));
    dialog.setSize(new Dimension(800, 600));
    dialog.setResizable(true);
    dialog.setLocationRelativeTo(this);
    dialog.setDefaultCloseOperation(2);
    dialog.setVisible(true);
  }

  void addMessage(final Level level, final String message) {
    try {
      if (currentLogTextArea == null) {
        currentLogTextArea = aiOutputLogArea;
      }
      currentLogTextArea.append(message + "\r\n");
    } catch (final NullPointerException ex) { // This is bad, but we don't want TripleA crashing because of this...
      System.out.print("Error adding Pro log message! Level: " + level.getName() + " Message: " + message);
    }
  }

  void notifyNewRound(final int roundNumber, final String name) {
    Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
      final JPanel newPanel = new JPanel();
      final JScrollPane newScrollPane = new JScrollPane();
      final JTextArea newTextArea = new JTextArea();
      newTextArea.setColumns(20);
      newTextArea.setRows(5);
      newTextArea.setFont(new Font("Segoe UI", 0, 10));
      newTextArea.setEditable(false);
      newScrollPane.getHorizontalScrollBar().setEnabled(true);
      newScrollPane.setViewportView(newTextArea);
      newPanel.setLayout(new GridLayout());
      newPanel.add(newScrollPane);
      logHolderTabbedPane.addTab(Integer.toString(roundNumber) + "-" + name, newPanel);
      currentLogTextArea = newTextArea;
    }));
    // Now remove round logging that has 'expired'.
    // Note that this method will also trim all but the first and last log panels if logging is turned off
    // (We always keep first round's log panel, and we keep last because the user might turn logging back on in the
    // middle of the round)
    trimLogRoundPanels();
  }

  private void trimLogRoundPanels() {
    // If we're logging and we have trimming enabled, or if we have logging turned off
    if (!ProLogSettings.loadSettings().EnableAILogging || ProLogSettings.loadSettings().LimitLogHistory) {
      final int maxHistoryRounds;
      if (ProLogSettings.loadSettings().EnableAILogging) {
        maxHistoryRounds = ProLogSettings.loadSettings().LimitLogHistoryTo;
      } else {
        maxHistoryRounds = 1; // If we're not logging, trim to 1
      }
      Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
        for (int i = 0; i < logHolderTabbedPane.getTabCount(); i++) {
          // Remember, we never remove last tab, in case user turns logging back on in the middle of a round
          if ((i != 0) && (i < (logHolderTabbedPane.getTabCount() - maxHistoryRounds))) {
            // Remove the tab and decrease i by one, so the next component will be checked
            logHolderTabbedPane.removeTabAt(i);
            i--;
          }
        }
      }));
    }
  }
}
