package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import games.strategy.debug.ErrorConsole;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ProcessRunnerUtil;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.settings.SettingsWindow;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Triple;
import tools.map.making.MapCreator;
import tools.map.xml.creator.MapXmlCreator;

/**
 * Class for holding various engine related options and preferences.
 */
class EnginePreferences extends JDialog {
  private static final long serialVersionUID = 5071190543005064757L;
  private final Frame m_parentFrame;
  private JButton m_okButton;
  private JButton m_lookAndFeel;
  private JButton m_setupProxies;
  private JButton m_hostWaitTime;
  private JButton m_setMaxMemory;
  private JButton m_console;
  // private JButton m_runAutoHost;
  private JButton m_mapCreator;
  private JButton m_mapXmlCreator;

  private EnginePreferences(final Frame parentFrame) {
    super(parentFrame, "Edit TripleA Engine Preferences", true);
    this.m_parentFrame = parentFrame;
    createComponents();
    layoutCoponents();
    setupListeners();
    setWidgetActivation();
    // Listen for windowOpened event to set focus
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(final WindowEvent e) {
        m_okButton.requestFocus();
      }
    });
  }

  private void createComponents() {
    m_okButton = new JButton("OK");
    m_lookAndFeel = new JButton("Set Look And Feel");
    m_setupProxies = new JButton("Setup Network and Proxy Settings");
    m_hostWaitTime = new JButton("Set Max Host Wait Time for Clients and Observers");
    m_setMaxMemory = new JButton("Set Max Memory Usage");
    m_mapCreator = new JButton("Run the Map Creator");
    m_mapXmlCreator = new JButton("[Beta] Run the Map Creator");
    m_console = new JButton("Show Console");
  }

  private void layoutCoponents() {
    setLayout(new BorderLayout());
    final JPanel buttonsPanel = new JPanel();
    add(buttonsPanel, BorderLayout.CENTER);
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
    buttonsPanel.add(Box.createGlue());

    // add buttons here:
    SoundOptions.addGlobalSoundSwitchCheckbox(buttonsPanel);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(SwingComponents.newJButton("Engine Settings", e -> SettingsWindow.showWindow()));
    buttonsPanel.add(new JLabel(" "));
    SoundOptions.addToPanel(buttonsPanel);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(m_lookAndFeel);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(m_setupProxies);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(m_hostWaitTime);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(m_setMaxMemory);
    buttonsPanel.add(new JLabel(" "));

    buttonsPanel.add(m_mapCreator);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(m_mapXmlCreator);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(m_console);
    buttonsPanel.add(new JLabel(" "));
    buttonsPanel.add(Box.createGlue());
    buttonsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    final JPanel main = new JPanel();
    main.setBorder(new EmptyBorder(30, 30, 30, 30));
    main.setLayout(new BoxLayout(main, BoxLayout.X_AXIS));
    main.add(m_okButton);
    add(main, BorderLayout.SOUTH);
  }

  private void setupListeners() {
    m_okButton.addActionListener(SwingAction.of("OK", e -> setVisible(false)));
    final String lookAndFeelTitle = "Set Look And Feel";
    m_lookAndFeel.addActionListener(SwingAction.of(lookAndFeelTitle, e -> {
      final Triple<JList<String>, Map<String, String>, String> lookAndFeel = TripleAMenuBar.getLookAndFeelList();
      final JList<String> list = lookAndFeel.getFirst();
      final String currentKey = lookAndFeel.getThird();
      final Map<String, String> lookAndFeels = lookAndFeel.getSecond();
      if (JOptionPane.showConfirmDialog(m_parentFrame, list, lookAndFeelTitle,
          JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION) {
        final String selectedValue = list.getSelectedValue();
        if (selectedValue == null) {
          return;
        }
        if (selectedValue.equals(currentKey)) {
          return;
        }
        LookAndFeel.setDefaultLookAndFeel(lookAndFeels.get(selectedValue));
        EventThreadJOptionPane.showMessageDialog(m_parentFrame,
            "The look and feel has been applied. Please restart TripleA for it to take full effect",
            new CountDownLatchHandler(true));
      }
    }));
    m_setupProxies.addActionListener(SwingAction.of("Setup Network and Proxy Settings", e -> {
      // TODO: this action listener should probably come from the HttpProxy class
      final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
      final HttpProxy.ProxyChoice proxyChoice =
          HttpProxy.ProxyChoice.valueOf(pref.get(HttpProxy.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString()));
      final String proxyHost = pref.get(HttpProxy.PROXY_HOST, "");
      final JTextField hostText = new JTextField(proxyHost);
      final String proxyPort = pref.get(HttpProxy.PROXY_PORT, "");
      final JTextField portText = new JTextField(proxyPort);
      final JRadioButton noneButton = new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);
      final JRadioButton systemButton =
          new JRadioButton("Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
      final JRadioButton userButton =
          new JRadioButton("Use These User Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      final ButtonGroup bgroup = new ButtonGroup();
      bgroup.add(noneButton);
      bgroup.add(systemButton);
      bgroup.add(userButton);
      final JPanel radioPanel = new JPanel();
      radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
      radioPanel.add(new JLabel("Configure TripleA's Network and Proxy Settings: "));
      radioPanel.add(new JLabel("(This only effects Play-By-Forum games, dice servers, and map downloads.)"));
      radioPanel.add(noneButton);
      radioPanel.add(systemButton);
      radioPanel.add(userButton);
      radioPanel.add(new JLabel("Proxy Host: "));
      radioPanel.add(hostText);
      radioPanel.add(new JLabel("Proxy Port: "));
      radioPanel.add(portText);
      final Object[] options = {"Accept", "Cancel"};
      final int answer = JOptionPane.showOptionDialog(m_parentFrame, radioPanel, "Network Settings",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
      if (answer != JOptionPane.YES_OPTION) {
        return;
      }
      final HttpProxy.ProxyChoice newChoice;
      if (systemButton.isSelected()) {
        newChoice = HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS;
      } else if (userButton.isSelected()) {
        newChoice = HttpProxy.ProxyChoice.USE_USER_PREFERENCES;
      } else {
        newChoice = HttpProxy.ProxyChoice.NONE;
      }
      HttpProxy.setProxy(hostText.getText(), portText.getText(), newChoice);
    }));
    m_hostWaitTime.addActionListener(SwingAction.of("Set Max Host Wait Time for Clients and Observers", e -> {
      final NumberProperty clientWait =
          new NumberProperty("Max seconds to wait for all clients to sync data on game start",
              "Max seconds to wait for all clients to sync data on game start", 9999,
              GameRunner.MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME, GameRunner.getServerStartGameSyncWaitTime());
      final NumberProperty observerWait =
          new NumberProperty("Max seconds to wait for an observer joining a running game",
              "Max seconds to wait for an observer joining a running game", 9000,
              GameRunner.MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME, GameRunner.getServerObserverJoinWaitTime());
      final List<IEditableProperty> list = new ArrayList<>();
      list.add(clientWait);
      list.add(observerWait);
      final PropertiesUI ui = new PropertiesUI(list, true);
      final Object[] options = {"Accept", "Reset to Defaults", "Cancel"};
      final int answer = JOptionPane.showOptionDialog(m_parentFrame, ui, "Host Wait Settings",
          JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
      if (answer == JOptionPane.YES_OPTION) {
        GameRunner.setServerStartGameSyncWaitTime(clientWait.getValue());
        GameRunner.setServerObserverJoinWaitTime(observerWait.getValue());
      } else if (answer == JOptionPane.NO_OPTION) { // reset
        GameRunner.resetServerStartGameSyncWaitTime();
        GameRunner.resetServerObserverJoinWaitTime();
      }
    }));
    m_mapCreator
        .addActionListener(SwingAction.of("Run the Map Creator", e -> ProcessRunnerUtil.runClass(MapCreator.class)));
    m_mapXmlCreator.addActionListener(
        SwingAction.of("[Beta] Run the Map Creator", e -> ProcessRunnerUtil.runClass(MapXmlCreator.class)));
    m_console.addActionListener(SwingAction.of("Show Console", e -> {
      ErrorConsole.getConsole().setVisible(true);
      reportMemoryUsageToConsole();
    }));
  }

  private static void reportMemoryUsageToConsole() {
    final int mb = 1024 * 1024;
    final Runtime runtime = Runtime.getRuntime();
    System.out.println("Heap utilization statistics [MB]");
    System.out.println("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / mb);
    System.out.println("Free Memory: " + runtime.freeMemory() / mb);
    System.out.println("Total Memory: " + runtime.totalMemory() / mb);
    System.out.println("Max Memory: " + runtime.maxMemory() / mb);
  }

  private void setWidgetActivation() {}

  static void showEnginePreferences(final JComponent parent) {
    final Frame parentFrame = JOptionPane.getFrameForComponent(parent);
    final EnginePreferences enginePrefs = new EnginePreferences(parentFrame);
    enginePrefs.pack();
    enginePrefs.setLocationRelativeTo(parentFrame);
    enginePrefs.setVisible(true);
  }
}
