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

import games.strategy.engine.data.*;
import games.strategy.engine.framework.GameRunner;
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
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
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

    private final BattleModel m_defenderModel;
    private final BattleModel m_attackerModel;
    private BattleStepsPanel m_steps;

    private DicePanel m_dicePanel;
    private final CasualtyNotificationPanel m_casualties;
    private JPanel m_actionPanel;
    private final CardLayout m_actionLayout = new CardLayout();
    private final JPanel m_messagePanel = new JPanel();
    private final MapPanel m_mapPanel;
    private final JPanel m_causalitiesInstantPanelDefender = new JPanel();
    private final JPanel m_causalitiesInstantPanelAttacker = new JPanel();
    private final JLabel LABEL_NONE_ATTACKER = new JLabel("None");
    private final JLabel LABEL_NONE_DEFENDER = new JLabel("None");
    private final Map<UnitType, JLabel> m_UnitKillMapDefender = new HashMap<UnitType, JLabel>();
    private final Map<UnitType, JLabel> m_UnitKillMapAttacker = new HashMap<UnitType, JLabel>();
    private UIContext m_uiContext;

    private final JLabel m_messageLabel = new JLabel();
    
    private Action m_nullAction = new AbstractAction(" "){
    
        public void actionPerformed(ActionEvent e)
        {}
    
    };
    
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
        m_uiContext = mapPanel.getUIContext(); 

        initLayout();
    }

    public void cleanUp()
    {
        m_steps.deactivate();
        m_mapPanel.getUIContext().removeACtive(m_steps);
        m_steps = null;
    }

    void takeFocus() {
        //we want a component on this frame to take focus
        //so that pressing space will work (since it requires in focused
        //window).  Only seems to be an issue on windows
        m_actionButton.requestFocus();
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

    /**
     * updates the panel content according to killed units for the player
     * @param aKilledUnits list of units killed
     * @param aPlayerID player kills belongs to
     * @author ahmet pata
     */
    private void updateKilledUnits(Collection<Unit> aKilledUnits, PlayerID aPlayerID)
    {
        final Map<UnitType, JLabel> lKillMap;
        final JPanel lCausalityPanel;
        final JLabel lPlayerNoneLabel;
        
        if (aPlayerID.equals(m_defender))
        {
            lKillMap = m_UnitKillMapDefender;
            lCausalityPanel = m_causalitiesInstantPanelDefender;
            lPlayerNoneLabel = LABEL_NONE_DEFENDER;
        }
        else
        {
            lKillMap = m_UnitKillMapAttacker;
            lCausalityPanel = m_causalitiesInstantPanelAttacker;
            lPlayerNoneLabel = LABEL_NONE_ATTACKER;
        }

        if (!aKilledUnits.isEmpty())
        {
        	JLabel label = new JLabel("x1");
        }
        
        Iterator killedIter = UnitSeperator.categorize(aKilledUnits, null, false).iterator();
        
        while (killedIter.hasNext())
        {
            UnitCategory category = (UnitCategory) killedIter.next();
            JPanel panel = new JPanel();
            JLabel unit = new JLabel(m_uiContext.getUnitImageFactory().getIcon(category.getType(), category.getOwner(), m_data, false));
            panel.add(unit);
            
            Iterator iter = category.getDependents().iterator();
            while (iter.hasNext())
            {
                UnitOwner owner = (UnitOwner) iter.next();
                unit.add(new JLabel(m_uiContext.getUnitImageFactory().getIcon(owner.getType(), owner.getOwner(), m_data, false)));
            }
            panel.add(new JLabel("x " + category.getUnits().size()));
            lCausalityPanel.add(panel);            
        }
    }

    public void casualtyNotification(String step, DiceRoll dice, PlayerID player, Collection<Unit> killed, Collection<Unit> damaged, Map<Unit, Collection<Unit>> dependents)
    {
        setStep(step);
        m_casualties.setNotication(dice, player, killed, damaged, dependents);
        m_actionLayout.show(m_actionPanel, CASUALTIES_KEY);

        updateKilledUnits(killed, player);
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

        m_mapPanel.getUIContext().addShutdownLatch(continueLatch);
        
        //wait for the button to be pressed.
        try
        {
            continueLatch.await();
        } catch (InterruptedException ie)
        {
            
        }
        finally
        {
            m_mapPanel.getUIContext().removeShutdownLatch(continueLatch);
        }
        

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_actionButton.setAction(m_nullAction);
            }
        });

    }

    public void endBattle(String message, final Window enclosingFrame)
    {
        m_steps.walkToLastStep();
        final Action close = new AbstractAction(message + " : (Press Space to close)")
        {
            public void actionPerformed(ActionEvent e)
            {
                enclosingFrame.setVisible(false);
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

        
        m_mapPanel.getUIContext().addShutdownLatch(latch);
        try
        {
            latch.await();
        } catch (InterruptedException e1)
        {
            
        }
        finally
        {
            m_mapPanel.getUIContext().removeShutdownLatch(latch);
        }
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_actionButton.setAction(m_nullAction);
        
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
                int option = JOptionPane.showConfirmDialog(BattleDisplay.this, comp, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, (Icon) null);
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
                m_actionButton.setAction(m_nullAction);
        
            }
        
        });
        
        
        return retreatTo[0];
    }

    private class RetreatComponent extends JPanel
    {
        private JList m_list;
        private JLabel m_retreatTerritory = new JLabel("");
        
        RetreatComponent(Collection<Territory> possible)
        {

            this.setLayout(new BorderLayout());

            JLabel label = new JLabel("Retreat to...");
            label.setBorder(new EmptyBorder(0,0,10,0));
            this.add(label, BorderLayout.NORTH);
            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            imagePanel.add(m_retreatTerritory);
            
            imagePanel.setBorder(new EmptyBorder(10,10,10,0));
            
            this.add(imagePanel, BorderLayout.EAST);

            Vector<Territory> listElements = new Vector<Territory>(possible);

            m_list = new JList(listElements);
            m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (listElements.size() >= 1)
                m_list.setSelectedIndex(0);
            JScrollPane scroll = new JScrollPane(m_list);
            this.add(scroll, BorderLayout.CENTER);
            
            scroll.setBorder(new EmptyBorder(10,0,10,0));
            updateImage();
            
            
            m_list.addListSelectionListener(new ListSelectionListener()
            {
            
                public void valueChanged(ListSelectionEvent e)
                {
                    updateImage();
                }
            
            });
            
        }
        
        private void updateImage()
        {
            int width = 250;
            int height = 250;
            Image img = m_mapPanel.getTerritoryImage((Territory) m_list.getSelectedValue(), m_location); 
            
            Image finalImage = Util.createImage(width, height, true);

            

            Graphics g = finalImage.getGraphics();
            g.drawImage(img, 0, 0, width, height, this);
            g.dispose();
            
            
            m_retreatTerritory.setIcon(new ImageIcon( finalImage ));
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
            throw new IllegalStateException("This method should not be run in the event dispatch thread");

        final AtomicReference<CasualtyDetails> casualtyDetails = new AtomicReference<CasualtyDetails>();
        final CountDownLatch continueLatch = new CountDownLatch(1);
        
        SwingUtilities.invokeLater(new Runnable()
        {

            
            public void run()
            {
                final boolean isEditMode = (dice == null);

                if (!isEditMode)
                {
                    m_actionLayout.show(m_actionPanel, DICE_KEY);
                    m_dicePanel.setDiceRoll(dice);
                }

                boolean plural = isEditMode || (count > 1);
                String countStr = isEditMode ? "" : "" + count;
                final String btnText = hit.getName() + ", press space to select " + countStr + (plural ? " casualties" : " casualty");
                m_actionButton.setAction(new AbstractAction(btnText)
                {
                    public void actionPerformed(ActionEvent e)
                    {

                        String messageText = message + " " + btnText + ".";
                        //TODO COMCO perhaps here's where to restrict TRNs
                        kev
                        UnitChooser chooser = new UnitChooser(selectFrom, defaultCasualties, dependents, m_data, true, m_mapPanel.getUIContext());

                        chooser.setTitle(messageText);
                        if (isEditMode)
                            chooser.setMax(selectFrom.size());
                        else
                            chooser.setMax(count);
                        String[] options =
                        { "Ok", "Cancel" };

                        int option = JOptionPane.showOptionDialog(BattleDisplay.this, chooser, hit.getName() + " select casualties",
                                JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
                        if (option != 0)
                            return;
                        List<Unit> killed = chooser.getSelected(false);
                        List<Unit> damaged = chooser.getSelectedFirstHit();

                        if (!isEditMode && (killed.size() + damaged.size() != count))
                        {
                            JOptionPane.showMessageDialog(BattleDisplay.this, "Wrong number of casualties selected", hit.getName()
                                    + " select casualties", JOptionPane.ERROR_MESSAGE);
                            return;
                        } else
                        {
                            CasualtyDetails response = new CasualtyDetails(killed, damaged, false);
                            casualtyDetails.set(response);
                            
                            m_dicePanel.clear();
                            m_actionButton.setEnabled(false);
                            m_actionButton.setAction(m_nullAction);

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
            ex.printStackTrace();
        }

        
        return casualtyDetails.get();
        
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
        
        m_mapPanel.getUIContext().addActive(m_steps);
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
        
        m_causalitiesInstantPanelAttacker.setLayout(
                new FlowLayout(FlowLayout.LEFT , 2, 2));
        m_causalitiesInstantPanelAttacker.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        m_causalitiesInstantPanelAttacker.add(LABEL_NONE_ATTACKER);

        m_causalitiesInstantPanelDefender.setLayout(
                new FlowLayout(FlowLayout.LEFT , 2, 2));
        m_causalitiesInstantPanelDefender.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        m_causalitiesInstantPanelDefender.add(LABEL_NONE_DEFENDER);

        JPanel lInstantCausalitiesPanel = new JPanel();
        lInstantCausalitiesPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        lInstantCausalitiesPanel.setLayout(new GridBagLayout());

        JLabel lCausalities = new JLabel("Causalities", JLabel.CENTER);
        lCausalities.setFont(getPlayerComponent(m_attacker).getFont().deriveFont(Font.BOLD, 14));        
        lInstantCausalitiesPanel.add(
                lCausalities,
                new GridBagConstraints(0, 0, 2, 1, 1.0d, 1.0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
                new Insets(0, 0, 0, 0), 0, 0));
        
        
        
        lInstantCausalitiesPanel.add(
                m_causalitiesInstantPanelAttacker,
                new GridBagConstraints(0, 2, 1, 1, 1.0d, 1.0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
                        new Insets(0, 0, 0, 0), 0, 0));
        
        lInstantCausalitiesPanel.add(
                m_causalitiesInstantPanelDefender,
                new GridBagConstraints(1, 2, 1, 1, 1.0d, 1.0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
                        new Insets(0, 0, 0, 0), 0, 0));
        
        diceAndSteps.add(lInstantCausalitiesPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(diceAndSteps, BorderLayout.CENTER);

        add(m_actionButton, BorderLayout.SOUTH);
        m_actionButton.setEnabled(false);
        if(!GameRunner.isMac())
        {
            m_actionButton.setBackground(Color.lightGray.darker());
            m_actionButton.setForeground(Color.white);
        }

        setDefaultWidhts(defenderTable);
        setDefaultWidhts(attackerTable);
        
        
        Action continueAction = new AbstractAction()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                Action a = m_actionButton.getAction();
                if(a != null)
                    a.actionPerformed(null);
            }
        };
 
        //press space to continue
        String key = "battle.display.press.space.to.continue";
        getActionMap().put(key, continueAction);
        getInputMap( WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), key);
                


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

        Graphics g = finalImage.getGraphics();
        g.drawImage(territory, 0, 0, MY_WIDTH, MY_HEIGHT, this);
        g.dispose();

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
            UnitAttachment attachment = UnitAttachment.get(category.getType());

            if (m_attack)
            {
                strength = attachment.getAttack(category.getOwner());
                // Increase attack value if it's an assaulting marine
                if (DiceRoll.isAmphibiousMarine(attachment, m_data))
                	++strength;
	        } 
            else
                //If it's Pacific_Edition and Japan's turn one, all but Chinese defend at a 1
            {
                strength = attachment.getDefense(category.getOwner());
                if( DiceRoll.isFirstTurnLimitedRoll(category.getOwner()))
                    strength = Math.min(1, strength);
            }

            int unitsToAdd = category.getUnits().size();
            int supportedUnitsToAdd = 0;
            
            //Note it's statistically irrelevant whether we support the Infantry or Marines
            //factor in artillery support
            if (attachment.isArtillerySupportable() && m_attack)
            {
                supportedUnitsToAdd = Math.min(artillerySupportAvailable, unitsToAdd);
                artillerySupportAvailable -= supportedUnitsToAdd;
                unitsToAdd -= supportedUnitsToAdd;
            }
            if (unitsToAdd > 0)
                columns[strength].add(new TableData(category.getOwner(), unitsToAdd, category.getType(), m_data, category.getDamaged(), m_uiContext));
            if (supportedUnitsToAdd > 0)
                columns[strength + 1].add(new TableData(category.getOwner(), supportedUnitsToAdd, category.getType(), m_data, category.getDamaged(), m_uiContext));
        } //while

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

    static final TableData NULL = new TableData();
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

        boolean isEditMode = (dice == null);
        if (!isEditMode)
            m_dice.setDiceRoll(dice);

        m_killed.removeAll();
        m_damaged.removeAll();

        if (!killed.isEmpty())
        {
            m_killed.add(new JLabel("Killed"));
        }
        
        Iterator killedIter = UnitSeperator.categorize(killed, dependents, false).iterator();
        categorizeUnits(killedIter, false);

        damaged.removeAll(killed);
        if (!damaged.isEmpty())
        {
            m_damaged.add(new JLabel("Damaged"));
        }
        Iterator damagedIter = UnitSeperator.categorize(damaged, dependents, false).iterator();
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
                //we don't want to use the damaged icon for units that have just
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