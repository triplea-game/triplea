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
* UnitChooser.java
*
* Created on December 3, 2001, 7:32 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.util.*;
import games.strategy.ui.*;
import games.strategy.util.IntegerMap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitChooser extends JPanel
{

  private List<ChooserEntry> m_entries = new ArrayList<ChooserEntry>();
  private final Map<Unit, Collection<Unit>> m_dependents;
  private JTextArea m_title;
  private int m_total = -1;
  private JLabel m_leftToSelect = new JLabel();
  private GameData m_data;
  private boolean m_allowTwoHit = false;
  private JButton m_autoSelectButton;
  private JButton m_selectNoneButton;
  /** Creates new UnitChooser */

  @SuppressWarnings("unchecked")
public UnitChooser(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, IntegerMap<Unit> movement, GameData data)
  {
    m_dependents = dependent;
    m_data = data;
    createEntries(units, dependent, movement, Collections.EMPTY_LIST);
    layoutEntries();
  }

 @SuppressWarnings("unchecked")
public UnitChooser(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, GameData data, boolean allowTwoHit)
  {
    this(units, Collections.EMPTY_LIST, dependent, data, allowTwoHit);
  }

  public UnitChooser(Collection<Unit> units, List<Unit> defaultSelections, Map<Unit, Collection<Unit>> dependent, GameData data, boolean allowTwoHit)
  {
    m_dependents = dependent;
    m_data = data;
    m_allowTwoHit = allowTwoHit;
    createEntries(units, dependent, null, defaultSelections);
    layoutEntries();
  }

  /**
   * Set the maximum number of units that we can choose.
   */
  public void setMax(int max)
  {
    m_total = max;
    m_textFieldListener.changedValue(null);
    m_autoSelectButton.setVisible(false);
    m_selectNoneButton.setVisible(false);
  }

  public void setTitle(String title)
  {
    m_title.setText(title);
  }

  private void updateLeft()
  {
    if(m_total == -1)
      return;

    int selected = 0;
    Iterator<ChooserEntry> iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      selected += entry.getTotalHits();
    }

    m_leftToSelect.setText("Left to select:" + (m_total - selected));

    iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      entry.setLeftToSelect(m_total - selected);
    }

    m_leftToSelect.setText("Left to select:" + (m_total - selected));

  }



  private void createEntries(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, IntegerMap<Unit> movement, List<Unit> defaultSelections)
  {
    Collection categories = UnitSeperator.categorize(units, dependent, movement);
    Collection defaultSelectionsCategorized = UnitSeperator.categorize(defaultSelections, dependent, movement);
    IntegerMap<UnitCategory> defaultValues = createDefaultSelectionsMap(defaultSelectionsCategorized);
    Iterator iter = categories.iterator();
    while(iter.hasNext())
    {
      UnitCategory category = (UnitCategory) iter.next();
      addCategory(category, defaultValues.getInt(category));
    }
  }

  private IntegerMap<UnitCategory> createDefaultSelectionsMap(Collection categories)
  {
    IntegerMap<UnitCategory> defaultValues = new IntegerMap<UnitCategory>();
    Iterator iter = categories.iterator();
    while (iter.hasNext()) {
      UnitCategory category = (UnitCategory) iter.next();
      int defaultValue = category.getUnits().size();
      defaultValues.put(category, defaultValue);
    }
    return defaultValues;
  }

  private void addCategory(UnitCategory category, int defaultValue)
  {
    ChooserEntry entry = new ChooserEntry(category, m_textFieldListener, m_data, m_allowTwoHit, defaultValue);
    m_entries.add(entry);

  }

  private void layoutEntries()
  {
    this.setLayout(new GridBagLayout());


    m_title = new JTextArea("Choose units");
    m_title.setBackground(this.getBackground());
    m_title.setEditable(false);
    //m_title.setColumns(15);
    m_title.setWrapStyleWord(true);
    Insets nullInsets = new Insets(0,0,0,0);

    Dimension buttonSize = new Dimension(80,20);

    m_selectNoneButton = new JButton("None");
    m_selectNoneButton.setPreferredSize(buttonSize);
    m_autoSelectButton = new JButton("All");
    m_autoSelectButton.setPreferredSize(buttonSize);

    add(m_title, new GridBagConstraints(0,0,7 ,1,0,0.5,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 )  );


    m_selectNoneButton.addActionListener
       (
       new ActionListener()
       {
         public void actionPerformed(ActionEvent e)
         {
           selectNone();
         }
       }
     );


    m_autoSelectButton.addActionListener
      (
      new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          autoSelect();
        }
      }
    );

    int yIndex = 1;
    Iterator<ChooserEntry> iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      entry.createComponents(this, yIndex);
      yIndex++;
    }

    add(m_autoSelectButton, new GridBagConstraints(0,yIndex,7 ,1,0,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE,nullInsets, 0,0 )  );
    yIndex++;
    add(m_leftToSelect, new GridBagConstraints(0,yIndex,5,2,0,0.5,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ));



  }


  public Collection<Unit> getSelected()
  {
    return getSelected(true);
  }

  /**
   * get the units selected.
   * If units are two hit enabled, returns those with two hits.
   */
  public List<Unit> getSelected(boolean selectDependents)
  {
    List<Unit> selectedUnits = new ArrayList<Unit>();

    Iterator<ChooserEntry> entries = m_entries.iterator();
    while(entries.hasNext())
    {
      ChooserEntry chooserEntry = entries.next();
      if(chooserEntry.isTwoHit())
          addToCollection(selectedUnits, chooserEntry,  chooserEntry.getSecondHits(), selectDependents);
      else
          addToCollection(selectedUnits, chooserEntry, chooserEntry.getFirstHits(), selectDependents);

    }
    return selectedUnits;
  }

  /**
   * Only applicable if this dialog was constructed using twoHits
   */
  public List<Unit> getSelectedFirstHit()
  {
      List<Unit> selectedUnits = new ArrayList<Unit>();

      Iterator<ChooserEntry> entries = m_entries.iterator();
      while(entries.hasNext())
      {
        ChooserEntry chooserEntry = entries.next();
        if(chooserEntry.isTwoHit())
            addToCollection(selectedUnits, chooserEntry, chooserEntry.getFirstHits(), false);
      }
      return selectedUnits;
  }

  private void selectNone()
  {
    Iterator<ChooserEntry> iter = m_entries.iterator();
    while (iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      entry.selectNone();
    }
  }

  private void autoSelect()
  {
    Iterator<ChooserEntry> iter = m_entries.iterator();
    while (iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      entry.selectAll();
    }
  }

  private void addToCollection(Collection<Unit> addTo, ChooserEntry entry, int quantity, boolean addDependents)
  {

    Collection<Unit> possible = entry.getCategory().getUnits();
    if(possible.size() < quantity)
      throw new IllegalStateException("Not enough units");

    Iterator<Unit> iter = possible.iterator();
    for(int i = 0; i < quantity; i++)
    {
      Unit current = (Unit) iter.next();
      addTo.add(current);
      if(addDependents)
      {
        Collection<Unit> dependents =  m_dependents.get(current);
        if(dependents != null)
          addTo.addAll(dependents);
      }
    }
  }

  private ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener()
  {
    public void changedValue(ScrollableTextField field)
    {
      updateLeft();
    }
  };
}

