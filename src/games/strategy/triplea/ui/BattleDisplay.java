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

import java.util.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.font.*;
import java.awt.event.*;

import games.strategy.engine.data.events.GameDataChangeListener;
import java.util.List;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.engine.data.*;
import games.strategy.ui.Util;
import games.strategy.util.*;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.message.*;
import games.strategy.engine.message.Message;

/**
 * Displays a running battle
 */

public class BattleDisplay extends JPanel
{
  private PlayerID m_defender;
  private PlayerID m_attacker;
  private Territory m_location;
  private GameData m_data;

  private JButton m_selectCasualtiesButton = new JButton("");

  private BattleModel m_defenderModel;
  private BattleModel m_attackerModel;
  private BattleStepsPanel m_steps;

  private Object m_selectCasualtyLock = new Object();
  private SelectCasualtyMessage m_selectCasualtyResponse;

//  private Image m_offscreen;

//  private final Dimension SIZE = new Dimension(500,200);

  public BattleDisplay(GameData data, Territory territory, PlayerID attacker, PlayerID defender)
  {
    m_defender = defender;
    m_attacker = attacker;
    m_location = territory;
    m_data = data;

    m_defenderModel = new BattleModel(m_data, m_location, m_defender, false);
    m_defenderModel.refresh();
    m_attackerModel = new BattleModel(m_data, m_location, m_attacker, true);
    m_attackerModel.refresh();

    initLayout();

    m_data.addDataChangeListener(new GameDataChangeListener()
    {
      public void gameDataChanged()
      {
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            m_defenderModel.refresh();
            m_attackerModel.refresh();
          }
        }
        );
      }
    }
    );
  }

  public SelectCasualtyMessage getCasualties(final PlayerID player,final SelectCasualtyQueryMessage msg)
  {
    boolean plural = msg. getCount() > 1;
    final String btnText =player.getName() + " select " + msg.getCount() + (plural ? " casualties" :" casualty");
    m_selectCasualtiesButton.setEnabled(true);

    m_selectCasualtiesButton.setAction(new AbstractAction(btnText)
    {
      public void actionPerformed(ActionEvent e)
      {

        String messageText = msg.getMessage() + " " + btnText + ".";
        UnitChooser chooser = new UnitChooser(msg.getSelectFrom(), msg.getDependent(), m_data);

        chooser.setTitle(messageText);
        chooser.setMax(msg.getCount());
        String[] options = {"OK"};
        int option = JOptionPane.showOptionDialog(BattleDisplay.this, chooser, player.getName() + " select casualties", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
        Collection choosen = chooser.getSelected(false);

        if(choosen.size() != msg.getCount())
        {
          JOptionPane.showMessageDialog(BattleDisplay.this, "Wrong number of casualties choosen", player.getName() + " select casualties", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
          SelectCasualtyMessage response = new SelectCasualtyMessage(choosen);
          m_selectCasualtyResponse = response;
          synchronized(m_selectCasualtyLock)
          {
            m_selectCasualtyLock.notifyAll();
          }
        }
      }
    });

    try
    {
      synchronized(m_selectCasualtyLock)
      {
          m_selectCasualtyLock.wait();
      }
    }
    catch (InterruptedException ex)
    {
    }

    m_selectCasualtiesButton.setEnabled(false);
    m_selectCasualtiesButton.setAction(null);
    SelectCasualtyMessage rVal = m_selectCasualtyResponse;
    m_selectCasualtyResponse = null;
    return rVal;

  }



  private void initLayout()
  {
    JPanel attackerUnits = new JPanel();
    attackerUnits.setLayout(new BoxLayout(attackerUnits, BoxLayout.Y_AXIS));
    attackerUnits.add(getPlayerComponent(m_attacker));
    JTable attackerTable = new BattleTable(m_attackerModel);
    attackerUnits.add(attackerTable);
    attackerUnits.add(attackerTable.getTableHeader());

    JPanel defenderUnits = new JPanel();
    defenderUnits.setLayout(new BoxLayout(defenderUnits, BoxLayout.Y_AXIS));
    defenderUnits.add(getPlayerComponent(m_defender));
    JTable defenderTable = new BattleTable(m_defenderModel);
    defenderUnits.add(defenderTable);
    defenderUnits.add(defenderTable.getTableHeader());

    JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
    north.add(attackerUnits);
    north.add(getTerritoryComponent());
    north.add(defenderUnits);

    m_steps = new BattleStepsPanel();

    setLayout(new BorderLayout());
    add(north, BorderLayout.NORTH);
    add(m_steps, BorderLayout.CENTER);

    add(m_selectCasualtiesButton, BorderLayout.SOUTH);
    m_selectCasualtiesButton.setEnabled(false);
  }

  public void setStep(BattleMessage message)
  {
    m_steps.setStep(message);
  }

  public Message battleInfo(BattleInfoMessage msg)
  {
    setStep(msg);
    String ok = "OK";
    String[] options =  {ok};
    JOptionPane.showOptionDialog(this, msg.getMessage(), msg.getShortMessage(), JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, ok);

    return null;
  }

  public void listBattle(BattleStepMessage message)
  {
    m_steps.listBattle(message);
  }

  private JComponent getPlayerComponent(PlayerID id)
  {
    JLabel player = new JLabel(id.getName());
    player.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));
    player.setFont(player.getFont().deriveFont((float) 14));
    return player;
  }

  private JComponent getTerritoryComponent()
  {
    final int WIDTH = 100;
    final int HEIGHT = 100;
    Image finalImage = Util.createImage(WIDTH, HEIGHT);
    if(m_location.isWater())
    {
      finalImage.getGraphics().setColor(Color.blue);
      finalImage.getGraphics().fillRect(0,0,WIDTH, HEIGHT);
    }
    else
    {
      Image territory = games.strategy.triplea.image.MapImage.getTerritoryImage(m_location, m_defender);
      finalImage.getGraphics().drawImage(territory,  0,0,WIDTH, HEIGHT, this);
    }

    return new JLabel( new ImageIcon(finalImage));

  }
}

