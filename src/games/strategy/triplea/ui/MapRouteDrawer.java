/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Logic to draw a route on a map.
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.ui.ImageScrollerLargeView;
import java.util.*;
import java.util.List;
import java.awt.geom.*;

/**
 *   Draws a route on a map.
 *   This code is really ugly, bad and it barely works.
 *   It should be rewritten.
 */

public class MapRouteDrawer
{
    //only static methods
    private MapRouteDrawer() {};

    /**
     * Draw m_route to the screen, do nothing if null.
     */
    public static void drawRoute(Graphics2D graphics, Route m_route, ImageScrollerLargeView view)
    {
      if(m_route == null)
        return;

      graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      graphics.setPaint(Color.red);

      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      Territory current = m_route.getStart();

      Point currentStart =  TerritoryData.getInstance().getCenter(current);

      Point previousPoint = null;
      Point currentFinish = null;

      if(m_route.getLength() > 0)
          currentFinish = TerritoryData.getInstance().getCenter( m_route.at(0));

      Point nextPoint = null;
      if(m_route.getLength() > 1)
          nextPoint = TerritoryData.getInstance().getCenter( m_route.at(1));

      int yOffset = view.getYOffset();
      int xOffset = view.getXOffset();

      List shapes = new ArrayList();


      for(int i = 0; i < m_route.getLength(); i++)
      {
          if(i < m_route.getLength())
          {
              Ellipse2D oval = new Ellipse2D.Double(currentStart.x - 3 - xOffset,
                  currentStart.y - yOffset - 3, 6, 6);

              shapes.add(oval);
          }

          //correct for the map going over the edge
          if (Math.abs(currentFinish.x - currentStart.x) > view.getImageWidth() / 2)
          {
            currentFinish = new Point(currentFinish);
            if (currentFinish.x > currentStart.x)
            {
              currentFinish.x -= view.getImageWidth();
            }
            else
            {
              currentFinish.x += view.getImageWidth();
            }
          }




          if (nextPoint != null)
              drawCurvedLineWithNextPoint(graphics, currentStart.x - xOffset,
                                          currentStart.y - yOffset, currentFinish.x - xOffset,
                                          currentFinish.y - yOffset, nextPoint.x - xOffset,
                                          nextPoint.y - yOffset, shapes);
//        else if (previousPoint != null)
//            drawCurvedLineWithNextPoint(graphics, currentFinish.x,
//                                        currentFinish.y, currentStart.x,
//                                        currentStart.y, previousPoint.x,
//                                        previousPoint.y);
          else
              drawLineSegment(graphics, currentStart.x - xOffset, currentStart.y - yOffset,
                              currentFinish.x - xOffset, currentFinish.y - yOffset, shapes);

        previousPoint = currentStart;
        currentStart = currentFinish;
        currentFinish = nextPoint;
        if(m_route.getLength() > i + 2)
            nextPoint = TerritoryData.getInstance().getCenter( m_route.at(i + 2 ));
        else
            nextPoint = null;

      }


      for(int i = 0; i < shapes.size(); i ++)
      {
        Shape shape = (Shape) shapes.get(i);

        drawWithTranslate(graphics, shape, 0);
        int translate = -view.getImageWidth();
        drawWithTranslate(graphics, shape, translate);
        drawWithTranslate(graphics, shape, -translate);
      }

    }

    private static void drawWithTranslate(Graphics2D graphics, Shape shape, int translate)
    {
      if(shape instanceof Ellipse2D.Double)
      {
        Ellipse2D.Double elipse = (Ellipse2D.Double) shape;
        elipse = new Ellipse2D.Double(elipse.x + translate, elipse.y, elipse.width, elipse.height);
        graphics.draw(elipse);
      }
      if(shape instanceof Polygon)
      {
        ((Polygon) shape).translate(translate, 0);
        graphics.fill(shape);

        ((Polygon) shape).translate(-translate, 0);
      }
      if(shape instanceof Line2D)
      {
         Line2D  line = (Line2D) shape;
         Point2D p1 = new Point2D.Double( line.getP1().getX() + translate , line.getP1().getY());
         Point2D p2 = new Point2D.Double( line.getP2().getX() + translate , line.getP2().getY());
         graphics.draw(new Line2D.Double(p1,p2));
      }
      if(shape instanceof QuadCurve2D)
      {
        QuadCurve2D.Double curve = (QuadCurve2D.Double) shape;
        curve = new QuadCurve2D.Double(curve.x1 + translate, curve.y1,
                                       curve.ctrlx + translate, curve.ctrly ,
                                       curve.x2 + translate, curve.y2
                                       );
        graphics.draw(curve);

      }
    }