class ChooserEntry
{
  private final UnitCategory m_category;
  private ScrollableTextField m_hitText;
  private final ScrollableTextFieldListener m_hitTextFieldListener;
  private final GameData m_data;
  private final boolean m_hasSecondHit;

  private final int m_defaultValueFirstHits;
  private final int m_defaultValueSecondHits;

  private ScrollableTextField m_secondHitText;

  private JLabel m_secondHitLabel;
  private int m_leftToSelect = 0;
  private static Insets nullInsets = new Insets(0,0,0,0);


  ChooserEntry(UnitCategory category, ScrollableTextFieldListener listener, GameData data, boolean allowTwoHit, int defaultValue)
  {
    m_hitTextFieldListener = listener;
    m_data = data;
    m_category = category;
    m_hasSecondHit = allowTwoHit && category.isTwoHit() && ! category.getDamaged();
    int numUnits = category.getUnits().size();
    m_defaultValueFirstHits = numUnits < defaultValue ? numUnits : defaultValue;
    m_defaultValueSecondHits = numUnits < defaultValue ? defaultValue - numUnits : 0;
    //    System.out.println("Default hits: " + m_defaultValueFirstHits + " " + m_defaultValueSecondHits);
  }

  public void createComponents(JPanel panel, int yIndex)
  {

      panel.add(new UnitChooserEntryIcon(false) ,
                new GridBagConstraints(0,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ) );

      if (m_category.getMovement() != -1)
          panel.add(new JLabel("mvt " + m_category.getMovement()),
                new GridBagConstraints(1,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,4,0,4), 0,0 ) );

