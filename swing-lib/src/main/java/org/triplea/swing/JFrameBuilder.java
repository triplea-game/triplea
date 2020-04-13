package org.triplea.swing;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import lombok.extern.java.Log;

/**
 * Provides a builder API for creating a JFrame that will include project specific defaults when
 * constructed. Defaults provided:
 *
 * <ul>
 *   <li>min size (can be changed)
 *   <li>system-menu triplea application icon (default is otherwise a generic java icon)
 *   <li>JFrame dispose on close
 * </ul>
 */
@Log
public class JFrameBuilder {
  private final Collection<Function<JFrame, Component>> children = new ArrayList<>();
  private boolean escapeClosesWindow;
  private boolean alwaysOnTop;
  private boolean pack;
  private boolean visible;

  private String title;
  @Nullable private Component parent;

  private int minWidth = 50;
  private int minHeight = 50;
  private int width;
  private int height;
  @Nullable private LayoutManager layoutManager;
  @Nullable private Runnable windowClosedAction;
  @Nullable private Runnable windowActivatedAction;

  public static JFrameBuilder builder() {
    return new JFrameBuilder();
  }

  /**
   * Constructs the JFrame instance. It will not be visible. on the other we do not set the JFrame
   * to visible explicitly for testing.
   */
  public JFrame build() {
    // note: we use the two arg JFrame constructor to avoid the headless check that is in the single
    // arg constructor.
    final JFrame frame = new JFrame(title, null);
    frame.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowActivated(final WindowEvent e) {
            Optional.ofNullable(windowActivatedAction).ifPresent(Runnable::run);
          }

          @Override
          public void windowClosed(final WindowEvent e) {
            Optional.ofNullable(windowClosedAction).ifPresent(Runnable::run);
          }
        });

    frame.setMinimumSize(new Dimension(minWidth, minHeight));
    frame.setIconImage(getGameIcon());
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

    children.stream().map(component -> component.apply(frame)).forEach(frame::add);

    if (pack) {
      frame.pack();
    }
    frame.setVisible(visible);
    return frame;
  }

  /** Returns the standard application icon typically displayed in a window's title bar. */
  public static Image getGameIcon() {
    try {
      return ImageIO.read(JFrameBuilder.class.getResource("ta_icon.png"));
    } catch (final IOException e) {
      log.log(Level.SEVERE, "ta_icon.png not loaded", e);
    }
    return null;
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
   * Adds a component to the frame, can be called multiple times and will keep appending to the
   * current frame.
   */
  public JFrameBuilder add(final Component componentToAdd) {
    add(frame -> componentToAdd);
    return this;
  }

  /**
   * Adds a component to the frame. The function parameter provides a reference to the frame that
   * will be created for the cases when for example the new component needs a 'parent-frame'
   * reference.
   */
  public JFrameBuilder add(final Function<JFrame, Component> componentToAdd) {
    children.add(componentToAdd);
    return this;
  }

  /** Adds an action that will be executed when the frame is closed. */
  public JFrameBuilder windowClosedAction(final Runnable windowClosedAction) {
    this.windowClosedAction = windowClosedAction;
    return this;
  }

  /** Adds an action that will be executed when the frame is activated. */
  public JFrameBuilder windowActivatedAction(final Runnable windowActivatedAction) {
    this.windowActivatedAction = windowActivatedAction;
    return this;
  }

  public JFrameBuilder pack() {
    this.pack = true;
    return this;
  }

  public JFrameBuilder visible(final boolean visible) {
    this.visible = visible;
    return this;
  }
}
