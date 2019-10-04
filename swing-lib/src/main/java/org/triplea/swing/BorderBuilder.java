package org.triplea.swing;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/** Presents a builder API for creating a swing Border object. */
public final class BorderBuilder {

  private int top;
  private int bottom;
  private int left;
  private int right;

  private BorderBuilder() {}

  public static BorderBuilder builder() {
    return new BorderBuilder();
  }

  public Border build() {
    return new EmptyBorder(top, left, bottom, right);
  }

  public BorderBuilder top(final int size) {
    top = size;
    return this;
  }

  public BorderBuilder left(final int size) {
    left = size;
    return this;
  }

  public BorderBuilder bottom(final int size) {
    bottom = size;
    return this;
  }

  public BorderBuilder right(final int size) {
    right = size;
    return this;
  }
}
