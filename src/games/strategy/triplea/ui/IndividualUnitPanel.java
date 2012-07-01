package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * For when you do not want things condensed into categories.
 * <p>
 * This creates a panel which shows a group of units individually, and lets you put points/hits towards each unit individually.
 * 
 * It lets you set a max number of points total (though max per unit is not allowed yet). It can return an IntegerMap with the points per unit.
 * 
 * @author Veqryn
 * 
 */
public class IndividualUnitPanel extends JPanel
{
	private static final long serialVersionUID = -4222938655315991715L;
	private final List<SingleUnitPanel> m_entries = new ArrayList<SingleUnitPanel>();
	private final JTextArea m_title;
	private int m_max = -1;
	private final JLabel m_leftToSelect = new JLabel();
	private final GameData m_data;
	private JButton m_autoSelectButton;
	private JButton m_selectNoneButton;
	private final UIContext m_uiContext;
	private ScrollableTextField m_textFieldPurelyForListening;
	private final ScrollableTextFieldListener m_countOptionalTextFieldListener;
	private final boolean m_showSelectAll;
	
	private final ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener()
	{
		public void changedValue(final ScrollableTextField field)
		{
			updateLeft();
		}
	};
	
	/**
	 * For when you do not want things condensed into categories.
	 * This creates a panel which shows a group of units individually, and lets you put points/hits towards each unit individually.
	 * It lets you set a max number of points total (though max per unit is not allowed yet). It can return an IntegerMap with the points per unit.
	 * 
	 * @param units
	 * @param title
	 * @param data
	 * @param context
	 * @param max
	 * @param showMinAndMax
	 * @param showSelectAll
	 * @param optionalListener
	 */
	public IndividualUnitPanel(final Collection<Unit> units, final String title, final GameData data, final UIContext context, final int max,
				final boolean showMinAndMax, final boolean showSelectAll, final ScrollableTextFieldListener optionalListener)
	{
		m_data = data;
		m_uiContext = context;
		m_title = new JTextArea(title);
		m_title.setBackground(this.getBackground());
		m_title.setEditable(false);
		// m_title.setColumns(15);
		m_title.setWrapStyleWord(true);
		m_countOptionalTextFieldListener = optionalListener;
		setMaxAndShowMaxButton(max);
		m_showSelectAll = showSelectAll;
		for (final Unit u : units)
		{
			m_entries.add(new SingleUnitPanel(u, m_data, m_uiContext, m_textFieldListener, m_max, 0, showMinAndMax));
		}
		layoutEntries();
	}
	
	/**
	 * For when you do not want things condensed into categories.
	 * This creates a panel which shows a group of units individually, and lets you put points/hits towards each unit individually.
	 * It lets you set a max number of points total AND per unit. It can return an IntegerMap with the points per unit.
	 * 
	 * @param units
	 *            mapped to their individual max, then min, then current values
	 * @param title
	 * @param data
	 * @param context
	 * @param max
	 * @param showMinAndMax
	 * @param showSelectAll
	 * @param optionalListener
	 */
	public IndividualUnitPanel(final HashMap<Unit, Triple<Integer, Integer, Integer>> unitsAndTheirMaxMinAndCurrent, final String title, final GameData data, final UIContext context, final int max,
				final boolean showMinAndMax, final boolean showSelectAll, final ScrollableTextFieldListener optionalListener)
	{
		m_data = data;
		m_uiContext = context;
		m_title = new JTextArea(title);
		m_title.setBackground(this.getBackground());
		m_title.setEditable(false);
		// m_title.setColumns(15);
		m_title.setWrapStyleWord(true);
		m_countOptionalTextFieldListener = optionalListener;
		setMaxAndShowMaxButton(max);
		m_showSelectAll = showSelectAll;
		for (final Entry<Unit, Triple<Integer, Integer, Integer>> entry : unitsAndTheirMaxMinAndCurrent.entrySet())
		{
			final int unitMax = entry.getValue().getFirst();
			int thisMax;
			if (m_max < 0 && unitMax < 0)
				thisMax = -1;
			else if (unitMax < 0)
				thisMax = m_max;
			else if (m_max < 0)
				thisMax = unitMax;
			else
				thisMax = Math.min(m_max, unitMax);
			final int thisMin = Math.max(0, entry.getValue().getSecond());
			final int thisCurrent = Math.max(thisMin, Math.min(thisMax, entry.getValue().getThird()));
			m_entries.add(new SingleUnitPanel(entry.getKey(), m_data, m_uiContext, m_textFieldListener, thisMax, thisMin, thisCurrent, showMinAndMax));
		}
		layoutEntries();
	}
	
	private void setMaxAndShowMaxButton(final int max)
	{
		m_max = max;
		m_textFieldPurelyForListening = new ScrollableTextField(0, 0);
		m_textFieldListener.changedValue(null);
		if (m_countOptionalTextFieldListener != null)
			m_textFieldPurelyForListening.addChangeListener(m_countOptionalTextFieldListener);
	}
	
	public void setTitle(final String title)
	{
		m_title.setText(title);
	}
	
	public int getMax()
	{
		return m_max;
	}
	
