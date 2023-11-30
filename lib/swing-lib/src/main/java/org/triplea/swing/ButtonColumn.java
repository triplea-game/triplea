package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import lombok.Getter;

/**
 * Based on code from: http://www.camick.com/java/source/ButtonColumn.java
 *
 * <p>The ButtonColumn class provides a renderer and an editor that looks like a JButton. The
 * renderer and editor will then be used for a specified column in the table. The TableModel will
 * contain the String to be displayed on the button.
 *
 * <p>The button can be invoked by a mouse click or by pressing the space bar when the cell has
 * focus. Optionally a mnemonic can be set to invoke the button. When the button is invoked the
 * provided Action is invoked. The source of the Action will be the table. The action command will
 * contain the model row number of the button that was clicked.
 */
public class ButtonColumn extends AbstractCellEditor
    implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
  private static final long serialVersionUID = -9142097942528738143L;
  private final JTable table;
  private final Action action;
  @Getter private int mnemonic;
  private final Border originalBorder;

  /**
   * -- GETTER -- Get foreground color of the button when the cell has focus.
   *
   * @return the foreground color
   */
  @Getter private Border focusBorder;

  private final JButton renderButton;
  private final JButton editButton;
  private Object cellEditorValue;
  private boolean isButtonColumnEditor;

  /**
   * Create the ButtonColumn to be used as a renderer and editor. The renderer and editor will
   * automatically be installed on the TableColumn of the specified column.
   *
   * @param table the table containing the button renderer/editor
   * @param action the Action to be invoked when the button is invoked
   * @param column the column to which the button renderer/editor is added
   */
  private ButtonColumn(final JTable table, final int column, final Action action) {
    this.table = table;
    this.action = action;

    renderButton = new JButton();
    editButton = new JButton();
    editButton.setFocusPainted(false);
    editButton.addActionListener(this);
    originalBorder = editButton.getBorder();
    setFocusBorder(new LineBorder(Color.BLUE));

    final TableColumnModel columnModel = table.getColumnModel();
    columnModel.getColumn(column).setCellRenderer(this);
    columnModel.getColumn(column).setCellEditor(this);
    table.addMouseListener(this);
  }

  /**
   * Converts a given column to buttons. The existing column data text will become the text of the
   * new buttons.
   *
   * @param table The table to be updated.
   * @param column Zero-based column number of the table.
   * @param buttonListener The listener that will be fired when one of the new buttons are clicked.
   */
  public static void attachButtonColumn(
      final JTable table,
      final int column,
      final BiConsumer<Integer, DefaultTableModel> buttonListener) {
    Preconditions.checkState(table.getModel().getColumnCount() > column);

    new ButtonColumn(
        table,
        column,
        new AbstractAction() {
          private static final long serialVersionUID = 786926815237533866L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            final int rowNumber = Integer.parseInt(e.getActionCommand());
            final DefaultTableModel defaultTableModel = (DefaultTableModel) table.getModel();
            buttonListener.accept(rowNumber, defaultTableModel);
          }
        });
  }

  /**
   * The foreground color of the button when the cell has focus.
   *
   * @param focusBorder the foreground color
   */
  private void setFocusBorder(final Border focusBorder) {
    this.focusBorder = focusBorder;
    editButton.setBorder(focusBorder);
  }

  /**
   * The mnemonic to activate the button when the cell has focus.
   *
   * @param mnemonic the mnemonic
   */
  public void setMnemonic(final int mnemonic) {
    this.mnemonic = mnemonic;
    renderButton.setMnemonic(mnemonic);
    editButton.setMnemonic(mnemonic);
  }

  @Override
  public Component getTableCellEditorComponent(
      final JTable table,
      final Object value,
      final boolean isSelected,
      final int row,
      final int column) {
    if (value == null) {
      editButton.setText("");
      editButton.setIcon(null);
    } else if (value instanceof Icon) {
      editButton.setText("");
      editButton.setIcon((Icon) value);
    } else {
      editButton.setText(value.toString());
      editButton.setIcon(null);
    }

    cellEditorValue = value;
    return editButton;
  }

  @Override
  public Object getCellEditorValue() {
    return cellEditorValue;
  }

  @Override
  public Component getTableCellRendererComponent(
      final JTable table,
      final Object value,
      final boolean isSelected,
      final boolean hasFocus,
      final int row,
      final int column) {
    if (isSelected) {
      renderButton.setForeground(table.getSelectionForeground());
      renderButton.setBackground(table.getSelectionBackground());
    } else {
      renderButton.setForeground(table.getForeground());
      renderButton.setBackground(UIManager.getColor("Button.background"));
    }

    if (hasFocus) {
      renderButton.setBorder(focusBorder);
    } else {
      renderButton.setBorder(originalBorder);
    }

    if (value == null) {
      renderButton.setText("");
      renderButton.setIcon(null);
    } else if (value instanceof Icon) {
      renderButton.setText("");
      renderButton.setIcon((Icon) value);
    } else {
      renderButton.setText(value.toString());
      renderButton.setIcon(null);
    }

    return renderButton;
  }

  /** The button has been pressed. Stop editing and invoke the custom Action */
  @Override
  public void actionPerformed(final ActionEvent e) {
    final int row = table.convertRowIndexToModel(table.getEditingRow());
    fireEditingStopped();

    // Invoke the Action

    final ActionEvent event =
        new ActionEvent(table, ActionEvent.ACTION_PERFORMED, String.valueOf(row));
    action.actionPerformed(event);
  }

  /**
   * When the mouse is pressed the editor is invoked. If you then then drag the mouse to another
   * cell before releasing it, the editor is still active. Make sure editing is stopped when the
   * mouse is released.
   */
  @Override
  public void mousePressed(final MouseEvent e) {
    if (table.isEditing() && (table.getCellEditor() == this)) {
      isButtonColumnEditor = true;
    }
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
    if (isButtonColumnEditor && table.isEditing()) {
      table.getCellEditor().stopCellEditing();
    }

    isButtonColumnEditor = false;
  }

  @Override
  public void mouseClicked(final MouseEvent e) {}

  @Override
  public void mouseEntered(final MouseEvent e) {}

  @Override
  public void mouseExited(final MouseEvent e) {}
}
