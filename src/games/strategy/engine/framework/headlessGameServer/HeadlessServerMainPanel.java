package games.strategy.engine.framework.headlessGameServer;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.MetaSetupPanel;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.triplea.ui.ErrorHandler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author veqryn
 * 
 */
public class HeadlessServerMainPanel extends JPanel implements Observer
{
	private static final long serialVersionUID = 1932202117432783020L;
	private JScrollPane m_gameSetupPanelScroll;
	private HeadlessGameSelectorPanel m_gameSelectorPanel;
	private JButton m_playButton;
	private JButton m_quitButton;
	// private JButton m_cancelButton;
	private final GameSelectorModel m_gameSelectorModel;
	private ISetupPanel m_gameSetupPanel;
	private JPanel m_gameSetupPanelHolder;
	private JPanel m_chatPanelHolder;
	private final SetupPanelModel m_gameTypePanelModel;
	private final JPanel m_mainPanel = new JPanel();
	private JSplitPane m_chatSplit;
	private static final Dimension m_initialSize = new Dimension(685, 620);
	private boolean m_isChatShowing;
	
	public HeadlessServerMainPanel(final SetupPanelModel typePanelModel, final AvailableGames availableGames)
	{
		m_gameTypePanelModel = typePanelModel;
		m_gameSelectorModel = typePanelModel.getGameSelectorModel();
		createComponents(availableGames);
		layoutComponents();
		setupListeners();
		setWidgetActivation();
		if (typePanelModel.getPanel() != null)
		{
			setGameSetupPanel(typePanelModel.getPanel());
		}
	}
	
	public JButton getDefaultButton()
	{
		return m_playButton;
	}
	
	private void createComponents(final AvailableGames availableGames)
	{
		m_playButton = new JButton("Play");
		m_quitButton = new JButton("Quit");
		// m_cancelButton = new JButton("Cancel");
		m_gameSelectorPanel = new HeadlessGameSelectorPanel(m_gameSelectorModel, availableGames);
		m_gameSelectorPanel.setBorder(new EtchedBorder());
		m_gameSetupPanelHolder = new JPanel();
		m_gameSetupPanelHolder.setLayout(new BorderLayout());
		m_gameSetupPanelScroll = new JScrollPane(m_gameSetupPanelHolder);
		m_gameSetupPanelScroll.setBorder(BorderFactory.createEmptyBorder());
		m_chatPanelHolder = new JPanel();
		m_chatPanelHolder.setLayout(new BorderLayout());
		m_chatSplit = new JSplitPane();
		m_chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
		m_chatSplit.setResizeWeight(0.8);
		m_chatSplit.setOneTouchExpandable(false);
		m_chatSplit.setDividerSize(5);
	}
	
