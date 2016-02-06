package games.strategy.ui;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import games.strategy.common.swing.SwingAction;

/**
 * Start with one arg that is the filename
 * press z to set the zoom.
 */
public class ZoomableImageExample extends JFrame {
  private static final long serialVersionUID = -2966214293779872824L;
  ZoomableImage panel;

  public static void main(final String[] args) {
    if (args.length == 0) {
      System.out.println("Expecting first and only arg to be the name of the image file");
    }
    final JFrame frame = new ZoomableImageExample(args[0]);
    frame.setVisible(true);
  }

  /** Creates new ZoomableImageExample */
  public ZoomableImageExample(final String imageName) {
    final Image image = Toolkit.getDefaultToolkit().getImage(imageName);
    panel = new ZoomableImage(image);
    this.setSize(400, 400);
    this.addWindowListener(EXIT_ON_CLOSE_WINDOW_LISTENER);
    this.getContentPane().add(new JScrollPane(panel));
    this.addKeyListener(KEY_LISTENER);
  }

  final KeyListener KEY_LISTENER = new KeyAdapter() {
    @Override
    public void keyPressed(final KeyEvent e) {
      final char key = e.getKeyChar();
      if (key == 'z') {
        ZOOM_COMMAND.actionPerformed(null);
      }
    }
  };
  private final Action ZOOM_COMMAND = SwingAction.of("save", e -> {
    final String input = JOptionPane.showInputDialog("Get Zoom Factor");
    panel.setZoom(Integer.parseInt(input));
  });
  public static final WindowListener EXIT_ON_CLOSE_WINDOW_LISTENER = new WindowAdapter() {
    @Override
    public void windowClosing(final WindowEvent e) {
      System.exit(0);
    }
  };
}
