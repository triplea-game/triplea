package org.triplea.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.MediaTracker;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;

import javax.annotation.Nullable;
import javax.swing.JFrame;

import lombok.extern.java.Log;

/**
 * Provides a builder API for creating a JFrame that will include project specific defaults when constructed.
 * Defaults provided:
 * <ul>
 * <li>min size (can be changed)</li>
 * <li>system-menu triplea application icon (default is otherwise a generic java icon)</li>
 * <li>JFrame dispose on close</li>
 * </ul>
 */
@Log
public class JFrameBuilder {
  private final Collection<Component> children = new ArrayList<>();
  private boolean escapeClosesWindow;
  private boolean alwaysOnTop;
  private String title;
  @Nullable
  private Component parent;

  private int minWidth = 50;
  private int minHeight = 50;
  private int width;
  private int height;
  @Nullable
  private LayoutManager layoutManager;
  @Nullable
  private Runnable windowCloseAction;

  @Nullable
  private Runnable windowActivatedAction;

  public static JFrameBuilder builder() {
    return new JFrameBuilder();
  }

  /**
   * Constructs the JFrame instance. It will not be visible.
   * on the other we do not set the JFrame to visible explicitly for testing.
   */
  public JFrame build() {
    // note: we use the two arg JFrame constructor to avoid the headless check that is in the single arg constructor.
    final JFrame frame = new JFrame(title, null) {

      @Override
      public void dispose() {
        // Note: we override dispose method instead of using a WindowStateListener to allow for testing,
        // the overrided dispose method is always called in headed or headless environment.
        super.dispose();
        Optional.ofNullable(windowCloseAction).ifPresent(Runnable::run);
      }
    };
    Optional.ofNullable(windowActivatedAction)
        .ifPresent(action -> frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowActivated(final WindowEvent e) {
            windowActivatedAction.run();
          }
        }));

    frame.setMinimumSize(new Dimension(minWidth, minHeight));
    frame.setIconImage(getGameIcon(frame));
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    if ((width > 0) || (height > 0)) {
      frame.setPreferredSize(new Dimension(width, height));
      frame.setSize(new Dimension(width, height));
    }

    if (escapeClosesWindow) {
      SwingComponents.addEscapeKeyListener(frame, frame::dispose);
    }
    if (alwaysOnTop) {
      frame.setAlwaysOnTop(true);
    }
    Optional.ofNullable(parent).ifPresent(frame::setLocationRelativeTo);
    Optional.ofNullable(layoutManager).ifPresent(frame.getContentPane()::setLayout);

    children.forEach(frame::add);

    return frame;
  }

  /**
   * Returns the standard application icon typically displayed in a window's title bar.
   */
  public static Image getGameIcon(final Window frame) {
    Image img = null;
    try {
      img = frame.getToolkit().getImage(JFrameBuilder.class.getResource("ta_icon.png"));
    } catch (final Exception ex) {
      log.log(Level.SEVERE, "ta_icon.png not loaded", ex);
    }
    final MediaTracker tracker = new MediaTracker(frame);
    tracker.addImage(img, 0);
    try {
      tracker.waitForAll();
    } catch (final InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    return img;
  }

  public JFrameBuilder escapeKeyClosesFrame() {
    this.escapeClosesWindow = true;
    return this;
  }

  public JFrameBuilder locateRelativeTo(final Component parent) {
    this.parent = parent;
    return this;
  }

  public JFrameBuilder title(final String title) {
    this.title = title;
    return this;
  }

  public JFrameBuilder layout(final LayoutManager layoutManager) {
    this.layoutManager = layoutManager;
    return this;
  }

  public JFrameBuilder minSize(final int minWidth, final int minHeight) {
    this.minWidth = minWidth;
    this.minHeight = minHeight;
    return this;
  }

  public JFrameBuilder size(final int width, final int height) {
    this.width = width;
    this.height = height;
    return this;
  }

  public JFrameBuilder alwaysOnTop() {
    alwaysOnTop = true;
    return this;
  }

  /**
   * Adds a component to the frame, can be called multiple times and will keep appending to the current frame.
   */
  public JFrameBuilder add(final Component componentToAdd) {
    children.add(componentToAdd);
    return this;
  }

  /**
   * Adds an action that will be executed when the frame is closed.
   */
  public JFrameBuilder windowCloseAction(final Runnable windowAction) {
    this.windowCloseAction = windowAction;
    return this;
  }


  /**
   * Adds an action that will be executed when the frame is activated\.
   */
  public JFrameBuilder windowActivatedAction(final Runnable windowActivatedAction) {
    this.windowActivatedAction = windowActivatedAction;
    return this;
  }
}
