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

/**
 *
 * @author  Sean Bridges
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
  private HistoryPanel m_historyTree ;


  /** Creates new TripleAFrame */
  public TripleAFrame(IGame game, Set players) throws IOException
  {
    super("TripleA");
    TaskTimer total = new TaskTimer("Loading game");

    setIconImage(GameRunner.getGameIcon(this));

    m_game = game;

    game.getMessenger().addErrorListener(m_messengerErrorListener);

    m_data = game.getData();
    m_localPlayers = players;

    game.addGameStepListener(m_stepListener);

    this.addWindowListener(WINDOW_LISTENER);

    createMenuBar();

    TerritoryData.getInstance().verify(m_data);



    TaskTimer loadMaps = new TaskTimer("Loading maps");
    MapImage.getInstance().loadMaps(m_data);

    Image small = MapImage.getInstance().getSmallMapImage();
    m_smallView = new MapPanelSmallView(small);

    Image large =  MapImage.getInstance().getLargeMapImage();
    m_mapPanel = new MapPanel(large,m_data, m_smallView);
    m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);

    loadMaps.done();

    TaskTimer loadFlags = new TaskTimer("Loading flag images");
    FlagIconImageFactory.instance().load(this);
    loadFlags.done();


    //link the small and large images
    new ImageScrollControl(m_mapPanel, m_smallView);


    m_gameMainPanel.setLayout(new BorderLayout());

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);

    m_gameSouthPanel = new JPanel();
    m_gameSouthPanel.setLayout(new BorderLayout());
    m_gameSouthPanel.add(m_message, BorderLayout.CENTER);
    m_message.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    m_gameSouthPanel.add(m_step, BorderLayout.EAST);
    m_step.setBorder(new EtchedBorder(EtchedBorder.RAISED));


    m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);

    JPanel gameCenterPanel = new JPanel();
    gameCenterPanel.setLayout(new BorderLayout());

    JPanel mapBorderPanel = new JPanel();
    mapBorderPanel.setLayout(new BorderLayout());
    mapBorderPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    mapBorderPanel.add(m_mapPanel, BorderLayout.CENTER);

    gameCenterPanel.add(mapBorderPanel, BorderLayout.CENTER);


    m_rightHandSidePanel.setLayout(new BorderLayout());
    m_rightHandSidePanel.add(m_smallView, BorderLayout.NORTH);

    m_tabsPanel.setBorder(null);
    m_rightHandSidePanel.add(m_tabsPanel, BorderLayout.CENTER);

    m_actionButtons = new ActionButtons(m_data, m_mapPanel, this);
    m_tabsPanel.addTab( "Actions", m_actionButtons);
    m_actionButtons.setBorder(null);

    m_statsPanel = new StatPanel(m_data);
    m_tabsPanel.addTab("Stats", m_statsPanel);

    m_details = new TerritoryDetailPanel(m_mapPanel, m_data);
    m_tabsPanel.addTab("Territory", m_details);

    m_rightHandSidePanel.setPreferredSize(new Dimension((int) m_smallView.getPreferredSize().getWidth(), (int) m_mapPanel.getPreferredSize().getHeight()));
    gameCenterPanel.add(m_rightHandSidePanel, BorderLayout.EAST);

    m_gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);
    total.done();

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
    JMenuItem menuFileSave = new JMenuItem( new AbstractAction( "Save..." )
      {
        public void actionPerformed(ActionEvent e)
        {
            if(!m_game.canSave())
          {
            JOptionPane.showMessageDialog(TripleAFrame.this, "You cannot save the game if you are playing as a client", "Cant save", JOptionPane.OK_OPTION);
            return;
          }

          GameDataManager manager = new GameDataManager();

          try
          {
            JFileChooser fileChooser = SaveGameFileChooser.getInstance();

            int rVal = fileChooser.showSaveDialog(TripleAFrame.this);
            if(rVal == JFileChooser.APPROVE_OPTION)
            {
              File f = fileChooser.getSelectedFile();
              if(!f.getName().toLowerCase().endsWith(".svg"))
              {
                f= new File(f.getParent(), f.getName() + ".svg");
              }

              manager.saveGame(f, m_data);
              JOptionPane.showMessageDialog(TripleAFrame.this, "Game Saved", "Game Saved",  JOptionPane.PLAIN_MESSAGE);
            }

          } catch(Exception se)
          {
            se.printStackTrace();
            JOptionPane.showMessageDialog(TripleAFrame.this, se.getMessage(), "Error Saving Game", JOptionPane.OK_OPTION);
          }
        }
      }
    );
    menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

    fileMenu.add( menuFileSave );


    /* Following change was made for personal convenience */
    JMenuItem menuFileExit = new JMenuItem( new AbstractAction("Exit")
      {
        public void actionPerformed(ActionEvent e)
        {
          shutdown();
        }
      }
    );
    menuFileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
    fileMenu.add( menuFileExit );

    JMenu menuGame = new JMenu("Game");
    menuGame.add(m_showGameAction);
    menuGame.add(m_showHistoryAction);

    menuBar.add(menuGame);




    if(!m_game.getData().getProperties().getEditableProperties().isEmpty())
    {


      AbstractAction optionsAction = new AbstractAction("View Game Options...")
      {
        public void actionPerformed(ActionEvent e)
        {
          PropertiesUI ui = new PropertiesUI(m_game.getData().getProperties(), false);
          JOptionPane.showMessageDialog(TripleAFrame.this, ui, "Game options", JOptionPane.PLAIN_MESSAGE );
        }
      };
      menuGame.addSeparator();
      menuGame.add(optionsAction);

    }



    JMenu helpMenu = new JMenu("Help");
    menuBar.add(helpMenu);
    helpMenu.add( new AbstractAction("About...")
      {
        public void actionPerformed(ActionEvent e)
        {
          String text = "<b>TripleA</b>  " +  games.strategy.engine.EngineVersion.VERSION.toString() + "<br>" +
                        "Game: " + m_data.getGameName() + ". Version:" + m_data.getGameVersion() + "<br><br>" +
                        "<a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a>" ;

          JEditorPane editorPane = new JEditorPane();
          editorPane.setEditable(false);
          editorPane.setContentType("text/html");
          editorPane.setText(text);


          JScrollPane scroll = new JScrollPane(editorPane);

          JOptionPane.showMessageDialog(TripleAFrame.this, scroll, "About", JOptionPane.PLAIN_MESSAGE);
        }
      }
    );

    helpMenu.add(new AbstractAction("Hints...")
    {
      public void actionPerformed(ActionEvent e)
      {
        //html formatted string
        String hints =
          "To force a path while moving, right click on each territory in turn.<br><br>" +
          "You may be able to set game properties such as a bid in the Properties tab at game start up.";
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setText(hints);

        JScrollPane scroll = new JScrollPane(editorPane);

        JOptionPane.showMessageDialog(TripleAFrame.this, editorPane, "Hints",
                                      JOptionPane.PLAIN_MESSAGE);
      }
    }
    );



        //allow the game developer to write notes that appear in the game
        //displays whatever is in the notes field in html
        final String notes = (String) m_data.getProperties().get("notes");
        if(notes != null && notes.trim().length() != 0)
        {


          helpMenu.add( new AbstractAction("Game Notes...")
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
              }
          );

        }


    this.setJMenuBar(menuBar);
  }

  private final WindowListener WINDOW_LISTENER = new WindowAdapter()
  {
    public void windowClosing(WindowEvent e)
    {
      shutdown();
    }
  };

  public final MapSelectionListener  MAP_SELECTION_LISTENER = new MapSelectionListener ()
  {
    Territory in;

    public void territorySelected(Territory territory, MouseEvent me)
    {}

    public void mouseEntered(Territory territory)
    {
      in = territory;
      refresh();
    }

    void refresh()
    {
      StringBuffer buf = new StringBuffer();
      buf.append(in == null ? "none" : in.getName());
      if(in != null)
      {
        TerritoryAttatchment ta = TerritoryAttatchment.get(in);
        if(ta != null)
        {
          int production = ta.getProduction();
          if(production > 0)
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

  public MoveMessage getMove(PlayerID player, PlayerBridge bridge, boolean nonCombat)
  {
    m_actionButtons.changeToMove(player, nonCombat);
    return m_actionButtons.waitForMove(bridge);
  }

  public PlaceMessage getPlace(PlayerID player, boolean bid, PlayerBridge bridge)
  {
    m_actionButtons.changeToPlace(player);
    return m_actionButtons.waitForPlace(bid, bridge);
  }

  public Message listBattle(BattleStepMessage msg)
  {
    return m_actionButtons.listBattle(msg);
  }

  public FightBattleMessage getBattle(PlayerID player, Collection battles, Collection bombingRaids)
  {
    m_actionButtons.changeToBattle(player, battles, bombingRaids);
    return m_actionButtons.waitForBattleSelection();
  }

  public SelectCasualtyMessage getCasualties( SelectCasualtyQueryMessage msg)
  {
    return m_actionButtons.getCasualties(msg);
  }

  public Message battleStringMessage(BattleStringMessage message)
  {
    return m_actionButtons.battleStringMessage(message);
  }
  public void casualtyNoticicationMessage(CasualtyNotificationMessage message)
  {
    m_actionButtons.casualtyNoticicationMessage( message);
  }


  public RetreatMessage getRetreat(RetreatQueryMessage rqm)
  {
    return m_actionButtons.getRetreat(rqm);
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
    JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
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
    JOptionPane.showMessageDialog(this, message, message, JOptionPane.PLAIN_MESSAGE);
  }

  public boolean getOKToLetAirDie(String message)
  {
    String ok = "Kill air";
    String cancel = "Keep moving";
    String[] options = {cancel,ok};
    int choice = JOptionPane.showOptionDialog(this, message, "Air cannot land", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, cancel);
    return choice == 1;
  }

  public boolean getOK(String message)
  {
    int choice = JOptionPane.showConfirmDialog(this, message, message, JOptionPane.OK_CANCEL_OPTION);
    return choice == JOptionPane.OK_OPTION;
  }

  public void notifyTechResults(TechResultsMessage msg)
  {
    TechResultsDisplay display = new TechResultsDisplay(msg);
    if(msg.getHits() != 0)
    {
        Match match = Matches.territoryHasUnitsOwnedBy(msg.getPlayer());
        Collection updatedCountries = Match.getMatches( m_data.getMap().getTerritories(), match);
        m_mapPanel.updateCounties(updatedCountries);
    }
    JOptionPane.showOptionDialog(this, display, "Tech roll", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] {"OK"}, "OK");


  }

  public boolean getStrategicBombingRaid(StrategicBombQuery query)
  {
    String message = "Bomb in " + query.getLocation().getName();
    String bomb = "Bomb";
    String normal = "Attack";
    String[] choices = {bomb, normal};
    int choice = JOptionPane.showOptionDialog(this, message, "Bomb?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, choices, bomb);
    return choice == 0;
  }

  public IntegerMessage getTechRolls(PlayerID id)
  {
    m_actionButtons.changeToTech(id);
    return m_actionButtons.waitForTech();
  }

  public TerritoryMessage getRocketAttack(Collection territories)
  {
    JList list = new JList(new Vector(territories));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(0);
    JScrollPane scroll = new JScrollPane(list);
    String[] options = {"OK", "Dont attack"};
    int selection = JOptionPane.showOptionDialog(this, scroll, "Select rocket attack", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);

    Territory selected = null;
    if(selection == 0) //OK
      selected = (Territory) list.getSelectedValue();

    return new TerritoryMessage(selected);
  }

  public boolean playing(PlayerID id)
  {
    if(id == null)
      return false;

    Iterator iter = m_localPlayers.iterator();
    while(iter.hasNext())
    {
      TripleAPlayer player = (TripleAPlayer) iter.next();
      if(player.getID().equals(id))
        return true;
    }
    return false;
  }



  private  IMessengerErrorListener m_messengerErrorListener = new IMessengerErrorListener()
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



  public static int save(String filename, GameData m_data) {
    FileOutputStream fos = null;
    ObjectOutputStream oos = null;

    try {
      fos = new FileOutputStream( filename );
      oos = new ObjectOutputStream( fos );

      oos.writeObject( m_data );

      return 0;
    }
    catch (Throwable t) {
      // t.printStackTrace();
      System.err.println(t.getMessage());
      return -1;
    }
    finally {
      try { fos.flush(); } catch (Exception ignore) { }
      try { oos.close(); } catch (Exception ignore) { }
    }
  }

  GameStepListener m_stepListener = new GameStepListener()
  {

    public void gameStepChanged(String stepName, String delegateName,
                                PlayerID player, int round)
    {
        m_step.setText("Round: " + round + " ");
        if(player != null)
            m_step.setIcon(new ImageIcon(FlagIconImageFactory.instance().getFlag(player)));
    }
  };

  public void showHistory()
  {
       m_actionButtons.getCurrent().setActive(false);
      //we want to use a clone of the data, so we can make changes to it
      GameData clonedGameData = cloneGameData();
      if(clonedGameData == null)
          return;

      clonedGameData.getAllianceTracker();

      m_statsPanel.setGameData(clonedGameData);
      m_details.setGameData(clonedGameData);
      m_mapPanel.setGameData(clonedGameData);

       HistoryDetailsPanel historyDetailPanel = new HistoryDetailsPanel(clonedGameData, m_mapPanel);

      m_tabsPanel.removeAll();
      m_tabsPanel.add("History", historyDetailPanel);
      m_tabsPanel.add("Stats", m_statsPanel);
      m_tabsPanel.add("Territory", m_details);



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
          manager.saveGame(sink, m_data);
          sink.close();
          ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
          sink = null;
          return manager.loadGame(source);
      }
      catch (IOException ex)
      {
          ex.printStackTrace();
          return null;
      }
  }

  public void showGame()
  {
      m_actionButtons.getCurrent().setActive(true);
      m_historyTree.goToEnd();
      m_historyTree = null;

      m_statsPanel.setGameData(m_data);
      m_details.setGameData(m_data);
      m_mapPanel.setGameData(m_data);

      m_tabsPanel.removeAll();
      m_tabsPanel.add("Action", m_actionButtons);
      m_tabsPanel.add("Territory", m_details);
      m_tabsPanel.add("Stats", m_statsPanel);

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

  private AbstractAction m_showHistoryAction = new AbstractAction("Show history")
  {
      public void actionPerformed(ActionEvent e)
      {
          showHistory();
          setEnabled(false);
          m_showGameAction.setEnabled(true);
      };
  };

  private AbstractAction m_showGameAction = new AbstractAction("Show current game")
  {
      {
          setEnabled(false);
      }

      public void actionPerformed(ActionEvent e)
      {
          showGame();
          setEnabled(false);
          m_showHistoryAction.setEnabled(true);

      };
  };

}
