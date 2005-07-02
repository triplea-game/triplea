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

import games.strategy.debug.Console;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.history.*;
import games.strategy.engine.random.*;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.engine.stats.IStat;
import games.strategy.net.*;
import games.strategy.triplea.*;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.triplea.image.*;
import games.strategy.triplea.sound.SoundPath;
import games.strategy.triplea.ui.history.*;
import games.strategy.ui.*;
import games.strategy.util.IntegerMap;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.DefaultMutableTreeNode;

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
    private Set<IGamePlayer> m_localPlayers;

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
    public TripleAFrame(IGame game, Set<IGamePlayer> players) throws IOException
    {
        super("TripleA");

        setIconImage(GameRunner.getGameIcon(this));

        m_game = game;

        game.getMessenger().addErrorListener(m_messengerErrorListener);

        m_data = game.getData();
        m_localPlayers = players;

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(WINDOW_LISTENER);

        createMenuBar();

        MapData.getInstance().verify(m_data);
        MapImage.getInstance().loadMaps(m_data);

        Image small = MapImage.getInstance().getSmallMapImage();
        m_smallView = new MapPanelSmallView(small);
        
        m_mapPanel = new MapPanel(MapData.getInstance().getMapDimensions(), m_data, m_smallView);
        m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);

        //link the small and large images
        ImageScrollControl control = new ImageScrollControl(m_mapPanel, m_smallView);
        control.setScrollWrapX(MapData.getInstance().scrollWrapX());

        m_mapPanel.initSmallMap();

        m_gameMainPanel.setLayout(new BorderLayout());

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);

        m_gameSouthPanel = new JPanel();
        m_gameSouthPanel.setLayout(new BorderLayout());
        m_gameSouthPanel.add(m_message, BorderLayout.CENTER);
        m_message.setBorder(new EtchedBorder(EtchedBorder.RAISED));

        JPanel stepPanel = new JPanel();
        stepPanel.setLayout(new GridBagLayout());
        stepPanel.add(m_step,
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        stepPanel.add(m_round, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0,
                0));

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

        m_rightHandSidePanel.setPreferredSize(new Dimension((int) m_smallView.getPreferredSize().getWidth(), (int) m_mapPanel.getPreferredSize()
                .getHeight()));
        gameCenterPanel.add(m_rightHandSidePanel, BorderLayout.EAST);

        m_gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);

        game.addGameStepListener(m_stepListener);
        updateStep();

        //there are a lot of images that can be gcd right now
        System.gc();
    }

    private void shutdown()
    {
        int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit" , JOptionPane.YES_NO_OPTION);
        if(rVal != JOptionPane.OK_OPTION)
            return;
        
        m_game.shutdown();
        System.exit(0);
    }

    private void createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        createFileMenu(menuBar);
        createGameMenu(menuBar);
        createHelpMenu(menuBar);

        this.setJMenuBar(menuBar);
    }

    /**
     * @param menuBar
     */
    private void createHelpMenu(JMenuBar menuBar)
    {
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        addHelpMenu(helpMenu);

        addHintsMenu(helpMenu);
        addGameNotesMenu(helpMenu);
        addConsoleMenu(helpMenu);
    }

    private void addConsoleMenu(JMenu parentMenu)
    {
        parentMenu.add(new AbstractAction("Show Console...")
                {
            		public void actionPerformed(ActionEvent e)
            		{
            		   Console.getConsole().setVisible(true);   
            		}
                });
    }
    
    /**
     * @param parentMenu
     */
    private void addGameNotesMenu(JMenu parentMenu)
    {
        //allow the game developer to write notes that appear in the game
        //displays whatever is in the notes field in html
        final String notes = (String) m_data.getProperties().get("notes");
        if (notes != null && notes.trim().length() != 0)
        {

            parentMenu.add(new AbstractAction("Game Notes...")
            {
                public void actionPerformed(ActionEvent e)
                {

                    JEditorPane editorPane = new JEditorPane();
                    editorPane.setEditable(false);
                    editorPane.setContentType("text/html");
                    editorPane.setText(notes);

                    JScrollPane scroll = new JScrollPane(editorPane);

                    JOptionPane.showMessageDialog(TripleAFrame.this, scroll, "Notes", JOptionPane.PLAIN_MESSAGE);
                }
            });

        }
    }

    /**
     * @param parentMenu
     */
    private void addHintsMenu(JMenu parentMenu)
    {
        parentMenu.add(new AbstractAction("Hints...")
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

                JOptionPane.showMessageDialog(TripleAFrame.this, scroll, "Hints", JOptionPane.PLAIN_MESSAGE);
            }
        });
    }

    /**
     * @param parentMenu
     * @return
     */
    private void addHelpMenu(JMenu parentMenu)
    {
        
        parentMenu.add(new AbstractAction("About...")
        {
            public void actionPerformed(ActionEvent e)
            {
                String text = "<h2>TripleA</h2>"+

                "<p><b>Engine Version:</b> " + games.strategy.engine.EngineVersion.VERSION.toString()+
		"<br><b>Game:</b> "+m_data.getGameName()+
                "<br><b>Game Version:</b>" + m_data.getGameVersion()+"</p>"+
		"<p>For more information please visit,<br>"+
                "<b><a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a></b>";

                JEditorPane editorPane = new JEditorPane();
                editorPane.setBorder(null);
                editorPane.setBackground(getBackground());
                editorPane.setEditable(false);
                editorPane.setContentType("text/html");
                editorPane.setText(text);

                JScrollPane scroll = new JScrollPane(editorPane);
                scroll.setBorder(null);

                JOptionPane.showMessageDialog(TripleAFrame.this, editorPane, "About", JOptionPane.PLAIN_MESSAGE);
            }
        });
        
    }

    /**
     * @param menuBar
     */
    private void createGameMenu(JMenuBar menuBar)
    {
        JMenu menuGame = new JMenu("Game");
        menuBar.add(menuGame);

        menuGame.add(m_showGameAction);
        menuGame.add(m_showHistoryAction);
        addShowVerifiedDice(menuGame);
        // Put in unit size menu
        addUnitSizeMenu(menuGame);

        addMapSkinsMenu(menuGame);
        addEnableSound(menuGame);
        addShowMapDetails(menuGame);
        menuGame.addSeparator();
        addGameOptionsMenu(menuGame);
        addShowEnemyCasualties(menuGame);
        addShowDiceStats(menuGame);
        addExportStats(menuGame);
    }

    /**
     * @param parentMenu
     */
    private void addShowVerifiedDice(JMenu parentMenu)
    {
        Action showVerifiedDice = new AbstractAction("Show  Verified Dice..")
        {
            public void actionPerformed(ActionEvent e)
            {
                new VerifiedRandomNumbersDialog(TripleAFrame.this.getRootPane()).setVisible(true);
            }
        };
        if (m_game instanceof ClientGame)
            parentMenu.add(showVerifiedDice);
    }

    /**
     * @param parentMenu
     */
    private void addShowEnemyCasualties(JMenu parentMenu)
    {
        final JCheckBoxMenuItem showEnemyCasualties = new JCheckBoxMenuItem("Confirm Enemy Casualties");
        showEnemyCasualties.setSelected(BattleDisplay.getShowEnemyCasualtyNotification());
        showEnemyCasualties.addActionListener(new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                BattleDisplay.setShowEnemyCasualtyNotification(showEnemyCasualties.isSelected());
            }
        });

        parentMenu.add(showEnemyCasualties);
    }

    /**
     * @param menuGame
     */
    private void addGameOptionsMenu(JMenu menuGame)
    {
        if (!m_game.getData().getProperties().getEditableProperties().isEmpty())
        {
            AbstractAction optionsAction = new AbstractAction("View Game Options...")
            {
                public void actionPerformed(ActionEvent e)
                {
                    PropertiesUI ui = new PropertiesUI(m_game.getData().getProperties(), false);
                    JOptionPane.showMessageDialog(TripleAFrame.this, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
                }
            };
            

            menuGame.add(optionsAction);

        }
    }

    /**
     * @param parentMenu
     */
    private void addEnableSound(JMenu parentMenu)
    {
        final JCheckBoxMenuItem soundCheckBox = new JCheckBoxMenuItem("Enable Sound");

        soundCheckBox.setSelected(!ClipPlayer.getInstance().getBeSilent());
        //temporarily disable sound

        soundCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ClipPlayer.getInstance().setBeSilent(!soundCheckBox.isSelected());
            }
        });
        parentMenu.add(soundCheckBox);
    }

    /**
     * @param parentMenu
     */
    private void addShowDiceStats(JMenu parentMenu)
    {
        Action showDiceStats = new AbstractAction("Show Dice Stats...")
        {
            public void actionPerformed(ActionEvent e)
            {
                IRandomStats randomStats = (IRandomStats) m_game.getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME);

                RandomStatsDetails stats = randomStats.getRandomStats();

                JPanel panel = new JPanel();
                BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
                panel.setLayout(layout);

                Iterator<Integer> iter = new TreeSet<Integer>(stats.getData().keySet()).iterator();
                while (iter.hasNext())
                {
                    Integer key = iter.next();
                    int value = stats.getData().getInt(key);
                    JLabel label = new JLabel(key + " was rolled " + value + " times");
                    panel.add(label);
                }
                panel.add(new JLabel("  "));
                DecimalFormat format = new DecimalFormat("#0.000");
                panel.add(new JLabel("Average roll : " + format.format(stats.getAverage())));
		panel.add(new JLabel("Median : " + format.format(stats.getMedian())));
		panel.add(new JLabel("Variance : " + format.format(stats.getVariance())));
		panel.add(new JLabel("Standard Deviation : " + format.format(stats.getStdDeviation())));
		panel.add(new JLabel("Total rolls : " + stats.getTotal()));
		
                JOptionPane.showMessageDialog(TripleAFrame.this, panel, "Random Stats", JOptionPane.INFORMATION_MESSAGE);

            }
        };
        parentMenu.add(showDiceStats);
    }

    /**
     * @param parentMenu
     */
    private void addExportStats(JMenu parentMenu)
    {
        Action showDiceStats = new AbstractAction("Export Game Stats...")
        {
            public void actionPerformed(ActionEvent e)
            {
                createAndSaveStats();
                
            }

            /**
             * 
             */
            private void createAndSaveStats()
            {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                File rootDir = new File(System.getProperties().getProperty("user.dir"));
                chooser.setSelectedFile(new File(rootDir, "stats.csv"));
                
                if(chooser.showSaveDialog(TripleAFrame.this) != JOptionPane.OK_OPTION)
                    return;
                                
                GameData clone = cloneGameData(m_data);
                IStat[] stats = m_statsPanel.getStats();

                String[] alliances = (String[]) m_statsPanel.getAlliances().toArray(new String[m_statsPanel.getAlliances().size()]);
                PlayerID[] players = (PlayerID[]) m_statsPanel.getPlayers().toArray(new PlayerID[m_statsPanel.getPlayers().size()]);
                
                //its important here to translate the player objects into our game data
                //the players for the stat panel are only relevant with respect to
                //the game data they belong to
                for(int i = 0; i < players.length; i++)
                {
                    players[i] = clone.getPlayerList().getPlayerID(players[i].getName());
                }
                
                
                StringBuilder text = new StringBuilder(1000);
                                
                text.append("Round,Player Turn,");
                
                for(int i = 0; i < stats.length; i++)
                {
                    for (int j = 0; j < players.length; j++)
                    {
                        text.append(stats[i].getName()).append(" ");
                        text.append(players[j].getName());
                        text.append(",");
                        
                    }
                    
                    for (int j = 0; j < alliances.length; j++)
                    {
                        text.append(stats[i].getName()).append(" ");
                        text.append(alliances[j]);
                        text.append(",");
                    }
                }
                text.append("\n");
                clone.getHistory().gotoNode(clone.getHistory().getLastNode());
                
                Enumeration nodes = ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
                
                PlayerID currentPlayer = null;
                
                int round = 0;
                while (nodes.hasMoreElements())
                {
                    //we want to export on change of turn
                    
                    HistoryNode element = (HistoryNode) nodes.nextElement();
                    
                    if(element instanceof Round)
                        round++;
                    
                    if(!( element instanceof Step))
                        continue;
                    
                    Step step = (Step) element;
                    
                    if(step.getPlayerID() == null || step.getPlayerID().isNull())
                        continue;
                    
                    if(step.getPlayerID() == currentPlayer)
                        continue;
                    
                    currentPlayer = step.getPlayerID();
                    
                    clone.getHistory().gotoNode(element);
                    
                    
                    String playerName = step.getPlayerID() == null ? "" : step.getPlayerID().getName() + ": ";
                    text.append(round).append(",").append(playerName).append(",");
                    
                    for(int i = 0; i < stats.length; i++)
                    {
                        
                        
                        for (int j = 0; j < players.length; j++)
                        {
                            text.append(stats[i].getFormatter().format(stats[i].getValue(players[j], clone)));
                            text.append(",");
                            
                        }
                        
                        for (int j = 0; j < alliances.length; j++)
                        {
                            text.append(stats[i].getFormatter().format(stats[i].getValue(alliances[j], clone)));
                            text.append(",");
                        }
                        
                        
                    }
                    text.append("\n");
                }
                
                
                
                try
                {
                    FileWriter writer = new FileWriter(chooser.getSelectedFile());
                    try
                    {
                        writer.write(text.toString());
                    }
                    finally
                    {
                        writer.close();
                    }
                } catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }
        };
        parentMenu.add(showDiceStats);
    }
    
    
    /**
     * @param menuGame
     */
    private void addShowMapDetails(JMenu menuGame)
    {
        final JCheckBoxMenuItem showMapDetails = new JCheckBoxMenuItem("Show Map Details");

        showMapDetails.setSelected(TileImageFactory.getShowReliefImages());

        showMapDetails.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {

                TileImageFactory.setShowReliefImages(showMapDetails.isSelected());
                Thread t = new Thread("Triplea : Show map details thread")
                {
                    public void run()
                    {
                        yield();
                        m_mapPanel.updateCounties(m_data.getMap().getTerritories());

                    }
                };
                t.start();

            }
        });
        menuGame.add(showMapDetails);
        showMapDetails.setEnabled(MapData.getInstance().getHasRelief());
    }

    /**
     * @param menuGame
     */
    private void addMapSkinsMenu(JMenu menuGame)
    {
        // beagles Mapskin code
        // creates a sub menu of radiobuttons for each available mapdir

        JMenuItem mapMenuItem;
        JMenu mapSubMenu = new JMenu("Map Skins");
        ButtonGroup mapButtonGroup = new ButtonGroup();

        // Create A String array of compatible MapDirs

        final String currentMapSubDir = TileImageFactory.getMapDir();
        final File mapsDir = new File(GameRunner.getRootFolder() + java.io.File.separator + "classes" + Constants.MAP_SKINS_DIR);

        if (currentMapSubDir != null)
        {
            //Filter only MapDirs that start with the originals name

            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File aDir, String name)
                {
                    if (name.startsWith(currentMapSubDir))
                    {
                        File file = new File(aDir + java.io.File.separator + name);
                        return file.isDirectory();
                    }
                    return (false);
                }
            };

            String[] mapDirs = mapsDir.list(filter);
            if(mapDirs == null)
                return;
            
            //create entry for each mapdir
            String mapMenuItemName;
            for (int i = 0; i < mapDirs.length; i++)
            {
                mapMenuItemName = mapDirs[i].replaceFirst(currentMapSubDir, "");
                if (mapMenuItemName.length() == 0)
                {
                    // mapMenuItemName="Original";
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
                                updateMap(currentMapSubDir);
                            } catch (Exception se)
                            {
                                se.printStackTrace();
                                JOptionPane.showMessageDialog(TripleAFrame.this, se.getMessage(), "Error Changing Map Skin", JOptionPane.OK_OPTION);
                            }
                        } else
                        {
                            try
                            {
                                updateMap(currentMapSubDir + e.getActionCommand());
                            } catch (Exception se)
                            {
                                se.printStackTrace();
                                JOptionPane.showMessageDialog(TripleAFrame.this, se.getMessage(), "Error Changing Map Skin2", JOptionPane.OK_OPTION);
                            }

                        }//else

                    }//actionPerformed
                });

            }//for

        }//if

        // add the sub menu to the menu
        menuGame.add(mapSubMenu);
    }

    /**
     * @param menuBar
     */
    private void createFileMenu(JMenuBar menuBar)
    {
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        addSaveMenu(fileMenu);
        addExitMenu(fileMenu);
    }

    /**
     * @param parentMenu
     */
    private void addExitMenu(JMenu parentMenu)
    {
        JMenuItem menuFileExit = new JMenuItem(new AbstractAction("Exit")
        {
            public void actionPerformed(ActionEvent e)
            {
                shutdown();
            }
        });
        menuFileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        parentMenu.add(menuFileExit);
    }

    /**
     * @param parent
     */
    private void addSaveMenu(JMenu parent)
    {
        // menuFileSave = new JMenuItem("Save", KeyEvent.VK_S);
        JMenuItem menuFileSave = new JMenuItem(new AbstractAction("Save...")
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!m_game.canSave())
                {
                    JOptionPane.showMessageDialog(TripleAFrame.this, "You cannot save the game if you are playing as a client", "Cant save",
                            JOptionPane.OK_OPTION);
                    return;
                }

                GameDataManager manager = new GameDataManager();

                try
                {
                    JFileChooser fileChooser = SaveGameFileChooser.getInstance();

                    int rVal = fileChooser.showSaveDialog(TripleAFrame.this);
                    if (rVal == JFileChooser.APPROVE_OPTION)
                    {
                        File f = fileChooser.getSelectedFile();

                        //A small warning so users will not over-write a file,
                        // added by NeKromancer
                        if (f.exists())
                        {
                            int choice = JOptionPane.showConfirmDialog(TripleAFrame.this,
                                    "A file by that name already exists. Do you wish to over write it?", "Over-write?", JOptionPane.YES_NO_OPTION,
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
                        JOptionPane.showMessageDialog(TripleAFrame.this, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
                    }

                }
                catch(DelegateNotCurrentlySaveableException dncse)
                {
                    JOptionPane.showMessageDialog(TripleAFrame.this, dncse.getMessage(), "Can't Save Game", JOptionPane.OK_OPTION);                    
                }catch (IOException se)
                {
                    se.printStackTrace();
                    JOptionPane.showMessageDialog(TripleAFrame.this, se.getMessage(), "Error Saving Game", JOptionPane.OK_OPTION);
                }
            }
        });
        menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

        parent.add(menuFileSave);
    }

    // Beagle Code Called to Change Mapskin
    private void updateMap(String mapdir) throws IOException
    {

        MapData.setMapDir(mapdir); // set mapdir
        TileImageFactory.setMapDir(mapdir);

        MapImage.getInstance().loadMaps(m_data); // load map data
        m_mapPanel.setGameData(m_data);

        // update mappanels to use new image
        
        m_mapPanel.changeImage(MapData.getInstance().getMapDimensions());

        Image small = MapImage.getInstance().getSmallMapImage();
        m_smallView.changeImage(small);

        m_mapPanel.resetMap(); // redraw territories
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
            StringBuilder buf = new StringBuilder();
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

    public IntegerMap<ProductionRule> getProduction(PlayerID player, boolean bid)
    {
        m_actionButtons.changeToProduce(player);
        return m_actionButtons.waitForPurchase(bid);
    }

    public MoveDescription getMove(PlayerID player, IPlayerBridge bridge, boolean nonCombat)
    {
        m_actionButtons.changeToMove(player, nonCombat);
        return m_actionButtons.waitForMove(bridge);
    }

    public PlaceData waitForPlace(PlayerID player, boolean bid, IPlayerBridge bridge)
    {
        m_actionButtons.changeToPlace(player);
        return m_actionButtons.waitForPlace(bid, bridge);
    }

    public FightBattleDetails getBattle(PlayerID player, Collection battles, Collection bombingRaids)
    {
        m_actionButtons.changeToBattle(player, battles, bombingRaids);
        return m_actionButtons.waitForBattleSelection();
    }

    public void notifyError(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void notifyMessage(String message)
    {
        JOptionPane.showMessageDialog(this, message, message, JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean getOKToLetAirDie(String message)
    {
        String ok = "Kill air";
        String cancel = "Keep moving";
        String[] options =
        { cancel, ok };
        int choice = JOptionPane.showOptionDialog(this, message, "Air cannot land", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                options, cancel);
        return choice == 1;
    }

    public boolean getOK(String message)
    {
        int choice = JOptionPane.showConfirmDialog(this, message, message, JOptionPane.OK_CANCEL_OPTION);
        return choice == JOptionPane.OK_OPTION;
    }

    public void notifyTechResults(TechResults msg)
    {
        TechResultsDisplay display = new TechResultsDisplay(msg);
        JOptionPane.showOptionDialog(this, display, "Tech roll", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]
        { "OK" }, "OK");

    }

    public boolean getStrategicBombingRaid(Territory location)
    {
        String message = "Bomb in " + location.getName();
        String bomb = "Bomb";
        String normal = "Attack";
        String[] choices =
        { bomb, normal };
        int choice = JOptionPane.showOptionDialog(this, message, "Bomb?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                choices, bomb);
        return choice == 0;
    }

    public Territory selectTerritoryForAirToLand(Collection<Territory> candidates)
    {

        JList list = new JList(new Vector<Territory>(candidates));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(list);
        panel.add(scroll, BorderLayout.CENTER);

        String[] options =
        { "OK" };
        String message = "Select territory for air units to land";

        JOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);

        Territory selected = (Territory) list.getSelectedValue();

        return selected;
    }

    public TechRoll getTechRolls(PlayerID id)
    {
        m_actionButtons.changeToTech(id);
        return m_actionButtons.waitForTech();
    }

    public Territory getRocketAttack(Collection<Territory> candidates, Territory from)
    {

        JList list = new JList(new Vector<Territory>(candidates));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(list);
        panel.add(scroll, BorderLayout.CENTER);

        if (from != null)
        {
            panel.add(BorderLayout.NORTH, new JLabel("Targets for rocket in " + from.getName()));
        }

        String[] options =
        { "OK", "Dont attack" };
        String message = "Select Rocket Target";

        int selection = JOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
                null);

        Territory selected = null;
        if (selection == 0) //OK
            selected = (Territory) list.getSelectedValue();

        return selected;
    }

    public boolean playing(PlayerID id)
    {
        if (id == null)
            return false;

        Iterator<IGamePlayer> iter = m_localPlayers.iterator();
        while (iter.hasNext())
        {
            IGamePlayer gamePlayer = iter.next();
            if (gamePlayer.getID().equals(id) && gamePlayer instanceof TripleAPlayer)
            {
                return true;
            }
        }
        return false;
    }

    private IMessengerErrorListener m_messengerErrorListener = new IMessengerErrorListener()
    {
        public void connectionLost(INode node, Exception reason, java.util.List unsent)
        {
            String message = "Connection lost to " + node.getName() + ". Game over.";
            JOptionPane.showMessageDialog(TripleAFrame.this, message, "Error", JOptionPane.ERROR_MESSAGE);
        }

        public void messengerInvalid(IMessenger messenger, Exception reason, java.util.List unsent)
        {
            String message = "Network connection lost. Game over.";
            JOptionPane.showMessageDialog(TripleAFrame.this, message, "Error", JOptionPane.ERROR_MESSAGE);
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

        public void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String stepDisplayName)
        {
            updateStep();
        }
    };

    private void updateStep()
    {
        if (m_data.getSequence().getStep() == null)
            return;

        //we nee d to invoke and wait here since
        //if we switch to the history as a result of a history
        //change, we need to ensure that no further history
        //events are run until our historySynchronizer is set up
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
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            } catch (InvocationTargetException e)
            {
                e.getCause().printStackTrace();
                throw new IllegalStateException(e.getCause().getMessage());
            }
            return;
        }

        int round = m_data.getSequence().getRound();
        String stepDisplayName = m_data.getSequence().getStep().getDisplayName();
        PlayerID player = m_data.getSequence().getStep().getPlayerID();

        m_round.setText("Round:" + round + " ");
        m_step.setText(stepDisplayName);
        if (player != null && !player.isNull())
            m_round.setIcon(new ImageIcon(FlagIconImageFactory.instance().getFlag(player)));

        //if the game control has passed to someone else
        //show the history
        if (player != null && !player.isNull() && !playing(player) && !m_inHistory)
        {
            if (!SwingUtilities.isEventDispatchThread())
                throw new IllegalStateException("We should be in dispatch thread");
            
            showHistory();
        }
        //if the game control is with us
        //show the current game
        else if (player != null && !player.isNull() && playing(player) && m_inHistory)
        {
            showGame();
            ClipPlayer.getInstance().playClip(SoundPath.START_TURN, SoundPath.class); //play
                                                                                      // sound
        }

    }

    public void showHistory()
    {
        m_inHistory = true;
        setWidgetActivation();

        //we want to use a clone of the data, so we can make changes to it
        GameData clonedGameData = cloneGameData(m_data);
        if (clonedGameData == null)
            return;

        clonedGameData.getAllianceTracker();
        m_historySyncher = new HistorySynchronizer(clonedGameData, m_game);

        m_statsPanel.setGameData(clonedGameData);
        m_details.setGameData(clonedGameData);
        m_mapPanel.setGameData(clonedGameData);

        HistoryDetailsPanel historyDetailPanel = new HistoryDetailsPanel(clonedGameData, m_mapPanel);

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
    public static GameData cloneGameData(GameData data)
    {
        try
        {
            GameDataManager manager = new GameDataManager();
            ByteArrayOutputStream sink = new ByteArrayOutputStream(10000);
            manager.saveGame(sink, data, false);
            sink.close();
            ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
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

    private AbstractAction m_showHistoryAction = new AbstractAction("Show history")
    {
        public void actionPerformed(ActionEvent e)
        {
            showHistory();
        }
    };

    private AbstractAction m_showGameAction = new AbstractAction("Show current game")
    {
        {
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            showGame();
        }
    };

    public Collection<Unit> moveFightersToCarrier(Collection<Unit> fighters, Territory where)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        ScrollableTextField text = new ScrollableTextField(0, fighters.size());
        text.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.add(text, BorderLayout.CENTER);
        panel.add(new JLabel("How many fighters do you want to move from " + where.getName() + " to new carrier?"), BorderLayout.NORTH);

        int choice = JOptionPane.showOptionDialog(this, panel, "Place fighters on new carrier?", JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, null, new String[]
                { "OK", "Cancel" }, "OK");
        if (choice == 0)
        {
            //arrayList.subList() is not serializable
            return new ArrayList<Unit>(new ArrayList<Unit>(fighters).subList(0, text.getValue()));
        } else
            return new ArrayList<Unit>(0);
    }

    private void addUnitSizeMenu(JMenu parentMenu)
    {
         
        final NumberFormat s_decimalFormat = new DecimalFormat("00.##");

        // This is the action listener used
        class UnitSizeAction extends AbstractAction
        {
            private double m_scaleFactor;

            public UnitSizeAction(double scaleFactor)
            {
                m_scaleFactor = scaleFactor;
                putValue(Action.NAME, s_decimalFormat.format(m_scaleFactor * 100) + "%");
            }

            public void actionPerformed(ActionEvent e)
            {
                UnitImageFactory.instance().setScaleFactor(m_scaleFactor);
                m_mapPanel.resetMap();
            }
        }

        JMenu unitSizeMenu = new JMenu();
        unitSizeMenu.setText("Unit Size");
        ButtonGroup unitSizeGroup = new ButtonGroup();
        JRadioButtonMenuItem radioItem125 = new JRadioButtonMenuItem(new UnitSizeAction(1.25));

        JRadioButtonMenuItem radioItem100 = new JRadioButtonMenuItem(new UnitSizeAction(1.0));
        JRadioButtonMenuItem radioItem87 = new JRadioButtonMenuItem(new UnitSizeAction(0.875));
        JRadioButtonMenuItem radioItem83 = new JRadioButtonMenuItem(new UnitSizeAction(0.8333));
        JRadioButtonMenuItem radioItem75 = new JRadioButtonMenuItem(new UnitSizeAction(0.75));
        JRadioButtonMenuItem radioItem66 = new JRadioButtonMenuItem(new UnitSizeAction(0.6666));
        JRadioButtonMenuItem radioItem56 = new JRadioButtonMenuItem(new UnitSizeAction(0.5625));
        JRadioButtonMenuItem radioItem50 = new JRadioButtonMenuItem(new UnitSizeAction(0.5));

        unitSizeGroup.add(radioItem125);
        unitSizeGroup.add(radioItem100);
        unitSizeGroup.add(radioItem87);
        unitSizeGroup.add(radioItem83);
        unitSizeGroup.add(radioItem75);
        unitSizeGroup.add(radioItem66);
        unitSizeGroup.add(radioItem56);
        unitSizeGroup.add(radioItem50);

        radioItem100.setSelected(true);

        //select the closest to to the default size
        Enumeration enum1 = unitSizeGroup.getElements();
        boolean matchFound = false;
        while (enum1.hasMoreElements())
        {
            JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) enum1.nextElement();
            UnitSizeAction action = (UnitSizeAction) menuItem.getAction();
            if (Math.abs(action.m_scaleFactor - MapData.getInstance().getDefaultUnitScale()) < 0.01)
            {
                menuItem.setSelected(true);
                matchFound = true;
                break;
            }
        }

        if (!matchFound)
            System.err.println("default unit size does not match any menu item");

        unitSizeMenu.add(radioItem125);
        unitSizeMenu.add(radioItem100);
        unitSizeMenu.add(radioItem87);
        unitSizeMenu.add(radioItem83);
        unitSizeMenu.add(radioItem75);
        unitSizeMenu.add(radioItem66);
        unitSizeMenu.add(radioItem56);
        unitSizeMenu.add(radioItem50);

        parentMenu.add(unitSizeMenu);
    }

    public BattlePanel getBattlePanel()
    {
        return m_actionButtons.getBattlePanel();
    }

    public MovePanel getMovePanel()
    {
        return m_actionButtons.getMovePanel();
    }

    public TechPanel getTechPanel()
    {
        return m_actionButtons.getTechPanel();
    }

    public PlacePanel getPlacePanel()
    {
        return m_actionButtons.getPlacePanel();
    }

    public PurchasePanel getPurchasePanel()
    {
        return m_actionButtons.getPurchasePanel();
    }

}
