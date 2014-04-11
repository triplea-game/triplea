package games.strategy.engine.framework.startup.ui;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.debug.Console;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.GameRunner2.ProxyChoice;
import games.strategy.engine.framework.ProcessRunnerUtil;
import games.strategy.engine.framework.TripleAProcessRunner;
import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.sound.SoundOptions;
import games.strategy.sound.SoundPath;
import games.strategy.ui.IntTextField;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Triple;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Class for holding various engine related options and preferences.
 * 
 * @author Veqryn
 * 
 */
public class EnginePreferences extends JDialog
{
	private static final long serialVersionUID = 5071190543005064757L;
	private final Frame m_parentFrame;
	private JButton m_okButton;
	private JButton m_lookAndFeel;
	private JButton m_gameParser;
	private JButton m_setupProxies;
	private JButton m_hostWaitTime;
	private JButton m_setMaxMemory;
	private JButton m_console;
	// private JButton m_runAutoHost;
	private JButton m_mapCreator;
	private JButton m_userFolder;
	private JButton m_programFolder;
	private JButton m_readme;
	
	private EnginePreferences(final Frame parentFrame)
	{
		super(parentFrame, "Edit TripleA Engine Preferences", true);
		this.m_parentFrame = parentFrame;
		createComponents();
		layoutCoponents();
		setupListeners();
		setWidgetActivation();
		// Listen for windowOpened event to set focus
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(final WindowEvent e)
			{
				m_okButton.requestFocus();
			}
		});
	}
	
	private void createComponents()
	{
		m_okButton = new JButton("OK");
		m_lookAndFeel = new JButton("Set Look And Feel");
		m_gameParser = new JButton("Enable/Disable Delayed Parsing of Game XML's");
		m_setupProxies = new JButton("Setup Network and Proxy Settings");
		m_hostWaitTime = new JButton("Set Max Host Wait Time for Clients and Observers");
		m_setMaxMemory = new JButton("Set Max Memory Usage");
		// m_runAutoHost = new JButton("Run an Automated Game Host Bot");
		m_mapCreator = new JButton("Run the Map Creator");
		m_console = new JButton("Show Console");
		m_userFolder = new JButton("Open User Maps and Savegames Folder");
		m_programFolder = new JButton("Open Installed Program Folder");
		m_readme = new JButton("Open Readme / User Manual");
	}
	
	private void layoutCoponents()
	{
		setLayout(new BorderLayout());
		final JPanel buttonsPanel = new JPanel();
		add(buttonsPanel, BorderLayout.CENTER);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.add(new JLabel("Change Engine Properties: "));
		buttonsPanel.add(new JLabel(" "));
		// add buttons here:
		SoundOptions.addGlobalSoundSwitchCheckbox(buttonsPanel);
		buttonsPanel.add(new JLabel(" "));
		SoundOptions.addToPanel(buttonsPanel, SoundPath.SoundType.TRIPLEA);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_lookAndFeel);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_gameParser);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_setupProxies);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_hostWaitTime);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_setMaxMemory);
		buttonsPanel.add(new JLabel(" "));
		// buttonsPanel.add(m_runAutoHost);
		// buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_mapCreator);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_console);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_userFolder);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_programFolder);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_readme);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
		final JPanel main = new JPanel();
		main.setBorder(new EmptyBorder(30, 30, 30, 30));
		main.setLayout(new BoxLayout(main, BoxLayout.X_AXIS));
		main.add(m_okButton);
		add(main, BorderLayout.SOUTH);
	}
	
	private void setupListeners()
	{
		m_okButton.addActionListener(new AbstractAction("OK")
		{
			private static final long serialVersionUID = 8014389179875584858L;
			
			public void actionPerformed(final ActionEvent e)
			{
				setVisible(false);
			}
		});
		m_lookAndFeel.addActionListener(new AbstractAction("Set Look And Feel")
		{
			private static final long serialVersionUID = -6524988343523615143L;
			
			public void actionPerformed(final ActionEvent event)
			{
				final Triple<JList, Map<String, String>, String> lookAndFeel = BasicGameMenuBar.getLookAndFeelList();
				final JList list = lookAndFeel.getFirst();
				final String currentKey = lookAndFeel.getThird();
				final Map<String, String> lookAndFeels = lookAndFeel.getSecond();
				if (JOptionPane.showConfirmDialog(m_parentFrame, list) == JOptionPane.OK_OPTION)
				{
					final String selectedValue = (String) list.getSelectedValue();
					if (selectedValue == null)
					{
						return;
					}
					if (selectedValue.equals(currentKey))
					{
						return;
					}
					GameRunner2.setDefaultLookAndFeel(lookAndFeels.get(selectedValue));
					EventThreadJOptionPane.showMessageDialog(m_parentFrame, "The look and feel will update when you restart TripleA", new CountDownLatchHandler(true));
				}
			}
		});
		m_gameParser.addActionListener(new AbstractAction("Enable/Disable Delayed Parsing of Game XML's")
		{
			private static final long serialVersionUID = -6223524865968800051L;
			
			public void actionPerformed(final ActionEvent e)
			{
				// TODO: replace with 2 radio buttons
				final boolean current = GameRunner2.getDelayedParsing();
				final Object[] options = { "Parse Selected", "Parse All", "Cancel" };
				final int answer = JOptionPane.showOptionDialog(m_parentFrame, new JLabel("<html>Delay Parsing of Game Data from XML until game is selected?" +
							"<br><br>'" + options[1] + "' means each map is fully parsed as TripleA starts (useful for testing to make sure all your maps are valid)." +
							"<br><br>Your current setting is: '" + (current ? options[0].toString() : options[1].toString()) + "'</html>"),
							"Select Parsing Method", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
				if (answer == JOptionPane.CANCEL_OPTION)
					return;
				final boolean delay = (answer == JOptionPane.YES_OPTION);
				if (delay == current)
					return;
				GameRunner2.setDelayedParsing(delay);
				EventThreadJOptionPane.showMessageDialog(m_parentFrame, "Please restart TripleA to avoid any potential errors", new CountDownLatchHandler(true));
			}
		});
		m_setupProxies.addActionListener(new AbstractAction("Setup Network and Proxy Settings")
		{
			private static final long serialVersionUID = 1673056396542959597L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
				final ProxyChoice proxyChoice = ProxyChoice.valueOf(pref.get(GameRunner2.PROXY_CHOICE, ProxyChoice.NONE.toString()));
				final String proxyHost = pref.get(GameRunner2.PROXY_HOST, "");
				final JTextField hostText = new JTextField(proxyHost);
				final String proxyPort = pref.get(GameRunner2.PROXY_PORT, "");
				final JTextField portText = new JTextField(proxyPort);
				final JRadioButton noneButton = new JRadioButton("None", proxyChoice == ProxyChoice.NONE);
				final JRadioButton systemButton = new JRadioButton("Use System Settings", proxyChoice == ProxyChoice.USE_SYSTEM_SETTINGS);
				final JRadioButton userButton = new JRadioButton("Use These User Settings:", proxyChoice == ProxyChoice.USE_USER_PREFERENCES);
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
				final Object[] options = { "Accept", "Cancel" };
				final int answer = JOptionPane.showOptionDialog(m_parentFrame, radioPanel, "Network Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (answer != JOptionPane.YES_OPTION)
					return;
				final ProxyChoice newChoice;
				if (systemButton.isSelected())
					newChoice = ProxyChoice.USE_SYSTEM_SETTINGS;
				else if (userButton.isSelected())
					newChoice = ProxyChoice.USE_USER_PREFERENCES;
				else
					newChoice = ProxyChoice.NONE;
				GameRunner2.setProxy(hostText.getText(), portText.getText(), newChoice);
			}
		});
		m_hostWaitTime.addActionListener(new AbstractAction("Set Max Host Wait Time for Clients and Observers")
		{
			private static final long serialVersionUID = 1262782782389758914L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final NumberProperty clientWait = new NumberProperty("Max seconds to wait for all clients to sync data on game start",
							"Max seconds to wait for all clients to sync data on game start", 3600, GameRunner2.DEFAULT_SERVER_START_GAME_SYNCE_WAIT_TIME, GameRunner2.getServerStartGameSyncWaitTime());
				final NumberProperty observerWait = new NumberProperty("Max seconds to wait for an observer joining a running game", "Max seconds to wait for an observer joining a running game",
							3600, GameRunner2.DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME, GameRunner2.getServerObserverJoinWaitTime());
				final List<IEditableProperty> list = new ArrayList<IEditableProperty>();
				list.add(clientWait);
				list.add(observerWait);
				final PropertiesUI ui = new PropertiesUI(list, true);
				final Object[] options = { "Accept", "Reset to Defaults", "Cancel" };
				final int answer = JOptionPane.showOptionDialog(m_parentFrame, ui, "Host Wait Settings", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
				if (answer == JOptionPane.YES_OPTION)
				{
					GameRunner2.setServerStartGameSyncWaitTime(clientWait.getValue());
					GameRunner2.setServerObserverJoinWaitTime(observerWait.getValue());
				}
				else if (answer == JOptionPane.NO_OPTION)
				{// reset
					GameRunner2.resetServerStartGameSyncWaitTime();
					GameRunner2.resetServerObserverJoinWaitTime();
				}
			}
		});
		m_setMaxMemory.addActionListener(new AbstractAction("Set Max Memory Usage")
		{
			private static final long serialVersionUID = 1262782782917758914L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final AtomicBoolean tested = new AtomicBoolean();
				tested.set(false);
				final Properties systemIni = GameRunner2.getSystemIni();
				final int currentSetting = GameRunner2.getMaxMemoryFromSystemIniFileInMB(systemIni);
				final boolean useDefault = GameRunner2.useDefaultMaxMemory(systemIni) || currentSetting <= 0;
				final int currentMaxMemoryInMB = (int) (GameRunner2.getMaxMemoryInBytes() / (1024 * 1024));
				final IntTextField newMaxMemory = new IntTextField(0, (1024 * 3), currentMaxMemoryInMB, 5);
				final JRadioButton noneButton = new JRadioButton("Use Default", useDefault);
				final JRadioButton userButton = new JRadioButton("Use These User Settings:", !useDefault);
				final ButtonGroup bgroup = new ButtonGroup();
				bgroup.add(noneButton);
				bgroup.add(userButton);
				final boolean onlineOnlyOriginalSetting = GameRunner2.getUseMaxMemorySettingOnlyForOnlineJoinOrHost(systemIni);
				final JCheckBox onlyOnlineCheckBox = new JCheckBox("Only use these user memory settings for online games (join/host). [Default = On]");
				onlyOnlineCheckBox.setSelected(onlineOnlyOriginalSetting);
				onlyOnlineCheckBox.setToolTipText("<html>If checked, only joining and hosting from online lobby will be affected by these settings."
							+ "<br />If unchecked, TripleA will automatically restart itself with the new memory setting every time you start TripleA.</html>");
				final JButton test = new JButton("Test User Settings");
				test.addActionListener(new AbstractAction("Test User Settings")
				{
					private static final long serialVersionUID = -4398183978989504112L;
					
					public void actionPerformed(final ActionEvent e)
					{
						tested.set(true);
						System.out.println("Testing TripleA launch with max memory of: " + newMaxMemory.getValue() + "m");
						TripleAProcessRunner.startNewTripleA((((long) newMaxMemory.getValue()) * 1024 * 1024) + 67108864); // it is in MB
					}
				});
				final JPanel radioPanel = new JPanel();
				radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
				radioPanel.add(new JLabel("<html>Configure TripleA's Maxmimum Memory Usage Settings: "
							+ "<br />(TripleA will only use 80-90% of this, the rest is used by Java VM)</html>"));
				radioPanel.add(new JLabel(" "));
				radioPanel.add(new JLabel("<html><b>WARNING: You could permanently stop TripleA from working if you mess with this! </b></html>"));
				radioPanel.add(new JLabel("<html><em><p>By default TripleA uses a bit less than 1gb of RAM memory, "
							+ "<br />and this is because on some computers Java can fail when greater than 1gb (1024mb). "
							+ "<br />The symptoms of this failing are: TripleA not starting, not being able to 'Join' or 'Host' "
							+ "<br />in the online lobby, and not being able to start the map creator. "
							+ "<br />For whatever max you set, Java requires you to have approximately double that much "
							+ "<br />free memory available, not being used by your operating system or other programs you are running. "
							+ "<br />Otherwise, TripleA will fail to start, and/or fail to join/host games online. "
							+ "<br />If you do mess this up, you can always run TripleA by command line with a different setting: "
							+ "<br />java -Xmx512m -classpath triplea.jar games.strategy.engine.framework.GameRunner triplea.memory.set=true"
							+ "<br />Or you can delete or change the 'system.ini' file located where TripleA was installed. </p>"
							+ "<br /><p>In order to make sure you do not mess this up, click the 'Test' button and make sure that "
							+ "<br />a new TripleA process is able to run with your new max memory setting. "
							+ "<br />If one does not run, you had better lower the setting or just use the default. </p></em></html>"));
				radioPanel.add(new JLabel(" "));
				radioPanel.add(onlyOnlineCheckBox);
				radioPanel.add(new JLabel(" "));
				radioPanel.add(noneButton);
				radioPanel.add(userButton);
				radioPanel.add(new JLabel("Maximum Memory (in MB): "));
				radioPanel.add(newMaxMemory);
				radioPanel.add(new JLabel(" "));
				radioPanel.add(new JLabel("<html>After clicking the 'Test' button, a new TripleA should launch. "
							+ "<br />If nothing launches, there is something wrong and you probably set the maximum too high. "
							+ "<br />You MUST test user settings before you use them! Otherwise the engine will discard changes. "
							+ "<br />TripleA has no way of knowing if this fails or succeeds, and there will not be an error message of any kind. </html>"));
				radioPanel.add(test);
				radioPanel.add(new JLabel(" "));
				final Object[] options = { "Accept", "Cancel" };
				final int answer = JOptionPane.showOptionDialog(m_parentFrame, radioPanel, "Max Memory Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							options, options[1]);
				if (answer != JOptionPane.YES_OPTION)
					return;
				if (noneButton.isSelected())
				{
					GameRunner2.clearMaxMemory();
				}
				else if (userButton.isSelected())
				{
					final boolean setOnlineOnly = onlineOnlyOriginalSetting != onlyOnlineCheckBox.isSelected();
					final boolean setMaxMemory = newMaxMemory.getValue() > 64 && tested.get();
					if (setOnlineOnly || setMaxMemory)
					{
						Properties prop;
						if (setMaxMemory)
							prop = GameRunner2.setMaxMemoryInMB(newMaxMemory.getValue());
						else
							prop = new Properties();
						GameRunner2.setUseMaxMemorySettingOnlyForOnlineJoinOrHost(onlyOnlineCheckBox.isSelected(), prop);
						GameRunner2.writeSystemIni(prop, false);
					}
				}
			}
		});
		/*
		m_runAutoHost.addActionListener(new AbstractAction("Run an Automated Game Host Bot")
		{
			private static final long serialVersionUID = -3582932190184978286L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final List<String> commands = new ArrayList<String>();
				ProcessRunnerUtil.populateBasicJavaArgs(commands, 1024 * 1024 * 64);
				final String javaClass = "games.strategy.engine.framework.HeadlessGameServer";
				commands.add(javaClass);
				commands.addAll(Arrays.asList(new String[] {
							"triplea.game.host.console=true", "triplea.game.host.ui=true", "triplea.game=", "triplea.server=true", "triplea.port=3300", "triplea.name=BotTest1",
							"triplea.lobby.game.hostedBy=BotTest1", "triplea.lobby.host=173.255.229.134", "triplea.lobby.port=3303", "triplea.lobby.game.comments=\"automated_hosting_service\""
				}));
				ProcessRunnerUtil.exec(commands);
			}
		});*/
		m_mapCreator.addActionListener(new AbstractAction("Run the Map Creator")
		{
			private static final long serialVersionUID = -3588932790184974286L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final List<String> commands = new ArrayList<String>();
				ProcessRunnerUtil.populateBasicJavaArgs(commands);
				final String javaClass = "util.image.MapCreator";
				commands.add(javaClass);
				ProcessRunnerUtil.exec(commands);
			}
		});
		m_console.addActionListener(new AbstractAction("Show Console")
		{
			private static final long serialVersionUID = 5333381081739176723L;
			
			public void actionPerformed(final ActionEvent e)
			{
				Console.getConsole().setVisible(true);
				reportMemoryUsageToConsole();
			}
		});
		m_userFolder.addActionListener(new AbstractAction("Open User Maps and Savegames Folder")
		{
			private static final long serialVersionUID = -3882256687728469915L;
			
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openFile(GameRunner2.getUserRootFolder());
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		m_programFolder.addActionListener(new AbstractAction("Open Installed Program Folder")
		{
			private static final long serialVersionUID = 3621594954694705711L;
			
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openFile(GameRunner2.getRootFolder());
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		m_readme.addActionListener(new AbstractAction("Open Readme / User Manual")
		{
			private static final long serialVersionUID = 7125025575496119585L;
			
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openFile(new File(GameRunner2.getRootFolder(), "readme.html"));
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
	}
	
	private void reportMemoryUsageToConsole()
	{
		final int mb = 1024 * 1024;
		final Runtime runtime = Runtime.getRuntime(); // Getting the runtime reference from system
		System.out.println("Heap utilization statistics [MB]");
		System.out.println("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / mb); // Print used memory
		System.out.println("Free Memory: " + runtime.freeMemory() / mb); // Print free memory
		System.out.println("Total Memory: " + runtime.totalMemory() / mb); // Print total available memory
		System.out.println("Max Memory: " + runtime.maxMemory() / mb); // Print Maximum available memory
		final int currentMaxSetting = GameRunner2.getMaxMemoryFromSystemIniFileInMB(GameRunner2.getSystemIni());
		if (currentMaxSetting > 0)
			System.out.println("Max Memory user setting within 20% of: " + currentMaxSetting);
	}
	
	private void setWidgetActivation()
	{
	}
	
	public static void showEnginePreferences(final JComponent parent)
	{
		final Frame parentFrame = JOptionPane.getFrameForComponent(parent);
		final EnginePreferences enginePrefs = new EnginePreferences(parentFrame);
		enginePrefs.pack();
		enginePrefs.setLocationRelativeTo(parentFrame);
		enginePrefs.setVisible(true);
	}
	
	public static void main(final String[] args)
	{
		showEnginePreferences(null);
	}
}
