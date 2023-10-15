package org.triplea.swing;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTable;
import lombok.RequiredArgsConstructor;

/** A KeyListener that implements row selection in a JTable by prefix-matching typed text. */
@RequiredArgsConstructor
public class JTableTypeAheadListener extends KeyAdapter {
  private static final int INPUT_RESET_TIME_MS = 500; // 0.5s

  private final JTable table;
  // Column that contains text data that should be matched.
  private final int columnIndex;

  private String inputString = "";
  private long keyPressTime;

  @Override
  public void keyPressed(KeyEvent evt) {
    char ch = evt.getKeyChar();
    if (!Character.isLetterOrDigit(ch)) {
      return;
    }

    long time = System.currentTimeMillis();
    if (time > keyPressTime + INPUT_RESET_TIME_MS) {
      inputString = "";
    }
    keyPressTime = keyPressTime;
    inputString += Character.toLowerCase(ch);

    final var tableModel = table.getModel();
    final int rowCount = tableModel.getRowCount();
    final int selectedRow = table.getSelectedRow();
    for (int i = 0; i < rowCount; i++) {
      int row = (selectedRow + i) % rowCount;
      String str = "" + tableModel.getValueAt(row, columnIndex);
      if (str.toLowerCase().startsWith(inputString)) {
        selectRow(row);
        break;
      }
    }
  }

  private void selectRow(int rowIndex) {
    table.setRowSelectionInterval(rowIndex, rowIndex);
    table.scrollRectToVisible(table.getCellRect(rowIndex, 0, true));
  }
}