	private void layoutComponents()
	{
		final JPanel buttonsPanel = new JPanel();
		buttonsPanel.setBorder(new EtchedBorder());
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonsPanel.add(m_playButton);
		buttonsPanel.add(m_quitButton);
		setLayout(new BorderLayout());
		m_mainPanel.setLayout(new GridBagLayout());
		m_mainPanel.setBorder(BorderFactory.createEmptyBorder());
		m_gameSetupPanelHolder.setLayout(new BorderLayout());
		m_mainPanel.add(m_gameSelectorPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(00, 0, 0, 0), 0, 0));
		m_mainPanel.add(m_gameSetupPanelScroll, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00, 0, 0, 0), 0, 0));
		addChat();
		add(buttonsPanel, BorderLayout.SOUTH);
		setPreferredSize(m_initialSize);
	}
	
	private void addChat()
	{
		remove(m_mainPanel);
		remove(m_chatSplit);
		m_chatPanelHolder.removeAll();
		final ChatPanel chat;
		if (m_gameTypePanelModel != null && m_gameTypePanelModel.getPanel() != null && m_gameTypePanelModel.getPanel().getChatPanel() != null
					&& m_gameTypePanelModel.getPanel().getChatPanel() instanceof ChatPanel)
		{
			chat = (ChatPanel) m_gameTypePanelModel.getPanel().getChatPanel();
			m_chatPanelHolder = new JPanel();
			m_chatPanelHolder.setLayout(new BorderLayout());
			m_chatPanelHolder.add(chat, BorderLayout.CENTER);
			m_chatSplit.setTopComponent(m_mainPanel);
			m_chatSplit.setBottomComponent(m_chatPanelHolder);
			add(m_chatSplit, BorderLayout.CENTER);
			m_chatPanelHolder.setPreferredSize(new Dimension(m_chatPanelHolder.getPreferredSize().width, 62));
		}
		else
		{
			chat = null;
			add(m_mainPanel, BorderLayout.CENTER);
		}
		m_isChatShowing = chat != null;
	}
	
	public void setGameSetupPanel(final ISetupPanel panel)
	{
		SetupPanel setupPanel = null;
		if (SetupPanel.class.isAssignableFrom(panel.getClass()))
			setupPanel = (SetupPanel) panel;
		if (m_gameSetupPanel != null)
		{
			m_gameSetupPanel.removeObserver(this);
			if (setupPanel != null)
				m_gameSetupPanelHolder.remove(setupPanel);
		}
		m_gameSetupPanel = panel;
		m_gameSetupPanelHolder.removeAll();
		if (setupPanel != null)
			m_gameSetupPanelHolder.add(setupPanel, BorderLayout.CENTER);
		panel.addObserver(this);
		setWidgetActivation();
		// add the cancel button if we are not choosing the type.
		if (!(panel instanceof MetaSetupPanel))
		{
			final JPanel cancelPanel = new JPanel();
			cancelPanel.setBorder(new EmptyBorder(10, 0, 10, 10));
			cancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			createUserActionMenu(cancelPanel);
			// cancelPanel.add(m_cancelButton);
			m_gameSetupPanelHolder.add(cancelPanel, BorderLayout.SOUTH);
		}
		final boolean panelHasChat = (m_gameTypePanelModel.getPanel().getChatPanel() != null);
		if (panelHasChat != m_isChatShowing)
			addChat();
		invalidate();
		revalidate();
	}
	
	private void createUserActionMenu(final JPanel cancelPanel)
	{
		if (m_gameSetupPanel.getUserActions() == null)
			return;
		// if we need this for something other than network, add a way to set it
		final JButton button = new JButton("Network...");
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final JPopupMenu menu = new JPopupMenu();
				final List<Action> actions = m_gameSetupPanel.getUserActions();
				if (actions != null && !actions.isEmpty())
				{
					for (final Action a : actions)
					{
						menu.add(a);
					}
				}
				menu.show(button, 0, button.getHeight());
			}
		});
		cancelPanel.add(button);
	}
	
	private void setupListeners()
	{
		m_gameTypePanelModel.addObserver(new Observer()
		{
			public void update(final Observable o, final Object arg)
			{
				setGameSetupPanel(m_gameTypePanelModel.getPanel());
			}
		});
		m_playButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				play();
			}
		});
		m_quitButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					m_gameSetupPanel.shutDown();
				} finally
				{
					System.exit(0);
				}
			}
		});
		/*m_cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_gameTypePanelModel.showSelectType();
			}
		});*/
		m_gameSelectorModel.addObserver(this);
	}
	
	private void play()
	{
		ErrorHandler.setGameOver(false);
		System.out.println("Starting Game: " + m_gameSelectorModel.getGameData().getGameName() + ", Round: " + m_gameSelectorModel.getGameData().getSequence().getRound());
		m_gameSetupPanel.preStartGame();
		final ILauncher launcher = m_gameTypePanelModel.getPanel().getLauncher();
		if (launcher != null)
			launcher.launch(this);
		m_gameSetupPanel.postStartGame();
	}
	
	private void setWidgetActivation()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setWidgetActivation();
				}
			});
			return;
		}
		m_gameTypePanelModel.setWidgetActivation();
		if (m_gameSetupPanel != null)
		{
			m_playButton.setEnabled(m_gameSetupPanel.canGameStart());
		}
		else
		{
			m_playButton.setEnabled(false);
		}
	}
	
	public void update(final Observable o, final Object arg)
	{
		setWidgetActivation();
	}
}
