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

import java.util.*;
import java.util.List;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import games.strategy.engine.sound.ClipPlayer; //the player
import games.strategy.triplea.sound.SoundPath; //the relative path of sounds

import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.Util;
import games.strategy.util.Match;

import javax.swing.border.*;

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

        m_defenderModel = new BattleModel(m_data, defendingUnits, false);
        m_defenderModel.refresh();
        m_attackerModel = new BattleModel(m_data, attackingUnits, true);
        m_attackerModel.refresh();

        initLayout();
    }


    public void bombingResults(BombingResults message)
    {

        ClipPlayer.getInstance().playClip(SoundPath.BOMB, SoundPath.class); //play sound
        m_dicePanel.setDiceRoll(message);
        m_actionLayout.show(m_actionPanel, DICE_KEY);
    }

    public void casualtyNotificationMessage(CasualtyNotificationMessage message, boolean waitFOrUserInput)
    {

        setStep(message);
        m_casualties.setNotication(message);
        m_actionLayout.show(m_actionPanel, CASUALTIES_KEY);

        if (message.getPlayer().equals(m_defender))
        {
            m_defenderModel.removeCasualties(message);
        } else
        {
            m_attackerModel.removeCasualties(message);
        }

        //if wait is true, then dont return until the user presses continue
        if (!waitFOrUserInput)
            return;

        m_actionButton.setAction(new AbstractAction("Continue")
        {

            public void actionPerformed(ActionEvent e)
            {

                synchronized (m_continueLock)
                {
                    m_continueLock.notifyAll();
                }
            }
        });

        try
        {
            synchronized (m_continueLock)
            {
                m_continueLock.wait();
            }
        } catch (InterruptedException ie)
        {

        }

        m_actionButton.setAction(null);
    }

    public void endBattle(BattleEndMessage msg)
    {

        m_steps.walkToLastStep();

        m_actionButton.setAction(new AbstractAction(msg.getMessage() + " : (Click to close)")
        {

            public void actionPerformed(ActionEvent e)
            {

                synchronized (m_continueLock)
                {
                    m_continueLock.notifyAll();
                }
            }
        });

        try
        {
            synchronized (m_continueLock)
            {
                m_continueLock.wait();
            }
        } catch (InterruptedException ie)
        {

        }

        m_actionButton.setAction(null);
    }

    public void notifyRetreat(RetreatNotificationMessage msg)
    {
        m_defenderModel.notifyRetreat(msg);
        m_attackerModel.notifyRetreat(msg);        
    }
    
    public RetreatMessage getRetreat(RetreatQueryMessage rqm)
    {

        if (!rqm.getSubmerge())
        {
            return getRetreatInternal(rqm);
        } else
        {
            return getSubmerge(rqm);
        }
    }

    private RetreatMessage getSubmerge(RetreatQueryMessage rqm)
    {
        setStep(rqm);

        String message = rqm.getMessage();
        String ok = "Submerge";
        String cancel = "Remain";
        String[] options = {ok, cancel};
        int choice = JOptionPane.showOptionDialog(this, message, "Retreat?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, cancel);
        boolean retreat = (choice == 0);
        if(retreat)
            return new RetreatMessage(null);
        else
            return null;
        
    }

    private RetreatMessage getRetreatInternal(RetreatQueryMessage rqm)
    {

        setStep(rqm);

        String message = rqm.getMessage();
        String ok = "Retreat";
        String cancel = "Remain";
        String[] options = {ok, cancel};
        int choice = JOptionPane.showOptionDialog(this, message, "Retreat?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, cancel);
        boolean retreat = (choice == 0);
        if (!retreat)
            return null;

        RetreatComponent comp = new RetreatComponent(rqm);
        int option = JOptionPane.showConfirmDialog(this, comp, rqm.getMessage(), JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION)
        {
            if (comp.getSelection() != null)
                return new RetreatMessage(comp.getSelection());
        } else
        {
            return getRetreat(rqm);
        }

        return null;
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

    public SelectCasualtyMessage getCasualties(final SelectCasualtyQueryMessage msg)
    {

        setStep(msg);
        m_actionLayout.show(m_actionPanel, DICE_KEY);
        m_dicePanel.setDiceRoll(msg.getDice());

        boolean plural = msg.getCount() > 1;
        final String btnText = msg.getPlayer().getName() + " select " + msg.getCount() + (plural ? " casualties" : " casualty");
        m_actionButton.setEnabled(true);

        m_actionButton.setAction(new AbstractAction(btnText)
        {

            public void actionPerformed(ActionEvent e)
            {

                String messageText = msg.getMessage() + " " + btnText + ".";
                UnitChooser chooser = new UnitChooser(msg.getSelectFrom(), msg.getDefaultCasualties(), msg.getDependent(), m_data, true);

                chooser.setTitle(messageText);
                chooser.setMax(msg.getCount());
                String[] options = {"Ok", "Cancel"};
                int option = JOptionPane.showOptionDialog(BattleDisplay.this, chooser, msg.getPlayer().getName() + " select casualties", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
                if (option != 0)
                    return;
                List killed = chooser.getSelected(false);
                List damaged = chooser.getSelectedFirstHit();

                if (killed.size() + damaged.size() != msg.getCount())
                {
                    JOptionPane.showMessageDialog(BattleDisplay.this, "Wrong number of casualties choosen", msg.getPlayer().getName() + " select casualties", JOptionPane.ERROR_MESSAGE);
                } else
                {
                    SelectCasualtyMessage response = new SelectCasualtyMessage(killed, damaged, false);
                    m_selectCasualtyResponse = response;
                    synchronized (m_continueLock)
                    {
                        m_continueLock.notifyAll();
                    }
                }
            }
        });

        try
        {
            synchronized (m_continueLock)
            {
                m_continueLock.wait();
            }
        } catch (InterruptedException ex)
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
        m_steps.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
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

    public void setStep(BattleMessage message)
    {

        m_steps.setStep(message);

    }

    public Message battleInfo(BattleInfoMessage msg)
    {
        setStep(msg);
        
        if(msg.getMessage() instanceof DiceRoll)
        {

             m_dicePanel.setDiceRoll((DiceRoll) msg.getMessage());
             m_actionLayout.show(m_actionPanel, DICE_KEY);
             
        }
        else
        {
        
            String ok = "OK";
            String[] options = {ok};
            String message = (String) msg.getMessage();
            JOptionPane.showOptionDialog(this, message, msg.getShortMessage(), JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, ok);
        }
        return null;
    }

    public void listBattle(BattleStepMessage message)
    {

        m_steps.listBattle(message);
    }

    private JComponent getPlayerComponent(PlayerID id)
    {

        JLabel player = new JLabel(id.getName());
        player.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
        player.setFont(player.getFont().deriveFont((float) 14));
        return player;
    }

    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;

    private JComponent getTerritoryComponent()
    {

        Image finalImage = Util.createImage(WIDTH, HEIGHT, true);

        Image territory;
        if (m_location.isWater())
            territory = games.strategy.triplea.image.TerritoryImageFactory.getInstance().getSeaImage(m_location);
        else
            territory = games.strategy.triplea.image.TerritoryImageFactory.getInstance().getTerritoryImage(m_location, m_defender);

        finalImage.getGraphics().drawImage(territory, 0, 0, WIDTH, HEIGHT, this);

        return new JLabel(new ImageIcon(finalImage));

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

        super(new Object[0][0], new String[]{" ", "1", "2", "3", "4", "5"});
        m_data = data;
        m_attack = attack;
        //were going to modify the units
        m_units = new ArrayList(units);
    }

    public void notifyRetreat(RetreatNotificationMessage msg)
    {
        m_units.removeAll(msg.getUnits());
        refresh();
    }
    
    
    public void removeCasualties(CasualtyNotificationMessage msg)
    {
        m_units.removeAll(msg.getKilled());
        refresh();
    }

    /**
     * refresh the model from m_units
     */
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

            UnitAttatchment attatchment = UnitAttatchment.get(category.getType());

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
            if(unitsToAdd > 0)
                columns[strength].add(new TableData(category.getOwner(), unitsToAdd, category.getType(), m_data, category.getDamaged()));
            if(supportedUnitsToAdd > 0)
                columns[strength + 1].add(new TableData(category.getOwner(), supportedUnitsToAdd, category.getType(), m_data, category.getDamaged()));
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

    TableData(PlayerID player, int count, UnitType type, GameData data, boolean damaged)
    {

        m_count = count;
        m_icon = UnitIconImageFactory.instance().getIcon(type, player, data, damaged);
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


class BattleStepsPanel extends JPanel
{

    private DefaultListModel m_listModel = new DefaultListModel();
    private JList m_list = new JList(m_listModel);
    private MyListSelectionModel m_listSelectionModel = new MyListSelectionModel();

    public BattleStepsPanel()
    {

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

    public void listBattle(final BattleStepMessage msg)
    {

        m_listModel.removeAllElements();

        Iterator iter = msg.getSteps().iterator();
        while (iter.hasNext())
        {
            m_listModel.addElement(iter.next());
        }
        m_listSelectionModel.hiddenSetSelectionInterval(0);

        validate();

    }

    /**
     * Walks through and pause at each list item.
     */
    private void walkStep(final int start, final int stop)
    {

        if (start < 0 || stop < 0 || stop >= m_listModel.getSize())
            throw new IllegalStateException("Illegal start and stop.  start:" + start + " stop:" + stop);

        int current = start;
        while (current != stop)
        {
            if (current == 0)
                pause();

            current++;
            if (current >= m_listModel.getSize())
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
            } catch (InterruptedException ie)
            {
            } catch (java.lang.reflect.InvocationTargetException ioe)
            {
                ioe.printStackTrace();
                throw new RuntimeException(ioe.getMessage());
            }
        }
    }

    /**
     * Doesnt allow the user to change the selection, must be done through
     * hiddenSetSelectionInterval.
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
            synchronized (lock)
            {
                lock.wait(300);
            }
        } catch (InterruptedException ie)
        {
        }
    }

    public void walkToLastStep()
    {

        if (m_list.getSelectedIndex() == m_list.getModel().getSize() - 1)
            return;
        walkStep(m_list.getSelectedIndex(), m_list.getModel().getSize() - 1);
    }

    public void setStep(BattleMessage msg)
    {

        if (msg.getStep() != null)
        {
            int newIndex = m_listModel.lastIndexOf(msg.getStep());
            int currentIndex = m_list.getSelectedIndex();
            if (newIndex != -1)
                walkStep(currentIndex, newIndex);
        }
    }

}


class CasualtyNotificationPanel extends JPanel
{

    private DicePanel m_dice = new DicePanel();
    private JPanel m_killed = new JPanel();
    private JPanel m_damaged = new JPanel();
    private GameData m_data;

    public CasualtyNotificationPanel(GameData data)
    {

        m_data = data;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(m_dice);
        add(m_killed);
        add(m_damaged);
    }

    public void setNotication(CasualtyNotificationMessage msg)
    {

        m_dice.setDiceRoll(msg.getDice());

        m_killed.removeAll();
        m_damaged.removeAll();
        Collection killed = msg.getKilled();
        if (!killed.isEmpty())
        {
            m_killed.add(new JLabel("Killed"));
        }
        Iterator killedIter = UnitSeperator.categorize(killed, msg.getDependents(), null).iterator();
        categorizeUnits(killedIter, false);

        Collection damaged = new ArrayList(msg.getDamaged());
        damaged.removeAll(killed);
        if (!damaged.isEmpty())
        {
            m_damaged.add(new JLabel("Damaged"));
        }
        Iterator damagedIter = UnitSeperator.categorize(damaged, msg.getDependents(), null).iterator();
        categorizeUnits(damagedIter, true);

        invalidate();
    }

    private void categorizeUnits(Iterator categoryIter, boolean damaged)
    {

        while (categoryIter.hasNext())
        {
            UnitCategory category = (UnitCategory) categoryIter.next();
            JPanel panel = new JPanel();
            JLabel unit = new JLabel(UnitIconImageFactory.instance().getIcon(category.getType(), category.getOwner(), m_data, category.getDamaged()));
            panel.add(unit);
            Iterator iter = category.getDependents().iterator();
            while (iter.hasNext())
            {
                UnitOwner owner = (UnitOwner) iter.next();
                //we dont want to use the damaged icon for unuts that have just
                // been damaged
                boolean useDamagedIcon = category.getDamaged() && !damaged;
                unit.add(new JLabel(UnitIconImageFactory.instance().getIcon(owner.getType(), owner.getOwner(), m_data, useDamagedIcon)));
            }
            panel.add(new JLabel("x " + category.getUnits().size()));
            if (damaged)
                m_damaged.add(panel);
            else
                m_killed.add(panel);
        }
    }

}
