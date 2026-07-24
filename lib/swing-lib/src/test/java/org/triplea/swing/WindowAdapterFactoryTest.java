package org.triplea.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.WindowAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WindowAdapterFactoryTest {
  static final int START_VALUE = 11;
  static final int END_VALUE = 22;

  int modifiableValue1;
  int modifiableValue2;

  @BeforeEach
  void setUp() {
    modifiableValue1 = START_VALUE;
    modifiableValue2 = START_VALUE;
  }

  @Test
  void testActivatedAndClosed() {
    WindowAdapter windowAdapter =
        WindowAdapterFactory.activatedAndClosed(adjustValue1(), adjustValue2());
    windowAdapter.windowActivated(null);
    assertEquals(END_VALUE, modifiableValue1);
    windowAdapter.windowClosed(null);
    assertEquals(END_VALUE, modifiableValue2);
  }

  private Runnable adjustValue1() {
    return () -> modifiableValue1 = END_VALUE;
  }

  private Runnable adjustValue2() {
    return () -> modifiableValue2 = END_VALUE;
  }

  @Test
  void testGainedFocusAndClosing() {
    WindowAdapter windowAdapter =
        WindowAdapterFactory.gainedFocusAndClosing(adjustValue1(), adjustValue2());
    windowAdapter.windowGainedFocus(null);
    assertEquals(END_VALUE, modifiableValue1);
    windowAdapter.windowClosing(null);
    assertEquals(END_VALUE, modifiableValue2);
  }

  @Test
  void testClosing() {
    WindowAdapter windowAdapter =
        WindowAdapterFactory.gainedFocusAndClosing(adjustValue1(), adjustValue2());
    windowAdapter.windowClosing(null);
    assertEquals(START_VALUE, modifiableValue1);
    windowAdapter.windowClosing(null);
    assertEquals(END_VALUE, modifiableValue2);
  }

  @Test
  void testOpenedAndClosing() {
    WindowAdapter windowAdapter =
        WindowAdapterFactory.openedAndClosing(adjustValue1(), adjustValue2());
    windowAdapter.windowOpened(null);
    assertEquals(END_VALUE, modifiableValue1);
    windowAdapter.windowClosing(null);
    assertEquals(END_VALUE, modifiableValue2);
  }
}
