package games.strategy.ui;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import games.strategy.engine.framework.system.SystemProperties;

public class ScrollableTextField extends JPanel {
  private static final long serialVersionUID = 6940592988573672224L;

  private static boolean imagesLoaded;
  private static Icon up;
  private static Icon down;
  private static Icon max;
  private static Icon min;

  private final IntTextField text;
  private final JButton upButton;
  private final JButton downButton;
  private final JButton maxButton;
  private final JButton minButton;
  private final List<ScrollableTextFieldListener> listeners = new CopyOnWriteArrayList<>();

  /** Creates new ScrollableTextField. */
  public ScrollableTextField(final int minVal, final int maxVal) {
    super();
    loadImages();
    text = new IntTextField(minVal, maxVal);
    setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    add(text);
    Insets inset = new Insets(0, 0, 0, 0);
    if (SystemProperties.isMac()) {
      inset = new Insets(2, 0, 2, 0);
    }
    upButton = new JButton(up);
    final Action m_incrementAction = new AbstractAction("inc") {
      private static final long serialVersionUID = 2125871167112459475L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (text.isEnabled()) {
          text.setValue(text.getValue() + 1);
          setWidgetActivation();
        }
      }
    };
    upButton.addActionListener(m_incrementAction);
    upButton.setMargin(inset);
    downButton = new JButton(down);
    downButton.setMargin(inset);
    final Action m_decrementAction = new AbstractAction("dec") {
      private static final long serialVersionUID = 787758939168986726L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (text.isEnabled()) {
          text.setValue(text.getValue() - 1);
          setWidgetActivation();
        }
      }
    };
    downButton.addActionListener(m_decrementAction);
    maxButton = new JButton(max);
    maxButton.setMargin(inset);
    final Action m_maxAction = new AbstractAction("max") {
      private static final long serialVersionUID = -3899827439573519512L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (text.isEnabled()) {
          text.setValue(text.getMax());
          setWidgetActivation();
        }
      }
    };
    maxButton.addActionListener(m_maxAction);
    minButton = new JButton(min);
    minButton.setMargin(inset);
    final Action m_minAction = new AbstractAction("min") {
      private static final long serialVersionUID = 5785321239855254848L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (text.isEnabled()) {
          text.setValue(text.getMin());
          setWidgetActivation();
        }
      }
    };
    minButton.addActionListener(m_minAction);
    final JPanel upDown = new JPanel();
    upDown.setLayout(new BoxLayout(upDown, BoxLayout.Y_AXIS));
    upDown.add(upButton);
    upDown.add(downButton);
    final JPanel maxMin = new JPanel();
    maxMin.setLayout(new BoxLayout(maxMin, BoxLayout.Y_AXIS));
    maxMin.add(maxButton);
    maxMin.add(minButton);
    add(upDown);
    add(maxMin);
    final IntTextFieldChangeListener m_textListener = field -> notifyListeners();
    text.addChangeListener(m_textListener);
    setWidgetActivation();
  }

  private static synchronized void loadImages() {
    if (imagesLoaded) {
      return;
    }
    up = new ImageIcon(ScrollableTextField.class.getResource("images/up.gif"));
    down = new ImageIcon(ScrollableTextField.class.getResource("images/down.gif"));
    max = new ImageIcon(ScrollableTextField.class.getResource("images/max.gif"));
    min = new ImageIcon(ScrollableTextField.class.getResource("images/min.gif"));
    imagesLoaded = true;
  }


  public void setMax(final int max) {
    text.setMax(max);
    setWidgetActivation();
  }

  public void setTerr(final String terr) {
    text.setTerr(terr);
  }

  public void setShowMaxAndMin(final boolean aBool) {
    maxButton.setVisible(aBool);
    minButton.setVisible(aBool);
  }

  public int getMax() {
    return text.getMax();
  }

  public String getTerr() {
    return text.getTerr();
  }

  public void setMin(final int min) {
    text.setMin(min);
    setWidgetActivation();
  }

  private void setWidgetActivation() {
    if (text.isEnabled()) {
      final int value = text.getValue();
      final int max = text.getMax();
      final boolean enableUp = (value != max);
      upButton.setEnabled(enableUp);
      maxButton.setEnabled(enableUp);
      final int min = text.getMin();
      final boolean enableDown = (value != min);
      downButton.setEnabled(enableDown);
      minButton.setEnabled(enableDown);
    } else {
      upButton.setEnabled(false);
      downButton.setEnabled(false);
      maxButton.setEnabled(false);
      minButton.setEnabled(false);
    }
  }

  public int getValue() {
    return text.getValue();
  }

  public void setValue(final int value) {
    text.setValue(value);
    setWidgetActivation();
  }

  public void addChangeListener(final ScrollableTextFieldListener listener) {
    listeners.add(listener);
  }

  public void removeChangeListener(final ScrollableTextFieldListener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners() {
    for (final ScrollableTextFieldListener listener : listeners) {
      listener.changedValue(this);
    }
  }

  @Override
  public void setEnabled(final boolean enabled) {
    text.setEnabled(enabled);
    setWidgetActivation();
  }
}