    /**
     * (x,y) - the first point to draw from
     * (xx, yy) - the point to draw too
     * (xxx, yyy) - the next point that the line segment will be drawn to
     */
    private static void drawCurvedLineWithNextPoint(Graphics2D graphics, int x, int y, int xx, int yy, int xxx, int yyy, List shapes)
    {
        final int maxControlLength = 150;
        int controlDiffx = xx - xxx;
        int controlDiffy = yy - yyy;

        if( Math.abs(controlDiffx) > maxControlLength || Math.abs(controlDiffy) > maxControlLength)
        {
            double ratio = 0.0;
            try
            {
                ratio = Math.abs(controlDiffx / controlDiffy);
            }
            catch (ArithmeticException ex)
            {
                ratio = 1000;
            }

            if( Math.abs(controlDiffx) >  Math.abs(controlDiffy))
            {
                controlDiffx = controlDiffx < 0 ?  -maxControlLength : maxControlLength;
                controlDiffy = controlDiffy < 0 ?  (int) (-maxControlLength / ratio) : (int) (maxControlLength / ratio);
            }
            else
            {
                controlDiffy = controlDiffy < 0 ?  -maxControlLength : maxControlLength;
                controlDiffx = controlDiffx < 0 ?  (int) (-maxControlLength * ratio) : (int) (maxControlLength * ratio);

            }
//          controlDiffx = controlDiffx < 0 ?  -maxControlLength : maxControlLength;
//          controlDiffx *= ration;
//          controlDiffy = controlDiffy < 0 ? -maxControlLength ; max
//          controlDiffy *= ratio;
        }



        int controlx = xx + controlDiffx;
        int controly = yy + controlDiffy;

        QuadCurve2D.Double curve = new QuadCurve2D.Double(x,y,controlx, controly, xx,yy);
        shapes.add(curve);
    }

    //http://www.experts-exchange.com/Programming/Programming_Languages/Java/Q_20627343.html
    private static void drawLineSegment( Graphics2D graphics, int x, int y, int xx, int yy, List shapes)
    {
      float arrowWidth = 12.0f ;
      float theta = 0.7f ;
      int[] xPoints = new int[ 3 ] ;
      int[] yPoints = new int[ 3 ] ;
      float[] vecLine = new float[ 2 ] ;
      float[] vecLeft = new float[ 2 ] ;
      float fLength;
      float th;
      float ta;
      float baseX, baseY ;

      xPoints[ 0 ] = xx ;
      yPoints[ 0 ] = yy ;

      // build the line vector
      vecLine[ 0 ] = (float)xPoints[ 0 ] - x ;
      vecLine[ 1 ] = (float)yPoints[ 0 ] - y ;

      // build the arrow base vector - normal to the line
      vecLeft[ 0 ] = -vecLine[ 1 ] ;
      vecLeft[ 1 ] = vecLine[ 0 ] ;

      // setup length parameters
      fLength = (float)Math.sqrt( vecLine[0] * vecLine[0] + vecLine[1] * vecLine[1] ) ;
      th = arrowWidth / ( 2.0f * fLength ) ;
      ta = arrowWidth / ( 2.0f * ( (float)Math.tan( theta ) / 2.0f ) * fLength ) ;

      // find the base of the arrow
      baseX = ( (float)xPoints[ 0 ] - ta * vecLine[0]);
      baseY = ( (float)yPoints[ 0 ] - ta * vecLine[1]);

      // build the points on the sides of the arrow
      xPoints[ 1 ] = (int)( baseX + th * vecLeft[0] );
      yPoints[ 1 ] = (int)( baseY + th * vecLeft[1] );
      xPoints[ 2 ] = (int)( baseX - th * vecLeft[0] );
      yPoints[ 2 ] = (int)( baseY - th * vecLeft[1] );

      //draw an arrow
      Shape line = new Line2D.Double(x, y, (int) baseX, (int) baseY);
      shapes.add(line);

      Polygon poly = new Polygon(xPoints, yPoints, 3);
      shapes.add(poly);
    }

}
