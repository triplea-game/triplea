package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.GameRunner2.ProxyChoice;
import games.strategy.engine.framework.ProcessRunnerUtil;
import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.sound.SoundOptions;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.ui.TripleaMenu;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Triple;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
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
	private JButton m_mapCreator;
	private JButton m_userFolder;
	private JButton m_programFolder;
	private JButton m_readme;
	private JButton m_donate;
	
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
		m_lookAndFeel = new JButton("Set Look And Feel...");
		m_gameParser = new JButton("Enable/Disable Delayed Parsing of Game XML's");
		m_setupProxies = new JButton("Setup Network and Proxy Settings");
		m_mapCreator = new JButton("Run the Map Creator");
		m_userFolder = new JButton("Open User Maps and Savegames Folder");
		m_programFolder = new JButton("Open Installed Program Folder");
		m_readme = new JButton("Open Readme / User Manual");
		m_donate = new JButton("Donate...");
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
		buttonsPanel.add(m_mapCreator);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_userFolder);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_programFolder);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_readme);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_donate);
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
		m_okButton.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = 8014389079875584858L;
			
			public void actionPerformed(final ActionEvent e)
			{
				setVisible(false);
			}
		});
		m_lookAndFeel.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -6524988243523615143L;
			
			public void actionPerformed(final ActionEvent event)
			{
				final Triple<JList, Map<String, String>, String> lookAndFeel = TripleaMenu.getLookAndFeelList();
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
					EventThreadJOptionPane.showMessageDialog(m_parentFrame, "The look and feel will update when you restart TripleA");
				}
			}
		});
		m_gameParser.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -6223524855968800051L;
			
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
				EventThreadJOptionPane.showMessageDialog(m_parentFrame, "Please restart TripleA to avoid any potential errors");
			}
		});
		m_setupProxies.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = 1673056396342959597L;
			
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
		m_mapCreator.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = 1262782772917758914L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final List<String> commands = new ArrayList<String>();
				ProcessRunnerUtil.populateBasicJavaArgs(commands);
				final String javaClass = "util.image.MapCreator";
				commands.add(javaClass);
				ProcessRunnerUtil.exec(commands);
			}
		});
		m_userFolder.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openFile(GameRunner.getUserRootFolder());
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		m_programFolder.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openFile(GameRunner.getRootFolder());
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		m_readme.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openFile(new File(GameRunner.getRootFolder(), "readme.html"));
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		m_donate.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("https://sourceforge.net/donate/index.php?group_id=44492");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
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
