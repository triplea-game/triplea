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
import java.util.List;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.Util;

/**
 * Displays a running battle
 */

public class BattleDisplay extends JPanel
{
  private final String DICE_KEY = "D";
  private final String CASUALTIES_KEY = "C";

  private PlayerID m_defender;
  private PlayerID m_attacker;
  private Territory m_location;
  private GameData m_data;

  private JButton m_actionButton = new JButton("");

  private BattleModel m_defenderModel;
  private BattleModel m_attackerModel;
  private BattleStepsPanel m_steps;

  private Object m_continueLock = new Object();
  private SelectCasualtyMessage m_selectCasualtyResponse;

  private DicePanel m_dicePanel;
  private CasualtyNotificationPanel m_casualties;
  private JPanel m_actionPanel;
  private CardLayout m_actionLayout = new CardLayout();


//  private Image m_offscreen;

//  private final Dimension SIZE = new Dimension(500,200);

  public BattleDisplay(GameData data, Territory territory, PlayerID attacker, PlayerID defender, Collection attackingUnits, Collection defendingUnits)
  {
    m_defender = defender;
    m_attacker = attacker;
    m_location = territory;
    m_data = data;
    m_casualties = new CasualtyNotificationPanel(data);

    m_defenderModel = new BattleModel(m_data,defendingUnits, false);
    m_defenderModel.refresh();
    m_attackerModel = new BattleModel(m_data, attackingUnits, true);
    m_attackerModel.refresh();

    initLayout();
  }

  public void bombingResults(BombingResults message)
  {
    m_dicePanel.setDiceRoll(message);
    m_actionLayout.show(m_actionPanel, DICE_KEY);
  }


  public void casualtyNoticicationMessage(CasualtyNotificationMessage message, boolean waitFOrUserInput)
  {
    setStep(message);
    m_casualties.setNotication(message);
    m_actionLayout.show(m_actionPanel, CASUALTIES_KEY);

    if(message.getPlayer().equals(m_defender))
      m_defenderModel.removeCasualties(message);
    else
      m_attackerModel.removeCasualties(message);

    //if wait is true, then dont return until the user presses continue
    if(!waitFOrUserInput)
      return;

    m_actionButton.setAction(
        new AbstractAction("Continue")
    {

      public void actionPerformed(ActionEvent e)
      {
        synchronized(m_continueLock)
        {
          m_continueLock.notifyAll();
        }
      }
    }
    );

    try
    {
      synchronized(m_continueLock)
      {
        m_continueLock.wait();
      }
      } catch(InterruptedException ie)
      {

      }

    m_actionButton.setAction(null);
  }

  public void endBattle(BattleEndMessage msg)
  {
    m_steps.walkToLastStep();

    m_actionButton.setAction(
        new AbstractAction(msg.getMessage() + " : (Click to close)")
    {

      public void actionPerformed(ActionEvent e)
      {
        synchronized(m_continueLock)
        {
          m_continueLock.notifyAll();
        }
      }
    }
    );

    try
    {
      synchronized(m_continueLock)
      {
        m_continueLock.wait();
      }
      } catch(InterruptedException ie)
      {

      }

      m_actionButton.setAction(null);

  }

