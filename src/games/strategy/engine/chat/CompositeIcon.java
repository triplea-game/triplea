package games.strategy.engine.chat;

import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

import javax.swing.Icon;

public class CompositeIcon implements Icon
{
	private static final int GAP = 2;
	private final List<Icon> m_incons;
	
	CompositeIcon(final List<Icon> icons)
	{
		m_incons = icons;
	}
	
	public void paintIcon(final Component c, final Graphics g, final int x, final int y)
	{
		int dx = 0;
		for (final Icon icon : m_incons)
		{
			icon.paintIcon(c, g, x + dx, y);
			dx += GAP;
			dx += icon.getIconWidth();
		}
	}
	
	public int getIconWidth()
	{
		int sum = 0;
		for (final Icon icon : m_incons)
		{
			sum += icon.getIconWidth();
			sum += GAP;
		}
		return sum;
	}
	
	public int getIconHeight()
	{
		int max = 0;
		for (final Icon icon : m_incons)
		{
			max = Math.max(icon.getIconHeight(), max);
		}
		return max;
	}
}
