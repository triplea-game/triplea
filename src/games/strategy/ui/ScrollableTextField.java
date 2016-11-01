package games.strategy.ui;


import games.strategy.util.ListenerList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class ScrollableTextField extends FlowPane {
  private static boolean s_imagesLoaded;
  private static Image s_up;
  private static Image s_down;
  private static Image s_max;
  private static Image s_min;

  private final IntTextField m_text;
  private final Button m_up;
  private final Button m_down;
  private final Button m_max;
  private final Button m_min;
  private final ListenerList<ScrollableTextFieldListener> m_listeners = new ListenerList<>();

  /** Creates new ScrollableTextField */
  public ScrollableTextField(final int minVal, final int maxVal) {
    super();
    loadImages();
    m_text = new IntTextField(minVal, maxVal);
    setAlignment(Pos.CENTER_LEFT);
    setHgap(0);
    setVgap(0);
    getChildren().add(m_text);
    m_up = new Button();
    m_up.setGraphic(new ImageView(s_up));
    final EventHandler<ActionEvent> m_incrementAction = e -> {
      if (!m_text.isDisabled()) {
        m_text.setValue(m_text.getValue() + 1);
        setWidgetActivation();
      }
    };
    m_up.setOnAction(m_incrementAction);
    m_down = new Button();
    m_down.setGraphic(new ImageView(s_down));
    final EventHandler<ActionEvent> m_decrementAction = e -> {
      if (!m_text.isDisabled()) {
        m_text.setValue(m_text.getValue() - 1);
        setWidgetActivation();
      }
    };
    m_down.setOnAction(m_decrementAction);
    m_max = new Button();
    m_max.setGraphic(new ImageView(s_max));
    final EventHandler<ActionEvent> m_maxAction = e -> {
      if (!m_text.isDisabled()) {
        m_text.setValue(m_text.getMax());
        setWidgetActivation();
      }
    };
    m_max.setOnAction(m_maxAction);
    m_min = new Button();
    m_min.setGraphic(new ImageView(s_min));
    final EventHandler<ActionEvent> m_minAction = e -> {
      if (m_text.isDisabled()) {
        m_text.setValue(m_text.getMin());
        setWidgetActivation();
      }
    };
    m_min.setOnAction(m_minAction);
    final VBox upDown = new VBox();
    upDown.getChildren().add(m_up);
    upDown.getChildren().add(m_down);
    final VBox maxMin = new VBox();
    maxMin.getChildren().add(m_max);
    maxMin.getChildren().add(m_min);
    getChildren().add(upDown);
    getChildren().add(maxMin);
    final IntTextFieldChangeListener m_textListener = field -> notifyListeners();
    m_text.addChangeListener(m_textListener);
    setWidgetActivation();
  }

  private synchronized static void loadImages() {
    if (s_imagesLoaded) {
      return;
    }
    s_up = new Image(ScrollableTextField.class.getResourceAsStream("images/up.gif"));
    s_down = new Image(ScrollableTextField.class.getResourceAsStream("images/down.gif"));
    s_max = new Image(ScrollableTextField.class.getResourceAsStream("images/max.gif"));
    s_min = new Image(ScrollableTextField.class.getResourceAsStream("images/min.gif"));
    s_imagesLoaded = true;
  }


  public void setMax(final int max) {
    m_text.setMax(max);
    setWidgetActivation();
  }

  public void setTerr(final String terr) {
    m_text.setTerr(terr);
  }

  public void setShowMaxAndMin(final boolean aBool) {
    m_max.setVisible(aBool);
    m_min.setVisible(aBool);
  }

  public int getMax() {
    return m_text.getMax();
  }

  public String getTerr() {
    return m_text.getTerr();
  }

  public void setMin(final int min) {
    m_text.setMin(min);
    setWidgetActivation();
  }

  private void setWidgetActivation() {
    if (m_text.isDisabled()) {
      final int value = m_text.getValue();
      final int max = m_text.getMax();
      final boolean enableUp = value == max;
      m_up.setDisable(enableUp);
      m_max.setDisable(enableUp);
      final int min = m_text.getMin();
      final boolean enableDown = value == min;
      m_down.setDisable(enableDown);
      m_min.setDisable(enableDown);
    } else {
      m_up.setDisable(true);
      m_down.setDisable(true);
      m_max.setDisable(true);
      m_min.setDisable(true);
    }
  }

  public int getValue() {
    return m_text.getValue();
  }

  public void setValue(final int value) {
    m_text.setValue(value);
    setWidgetActivation();
  }

  public void addChangeListener(final ScrollableTextFieldListener listener) {
    m_listeners.add(listener);
  }

  public void removeChangeListener(final ScrollableTextFieldListener listener) {
    m_listeners.remove(listener);
  }

  private void notifyListeners() {
    for (final ScrollableTextFieldListener listener : m_listeners) {
      listener.changedValue(this);
    }
  }
}
