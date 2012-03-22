package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.net.BareBonesBrowserLaunch;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.ui.TripleaMenu;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Triple;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Class for holding various engine related options and preferences.
 * 
 * @author Veqryn
 * 
 */
@SuppressWarnings("serial")
public class EnginePreferences extends JDialog
{
	private final Frame m_parentFrame;
	private JButton m_okButton;
	private JButton m_lookAndFeel;
	private JButton m_gameParser;
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
		SoundOptions.addToPanel(buttonsPanel);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_lookAndFeel);
		buttonsPanel.add(new JLabel(" "));
		buttonsPanel.add(m_gameParser);
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
			public void actionPerformed(final ActionEvent e)
			{
				setVisible(false);
			}
		});
		m_lookAndFeel.addActionListener(new AbstractAction()
		{
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
		m_donate.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					BareBonesBrowserLaunch.openURL("https://sourceforge.net/donate/index.php?group_id=44492");
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