class BattleTable extends JTable
{
  BattleTable(BattleModel model)
  {
    super(model);
    setDefaultRenderer(Object.class, new Renderer());
    setRowHeight(UnitIconImageFactory.UNIT_ICON_HEIGHT + 5);
    setBackground(new JButton().getBackground());
    setShowHorizontalLines(false);

    getTableHeader().setReorderingAllowed(false);
    getTableHeader().setResizingAllowed(false);
  }
}

class BattleModel extends DefaultTableModel
{

  private Territory m_location;
  private GameData m_data;
  private PlayerID m_player;
  //is the player the agressor?
  private boolean m_attack;

  BattleModel(GameData data, Territory territory, PlayerID player, boolean attack)
  {
    super(new Object[0][0], new String[] {" ", "1", "2", "3", "4", "5"});
    m_player = player;
    m_location = territory;
    m_data = data;
    m_attack = attack;
  }


  public void refresh()
  {
    List[] columns = new List[6];
    for(int i = 0; i < columns.length; i++)
    {
      columns[i] = new ArrayList();
    }

    Collection players = m_location.getUnits().getPlayersWithUnits();
    Iterator playerIter = players.iterator();
    while(playerIter.hasNext())
    {
      PlayerID player = (PlayerID) playerIter.next();
      if(!m_data.getAllianceTracker().isAllied(m_player, player))
        continue;

      IntegerMap units = m_location.getUnits().getUnitsByType(player);
      Iterator unitIter = units.keySet().iterator();
      while(unitIter.hasNext())
      {
        UnitType unit = (UnitType) unitIter.next();
        int strength;
        if(m_attack)
          strength = UnitAttatchment.get(unit).getAttack(player);
        else
          strength = UnitAttatchment.get(unit).getDefense(player);

        columns[strength].add(new TableData(player, units.getInt(unit), unit, m_data));
      }
    }

    int rowCount = 0;
    for(int i = 0; i < columns.length; i++)
    {
      rowCount = Math.max(rowCount, columns[i].size());
    }

    setNumRows(rowCount);

    for(int row = 0; row < rowCount; row++)
    {
      for(int column = 0; column < columns.length; column++)
      {
        if(columns[column].size() > row)
        {
          setValueAt(columns[column].get(row), row, column);
        }
        else
        {
          setValueAt(TableData.NULL, row, column);
        }
      }
    }
  }


}

