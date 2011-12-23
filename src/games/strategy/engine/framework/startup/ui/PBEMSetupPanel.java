package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.LocalBeanCache;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.engine.framework.startup.ui.editors.SelectAndViewEditor;
import games.strategy.engine.pbem.*;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.InternalDiceServer;
import games.strategy.engine.random.PBEMDiceRoller;
import games.strategy.engine.random.PropertiesDiceRoller;
import games.strategy.triplea.pbem.AxisAndAlliesForumPoster;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

/**
 * A panel for setting up Play by Email/Forum.
 * This panel listens to the GameSelectionModel so it can refresh when a new (save) game is loaded
 * The MainPanel also listens to this panel, and we notify it through the notifyObservers()
 */
public class PBEMSetupPanel extends SetupPanel implements Observer
{

	private static final String DICE_ROLLER = "games.strategy.engine.random.IRemoteDiceServer";

	private final GameSelectorModel m_gameSelectorModel;

	private SelectAndViewEditor m_diceServerEditor;
	private SelectAndViewEditor m_forumPosterEditor;
	private SelectAndViewEditor m_emailSenderEditor;

	public PBEMSetupPanel(final GameSelectorModel model)
	{
		m_gameSelectorModel = model;

		// register, so we get notified when the game model (GameData) changes (e.g if the user load a save game or selects another game)
		m_gameSelectorModel.addObserver(this);

		setLayout(new GridBagLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10)); // Empty border works as margin

		m_diceServerEditor = new SelectAndViewEditor("Dice Server");
		m_forumPosterEditor = new SelectAndViewEditor("Post to Forum");
		m_emailSenderEditor = new SelectAndViewEditor("Provider");

