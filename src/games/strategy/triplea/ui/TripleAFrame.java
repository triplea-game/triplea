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

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.AirThatCantLandUtil;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.sound.SoundPath;
import games.strategy.triplea.ui.history.*;
import games.strategy.ui.*;
import games.strategy.util.IntegerMap;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

/**
 * 
 * @author Sean Bridges
 * 
 * Main frame for the triple a game
 */
public class TripleAFrame extends JFrame
{
    private GameData m_data;
    private IGame m_game;
    private MapPanel m_mapPanel;
    private MapPanelSmallView m_smallView;
    private JLabel m_message = new JLabel("No selection");
    private JLabel m_status = new JLabel("");
    private JLabel m_step = new JLabel("xxxxxx");
    private JLabel m_round = new JLabel("xxxxxx");
    private JLabel m_player = new JLabel("xxxxxx");
    private ActionButtons m_actionButtons;
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
    private UIContext m_uiContext;
    private JPanel m_mapAndChatPanel;
    private ChatPanel m_chatPanel;
    private JSplitPane m_chatSplit;

    /** Creates new TripleAFrame */
    public TripleAFrame(IGame game, Set<IGamePlayer> players) throws IOException
    {
        super("TripleA");
        setIconImage(GameRunner.getGameIcon(this));

        m_game = game;

        m_data = game.getData();
        m_localPlayers = players;
        
        addZoomKeyboardShortcuts();

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(WINDOW_LISTENER);

        m_uiContext = new UIContext();
        m_uiContext.setDefaltMapDir(game.getData());
        m_uiContext.getMapData().verify(m_data);

        createMenuBar();
        
        ImageScrollModel model = new ImageScrollModel();
        model.setScrollX(m_uiContext.getMapData().scrollWrapX());
        model.setMaxBounds(m_uiContext.getMapData().getMapDimensions().width, m_uiContext.getMapData().getMapDimensions().height);
        
        Image small = m_uiContext.getMapImage().getSmallMapImage();
        m_smallView = new MapPanelSmallView(small, model);
        
        m_mapPanel = new MapPanel(m_data, m_smallView, m_uiContext, model);
        m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);

        //link the small and large images
        

        m_mapPanel.initSmallMap();

        
        m_mapAndChatPanel = new JPanel();
        m_mapAndChatPanel.setLayout(new BorderLayout());
        if(MainFrame.getInstance().getChat() != null)
        {
            m_chatPanel = new ChatPanel(MainFrame.getInstance().getChat());
            m_chatPanel.setPlayerRenderer(new PlayerChatRenderer(m_game, m_uiContext ));
            
            Dimension chatPrefSize = new Dimension((int) m_chatPanel.getPreferredSize().getWidth(), 95);
            m_chatPanel.setPreferredSize(chatPrefSize);
            
            
            m_chatSplit = new JSplitPane();
            m_chatSplit.setTopComponent(m_mapPanel);
            m_chatSplit.setBottomComponent(m_chatPanel);
            m_chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
            m_chatSplit.setResizeWeight(0.95);
                        
            m_mapAndChatPanel.add(m_chatSplit, BorderLayout.CENTER);
        }
        else
        {
            m_mapAndChatPanel.add(m_mapPanel, BorderLayout.CENTER);
        }
        