       panel.add(new JLabel("x" + m_category.getUnits().size()),
           new GridBagConstraints(2,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ) );

      m_hitText = new ScrollableTextField(0, m_category.getUnits().size());
      m_hitText.setValue(m_defaultValueFirstHits);
      m_hitText.addChangeListener(m_hitTextFieldListener);
      panel.add(m_hitText,
        new GridBagConstraints(3,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,4,0,0), 0,0 ) );


      if (m_hasSecondHit)
      {
          panel.add(new UnitChooserEntryIcon(true),
              new GridBagConstraints(4,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,8,0,0), 0,0 ) );

          m_secondHitLabel = new JLabel("x0");

          m_secondHitText = new ScrollableTextField(0, 0);
	  m_secondHitText.setValue(m_defaultValueSecondHits);
	  updateLeftToSelect();
          panel.add(m_secondHitLabel,
                    new GridBagConstraints(5,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,0,0,4), 0,0 ) );

          panel.add(m_secondHitText,
                    new GridBagConstraints(6,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ) );

          m_hitText.addChangeListener(
              new ScrollableTextFieldListener()
          {
              public void changedValue(ScrollableTextField field)
              {
                  m_secondHitLabel.setText("x" + field.getValue());
                  updateLeftToSelect();
              }
          }
          );

           m_secondHitText.addChangeListener(m_hitTextFieldListener);
      }
  }


  public UnitCategory getCategory()
  {
    return m_category;
  }

  public void selectAll()
  {
    m_hitText.setValue(m_hitText.getMax());
  }

  public void selectNone()
  {
    m_hitText.setValue(0);
  }

  public void setLeftToSelect(int leftToSelect)
  {
    m_leftToSelect = leftToSelect;
    updateLeftToSelect();

  }

  private void updateLeftToSelect()
  {
      int newMax = m_leftToSelect + m_hitText.getValue();

      m_hitText.setMax(Math.min(newMax, m_category.getUnits().size()));

      if (m_hasSecondHit)
      {
          int newSecondHitMax = m_leftToSelect + m_secondHitText.getValue();
          m_secondHitText.setMax(Math.min(newSecondHitMax, m_hitText.getValue()));
      }
  }


  public int getTotalHits()
  {
      return getFirstHits() + getSecondHits();
  }

  public int getFirstHits()
  {
      return m_hitText.getValue();
  }

  public int getSecondHits()
  {
      if(!m_hasSecondHit)
          return 0;
      return m_secondHitText.getValue();
  }

  public boolean isTwoHit()
  {
      return m_hasSecondHit;
  }

  class UnitChooserEntryIcon extends JComponent
  {
      private boolean m_forceDamaged;

    UnitChooserEntryIcon(boolean forceDamaged)
    {
        m_forceDamaged = forceDamaged;
    }



    public void paint(Graphics g)
    {
      super.paint(g);
      g.drawImage( UnitImageFactory.instance().getImage(m_category.getType(), m_category.getOwner(), m_data, m_forceDamaged || m_category.getDamaged()), 0,0,this);
      Iterator iter = m_category.getDependents().iterator();
      int index = 1;
      while(iter.hasNext())
      {
        UnitOwner holder = (UnitOwner) iter.next();
        int x = UnitImageFactory.instance().getUnitImageWidth() * index;
        Image unitImg = UnitImageFactory.instance().getImage(holder.getType(), holder.getOwner(), m_data, false);
        g.drawImage(unitImg, x, 0, this);


        index++;
      }
    }

    public int getWidth()
    {
      //we draw a unit symbol for each dependent
      return UnitImageFactory.instance().getUnitImageWidth() * (1 + m_category.getDependents().size());
    }

    public int getHeight()
    {
      return UnitImageFactory.instance().getUnitImageHeight();
    }

    public Dimension getMaximumSize()
    {
      return getDimension();
    }

    public Dimension getMinimumSize()
    {
      return getDimension();
    }

    public Dimension getPreferredSize()
    {
      return getDimension();
    }

    public Dimension getDimension()
    {
      return new Dimension(getWidth(), getHeight());
    }
  }
}

