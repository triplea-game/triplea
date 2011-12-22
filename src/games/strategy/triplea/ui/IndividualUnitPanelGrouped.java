package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * For when you want multiple individual unit panels, perhaps one for each territory, etc.
 * <p>
 * This lets you create multiple IndividualUnitPanel into a single panel, and have them integrated to use the same MAX.
 * 
 * IndividualUnitPanel is a group of units each displayed individually, and you can set an integer up to max for each unit.
 * 
 * @author Veqryn
 * 
 */
@SuppressWarnings("serial")
public class IndividualUnitPanelGrouped extends JPanel
{
	private int m_max = 0;
	private final boolean m_showMinAndMax;
	private final JTextArea m_title;
	private final GameData m_data;
	private final UIContext m_uiContext;
	private final Map<String, Collection<Unit>> m_unitsToChooseFrom;
	private final Collection<Tuple<String, IndividualUnitPanel>> m_entries = new ArrayList<Tuple<String, IndividualUnitPanel>>();
	private final JLabel m_leftToSelect = new JLabel();
	private JButton m_autoSelectButton;
	private JButton m_selectNoneButton;
	private final boolean m_showSelectAll;
	
	private final ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener()
	{
		public void changedValue(final ScrollableTextField field)
		{
			updateLeft();
		}
	};
	
	/**
	 * For when you want multiple individual unit panels, perhaps one for each territory, etc.
	 * This lets you create multiple IndividualUnitPanel into a single panel, and have them integrated to use the same MAX.
	 * IndividualUnitPanel is a group of units each displayed individually, and you can set an integer up to max for each unit.
	 * 
	 * @param unitsToChooseFrom
	 * @param data
	 * @param context
	 * @param title
	 * @param maxTotal
	 * @param showMinAndMax
	 * @param showSelectAll
	 */
	public IndividualUnitPanelGrouped(final Map<String, Collection<Unit>> unitsToChooseFrom, final GameData data, final UIContext context, final String title,
				final int maxTotal, final boolean showMinAndMax, final boolean showSelectAll)
	{
		m_data = data;
		m_uiContext = context;
		setMaxAndShowMaxButton(maxTotal);
		m_showMinAndMax = showMinAndMax;
		m_title = new JTextArea(title);
		m_title.setBackground(this.getBackground());
		m_title.setEditable(false);
		m_title.setWrapStyleWord(true);
		m_unitsToChooseFrom = unitsToChooseFrom;
		m_showSelectAll = showSelectAll;
		layoutEntries();
	}
	
	private void setMaxAndShowMaxButton(final int max)
	{
		m_max = max;
		m_textFieldListener.changedValue(null);
	}
	
	public void setTitle(final String title)
	{
		m_title.setText(title);
	}
	
	private void updateLeft()
	{
		if (m_max == -1)
			return;
		final int selected = getSelectedCount();
		final int newMax = m_max - selected;
		for (final Tuple<String, IndividualUnitPanel> entry : m_entries)
		{
			final int current = entry.getSecond().getSelectedCount();
			final int maxForThis = current + newMax;
			if (entry.getSecond().getMax() != maxForThis)
				entry.getSecond().setMaxAndUpdate(maxForThis);
		}
		m_leftToSelect.setText("Left to select:" + newMax);
	}
	
	protected int getSelectedCount()
	{
		int selected = 0;
		for (final Tuple<String, IndividualUnitPanel> entry : m_entries)
		{
			selected += entry.getSecond().getSelectedCount();
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
		final JPanel entries = new JPanel();
		entries.setLayout(new FlowLayout());
		entries.setBorder(BorderFactory.createEmptyBorder());
		for (final Entry<String, Collection<Unit>> entry : m_unitsToChooseFrom.entrySet())
		{
			final String miniTitle = entry.getKey();
			final Collection<Unit> possibleTargets = entry.getValue();
			final JPanel panelChooser = new JPanel();
			panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
			panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
			final JLabel chooserTitle = new JLabel("Choose Per Unit");
			chooserTitle.setHorizontalAlignment(JLabel.LEFT);
			chooserTitle.setFont(new Font("Arial", Font.BOLD, 12));
			panelChooser.add(chooserTitle);
			panelChooser.add(new JLabel(" "));
			final IndividualUnitPanel chooser = new IndividualUnitPanel(possibleTargets, miniTitle, m_data, m_uiContext, m_max, m_showMinAndMax, m_showSelectAll, m_textFieldListener);
			m_entries.add(new Tuple<String, IndividualUnitPanel>(miniTitle, chooser));
			panelChooser.add(chooser);
			final JScrollPane chooserScrollPane = new JScrollPane(panelChooser);
			chooserScrollPane.setMaximumSize(new Dimension(220, 520));
			chooserScrollPane.setPreferredSize(new Dimension(
						(chooserScrollPane.getPreferredSize().width > 220 ? 220 :
									(chooserScrollPane.getPreferredSize().height > 520 ? chooserScrollPane.getPreferredSize().width + 20 : chooserScrollPane.getPreferredSize().width)),
						(chooserScrollPane.getPreferredSize().height > 520 ? 520 :
									(chooserScrollPane.getPreferredSize().width > 220 ? chooserScrollPane.getPreferredSize().height + 20 : chooserScrollPane.getPreferredSize().height))));
			entries.add(chooserScrollPane);
		}
		int yIndex = 1;
		add(entries, new GridBagConstraints(0, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
		yIndex++;
		if (m_showSelectAll)
		{
			add(m_autoSelectButton, new GridBagConstraints(0, 2, 7, 1, 0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.NONE, nullInsets, 0, 0));
			yIndex++;
		}
		add(m_leftToSelect, new GridBagConstraints(0, 3, 5, 2, 0, 0.5, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
	}
	
	public Map<String, IntegerMap<Unit>> getSelected()
	{
		final HashMap<String, IntegerMap<Unit>> selectedUnits = new HashMap<String, IntegerMap<Unit>>();
		for (final Tuple<String, IndividualUnitPanel> entry : m_entries)
		{
			selectedUnits.put(entry.getFirst(), entry.getSecond().getSelected());
		}
		return selectedUnits;
	}
	
	protected void selectNone()
	{
		for (final Tuple<String, IndividualUnitPanel> entry : m_entries)
		{
			entry.getSecond().selectNone();
		}
	}
	
	protected void autoSelect()
	{
		for (final Tuple<String, IndividualUnitPanel> entry : m_entries)
		{
			entry.getSecond().autoSelect();
		}
	}
}
