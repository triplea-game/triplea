package org.triplea.debug.error.reporting;

/** Interface for interactions with the stack trace user-reporting window UI. */
public interface StackTraceReportView {

  /** Returns the data user has entered in the error description field. */
  String readUserDescription();

  /** Method where UI components should bind components actions to model methods. */
  void bindActions(StackTraceReportModel viewModel);

  /** Closes the window. */
  void close();

  /** Sets window to visible. */
  void show();

}
