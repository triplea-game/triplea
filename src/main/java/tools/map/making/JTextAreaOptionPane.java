package tools.map.making;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

/**
 * A text area that can show updates scrolling by.
 */
public class JTextAreaOptionPane {
  private final JTextArea editor = new JTextArea();
  private final JFrame windowFrame = new JFrame();
  private final JButton okButton = new JButton();
  private final boolean logToSystemOut;
  private final Window parentComponent;
  private int counter;
  private final CountDownLatch countDownLatch;

  public JTextAreaOptionPane(final JFrame parentComponent, final String initialEditorText, final String labelText,
      final String title, final Image icon, final int editorSizeX, final int editorSizeY, final boolean logToSystemOut,
      final int latchCount, final CountDownLatch countDownLatch) {
    this.logToSystemOut = logToSystemOut;
    this.countDownLatch = countDownLatch;
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
    final JLabel m_label = new JLabel();
    m_label.setText(labelText);
    okButton.setText("OK");
    okButton.setEnabled(false);
    editor.setEditable(false);
    // editor.setContentType("text/html");
    editor.setText(initialEditorText);
    if (this.logToSystemOut) {
      System.out.println(initialEditorText);
    }
    editor.setCaretPosition(0);
    windowFrame.setPreferredSize(new Dimension(editorSizeX, editorSizeY));
    windowFrame.getContentPane().add(m_label, BorderLayout.NORTH);
    windowFrame.getContentPane().add(new JScrollPane(editor), BorderLayout.CENTER);
    windowFrame.getContentPane().add(okButton, BorderLayout.SOUTH);
    okButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (JTextAreaOptionPane.this.countDownLatch != null) {
          JTextAreaOptionPane.this.countDownLatch.countDown();
        }
        dispose();
      }
    });
  }

  private void setWidgetActivation() {
    if (counter <= 0) {
      okButton.setEnabled(true);
    }
  }

  public void show() {
    windowFrame.pack();
    windowFrame.setLocationRelativeTo(parentComponent);
    windowFrame.setVisible(true);
  }

  public void dispose() {
    windowFrame.setVisible(false);
    windowFrame.dispose();
  }

  public void countDown() {
    counter--;
    setWidgetActivation();
  }

  void append(final String text) {
    if (logToSystemOut) {
      System.out.print(text);
    }
    editor.append(text);
    editor.setCaretPosition(editor.getText().length());
  }

  public void appendNewLine(final String text) {
    append(text + "\r\n");
  }
}
