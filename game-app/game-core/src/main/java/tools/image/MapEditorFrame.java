package tools.image;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

abstract class MapEditorFrame extends JFrame {
  protected final Image image;
  protected final JPanel imagePanel;

  /**
   * Map editor frame that sets up the mouse listeners and toolbars and loads the actual image of
   * the map.
   *
   * @param mapFolder The {@link Path} pointing to the map folder.
   */
  protected MapEditorFrame(String title, Path mapFolder) {
    super(title);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setSize(800, 600);
    setLayout(new BorderLayout());
    setLocationRelativeTo(null);

    this.image = loadImage(mapFolder);

    JLabel locationLabel = new JLabel();
    imagePanel = createImagePanel(locationLabel, getMouseClickedAdapter());

    Container contentPane = this.getContentPane();
    contentPane.add(new JScrollPane(imagePanel), BorderLayout.CENTER);
    contentPane.add(locationLabel, BorderLayout.SOUTH);

    initializeLayout();
  }

  protected Image loadImage(Path mapFolder) {
    return FileHelper.newImage(mapFolder);
  }

  protected JPanel createImagePanel(JLabel locationLabel, MouseAdapter mouseClickedAdapter) {
    final JPanel newImagePanel = createMainPanel();
    newImagePanel
        .addMouseMotionListener( // to show X : Y coordinates on the lower left corner of the
            // screen
            new MouseMotionAdapter() {
              @Override
              public void mouseMoved(final MouseEvent e) {
                locationLabel.setText("x: " + e.getX() + " y: " + e.getY());
              }
            });
    newImagePanel.addMouseListener(mouseClickedAdapter);
    // set up the image panel size dimensions ...etc
    final Dimension imageDimension = new Dimension(image.getWidth(this), image.getHeight(this));
    newImagePanel.setMinimumSize(imageDimension);
    newImagePanel.setPreferredSize(imageDimension);
    newImagePanel.setMaximumSize(imageDimension);
    return newImagePanel;
  }

  /** Creates the main panel and returns a JPanel object. */
  protected abstract JPanel createMainPanel();

  protected abstract MouseAdapter getMouseClickedAdapter();

  protected abstract void initializeLayout();
}
