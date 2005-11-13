/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.ui;

import java.util.concurrent.CountDownLatch;
import games.strategy.engine.data.*;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.sound.SoundPath;
import games.strategy.triplea.util.*;
import games.strategy.ui.Util;
import games.strategy.util.Match;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.*;

/**
 * Displays a running battle
 */

public class BattleDisplay extends JPanel
{
    private static final String DICE_KEY = "D";
    private static final String CASUALTIES_KEY = "C";
    private static final String MESSAGE_KEY = "M";

    private final GUID m_battleID;

    private final PlayerID m_defender;
    private final PlayerID m_attacker;
    private final Territory m_location;
    private final GameData m_data;

    private final JButton m_actionButton = new JButton("");

    private BattleModel m_defenderModel;
    private BattleModel m_attackerModel;
    private BattleStepsPanel m_steps;

    private CasualtyDetails m_selectCasualtyResponse;

    private DicePanel m_dicePanel;
    private CasualtyNotificationPanel m_casualties;
    private JPanel m_actionPanel;
    private CardLayout m_actionLayout = new CardLayout();
    private JPanel m_messagePanel = new JPanel();
    private final MapPanel m_mapPanel;

    private JLabel m_messageLabel = new JLabel();

    public BattleDisplay(GameData data, Territory territory, PlayerID attacker, PlayerID defender, Collection<Unit> attackingUnits,
            Collection<Unit> defendingUnits, GUID battleID, MapPanel mapPanel)
    {
        m_battleID = battleID;
        m_defender = defender;
        m_attacker = attacker;
        m_location = territory;
        m_mapPanel = mapPanel;
        m_data = data;
        m_casualties = new CasualtyNotificationPanel(data, m_mapPanel.getUIContext());

        m_defenderModel = new BattleModel(m_data, defendingUnits, false, m_mapPanel.getUIContext());
        m_defenderModel.refresh();
        m_attackerModel = new BattleModel(m_data, attackingUnits, true, m_mapPanel.getUIContext());
        m_attackerModel.refresh();

        initLayout();
    }

    public void cleanUp()
    {
        m_steps.deactivate();
    }
    
    public Territory getBattleLocation()
    {
        return m_location;
    }

    public GUID getBattleID()
    {
        return m_battleID;
    }

    public void bombingResults(int[] dice, int cost)
    {

        ClipPlayer.getInstance().playClip(SoundPath.BOMB, SoundPath.class); //play
        // sound
        m_dicePanel.setDiceRollForBombing(dice, cost);
        m_actionLayout.show(m_actionPanel, DICE_KEY);
    }

    public static boolean getShowEnemyCasualtyNotification()
    {
        Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
        return prefs.getBoolean(Constants.SHOW_ENEMY_CASUALTIES_USER_PREF, true);
    }

    public static void setShowEnemyCasualtyNotification(boolean aVal)
    {
        Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
        prefs.putBoolean(Constants.SHOW_ENEMY_CASUALTIES_USER_PREF, aVal);

    }

    public void casualtyNotification(String step, DiceRoll dice, PlayerID player, Collection<Unit> killed, Collection<Unit> damaged, Map<Unit, Collection<Unit>> dependents)
    {
        setStep(step);
        m_casualties.setNotication(dice, player, killed, damaged, dependents);
        m_actionLayout.show(m_actionPanel, CASUALTIES_KEY);

        if (player.equals(m_defender))
        {
            m_defenderModel.removeCasualties(killed);
        } else
        {
            m_attackerModel.removeCasualties(killed);
        }
    }

