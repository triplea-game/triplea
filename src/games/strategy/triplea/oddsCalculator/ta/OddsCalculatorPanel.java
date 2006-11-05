package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.ui.background.WaitDialog;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.UIContext;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.ScrollableTextField;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class OddsCalculatorPanel extends JPanel
{    
    
    private static final int FIGHT_COUNT = 5000;
    
    
    private Window m_parent;
    private JLabel m_attackerWin;
    private JLabel m_defenderWin;
    private JLabel m_draw;
    private JLabel m_defenderLeft;
    private JLabel m_attackerLeft;
    private JLabel m_count;
    
    private UIContext m_context;
    private GameData m_data;
    
    
    private JPanel m_resultsPanel;
    private JButton m_calculateButton;
    private JButton m_closeButton;
    
    private PlayerUnitsPanel m_attackingUnitsPanel;
    private PlayerUnitsPanel m_defendingUnitsPanel;
    
    private JComboBox m_attackerCombo;
    private JComboBox m_defenderCombo;
    private JCheckBox m_keepOneAttackingLandUnitCombo;
    
    private JCheckBox m_landBattle;
    
    private JButton m_clearButton;


    private JLabel m_time;
    
    public OddsCalculatorPanel(GameData data, UIContext context, Territory location, Window parent)
    {

        m_data = data;
        m_context = context;
        
        createComponents();
        layoutComponents();
        setupListeners();
        
        m_parent = parent;
        
        
        
        if(location != null) 
        {
            m_data.acquireReadLock();
            try
            {
                m_landBattle.setSelected(!location.isWater());
                
                if(!location.getUnits().isEmpty())
                {
                    if(!location.isWater())
                    {
                        m_defenderCombo.setSelectedItem(location.getOwner());
                        
                    }
                    else
                    {
                        //we need to find out the defender for sea zones
                        m_defenderCombo.setSelectedItem(location.getUnits().getPlayersWithUnits().iterator().next());
                    }
                    
                    //find a player with units not allied to the defender
                    //he is the attacker
                    for(PlayerID player : location.getUnits().getPlayersWithUnits())
                    {
                        if(!m_data.getAllianceTracker().isAllied(player, getDefender()))
                        {
                            m_attackerCombo.setSelectedItem(player);
                            break;
                        }
                    }

                }
                
                updateDefender(location.getUnits().getMatches(Matches.alliedUnit(getDefender(), data)));
                updateAttacker(location.getUnits().getMatches(Matches.alliedUnit(getAttacker(), data)));
    
            }
            finally
            {
                m_data.releaseReadLock();
            }
            
               
        }
        else
        {
            m_landBattle.setSelected(true);
            m_defenderCombo.setSelectedItem(data.getPlayerList().getPlayers().iterator().next());
            updateDefender(null);
            updateAttacker(null);

        }
        
        
        setWidgetActivation();

        
        
    }
    
    
    

    private PlayerID getDefender()
    {
        return (PlayerID) m_defenderCombo.getSelectedItem();
    }
    
    private PlayerID getAttacker()
    {
        return (PlayerID) m_attackerCombo.getSelectedItem();
    }

    
    private void setupListeners()
    {
        m_defenderCombo.addActionListener(new ActionListener()
        {
        
        
            public void actionPerformed(ActionEvent e)
            {
                if(m_data.getAllianceTracker().isAllied(getDefender(), getAttacker()))
                {
                    m_attackerCombo.setSelectedItem(getNonAllied(getDefender()));
                }
                updateDefender(null);
                
            }


        
        });
        
        
        m_attackerCombo.addActionListener(new ActionListener()
        {
        
           
            public void actionPerformed(ActionEvent e)
            {
                if(m_data.getAllianceTracker().isAllied(getDefender(), getAttacker()))
                {
                    m_defenderCombo.setSelectedItem(getNonAllied(getAttacker()));
                }
                updateAttacker(null);
                
            }
        
        });
        
        m_landBattle.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                updateDefender(null);
                updateAttacker(null);
                setWidgetActivation();
        
            }
        
        });
        
        m_calculateButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                updateStats();
        
            }
        
        });
        
        m_closeButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_parent.setVisible(false);
        
            }
        
        });
        
        m_clearButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_defendingUnitsPanel.clear();
                m_attackingUnitsPanel.clear();
        
            }
        
        });

        
    }
    
    private void updateStats()
    {
        final WaitDialog dialog = new WaitDialog(this, "Calculating Odds");
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        
        final AtomicReference<AggregateResults> results = new AtomicReference<AggregateResults>();
        
        new Thread(new Runnable()
        {
        
            public void run()
            {
                try
                {
                    OddsCalculator calculator = new OddsCalculator();
                    
                    //find a territory to fight in
                    Territory location = null;
                    for(Territory t : m_data.getMap())
                    {
                        if(t.isWater() == !isLand())
                        {
                            location = t;
                            break;
                        }
                    }
                    if(location == null)
                        throw new IllegalStateException("No territory found that is land:" + isLand());
                   
                    List<Unit> defending = m_defendingUnitsPanel.getUnits();
                    List<Unit> attacking = m_attackingUnitsPanel.getUnits();
                    List<Unit> bombarding = new ArrayList<Unit>();
                    if(isLand())
                    {
                        bombarding = Match.getMatches(attacking, Matches.unitCanBombard(getAttacker()));
                        attacking.removeAll(bombarding);
                    }
                    
    
                    if(m_landBattle.isSelected() && m_keepOneAttackingLandUnitCombo.isSelected())
                        calculator.setKeepOneAttackingLandUnit(true);
                    else
                        calculator.setKeepOneAttackingLandUnit(false);
                    
                    results.set(calculator.calculate(m_data, getAttacker(), getDefender(), location, attacking, defending, bombarding, FIGHT_COUNT));
                 
                }
                finally
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                    
                        public void run()
                        {
                            dialog.setVisible(false);
                            dialog.dispose();
                        }
                    
                    });
                    
                }
        
            }
        
        }, "Odds calc thread").start();
       
        if(results.get() == null)
            dialog.setVisible(true);

        m_attackerWin.setText(formatPercentage(results.get().getAttackerWinPercent()));
        m_defenderWin.setText(formatPercentage(results.get().getDefenderWinPercent()));
        m_draw.setText(formatPercentage(results.get().getDrawPercent()));
        
        m_defenderLeft.setText(formatValue(results.get().getAverageDefendingUnitsLeft()));
        m_attackerLeft.setText(formatValue(results.get().getAverageAttackingUnits()));
        m_count.setText(results.get().getRollCount() + "");
        m_time.setText(formatValue(results.get().getTime() / 1000.0) + "s");
        
        
    }
    
    public String formatPercentage(double percentage)
    {
        NumberFormat format = new DecimalFormat("%");
        return format.format(percentage);
        
    }
    
    public String formatValue(double value)
    {
        NumberFormat format = new DecimalFormat("#0.##");
        return format.format(value);
        
    }


    
    
    private void updateDefender(List<Unit> units)
    {
        if(units == null)
            units = Collections.emptyList();
        units = Match.getMatches(units, Matches.UnitIsNotFactory);

        m_defendingUnitsPanel.init(getDefender(), units, isLand());        
    }
    
    private boolean isLand()
    {
        return m_landBattle.isSelected();
    }
    
    
    private PlayerID getNonAllied(PlayerID player)
    {
        for(PlayerID id : m_data.getPlayerList())
        {
            if(!m_data.getAllianceTracker().isAllied(player, id))
                return id;
        }
        
        throw new IllegalStateException("No enemies for :" + player);
    }

    private void layoutComponents()
    {
        setLayout(new BorderLayout());
        
        
        JPanel main = new JPanel();
        add(main, BorderLayout.CENTER);
    
        JPanel attackAndDefend = new JPanel();
        attackAndDefend.setLayout(new GridBagLayout());
        
        
        int gap = 20;
        
        attackAndDefend.add(new JLabel("Attacker: "), new GridBagConstraints(0,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,gap,gap,0), 0,0));
        attackAndDefend.add(m_attackerCombo, new GridBagConstraints(1,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,gap,gap), 0,0));
        attackAndDefend.add(new JLabel("Defender: "), new GridBagConstraints(2,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,gap,gap,0), 0,0));
        attackAndDefend.add(m_defenderCombo, new GridBagConstraints(3,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,gap,gap), 0,0));
        
        JScrollPane attackerScroll = new JScrollPane(m_attackingUnitsPanel);
        attackerScroll.setBorder(null);
        attackerScroll.getViewport().setBorder(null);
        JScrollPane defenderScroll = new JScrollPane(m_defendingUnitsPanel);
        defenderScroll.setBorder(null);
        defenderScroll.getViewport().setBorder(null);
        
        
        attackAndDefend.add(attackerScroll, new GridBagConstraints(0,1, 2,1,1,1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10,gap,gap,gap), 0,0));
        attackAndDefend.add(defenderScroll, new GridBagConstraints(2,1, 2,1,1,1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10,gap,gap,gap), 0,0));
        
        
        main.add(attackAndDefend);
        
        
        JPanel resultsText = new JPanel();
        resultsText.setLayout(new GridBagLayout());
        
        resultsText.add(new JLabel("Attacker Wins:"), new GridBagConstraints(0,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        resultsText.add(new JLabel("Draw:"), new GridBagConstraints(0,1, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        resultsText.add(new JLabel("Defender Wins:"), new GridBagConstraints(0,2, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        
        resultsText.add(new JLabel("Defender Units Left:"), new GridBagConstraints(0,3, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        resultsText.add(new JLabel("Attacker Units Left:"), new GridBagConstraints(0,4, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        resultsText.add(new JLabel("Simulation Count:"), new GridBagConstraints(0,5, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15,0,0,0), 0,0));
        resultsText.add(new JLabel("Time:"), new GridBagConstraints(0,6, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        
        
        resultsText.add(m_attackerWin, new GridBagConstraints(1,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0,0));   
        resultsText.add(m_draw, new GridBagConstraints(1,1, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0,0));        
        resultsText.add(m_defenderWin, new GridBagConstraints(1,2, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0,0));
        resultsText.add(m_defenderLeft, new GridBagConstraints(1,3, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0,0));
        resultsText.add(m_attackerLeft, new GridBagConstraints(1,4, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0,0));
        resultsText.add(m_count, new GridBagConstraints(1,5, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15,5,0,0), 0,0));
        resultsText.add(m_time, new GridBagConstraints(1,6, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0,0));
        
        
        resultsText.add(m_keepOneAttackingLandUnitCombo, new GridBagConstraints(0,7, 2,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(35,5,5,5), 0,0));
        resultsText.add(m_landBattle, new GridBagConstraints(0,8, 2,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,5,5,5), 0,0));
        
        resultsText.add(m_clearButton, new GridBagConstraints(0,9, 2,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10,5,5,5), 0,0));
        resultsText.add(m_calculateButton, new GridBagConstraints(0,10, 2,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(25,5,5,5), 0,0));
        
        m_resultsPanel.add(resultsText);
        
        
        
        main.add(m_resultsPanel);
        

        JPanel south = new JPanel();
        
        
        
        south.setLayout( new BorderLayout());
        
        
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        
        buttons.add(m_closeButton);
       
        south.add(buttons, BorderLayout.SOUTH);
        
        
        
        add(south, BorderLayout.SOUTH);
        
    }

    private void createComponents()
    {
    
        
        m_attackerCombo = new JComboBox(new Vector<PlayerID>(m_data.getPlayerList().getPlayers()));
        m_defenderCombo = new JComboBox(new Vector<PlayerID>(m_data.getPlayerList().getPlayers()));
        
        m_defenderCombo.setRenderer( new PlayerRenderer());
        m_attackerCombo.setRenderer( new PlayerRenderer());
        
        m_defendingUnitsPanel = new PlayerUnitsPanel(m_data, m_context, true);
        m_attackingUnitsPanel = new PlayerUnitsPanel(m_data, m_context, false);
        
        m_landBattle = new JCheckBox("Land Battle");
        
        m_calculateButton = new JButton("Calculate Odds");
        m_resultsPanel = new JPanel();
        
        String blank = "------";
        m_attackerWin = new JLabel(blank);
        m_defenderWin = new JLabel(blank);
        m_draw = new JLabel(blank);
        
        m_defenderLeft = new JLabel(blank);
        m_attackerLeft = new JLabel(blank);
        
        m_count = new JLabel(blank);
        m_time = new JLabel(blank);
        
        m_closeButton = new JButton("Close");
        m_clearButton = new JButton("Clear");
        
        m_keepOneAttackingLandUnitCombo = new JCheckBox("One attacking unit must live");
        
    }
    
    
    
    
    
    private void updateAttacker(List<Unit> units)
    {
        if(units == null)
            units = Collections.emptyList();
        m_attackingUnitsPanel.init(getAttacker(), units, isLand());
        
        
        
        
    }
    
    public void setWidgetActivation()
    {
        m_keepOneAttackingLandUnitCombo.setEnabled(m_landBattle.isSelected());
    }






    class PlayerRenderer extends DefaultListCellRenderer
    {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            PlayerID id = (PlayerID) value;
            setText(id.getName());
            setIcon(new ImageIcon(m_context.getFlagImageFactory().getSmallFlag(id)));
            return this;
        }
        
    }
    
    
}

class PlayerUnitsPanel extends JPanel
{
    private final GameData m_data;
    private final UIContext m_context;
    private final boolean m_defender;
    
    
    PlayerUnitsPanel(GameData data, UIContext context, boolean defender)
    {
        m_data = data;
        m_context = context;
        m_defender = defender;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
    
    public void clear()
    {
        for(Component c : getComponents())
        {
            UnitPanel panel = (UnitPanel) c;
            panel.setCount(0);
            
        }
    }
    
    public List<Unit> getUnits()
    {
        List<Unit> allUnits = new ArrayList<Unit>();
        for(Component c : getComponents())
        {
            UnitPanel panel = (UnitPanel) c;
            allUnits.addAll(panel.getUnits());
            
        }
        return allUnits;
    
    }
    
    
    public void init(PlayerID id, List<Unit> units, final boolean land)
    {
        List<UnitCategory> categories = new ArrayList<UnitCategory>(categorize(id, units));
        Collections.sort(categories, new Comparator<UnitCategory>()
        {
        
            public int compare(UnitCategory o1, UnitCategory o2)
            {
                UnitAttachment u1 = UnitAttachment.get(o1.getType());
                UnitAttachment u2 = UnitAttachment.get(o2.getType());
                
                //for land, we want land, air, aa gun, then bombarding
                if(land)
                {
                    if(u1.isSea() != u2.isSea())
                    {
                        return u1.isSea() ? 1 : -1;
                    }

                    if(u1.isAA() != u2.isAA())
                    {
                        return u1.isAA() ? 1 : -1;
                    }

                    
                    if(u1.isAir() != u2.isAir())
                    {
                        return u1.isAir() ? 1 : -1;
                    }

                    
                }
                else
                {
                    if(u1.isSea() != u2.isSea())
                    {
                        return u1.isSea() ? -1 : 1;
                    }
                }
                
                return u1.getName().compareTo(u2.getName());
         
            }
        
        });
        
        
        removeAll();
        
        Match<UnitType> predicate;
        if(land)
        {
            if(m_defender)
                predicate = Matches.UnitTypeIsNotSea;
            else
                predicate = new CompositeMatchOr<UnitType>(Matches.UnitTypeIsNotSea, Matches.unitTypeCanBombard(id));
        }
        else
            predicate = Matches.UnitTypeIsSeaOrAir;
        
        for(UnitCategory category : categories)
        {
            if(predicate.match(category.getType()))
                add(new UnitPanel(m_data, m_context, category));
        }
        
        
        invalidate();
        validate();
        revalidate();
        getParent().invalidate();
        
        
    }

    private Set<UnitCategory> categorize(PlayerID id, List<Unit> units)
    {
        //these are the units that exist
        Set<UnitCategory> categories = UnitSeperator.categorize(units);
        
        //the units that can be produced or moved in
        for(UnitType t : getUnitTypes(id))
        {
            UnitCategory category = new UnitCategory(t, id);
            categories.add(category);
        }
        return categories;
    }
    
    
    /**
     * return all the unit types available for the given player.
     * a unit type is available if the unit is producable,
     * or if a player has one
     */
    private Collection<UnitType> getUnitTypes(PlayerID player)
    {
        Collection<UnitType> rVal = new HashSet<UnitType>();
        
        ProductionFrontier frontier = player.getProductionFrontier();
        for(ProductionRule rule : frontier)
        {
            for(NamedAttachable type : rule.getResults().keySet())
            {
                if(type instanceof UnitType)
                    rVal.add((UnitType) type);
            }
        }
        
        for(Territory t : m_data.getMap())
        {
            for(Unit u : t.getUnits())
            {
                if(u.getOwner().equals(player))
                    rVal.add(u.getType());
            }
        }
        
        
        //filter out factories
        rVal = Match.getMatches(rVal, Matches.UnitTypeIsFactory.invert());
        
        //aa guns can't attack
        if(!m_defender)
            rVal = Match.getMatches(rVal, Matches.UnitTypeIsAA.invert());
        
        return rVal;
            
    }
    
    
}



class UnitPanel extends JPanel
{
    private final UIContext m_context;
    private final UnitCategory m_category;
    private final ScrollableTextField m_textField;
    private final GameData m_data;
    
    public UnitPanel(GameData data, UIContext context, UnitCategory category)
    {
        m_category = category;
        m_context = context;
        m_data = data;
        
        
        
        m_textField = new ScrollableTextField(0,512);
        
        
        
        
        m_textField.setShowMaxAndMin(false);
        
        Image img = m_context.getUnitImageFactory().getImage(m_category.getType(), m_category.getOwner(), m_data, m_category.getDamaged());
        
        setCount(m_category.getUnits().size());
        
        setLayout(new GridBagLayout());
        add(new JLabel(new ImageIcon(img)),  new GridBagConstraints(0,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,10), 0,0) );
        add(m_textField, new GridBagConstraints(1,0, 1,1,0,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        

    }
    
    public List<Unit> getUnits()
    {
        return m_category.getType().create(m_textField.getValue(), m_category.getOwner() ,true);
    }

    public int getCount()
    {
        return m_textField.getValue();
    }
    
    public void setCount(int value)
    {
        m_textField.setValue(value);
    }
    
    public UnitCategory getCategory()
    {
        return m_category;
    }

    
    
}


