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
 * ScrollableTextField.java
 *
 * Created on November 26, 2001, 11:03 AM
 */

package games.strategy.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import games.strategy.util.ListenerList;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ScrollableTextField extends JPanel
{
  public static void main(String[] args)
  {
    JFrame frame = new JFrame();
    frame.addWindowListener(Util.EXIT_ON_CLOSE_WINDOW_LISTENER);
    frame.getContentPane().setLayout(new FlowLayout());
    frame.getContentPane().add(new JLabel("10 - 20"));

    frame.getContentPane().add(new ScrollableTextField(10,20));
    frame.getContentPane().add(new JLabel("-10 - 10"));
    frame.getContentPane().add(new ScrollableTextField(-10,10));

    ScrollableTextField field = new ScrollableTextField(0,100);
    field.addChangeListener( new ScrollableTextFieldListener() {

      public void changedValue(ScrollableTextField field)
      {
        System.out.println(field.getValue());
      }
    } );
    frame.getContentPane().add(new JLabel("0-100, listened to"));
    frame.getContentPane().add(field);


    frame.setSize(400,140);
    frame.show();
  }


  private static boolean s_imagesLoaded;

  private static Icon s_up;
  private static Icon s_down;
  private static Icon s_max;
  private static Icon s_min;

  private synchronized static void loadImages(ScrollableTextField field)
  {
    if(s_imagesLoaded)
      return;

    s_up = new ImageIcon(ScrollableTextField.class.getResource("images/up.gif"));
    s_down = new ImageIcon(ScrollableTextField.class.getResource("images/down.gif"));
    s_max = new ImageIcon(ScrollableTextField.class.getResource("images/max.gif"));
    s_min = new ImageIcon(ScrollableTextField.class.getResource("images/min.gif"));

    s_imagesLoaded = true;
  }


  private IntTextField m_text;
  private JButton m_up;
  private JButton m_down;
  private JButton m_max;
  private JButton m_min;
  private ListenerList m_listeners = new ListenerList();

  /** Creates new ScrollableTextField */
    public ScrollableTextField(int minVal, int maxVal)
  {
    super();
    loadImages(this);

    m_text = new IntTextField(minVal, maxVal);

    setLayout(new FlowLayout(FlowLayout.LEFT, 0,0 ));
    add(m_text);

    Insets inset = new Insets(0,0,0,0);

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

    JPanel upDown = new JPanel();
    upDown.setLayout(new BoxLayout(upDown, BoxLayout.Y_AXIS));
    upDown.add(m_up);
    upDown.add(m_down);

    JPanel maxMin = new JPanel();
    maxMin.setLayout(new BoxLayout(maxMin, BoxLayout.Y_AXIS));
    maxMin.add(m_max);
    maxMin.add(m_min);

    add(upDown);
    add(maxMin);

    m_text.addChangeListener(m_textListener);

    setWidgetActivation();
    }

  public void setMax(int max)
  {
    m_text.setMax(max);
    setWidgetActivation();
  }

  public int getMax()
  {
    return m_text.getMax();
  }

  public void setMin(int min)
  {
    m_text.setMin(min);
    setWidgetActivation();
  }

  private void setWidgetActivation()
  {
    int value = m_text.getValue();

    int max = m_text.getMax();
    boolean enableUp = (value != max);
    m_up.setEnabled(enableUp);
    m_max.setEnabled(enableUp);

    int min = m_text.getMin();
    boolean enableDown = (value != min);
    m_down.setEnabled(enableDown);
    m_min.setEnabled(enableDown);
  }


  private Action m_incrementAction = new  AbstractAction("inc")
  {
    public void actionPerformed(ActionEvent e)
    {
      m_text.setValue(m_text.getValue() + 1);
      setWidgetActivation();
    }
  };

  private Action m_decrementAction = new  AbstractAction("dec")
  {
    public void actionPerformed(ActionEvent e)
    {
      m_text.setValue(m_text.getValue() - 1);
      setWidgetActivation();
    }
  };

  private Action m_maxAction = new  AbstractAction("max")
  {
    public void actionPerformed(ActionEvent e)
    {
      m_text.setValue(m_text.getMax());
      setWidgetActivation();
    }
  };

  private Action m_minAction = new  AbstractAction("min")
  {
    public void actionPerformed(ActionEvent e)
    {
      m_text.setValue(m_text.getMin());
      setWidgetActivation();
    }
  };

  public int getValue()
  {
    return m_text.getValue();
  }

  public void setValue(int value)
  {
    m_text.setValue(value);
    setWidgetActivation();
  }

  public void addChangeListener(ScrollableTextFieldListener listener)
  {
    m_listeners.add(listener);
  }

  public void removeChangeListener(ScrollableTextFieldListener listener)
  {
    m_listeners.remove(listener);
  }

  private void notifyListeners()
  {
    Iterator iter = m_listeners.iterator();
    while(iter.hasNext())
    {
      ScrollableTextFieldListener listener = (ScrollableTextFieldListener) iter.next();
      listener.changedValue(this);
    }
  }

  private IntTextFieldChangeListener m_textListener = new IntTextFieldChangeListener()
  {
    public void changedValue(IntTextField field)
    {
      notifyListeners();
    }
  };
}