		int row = 0;
		add(m_diceServerEditor, new GridBagConstraints(0, row, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 1, 0), 0, 0));
		row++;

		// the play by Forum settings
		m_forumPosterEditor.setBorder(new TitledBorder("Play By Forum"));
		int panelRow = 0;
		add(m_forumPosterEditor, new GridBagConstraints(0, row, 2, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 1, 0), 0, 0));


		row++;
		panelRow = 0;
		JPanel emailPanel = new JPanel(new GridBagLayout());
		emailPanel.setBorder(new TitledBorder("Play By Email"));
		add(emailPanel, new GridBagConstraints(0, row, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));
		JTextArea emailInstructions = new JTextArea();
		emailInstructions.setText("Select an email provider if you want to receive an email with the turn summary and save game\n" +
				"You can provide a subject (player and round # is automatically added)\n" +
				"You can provide multiple email addresses separated by space");
		emailInstructions.setWrapStyleWord(true);
		emailInstructions.setLineWrap(true);
		emailPanel.add(emailInstructions, new GridBagConstraints(0, panelRow, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));
		panelRow++;
		emailPanel.add(m_emailSenderEditor, new GridBagConstraints(0, panelRow, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));



		setupListeners();
		if (m_gameSelectorModel.getGameData() != null)
		{
			loadAll();
		}
	}






	private void setupListeners()
	{
		// subscribe to editor changes, so we can notify the MainPanel
		m_diceServerEditor.addPropertyChangeListener(new NotifyingPropertyChangeListener());
		m_forumPosterEditor.addPropertyChangeListener(new NotifyingPropertyChangeListener());
		m_emailSenderEditor.addPropertyChangeListener(new NotifyingPropertyChangeListener());



	}

	private void loadAll()
	{
		loadDiceServer(m_gameSelectorModel.getGameData());
		loadForumPosters(m_gameSelectorModel.getGameData());
		loadEmailSender(m_gameSelectorModel.getGameData());
	}



	/**
	 * Loads the to/cc email and forum ID if the game is a save game
	 *
	 * @param data the game data
	 */
	private void loadDiceServer(final GameData data)
	{

		List<IRemoteDiceServer> diceRollers = new ArrayList<IRemoteDiceServer>(PropertiesDiceRoller.loadFromFile());
		diceRollers.add(new InternalDiceServer());

		for (IRemoteDiceServer diceRoller : diceRollers)
		{
			IRemoteDiceServer cached = (IRemoteDiceServer) LocalBeanCache.getInstance().getSerializable(diceRoller.getDisplayName());
			if (cached != null) {
				// Since dice rollers are serialized with their Properties, and the user may have changed these in the files
				// we only use the gameId and email from the cached version
				diceRoller.setCcAddress(cached.getCcAddress());
				diceRoller.setToAddress(cached.getToAddress());
				diceRoller.setGameId(cached.getGameId());
			}
		}

		m_diceServerEditor.setBeans(diceRollers);

		if (m_gameSelectorModel.isSavedGame())
		{
			// get the dice roller from the save game, if any
			IRemoteDiceServer roller = (IRemoteDiceServer) data.getProperties().get(DICE_ROLLER);
			if (roller != null)
			{
				m_diceServerEditor.setSelectedBean(roller);
			}
		}
	}


	/**
	 * Load the Forum poster that are stored in the GameData, and select it in the list.
	 * Sensitive information such as passwords are not stored in save games, so the are loaded from the local Serialization Cache
	 *
	 * @param data the game data
	 */
	private void loadForumPosters(final GameData data)
	{

		// get the forum posters,
		List<IForumPoster> forumPosters = new ArrayList<IForumPoster>();
		forumPosters.add((IForumPoster) findCachedOrCreateNew(NullForumPoster.class));
		forumPosters.add((IForumPoster) findCachedOrCreateNew(AxisAndAlliesForumPoster.class));
		m_forumPosterEditor.setBeans(forumPosters);

		// now get the poster stored in the save game
		IForumPoster forumPoster = (IForumPoster) data.getProperties().get(PBEMMessagePoster.FORUM_POSTER_PROP_NAME);
		if (forumPoster != null)
		{
			// if we have a cached version, use the credentials from this, as each player has different forum login
			IForumPoster cached = (IForumPoster) LocalBeanCache.getInstance().getSerializable(forumPoster.getClass().getCanonicalName());
			if (cached != null)
			{
				forumPoster.setUsername(cached.getUsername());
				forumPoster.setPassword(cached.getPassword());
			}
			m_forumPosterEditor.setSelectedBean(forumPoster);
		}
	}

	private void loadEmailSender(GameData data)
	{
		// The list of email, either loaded from cache or created
		List<IEmailSender> emailSenders = new ArrayList<IEmailSender>();
		emailSenders.add((IEmailSender) findCachedOrCreateNew(NullEmailSender.class));
		emailSenders.add((IEmailSender) findCachedOrCreateNew(GmailEmailSender.class));
		emailSenders.add((IEmailSender) findCachedOrCreateNew(HotmailEmailSender.class));
		emailSenders.add((IEmailSender) findCachedOrCreateNew(GenericEmailSender.class));
		m_emailSenderEditor.setBeans(emailSenders);

		// now get the sender from the save game, update it with credentials from the cache, and set it
		IEmailSender sender = (IEmailSender) data.getProperties().get(PBEMMessagePoster.EMAIL_SENDER_PROP_NAME);
		if (sender != null)
		{
			IEmailSender cached = (IEmailSender) LocalBeanCache.getInstance().getSerializable(sender.getClass().getCanonicalName());
			if (cached != null)
			{
				sender.setUserName(cached.getUserName());
				sender.setPassword(cached.getPassword());
			}
			m_emailSenderEditor.setSelectedBean(sender);
		}
	}

	/**
	 * finds a cached instance of the give type. If a cached version is not available a new one is created
	 * @param theClassType the type of class
	 * @return a IBean either loaded from the cache or created
	 */
	private IBean findCachedOrCreateNew(Class<? extends IBean> theClassType)
	{
		IBean cached = LocalBeanCache.getInstance().getSerializable(theClassType.getCanonicalName());
		if (cached == null) {
			try
			{
				cached = theClassType.newInstance();
			} catch (Exception e)
			{
				throw new RuntimeException("Bean of type " + theClassType + " doesn't have public default constructor, error: " + e.getMessage());
			}
		}
		return cached;
	}


	@Override
	public void cancel()
	{
		m_gameSelectorModel.deleteObserver(this);
	}

	@Override
	public boolean canGameStart()
	{
		boolean diceServerValid = m_diceServerEditor.isInputValid();
		boolean summaryValid = m_forumPosterEditor.isInputValid();
		boolean emailValid = m_emailSenderEditor.isInputValid();

		return diceServerValid &&
				summaryValid &&
				emailValid &&
				m_gameSelectorModel.getGameData() != null;

	}


	@Override
	public void postStartGame()
	{
		// // store the dice server
		final GameData data = m_gameSelectorModel.getGameData();
		data.getProperties().set(DICE_ROLLER, m_diceServerEditor.getBean());

		// store the Turn Summary Poster
		IForumPoster poster = (IForumPoster) m_forumPosterEditor.getBean();
		if (poster != null)
		{
			IForumPoster summaryPoster = poster;
			// clone the poster, the remove sensitive info, and put the clone into the game data
			// this was the sensitive info is not stored in the save game, but the user cache still has the password
			summaryPoster = summaryPoster.doClone();
			summaryPoster.clearSensitiveInfo();
			data.getProperties().set(PBEMMessagePoster.FORUM_POSTER_PROP_NAME, summaryPoster);
		}

		// store the email poster
		IEmailSender sender = (IEmailSender) m_emailSenderEditor.getBean();
		if (sender != null)
		{
			// create a clone, delete the sensitive information in the clone, and use it in the game
			// the locally cached version still has the password so the user doesn't have to enter it every time
			sender = sender.doClone();
			sender.clearSensitiveInfo();
			data.getProperties().set(PBEMMessagePoster.EMAIL_SENDER_PROP_NAME, sender);
		}
	}

	/**
	 * Is called in response to the GameSelectionModel being updated. It means the we have to reload the form
	 *
	 * @param o   always null
	 * @param arg always null
	 */
	public void update(final Observable o, final Object arg)
	{

		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					loadAll();
				}
			});

		} else
		{
			loadAll();
		}
	}


	@Override
	/**
	 * Called when the user hits play
	 */
	public ILauncher getLauncher()
	{
		// update local cache and write to disk before game starts
		IForumPoster poster = (IForumPoster) m_forumPosterEditor.getBean();
		if (poster != null)
		{
			LocalBeanCache.getInstance().storeSerializable(poster.getClass().getCanonicalName(), poster);
		}

		IEmailSender sender = (IEmailSender) m_emailSenderEditor.getBean();
		if (sender != null)
		{
			LocalBeanCache.getInstance().storeSerializable(sender.getClass().getCanonicalName(), sender);
		}

		IRemoteDiceServer server = (IRemoteDiceServer) m_diceServerEditor.getBean();
		LocalBeanCache.getInstance().storeSerializable(server.getDisplayName(), server);

		LocalBeanCache.getInstance().writeToDisk();

		// create local launcher
		final String gameUUID = (String) m_gameSelectorModel.getGameData().getProperties().get(GameData.GAME_UUID);
		final PBEMDiceRoller randomSource = new PBEMDiceRoller((IRemoteDiceServer) m_diceServerEditor.getBean(), gameUUID);
		final Map<String, String> playerTypes = new HashMap<String, String>();
		final String playerType = m_gameSelectorModel.getGameData().getGameLoader().getServerPlayerTypes()[0];
		for (final String playerName : m_gameSelectorModel.getGameData().getPlayerList().getNames())
		{
			playerTypes.put(playerName, playerType);
		}

 		return new LocalLauncher(m_gameSelectorModel, randomSource, playerTypes);
	}

	//-----------------------------------------------------------------------
	// inner classes
	//-----------------------------------------------------------------------

	/**
	 * A property change listener that notify our observers
	 */
	private class NotifyingPropertyChangeListener implements PropertyChangeListener
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			notifyObservers();
		}
	}


}
