package org.triplea.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import javax.swing.JFrame;

import games.strategy.engine.framework.GameRunner;
import games.strategy.ui.SwingComponents;

/**
 * Provides a builder API for creating a JFrame that will include project specific defaults when constructed.
 * Defaults provided:
 * <ul>
 * <li>min size (can be changed)</li>
 * <li>system-menu triplea application icon (default is otherwise a generic java icon)</li>
 * <li>JFrame dispose on close</li>
 * </ul>
 */
public class JFrameBuilder {
  private final Collection<Component> children = new ArrayList<>();
  private boolean escapeClosesWindow;
  private boolean alwaysOnTop;
  private String title;
  private Component parent;

  private int minWidth = 50;
  private int minHeight = 50;
  private int width;
  private int height;

  private LayoutManager layoutManager;

  public static JFrameBuilder builder() {
    return new JFrameBuilder();
  }

  /**
   * Constructs the JFrame instance. It will not be visible.
   * on the other we do not set the JFrame to visible explicitly for testing.
   */
  public JFrame build() {
    // note: we use the two arg JFrame constructor to avoid the headless check that is in the single arg constructor.
    final JFrame frame = new JFrame(title, null);
    frame.setMinimumSize(new Dimension(minWidth, minHeight));

    if ((width > 0) || (height > 0)) {
      frame.setPreferredSize(new Dimension(width, height));
      frame.setSize(new Dimension(width, height));
    }

    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setIconImage(GameRunner.getGameIcon(frame));

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

  // TODO: unit-test this.
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
}