        m_gameMainPanel.setLayout(new BorderLayout());

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);

        m_gameSouthPanel = new JPanel();
        m_gameSouthPanel.setLayout(new BorderLayout());
        m_gameSouthPanel.add(m_message, BorderLayout.WEST);
        m_message.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_message.setText("some text to set a reasonable preferred size");
        m_message.setPreferredSize(m_message.getPreferredSize());
        m_message.setText("");
        m_gameSouthPanel.add(m_status, BorderLayout.CENTER);
        m_status.setBorder(new EtchedBorder(EtchedBorder.RAISED));

        JPanel stepPanel = new JPanel();
        stepPanel.setLayout(new GridBagLayout());
        stepPanel.add(m_step,
                new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        stepPanel.add(m_player, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0,
                0));

        stepPanel.add(m_round, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0,
                0));

        

        m_step.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_round.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_player.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_step.setHorizontalTextPosition(SwingConstants.LEADING);

        m_gameSouthPanel.add(stepPanel, BorderLayout.EAST);

        m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);

        JPanel gameCenterPanel = new JPanel();
        gameCenterPanel.setLayout(new BorderLayout());

        
        
        gameCenterPanel.add(m_mapAndChatPanel, BorderLayout.CENTER);

        m_rightHandSidePanel.setLayout(new BorderLayout());
        m_rightHandSidePanel.add(m_smallView, BorderLayout.NORTH);

        m_tabsPanel.setBorder(null);
        m_rightHandSidePanel.add(m_tabsPanel, BorderLayout.CENTER);

        m_actionButtons = new ActionButtons(m_data, m_mapPanel, this);
        m_tabsPanel.addTab("Actions", m_actionButtons);
        m_actionButtons.setBorder(null);

        m_statsPanel = new StatPanel(m_data);
        m_tabsPanel.addTab("Stats", m_statsPanel);

        m_details = new TerritoryDetailPanel(m_mapPanel, m_data, m_uiContext, this);
        m_tabsPanel.addTab("Territory", m_details);
        
    
        m_rightHandSidePanel.setPreferredSize(new Dimension((int) m_smallView.getPreferredSize().getWidth(), (int) m_mapPanel.getPreferredSize()
                .getHeight()));
        gameCenterPanel.add(m_rightHandSidePanel, BorderLayout.EAST);

        m_gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);

        game.addGameStepListener(m_stepListener);
        updateStep();

        m_uiContext.addShutdownWindow(this);
    }

    private void addZoomKeyboardShortcuts()
    {
        String zoom_map_in = "zoom_map_in";
        //do both = and + (since = is what you get when you hit ctrl+ )
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.META_MASK), zoom_map_in );
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in );
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.META_MASK), zoom_map_in );
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in );
        ((JComponent)getContentPane()).getActionMap().put(zoom_map_in, new AbstractAction(zoom_map_in)
        {
        
            public void actionPerformed(ActionEvent e)
            {
                if(getScale() < 100)
                    setScale(getScale() + 10  );
            }
        
        });
        

        String zoom_map_out = "zoom_map_out";
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.META_MASK), zoom_map_out );
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.CTRL_MASK), zoom_map_out );
        ((JComponent)getContentPane()).getActionMap().put(zoom_map_out, new AbstractAction(zoom_map_out)
        {
        
            public void actionPerformed(ActionEvent e)
            {
                if(getScale() > 16)
                    setScale(getScale() - 10  );
            }
        
        });
        
        
    }

    /**
     * 
     * @param value - a number between 15 and 100
     */
    void setScale(double value)
    {
        getMapPanel().setScale(value / (double) 100);
    }
    
    /**
     * 
     * @return a scale between 15 and 100
     */
    private double getScale() 
    {
        return getMapPanel().getScale() * 100;
    }
    

    public void stopGame()
    {        
        //we have already shut down
        if(m_uiContext == null)
            return;
        
        
        if(GameRunner.isMac()) 
        {
            //this frame should not handle shutdowns anymore
            MacWrapper.unregisterShutdownHandler();
        }
        
        m_uiContext.shutDown();
        if(m_chatPanel != null)
        {
            m_chatPanel.setPlayerRenderer(new DefaultListCellRenderer());
            m_chatPanel.setChat(null);
        }
            

        if(m_historySyncher != null)
        {
            m_historySyncher.deactivate();
            m_historySyncher = null;
        }

        
        //there is a bug in java (1.50._06  for linux at least)
        //where frames are not garbage collected.
        //
        //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6364875
        //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6368950
        //
        //so remove all references to everything
        //to minimize the damage

        
        m_game.removeGameStepListener(m_stepListener);
        
        m_game = null;
        m_uiContext = null;
        if(m_data != null)
            m_data.clearAllListeners();
        m_data = null;
        
        MAP_SELECTION_LISTENER = null;
                
        
        m_actionButtons = null;
        m_chatPanel = null;
        m_chatSplit = null;
        m_details = null;        
        m_gameMainPanel = null;
        m_stepListener = null;
        m_gameSouthPanel = null;
        m_historyTree = null;
        m_historyPanel= null;        
        m_mapPanel = null;
        m_mapAndChatPanel = null;
        m_message = null;
        m_status = null;
        m_rightHandSidePanel = null;
        m_smallView = null;
        m_statsPanel = null;
        m_step = null;
        m_round = null;
        m_player = null;
        m_tabsPanel = null;
        
        m_showGameAction = null;
        m_showHistoryAction = null;
        m_localPlayers = null;
        
        
        removeWindowListener(WINDOW_LISTENER);
        WINDOW_LISTENER = null;

    }
    
    void shutdown()
    {
        int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit" , JOptionPane.YES_NO_OPTION);
        if(rVal != JOptionPane.OK_OPTION)
            return;

        System.exit(0);
    }
    
    void leaveGame()
    {
        int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to leave?\nUnsaved game data will be lost.", "Exit" , JOptionPane.YES_NO_OPTION);
        if(rVal != JOptionPane.OK_OPTION)
            return;
        
        if(m_game instanceof ServerGame)
        {
            ((ServerGame) m_game).stopGame();
        }
        else
        {
            m_game.getMessenger().shutDown();
            ((ClientGame) m_game).shutDown();
            
            //an ugly hack, we need a better
            //way to get the main frame
            MainFrame.getInstance().clientLeftGame();
        }
    }

    private void createMenuBar()
    {
        TripleaMenu menu = new TripleaMenu(this);

        this.setJMenuBar(menu);
    }

 
  



    private WindowListener WINDOW_LISTENER = new WindowAdapter()
    {
        public void windowClosing(WindowEvent e)
        {
            leaveGame();
        }
    };

    public MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
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
                TerritoryAttachment ta = TerritoryAttachment.get(in);
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

    public void clearStatusMessage()
    {
        m_status.setText("");
        m_status.setIcon(null);
    }

    public void setStatusErrorMessage(String msg)
    {
        m_status.setText(msg);
        if (!msg.equals(""))
            m_status.setIcon(new ImageIcon(m_mapPanel.getErrorImage()));
        else
            m_status.setIcon(null);
    }

    public void setStatusWarningMessage(String msg)
    {
        m_status.setText(msg);
        if (!msg.equals(""))
            m_status.setIcon(new ImageIcon(m_mapPanel.getWarningImage()));
        else
            m_status.setIcon(null);
    }

    public void setStatusInfoMessage(String msg)
    {
        m_status.setText(msg);
        if (!msg.equals(""))
            m_status.setIcon(new ImageIcon(m_mapPanel.getInfoImage()));
        else
            m_status.setIcon(null);
    }

    public IntegerMap<ProductionRule> getProduction(final PlayerID player, boolean bid)
    {
        m_actionButtons.changeToProduce(player);
        return m_actionButtons.waitForPurchase(bid);
    }

    public MoveDescription getMove(final PlayerID player, IPlayerBridge bridge, final boolean nonCombat)
    {
        m_actionButtons.changeToMove(player, nonCombat);
        // workaround for panel not receiving focus at beginning of n/c move phase
        if (!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        requestFocusInWindow();
                        transferFocus();
                    }
                });
            } catch (Exception e)
            {
                e.printStackTrace();
            } 
        }
        else
        {
            requestFocusInWindow();
            transferFocus();
        }
        return m_actionButtons.waitForMove(bridge);
    }

    public PlaceData waitForPlace(final PlayerID player, final boolean bid, final IPlayerBridge bridge)
    {
        m_actionButtons.changeToPlace(player);
        return m_actionButtons.waitForPlace(bid, bridge);
    }

    public FightBattleDetails getBattle(final PlayerID player, final Collection<Territory> battles, final Collection<Territory> bombingRaids)
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

    
    
    public boolean getOKToLetAirDie(PlayerID m_id, String message, boolean movePhase)
    {
        boolean lhtrProd = AirThatCantLandUtil.isLHTRCarrierProdcution(m_data);
        int carrierCount = m_id.getUnits().getMatches(Matches.UnitIsCarrier).size();
        boolean canProduceCarriersUnderFighter = lhtrProd && carrierCount != 0; 
        
        if(canProduceCarriersUnderFighter && carrierCount > 0)
        {
            message = message + " You have " + carrierCount + MyFormatter.pluralize("carrier", carrierCount);
        }
        
        
        String ok = movePhase ?  "End Move Phase" : "Kill Planes";
        String cancel = movePhase ? "Keep Moving" : "Change Placement";
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
        TechResultsDisplay display = new TechResultsDisplay(msg, m_uiContext);
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

    public TechRoll getTechRolls(final PlayerID id)
    {
        m_actionButtons.changeToTech(id);
        // workaround for panel not receiving focus at beginning of tech phase
        if (!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        requestFocusInWindow();
                        transferFocus();
                    }
                });
            } catch (Exception e)
            {
                e.printStackTrace();
            } 
        }
        else
        {
            requestFocusInWindow();
            transferFocus();
        }
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
                if(fos != null)
                    fos.flush();
            } catch (Exception ignore)
            {
            }
            try
            {
                if(oos != null)
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
        UIContext context = m_uiContext;
        if(context == null || context.isShutDown())
            return;
        
        m_data.acquireReadLock();
        try
        {
            if (m_data.getSequence().getStep() == null)
                return;
        }
        finally 
        {
            m_data.releaseReadLock();
        }

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

        m_round.setText("Round:" + round + " ");
        m_step.setText(stepDisplayName);
        if(player != null)
            m_player.setText((playing(player)?"":"REMOTE: ") + player.getName());
        if (player != null && !player.isNull())
            m_player.setIcon(new ImageIcon(m_uiContext.getFlagImageFactory().getFlag(player)));

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

    private void showHistory()
    {
        m_inHistory = true;
        setWidgetActivation();

        GameData clonedGameData; 
        m_data.acquireReadLock();
        try
        {
            //we want to use a clone of the data, so we can make changes to it
            //as we walk up and down the history
            clonedGameData = GameDataUtils.cloneGameData(m_data);
            if (clonedGameData == null)
                return;
            
            clonedGameData.testLocksOnRead();
            
            if(m_historySyncher != null)
                throw new IllegalStateException("Two history synchers?");
    
            m_historySyncher = new HistorySynchronizer(clonedGameData, m_game);
        }
        finally
        {
            m_data.releaseReadLock();
        }

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
        m_historyTree = new HistoryPanel(clonedGameData, historyDetailPanel, m_uiContext);
        split.setLeftComponent(m_historyTree);
        split.setRightComponent(m_mapAndChatPanel);
        split.setDividerLocation(150);

        m_historyPanel.add(split, BorderLayout.CENTER);
        m_historyPanel.add(m_rightHandSidePanel, BorderLayout.EAST);
        m_historyPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);

        getContentPane().removeAll();
        getContentPane().add(m_historyPanel, BorderLayout.CENTER);
        validate();
    }


    
    @SuppressWarnings("deprecation")
    @Override
    public void show()
    {
        super.show();
        
    }

    public void showGame()
    {
        m_inHistory = false;

        if (m_historySyncher != null)
        {
            m_historySyncher.deactivate();
            m_historySyncher = null;
        }

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
        m_gameMainPanel.add(m_mapAndChatPanel, BorderLayout.CENTER);
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

    Action getShowGameAction()
    {
        return m_showGameAction;
    }
    
    Action getShowHistoryAction()
    {
        return m_showHistoryAction;
    }
    
    public UIContext getUIContext()
    {
        return m_uiContext;
    }
    
    MapPanel getMapPanel()
    {
        return m_mapPanel;
    }
    
    // Beagle Code Called to Change Mapskin
    void updateMap(String mapdir) throws IOException
    {
        m_uiContext.setMapDir(m_data, mapdir);
        //when changing skins, always show relief images
        if(m_uiContext.getMapData().getHasRelief())
        {
            TileImageFactory.setShowReliefImages(true);
        }
        
        m_mapPanel.setGameData(m_data);

        // update mappanels to use new image
        
        m_mapPanel.changeImage(m_uiContext.getMapData().getMapDimensions());

        Image small = m_uiContext.getMapImage().getSmallMapImage();
        m_smallView.changeImage(small);

        m_mapPanel.resetMap(); // redraw territories
    }
    
    public IGame getGame()
    {
        return m_game;
    }

    public StatPanel getStatPanel()
    {
        return m_statsPanel;
    }
}




