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
 * IntTextField.java
 *
 * Created on November 26, 2001, 10:16 AM
 */

package games.strategy.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import games.strategy.util.ListenerList;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Text field for entering int values.  
 * Ensures valid integers are entered, and can limit the range of 
 * values user can enter.
 */
public class IntTextField extends JTextField 
{

	private int m_max = Integer.MAX_VALUE;
	private int m_min = Integer.MIN_VALUE;
	private ListenerList m_listeners = new ListenerList();
	
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.addWindowListener(Util.EXIT_ON_CLOSE_WINDOW_LISTENER);
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel("no range"));
		frame.getContentPane().add(new IntTextField());
		frame.getContentPane().add(new JLabel("positive"));
		frame.getContentPane().add(new IntTextField(0));
		frame.getContentPane().add(new JLabel("0 to 5"));
		frame.getContentPane().add(new IntTextField(0,5));
		
		frame.setSize(400,60);
		frame.show();
	}
	
	/** Creates new IntTextBox */
    public IntTextField() 
	{
		super(3);
		initTextField();
    }
	
	public IntTextField(int min)
	{
		this();
		setMin(min);
	}
	
	public IntTextField(int min, int max)
	{
		this();
		setMin(min);
		setMax(max);		
	}
	
	private void initTextField()
	{
		setDocument(new IntegerDocument());
		setText(String.valueOf(m_min));
		addFocusListener(new LostFocus());
	}

	public int getValue()
	{
		return Integer.parseInt(getText());	
	}
	
	private void checkValue()
	{
		if(getText().trim().equals("-"))
		{
			setText(String.valueOf(m_min));
		}
		
		try
		{
			Integer.parseInt(getText());	
		} catch(NumberFormatException e)
		{
			setText( String.valueOf(m_min));
		}
		
		if(getValue() > m_max)
		{
			setText(String.valueOf(m_max));

		}
		
		if(getValue() < m_min)
		{
			setText(String.valueOf(m_min));
		}
	}
	
	public void setValue(int value)
	{
		if(isGood(value))
		{
			setText(String.valueOf(value));
			
		}
	}
	
	public void setMax(int max)
	{
		if(max < m_min)
			throw new IllegalArgumentException("Max cant be less than min");
		
		m_max = max;
		
		if(getValue() > m_max)
		{
			setText(String.valueOf(max));
			
		}
	}
	
	public void setMin(int min)
	{
		if(min > m_max)
			throw new IllegalArgumentException("Min cant be greater than max");
		
		m_min = min;
		
		if(getValue() <  m_min)
		{
			setText(String.valueOf(min));
			
		}
	}	
	
	public int getMax()
	{
		return m_max;
	}
	
	public int getMin()
	{
		return m_min;
	}
	
	private final boolean isGood(int value)
	{
		return value <= m_max && value >= m_min;
	}	
	
	private boolean isGood(String value)
	{
		try
		{
			int asInt = Integer.parseInt(value);
			return isGood(asInt);
		}
		catch(NumberFormatException e)
		{
			return false;
		}
		
	}
	
	/**
	 * Make sure that no non numeric data is typed.
	 */
	private class IntegerDocument extends PlainDocument 
	{
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
		{
			String currentText = this.getText(0, getLength());
			String beforeOffset = currentText.substring(0, offs);
			String afterOffset = currentText.substring(offs, currentText.length());
			String proposedResult = beforeOffset + str + afterOffset;
			
			//allow start of negative 
			try
			{
				Integer.parseInt(proposedResult);
				super.insertString(offs, str, a);
				notifyListeners();
			} catch(NumberFormatException e)
			{
				//if an error dont insert
				//allow start of negative numbers
				if(offs == 0)
				{
					if(m_min < 0)
						if(str.equals("-"))
							super.insertString(offs, str, a);
				}
			}
		}
		
		public void remove(int offs, int len) throws BadLocationException
		{
			super.remove(offs, len);
			//if its a valid number weve changed
			try
			{
				Integer.parseInt(IntTextField.this.getText());
				notifyListeners();
			} catch(NumberFormatException e)
			{}
			
		}
	}
	
	public void addChangeListener(IntTextFieldChangeListener listener)
	{
		m_listeners.add(listener);
	}
	
	public void removeChangeListener(IntTextFieldChangeListener listener)
	{
		m_listeners.remove(listener);
	}
	
	private void notifyListeners()
	{
		Iterator iter = m_listeners.iterator();
		while(iter.hasNext())
		{
			IntTextFieldChangeListener listener = (IntTextFieldChangeListener) iter.next();
			listener.changedValue(this);
		}
	}
	
	private class LostFocus extends FocusAdapter
	{
		public void focusLost(FocusEvent e)
		{
			//make sure the value is valid
			checkValue();
		}
	}
}
