package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.AbstractUndoableMove;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

abstract public class AbstractUndoableMovesPanel extends JPanel
{
	private static final long serialVersionUID = 1910945925958952416L;
	protected List<AbstractUndoableMove> m_moves;
	protected final GameData m_data;
	protected final AbstractMovePanel m_movePanel;
	protected JScrollPane scroll;
	protected Integer scrollBarPreviousValue = null;
	
	public AbstractUndoableMovesPanel(final GameData data, final AbstractMovePanel movePanel)
	{
		m_data = data;
		m_movePanel = movePanel;
		m_moves = Collections.emptyList();
	}
	
	public void setMoves(final List moves)
	{
		m_moves = moves;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				initLayout();
			}
		});
	}
	
	private void initLayout()
	{
		removeAll();
		setLayout(new BorderLayout());
		final JPanel items = new JPanel();
		items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
		// we want the newest move at the top
		m_moves = new ArrayList<AbstractUndoableMove>(m_moves);
		Collections.reverse(m_moves);
		final Iterator<AbstractUndoableMove> iter = m_moves.iterator();
		if (iter.hasNext())
			add(new JLabel((this instanceof UndoablePlacementsPanel) ? "Placements:" : "Moves:"), BorderLayout.NORTH);
		int scrollIncrement = 10;
		final Dimension seperatorSize = new Dimension(150, 20);
		while (iter.hasNext())
		{
			final AbstractUndoableMove item = iter.next();
			final JComponent moveComponent = createComponentForMove(item);
			scrollIncrement = moveComponent.getPreferredSize().height;
			items.add(moveComponent);
			if (iter.hasNext())
			{
				final JSeparator seperator = new JSeparator(SwingConstants.HORIZONTAL);
				seperator.setPreferredSize(seperatorSize);
				seperator.setMaximumSize(seperatorSize);
				items.add(seperator);
			}
		}
		// JScrollPane scroll = new JScrollPane(items);
		scroll = new JScrollPane(items);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(scrollIncrement);
		if (scrollBarPreviousValue != null)
		{
			scroll.getVerticalScrollBar().setValue(scrollBarPreviousValue);
			scrollBarPreviousValue = null;
		}
		add(scroll, BorderLayout.CENTER);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				validate();
			}
		});
	}
	
	private JComponent createComponentForMove(final AbstractUndoableMove move)
	{
		final Box unitsBox = new Box(BoxLayout.X_AXIS);
		unitsBox.add(new JLabel((move.getIndex() + 1) + ") "));
		final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(move.getUnits());
		final Iterator<UnitCategory> iter = unitCategories.iterator();
		final Dimension buttonSize = new Dimension(80, 22);
		while (iter.hasNext())
		{
			final UnitCategory category = iter.next();
			final Icon icon = m_movePanel.getMap().getUIContext().getUnitImageFactory().getIcon(category.getType(), category.getOwner(), m_data, category.getDamaged(), category.getDisabled());
			final JLabel label = new JLabel("x" + category.getUnits().size() + " ", icon, SwingConstants.LEFT);
			unitsBox.add(label);
		}
		unitsBox.add(Box.createHorizontalGlue());
		final JLabel text = new JLabel(move.getMoveLabel());
		final Box textBox = new Box(BoxLayout.X_AXIS);
		textBox.add(text);
		textBox.add(Box.createHorizontalGlue());
		final JButton cancelButton = new JButton(new UndoMoveAction(move.getIndex()));
		setSize(buttonSize, cancelButton);
		final JButton viewbutton = new JButton(new ViewAction(move));
		setSize(buttonSize, viewbutton);
		final Box buttonsBox = new Box(BoxLayout.X_AXIS);
		buttonsBox.add(viewbutton);
		buttonsBox.add(cancelButton);
		buttonsBox.add(Box.createHorizontalGlue());
		final Box rVal = new Box(BoxLayout.Y_AXIS);
		rVal.add(unitsBox);
		rVal.add(textBox);
		rVal.add(buttonsBox);
		rVal.add(new JLabel(" "));
		return rVal;
	}
	
	public int getCountOfMovesMade()
	{
		return m_moves.size();
	}
	
	protected void setSize(final Dimension buttonSize, final JButton cancelButton)
	{
		cancelButton.setMinimumSize(buttonSize);
		cancelButton.setPreferredSize(buttonSize);
		cancelButton.setMaximumSize(buttonSize);
	}
	
	
	class UndoMoveAction extends AbstractAction
	{
		private static final long serialVersionUID = -397312652244693138L;
		private final int m_moveIndex;
		
		public UndoMoveAction(final int index)
		{
			super("Undo");
			m_moveIndex = index;
		}
		
		public void actionPerformed(final ActionEvent e)
		{
			// Record position of scroll bar as percentage.
			scrollBarPreviousValue = scroll.getVerticalScrollBar().getValue();
			m_movePanel.undoMove(m_moveIndex);
		}
	}
	

	class ViewAction extends AbstractAction
	{
		private static final long serialVersionUID = -6999284663802575467L;
		private final AbstractUndoableMove m_move;
		
		public ViewAction(final AbstractUndoableMove move)
		{
			super("Show");
			m_move = move;
		}
		
		public void actionPerformed(final ActionEvent e)
		{
			m_movePanel.cancelMove();
			if (!m_movePanel.getMap().isShowing(m_move.getEnd()))
				m_movePanel.getMap().centerOn(m_move.getEnd());
			specificViewAction(m_move);
		}
	}
	
	protected void specificViewAction(final AbstractUndoableMove move)
	{
		// do nothing if not overwritten in child class
	}
}
