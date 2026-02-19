package tools.map.making.ui.runnable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import lombok.extern.slf4j.Slf4j;

/** A text area that can show updates scrolling by. */
@Slf4j
class JTextAreaOptionPane {
  private final JTextArea editor = new JTextArea();
  private final JFrame windowFrame = new JFrame();
  private final JButton okButton = new JButton();
  private final Window parentComponent;
  private int counter;

  JTextAreaOptionPane(
      final JFrame parentComponent,
      final String initialEditorText,
      final String labelText,
      final String title,
      final Image icon,
      final int editorSizeX,
      final int editorSizeY,
      final int latchCount,
      final CountDownLatch countDownLatch) {
    counter = latchCount;
    this.parentComponent = parentComponent;
    windowFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    if (icon != null) {
      windowFrame.setIconImage(icon);
    } else if (parentComponent != null && parentComponent.getIconImage() != null) {
      windowFrame.setIconImage(parentComponent.getIconImage());
    }
    final BorderLayout layout = new BorderLayout();
    layout.setHgap(30);
    layout.setVgap(30);
    windowFrame.setLayout(layout);
    windowFrame.setTitle(title);
    final JLabel label = new JLabel();
    label.setText(labelText);
    okButton.setText("OK");
    okButton.setEnabled(false);
    editor.setEditable(false);
    editor.setText(initialEditorText);
    log.info(initialEditorText);
    editor.setCaretPosition(0);

    windowFrame.setPreferredSize(new Dimension(editorSizeX, editorSizeY));
    windowFrame.getContentPane().add(label, BorderLayout.NORTH);
    windowFrame.getContentPane().add(new JScrollPane(editor), BorderLayout.CENTER);
    windowFrame.getContentPane().add(okButton, BorderLayout.SOUTH);

    okButton.addActionListener(
        e -> {
          if (countDownLatch != null) {
            countDownLatch.countDown();
          }
          dispose();
        });
  }

  private void setWidgetActivation() {
    if (counter <= 0) {
      okButton.setEnabled(true);
    }
  }

  void show() {
    windowFrame.pack();
    windowFrame.setLocationRelativeTo(parentComponent);
    windowFrame.setVisible(true);
  }

  void dispose() {
    windowFrame.setVisible(false);
    windowFrame.dispose();
  }

  void countDown() {
    counter--;
    setWidgetActivation();
  }

  void append(final String text) {
    log.info(text);
    editor.append(text);
    editor.setCaretPosition(editor.getText().length());
  }

  void appendNewLine(final String text) {
    append(text + "\r\n");
  }
}
