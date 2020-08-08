package org.triplea.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

/** Class to provide a builder API for defining a {@code MouseListener}. */
public class MouseListenerBuilder {

  private Consumer<MouseEvent> mouseClicked = e -> {};
  private Consumer<MouseEvent> mousePressed = e -> {};
  private Consumer<MouseEvent> mouseEntered = e -> {};
  private Consumer<MouseEvent> mouseReleased = e -> {};
  private Consumer<MouseEvent> mouseExited = e -> {};

  public MouseListener build() {
    return new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        mouseClicked.accept(e);
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        mousePressed.accept(e);
      }

      @Override
      public void mouseReleased(final MouseEvent e) {
        mouseReleased.accept(e);
      }

      @Override
      public void mouseEntered(final MouseEvent e) {
        mouseEntered.accept(e);
      }

      @Override
      public void mouseExited(final MouseEvent e) {
        mouseExited.accept(e);
      }
    };
  }

  public MouseListenerBuilder mouseClicked(final Consumer<MouseEvent> mouseClicked) {
    this.mouseClicked = mouseClicked;
    return this;
  }

  public MouseListenerBuilder mouseClicked(final Runnable mouseClicked) {
    this.mouseClicked = e -> mouseClicked.run();
    return this;
  }

  public MouseListenerBuilder mousePressed(final Consumer<MouseEvent> mousePressed) {
    this.mousePressed = mousePressed;
    return this;
  }

  public MouseListenerBuilder mousePressed(final Runnable mousePressed) {
    this.mousePressed = e -> mousePressed.run();
    return this;
  }

  public MouseListenerBuilder mouseReleased(final Consumer<MouseEvent> mouseReleased) {
    this.mouseReleased = mouseReleased;
    return this;
  }

  public MouseListenerBuilder mouseReleased(final Runnable mouseReleased) {
    this.mouseReleased = e -> mouseReleased.run();
    return this;
  }

  public MouseListenerBuilder mouseEntered(final Consumer<MouseEvent> mouseEntered) {
    this.mouseEntered = mouseEntered;
    return this;
  }

  public MouseListenerBuilder mouseEntered(final Runnable mouseEntered) {
    this.mouseEntered = e -> mouseEntered.run();
    return this;
  }

  public MouseListenerBuilder mouseExited(final Consumer<MouseEvent> mouseExited) {
    this.mouseExited = mouseExited;
    return this;
  }

  public MouseListenerBuilder mouseExited(final Runnable mouseExited) {
    this.mouseExited = e -> mouseExited.run();
    return this;
  }
}
