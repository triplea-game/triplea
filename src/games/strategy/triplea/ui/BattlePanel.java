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

import games.strategy.engine.data.*;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * 
 * UI for fighting battles.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class BattlePanel extends ActionPanel
{

//    static
//    {
//        Map atts = new HashMap();
//        atts.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
//        BOLD = new Font(atts);
//    }

    private JLabel m_actionLabel = new JLabel();
    private FightBattleDetails m_fightBattleMessage;
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
        while (iter.hasNext())
        {
            Action action = new FightBattleAction((Territory) iter.next(),
                    false);
            add(new JButton(action));
        }

        iter = bombing.iterator();
        while (iter.hasNext())
        {
            Action action = new FightBattleAction((Territory) iter.next(), true);
            add(new JButton(action));
        }
        SwingUtilities.invokeLater(REFRESH);
    }

    public void battleInfo(String messageShort, String messageLong, String step)
    {
        if (m_battleDisplay != null)
            m_battleDisplay.battleInfo(messageShort, messageLong, step);
    }

    public void battleInfo(String messageShort, DiceRoll dice, String step)
    {
        if (m_battleDisplay != null)
            m_battleDisplay.battleInfo(messageShort, dice, step);
    }

    
    public void battleEndMessage(GUID battleId, String message)
    {
        m_battleDisplay.endBattle(message);
        m_battleDisplay = null;
        m_battleFrame.setVisible(false);
        m_battleFrame.dispose();
        m_battleFrame = null;
    }

    private void ensureBattleIsDisplayed(GUID battleID)
    {
        while(m_battleDisplay == null || !m_battleDisplay.getBattleID().equals(battleID))
        {
            try
            {
                Thread.sleep(20);
            } catch (InterruptedException e)
            {
                return;
            }
        }
    }
    
    public void listBattle(final GUID battleID, final String currentStep,final  List steps)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            Runnable r = new Runnable()
            {
                public void run()
                {
                    listBattle(battleID, currentStep, steps);
                }
            };
            try
            {
                SwingUtilities.invokeAndWait(r);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            
        }

        removeAll();

        getMap().centerOn(m_battleDisplay.getBattleLocation());
        m_battleDisplay.listBattle(currentStep, steps);

        
    }

    public void showBattle(GUID battleID, Territory location, String battleTitle, Collection attackingUnits, Collection defendingUnits, Map unit_dependents, PlayerID attacker, PlayerID defender)
    {
        if (!(m_battleDisplay == null))
        {
            throw new IllegalStateException("Battle display already showing");
        }

        m_battleDisplay = new BattleDisplay(getData(), location, attacker, defender, attackingUnits, defendingUnits, battleID);

        m_battleFrame = new JFrame(attacker.getName() + " attacks "
                + defender.getName() + " in "
                + location.getName());
        m_battleFrame.setIconImage(games.strategy.engine.framework.GameRunner
                .getGameIcon(m_battleFrame));
        m_battleFrame.getContentPane().add(m_battleDisplay);
        m_battleFrame.setSize(750, 500);
        games.strategy.ui.Util.center(m_battleFrame);
        m_battleFrame.show();
        m_battleFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_battleFrame.toFront();
            }
        });

        
    }

    public FightBattleDetails waitForBattleSelection()
    {
        try
        {
            synchronized (getLock())
            {
                getLock().wait();
            }
        } catch (InterruptedException ie)
        {
            waitForBattleSelection();
        }

        if (m_fightBattleMessage != null)
            getMap().centerOn(m_fightBattleMessage.getWhere());

        return m_fightBattleMessage;
    }

    /**
     * Ask user which territory to bombard with a given unit.
     */
    public Territory getBombardment(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
    {
        BombardComponent comp = new BombardComponent(unit, unitTerritory, territories, noneAvailable);
    
        int option = JOptionPane.NO_OPTION;
        while(option != JOptionPane.OK_OPTION)
        {           
            option = JOptionPane.showConfirmDialog(this, comp,
                "Bombardment Territory Selection", JOptionPane.OK_OPTION);
        }
        return comp.getSelection();
    }

    public void casualtyNotification(String step, DiceRoll dice, PlayerID player, Collection killed, Collection damaged, Map dependents, boolean autoCalculated)
    {
        //if we are playing this player, then dont wait for the user
        //to see the units, since the player selected the units, and knows
        //what they are
        //if all the units to be removed have been calculated automatically
        // then wait so user can see units which have been removed.
        //if no units died, then wait, since the user hasnt had a chance to
        //see the roll
        boolean waitFOrUserInput = !m_parent.playing(player)
                || autoCalculated ||  (damaged.isEmpty() && killed.isEmpty());
        
        m_battleDisplay.casualtyNotification(step, dice, player, killed, damaged, dependents, autoCalculated, waitFOrUserInput);
    }

    public CasualtyDetails getCasualties(String step, Collection selectFrom, Map dependents, int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties)
    {
        //if the battle display is null, then this is a bombing raid
        if (m_battleDisplay == null)
            return getCasualtiesAA(step, selectFrom, dependents, count, message, dice,hit, defaultCasualties);
        else
        {
            m_battleDisplay.setStep(step);
            return m_battleDisplay.getCasualties(step, selectFrom, dependents, count, message, dice,hit, defaultCasualties);
        }
    }

    private CasualtyDetails getCasualtiesAA(String step, Collection selectFrom, Map dependents, int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties)
    {
        UnitChooser chooser = new UnitChooser(selectFrom, dependents, getData(), false);

        chooser.setTitle(message);
        chooser.setMax(count);

        DicePanel dicePanel = new DicePanel();
        dicePanel.setDiceRoll(dice);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chooser, BorderLayout.CENTER);
        dicePanel.setMaximumSize(new Dimension(450, 600));

        dicePanel.setPreferredSize(new Dimension(300, (int) dicePanel.getPreferredSize()
                .getHeight()));
        panel.add(dicePanel, BorderLayout.SOUTH);

        String[] options = { "OK" };
        JOptionPane.showOptionDialog(getRootPane(), panel, hit.getName()
                + " select casualties", JOptionPane.OK_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, null);
        List killed = chooser.getSelected(false);
        CasualtyDetails response = new CasualtyDetails(killed,
                chooser.getSelectedFirstHit(), false);
        return response;
    }

    public Territory getRetreat(GUID battleID, String step, String message, Collection possible, boolean submerge)
    {
        ensureBattleIsDisplayed(battleID);
        return m_battleDisplay.getRetreat(step, message, possible, submerge);
    }

    public void notifyRetreat(Collection retreating)
    {
        m_battleDisplay.notifyRetreat(retreating);
    }

    public void bombingResults(GUID battleID,  int[] dice,  int cost)
    {
        m_battleDisplay.bombingResults(dice, cost);
    }

    class FightBattleAction extends AbstractAction
    {
        Territory m_territory;
        boolean m_bomb;

        FightBattleAction(Territory battleSite, boolean bomb)
        {
            super((bomb ? "Bombing raid in " : "Battle in ")
                    + battleSite.getName() + "...");
            m_territory = battleSite;
            m_bomb = bomb;
        }

        public void actionPerformed(ActionEvent actionEvent)
        {
            m_fightBattleMessage = new FightBattleDetails(m_bomb, m_territory);
            synchronized (getLock())
            {
                getLock().notify();
            }
        }
    }

    public String toString()
    {
        return "BattlePanel";
    }

    private class BombardComponent extends JPanel
    {

        private JList m_list;

        BombardComponent(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
        {

            this.setLayout(new BorderLayout());

            String unitName = unit.getUnitType().getName() + " in "
                    + unitTerritory;
            JLabel label = new JLabel("Which territory should " + unitName
                    + " bombard?");
            this.add(label, BorderLayout.NORTH);

            Vector listElements = new Vector(territories);
            if (noneAvailable)
            {
                listElements.add(0, "None");
            }

            m_list = new JList(listElements);
            m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (listElements.size() >= 1)
                m_list.setSelectedIndex(0);
            JScrollPane scroll = new JScrollPane(m_list);
            this.add(scroll, BorderLayout.CENTER);
        }

        public Territory getSelection()
        {
            Object selected = m_list.getSelectedValue();
            if (selected instanceof Territory)
            {
                return (Territory) selected;
            }

            return null; // User selected "None" option
        }
    }
}