class Renderer implements TableCellRenderer
{

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
  {
    return ((TableData) value).getStamp();
  }
}

class TableData
{
  static TableData NULL = new TableData();

  private JLabel m_stamp = new JLabel();

  private TableData()
  {

  }

  TableData(PlayerID player, int count, UnitType type, GameData data)
  {
    m_stamp.setText("x" + count);
    m_stamp.setIcon(UnitIconImageFactory.instance().getIcon(type, player, data));
  }


  public JLabel getStamp()
  {
    return m_stamp;
  }

}



class BattleStepsPanel extends JPanel
{
  private DefaultListModel m_listModel = new DefaultListModel();
  private JList m_list = new JList(m_listModel);
  private MyListSelectionModel m_listSelectionModel = new MyListSelectionModel();

  public BattleStepsPanel()
  {
    final int WIDTH = 150;
    final int HEIGHT = 300;
    setMinimumSize(new Dimension(WIDTH, HEIGHT));
    setMaximumSize(new Dimension(WIDTH, HEIGHT));
    setPreferredSize(new Dimension(WIDTH, HEIGHT));

    setLayout(new BorderLayout());
    add(m_list, BorderLayout.CENTER);
    m_list.setBackground(this.getBackground());
    m_list.setSelectionModel(m_listSelectionModel);
  }


  public Message battleStringMessage(BattleStringMessage message)
  {
    setStep(message);
    JOptionPane.showMessageDialog(getRootPane(), message.getMessage(), message.getMessage(), JOptionPane.PLAIN_MESSAGE);
    return null;
  }

  public void listBattle(BattleStepMessage msg)
  {
    m_listModel.removeAllElements();


    Iterator iter = msg.getSteps().iterator();
    while(iter.hasNext())
    {
      m_listModel.addElement(iter.next());
    }
    m_listSelectionModel.hiddenSetSelectionInterval(0);

  }


  /**
   * Walks through and pause at each list item.
   */
  private void walkStep(final int start, final int stop)
  {
    if(start < 0 || stop < 0 || stop >= m_listModel.getSize())
      throw new IllegalStateException("Illegal start and stop.  start:" + start + " stop:" + stop);

    Object lock = new Object();

    int current = start;
    while(current != stop)
    {
      if(current == 0)
        pause();

      current++;
      if( current >= m_listModel.getSize())
      {
        current = 0;
      }

      final int set = current;
      Runnable r = new Runnable()
      {
        public void run()
        {
          m_listSelectionModel.hiddenSetSelectionInterval(set);
        }
      };

      try
      {
        SwingUtilities.invokeAndWait(r);
        pause();
        } catch(InterruptedException ie)
        {
          } catch(java.lang.reflect.InvocationTargetException ioe)
          {
            ioe.printStackTrace();
            throw new RuntimeException(ioe.getMessage());
          }
    }
  }


  /**
   * Doesnt allow the user to change the selection,
   * must be done through hiddenSetSelectionInterval.
   */
  class MyListSelectionModel extends DefaultListSelectionModel
  {
    public void setSelectionInterval(int index0, int index1)
    {
    }

    public void hiddenSetSelectionInterval(int index)
    {
      super.setSelectionInterval(index, index);
    }
  }

  private void pause()
  {
    Object lock = new Object();
    try
    {
      synchronized(lock)
      {
        lock.wait(850);
      }
      } catch(InterruptedException ie)
      {
      }
  }

  public void setStep(BattleMessage msg)
  {
    if(msg.getStep() != null)
    {
      int newIndex = m_listModel.lastIndexOf(msg.getStep());
      int currentIndex = m_list.getSelectedIndex();
      if(newIndex != -1)
        walkStep(currentIndex,newIndex );
    }
  }

}