    public void waitForConfirmation(final String message)
    {
        if (SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("This cant be in dispatch thread");

        if (!getShowEnemyCasualtyNotification())
            return;

        final CountDownLatch continueLatch = new CountDownLatch(1);

        //set the action in the swing thread.

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_actionButton.setAction(new AbstractAction(message)
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        continueLatch.countDown();
                    }
                });
            }

        });

        //wait for the button to be pressed.
        try
        {
            continueLatch.await();
        } catch (InterruptedException ie)
        {

        }

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_actionButton.setAction(null);
            }
        });

    }

    public void endBattle(String message, final Frame enclosingFrame)
    {
        m_steps.walkToLastStep();
        final Action close = new AbstractAction(message + " : (Click to close)")
        {
            public void actionPerformed(ActionEvent e)
            {
                enclosingFrame.setVisible(false);
                enclosingFrame.dispose();
            }
        };

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_actionButton.setAction(close);
            }
        });

    }

    public void notifyRetreat(Collection retreating)
    {
        m_defenderModel.notifyRetreat(retreating);
        m_attackerModel.notifyRetreat(retreating);
    }

    public Territory getRetreat(String message, Collection<Territory> possible, boolean submerge)
    {
        if (!submerge)
        {
            return getRetreatInternal(message, possible);
        } else
        {
            return getSubmerge(message);
        }
    }

    private Territory getSubmerge(final String message)
    {
 
        if(SwingUtilities.isEventDispatchThread())
        {
            throw new IllegalStateException("Should not be called from dispatch thread");
        }
        
        final Territory[] retreatTo = new Territory[1];
        final CountDownLatch latch = new CountDownLatch(1);
        
        final Action action = new AbstractAction("Submerge Subs?")
        {
            public void actionPerformed(ActionEvent e)
            {
                String ok = "Submerge";
                String cancel = "Remain";

                String wait = "Ask Me Later";
                
                String[] options =
                { ok, cancel, wait };
                int choice = JOptionPane.showOptionDialog(BattleDisplay.this, message, "Submerge Subs?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
                        cancel);
                
                //dialog dismissed
                if(choice == -1)
                    return;
                //wait
                if(choice == 2)
                    return;
                
                //remain
                if (choice == 1)
                {
                    latch.countDown();
                    return;
                }
                
                //submerge
                
                retreatTo[0] =  m_location;
                latch.countDown();
                 
            }
            
        };
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_actionButton.setAction(action);
            }
        });
        
        SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        action.actionPerformed(null);
                    }
                });

        
        try
        {
            latch.await();
        } catch (InterruptedException e1)
        {
            e1.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_actionButton.setAction(null);
        
            }
        
        });
        
        
        return retreatTo[0];        
        

    }

    private Territory getRetreatInternal(final String message, final  Collection<Territory> possible)
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            throw new IllegalStateException("Should not be called from dispatch thread");
        }
        
        final Territory[] retreatTo = new Territory[1];
        final CountDownLatch latch = new CountDownLatch(1);
        
        final Action action = new AbstractAction("Retreat?")
        {
            public void actionPerformed(ActionEvent e)
            {
                String ok = "Retreat";
                String cancel = "Remain";
                String wait = "Ask Me Later";
                
                String[] options =
                { ok, cancel, wait };
                int choice = JOptionPane.showOptionDialog(BattleDisplay.this, message, "Retreat?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
                        cancel);
                
                //dialog dismissed
                if(choice == -1)
                    return;
                //wait
                if(choice == 2)
                    return;
                
                //remain
                if (choice == 1)
                {
                    latch.countDown();
                    return;
                }
                
                //if you have eliminated the impossible, whatever remains, no matter
                //how improbable, must be the truth
                //retreat

                RetreatComponent comp = new RetreatComponent(possible);
                int option = JOptionPane.showConfirmDialog(BattleDisplay.this, comp, message, JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION)
                {
                    if (comp.getSelection() != null)
                    {
                        retreatTo[0] =  comp.getSelection();
                        latch.countDown();
                    }
                        
                } 
            }
            
        };
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_actionButton.setAction(action);
            }
        });
        
        SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        action.actionPerformed(null);
                    }
                });

        
        try
        {
            latch.await();
        } catch (InterruptedException e1)
        {
            e1.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_actionButton.setAction(null);
        
            }
        
        });
        
        
        return retreatTo[0];
    }

    private class RetreatComponent extends JPanel
    {
        private JList m_list;

        RetreatComponent(Collection<Territory> possible)
        {

            this.setLayout(new BorderLayout());

            JLabel label = new JLabel("Retreat to...");
            this.add(label, BorderLayout.NORTH);

            Vector<Territory> listElements = new Vector<Territory>(possible);

            m_list = new JList(listElements);
            m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (listElements.size() >= 1)
                m_list.setSelectedIndex(0);
            JScrollPane scroll = new JScrollPane(m_list);
            this.add(scroll, BorderLayout.CENTER);
        }

        public Territory getSelection()
        {

            return (Territory) m_list.getSelectedValue();
        }
    }

    public CasualtyDetails getCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message,
            final DiceRoll dice, final PlayerID hit, final List<Unit> defaultCasualties)
    {
        if (SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("This method should not be run in teh event dispatch thread");

        final CountDownLatch continueLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(new Runnable()
        {

            
            public void run()
            {

                m_actionLayout.show(m_actionPanel, DICE_KEY);
                m_dicePanel.setDiceRoll(dice);

                boolean plural = count > 1;
                final String btnText = hit.getName() + ", click to select " + count + (plural ? " casualties" : " casualty");
                m_actionButton.setAction(new AbstractAction(btnText)
                {

                    public void actionPerformed(ActionEvent e)
                    {

                        String messageText = message + " " + btnText + ".";
                        UnitChooser chooser = new UnitChooser(selectFrom, defaultCasualties, dependents, m_data, true, m_mapPanel.getUIContext());

                        chooser.setTitle(messageText);
                        chooser.setMax(count);
                        String[] options =
                        { "Ok", "Cancel" };
                        int option = JOptionPane.showOptionDialog(BattleDisplay.this, chooser, hit.getName() + " select casualties",
                                JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
                        if (option != 0)
                            return;
                        List<Unit> killed = chooser.getSelected(false);
                        List<Unit> damaged = chooser.getSelectedFirstHit();

                        if (killed.size() + damaged.size() != count)
                        {
                            JOptionPane.showMessageDialog(BattleDisplay.this, "Wrong number of casualties choosen", hit.getName()
                                    + " select casualties", JOptionPane.ERROR_MESSAGE);
                        } else
                        {
                            CasualtyDetails response = new CasualtyDetails(killed, damaged, false);
                            m_selectCasualtyResponse = response;
                            continueLatch.countDown();
                        }
                    }
                });
            }
        });

        try
        {
            continueLatch.await();
        } catch (InterruptedException ex)
        {
        }

        m_dicePanel.clear();
        m_actionButton.setEnabled(false);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_actionButton.setAction(null);
            }
        });

        CasualtyDetails rVal = m_selectCasualtyResponse;
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

        m_messagePanel.setLayout(new BorderLayout());
        m_messagePanel.add(m_messageLabel, BorderLayout.CENTER);

        m_steps = new BattleStepsPanel();
        m_steps.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        m_dicePanel = new DicePanel(m_mapPanel.getUIContext());

        m_actionPanel = new JPanel();
        m_actionPanel.setLayout(m_actionLayout);

        m_actionPanel.add(m_dicePanel, DICE_KEY);
        m_actionPanel.add(m_casualties, CASUALTIES_KEY);
        m_actionPanel.add(m_messagePanel, MESSAGE_KEY);

        JPanel diceAndSteps = new JPanel();
        diceAndSteps.setLayout(new BorderLayout());
        diceAndSteps.add(m_steps, BorderLayout.WEST);
        diceAndSteps.add(m_actionPanel, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(diceAndSteps, BorderLayout.CENTER);

        add(m_actionButton, BorderLayout.SOUTH);
        m_actionButton.setBackground(Color.lightGray.darker());
        m_actionButton.setEnabled(false);
        m_actionButton.setForeground(Color.white);

        setDefaultWidhts(defenderTable);
        setDefaultWidhts(attackerTable);

    }

    /**
     * Shorten columsn with no units.
     */
    private void setDefaultWidhts(JTable table)
    {

        for (int column = 0; column < table.getColumnCount(); column++)
        {
            boolean hasData = false;
            for (int row = 0; row < table.getRowCount(); row++)
            {
                hasData |= (table.getValueAt(row, column) != TableData.NULL);
            }
            if (!hasData)
            {
                table.getColumnModel().getColumn(column).setPreferredWidth(8);
            }
        }

    }

    public void setStep(String step)
    {
        m_steps.setStep(step);
    }

    public void battleInfo(String messageShort, DiceRoll message, String step)
    {
        setStep(step);
        m_dicePanel.setDiceRoll(message);
        m_actionLayout.show(m_actionPanel, DICE_KEY);
    }

    public void battleInfo(String messageShort, String message, String step)
    {
        m_messageLabel.setText(message);
        setStep(step);
        m_actionLayout.show(m_actionPanel, MESSAGE_KEY);

    }

    public void listBattle(List steps)
    {

        m_steps.listBattle(steps);

    }

    private JComponent getPlayerComponent(PlayerID id)
    {

        JLabel player = new JLabel(id.getName());
        player.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
        player.setFont(player.getFont().deriveFont((float) 14));
        return player;
    }

    private static final int MY_WIDTH = 100;
    private static final int MY_HEIGHT = 100;

    private JComponent getTerritoryComponent()
    {

        Image finalImage = Util.createImage(MY_WIDTH, MY_HEIGHT, true);

        Image territory = m_mapPanel.getTerritoryImage(m_location);

        finalImage.getGraphics().drawImage(territory, 0, 0, MY_WIDTH, MY_HEIGHT, this);

        return new JLabel(new ImageIcon(finalImage));

    }
}

