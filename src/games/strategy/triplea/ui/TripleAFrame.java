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
import games.strategy.engine.transcript.*;
import games.strategy.engine.data.properties.PropertiesUI;

import games.strategy.ui.*;
import games.strategy.util.*;
import games.strategy.net.*;

import games.strategy.triplea.*;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.image.*;
import games.strategy.triplea.delegate.message.*;

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
  private ImageScrollerSmallView m_smallView;
  private JLabel m_message = new JLabel("No selection");
  private ActionButtons m_actionButtons;
  //a set of TripleAPlayers
  private Set m_localPlayers;

  /** Creates new TripleAFrame */
  public TripleAFrame(IGame game, Set players) throws IOException
  {
    super("TripleA");

    m_game = game;

    game.getMessenger().addErrorListener(m_messengerErrorListener);

    m_data = game.getData();
    m_localPlayers = players;

    game.getTranscript().addTranscriptListener(m_transcriptListener);

    this.addWindowListener(WINDOW_LISTENER);

    createMenuBar();

    System.out.print("Loading unit images");
    long now = System.currentTimeMillis();
    UnitIconImageFactory.instance().load(this);
    System.out.println(" done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");;

    System.out.print("Loading flag images");
    now = System.currentTimeMillis();
    FlagIconImageFactory.instance().load(this);
    System.out.println(" done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");

    System.out.print("Loading maps");
    now = System.currentTimeMillis();
    MapImage.getInstance().loadMaps(m_data);

    Image small = MapImage.getInstance().getSmallMapImage();
    m_smallView = new ImageScrollerSmallView(small);

    Image large =  MapImage.getInstance().getLargeMapImage();
    m_mapPanel = new MapPanel(large,m_data, m_smallView);
    m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);

    System.out.println(" done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");

    ImageScrollControl control = new ImageScrollControl(m_mapPanel, m_smallView);

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(m_message, BorderLayout.SOUTH);

    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());

    JPanel mapBorderPanel = new JPanel();
    mapBorderPanel.setLayout(new BorderLayout());
    mapBorderPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    mapBorderPanel.add(m_mapPanel, BorderLayout.CENTER);

    mainPanel.add(mapBorderPanel, BorderLayout.CENTER);

    JPanel rightHandSide = new JPanel();
    rightHandSide.setLayout(new BorderLayout());
    rightHandSide.add(m_smallView, BorderLayout.NORTH);

    JTabbedPane tabs = new JTabbedPane();
    tabs.setBorder(new EtchedBorder());
    rightHandSide.add(tabs, BorderLayout.CENTER);

    m_actionButtons = new ActionButtons(m_data, m_mapPanel, this);
    tabs.addTab( "Actions", m_actionButtons);

    StatPanel stats = new StatPanel(m_data);
    tabs.addTab("Stats", stats);

    rightHandSide.setPreferredSize(new Dimension((int) m_smallView.getPreferredSize().getWidth(), (int) m_mapPanel.getPreferredSize().getHeight()));
    mainPanel.add(rightHandSide, BorderLayout.EAST);

    this.getContentPane().add(mainPanel, BorderLayout.CENTER);
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
    JMenuItem menuFileSave = new JMenuItem( new AbstractAction( "Save" )
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
              manager.saveGame(f, m_data);
              JOptionPane.showMessageDialog(TripleAFrame.this, "Game Saved", "Game Saved", JOptionPane.OK_OPTION, null);
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


    if(!m_game.getData().getProperties().getEditableProperties().isEmpty())
    {
      JMenu optionsMenu = new JMenu("Options");

      AbstractAction optionsAction = new AbstractAction("View Game Options...")
      {
        public void actionPerformed(ActionEvent e)
        {
          PropertiesUI ui = new PropertiesUI(m_game.getData().getProperties(), false);
          JOptionPane.showMessageDialog(TripleAFrame.this, ui, "Game options", JOptionPane.PLAIN_MESSAGE );
        }
      };

      optionsMenu.add(optionsAction);
      menuBar.add(optionsMenu);
    }

    JMenu helpMenu = new JMenu("Help");
    menuBar.add(helpMenu);
    helpMenu.add( new AbstractAction("About")
      {
        public void actionPerformed(ActionEvent e)
        {
          String text = "Engine version: " +  games.strategy.engine.EngineVersion.VERSION.toString() + "<br>" +
                        "Game: " + m_data.getGameName() + " Version:" + m_data.getGameVersion() + "<br><br>" +
                        "<a hlink='http://sourceforge.net/projects/triplea'>http://sourceforge.net/projects/triplea</a>" ;

          JEditorPane editorPane = new JEditorPane();
          editorPane.setEditable(false);
          editorPane.setContentType("text/html");
          editorPane.setText(text);


          JScrollPane scroll = new JScrollPane(editorPane);
          scroll.setMinimumSize(new Dimension(150,200));
          scroll.setPreferredSize(new Dimension(150,200));



          JOptionPane.showMessageDialog(TripleAFrame.this, scroll, "TripleA", JOptionPane.PLAIN_MESSAGE);
        }
      }
    );

        helpMenu.add( new AbstractAction("Hints")
        {
          public void actionPerformed(ActionEvent e)
          {
            //html formatted string
            String hints = "To select a path when moving, right click on territories";
            JEditorPane editorPane = new JEditorPane();
            editorPane.setEditable(false);
            editorPane.setContentType("text/html");
            editorPane.setText(hints);

            JScrollPane scroll = new JScrollPane(editorPane);

            JOptionPane.showMessageDialog(TripleAFrame.this, editorPane, "TripleA", JOptionPane.PLAIN_MESSAGE);
          }
        }
        );



        //allow the game developer to write notes that appear in the game
        //displays whatever is in the notes field in html
        final String notes = (String) m_data.getProperties().get("notes");
        if(notes != null && notes.trim().length() != 0)
        {
          JMenu notesMenu = new JMenu("Notes");
          menuBar.add(notesMenu);
          notesMenu.add( new AbstractAction("Game Notes")
              {
                  public void actionPerformed(ActionEvent e)
                  {

                      JEditorPane editorPane = new JEditorPane();
                      editorPane.setEditable(false);
                      editorPane.setContentType("text/html");
                      editorPane.setText(notes);

                      JScrollPane scroll = new JScrollPane(editorPane);

                      JOptionPane.showMessageDialog(TripleAFrame.this, editorPane, "Notes", JOptionPane.PLAIN_MESSAGE);
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

  public PlaceMessage getPlace(PlayerID player, boolean bid)
  {
    m_actionButtons.changeToPlace(player);
    return m_actionButtons.waitForPlace(bid);
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

  private ITranscriptListener m_transcriptListener = new ITranscriptListener()
  {
    public void messageRecieved(TranscriptMessage msg)
    {
      if(msg.getChannel() == TranscriptMessage.PRIORITY_CHANNEL)
      {
        JOptionPane.showMessageDialog(TripleAFrame.this, msg.getMessage(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
      }
    }
  };

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

}
