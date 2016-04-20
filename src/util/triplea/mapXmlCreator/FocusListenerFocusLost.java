package util.triplea.mapXmlCreator;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

abstract class FocusListenerFocusLost implements FocusListener {

  @Override
  abstract public void focusLost(final FocusEvent e);

  @Override
  public void focusGained(final FocusEvent e) {}


  public static FocusListenerFocusLost withAction(final Runnable r) {
    return new FocusListenerFocusLost() {

      @Override
      public void focusLost(final FocusEvent e) {
        r.run();
      }
    };
  }
}