class BattleTable extends JTable
{

    BattleTable(BattleModel model)
    {

        super(model);
        setDefaultRenderer(Object.class, new Renderer());
        setRowHeight(UnitImageFactory.UNIT_ICON_HEIGHT + 5);
        setBackground(new JButton().getBackground());
        setShowHorizontalLines(false);

        getTableHeader().setReorderingAllowed(false);
        //    getTableHeader().setResizingAllowed(false);
    }
}

class BattleModel extends DefaultTableModel
{
    private UIContext m_uiContext;
    private GameData m_data;
    //is the player the agressor?
    private boolean m_attack;
    private Collection<Unit> m_units;

    BattleModel(GameData data, Collection<Unit> units, boolean attack, UIContext uiContext)
    {

        super(new Object[0][0], new String[]
        { " ", "1", "2", "3", "4", "5" });
        m_uiContext = uiContext;
        m_data = data;
        m_attack = attack;
        //were going to modify the units
        m_units = new ArrayList<Unit>(units);
    }

    public void notifyRetreat(Collection retreating)
    {
        m_units.removeAll(retreating);
        refresh();
    }

    public void removeCasualties(Collection<Unit> killed)
    {
        m_units.removeAll(killed);
        refresh();
    }

    /**
     * refresh the model from m_units
     */
    @SuppressWarnings("unchecked")
    public void refresh()
    {

        List[] columns = new List[6];
        for (int i = 0; i < columns.length; i++)
        {
            columns[i] = new ArrayList();
        }

        //how many artillery units do we have
        int artillerySupportAvailable = Match.countMatches(m_units, Matches.UnitIsArtillery);

        Collection unitCategories = UnitSeperator.categorize(m_units);

        Iterator categoriesIter = unitCategories.iterator();

        while (categoriesIter.hasNext())
        {
            UnitCategory category = (UnitCategory) categoriesIter.next();

            int strength;

            UnitAttachment attatchment = UnitAttachment.get(category.getType());

            if (m_attack)
                strength = attatchment.getAttack(category.getOwner());
            else
                strength = attatchment.getDefense(category.getOwner());

            int unitsToAdd = category.getUnits().size();
            int supportedUnitsToAdd = 0;
            //factor in artillery support
            if (attatchment.isArtillerySupportable() && m_attack)
            {
                supportedUnitsToAdd = Math.min(artillerySupportAvailable, unitsToAdd);
                artillerySupportAvailable -= supportedUnitsToAdd;
                unitsToAdd -= supportedUnitsToAdd;
            }
            if (unitsToAdd > 0)
                columns[strength].add(new TableData(category.getOwner(), unitsToAdd, category.getType(), m_data, category.getDamaged(), m_uiContext));
            if (supportedUnitsToAdd > 0)
                columns[strength + 1].add(new TableData(category.getOwner(), supportedUnitsToAdd, category.getType(), m_data, category.getDamaged(), m_uiContext));
        }

        //find the number of rows
        //this will be the size of the largest column
        int rowCount = 1;
        for (int i = 0; i < columns.length; i++)
        {
            rowCount = Math.max(rowCount, columns[i].size());
        }

        setNumRows(rowCount);

        for (int row = 0; row < rowCount; row++)
        {
            for (int column = 0; column < columns.length; column++)
            {
                //if the column has that many items, add to the table, else add
                // null
                if (columns[column].size() > row)
                {
                    setValueAt(columns[column].get(row), row, column);
                } else
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

    JLabel m_stamp = new JLabel();

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {

        ((TableData) value).updateStamp(m_stamp);
        return m_stamp;
    }
}

class TableData
{

    static TableData NULL = new TableData();
    private int m_count;
    private Icon m_icon;

    private TableData()
    {
        
    }

    TableData(PlayerID player, int count, UnitType type, GameData data, boolean damaged, UIContext uiContext)
    {
        m_count = count;
        m_icon = uiContext.getUnitImageFactory().getIcon(type, player, data, damaged);
    }

    public void updateStamp(JLabel stamp)
    {

        if (m_count == 0)
        {
            stamp.setText("");
            stamp.setIcon(null);
        } else
        {
            stamp.setText("x" + m_count);
            stamp.setIcon(m_icon);
        }
    }
}



class CasualtyNotificationPanel extends JPanel
{

    private DicePanel m_dice;
    private JPanel m_killed = new JPanel();
    private JPanel m_damaged = new JPanel();
    private GameData m_data;
    private UIContext m_uiContext;

    public CasualtyNotificationPanel(GameData data, UIContext uiContext)
    {

        m_data = data;
        m_uiContext = uiContext;
        m_dice = new DicePanel(uiContext);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(m_dice);
        add(m_killed);
        add(m_damaged);
    }

    public void setNotication(DiceRoll dice, PlayerID player, Collection<Unit> killed, Collection<Unit> damaged, Map<Unit, Collection<Unit>> dependents)
    {

        m_dice.setDiceRoll(dice);

        m_killed.removeAll();
        m_damaged.removeAll();

        if (!killed.isEmpty())
        {
            m_killed.add(new JLabel("Killed"));
        }

        Iterator killedIter = UnitSeperator.categorize(killed, dependents, null).iterator();
        categorizeUnits(killedIter, false);

        damaged.removeAll(killed);
        if (!damaged.isEmpty())
        {
            m_damaged.add(new JLabel("Damaged"));
        }
        Iterator damagedIter = UnitSeperator.categorize(damaged, dependents, null).iterator();
        categorizeUnits(damagedIter, true);

        invalidate();
        validate();
    }

    private void categorizeUnits(Iterator categoryIter, boolean damaged)
    {

        while (categoryIter.hasNext())
        {
            UnitCategory category = (UnitCategory) categoryIter.next();
            JPanel panel = new JPanel();
            JLabel unit = new JLabel(m_uiContext.getUnitImageFactory().getIcon(category.getType(), category.getOwner(), m_data, category.getDamaged()));
            panel.add(unit);
            Iterator iter = category.getDependents().iterator();
            while (iter.hasNext())
            {
                UnitOwner owner = (UnitOwner) iter.next();
                //we dont want to use the damaged icon for unuts that have just
                // been damaged
                boolean useDamagedIcon = category.getDamaged() && !damaged;
                unit.add(new JLabel(m_uiContext.getUnitImageFactory().getIcon(owner.getType(), owner.getOwner(), m_data, useDamagedIcon)));
            }
            panel.add(new JLabel("x " + category.getUnits().size()));
            if (damaged)
                m_damaged.add(panel);
            else
                m_killed.add(panel);
        }
    }

}