package org.triplea.debug.console.window;

/** Defines the contract between console window and its view-model. */
interface ConsoleView {
  String readText();

  void setText(String text);

  void setVisible();

  void append(String text);

  void addWindowClosedListener(Runnable closeListener);
}
