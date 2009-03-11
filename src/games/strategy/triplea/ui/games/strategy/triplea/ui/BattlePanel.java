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

import games.strategy.debug.Console;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameRunner;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 
 * UI for fighting battles.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class BattlePanel extends ActionPanel
{

    private JLabel m_actionLabel = new JLabel();
    private FightBattleDetails m_fightBattleMessage;

    private volatile BattleDisplay m_battleDisplay;
    //if we are showing a battle, then this will be set to the currently
    //displayed battle.  This will only be set after the display 
    //is shown on the screen
    private volatile GUID m_currentBattleDisplayed;
    
    //there is a bug in linux jdk1.5.0_6 where frames are not
    //being garbage collected
    //reuse one frame
    private final JFrame m_battleFrame;

    /** Creates new BattlePanel */
    public BattlePanel(GameData data, MapPanel map)
    {
        super(data, map);
        m_battleFrame = new JFrame();
        m_battleFrame.setIconImage(GameRunner.getGameIcon(m_battleFrame ));
        getMap().getUIContext().addShutdownWindow(m_battleFrame);
        
        m_battleFrame.addWindowListener(new WindowListener()
        {
        
            public void windowActivated(WindowEvent e)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        if(m_battleDisplay != null)
                            m_battleDisplay.takeFocus();
                    }
                
                });
                
                
            }

            public void windowClosed(WindowEvent e)
            {}

            public void windowClosing(WindowEvent e)
            {}

            public void windowDeactivated(WindowEvent e)
            {}

            public void windowDeiconified(WindowEvent e)
            {}

            public void windowIconified(WindowEvent e)
            {}

            public void windowOpened(WindowEvent e)
            {}
        
        });

    }

    public void display(final PlayerID id,final Collection<Territory> battles, final Collection<Territory> bombing)
    {
        super.display(id);
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                removeAll();
                m_actionLabel.setText(id.getName() + " battle");
                
                setLayout(new BorderLayout());
                JPanel panel = new JPanel();
                panel.setLayout(new GridLayout(0, 1));
                panel.add(m_actionLabel);
                Iterator<Territory> iter = battles.iterator();
                while (iter.hasNext())
                {
                    addBattleActions(panel, iter, false);
                }

                iter = bombing.iterator();
                while (iter.hasNext())
                {
                    addBattleActions(panel, iter, true);
                }
                add(panel, BorderLayout.NORTH);
                SwingUtilities.invokeLater(REFRESH);

        
            }

            private void addBattleActions(JPanel panel, Iterator<Territory> iter, boolean bomb)
            {
                Territory next = iter.next();
                JPanel innerPanel = new JPanel();
                innerPanel.setLayout(new BorderLayout());
                innerPanel.add(new JButton(new FightBattleAction(next, bomb)), BorderLayout.CENTER);
                innerPanel.add(new JButton(new CenterBattleAction(next)), BorderLayout.EAST);
                panel.add(innerPanel);
            }
        
        });
    }

    public void notifyRetreat(final String messageShort, final String messageLong, final String step, final PlayerID retreatingPlayer)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (m_battleDisplay != null)
                    m_battleDisplay.battleInfo(messageShort, messageLong, step);
            }
        }

        );

    }

    public void showDice(final String messageShort, final DiceRoll dice, final String step)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (m_battleDisplay != null)
                    m_battleDisplay.battleInfo(messageShort, dice, step);
            }

        });
    }

    public void battleEndMessage(final GUID battleId, final String message)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if(m_battleDisplay != null)
                    m_battleDisplay.endBattle(message, m_battleFrame);
            }
        });

    }

    /**
     *  
     */
    private void cleanUpBattleWindow()
    {
        if (m_battleDisplay != null)
        {
            m_currentBattleDisplayed = null;
            m_battleDisplay.cleanUp();
            m_battleFrame.getContentPane().removeAll();
            m_battleDisplay = null;
            games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(m_battleFrame);

        }
    }

    private boolean ensureBattleIsDisplayed(GUID battleID) 
    {
        if(SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong threads");
        
        GUID displayed = m_currentBattleDisplayed;
        int count = 0;
        while (displayed == null || !battleID.equals(displayed))
        {
            try
            {
                count++;
                Thread.sleep(count);
            } catch (InterruptedException e)
            {
                return false;
            }
            
            //something is wrong, we shouldnt have to wait this long
            if(count > 200)
            {
                Console.getConsole().dumpStacks();
                new IllegalStateException("battle not displayed, looking for:" +  battleID + " showing:" + m_currentBattleDisplayed).printStackTrace();
                return false;
            }
            displayed = m_currentBattleDisplayed;
        }
        
        return true;
    }

    protected JFrame getBattleFrame()
    {
        return m_battleFrame;
    }
    
    public void listBattle(final GUID battleID, final List steps)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            Runnable r = new Runnable()
            {
                public void run()
                {
                    //recursive call
                    listBattle(battleID, steps);
                }
            };
            try
            {
                SwingUtilities.invokeLater(r);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            return;
        }

        removeAll();

        
        if(m_battleDisplay != null)
        {
            getMap().centerOn(m_battleDisplay.getBattleLocation());
            m_battleDisplay.listBattle(steps);
        }
    }

    public void showBattle(final GUID battleID, final Territory location, final String battleTitle, final Collection<Unit> attackingUnits,
            final Collection<Unit> defendingUnits, final Map<Unit, Collection<Unit>> unit_dependents, final PlayerID attacker, final PlayerID defender)
    {
        try
        {
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {

                    if (m_battleDisplay != null)
                    {
                        cleanUpBattleWindow();
                        m_currentBattleDisplayed = null;
                    }

		    if (! getMap().getUIContext().getShowMapOnly())
			{
			    
			    m_battleDisplay = new BattleDisplay(getData(), location, attacker, defender, attackingUnits, defendingUnits, battleID, BattlePanel.this.getMap());
			    
			    m_battleFrame.setTitle(attacker.getName() + " attacks " + defender.getName() + " in " + location.getName());
			    
			    
			    m_battleFrame.getContentPane().add(m_battleDisplay);
			    m_battleFrame.setSize(750, 540);
			    m_battleFrame.setLocationRelativeTo(JOptionPane.getFrameForComponent(BattlePanel.this));
			    
			    
			    games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(m_battleFrame);		    
			    m_battleFrame.setVisible(true);
			    m_battleFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			    
			    m_currentBattleDisplayed = battleID;
			    
			    SwingUtilities.invokeLater(new Runnable()
				{
				    public void run()
				    {
					m_battleFrame.toFront();
				    }
				    
				});
			}
                }
            });
        } catch (InterruptedException e)
        {
            
            e.printStackTrace();
        } catch (InvocationTargetException e)
        {
         
            e.printStackTrace();
        }

    }

    public FightBattleDetails waitForBattleSelection()
    {
        waitForRelease();

        if (m_fightBattleMessage != null)
            getMap().centerOn(m_fightBattleMessage.getWhere());

        return m_fightBattleMessage;
    }

    /**
     * Ask user which territory to bombard with a given unit.
     */
    public Territory getBombardment(Unit unit, Territory unitTerritory, Collection<Territory>  territories, boolean noneAvailable)
    {
        BombardComponent comp = new BombardComponent(unit, unitTerritory, territories, noneAvailable);

        int option = JOptionPane.NO_OPTION;
        while (option != JOptionPane.OK_OPTION)
        {
            option = JOptionPane.showConfirmDialog(this, comp, "Bombardment Territory Selection", JOptionPane.OK_OPTION);
        }
        return comp.getSelection();
    }

    public boolean getAttackSubs(final Territory terr) 
    {       
        return JOptionPane.showConfirmDialog(null, "Attack submarines in " + terr.toString() + "?", "Attack", JOptionPane.YES_NO_OPTION) == 0;        
    }
    
    public void casualtyNotification(final String step, final DiceRoll dice, final PlayerID player, final Collection<Unit> killed,
            final Collection<Unit> damaged, final Map<Unit, Collection<Unit>> dependents)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if(m_battleDisplay != null)
                    m_battleDisplay.casualtyNotification(step, dice, player, killed, damaged, dependents);
            }
        });
    }

    public void confirmCasualties(final GUID battleId, final String message)
    {
        //something is wrong
        if(!ensureBattleIsDisplayed(battleId)) {
            return;
        }
        m_battleDisplay.waitForConfirmation(message);
    }

    public CasualtyDetails getCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message,
            final DiceRoll dice, final PlayerID hit, final List<Unit> defaultCasualties, GUID battleID)
    {
        //if the battle display is null, then this is an aa fire during move
        if (battleID == null)
            return getCasualtiesAA(selectFrom, dependents, count, message, dice, hit, defaultCasualties);
        else
        {
            //something is wong
            if(!ensureBattleIsDisplayed(battleID))
                return new CasualtyDetails(defaultCasualties, Collections.<Unit>emptyList(), true);
            
            return m_battleDisplay.getCasualties(selectFrom, dependents, count, message, dice, hit, defaultCasualties);
        }

    }

    private CasualtyDetails getCasualtiesAA( Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice,
            PlayerID hit, List defaultCasualties)
    {
        boolean isEditMode = (dice == null);
        UnitChooser chooser = new UnitChooser(selectFrom, dependents, getData(), false, getMap().getUIContext());

        chooser.setTitle(message);
        if (isEditMode)
            chooser.setMax(selectFrom.size());
        else
            chooser.setMax(count);

        DicePanel dicePanel = new DicePanel(getMap().getUIContext());
        if (!isEditMode)
            dicePanel.setDiceRoll(dice);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chooser, BorderLayout.CENTER);
        dicePanel.setMaximumSize(new Dimension(450, 600));

        dicePanel.setPreferredSize(new Dimension(300, (int) dicePanel.getPreferredSize().getHeight()));
        panel.add(dicePanel, BorderLayout.SOUTH);

        String[] options =
        { "OK" };
        JOptionPane.showOptionDialog(getRootPane(), panel, hit.getName() + " select casualties", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, null);
        List<Unit> killed = chooser.getSelected(false);
        CasualtyDetails response = new CasualtyDetails(killed, chooser.getSelectedFirstHit(), false);
        return response;
    }

    public Territory getRetreat(GUID battleID, String message, Collection<Territory> possible, boolean submerge)
    {
        //something is really wrong
        if(!ensureBattleIsDisplayed(battleID))
            return null;
        return m_battleDisplay.getRetreat(message, possible, submerge);
    }

    public void gotoStep(GUID battleID, final String step)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (m_battleDisplay != null)
                    m_battleDisplay.setStep(step);
            }
        });
    }

    public void notifyRetreat(final Collection retreating)
    {

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if(m_battleDisplay != null)
                    m_battleDisplay.notifyRetreat(retreating);
            }
        });
    }

    public void bombingResults(final GUID battleID, final int[] dice, final int cost)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if(m_battleDisplay != null)
                    m_battleDisplay.bombingResults(dice, cost);
            }
        });

    }

    Territory m_oldCenteredTerritory = null;
    Timer m_CenterBattleActionTimer = null;

    class CenterBattleAction extends AbstractAction
    {
        Territory m_territory;

        CenterBattleAction(Territory battleSite)
    	{
			super("Center");
    		m_territory = battleSite;
    	}
        
		public void actionPerformed(ActionEvent e) 
		{
            if (m_CenterBattleActionTimer != null)
                m_CenterBattleActionTimer.cancel();
            
			if (m_oldCenteredTerritory != null)
				getMap().clearTerritoryOverlay(m_oldCenteredTerritory);
            
            getMap().centerOn(m_territory);

            m_CenterBattleActionTimer = new Timer();
            m_CenterBattleActionTimer.scheduleAtFixedRate(new MyTimerTask(m_territory, m_CenterBattleActionTimer), 150, 150);
			m_oldCenteredTerritory = m_territory;
		}
    	
        class MyTimerTask extends TimerTask
        {
            Territory m_territory;
            Timer m_stopTimer;
            int m_count = 0;
            
            MyTimerTask(Territory battleSite, Timer stopTimer)
            {
                m_territory = battleSite;     
                m_stopTimer = stopTimer;
            }
            
            @Override
            public void run()
            {
                if (m_count == 5)
                    m_stopTimer.cancel();
                if ((m_count % 3) == 0)
                {
                    getMap().setTerritoryOverlayForBorder(m_territory, Color.white);
                    getMap().paintImmediately(getMap().getBounds());
                    //TODO: getUIContext().getMapData().getBoundingRect(m_territory)); what kind of additional transformation needed here?
                    //TODO: setTerritoryOverlayForBorder is causing invalid ordered lock acquire atempt, why?
                }
                else
                {
                    getMap().clearTerritoryOverlay(m_territory);
                    getMap().paintImmediately(getMap().getBounds());
                    //TODO: getUIContext().getMapData().getBoundingRect(m_territory)); what kind of additional transformation needed here?
                    //TODO: setTerritoryOverlayForBorder is causing invalid ordered lock acquire atempt, why?
                }
                m_count ++;
            }
            
        }
    }
    class FightBattleAction extends AbstractAction
    {
        Territory m_territory;
        boolean m_bomb;

        FightBattleAction(Territory battleSite, boolean bomb)
        {
            super((bomb ? "Bombing raid in " : "Battle in ") + battleSite.getName() + "...");
            m_territory = battleSite;
            m_bomb = bomb;
        }

        public void actionPerformed(ActionEvent actionEvent)
        {
			if (m_oldCenteredTerritory != null)
				getMap().clearTerritoryOverlay(m_oldCenteredTerritory);
            m_fightBattleMessage = new FightBattleDetails(m_bomb, m_territory);
            release();
        }
    }

    public String toString()
    {
        return "BattlePanel";
    }

    private class BombardComponent extends JPanel
    {

        private JList m_list;

        BombardComponent(Unit unit, Territory unitTerritory, Collection<Territory> territories, boolean noneAvailable)
        {

            this.setLayout(new BorderLayout());

            String unitName = unit.getUnitType().getName() + " in " + unitTerritory;
            JLabel label = new JLabel("Which territory should " + unitName + " bombard?");
            this.add(label, BorderLayout.NORTH);

            Vector<Object>  listElements = new Vector<Object> (territories);
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

