package games.strategy.triplea.ui.logic;

import java.awt.geom.Point2D;

/**
 * Framework independent point class.
 */
public class Point {
  private double x;
  private double y;

  public Point() {
    this(0, 0);
  }

  public Point(final double x, final double y) {
    this.x = x;
    this.y = y;
  }

  public Point(final Point2D point) {
    this(point.getX(), point.getY());
  }

  public double getX() {
    return x;
  }

  public void setX(final double x) {
    this.x = x;
  }

  public double getY() {
    return y;
  }

  public void setY(final double y) {
    this.y = y;
  }

  public double distance(final Point point) {
    return Math.sqrt(Math.pow(this.getX() - point.getX(), 2) + Math.pow(this.getY() - point.getY(), 2));
  }

  public java.awt.Point toPoint() {
    return new java.awt.Point((int) x, (int) y);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof Point) {
      final Point point = (Point) o;
      return point.x == this.x && point.y == this.y;
    }
    return false;
  }
}
