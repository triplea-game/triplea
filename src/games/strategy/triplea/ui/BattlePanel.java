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
* BattlePanel.java
*
* Created on December 4, 2001, 7:00 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.events.*;

import games.strategy.triplea.delegate.message.*;

/**
 *
 * UI for fighting battles.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BattlePanel extends ActionPanel
{

  private static Font BOLD;
  static
  {
    Map atts = new HashMap();
    atts.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
    BOLD = new Font(atts);
  }

  private JLabel m_actionLabel = new JLabel();
  private FightBattleMessage m_fightBattleMessage;
  private TripleAFrame m_parent;

  private BattleDisplay m_battleDisplay;
  private JFrame m_battleFrame;

  /** Creates new BattlePanel */
  public BattlePanel(GameData data, MapPanel map, TripleAFrame parent)
  {
    super(data, map);
    m_parent = parent;
  }

  public void display(PlayerID id, Collection battles, Collection bombing)
  {
    super.display(id);
    removeAll();
    m_actionLabel.setText(id.getName() + " battle");
    add(m_actionLabel);
    Iterator iter = battles.iterator();
    while(iter.hasNext() )
    {
      Action action = new FightBattleAction((Territory) iter.next(), false);
      add(new JButton(action));
    }

    iter = bombing.iterator();
    while(iter.hasNext() )
    {
      Action action = new FightBattleAction((Territory) iter.next(), true);
      add(new JButton(action));
    }
    SwingUtilities.invokeLater(REFRESH);
  }

  public Message battleInfo(BattleInfoMessage msg)
  {
    if(m_battleDisplay != null)
      m_battleDisplay.battleInfo(msg);

    return null;
  }

  public void battleEndMessage(BattleEndMessage message)
  {
    m_battleDisplay.endBattle(message);
    m_battleDisplay = null;
    m_battleFrame.setVisible(false);
    m_battleFrame.dispose();
    m_battleFrame = null;
  }


  public Message listBattle(BattleStepMessage msg)
  {
    removeAll();

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JTextArea text = new JTextArea();

    text.setFont(BOLD);
    text.setEditable(false);
    text.setBackground(this.getBackground());
    text.setText(msg.getTitle());
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    panel.add(text, BorderLayout.NORTH);


    getMap().centerOn(msg.getTerritory());
    m_battleDisplay.listBattle(msg);

    return null;
  }

  public Message battleStartMessage(BattleStartMessage msg)
  {
    if(!(m_battleDisplay == null))
    {
      throw new IllegalStateException("Battle display already showing");
    }

    m_battleDisplay = new BattleDisplay(getData(), msg.getTerritory(), msg.getAttacker(), msg.getDefender(), msg.getAttackingUnits(), msg.getDefendingUnits());

    m_battleFrame = new JFrame(msg.getAttacker().getName() + " attacks " + msg.getDefender().getName() + " in " + msg.getTerritory().getName());
    m_battleFrame.setIconImage(games.strategy.engine.framework.GameRunner.getGameIcon(m_battleFrame));
    m_battleFrame.getContentPane().add(m_battleDisplay);
    m_battleFrame.setSize(750, 500);
    m_battleFrame.show();
    m_battleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    return null;
  }

  public FightBattleMessage waitForBattleSelection()
  {
    try
    {
      synchronized(getLock())
      {
        getLock().wait();
      }
      } catch(InterruptedException ie)
      {
        waitForBattleSelection();
      }

      if(m_fightBattleMessage != null)
        getMap().centerOn(m_fightBattleMessage.getTerritory());

      return m_fightBattleMessage;
  }


  public void casualtyNoticicationMessage(CasualtyNotificationMessage message)
  {
    //if we are playing this player, then dont wait for the user
    //to see the units, since the player selected the units, and knows
    //what they are
    //if all the units have died then wait, since the player
    //hasnt been asked to select casualties
    //if no units died, then wait, since the user hasnt had a chance to
    //see the roll
    boolean waitFOrUserInput = !m_parent.playing(message.getPlayer()) || message.getAll() || message.getUnits().isEmpty();
    m_battleDisplay.casualtyNoticicationMessage( message, waitFOrUserInput);
  }

  public SelectCasualtyMessage getCasualties(SelectCasualtyQueryMessage msg)
  {
    //if the battle display is null, then this is a bombing raid
    if(m_battleDisplay == null)
      return getCasualtiesAA(msg);
    else
    {
      m_battleDisplay.setStep(msg);
      return m_battleDisplay.getCasualties(msg);
    }
  }

  private SelectCasualtyMessage getCasualtiesAA(SelectCasualtyQueryMessage msg)
  {
    boolean plural = msg.getCount() > 1;
    UnitChooser chooser = new UnitChooser(msg.getSelectFrom(), msg.getDependent(), getData());

    chooser.setTitle(msg.getMessage());
    chooser.setMax(msg.getCount());

    DicePanel dice = new DicePanel();
    dice.setDiceRoll(msg.getDice());

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(chooser, BorderLayout.CENTER);
    dice.setMaximumSize(new Dimension(450, 600));

    dice.setPreferredSize(new Dimension(300, (int) dice.getPreferredSize().getHeight()));
    panel.add(dice, BorderLayout.SOUTH);



    String[] options = {"OK"};
    int option = JOptionPane.showOptionDialog( getRootPane(), panel, msg.getPlayer().getName() + " select casualties", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
    Collection choosen = chooser.getSelected(false);
    SelectCasualtyMessage response = new SelectCasualtyMessage(choosen);
    return response;
  }

  public Message battleStringMessage(BattleStringMessage message)
  {
    m_battleDisplay.setStep(message);
    return null;
  }

  public RetreatMessage getRetreat(RetreatQueryMessage rqm)
  {
    m_battleDisplay.setStep(rqm);

    String message = rqm.getMessage();
    String ok = "Retreat";
    String cancel ="Remain";
    String[] options ={ok, cancel};
    int choice = JOptionPane.showOptionDialog(m_battleDisplay, message, "Retreat?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, cancel);
    boolean retreat = (choice == 0);
    if(!retreat)
      return null;

    RetreatComponent comp = new RetreatComponent(rqm);
    int option = JOptionPane.showConfirmDialog( m_battleDisplay, comp,rqm.getMessage(), JOptionPane.OK_CANCEL_OPTION);
    if(option == JOptionPane.OK_OPTION)
    {
      if(comp.getSelection() != null)
        return new RetreatMessage(comp.getSelection());
    }

    return null;
  }

  public void bombingResults(BombingResults message)
  {
    m_battleDisplay.bombingResults(message);
  }



  private class RetreatComponent extends JPanel
  {
    RetreatQueryMessage m_query;
    JList m_list;

    RetreatComponent(RetreatQueryMessage rqm)
    {
      this.setLayout(new BorderLayout());

      JLabel label = new JLabel("Retreat to...");
      this.add(label, BorderLayout.NORTH);

      m_query = rqm;
      Vector listElements = new Vector(rqm.getTerritories());

      m_list = new JList(listElements);
      m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      JScrollPane scroll = new JScrollPane(m_list);
      this.add(scroll, BorderLayout.CENTER);
    }

    public Territory getSelection()
    {
      return (Territory) m_list.getSelectedValue();
    }
  }

  class FightBattleAction extends AbstractAction
  {
    Territory m_territory;
    boolean m_bomb;

    FightBattleAction(Territory battleSite, boolean bomb)
    {
      super( (bomb ? "Bombing raid in " :  "Battle in ") + battleSite.getName() + "...");
      m_territory = battleSite;
      m_bomb = bomb;
    }

    public void actionPerformed(ActionEvent actionEvent)
    {

      if(!m_bomb)
      {
        Iterator iter = m_territory.getUnits().getPlayersWithUnits().iterator();
        PlayerID first = (PlayerID) iter.next();
        PlayerID second = (PlayerID) iter.next();
      }

      m_fightBattleMessage = new FightBattleMessage(m_territory, m_bomb);
      synchronized(getLock())
      {
        getLock().notify();
      }
    }
  }


  public String toString()
  {
    return "BattlePanel";
  }
}

