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

package games.strategy.triplea.ui;

import games.strategy.debug.Console;
import games.strategy.engine.data.*;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.history.*;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.random.*;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.engine.stats.IStat;
import games.strategy.net.*;
import games.strategy.triplea.image.TileImageFactory;

import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * Main menu for the triplea frame.
 * 
 * @author sgb
 *
 */
public class TripleaMenu extends JMenuBar
{

    private final TripleAFrame m_frame;
    
    TripleaMenu(TripleAFrame frame)
    {
        m_frame = frame;
        
        createFileMenu(this);
        createGameMenu(this);
        createNetworkMenu(this);
        createHelpMenu(this);

    }
    
    public GameData getData()
    {
        return m_frame.getGame().getData();
    }

    public IGame getGame()
    {
        return m_frame.getGame();
    }

    private UIContext getUIContext()
    {
        return m_frame.getUIContext();
    }
    
    /**
     * @param menuBar
     */
    private void createHelpMenu(JMenuBar menuBar)
    {
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        

        addMoveHelpMenu(helpMenu);
        addGameNotesMenu(helpMenu);
        addConsoleMenu(helpMenu);
        helpMenu.addSeparator();
        addAboutMenu(helpMenu);
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
        final String notes = (String) getData().getProperties().get("notes");
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

                    JOptionPane.showMessageDialog(m_frame, scroll, "Notes", JOptionPane.PLAIN_MESSAGE);
                }
            });

        }
    }

    /**
     * @param parentMenu
     */
    private void addMoveHelpMenu(JMenu parentMenu)
    {
        parentMenu.add(new AbstractAction("Movement help...")
        {
            public void actionPerformed(ActionEvent e)
            {
                //html formatted string
                String hints = 
                    "<b> Selecting Units</b><br><br>" +
                    "Left click on a unit stack to select 1 unit.<br>" +
                "CTRL-Left click on a unit stack to select all units in the stack.<br>" +
                "Shift-Left click on a unit to select all units in the territory.<br>" +
                "Right click on a unit stack to un select one unit in the stack.<br>" +
                "CTRL-Right click on a unit stack to un select all units in the stack.<br>" +
                "Right click somewhere not on a unit stack to un select the last selected unit.<br>" +
                "CTRL-Right click somewhere not on a unit stack to un select all units.<br>" +
                "<br>" +
                "<b> Selecting Territories</b><br><br>" +
                "After selecting units Left click on a territory to move units to that territory.<br>" +
                "CTRL-Left click on a territory to select the territory as a way point.<br><br>";
                JEditorPane editorPane = new JEditorPane();
                editorPane.setEditable(false);
                editorPane.setContentType("text/html");
                editorPane.setText(hints);

                JScrollPane scroll = new JScrollPane(editorPane);

                JOptionPane.showMessageDialog(m_frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
            }
        });
    }

    /**
     * @param parentMenu
     * @return
     */
    private void addAboutMenu(JMenu parentMenu)
    {
        
        parentMenu.add(new AbstractAction("About...")
        {
            public void actionPerformed(ActionEvent e)
            {
                String text = "<h2>TripleA</h2>"+

                "<p><b>Engine Version:</b> " + games.strategy.engine.EngineVersion.VERSION.toString()+
        "<br><b>Game:</b> "+getData().getGameName()+
                "<br><b>Game Version:</b>" + getData().getGameVersion()+"</p>"+
        "<p>For more information please visit,<br><br>"+
                "<b><a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a></b><br><br>";

                JEditorPane editorPane = new JEditorPane();
                editorPane.setBorder(null);
                editorPane.setBackground(getBackground());
                editorPane.setEditable(false);
                editorPane.setContentType("text/html");
                editorPane.setText(text);

                JScrollPane scroll = new JScrollPane(editorPane);
                scroll.setBorder(null);

                JOptionPane.showMessageDialog(m_frame, editorPane, "About", JOptionPane.PLAIN_MESSAGE);
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

        menuGame.add(m_frame.getShowGameAction());
        menuGame.add(m_frame.getShowHistoryAction());
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
     * @param menuBar
     */
    private void createNetworkMenu(JMenuBar menuBar)
    {
        //revisit
        //if we are not a client or server game 
        //then this will not create the network menu
        if(getGame().getMessenger() instanceof DummyMessenger)
            return;
        
        JMenu menuNetwork = new JMenu("Network");
        addAllowObserversToJoin(menuNetwork);
        addBootPlayer(menuNetwork);
        addShowPlayers(menuNetwork);
        menuBar.add(menuNetwork);
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
                new VerifiedRandomNumbersDialog(m_frame.getRootPane()).setVisible(true);
            }
        };
        if (getGame() instanceof ClientGame)
            parentMenu.add(showVerifiedDice);
    }
    
    /**
     * @param parentMenu
     */
    private void addBootPlayer(JMenu parentMenu)
    {
        if(!getGame().getMessenger().isServer())
            return;
        
        final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
        
        Action boot =  new AbstractAction("Remove Player From Game...")
        {
            public void actionPerformed(ActionEvent e)
            {
                
                
                DefaultComboBoxModel model = new DefaultComboBoxModel();
                JComboBox combo = new JComboBox(model);
                model.addElement("");
                
                for(INode node : messenger.getNodes())
                {
                    if(!node.equals(messenger.getLocalNode()))
                        model.addElement(node.getName());
                }
                
                if(model.getSize() == 1)
                {
                    JOptionPane.showMessageDialog(m_frame,  "No remote players", "No Remote Players" , JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                int rVal = JOptionPane.showConfirmDialog(m_frame, combo, "Select player to remove", JOptionPane.OK_CANCEL_OPTION);
                if(rVal != JOptionPane.OK_OPTION)
                    return;
                
                String name = (String) combo.getSelectedItem();
                
                for(INode node : messenger.getNodes())
                {
                    if(node.getName().equals(name))
                    {
                        messenger.removeConnection(node);
                        return;
                    }
                }
                
            }
        };

        
        parentMenu.add(boot);
        return;
    }
    
    /**
     * @param parentMenu
     */
    private void addAllowObserversToJoin(JMenu parentMenu)
    {
        if(!getGame().getMessenger().isServer())
            return;
        
        final IServerMessenger messeneger = (IServerMessenger) getGame().getMessenger();
        
        final JCheckBoxMenuItem allowObservers = new JCheckBoxMenuItem("Allow New Observers");
        allowObservers.setSelected(messeneger.isAcceptNewConnections());
        
        
        allowObservers.addActionListener(new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                messeneger.setAcceptNewConnections(allowObservers.isSelected());
            }
        });

        parentMenu.add(allowObservers);
        return;
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
        if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
        {
            AbstractAction optionsAction = new AbstractAction("View Game Options...")
            {
                public void actionPerformed(ActionEvent e)
                {
                    PropertiesUI ui = new PropertiesUI(getGame().getData().getProperties(), false);
                    JOptionPane.showMessageDialog(m_frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
                }
            };
            

            menuGame.add(optionsAction);

        }
    }
    
    /**
     * @param menuGame
     */
    private void addShowPlayers(JMenu menuGame)
    {
        if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
        {
            AbstractAction optionsAction = new AbstractAction("Show Who is Who...")
            {
                public void actionPerformed(ActionEvent e)
                {
                    PlayersPanel.showPlayers(getGame(),getUIContext(),m_frame);
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
                IRandomStats randomStats = (IRandomStats) getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME);

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
        
                JOptionPane.showMessageDialog(m_frame, panel, "Random Stats", JOptionPane.INFORMATION_MESSAGE);

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
                StatPanel statPanel = m_frame.getStatPanel();
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                File rootDir = new File(System.getProperties().getProperty("user.dir"));
                chooser.setSelectedFile(new File(rootDir, "stats.csv"));
                
                if(chooser.showSaveDialog(m_frame) != JOptionPane.OK_OPTION)
                    return;
                                
                GameData clone = TripleAFrame.cloneGameData(getData());
                IStat[] stats = statPanel.getStats();

                String[] alliances = (String[]) statPanel.getAlliances().toArray(new String[statPanel.getAlliances().size()]);
                PlayerID[] players = (PlayerID[]) statPanel.getPlayers().toArray(new PlayerID[statPanel.getPlayers().size()]);
                
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
                        m_frame.getMapPanel().updateCounties(getData().getMap().getTerritories());

                    }
                };
                t.start();

            }
        });
        menuGame.add(showMapDetails);
        showMapDetails.setEnabled(getUIContext().getMapData().getHasRelief());
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

        menuGame.add(mapSubMenu);
        
        final Map<String,String> skins = UIContext.getSkins(m_frame.getGame().getData());
        for(final String key : skins.keySet() )
        {
            mapMenuItem = new JRadioButtonMenuItem(key);
            
            
            mapButtonGroup.add(mapMenuItem);
            mapSubMenu.add(mapMenuItem);
            
            mapMenuItem.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                           
                                try
                                {
                                    m_frame.updateMap(skins.get(key));
                                } catch (Exception se)
                                {
                                    se.printStackTrace();
                                    JOptionPane.showMessageDialog(m_frame, se.getMessage(), "Error Changing Map Skin2", JOptionPane.OK_OPTION);
                                }

                            }//else

                        }//actionPerformed
                    );
        }

        
       
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
        JMenuItem leaveGameMenuExit = new JMenuItem(new AbstractAction("Leave Game")
        {
            public void actionPerformed(ActionEvent e)
            {
                m_frame.leaveGame();
            }
        });
        leaveGameMenuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        
        parentMenu.add(leaveGameMenuExit);
        
        JMenuItem menuFileExit = new JMenuItem(new AbstractAction("Exit")
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        m_frame.shutdown();
                    }
                });  
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
                JFileChooser fileChooser = SaveGameFileChooser.getInstance();

                int rVal = fileChooser.showSaveDialog(m_frame);
                if (rVal == JFileChooser.APPROVE_OPTION)
                {
                    File f = fileChooser.getSelectedFile();

                    //A small warning so users will not over-write a file,
                    // added by NeKromancer
                    if (f.exists())
                    {
                        int choice = JOptionPane.showConfirmDialog(m_frame,
                                "A file by that name already exists. Do you wish to over write it?", "Over-write?", JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (choice != JOptionPane.OK_OPTION)
                        {
                            return;
                        }
                    }//end if exists

                    if (!f.getName().toLowerCase().endsWith(".tsvg"))
                    {
                        f = new File(f.getParent(), f.getName() + ".tsvg");
                    }

                    
                    getGame().saveGame(f);                            
                    JOptionPane.showMessageDialog(m_frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        });
        menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

        parent.add(menuFileSave);
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
                getUIContext().getUnitImageFactory().setScaleFactor(m_scaleFactor);
                m_frame.getMapPanel().resetMap();
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
            if (Math.abs(action.m_scaleFactor - getUIContext().getMapData().getDefaultUnitScale()) < 0.01)
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
    
}

