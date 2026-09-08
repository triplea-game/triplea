package org.triplea.swing;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

/**
 * Creates WindowAdapter objects with specific overwritten methods. All supplied callbacks are
 * invoked on the Swing EDT.
 */
@UtilityClass
public final class WindowAdapterFactory {

  public static WindowAdapter activatedAndClosed(
      @Nullable Runnable activatedRun, @Nullable Runnable closeRun) {
    return new WindowAdapter() {
      @Override
      public void windowClosed(final WindowEvent e) {
        if (closeRun != null) {
          closeRun.run();
        }
      }

      @Override
      public void windowActivated(final WindowEvent e) {
        if (activatedRun != null) {
          activatedRun.run();
        }
      }
    };
  }

  public static WindowAdapter closing(@Nullable Runnable closingRun) {
    return gainedFocusAndClosing(null, closingRun);
  }

  public static WindowAdapter gainedFocusAndClosing(
      @Nullable Runnable gainedFocusRun, @Nullable Runnable closingRun) {
    return new WindowAdapter() {

      @Override
      public void windowClosing(final WindowEvent e) {
        if (closingRun != null) {
          closingRun.run();
        }
      }

      @Override
      public void windowGainedFocus(final WindowEvent e) {
        if (gainedFocusRun != null) {
          gainedFocusRun.run();
        }
      }
    };
  }

  public static WindowAdapter openedAndClosing(
      @Nullable Runnable openedRun, @Nullable Runnable closingRun) {
    return new WindowAdapter() {

      @Override
      public void windowOpened(final WindowEvent e) {
        if (openedRun != null) {
          openedRun.run();
        }
      }

      @Override
      public void windowClosing(final WindowEvent e) {
        if (closingRun != null) {
          closingRun.run();
        }
      }
    };
  }
}
