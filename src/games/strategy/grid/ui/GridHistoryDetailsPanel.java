package games.strategy.grid.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.triplea.ui.history.IHistoryDetailsPanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * 
 * @author veqryn
 * 
 */
public class GridHistoryDetailsPanel extends JPanel implements IHistoryDetailsPanel
{
	private static final long serialVersionUID = 7703367732080472172L;
	protected final GameData m_data;
	protected final JTextArea m_title = new JTextArea();
	protected final JScrollPane m_scroll = new JScrollPane(m_title);
	protected final GridMapPanel m_mapPanel;
	
	public GridHistoryDetailsPanel(final GameData data, final GridMapPanel mapPanel)
	{
		m_data = data;
		setLayout(new GridBagLayout());
		m_title.setWrapStyleWord(true);
		m_title.setBackground(this.getBackground());
		m_title.setLineWrap(true);
		m_title.setBorder(null);
		m_title.setEditable(false);
		m_scroll.setBorder(null);
		m_mapPanel = mapPanel;
	}
	
	public void render(final HistoryNode node)
	{
		removeAll();
		final Insets insets = new Insets(5, 0, 0, 0);
		m_title.setText(node.getTitle());
		add(m_scroll, new GridBagConstraints(0, 0, 1, 1, 1, 0.1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
		final GridBagConstraints mainConstraints = new GridBagConstraints(0, 1, 1, 1, 1, 0.9, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0);
		if (node instanceof Renderable)
		{
			final Object details = ((Renderable) node).getRenderingData();
			if (details instanceof Collection)
			{
				@SuppressWarnings("unchecked")
				final Collection<Object> objects = (Collection<Object>) details;
				final Iterator<Object> objIter = objects.iterator();
				if (objIter.hasNext())
				{
					final Object obj = objIter.next();
					if (obj instanceof Unit)
					{
						@SuppressWarnings("unchecked")
						final Collection<Unit> units = (Collection<Unit>) details;
						renderUnits(mainConstraints, units);
					}
				}
			}
			/*
			else if (details instanceof Territory)
			{
				final Territory t = (Territory) details;
				if (!m_mapPanel.isShowing(t))
					m_mapPanel.centerOn(t);
			}
			*/
		}
		add(Box.createGlue());
		validate();
		repaint();
	}
	
	private void renderUnits(final GridBagConstraints mainConstraints, final Collection<Unit> units)
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (final Unit u : units)
		{
			final JLabel label = new JLabel(new ImageIcon(m_mapPanel.getUnitImageFactory().getImage(u.getType(), u.getOwner(), m_data)));
			label.setToolTipText(u.getType().getName());
			panel.add(label);
		}
		add(panel, mainConstraints);
	}
	
}