class PlayerChatRenderer extends DefaultListCellRenderer
{
    private final IGame m_game;
    private final UIContext m_uiContext;
    
    PlayerChatRenderer(IGame game, UIContext uiContext)
    {
        m_game = game;
        m_uiContext  = uiContext;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
       super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
       
       Set<String> players = m_game.getPlayerManager().getPlayedBy(value.toString());
       
       if(players.size() > 0)
       {
           setHorizontalTextPosition(SwingConstants.LEFT);
           List<Icon> icons = new ArrayList<Icon>(players.size());
           for(String player : players)
           {
               PlayerID playerID;
               m_game.getData().acquireReadLock();
               try
               {
                   playerID = m_game.getData().getPlayerList().getPlayerID(player);
               } 
               finally
               {
                   m_game.getData().releaseReadLock();
               }
               
               icons.add(new ImageIcon( m_uiContext.getFlagImageFactory().getSmallFlag( playerID )));
           }
           setIcon(new CompositeIcon(icons));
       }
       
       return this;
    }
    
}


class CompositeIcon implements Icon
{
    private static final int GAP = 2;
    
    private final List<Icon> m_incons;
    
    CompositeIcon(List<Icon> icons)
    {
        m_incons = icons;
    }

    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        int dx = 0;
        for(Icon icon : m_incons)
        {
            icon.paintIcon(c, g, x + dx, y);
            dx += GAP;
            dx += icon.getIconWidth();
        }
        
    }

    public int getIconWidth()
    {
        int sum = 0;
        for(Icon icon : m_incons)
        {
            sum += icon.getIconWidth();
            sum += GAP;
        }
        return sum;
    }

    public int getIconHeight()
    {
        int max = 0;
        for(Icon icon : m_incons)
        {
            max = Math.max(icon.getIconHeight(), max);
        
        }
        return max;
        
    }
    
}
