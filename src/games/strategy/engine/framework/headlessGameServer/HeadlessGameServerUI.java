package games.strategy.engine.framework.headlessGameServer;

import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.PlayerChatRenderer;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 
 * @author veqryn
 * 
 */
public class HeadlessGameServerUI extends MainGameFrame
{
	private static final long serialVersionUID = -3446398256211409031L;
	private GameData m_data;
	private IGame m_game;
	private IUIContext m_uiContext;
	private final boolean m_uiContextOriginallyNull;
	private JLabel m_gameName;
	private JLabel m_round;
	private JLabel m_step;
	private JLabel m_player;
	private JButton m_leaveGame;
	private JButton m_quit;
	private JPanel m_mapAndChatPanel;
	private JPanel m_mainPanel;
	private JSplitPane m_chatSplit;
	private ChatPanel m_chatPanel;
	
	public HeadlessGameServerUI(final IGame game, final LocalPlayers players, final IUIContext uiContext)
	{
		super("TripleA Headless Server - " + game.getData().getGameName(), players);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(WINDOW_LISTENER);
		m_game = game;
		m_data = game.getData();
		m_uiContext = uiContext;
		m_uiContextOriginallyNull = uiContext == null;
		m_gameName = new JLabel(m_data.getGameName());
		m_round = new JLabel("Round: -");
		m_step = new JLabel("Step: -");
		m_player = new JLabel("Player: -");
		m_leaveGame = new JButton("Leave Game");
		m_leaveGame.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = 699780405180654825L;
			
			public void actionPerformed(final ActionEvent e)
			{
				leaveGame();
			}
		});
		m_quit = new JButton("Quit Program");
		m_quit.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -3485076131973126967L;
			
			public void actionPerformed(final ActionEvent e)
			{
				shutdown();
			}
		});
		m_mapAndChatPanel = new JPanel();
		m_mapAndChatPanel.setLayout(new BorderLayout());
		m_mainPanel = new JPanel();
		m_mainPanel.setLayout(new BoxLayout(m_mainPanel, BoxLayout.Y_AXIS));
		m_mainPanel.add(m_gameName);
		m_mainPanel.add(m_round);
		m_mainPanel.add(m_step);
		m_mainPanel.add(m_player);
		m_chatSplit = new JSplitPane();
		m_chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
		m_chatSplit.setOneTouchExpandable(true);
		m_chatSplit.setDividerSize(8);
		m_chatSplit.setResizeWeight(0.3);
		if (HeadlessGameServer.getInstance() != null && HeadlessGameServer.getInstance().getChat() != null)
		{
			m_chatPanel = new ChatPanel(HeadlessGameServer.getInstance().getChat());
			m_chatPanel.setPlayerRenderer(new PlayerChatRenderer(m_game, m_uiContext));
			final Dimension chatPrefSize = new Dimension((int) m_chatPanel.getPreferredSize().getWidth(), 95);
			m_chatPanel.setPreferredSize(chatPrefSize);
			m_chatSplit.setTopComponent(m_mainPanel);
			m_chatSplit.setBottomComponent(m_chatPanel);
			m_mapAndChatPanel.add(m_chatSplit, BorderLayout.CENTER);
		}
		else
		{
			m_chatPanel = null;
			m_mapAndChatPanel.add(m_mainPanel, BorderLayout.CENTER);
		}
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(m_mapAndChatPanel, BorderLayout.CENTER);
		final JPanel buttonPanel = new JPanel();
		buttonPanel.add(m_leaveGame);
		buttonPanel.add(m_quit);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		m_data.addDataChangeListener(m_dataChangeListener);
		game.addGameStepListener(m_stepListener);
		updateStep();
		if (m_uiContext != null)
			m_uiContext.addShutdownWindow(this);
	}
	
	private WindowListener WINDOW_LISTENER = new WindowAdapter()
	{
		@Override
		public void windowClosing(final WindowEvent e)
		{
			leaveGame();
		}
	};
	
	GameStepListener m_stepListener = new GameStepListener()
	{
		public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String stepDisplayName)
		{
			updateStep();
		}
	};
	
	GameDataChangeListener m_dataChangeListener = new GameDataChangeListener()
	{
		public void gameDataChanged(final Change change)
		{
			try
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						if (m_uiContext == null)
							return;
						// stuff goes below here:
					}
				});
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	};
	
	private void updateStep()
	{
		final IUIContext context = m_uiContext;
		if (!m_uiContextOriginallyNull && (context == null || context.isShutDown()))
			return;
		m_data.acquireReadLock();
		try
		{
			if (m_data.getSequence().getStep() == null)
				return;
		} finally
		{
			m_data.releaseReadLock();
		}
		// we need to invoke and wait here since
		// if we switch to the history as a result of a history
		// change, we need to ensure that no further history
		// events are run until our historySynchronizer is set up
		if (!SwingUtilities.isEventDispatchThread())
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						updateStep();
					}
				});
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			} catch (final InvocationTargetException e)
			{
				e.getCause().printStackTrace();
				throw new IllegalStateException(e.getCause().getMessage());
			}
			return;
		}
		int round;
		String stepDisplayName;
		PlayerID player;
		m_data.acquireReadLock();
		try
		{
			round = m_data.getSequence().getRound();
			stepDisplayName = m_data.getSequence().getStep().getDisplayName();
			player = m_data.getSequence().getStep().getPlayerID();
		} finally
		{
			m_data.releaseReadLock();
		}
		m_round.setText("Round: " + round);
		m_step.setText("Step: " + stepDisplayName);
		if (player == null)
			m_player.setText("Player: -");
		else
			m_player.setText("Player: " + player.getName());
	}
	
	public IUIContext getUIContext()
	{
		return m_uiContext;
	}
	
	@Override
	public IGame getGame()
	{
		return m_game;
	}
	
	@Override
	public void leaveGame()
	{
		final int rVal = EventThreadJOptionPane.showConfirmDialog(this, "Are you sure you want to leave?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION,
					(m_uiContext == null ? new CountDownLatchHandler(true) : m_uiContext.getCountDownLatchHandler()));
		if (rVal != JOptionPane.OK_OPTION)
			return;
		if (m_game instanceof ServerGame)
		{
			((ServerGame) m_game).stopGame();
		}
		else
			throw new IllegalStateException("Should be leaving a server game.");
	}
	
	@Override
	public void stopGame()
	{
		// we have already shut down
		if (!m_uiContextOriginallyNull && m_uiContext == null)
			return;
		this.dispose();
		this.setVisible(false);
		if (GameRunner.isMac())
		{
			// this frame should not handle shutdowns anymore
			MacWrapper.unregisterShutdownHandler();
		}
		if (m_uiContext != null)
			m_uiContext.shutDown();
		if (m_chatPanel != null)
		{
			m_chatPanel.setPlayerRenderer(null);
			m_chatPanel.setChat(null);
		}
		m_game.removeGameStepListener(m_stepListener);
		m_game = null;
		m_uiContext = null;
		if (m_data != null)
			m_data.clearAllListeners();
		m_data = null;
		m_gameName = null;
		m_round = null;
		m_step = null;
		m_player = null;
		m_leaveGame = null;
		m_quit = null;
		m_mapAndChatPanel = null;
		m_mainPanel = null;
		m_chatSplit = null;
		m_chatPanel = null;
		m_stepListener = null;
		m_dataChangeListener = null;
		m_localPlayers = null;
		removeWindowListener(WINDOW_LISTENER);
		WINDOW_LISTENER = null;
	}
	
	@Override
	public void shutdown()
	{
		final int rVal = EventThreadJOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION,
					(m_uiContext == null ? new CountDownLatchHandler(true) : m_uiContext.getCountDownLatchHandler()));
		if (rVal != JOptionPane.OK_OPTION)
			return;
		stopGame();
		System.exit(0);
	}
	
	@Override
	public void notifyError(final String error)
	{
		System.out.println(error);
	}
	
	@Override
	public JComponent getMainPanel()
	{
		return m_mainPanel;
	}
	
	@Override
	public void setShowChatTime(final boolean showTime)
	{
		m_chatPanel.setShowChatTime(showTime);
	}
}
