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
	protected List<AbstractUndoableMove> m_moves;
	protected final GameData m_data;
	protected final AbstractMovePanel m_movePanel;
	protected JScrollPane scroll;
	protected Integer scrollBarPreviousValue = null;
	
	public AbstractUndoableMovesPanel(GameData data, AbstractMovePanel movePanel)
	{
		m_data = data;
		m_movePanel = movePanel;
		m_moves = Collections.emptyList();
	}
	
	public void setMoves(List moves)
	{
		m_moves = moves;
		SwingUtilities.invokeLater(new Runnable()
		{
			
			@Override
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
		
		JPanel items = new JPanel();
		
		items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
		
		// we want the newest move at the top
		m_moves = new ArrayList<AbstractUndoableMove>(m_moves);
		Collections.reverse(m_moves);
		Iterator<AbstractUndoableMove> iter = m_moves.iterator();
		
		if (iter.hasNext())
			add(new JLabel("Moves:"), BorderLayout.NORTH);
		
		int scrollIncrement = 10;
		Dimension seperatorSize = new Dimension(150, 20);
		while (iter.hasNext())
		{
			
			AbstractUndoableMove item = iter.next();
			JComponent moveComponent = createComponentForMove(item);
			scrollIncrement = moveComponent.getPreferredSize().height;
			
			items.add(moveComponent);
			
			if (iter.hasNext())
			{
				JSeparator seperator = new JSeparator(SwingConstants.HORIZONTAL);
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
			
			@Override
			public void run()
			{
				validate();
			}
			
		});
	}
	
	private JComponent createComponentForMove(AbstractUndoableMove move)
	{
		Box unitsBox = new Box(BoxLayout.X_AXIS);
		unitsBox.add(new JLabel((move.getIndex() + 1) + ") "));
		Collection<UnitCategory> unitCategories = UnitSeperator.categorize(move.getUnits());
		Iterator<UnitCategory> iter = unitCategories.iterator();
		Dimension buttonSize = new Dimension(80, 22);
		while (iter.hasNext())
		{
			UnitCategory category = iter.next();
			Icon icon = m_movePanel.getMap().getUIContext().getUnitImageFactory().getIcon(category.getType(), category.getOwner(), m_data, category.getDamaged(), category.getDisabled());
			JLabel label = new JLabel("x" + category.getUnits().size() + " ", icon, SwingConstants.LEFT);
			unitsBox.add(label);
		}
		
		unitsBox.add(Box.createHorizontalGlue());
		
		JLabel text = new JLabel(move.getMoveLabel());
		Box textBox = new Box(BoxLayout.X_AXIS);
		textBox.add(text);
		textBox.add(Box.createHorizontalGlue());
		
		JButton cancelButton = new JButton(new UndoMoveAction(move.getIndex()));
		setSize(buttonSize, cancelButton);
		
		JButton viewbutton = new JButton(new ViewAction(move));
		setSize(buttonSize, viewbutton);
		
		Box buttonsBox = new Box(BoxLayout.X_AXIS);
		buttonsBox.add(viewbutton);
		buttonsBox.add(cancelButton);
		buttonsBox.add(Box.createHorizontalGlue());
		
		Box rVal = new Box(BoxLayout.Y_AXIS);
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
	
	protected void setSize(Dimension buttonSize, JButton cancelButton)
	{
		cancelButton.setMinimumSize(buttonSize);
		cancelButton.setPreferredSize(buttonSize);
		cancelButton.setMaximumSize(buttonSize);
	}
	
	
	class UndoMoveAction extends AbstractAction
	{
		private final int m_moveIndex;
		
		public UndoMoveAction(int index)
		{
			super("Undo");
			m_moveIndex = index;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			// Record position of scroll bar as percentage.
			scrollBarPreviousValue = scroll.getVerticalScrollBar().getValue();
			m_movePanel.undoMove(m_moveIndex);
		}
	}
	

	class ViewAction extends AbstractAction
	{
		private final AbstractUndoableMove m_move;
		
		public ViewAction(AbstractUndoableMove move)
		{
			super("Show");
			m_move = move;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			m_movePanel.cancelMove();
			if (!m_movePanel.getMap().isShowing(m_move.getEnd()))
				m_movePanel.getMap().centerOn(m_move.getEnd());
			specificViewAction(m_move);
		}
	}
	
	protected void specificViewAction(AbstractUndoableMove move)
	{
		// do nothing if not overwritten in child class
	}
	
}
