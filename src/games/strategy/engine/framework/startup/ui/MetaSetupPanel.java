package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.mapDownload.DownloadMapDialog;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.net.BareBonesBrowserLaunch;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MetaSetupPanel extends SetupPanel
{
	private static final long serialVersionUID = 3926503672972937677L;
	private static final Logger s_logger = Logger.getLogger(MetaSetupPanel.class.getName());
	private static String s_serverPropertiesName = "server_" + EngineVersion.VERSION.toString() + ".properties";
	private JButton m_startLocal;
	private JButton m_startPBEM;
	private JButton m_hostGame;
	private JButton m_connectToHostedGame;
	private JButton m_connectToLobby;
	private JButton m_enginePreferences;
	private JButton m_downloadMaps;
	private JButton m_ruleBook;
	private JButton m_about;
	private final SetupPanelModel m_model;
	
	public MetaSetupPanel(final SetupPanelModel model)
	{
		m_model = model;
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void createComponents()
	{
		m_connectToLobby = new JButton("Play Online...");
		final Font bigButtonFont = new Font(m_connectToLobby.getFont().getName(), m_connectToLobby.getFont().getStyle(), m_connectToLobby.getFont().getSize() + 2);
		m_connectToLobby.setFont(bigButtonFont);
		m_connectToLobby.setToolTipText("<html>Find Games Online on the Lobby Server. <br>TripleA is MEANT to be played Online against other humans. <br>Any other way is not as fun!</html>");
		m_startLocal = new JButton("Start Local Game");
		m_startLocal.setToolTipText("<html>Start a game on this computer. <br>You can play against a friend sitting besides you (hotseat mode), <br>or against one of the AIs.</html>");
		m_startPBEM = new JButton("Start PBEM (Play-By-Email/Forum) Game");
		m_startPBEM.setToolTipText("<html>Starts a game which will be emailed back and forth between all players, <br>or be posted to an online forum or message board.</html>");
		m_hostGame = new JButton("Host Networked Game");
		m_hostGame.setToolTipText("<html>Hosts a network game, which people can connect to. <br>Anyone on a LAN will be able to connect. <br>Anyone from the internet can connect as well, but only if the host has configured port forwarding correctly.</html>");
		m_connectToHostedGame = new JButton("Connect to Networked Game");
		m_connectToHostedGame.setToolTipText("<html>Connects to someone's hosted game, <br>so long as you know their IP address.</html>");
		m_enginePreferences = new JButton("Engine Preferences...");
		m_enginePreferences.setToolTipText("<html>Configure certain options related to the engine.");
		m_downloadMaps = new JButton("Download Maps...");
		m_downloadMaps.setToolTipText("<html>Download new maps. Everyone should use this, <br>the best maps are online and have to be downloaded!</html>");
		m_ruleBook = new JButton("Rule Book...");
		m_ruleBook.setToolTipText("<html>Download a manual of how to play <br>(it is also included in the directory TripleA was installed to).</html>");
		m_about = new JButton("About...");
		m_about.setToolTipText("<html>See info about version number, developers, <br>official website, and a quick instruction on how to play.</html>");
	}
	
	private void layoutComponents()
	{
		setLayout(new GridBagLayout());
		// top space
		add(new JPanel(), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00, 0, 0, 0), 0, 0));
		add(m_connectToLobby, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_startLocal, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_startPBEM, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_hostGame, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_connectToHostedGame, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_enginePreferences, new GridBagConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_downloadMaps, new GridBagConstraints(0, 7, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_ruleBook, new GridBagConstraints(0, 8, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		add(m_about, new GridBagConstraints(0, 9, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		// top space
		add(new JPanel(), new GridBagConstraints(0, 100, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00, 0, 0, 0), 0, 0));
	}
	
	private void setupListeners()
	{
		m_startLocal.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_model.showLocal();
			}
		});
		m_startPBEM.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_model.showPBEM();
			}
		});
		m_hostGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_model.showServer(MetaSetupPanel.this);
			}
		});
		m_connectToHostedGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_model.showClient(MetaSetupPanel.this);
			}
		});
		m_connectToLobby.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				connectToLobby();
			}
		});
		m_enginePreferences.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				enginePreferences();
			}
		});
		m_downloadMaps.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				downloadMaps();
			}
		});
		m_ruleBook.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				ruleBook();
			}
		});
		m_about.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				about();
			}
		});
	}
	
	private void downloadMaps()
	{
		DownloadMapDialog.downloadGames(this);
	}
	
	private void ruleBook()
	{
		try
		{
			// We open both the actual rule book, and the web page for all guides.
			// This way we can add other guides and rulebooks and tutorials later, as well as being able to update them after the stable is out.
			BareBonesBrowserLaunch.openURL("http://triplea.sourceforge.net/TripleA_RuleBook.pdf");
			BareBonesBrowserLaunch.openURL("http://triplea.sourceforge.net/mywiki/Guides");
			// BareBonesBrowserLaunch.openURL("https://sourceforge.net/projects/triplea/files/help/");
		} catch (final Exception ex)
		{
			System.out.println("Error: " + ex); // print the error
		}
		/* we could also try opening the file on the computer, if triplea comes with it:
		class pdfopen           //class pdfopen
		{
		    public static void main(String args[])       //main function
		    {
		        try                                      //try statement
		        {
		            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + "c:\\chart.pdf");   //open the file chart.pdf
		
		        } catch (Exception e)                    //catch any exceptions here
		          {
		              System.out.println("Error" + e );  //print the error
		          }
		    }
		}
		
		// or...
		if (Desktop.isSupported()) {
		    try {
		        File myFile = new File("/path/to/file.pdf");
		        Desktop.getDesktop().open(myFile);
		    } catch (IOException ex) {
		        // no application registered for PDFs
		    }
		}

		 */
	}
	
	private void enginePreferences()
	{
		EnginePreferences.showEnginePreferences(this);
	}
	
	private void about()
	{
		final String text = "<h2>TripleA</h2>" + "<p><b>Engine Version:</b> " + games.strategy.engine.EngineVersion.VERSION.toString()
					+ "<br><b>Authors:</b> Sean Bridges, and many others. Current Developers: Veqryn (Chris Duncan)."
					+ "<br>TripleA is an open-source game engine, allowing people to play many different games and maps." + "<br>For more information please visit:<br>"
					+ "<b>Site:</b> <a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a><br>"
					+ "<b>Forum:</b> <a hlink='http://triplea.sourceforge.net/mywiki/Forum'>http://triplea.sourceforge.net/mywiki/Forum</a><br>"
					+ "<b>Ladder:</b> <a hlink='http://www.tripleawarclub.org/'>http://www.tripleawarclub.org/</a></p>" + "<p><b>Very Basic How to Play:</b>"
					+ "<br>Though some games have special rules enforced, most games follow most of these basic guidelines.<br><ol>"
					+ "<li>Players start their turn by choosing what they will produce.  They spend the money they gathered during their "
					+ "<br>last turn on new units or even technology.  Units are displayed on the purchase screen as having x Cost, and "
					+ "<br>their attack/defense/movement values.  These units will be put on the board at the end of the player's turn.</li>"
					+ "<li>That Player then does a <em>Combat Move</em>, which means moving units to all the places they wish to attack this "
					+ "<br>turn.  Simply click on a unit, then move your mouse to the territory you wish to attack, and then click again "
					+ "<br>to drop it there.  You can deselect a unit by right-clicking.  You can select a path for a unit to take by holding "
					+ "<br>down 'ctrl' and clicking on all the territories on the way to the final territory.  Pressing shift or ctrl while "
					+ "<br>selecting a unit will select all units in that territory.</li>"
					+ "<li>Then everyone resolves all the combat battles.  This involves rolling dice for the attacking units and the "
					+ "<br>defending units too.  For example, a <em>Tank</em> might attack at a <em>3</em> meaning that when you roll the dice you need "
					+ "<br>a 3 or less for him to <em>hit</em> the enemy.  If the tank hits the enemy, then the other player chooses one of his "
					+ "<br>units to die, and the battle continues.  After each round of dice, the attacker chooses to retreat or press on "
					+ "<br>until he has defeated all enemy units in that territory.  The game rolls the dice for you automatically.</li>"
					+ "<li>After this, the Player may move any units that have not yet moved as a <em>Non-Combat</em> move, and any air units " + "<br>return to friendly territories to land.</li>"
					+ "<li>When the player has completed all of this, then he or she may place the units that they have purchased at the "
					+ "<br>beginning of their turn.  Then the game engine counts out the value of the territories they control and gives "
					+ "<br>them that much money.  The next nation then begins their turn.  Games last until one side surrenders.</li></ol>"
					+ "To see specific rules for each game, click <em>Game Notes</em> from inside that game, "
					+ "<br> accessible from the <em>Help</em> menu button at the top of the screen inside a game.</p>";
		final JEditorPane editorPane = new JEditorPane();
		editorPane.setBorder(null);
		editorPane.setBackground(getBackground());
		editorPane.setEditable(false);
		editorPane.setContentType("text/html");
		editorPane.setText(text);
		final JScrollPane scroll = new JScrollPane(editorPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(null);
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(getParent()), editorPane, "About...", JOptionPane.PLAIN_MESSAGE);
	}
	
	private void connectToLobby()
	{
		LobbyServerProperties props = getLobbyServerProperties();
		if (props == null)
		{
			props = new LobbyServerProperties(null, -1, "Server Lookup failed, try again later", null);
		}
		// for development, ignore what we read,
		// connect instead to localhost
		if (System.getProperties().getProperty("triplea.lobby.debug") != null)
		{
			props = new LobbyServerProperties("127.0.0.1", 3304, "", "the server says");
		}
		final LobbyLogin login = new LobbyLogin(JOptionPane.getFrameForComponent(this), props);
		final LobbyClient client = login.login();
		if (client == null)
			return;
		final LobbyFrame lobbyFrame = new LobbyFrame(client, props);
		NewGameChooser.clearNewGameChooserModel();
		MainFrame.getInstance().setVisible(false);
		MainFrame.getInstance().dispose();
		lobbyFrame.setVisible(true);
	}
	
	private LobbyServerProperties getLobbyServerProperties()
	{
		// try to look up an override
		final File f = new File(GameRunner.getRootFolder(), "lobby.properties");
		if (f.exists())
		{
			final Properties props = new Properties();
			try
			{
				final FileInputStream fis = new FileInputStream(f);
				props.load(fis);
				return new LobbyServerProperties(props);
			} catch (final IOException e)
			{
				throw new IllegalStateException(e);
			}
		}
		final List<URL> serverPropsList = getServerLookupURL();
		if (serverPropsList == null || serverPropsList.isEmpty())
			throw new IllegalStateException("No Server Properties Found!");
		final Iterator<URL> iter = serverPropsList.iterator();
		LobbyServerProperties props = null;
		while (iter.hasNext() && (props == null || props.getPort() == -1))
		{
			props = contactServerForLobbyServerProperties(iter.next());
		}
		return props;
	}
	
	private LobbyServerProperties contactServerForLobbyServerProperties(final URL serverPropsURL)
	{
		if (s_logger.isLoggable(Level.FINE))
		{
			s_logger.log(Level.FINE, "lobby url:" + serverPropsURL);
		}
		// sourceforge sometimes takes a long while
		// to return results
		// so run a couple requests in parallell, starting
		// with delays to try and get
		// a response quickly
		final AtomicReference<LobbyServerProperties> ref = new AtomicReference<LobbyServerProperties>();
		final CountDownLatch latch = new CountDownLatch(1);
		final Runnable r = new Runnable()
		{
			public void run()
			{
				for (int i = 0; i < 5; i++)
				{
					spawnRequest(serverPropsURL, ref, latch);
					try
					{
						latch.await(2, TimeUnit.SECONDS);
					} catch (final InterruptedException e)
					{
						e.printStackTrace();
					}
					if (ref.get() != null)
						break;
				}
				// we have spawned a bunch of requests
				try
				{
					latch.await(15, TimeUnit.SECONDS);
				} catch (final InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			private void spawnRequest(final URL serverPropsURL, final AtomicReference<LobbyServerProperties> ref, final CountDownLatch latch)
			{
				final Thread t1 = new Thread(new Runnable()
				{
					public void run()
					{
						ref.set(new LobbyServerProperties(serverPropsURL));
						latch.countDown();
					}
				});
				t1.start();
			}
		};
		BackgroundTaskRunner.runInBackground(this, "Looking Up Server", r);
		final LobbyServerProperties props = ref.get();
		return props;
	}
	
	/**
	 * Get the url which we use to lookup the lobby server.
	 * 
	 * we look for a system property triplea.lobby.server.lookup.url, if that is not defined
	 * we default to looking on sourceforge, with a version dependent url
	 */
	private List<URL> getServerLookupURL()
	{
		final List<URL> rVal = new ArrayList<URL>();
		final String propertyString = System.getProperties().getProperty("triplea.lobby.server.lookup.url");
		if (propertyString != null)
		{
			try
			{
				final URL propertyURL = new URL(propertyString);
				rVal.add(propertyURL);
			} catch (final MalformedURLException e)
			{
				e.printStackTrace();
			}
			// if we set it as a system property, then we don't want to try other ones, only this one.
			return rVal;
		}
		try
		{
			// first is the default, rest are backups. Do NOT add too many backups, as we want to maintain control over all of them.
			rVal.add(new URL("http://triplea.sourceforge.net/lobby/" + s_serverPropertiesName));
			// rVal.add(new URL("http://tripleamaps.sourceforge.net/lobby/" + s_serverPropertiesName));
			rVal.add(new URL("http://www.tripleawarclub.org/lobby/" + s_serverPropertiesName));
		} catch (final MalformedURLException e)
		{
			e.printStackTrace();
		}
		return rVal;
	}
	
	@Override
	public void setWidgetActivation()
	{
		if (m_model == null || m_model.getGameSelectorModel() == null || m_model.getGameSelectorModel().getGameData() == null)
		{
			m_startLocal.setEnabled(false);
			m_startPBEM.setEnabled(false);
			m_hostGame.setEnabled(false);
		}
		else
		{
			m_startLocal.setEnabled(true);
			m_startPBEM.setEnabled(true);
			m_hostGame.setEnabled(true);
		}
	}
	
	@Override
	public boolean canGameStart()
	{
		// we cannot start
		return false;
	}
	
	@Override
	public void cancel()
	{
		// nothing to do
	}
}
