/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * TripleAFrame.java
 *
 * Created on November 5, 2001, 1:32 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.PlayerBridge;
import games.strategy.engine.message.*;

import games.strategy.engine.data.properties.PropertiesUI;

import games.strategy.ui.*;
import games.strategy.util.*;
import games.strategy.net.*;

import games.strategy.triplea.*;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.image.*;
import games.strategy.triplea.delegate.message.*;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.delegate.*;
import games.strategy.engine.history.*;
import games.strategy.triplea.ui.history.*;
import games.strategy.engine.gamePlayer.*;

import games.strategy.engine.random.RandomStats;
import games.strategy.engine.random.RandomStatsMessage;
import games.strategy.engine.sound.ClipPlayer; //the player
import games.strategy.triplea.sound.SoundPath; //the relative path of sounds

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * Main frame for the triple a game
 */
public class TripleAFrame extends JFrame
{
    private final GameData m_data;

    private final IGame m_game;

    private MapPanel m_mapPanel;

    private MapPanelSmallView m_smallView;

    private JLabel m_message = new JLabel("No selection");

    private JLabel m_step = new JLabel("xxxxxx");

    private JLabel m_round = new JLabel("xxxxxx");

    private ActionButtons m_actionButtons;

    //a set of TripleAPlayers
    private Set m_localPlayers;

    private JPanel m_gameMainPanel = new JPanel();

    private JPanel m_rightHandSidePanel = new JPanel();

    private JTabbedPane m_tabsPanel = new JTabbedPane();

    private StatPanel m_statsPanel;

    private TerritoryDetailPanel m_details;

    private JPanel m_historyPanel = new JPanel();

    private JPanel m_gameSouthPanel;

    private HistoryPanel m_historyTree;

    private boolean m_inHistory = false;

    private HistorySynchronizer m_historySyncher;

    /** Creates new TripleAFrame */
    public TripleAFrame(IGame game, Set players) throws IOException
    {
        super("TripleA");

        FlagIconImageFactory.instance().load(this);
        setIconImage(GameRunner.getGameIcon(this));

        m_game = game;

        game.getMessenger().addErrorListener(m_messengerErrorListener);

        m_data = game.getData();
        m_localPlayers = players;

        this.addWindowListener(WINDOW_LISTENER);

        createMenuBar();

        TerritoryData.getInstance().verify(m_data);
        MapImage.getInstance().loadMaps(m_data);

        Image small = MapImage.getInstance().getSmallMapImage();
        m_smallView = new MapPanelSmallView(small);

        Image large = MapImage.getInstance().getLargeMapImage();
        m_mapPanel = new MapPanel(large, m_data, m_smallView);
        m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);

        //link the small and large images
        new ImageScrollControl(m_mapPanel, m_smallView);

