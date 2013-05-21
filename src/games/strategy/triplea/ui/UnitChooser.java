/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * UnitChooser.java
 * 
 * Created on December 3, 2001, 7:32 PM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class UnitChooser extends JPanel
{
	private static final long serialVersionUID = -4667032237550267682L;
	private final List<ChooserEntry> m_entries = new ArrayList<ChooserEntry>();
	private final Map<Unit, Collection<Unit>> m_dependents;
	private JTextArea m_title;
	private int m_total = -1;
	private final JLabel m_leftToSelect = new JLabel();
	private final GameData m_data;
	private boolean m_allowTwoHit = false;
	private JButton m_autoSelectButton;
	private JButton m_selectNoneButton;
	private final IUIContext m_uiContext;
	private final Match<Collection<Unit>> m_match;
	
	/** Creates new UnitChooser */
	public UnitChooser(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement, final boolean categorizeTransportCost, final GameData data,
				final IUIContext context)
	{
		m_dependents = dependent;
		m_data = data;
		m_uiContext = context;
		m_match = null;
		createEntries(units, dependent, categorizeMovement, categorizeTransportCost, Collections.<Unit> emptyList());
		layoutEntries();
	}
	
	public UnitChooser(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement, final boolean categorizeTransportCost, final GameData data,
				final IUIContext context, final Match<Collection<Unit>> match)
	{
		m_match = match;
		m_dependents = dependent;
		m_data = data;
		m_uiContext = context;
		createEntries(units, dependent, categorizeMovement, categorizeTransportCost, Collections.<Unit> emptyList());
		layoutEntries();
	}
	
	public UnitChooser(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowTwoHit, final IUIContext uiContext)
	{
		this(units, Collections.<Unit> emptyList(), dependent, data, allowTwoHit, uiContext);
	}
	
	public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections, final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowTwoHit,
				final IUIContext uiContext)
	{
		this(units, defaultSelections, dependent, false, false, data, allowTwoHit, uiContext);
	}
	
	public UnitChooser(final Collection<Unit> units, final CasualtyList defaultSelections, final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowTwoHit,
				final IUIContext uiContext)
	{
		m_dependents = dependent;
		m_data = data;
		m_allowTwoHit = allowTwoHit;
		m_uiContext = uiContext;
		m_match = null;
		final List<Unit> combinedList = defaultSelections.getDamaged();
		combinedList.addAll(defaultSelections.getKilled());
		createEntries(units, dependent, false, false, combinedList);
		layoutEntries();
	}
	
	public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections, final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
				final boolean categorizeTransportCost, final GameData data, final boolean allowTwoHit, final IUIContext uiContext)
	{
		m_dependents = dependent;
		m_data = data;
		m_allowTwoHit = allowTwoHit;
		m_uiContext = uiContext;
		m_match = null;
		createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
		layoutEntries();
	}
	
	public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections, final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
				final boolean categorizeTransportCost, final GameData data, final boolean allowTwoHit, final IUIContext uiContext, final Match<Collection<Unit>> match)
	{
		m_dependents = dependent;
		m_data = data;
		m_allowTwoHit = allowTwoHit;
		m_uiContext = uiContext;
		m_match = match;
		createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
		layoutEntries();
	}
	
	public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections, final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
				final boolean categorizeTransportCost, final boolean categorizeTerritories, final GameData data, final boolean allowTwoHit, final IUIContext uiContext,
				final Match<Collection<Unit>> match)
	{
		m_dependents = dependent;
		m_data = data;
		m_allowTwoHit = allowTwoHit;
		m_uiContext = uiContext;
		m_match = match;
		createEntries(units, dependent, categorizeMovement, categorizeTransportCost, categorizeTerritories, defaultSelections);
		layoutEntries();
	}
	
	/**
	 * Set the maximum number of units that we can choose.
	 */
	public void setMax(final int max)
	{
		m_total = max;
		m_textFieldListener.changedValue(null);
		m_autoSelectButton.setVisible(false);
		m_selectNoneButton.setVisible(false);
	}
	
	public void setMaxAndShowMaxButton(final int max)
	{
		m_total = max;
		m_textFieldListener.changedValue(null);
		m_autoSelectButton.setText("Max");
	}
	
	public void setTitle(final String title)
	{
		m_title.setText(title);
	}
	
	private void updateLeft()
	{
		if (m_total == -1)
			return;
		Iterator<ChooserEntry> iter;
		final int selected = getSelectedCount();
		m_leftToSelect.setText("Left to select:" + (m_total - selected));
		iter = m_entries.iterator();
		while (iter.hasNext())
		{
			final ChooserEntry entry = iter.next();
			entry.setLeftToSelect(m_total - selected);
		}
		m_leftToSelect.setText("Left to select:" + (m_total - selected));
	}
	
	private void checkMatches()
	{
		final Collection<Unit> allSelectedUnits = new ArrayList<Unit>();
		for (final ChooserEntry entry : m_entries)
			addToCollection(allSelectedUnits, entry, entry.getTotalHits(), false);
		// check match against each scroll button
		for (final ChooserEntry entry : m_entries)
		{
			final Collection<Unit> newSelectedUnits = new ArrayList<Unit>(allSelectedUnits);
			final int totalHits = entry.getTotalHits();
			final int totalUnits = entry.getCategory().getUnits().size();
			int leftToSelect = 0;
			final Iterator<Unit> unitIter = entry.getCategory().getUnits().iterator();
			for (int i = 1; i <= totalUnits; i++)
			{
				final Unit unit = unitIter.next();
				if (i > totalHits)
					newSelectedUnits.add(unit);
				if (i >= totalHits)
				{
					if (m_match.match(newSelectedUnits))
						leftToSelect = i - totalHits;
					else
						break;
				}
			}
			entry.setLeftToSelect(leftToSelect);
		}
	}
	
	private int getSelectedCount()
	{
		int selected = 0;
		for (final ChooserEntry entry : m_entries)
		{
			selected += entry.getTotalHits();
		}
		return selected;
	}
	
	private void createEntries(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement, final boolean categorizeTransportCost,
				final Collection<Unit> defaultSelections)
	{
		final Collection<UnitCategory> categories = UnitSeperator.categorize(units, dependent, categorizeMovement, categorizeTransportCost);
		final Collection<UnitCategory> defaultSelectionsCategorized = UnitSeperator.categorize(defaultSelections, dependent, categorizeMovement, categorizeTransportCost);
		final IntegerMap<UnitCategory> defaultValues = createDefaultSelectionsMap(defaultSelectionsCategorized);
		for (final UnitCategory category : categories)
		{
			addCategory(category, defaultValues.getInt(category));
		}
	}
	
	private void createEntries(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement, final boolean categorizeTransportCost,
				final boolean categorizeTerritories, final Collection<Unit> defaultSelections)
	{
		final Collection<UnitCategory> categories = UnitSeperator.categorize(dependent, units, categorizeMovement, categorizeTransportCost, categorizeTerritories);
		final Collection<UnitCategory> defaultSelectionsCategorized = UnitSeperator.categorize(defaultSelections, dependent, categorizeMovement, categorizeTransportCost);
		final IntegerMap<UnitCategory> defaultValues = createDefaultSelectionsMap(defaultSelectionsCategorized);
		for (final UnitCategory category : categories)
		{
			addCategory(category, defaultValues.getInt(category));
		}
	}
	
	private IntegerMap<UnitCategory> createDefaultSelectionsMap(final Collection<UnitCategory> categories)
	{
		final IntegerMap<UnitCategory> defaultValues = new IntegerMap<UnitCategory>();
		for (final UnitCategory category : categories)
		{
			final int defaultValue = category.getUnits().size();
			defaultValues.put(category, defaultValue);
		}
		return defaultValues;
	}
	
	private void addCategory(final UnitCategory category, final int defaultValue)
	{
		final ChooserEntry entry = new ChooserEntry(category, m_textFieldListener, m_data, m_allowTwoHit, defaultValue, m_uiContext);
		m_entries.add(entry);
	}
	
	private void layoutEntries()
	{
		this.setLayout(new GridBagLayout());
		m_title = new JTextArea("Choose units");
		m_title.setBackground(this.getBackground());
		m_title.setEditable(false);
		// m_title.setColumns(15);
		m_title.setWrapStyleWord(true);
		final Insets nullInsets = new Insets(0, 0, 0, 0);
		final Dimension buttonSize = new Dimension(80, 20);
		m_selectNoneButton = new JButton("None");
		m_selectNoneButton.setPreferredSize(buttonSize);
		m_autoSelectButton = new JButton("All");
		m_autoSelectButton.setPreferredSize(buttonSize);
		add(m_title, new GridBagConstraints(0, 0, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
		m_selectNoneButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				selectNone();
			}
		});
		m_autoSelectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				autoSelect();
			}
		});
		int yIndex = 1;
		for (final ChooserEntry entry : m_entries)
		{
			entry.createComponents(this, yIndex);
			yIndex++;
		}
		add(m_autoSelectButton, new GridBagConstraints(0, yIndex, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.NONE, nullInsets, 0, 0));
		yIndex++;
		add(m_leftToSelect, new GridBagConstraints(0, yIndex, 5, 2, 0, 0.5, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
		if (m_match != null)
		{
			m_autoSelectButton.setVisible(false);
			m_selectNoneButton.setVisible(false);
			checkMatches();
		}
	}
	
	public Collection<Unit> getSelected()
	{
		return getSelected(true);
	}
	
	/**
	 * get the units selected.
	 * If units are two hit enabled, returns those with two hits.
	 */
	public List<Unit> getSelected(final boolean selectDependents)
	{
		final List<Unit> selectedUnits = new ArrayList<Unit>();
		for (final ChooserEntry entry : m_entries)
		{
			if (entry.isTwoHit())
				addToCollection(selectedUnits, entry, entry.getSecondHits(), selectDependents);
			else
				addToCollection(selectedUnits, entry, entry.getFirstHits(), selectDependents);
		}
		return selectedUnits;
	}
	
	/**
	 * Only applicable if this dialog was constructed using twoHits
	 */
	public List<Unit> getSelectedFirstHit()
	{
		final List<Unit> selectedUnits = new ArrayList<Unit>();
		final Iterator<ChooserEntry> entries = m_entries.iterator();
		while (entries.hasNext())
		{
			final ChooserEntry chooserEntry = entries.next();
			if (chooserEntry.isTwoHit())
				addToCollection(selectedUnits, chooserEntry, chooserEntry.getFirstHits(), false);
		}
		return selectedUnits;
	}
	
	private void selectNone()
	{
		for (final ChooserEntry entry : m_entries)
		{
			entry.selectNone();
		}
	}
	
	private void autoSelect()
	{
		if (m_total == -1)
		{
			for (final ChooserEntry entry : m_entries)
			{
				entry.selectAll();
			}
		}
		else
		{
			int leftToSelect = m_total - getSelectedCount();
			for (final ChooserEntry entry : m_entries)
			{
				final int canSelect = entry.getMax() - entry.getFirstHits();
				if (leftToSelect >= canSelect)
				{
					entry.selectAll();
					leftToSelect -= canSelect;
				}
				else
				{
					entry.set(entry.getFirstHits() + canSelect);
					leftToSelect = 0;
					break;
				}
			}
		}
	}
	
	private void addToCollection(final Collection<Unit> addTo, final ChooserEntry entry, final int quantity, final boolean addDependents)
	{
		final Collection<Unit> possible = entry.getCategory().getUnits();
		if (possible.size() < quantity)
			throw new IllegalStateException("Not enough units");
		final Iterator<Unit> iter = possible.iterator();
		for (int i = 0; i < quantity; i++)
		{
			final Unit current = iter.next();
			addTo.add(current);
			if (addDependents)
			{
				final Collection<Unit> dependents = m_dependents.get(current);
				if (dependents != null)
					addTo.addAll(dependents);
			}
		}
	}
	
	private final ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener()
	{
		public void changedValue(final ScrollableTextField field)
		{
			if (m_match != null)
				checkMatches();
			else
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
	private static Insets nullInsets = new Insets(0, 0, 0, 0);
	private final IUIContext m_uiContext;
	
	ChooserEntry(final UnitCategory category, final ScrollableTextFieldListener listener, final GameData data, final boolean allowTwoHit, final int defaultValue, final IUIContext uiContext)
	{
		m_hitTextFieldListener = listener;
		m_data = data;
		m_category = category;
		m_hasSecondHit = allowTwoHit && category.isTwoHit() && !category.getDamaged();
		final int numUnits = category.getUnits().size();
		m_defaultValueFirstHits = numUnits < defaultValue ? numUnits : defaultValue;
		m_defaultValueSecondHits = numUnits < defaultValue ? defaultValue - numUnits : 0;
		m_uiContext = uiContext;
		// System.out.println("Default hits: " + m_defaultValueFirstHits + " " + m_defaultValueSecondHits);
	}
	
	public void createComponents(final JPanel panel, final int yIndex)
	{
		panel.add(new UnitChooserEntryIcon(false, m_uiContext), new GridBagConstraints(0, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
		if (m_category.getMovement() != -1)
			panel.add(new JLabel("mvt " + m_category.getMovement()),
						new GridBagConstraints(1, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4), 0, 0));
		if (m_category.getTransportCost() != -1)
			panel.add(new JLabel("cst " + m_category.getTransportCost()), new GridBagConstraints(1, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4),
						0, 0));
		panel.add(new JLabel("x" + m_category.getUnits().size()), new GridBagConstraints(2, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
		m_hitText = new ScrollableTextField(0, m_category.getUnits().size());
		m_hitText.setValue(m_defaultValueFirstHits);
		m_hitText.addChangeListener(m_hitTextFieldListener);
		panel.add(m_hitText, new GridBagConstraints(3, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0));
		if (m_hasSecondHit)
		{
			panel.add(new UnitChooserEntryIcon(true, m_uiContext), new GridBagConstraints(4, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0));
			m_secondHitLabel = new JLabel("x0");
			m_secondHitText = new ScrollableTextField(0, 0);
			m_secondHitText.setValue(m_defaultValueSecondHits);
			updateLeftToSelect();
			panel.add(m_secondHitLabel, new GridBagConstraints(5, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 4), 0, 0));
			panel.add(m_secondHitText, new GridBagConstraints(6, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
			m_hitText.addChangeListener(new ScrollableTextFieldListener()
			{
				public void changedValue(final ScrollableTextField field)
				{
					m_secondHitLabel.setText("x" + field.getValue());
					updateLeftToSelect();
				}
			});
			m_secondHitText.addChangeListener(m_hitTextFieldListener);
		}
	}
	
	public int getMax()
	{
		return m_hitText.getMax();
	}
	
	public void set(final int value)
	{
		m_hitText.setValue(value);
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
	
	public void setLeftToSelect(final int leftToSelect)
	{
		m_leftToSelect = leftToSelect;
		updateLeftToSelect();
	}
	
	private void updateLeftToSelect()
	{
		final int newMax = m_leftToSelect + m_hitText.getValue();
		m_hitText.setMax(Math.min(newMax, m_category.getUnits().size()));
		if (m_hasSecondHit)
		{
			final int newSecondHitMax = m_leftToSelect + m_secondHitText.getValue();
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
		if (!m_hasSecondHit)
			return 0;
		return m_secondHitText.getValue();
	}
	
	public boolean isTwoHit()
	{
		return m_hasSecondHit;
	}
	
	
	class UnitChooserEntryIcon extends JComponent
	{
		private static final long serialVersionUID = 591598594559651745L;
		private final boolean m_forceDamaged;
		private final IUIContext m_uiContext;
		
		UnitChooserEntryIcon(final boolean forceDamaged, final IUIContext uiContext)
		{
			m_forceDamaged = forceDamaged;
			m_uiContext = uiContext;
		}
		
		@Override
		public void paint(final Graphics g)
		{
			super.paint(g);
			g.drawImage(m_uiContext.getUnitImageFactory().getImage(m_category.getType(), m_category.getOwner(), m_data, m_forceDamaged || m_category.getDamaged(), m_category.getDisabled()), 0, 0,
						this);
			final Iterator<UnitOwner> iter = m_category.getDependents().iterator();
			int index = 1;
			while (iter.hasNext())
			{
				final UnitOwner holder = iter.next();
				final int x = m_uiContext.getUnitImageFactory().getUnitImageWidth() * index;
				final Image unitImg = m_uiContext.getUnitImageFactory().getImage(holder.getType(), holder.getOwner(), m_data, false, false);
				g.drawImage(unitImg, x, 0, this);
				index++;
			}
		}
		
		@Override
		public int getWidth()
		{
			// we draw a unit symbol for each dependent
			return m_uiContext.getUnitImageFactory().getUnitImageWidth() * (1 + m_category.getDependents().size());
		}
		
		@Override
		public int getHeight()
		{
			return m_uiContext.getUnitImageFactory().getUnitImageHeight();
		}
		
		@Override
		public Dimension getMaximumSize()
		{
			return getDimension();
		}
		
		@Override
		public Dimension getMinimumSize()
		{
			return getDimension();
		}
		
		@Override
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
