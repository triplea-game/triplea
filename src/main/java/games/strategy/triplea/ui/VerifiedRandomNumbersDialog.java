package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import games.strategy.engine.framework.VerifiedRandomNumbers;
import games.strategy.engine.random.RemoteRandom;
import games.strategy.triplea.formatter.MyFormatter;

public class VerifiedRandomNumbersDialog extends JDialog {
  private static final long serialVersionUID = 439577498545812164L;

  public VerifiedRandomNumbersDialog(final Component parent) {
    super(JOptionPane.getFrameForComponent(parent), "Verified Random Numbers", false);
    init();
    pack();
  }

  private void init() {
    final List<VerifiedRandomNumbers> verified = RemoteRandom.getVerifiedRandomNumbers();
    final String[][] tableValues = getTableValues(verified);
    final DefaultTableModel model = new DefaultTableModel(tableValues, new String[] {"Reason", "Dice Rolls"}) {
      private static final long serialVersionUID = 8876974698508561554L;

      @Override
      public boolean isCellEditable(final int row, final int column) {
        return false;
      }
    };
    final JTable table = new JTable(model);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
    final JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
    getContentPane().add(buttons, BorderLayout.SOUTH);
    final JButton close = new JButton("Close");
    close.addActionListener(e -> setVisible(false));
    buttons.add(close);
  }

  private String[][] getTableValues(final List<VerifiedRandomNumbers> verified) {
    if (verified.isEmpty()) {
      return new String[][] {{"", ""}};
    }
    final String[][] tableValues = new String[verified.size()][2];
    for (int i = 0; i < verified.size(); i++) {
      final VerifiedRandomNumbers number = verified.get(i);
      tableValues[i][0] = number.getAnnotation();
      tableValues[i][1] = MyFormatter.asDice(number.getValues());
    }
    return tableValues;
  }
}
