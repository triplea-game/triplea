package tools.map.making.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.file.Path;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.key.binding.KeyCode;
import tools.util.FileHelper;

public abstract class MapEditorFrame extends JFrame {
  protected final JPanel imagePanel;
  protected final Path mapFolderLocation;
  protected Image image;

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

    this.mapFolderLocation = mapFolder;
    this.image = loadImage(mapFolder);

    JLabel locationLabel = new JLabel();
    locationLabel.setFont(getLocationLabelFont(locationLabel.getFont()));
    imagePanel = createImagePanel(locationLabel);

    Container contentPane = this.getContentPane();
    contentPane.add(new JScrollPane(imagePanel), BorderLayout.CENTER);
    contentPane.add(locationLabel, BorderLayout.SOUTH);

    initializeLayout();
  }

  protected static JMenu getFileMenu(
      Action openAction, Action saveAction, Action exitAction, JMenuItem... additionalMenuItems) {
    JMenuBuilder fileMenuBuilder =
        new JMenuBuilder("File", KeyCode.F)
            .addMenuItem(new JMenuItemBuilder(openAction, KeyCode.O))
            .addMenuItem(new JMenuItemBuilder(saveAction, KeyCode.S));
    for (JMenuItem additionalMenuItem : additionalMenuItems)
      fileMenuBuilder.addMenuItem(additionalMenuItem);
    return fileMenuBuilder
        .addSeparator()
        .addMenuItem(new JMenuItemBuilder(exitAction, KeyCode.E))
        .build();
  }

  protected Image loadImage(Path mapFolder) {
    return FileHelper.newImage(mapFolder);
  }

  protected Font getLocationLabelFont(Font defaultFont) {
    return defaultFont;
  }

  protected JPanel createImagePanel(JLabel locationLabel) {
    final JPanel newImagePanel = createMainPanel();
    newImagePanel
        .addMouseMotionListener( // to show X : Y coordinates on the lower left corner of the
            // screen
            new MouseMotionAdapter() {
              @Override
              public void mouseMoved(final MouseEvent e) {
                locationLabel.setText(
                    getLocationLabelPrefix() + "x: " + e.getX() + " y: " + e.getY());
                reactToMouseMoved(e);
              }
            });
    newImagePanel.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent e) {
            reactToMouseClicked(e);
          }
        });
    // set up the image panel size dimensions ...etc
    final Dimension imageDimension = new Dimension(image.getWidth(this), image.getHeight(this));
    newImagePanel.setMinimumSize(imageDimension);
    newImagePanel.setPreferredSize(imageDimension);
    newImagePanel.setMaximumSize(imageDimension);
    return newImagePanel;
  }

  protected String getLocationLabelPrefix() {
    return "";
  }

  protected void reactToMouseMoved(MouseEvent e) {
    // intentionally empty: subclasses may override
  }

  /** Creates the main panel and returns a JPanel object. */
  protected abstract JPanel createMainPanel();

  protected abstract void reactToMouseClicked(MouseEvent e);

  protected abstract void initializeLayout();
}
