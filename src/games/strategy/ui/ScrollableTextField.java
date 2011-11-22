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
 * ScrollableTextField.java
 * 
 * Created on November 26, 2001, 11:03 AM
 */
package games.strategy.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.util.ListenerList;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class ScrollableTextField extends JPanel
{
	public static void main(final String[] args)
	{
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel("10 - 20"));
		frame.getContentPane().add(new ScrollableTextField(10, 20));
		frame.getContentPane().add(new JLabel("-10 - 10"));
		frame.getContentPane().add(new ScrollableTextField(-10, 10));
		final ScrollableTextField field = new ScrollableTextField(0, 100);
		field.addChangeListener(new ScrollableTextFieldListener()
		{
			public void changedValue(final ScrollableTextField aField)
			{
				System.out.println(aField.getValue());
			}
		});
		frame.getContentPane().add(new JLabel("0-100, listened to"));
		frame.getContentPane().add(field);
		frame.setSize(400, 140);
		frame.setVisible(true);
	}
	
	private static boolean s_imagesLoaded;
	private static Icon s_up;
	private static Icon s_down;
	private static Icon s_max;
	private static Icon s_min;
	
	private synchronized static void loadImages(final ScrollableTextField field)
	{
		if (s_imagesLoaded)
			return;
		s_up = new ImageIcon(ScrollableTextField.class.getResource("images/up.gif"));
		s_down = new ImageIcon(ScrollableTextField.class.getResource("images/down.gif"));
		s_max = new ImageIcon(ScrollableTextField.class.getResource("images/max.gif"));
		s_min = new ImageIcon(ScrollableTextField.class.getResource("images/min.gif"));
		s_imagesLoaded = true;
	}
	
	private final IntTextField m_text;
	private final JButton m_up;
	private final JButton m_down;
	private final JButton m_max;
	private final JButton m_min;
	private final ListenerList<ScrollableTextFieldListener> m_listeners = new ListenerList<ScrollableTextFieldListener>();
	
	/** Creates new ScrollableTextField */
	public ScrollableTextField(final int minVal, final int maxVal)
	{
		super();
		loadImages(this);
		m_text = new IntTextField(minVal, maxVal);
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(m_text);
		Insets inset = new Insets(0, 0, 0, 0);
		if (GameRunner.isMac())
		{
			inset = new Insets(2, 0, 2, 0);
		}
		m_up = new JButton(s_up);
		m_up.addActionListener(m_incrementAction);
		m_up.setMargin(inset);
		m_down = new JButton(s_down);
		m_down.setMargin(inset);
		m_down.addActionListener(m_decrementAction);
		m_max = new JButton(s_max);
		m_max.setMargin(inset);
		m_max.addActionListener(m_maxAction);
		m_min = new JButton(s_min);
		m_min.setMargin(inset);
		m_min.addActionListener(m_minAction);
		final JPanel upDown = new JPanel();
		upDown.setLayout(new BoxLayout(upDown, BoxLayout.Y_AXIS));
		upDown.add(m_up);
		upDown.add(m_down);
		final JPanel maxMin = new JPanel();
		maxMin.setLayout(new BoxLayout(maxMin, BoxLayout.Y_AXIS));
		maxMin.add(m_max);
		maxMin.add(m_min);
		add(upDown);
		add(maxMin);
		// add(new JSpinner());
		m_text.addChangeListener(m_textListener);
		setWidgetActivation();
	}
	
	public void setMax(final int max)
	{
		m_text.setMax(max);
		setWidgetActivation();
	}
	
	public void setTerr(final String terr)
	{
		m_text.setTerr(terr);
	}
	
	public void setShowMaxAndMin(final boolean aBool)
	{
		m_max.setVisible(aBool);
		m_min.setVisible(aBool);
	}
	
	public int getMax()
	{
		return m_text.getMax();
	}
	
	public String getTerr()
	{
		return m_text.getTerr();
	}
	
	public void setMin(final int min)
	{
		m_text.setMin(min);
		setWidgetActivation();
	}
	
	private void setWidgetActivation()
	{
		final int value = m_text.getValue();
		final int max = m_text.getMax();
		final boolean enableUp = (value != max);
		m_up.setEnabled(enableUp);
		m_max.setEnabled(enableUp);
		final int min = m_text.getMin();
		final boolean enableDown = (value != min);
		m_down.setEnabled(enableDown);
		m_min.setEnabled(enableDown);
	}
	
	private final Action m_incrementAction = new AbstractAction("inc")
	{
		public void actionPerformed(final ActionEvent e)
		{
			m_text.setValue(m_text.getValue() + 1);
			setWidgetActivation();
		}
	};
	private final Action m_decrementAction = new AbstractAction("dec")
	{
		public void actionPerformed(final ActionEvent e)
		{
			m_text.setValue(m_text.getValue() - 1);
			setWidgetActivation();
		}
	};
	private final Action m_maxAction = new AbstractAction("max")
	{
		public void actionPerformed(final ActionEvent e)
		{
			m_text.setValue(m_text.getMax());
			setWidgetActivation();
		}
	};
	private final Action m_minAction = new AbstractAction("min")
	{
		public void actionPerformed(final ActionEvent e)
		{
			m_text.setValue(m_text.getMin());
			setWidgetActivation();
		}
	};
	
	public int getValue()
	{
		return m_text.getValue();
	}
	
	public void setValue(final int value)
	{
		m_text.setValue(value);
		setWidgetActivation();
	}
	
	public void addChangeListener(final ScrollableTextFieldListener listener)
	{
		m_listeners.add(listener);
	}
	
	public void removeChangeListener(final ScrollableTextFieldListener listener)
	{
		m_listeners.remove(listener);
	}
	
	private void notifyListeners()
	{
		final Iterator<ScrollableTextFieldListener> iter = m_listeners.iterator();
		while (iter.hasNext())
		{
			final ScrollableTextFieldListener listener = iter.next();
			listener.changedValue(this);
		}
	}
	
	private final IntTextFieldChangeListener m_textListener = new IntTextFieldChangeListener()
	{
		public void changedValue(final IntTextField field)
		{
			notifyListeners();
		}
	};
}