	public void setMaxAndUpdate(final int newMax)
	{
		m_max = newMax;
		updateLeft();
		m_textFieldPurelyForListening.setValue(0);
	}
	
	private void updateLeft()
	{
		if (m_max == -1)
			return;
		final int selected = getSelectedCount();
		final int newMax = m_max - selected;
		for (final SingleUnitPanel entry : m_entries)
		{
			final int current = entry.getCount();
			final int maxForThis = current + newMax;
			entry.setMax(maxForThis);
		}
		m_leftToSelect.setText("Left to select:" + newMax);
		m_textFieldPurelyForListening.setValue(0);
	}
	
	protected int getSelectedCount()
	{
		int selected = 0;
		for (final SingleUnitPanel entry : m_entries)
		{
			selected += entry.getCount();
		}
		return selected;
	}
	
	private void layoutEntries()
	{
		this.setLayout(new GridBagLayout());
		final Insets nullInsets = new Insets(0, 0, 0, 0);
		final Dimension buttonSize = new Dimension(80, 20);
		m_selectNoneButton = new JButton("None");
		m_selectNoneButton.setPreferredSize(buttonSize);
		m_autoSelectButton = new JButton("Max");
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
		for (final SingleUnitPanel entry : m_entries)
		{
			entry.createComponents(this, yIndex);
			yIndex++;
		}
		if (m_showSelectAll)
		{
			add(m_autoSelectButton, new GridBagConstraints(0, yIndex, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.NONE, nullInsets, 0, 0));
			yIndex++;
		}
		add(m_leftToSelect, new GridBagConstraints(0, yIndex, 5, 2, 0, 0.5, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
	}
	
	public IntegerMap<Unit> getSelected()
	{
		final IntegerMap<Unit> selectedUnits = new IntegerMap<Unit>();
		for (final SingleUnitPanel entry : m_entries)
		{
			selectedUnits.put(entry.getUnit(), entry.getCount());
		}
		return selectedUnits;
	}
	
	protected void selectNone()
	{
		for (final SingleUnitPanel entry : m_entries)
		{
			entry.selectNone();
		}
	}
	
	protected void autoSelect()
	{
		if (m_max == -1)
		{
			for (final SingleUnitPanel entry : m_entries)
			{
				entry.selectAll();
			}
		}
		else
		{
			int leftToSelect = m_max - getSelectedCount();
			for (final SingleUnitPanel entry : m_entries)
			{
				final int leftToSelectForCurrent = leftToSelect + entry.getCount();
				final int canSelect = entry.getMax();
				if (leftToSelectForCurrent >= canSelect)
				{
					entry.selectAll();
					leftToSelect -= canSelect;
				}
				else
				{
					entry.setCount(leftToSelectForCurrent);
					leftToSelect = 0;
					break;
				}
			}
		}
	}
}


class SingleUnitPanel extends JPanel
{
	private static final long serialVersionUID = 5034287842323633030L;
	private final Unit m_unit;
	private final UIContext m_context;
	private final ScrollableTextField m_textField;
	private final GameData m_data;
	private static Insets nullInsets = new Insets(0, 0, 0, 0);
	private final ScrollableTextFieldListener m_countTextFieldListener;
	
	public SingleUnitPanel(final Unit unit, final GameData data, final UIContext context, final ScrollableTextFieldListener textFieldListener, final int max, final int min, final boolean showMaxAndMin)
	{
		this(unit, data, context, textFieldListener, max, min, 0, showMaxAndMin);
	}
	
	public SingleUnitPanel(final Unit unit, final GameData data, final UIContext context, final ScrollableTextFieldListener textFieldListener, final int max, final int min, final int currentValue,
				final boolean showMaxAndMin)
	{
		m_unit = unit;
		m_data = data;
		m_context = context;
		m_countTextFieldListener = textFieldListener;
		m_textField = new ScrollableTextField(0, 512);
		if (max >= 0)
			setMax(max);
		setMin(min);
		m_textField.setShowMaxAndMin(showMaxAndMin);
		final TripleAUnit taUnit = TripleAUnit.get(unit);
		final Image img = m_context.getUnitImageFactory().getImage(m_unit.getType(), m_unit.getOwner(), m_data, taUnit.getUnitDamage() > 0, taUnit.getDisabled());
		setCount(currentValue);
		setLayout(new GridBagLayout());
		final JLabel label = new JLabel(new ImageIcon(img));
		add(label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
		add(m_textField, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
	}
	
	public int getCount()
	{
		return m_textField.getValue();
	}
	
	public void setCount(final int value)
	{
		m_textField.setValue(value);
	}
	
	public void selectAll()
	{
		m_textField.setValue(m_textField.getMax());
	}
	
	public void selectNone()
	{
		m_textField.setValue(0);
	}
	
	public void setMax(final int value)
	{
		m_textField.setMax(value);
	}
	
	public int getMax()
	{
		return m_textField.getMax();
	}
	
	public void setMin(final int value)
	{
		m_textField.setMin(value);
	}
	
	public Unit getUnit()
	{
		return m_unit;
	}
	
	public void createComponents(final JPanel panel, final int yIndex)
	{
		panel.add(this, new GridBagConstraints(0, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
		m_textField.addChangeListener(m_countTextFieldListener);
	}
}
