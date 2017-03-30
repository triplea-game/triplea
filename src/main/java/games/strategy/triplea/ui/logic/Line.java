package games.strategy.triplea.ui.logic;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Framework independent Line class
 */
public class Line {

  private double x1;
  private double y1;
  private double x2;
  private double y2;

  public Line(double x1, double y1, double x2, double y2) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }

  public Line(Point2D point1, Point2D point2) {
    this(point1.getX(), point1.getY(), point2.getX(), point2.getY());
  }

  public Line(Point point1, Point point2) {
    this(point1.getX(), point1.getY(), point2.getX(), point2.getY());
  }

  public double getX1() {
    return x1;
  }

  public void setX1(double x1) {
    this.x1 = x1;
  }

  public double getY1() {
    return y1;
  }

  public void setY1(double y1) {
    this.y1 = y1;
  }

  public double getX2() {
    return x2;
  }

  public void setX2(double x2) {
    this.x2 = x2;
  }

  public double getY2() {
    return y2;
  }

  public void setY2(double y2) {
    this.y2 = y2;
  }

  public Point getP1() {
    return new Point(x1, y1);
  }

  public Point getP2() {
    return new Point(x2, y2);
  }

  public Line2D toLine2D() {
    return new Line2D.Double(x1, y1, x2, y2);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Line) {
      Line line = (Line) o;
      return line.x1 == x1 && line.x2 == x2 && line.y1 == y1 && line.y2 == y2;
    }
    return false;
  }
}