        m_gameMainPanel.setLayout(new BorderLayout());

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);

        m_gameSouthPanel = new JPanel();
        m_gameSouthPanel.setLayout(new BorderLayout());
        m_gameSouthPanel.add(m_message, BorderLayout.CENTER);
        m_message.setBorder(new EtchedBorder(EtchedBorder.RAISED));

        JPanel stepPanel = new JPanel();
        stepPanel.setLayout(new GridBagLayout());
        stepPanel.add(m_step, new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0,
                        0, 0, 0), 0, 0));
        stepPanel.add(m_round, new GridBagConstraints(1, 0, 1, 1, 0, 0,
                GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0,
                        0, 0, 0), 0, 0));

        m_step.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_round.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_step.setHorizontalTextPosition(SwingConstants.LEADING);

        m_gameSouthPanel.add(stepPanel, BorderLayout.EAST);

        m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);

        JPanel gameCenterPanel = new JPanel();
        gameCenterPanel.setLayout(new BorderLayout());

        JPanel mapBorderPanel = new JPanel();
        mapBorderPanel.setLayout(new BorderLayout());
        //    mapBorderPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        mapBorderPanel.setBorder(null);
        mapBorderPanel.add(m_mapPanel, BorderLayout.CENTER);

        gameCenterPanel.add(mapBorderPanel, BorderLayout.CENTER);

        m_rightHandSidePanel.setLayout(new BorderLayout());
        m_rightHandSidePanel.add(m_smallView, BorderLayout.NORTH);

        m_tabsPanel.setBorder(null);
        m_rightHandSidePanel.add(m_tabsPanel, BorderLayout.CENTER);

        m_actionButtons = new ActionButtons(m_data, m_mapPanel, this);
        m_tabsPanel.addTab("Actions", m_actionButtons);
        m_actionButtons.setBorder(null);

        m_statsPanel = new StatPanel(m_data);
        m_tabsPanel.addTab("Stats", m_statsPanel);

        m_details = new TerritoryDetailPanel(m_mapPanel, m_data);
        m_tabsPanel.addTab("Territory", m_details);

        m_rightHandSidePanel.setPreferredSize(new Dimension((int) m_smallView
                .getPreferredSize().getWidth(), (int) m_mapPanel
                .getPreferredSize().getHeight()));
        gameCenterPanel.add(m_rightHandSidePanel, BorderLayout.EAST);

        m_gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);

        game.addGameStepListener(m_stepListener);
        updateStep();

        //there are a lot of images that can be gcd right now
        System.gc();
    }

    private void shutdown()
    {
        m_game.shutdown();
        System.exit(0);
    }

    private void createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        // menuFileSave = new JMenuItem("Save", KeyEvent.VK_S);
        JMenuItem menuFileSave = new JMenuItem(new AbstractAction("Save...")
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!m_game.canSave())
                {
                    JOptionPane
                            .showMessageDialog(
                                    TripleAFrame.this,
                                    "You cannot save the game if you are playing as a client",
                                    "Cant save", JOptionPane.OK_OPTION);
                    return;
                }

                GameDataManager manager = new GameDataManager();

                try
                {
                    JFileChooser fileChooser = SaveGameFileChooser
                            .getInstance();

                    int rVal = fileChooser.showSaveDialog(TripleAFrame.this);
                    if (rVal == JFileChooser.APPROVE_OPTION)
                    {
                        File f = fileChooser.getSelectedFile();

                        //A small warning so users will not over-write a file,
                        // added by NeKromancer
                        if (f.exists())
                        {
                            int choice = JOptionPane
                                    .showConfirmDialog(
                                            TripleAFrame.this,
                                            "A file by that name already exists. Do you wish to over write it?",
                                            "Over-write?",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.WARNING_MESSAGE);
                            if (choice != JOptionPane.OK_OPTION)
                            {
                                return;
                            }
                        }//end if exists

                        if (!f.getName().toLowerCase().endsWith(".svg"))
                        {
                            f = new File(f.getParent(), f.getName() + ".svg");
                        }

                        manager.saveGame(f, m_data);
                        JOptionPane.showMessageDialog(TripleAFrame.this,
                                "Game Saved", "Game Saved",
                                JOptionPane.INFORMATION_MESSAGE);
                    }

                } catch (Exception se)
                {
                    se.printStackTrace();
                    JOptionPane.showMessageDialog(TripleAFrame.this, se
                            .getMessage(), "Error Saving Game",
                            JOptionPane.OK_OPTION);
                }
            }
        });
        menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.CTRL_MASK));

        fileMenu.add(menuFileSave);

        JMenuItem menuFileExit = new JMenuItem(new AbstractAction("Exit")
        {
            public void actionPerformed(ActionEvent e)
            {
                shutdown();
            }
        });
        menuFileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                ActionEvent.CTRL_MASK));
        fileMenu.add(menuFileExit);

        JMenu menuGame = new JMenu("Game");
        menuGame.add(m_showGameAction);
        menuGame.add(m_showHistoryAction);

        Action showVerifiedDice = new AbstractAction("Show  Verified Dice..")
        {
            public void actionPerformed(ActionEvent e)
            {
                new VerifiedRandomNumbersDialog(TripleAFrame.this.getRootPane())
                        .show();
            }
        };

        menuBar.add(menuGame);

        // Put in unit size menu
        menuGame.add(getUnitSizeMenu());

        //beagles Mapskin code
        //creates a sub menu of radiobuttons for each available mapdir

        JMenuItem mapMenuItem;
        menuGame.addSeparator();
        JMenu mapSubMenu = new JMenu("Map Skins");
        ButtonGroup mapButtonGroup = new ButtonGroup();

        // Create A String array of compatible MapDirs

        final String currentMapSubDir = TerritoryImageFactory.getMapDir();
        final File mapsDir = new File(System.getProperty("user.dir")
                + "/../classes/games/strategy/triplea/image/"
                + Constants.MAP_DIR);

        if (currentMapSubDir != null)
        {
            //Filter only MapDirs that start with the originals name

            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File mapsDir, String name)
                {
                    if (name.startsWith(currentMapSubDir))
                    {
                        File file = new File(mapsDir + "/" + name);
                        return file.isDirectory();
                    }
                    return (false);
                }
            };
            String[] mapDirs = mapsDir.list(filter);

            //create entry for each mapdir
            String mapMenuItemName;
            for (int i = 0; i < mapDirs.length; i++)
            {
                mapMenuItemName = mapDirs[i].replaceFirst(currentMapSubDir, "");
                if (mapMenuItemName.length() == 0)
                {
                    //	        mapMenuItemName="Original";
                    mapMenuItem = new JRadioButtonMenuItem("Original");
                    mapMenuItem.setSelected(true);
                } else
                {
                    mapMenuItem = new JRadioButtonMenuItem(mapMenuItemName);
                }

                // add item to button group and sub menu

                mapButtonGroup.add(mapMenuItem);
                mapSubMenu.add(mapMenuItem);

                //add the listening code

                mapMenuItem.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        if (e.getActionCommand() == "Original")
                        {
                            try
                            {
                                UpdateMap(currentMapSubDir);
                            } catch (Exception se)
                            {
                                se.printStackTrace();
                                JOptionPane.showMessageDialog(
                                        TripleAFrame.this, se.getMessage(),
                                        "Error Changing Map Skin",
                                        JOptionPane.OK_OPTION);
                            }
                        } else
                        {
                            try
                            {
                                UpdateMap(currentMapSubDir
                                        + e.getActionCommand());
                            } catch (Exception se)
                            {
                                se.printStackTrace();
                                JOptionPane.showMessageDialog(
                                        TripleAFrame.this, se.getMessage(),
                                        "Error Changing Map Skin2",
                                        JOptionPane.OK_OPTION);
                            }

                        }//else

                    }//actionPerformed
                });

            }//for

        }//if

        // add the sub menu to the menu
        menuGame.add(mapSubMenu);

        final JCheckBox soundCheckBox = new JCheckBox("Enable Sound");

        soundCheckBox.setSelected(!ClipPlayer.getInstance().getBeSilent());
        //temporarily disable sound

        soundCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ClipPlayer.getInstance().setBeSilent(
                        !soundCheckBox.isSelected());
            }
        });

        final JCheckBox showMapDetails = new JCheckBox("Show Map Details");

        showMapDetails.setSelected(TerritoryImageFactory.getShowReliefImages());
        //temporarily disable sound

        showMapDetails.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {

                TerritoryImageFactory.setShowReliefImages(showMapDetails
                        .isSelected());
                Thread t = new Thread()
                {
                    public void run()
                    {
                        yield();
                        m_mapPanel.updateCounties(m_data.getMap()
                                .getTerritories());

                    }
                };
                t.start();

            }
        });

        if (!m_game.getData().getProperties().getEditableProperties().isEmpty())
        {
            AbstractAction optionsAction = new AbstractAction(
                    "View Game Options...")
            {
                public void actionPerformed(ActionEvent e)
                {
                    PropertiesUI ui = new PropertiesUI(m_game.getData()
                            .getProperties(), false);
                    JOptionPane.showMessageDialog(TripleAFrame.this, ui,
                            "Game options", JOptionPane.PLAIN_MESSAGE);
                }
            };
            menuGame.addSeparator();

            menuGame.add(optionsAction);

        }

        menuGame.add(soundCheckBox);

        if (!m_data.getProperties().get(Constants.FOURTH_EDITION, false))
            menuGame.add(showMapDetails);

        if (m_game instanceof ClientGame)
            menuGame.add(showVerifiedDice);

        final JCheckBox showEnemyCasualties = new JCheckBox(
                "Confirm Enemy Casualties");
        showEnemyCasualties.setSelected(BattleDisplay
                .getShowEnemyCasualtyNotification());
        showEnemyCasualties.addActionListener(new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                BattleDisplay
                        .setShowEnemyCasualtyNotification(showEnemyCasualties
                                .isSelected());
            }
        });

        menuGame.add(showEnemyCasualties);

        Action showDiceStats = new AbstractAction("Show Dice Stats")
        {
            public void actionPerformed(ActionEvent e)
            {
                RandomStatsMessage stats = (RandomStatsMessage) m_game
                        .getMessageManager().send(null,
                                RandomStats.RANDOM_STATS_DESTINATION);
                JPanel panel = new JPanel();
                BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
                panel.setLayout(layout);

                Iterator iter = new TreeSet(stats.getData().keySet())
                        .iterator();
                while (iter.hasNext())
                {
                    Object key = iter.next();
                    int value = stats.getData().getInt(key);
                    JLabel label = new JLabel(key + " was rolled " + value
                            + " times");
                    panel.add(label);
                }
                panel.add(new JLabel("  "));
                DecimalFormat format = new DecimalFormat("#0.000");
                panel.add(new JLabel("Average roll is :"
                        + format.format(stats.getAverage())));

                JOptionPane.showMessageDialog(TripleAFrame.this, panel,
                        "Random Stats", JOptionPane.INFORMATION_MESSAGE);

            }
        };
        menuGame.add(showDiceStats);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        helpMenu.add(new AbstractAction("About...")
        {
            public void actionPerformed(ActionEvent e)
            {
                String text = "<h2>TripleA</h2>  "
                        +

                        "<b>Engine Version:</b> "
                        + games.strategy.engine.EngineVersion.VERSION
                                .toString()
                        + "<br>"
                        + "<b>Game:</b> "
                        + m_data.getGameName()
                        + "<br>"
                        + "<b>Game Version:</b>"
                        + m_data.getGameVersion()
                        + "<br>"
                        + "<br>"
                        + "For more information please visit, <p>"
                        +

                        "<b><a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a></b><p>";

                JEditorPane editorPane = new JEditorPane();
                editorPane.setBorder(null);
                editorPane.setBackground(getBackground());
                editorPane.setEditable(false);
                editorPane.setContentType("text/html");
                editorPane.setText(text);

                JScrollPane scroll = new JScrollPane(editorPane);
                scroll.setBorder(null);

                JOptionPane.showMessageDialog(TripleAFrame.this, editorPane,
                        "About", JOptionPane.PLAIN_MESSAGE);
            }
        });

        helpMenu.add(new AbstractAction("Hints...")
        {
            public void actionPerformed(ActionEvent e)
            {
                //html formatted string
                String hints = "To force a path while moving, right click on each territory in turn.<br><br>"
                        + "You may be able to set game properties such as a bid in the Properties tab at game start up.";
                JEditorPane editorPane = new JEditorPane();
                editorPane.setEditable(false);
                editorPane.setContentType("text/html");
                editorPane.setText(hints);

                JScrollPane scroll = new JScrollPane(editorPane);

                JOptionPane.showMessageDialog(TripleAFrame.this, scroll,
                        "Hints", JOptionPane.PLAIN_MESSAGE);
            }
        });

        //allow the game developer to write notes that appear in the game
        //displays whatever is in the notes field in html
        final String notes = (String) m_data.getProperties().get("notes");
        if (notes != null && notes.trim().length() != 0)
        {

            helpMenu.add(new AbstractAction("Game Notes...")
            {
                public void actionPerformed(ActionEvent e)
                {

                    JEditorPane editorPane = new JEditorPane();
                    editorPane.setEditable(false);
                    editorPane.setContentType("text/html");
                    editorPane.setText(notes);

                    JScrollPane scroll = new JScrollPane(editorPane);

                    JOptionPane.showMessageDialog(TripleAFrame.this, scroll,
                            "Notes", JOptionPane.PLAIN_MESSAGE);
                }
            });

        }

        this.setJMenuBar(menuBar);
    }

    // Beagle Code Called to Change Mapskin
    private void UpdateMap(String mapdir) throws IOException
    {

        TerritoryData.setMapDir(mapdir); // set mapdir
        TerritoryImageFactory.setMapDir(mapdir);

        MapImage.getInstance().loadMaps(m_data); // load map data
        m_mapPanel.setGameData(m_data);

        // update mappanels to use new image
        Image large = MapImage.getInstance().getLargeMapImage();
        m_mapPanel.changeImage(large);

        Image small = MapImage.getInstance().getSmallMapImage();
        m_smallView.changeImage(small);

        m_mapPanel.initTerritories(); // redraw territories
    }

    private final WindowListener WINDOW_LISTENER = new WindowAdapter()
    {
        public void windowClosing(WindowEvent e)
        {
            shutdown();
        }
    };

    public final MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
    {
        Territory in;

        public void mouseEntered(Territory territory)
        {
            in = territory;
            refresh();
        }

        void refresh()
        {
            StringBuffer buf = new StringBuffer();
            buf.append(in == null ? "none" : in.getName());
            if (in != null)
            {
                TerritoryAttatchment ta = TerritoryAttatchment.get(in);
                if (ta != null)
                {
                    int production = ta.getProduction();
                    if (production > 0)
                        buf.append(" production:" + production);
                }
            }
            m_message.setText(buf.toString());
        }
    };

    public IntegerMap getProduction(PlayerID player, boolean bid)
    {
        m_actionButtons.changeToProduce(player);
        return m_actionButtons.waitForPurchase(bid);
    }

    public MoveDescription getMove(PlayerID player, PlayerBridge bridge,
            boolean nonCombat)
    {
        m_actionButtons.changeToMove(player, nonCombat);
        return m_actionButtons.waitForMove(bridge);
    }

    public PlaceData waitForPlace(PlayerID player, boolean bid,
            PlayerBridge bridge)
    {
        m_actionButtons.changeToPlace(player);
        return m_actionButtons.waitForPlace(bid, bridge);
    }

    public Message listBattle(BattleStepMessage msg)
    {
        return m_actionButtons.listBattle(msg);
    }

    public FightBattleMessage getBattle(PlayerID player, Collection battles,
            Collection bombingRaids)
    {
        m_actionButtons.changeToBattle(player, battles, bombingRaids);
        return m_actionButtons.waitForBattleSelection();
    }

    public BombardmentSelectMessage getBombardment(BombardmentQueryMessage msg)
    {
        return m_actionButtons.getBombardment(msg);
    }

    public SelectCasualtyMessage getCasualties(SelectCasualtyQueryMessage msg)
    {
        return m_actionButtons.getCasualties(msg);
    }

    public Message battleStringMessage(BattleStringMessage message)
    {
        return m_actionButtons.battleStringMessage(message);
    }

    public void casualtyNotificationMessage(CasualtyNotificationMessage message)
    {
        m_actionButtons.casualtyNoticicationMessage(message);
    }

    public RetreatMessage getRetreat(RetreatQueryMessage rqm)
    {
        return m_actionButtons.getRetreat(rqm);
    }

    public void notifyRetreat(RetreatNotificationMessage msg)
    {
        m_actionButtons.notifyRetreat(msg);
    }

    public Message battleInfo(BattleInfoMessage msg)
    {
        return m_actionButtons.battleInfo(msg);
    }

    public Message battleStartMessage(BattleStartMessage msg)
    {
        return m_actionButtons.battleStartMessage(msg);
    }

    public void notifyError(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public void battleEndMessage(BattleEndMessage message)
    {
        m_actionButtons.battleEndMessage(message);
    }

    public void bombingResults(BombingResults message)
    {
        m_actionButtons.bombingResults(message);
    }

    public void notifyMessage(String message)
    {
        JOptionPane.showMessageDialog(this, message, message,
                JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean getOKToLetAirDie(String message)
    {
        String ok = "Kill air";
        String cancel = "Keep moving";
        String[] options = { cancel, ok };
        int choice = JOptionPane.showOptionDialog(this, message,
                "Air cannot land", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, cancel);
        return choice == 1;
    }

    public boolean getOK(String message)
    {
        int choice = JOptionPane.showConfirmDialog(this, message, message,
                JOptionPane.OK_CANCEL_OPTION);
        return choice == JOptionPane.OK_OPTION;
    }

    public void notifyTechResults(TechResults msg)
    {
        TechResultsDisplay display = new TechResultsDisplay(msg);
        JOptionPane.showOptionDialog(this, display, "Tech roll",
                JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new String[] { "OK" }, "OK");

    }

    public boolean getStrategicBombingRaid(StrategicBombQuery query)
    {
        String message = "Bomb in " + query.getLocation().getName();
        String bomb = "Bomb";
        String normal = "Attack";
        String[] choices = { bomb, normal };
        int choice = JOptionPane.showOptionDialog(this, message, "Bomb?",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, choices, bomb);
        return choice == 0;
    }

    public LandAirMessage getLandAir(LandAirQueryMessage msg)
    {
        Collection territories = msg.getTerritories();
        JList list = new JList(new Vector(territories));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(list);
        panel.add(scroll, BorderLayout.CENTER);

        String[] options = { "OK" };
        String message = "Select territory for air units to land";

        JOptionPane.showOptionDialog(this, panel, message,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                options, null);

        Territory selected = (Territory) list.getSelectedValue();

        return new LandAirMessage(selected);
    }

    public TechRoll getTechRolls(PlayerID id)
    {
        m_actionButtons.changeToTech(id);
        return m_actionButtons.waitForTech();
    }

    public TerritoryMessage getRocketAttack(RocketAttackQuery msg)
    {
        Collection territories = msg.getTerritories();
        JList list = new JList(new Vector(territories));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(list);
        panel.add(scroll, BorderLayout.CENTER);

        if (msg.getFrom() != null)
        {
            panel.add(BorderLayout.NORTH, new JLabel("Targets for rocket in "
                    + msg.getFrom().getName()));
        }

        String[] options = { "OK", "Dont attack" };
        String message = "Select Rocket Target";

        int selection = JOptionPane.showOptionDialog(this, panel, message,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                options, null);

        Territory selected = null;
        if (selection == 0) //OK
            selected = (Territory) list.getSelectedValue();

        return new TerritoryMessage(selected);
    }

    public boolean playing(PlayerID id)
    {
        if (id == null)
            return false;

        Iterator iter = m_localPlayers.iterator();
        while (iter.hasNext())
        {
            GamePlayer gamePlayer = (GamePlayer) iter.next();
            if (gamePlayer.getID().equals(id)
                    && gamePlayer instanceof TripleAPlayer)
            {
                return true;
            }
        }
        return false;
    }

    private IMessengerErrorListener m_messengerErrorListener = new IMessengerErrorListener()
    {
        public void connectionLost(INode node, Exception reason,
                java.util.List unsent)
        {
            String message = "Connection lost to " + node.getName()
                    + ". Game over.";
            JOptionPane.showMessageDialog(TripleAFrame.this, message, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        public void messengerInvalid(IMessenger messenger, Exception reason,
                java.util.List unsent)
        {
            String message = "Network connection lost. Game over.";
            JOptionPane.showMessageDialog(TripleAFrame.this, message, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    };

    public static int save(String filename, GameData m_data)
    {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try
        {
            fos = new FileOutputStream(filename);
            oos = new ObjectOutputStream(fos);

            oos.writeObject(m_data);

            return 0;
        } catch (Throwable t)
        {
            // t.printStackTrace();
            System.err.println(t.getMessage());
            return -1;
        } finally
        {
            try
            {
                fos.flush();
            } catch (Exception ignore)
            {
            }
            try
            {
                oos.close();
            } catch (Exception ignore)
            {
            }
        }
    }

    GameStepListener m_stepListener = new GameStepListener()
    {

        public void gameStepChanged(String stepName, String delegateName,
                PlayerID player, int round, String stepDisplayName)
        {
            updateStep();
        }
    };

    private void updateStep()
    {
        if (m_data.getSequence().getStep() == null)
            return;

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateStep();
                }
            });
            return;
        }

        int round = m_data.getSequence().getRound();
        String stepDisplayName = m_data.getSequence().getStep()
                .getDisplayName();
        PlayerID player = m_data.getSequence().getStep().getPlayerID();

        m_round.setText("Round:" + round + " ");
        m_step.setText(stepDisplayName);
        if (player != null && !player.isNull())
            m_round.setIcon(new ImageIcon(FlagIconImageFactory.instance()
                    .getFlag(player)));

        //if the game control has passed to someone else
        //show the history
        if (player != null && !player.isNull() && !playing(player)
                && !m_inHistory)
        {
            if (SwingUtilities.isEventDispatchThread())
                showHistory();
            else
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        showHistory();
                    }
                });
            }
        }
        //if the game control is with us
        //show the current game
        else if (player != null && !player.isNull() && playing(player)
                && m_inHistory)
        {
            showGame();
            ClipPlayer.getInstance().playClip(SoundPath.START_TURN,
                    SoundPath.class); //play sound
        }

    }

    public void showHistory()
    {
        m_inHistory = true;
        setWidgetActivation();

        //we want to use a clone of the data, so we can make changes to it
        GameData clonedGameData = cloneGameData();
        if (clonedGameData == null)
            return;

        clonedGameData.getAllianceTracker();
        m_historySyncher = new HistorySynchronizer(clonedGameData, m_game);

        m_statsPanel.setGameData(clonedGameData);
        m_details.setGameData(clonedGameData);
        m_mapPanel.setGameData(clonedGameData);

        HistoryDetailsPanel historyDetailPanel = new HistoryDetailsPanel(
                clonedGameData, m_mapPanel);

        m_tabsPanel.removeAll();
        m_tabsPanel.add("History", historyDetailPanel);
        m_tabsPanel.add("Stats", m_statsPanel);
        m_tabsPanel.add("Territory", m_details);

        if (m_actionButtons.getCurrent() != null)
            m_actionButtons.getCurrent().setActive(false);

        m_historyPanel.removeAll();
        m_historyPanel.setLayout(new BorderLayout());

        JSplitPane split = new JSplitPane();
        m_historyTree = new HistoryPanel(clonedGameData, historyDetailPanel);
        split.setLeftComponent(m_historyTree);
        split.setRightComponent(m_mapPanel);
        split.setDividerLocation(150);

        m_historyPanel.add(split, BorderLayout.CENTER);
        m_historyPanel.add(m_rightHandSidePanel, BorderLayout.EAST);
        m_historyPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);

        getContentPane().removeAll();
        getContentPane().add(m_historyPanel, BorderLayout.CENTER);
        validate();
    }

    /**
     * Create a deep copy of GameData
     */
    private GameData cloneGameData()
    {
        try
        {
            GameDataManager manager = new GameDataManager();
            ByteArrayOutputStream sink = new ByteArrayOutputStream(10000);
            manager.saveGame(sink, m_data, false);
            sink.close();
            ByteArrayInputStream source = new ByteArrayInputStream(sink
                    .toByteArray());
            sink = null;
            return manager.loadGame(source);
        } catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    public void showGame()
    {
        m_inHistory = false;

        if (m_historySyncher != null)
            m_historySyncher.deactivate();

        setWidgetActivation();

        m_historyTree.goToEnd();
        m_historyTree = null;

        m_statsPanel.setGameData(m_data);
        m_details.setGameData(m_data);
        m_mapPanel.setGameData(m_data);

        m_tabsPanel.removeAll();
        m_tabsPanel.add("Action", m_actionButtons);
        m_tabsPanel.add("Territory", m_details);
        m_tabsPanel.add("Stats", m_statsPanel);
        if (m_actionButtons.getCurrent() != null)
            m_actionButtons.getCurrent().setActive(true);

        m_gameMainPanel.removeAll();
        m_gameMainPanel.setLayout(new BorderLayout());
        m_gameMainPanel.add(m_mapPanel, BorderLayout.CENTER);
        m_gameMainPanel.add(m_rightHandSidePanel, BorderLayout.EAST);
        m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);

        getContentPane().removeAll();
        getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);

        m_mapPanel.setRoute(null);

        validate();
    }

    private void setWidgetActivation()
    {
        m_showHistoryAction.setEnabled(!m_inHistory);
        m_showGameAction.setEnabled(m_inHistory);
    }

    private AbstractAction m_showHistoryAction = new AbstractAction(
            "Show history")
    {
        public void actionPerformed(ActionEvent e)
        {
            showHistory();
        };
    };

    private AbstractAction m_showGameAction = new AbstractAction(
            "Show current game")
    {
        {
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            showGame();
        };
    };

    public IntegerMessage moveFightersToCarrier(
            MoveFightersToNewCarrierMessage msg)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        ScrollableTextField text = new ScrollableTextField(0, msg
                .getNumberOfFighters());
        text.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.add(text, BorderLayout.CENTER);
        panel.add(new JLabel("How many fighters do you want to move from "
                + msg.getTerritory().getName() + " to new carrier?"),
                BorderLayout.NORTH);

        int choice = JOptionPane.showOptionDialog(this, panel,
                "Place fighters on new carrier?", JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, null, new String[] { "OK",
                        "Cancel" }, "OK");
        if (choice == 0)
        {
            return new IntegerMessage(text.getValue());
        } else
            return new IntegerMessage(0);
    }

    private JMenu getUnitSizeMenu()
    {
        // This is the action listener used
        class UnitSizeListener implements ActionListener
        {
            private double m_scaleFactor;

            public UnitSizeListener(double scaleFactor)
            {
                m_scaleFactor = scaleFactor;
            }

            public void actionPerformed(ActionEvent e)
            {
                UnitIconImageFactory.instance().setScaleFactor(m_scaleFactor);
                m_mapPanel.initTerritories();
            }
        }

        JMenu unitSizeMenu = new JMenu();
        unitSizeMenu.setText("Unit Size");
        ButtonGroup unitSizeGroup = new ButtonGroup();
        JRadioButtonMenuItem radioItem150 = new JRadioButtonMenuItem("125%");
        radioItem150.addActionListener(new UnitSizeListener(1.25));
        JRadioButtonMenuItem radioItem100 = new JRadioButtonMenuItem("100%");
        radioItem100.addActionListener(new UnitSizeListener(1.0));
        JRadioButtonMenuItem radioItem87 = new JRadioButtonMenuItem("87.5%");
        radioItem87.addActionListener(new UnitSizeListener(.875));
        JRadioButtonMenuItem radioItem83 = new JRadioButtonMenuItem("83.33%");
        radioItem83.addActionListener(new UnitSizeListener(.833333));
        JRadioButtonMenuItem radioItem75 = new JRadioButtonMenuItem("75%");
        radioItem75.addActionListener(new UnitSizeListener(.75));
        JRadioButtonMenuItem radioItem66 = new JRadioButtonMenuItem("66.66%");
        radioItem66.addActionListener(new UnitSizeListener(.666666));

        unitSizeGroup.add(radioItem150);
        unitSizeGroup.add(radioItem100);
        unitSizeGroup.add(radioItem87);
        unitSizeGroup.add(radioItem83);
        unitSizeGroup.add(radioItem75);
        unitSizeGroup.add(radioItem66);

        radioItem100.setSelected(true);

        unitSizeMenu.add(radioItem150);
        unitSizeMenu.add(radioItem100);
        unitSizeMenu.add(radioItem87);
        unitSizeMenu.add(radioItem83);
        unitSizeMenu.add(radioItem75);
        unitSizeMenu.add(radioItem66);

        return unitSizeMenu;
    }
}