  public SelectCasualtyMessage getCasualties(final SelectCasualtyQueryMessage msg)
  {
    setStep(msg);
    m_actionLayout.show(m_actionPanel, DICE_KEY);
    m_dicePanel.setDiceRoll(msg.getDice());
    boolean plural = msg. getCount() > 1;
    final String btnText = msg.getPlayer().getName() + " select " + msg.getCount() + (plural ? " casualties" :" casualty");
    m_actionButton.setEnabled(true);

    m_actionButton.setAction(new AbstractAction(btnText)
    {
      public void actionPerformed(ActionEvent e)
      {

        String messageText = msg.getMessage() + " " + btnText + ".";
        UnitChooser chooser = new UnitChooser(msg.getSelectFrom(), msg.getDependent(), m_data);

        chooser.setTitle(messageText);
        chooser.setMax(msg.getCount());
        String[] options = {"Ok", "Cancel"};
        int option = JOptionPane.showOptionDialog(BattleDisplay.this, chooser, msg.getPlayer().getName() + " select casualties", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
        if(option != 0)
          return;
        Collection choosen = chooser.getSelected(false);

        if(choosen.size() != msg.getCount())
        {
          JOptionPane.showMessageDialog(BattleDisplay.this, "Wrong number of casualties choosen", msg.getPlayer().getName() + " select casualties", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
          SelectCasualtyMessage response = new SelectCasualtyMessage(choosen);
          m_selectCasualtyResponse = response;
          synchronized(m_continueLock)
          {
            m_continueLock.notifyAll();
          }
        }
      }
    });

    try
    {
      synchronized(m_continueLock)
      {
          m_continueLock.wait();
      }
    }
    catch (InterruptedException ex)
    {
    }

    m_dicePanel.clear();
    m_actionButton.setEnabled(false);
    m_actionButton.setAction(null);
    SelectCasualtyMessage rVal = m_selectCasualtyResponse;
    m_selectCasualtyResponse = null;
    return rVal;

  }



  private void initLayout()
  {

    JPanel attackerUnits = new JPanel();
    attackerUnits.setLayout(new BoxLayout(attackerUnits, BoxLayout.Y_AXIS));
    attackerUnits.add(getPlayerComponent(m_attacker));
    attackerUnits.add(Box.createGlue());
    JTable attackerTable = new BattleTable(m_attackerModel);
    attackerUnits.add(attackerTable);
    attackerUnits.add(attackerTable.getTableHeader());

    JPanel defenderUnits = new JPanel();
    defenderUnits.setLayout(new BoxLayout(defenderUnits, BoxLayout.Y_AXIS));
    defenderUnits.add(getPlayerComponent(m_defender));
    defenderUnits.add(Box.createGlue());
    JTable defenderTable = new BattleTable(m_defenderModel);
    defenderUnits.add(defenderTable);
    defenderUnits.add(defenderTable.getTableHeader());

    JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
    north.add(attackerUnits);
    north.add(getTerritoryComponent());
    north.add(defenderUnits);

    m_steps = new BattleStepsPanel();
    m_dicePanel = new DicePanel();

    m_actionPanel = new JPanel();
    m_actionPanel.setLayout(m_actionLayout);

    m_actionPanel.add(m_dicePanel, DICE_KEY);
    m_actionPanel.add(m_casualties, CASUALTIES_KEY);

    JPanel diceAndSteps = new JPanel();
    diceAndSteps.setLayout(new BorderLayout());
    diceAndSteps.add(m_steps, BorderLayout.WEST);
    diceAndSteps.add(m_actionPanel, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(north, BorderLayout.NORTH);
    add(diceAndSteps, BorderLayout.CENTER);

    add(m_actionButton, BorderLayout.SOUTH);
    m_actionButton.setEnabled(false);
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

    Image territory;
    if(m_location.isWater())
      //territory = games.strategy.triplea.image.MapImage.getWaterImage();
      territory = games.strategy.triplea.image.TerritoryImageFactory.getInstance().getWaterImage();
    else
      //territory = games.strategy.triplea.image.MapImage.getTerritoryImage(m_location, m_defender);
      territory = games.strategy.triplea.image.TerritoryImageFactory.getInstance().getTerritoryImage(m_location, m_defender);

    finalImage.getGraphics().drawImage(territory,  0, 0, WIDTH, HEIGHT, this);

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
//    getTableHeader().setResizingAllowed(false);
  }
}

class BattleModel extends DefaultTableModel
{
  private GameData m_data;
  //is the player the agressor?
  private boolean m_attack;
  private Collection m_units;

  BattleModel(GameData data, Collection units, boolean attack)
  {
    super(new Object[0][0], new String[] {" ", "1", "2", "3", "4", "5"});
    m_data = data;
    m_attack = attack;
    //were going to modify the units
    m_units = new ArrayList(units);
  }

  public void removeCasualties(CasualtyNotificationMessage msg)
  {
    m_units.removeAll(msg.getUnits());
    refresh();
  }

  /**
   * refresh the model from m_units
   */
  public void refresh()
  {
    List[] columns = new List[6];
    for(int i = 0; i < columns.length; i++)
    {
      columns[i] = new ArrayList();
    }

    Iterator categories = UnitSeperator.categorize(m_units).iterator();

    while(categories.hasNext())
    {
      UnitCategory category = (UnitCategory) categories.next();

      int strength;
      if(m_attack)
        strength = UnitAttatchment.get(category.getType()).getAttack(category.getOwner());
      else
        strength = UnitAttatchment.get(category.getType()).getDefense(category.getOwner());

      columns[strength].add(new TableData(category.getOwner(), category.getUnits().size(), category.getType(), m_data));
    }

    //find the number of rows
    //this will be the size of the largest column
    int rowCount = 1;
    for(int i = 0; i < columns.length; i++)
    {
      rowCount = Math.max(rowCount, columns[i].size());
    }

    setNumRows(rowCount);

    for(int row = 0; row < rowCount; row++)
    {
      for(int column = 0; column < columns.length; column++)
      {
        //if the column has that many items, add to the table, else add null
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

  public boolean isCellEditable(int row, int column)
  {
    return false;
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

  public void walkToLastStep()
  {
    if(m_list.getSelectedIndex() == m_list.getModel().getSize() - 1)
      return;
    walkStep(m_list.getSelectedIndex(), m_list.getModel().getSize() - 1);
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

class DicePanel extends JPanel
{
  public DicePanel()
  {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void clear()
  {
    removeAll();
  }

  public void setDiceRoll(BombingResults results)
  {
    removeAll();

    add(create(results.getDice(), -1));

    add(Box.createVerticalGlue());
    add(new JLabel("Cost:" + results.getCost()));

    invalidate();
  }

  public void setDiceRoll(DiceRoll diceRoll)
  {
    removeAll();
    for(int i = 1; i <= 6; i++)
    {

      int[] dice = diceRoll.getRolls(i);
      if(dice.length == 0)
        continue;

      add(create(diceRoll.getRolls(i), i));
    }
    add(Box.createVerticalGlue());
    add(new JLabel("Total hits:" + diceRoll.getHits()));

    invalidate();
  }

  private JComponent create(int[] dice, int rollAt)
  {
    JPanel dicePanel = new JPanel();
    dicePanel.setLayout(new BoxLayout(dicePanel, BoxLayout.X_AXIS));
    if(rollAt != -1)
      dicePanel.add(new JLabel("Rolled at " + rollAt + ":"));
    dicePanel.add(Box.createHorizontalStrut(5));
    for(int dieIndex = 0; dieIndex < dice.length; dieIndex++)
    {
      int roll = dice[dieIndex] + 1;
      dicePanel.add(new JLabel(DiceImageFactory.getInstance().getDieIcon(roll, roll <= rollAt)));
      dicePanel.add(Box.createHorizontalStrut(2));
    }
    JScrollPane scroll = new JScrollPane(dicePanel);
    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    //we're adding to a box layout, so to prevent the component from
    //grabbing extra space, set the max height.
    //allow room for a dice and a scrollbar
    scroll.setMinimumSize(new Dimension(scroll.getMinimumSize().width, DiceImageFactory.getInstance().DIE_HEIGHT + 17));
    scroll.setMaximumSize(new Dimension(scroll.getMaximumSize().width, DiceImageFactory.getInstance().DIE_HEIGHT + 17));
    scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, DiceImageFactory.getInstance().DIE_HEIGHT + 17));

    return scroll;
  }

}


class CasualtyNotificationPanel extends JPanel
{
  private DicePanel m_dice = new DicePanel();
  private JPanel m_units = new JPanel();
  private GameData m_data;

  public CasualtyNotificationPanel(GameData data)
  {
    m_data = data;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(m_dice);
    add(m_units);
    //add(Box.createVerticalGlue());
  }

  public void setNotication(CasualtyNotificationMessage msg)
  {
    m_dice.setDiceRoll(msg.getDice());

    m_units.removeAll();
    Iterator categoryIter = UnitSeperator.categorize(msg.getUnits(), msg.getDependents(), null).iterator();
    while(categoryIter.hasNext())
    {
      UnitCategory category = (UnitCategory) categoryIter.next();
      JPanel panel = new JPanel();
      JLabel unit = new JLabel(UnitIconImageFactory.instance().getIcon(category.getType(), category.getOwner(), m_data));
      panel.add(unit);
      Iterator iter = category.getDependents().iterator();
      while(iter.hasNext())
      {
        UnitOwner owner = (UnitOwner) iter.next();
        unit.add(new JLabel(UnitIconImageFactory.instance().getIcon(owner.getType(), owner.getOwnerr(), m_data)));
      }
      panel.add(new JLabel("x " + category.getUnits().size()));
      m_units.add(panel);
    }

    invalidate();
  }

}
