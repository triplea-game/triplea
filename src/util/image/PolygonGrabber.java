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


package util.image;

import java.util.*;
import java.util.List;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import javax.swing.*;

import games.strategy.ui.Util;
import games.strategy.util.*;
import java.io.*;
import java.awt.event.*;

/**
 *   Utility to break a map into polygons.
*    Not pretty, meant only for one time use.
*
*   Inputs - a map with 1 pixel wide borders
*           - a list of centers - this is used to guess the territory name and to verify the
*           - territory name entered
*   Outputs - a list of polygons for each country
 */

public class PolygonGrabber extends JFrame
{

        private static Map s_centers;
        static
        {
            try
            {
                s_centers = PointFileReaderWriter.readOneToOne(
                    new FileInputStream(
                    "/home/sgb/dev/triplea/data/games/strategy/triplea/ui/new_centers.txt"
     ));
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                System.exit(0);
            }
        }

     private Image m_image;

     //maps String -> List of polygons
     private Map m_polygons = new HashMap();

     private BufferedImage m_bufferedImage;
     private JLabel location = new JLabel();

     //the current set of polyongs
     private List m_current;

    public PolygonGrabber(String fileName)
    {
        super("Polygon gragger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        createImage(fileName);

        JPanel imagePanel = createMainPanel();

        imagePanel.addMouseMotionListener(
        new MouseMotionAdapter()
        {
             public void mouseMoved(MouseEvent e)
             {
                 location.setText("x:"+ e.getX() + " y:" + e.getY());
             }
        }
        );

        imagePanel.addMouseListener(
        new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                mouseEvent(e.getPoint(), e.isControlDown(), SwingUtilities.isRightMouseButton(e));
            }
        }

        );

        imagePanel.setMinimumSize(new Dimension( m_image.getWidth(this), m_image.getHeight(this)));
        imagePanel.setPreferredSize(new Dimension( m_image.getWidth(this), m_image.getHeight(this)));
        imagePanel.setMaximumSize(new Dimension( m_image.getWidth(this), m_image.getHeight(this)));

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(new JScrollPane(imagePanel),  BorderLayout.CENTER);
        this.getContentPane().add(location, BorderLayout.SOUTH);

        JToolBar toolBar = new JToolBar();
        toolBar.add(new AbstractAction("Save")
        {
            public void actionPerformed(ActionEvent e)
            {
                save();
            }
        }
        );

        toolBar.add(new AbstractAction("Load")
       {
           public void actionPerformed(ActionEvent e)
           {
               load();
           }
       }
       );


        this.getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    private JPanel createMainPanel()
    {
        JPanel imagePanel = new JPanel()
        {
            public void paint(Graphics g)
            {
                //super.paint(g);
                g.drawImage(m_image, 0,0, this);

                g.setColor(Color.black);
                Iterator iter = m_polygons.entrySet().iterator();
                while (iter.hasNext())
                {
                    Collection polygons = (Collection) ( (Map.Entry) iter.next()).getValue();
                    Iterator iter2 = polygons.iterator();
                    while (iter2.hasNext())
                    {
                        Polygon item = (Polygon)  iter2.next();
                        g.fillPolygon(item.xpoints, item.ypoints, item.npoints);
                    }
                }
                g.setColor(Color.red);
                if(m_current != null)
                {
                    Iterator currentIter = m_current.iterator();
                    while (currentIter.hasNext())
                    {
                        Polygon item = (Polygon) currentIter.next();
                        g.fillPolygon(item.xpoints, item.ypoints, item.npoints);
                    }
                }
            }
        };
        return imagePanel;
    }

    private void createImage(String fileName)
    {
        m_image = Toolkit.getDefaultToolkit().createImage(fileName);

        try
        {
            Util.ensureImageLoaded(m_image, this);
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }

        m_bufferedImage = new BufferedImage(m_image.getWidth(null), m_image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        m_bufferedImage.getGraphics().drawImage(m_image, 0,0, this);
    }

    private void load()
    {
        try
        {
            String fileName = JOptionPane.showInputDialog(this, "enter name to load");
            if(fileName.trim().length() == 0)
                return;
            FileInputStream in = new FileInputStream(fileName);
            m_polygons = PointFileReaderWriter.readOneToManyPolygons(in);
            repaint();
        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (HeadlessException ex)
        {
            ex.printStackTrace();
        }
    }

    private void save()
    {
        try
        {
            String fileName = JOptionPane.showInputDialog(this, "enter name to save");
            if(fileName.trim().length() == 0)
                return;
            FileOutputStream out = new FileOutputStream(fileName);
            PointFileReaderWriter.writeOneToManyPolygons(out, m_polygons);
            out.flush();
            out.close();
            System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        }
        catch (HeadlessException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    public static void main(String[] args)
    {
        String fileName = "/home/sgb/dev/triplea/baseMap.gif";
        PolygonGrabber grabber = new PolygonGrabber(fileName);
        grabber.setSize(600,550);
        grabber.show();
    }

    private void mouseEvent(Point point, boolean ctrlDown, boolean rightMouse)
    {
        Polygon p =findPolygon(point.x, point.y);
        if(p == null)
            return;
        if(rightMouse && m_current != null)
        {
            doneCurrentGroup();
        }
        else if(pointInCurrentPolygon(point))
        {
            System.out.println("rejecting");
            return;
        }
        else if(ctrlDown)
        {


            if(m_current == null)
                m_current = new ArrayList();
            m_current.add(p);
        }
        else
        {
            m_current = new ArrayList();
            m_current.add(p);
        }
        repaint();
    }

    private boolean pointInCurrentPolygon(Point p)
    {
        if(m_current == null)
            return false;
        Iterator iter = m_current.iterator();
        while (iter.hasNext()) {
            Polygon item = (Polygon)iter.next();
            if(item.contains(p))
                return true;
        }
        return false;
    }

    private void doneCurrentGroup() throws HeadlessException
    {
        JTextField text = new JTextField();
        Iterator centersiter = s_centers.entrySet().iterator();

        guessCountryName(text, centersiter);
        JOptionPane.showMessageDialog(this, text);
        if(!s_centers.keySet().contains(text.getText()))
        {
            //not a valid name
            JOptionPane.showMessageDialog(this, "not a valid name");
            return;
        }
        m_polygons.put(text.getText(), m_current);
        m_current = null;
    }

    /**
     * Guess the country name based on the location of the previous centers
     **/
    private void guessCountryName(JTextField text, Iterator centersiter)
    {
        while (centersiter.hasNext()) {
            Map.Entry item = (Map.Entry)centersiter.next();
            Point p = new Point((Point) item.getValue());

            Iterator currentIter = m_current.iterator();
            while (currentIter.hasNext()) {
                Polygon polygon = (Polygon)currentIter.next();
                if(polygon.contains(p))
                {
                    text.setText(item.getKey().toString());
                    break;
                }
            }



        }
    }



    private final boolean inBounds(int x, int y)
    {
        return x >= 0 && x < m_image.getWidth(null) && y >= 0 && y < m_image.getHeight(null);
    }

    private final boolean isBlack(Point p)
    {
        return isBlack(p.x, p.y);
    }

    private final boolean isBlack(int x, int y)
    {
        if(!inBounds(x,y))
            return false;
        return (m_bufferedImage.getRGB(x,y) & 0x00FFFFFF) == 0;
    }

    /**
     * Directions
     * 0 - North
     * 1 North east
     * 2 East
     * 3 South east
     * 4 South
     * 5 South west
     * 6 West
     * 7 North west
     */
    private final void move(Point p, int direction)
    {
        if(direction < 0 || direction > 7)
            throw new IllegalArgumentException("Not a direction :" + direction);

        if(direction == 1 || direction ==2 || direction == 3)
            p.x++;
        else if(direction == 5 || direction == 6 || direction == 7)
            p.x--;

        if(direction == 5 || direction ==4 || direction == 3)
            p.y++;
        else if(direction == 7 || direction == 0 || direction == 1)
            p.y--;

    }



    private Point m_testPoint = new Point();

    private boolean isOnEdge(int direction, Point currentPoint)
    {
        m_testPoint.setLocation(currentPoint);
        move(m_testPoint, direction);
        return isBlack(m_testPoint);
    }

    private final Polygon findPolygon(final int x, final int y)
    {
        //walk up, find the first black point
        Point startPoint = new Point(x,y);
        while( inBounds(startPoint.x, startPoint.y -1) && !isBlack(startPoint.x, startPoint.y))
        {
            startPoint.y--;
        }

        List points = new ArrayList(100);
        points.add( new Point(startPoint));

        int currentDirection = 2;
        Point currentPoint = new Point(startPoint);

        int iterCount = 0;
        
        while(!currentPoint.equals(startPoint) || points.size() == 1)
        {
            iterCount ++;
            
            if(iterCount > 100000)
            {
                JOptionPane.showMessageDialog(this, "FAIL" + currentPoint);
                return null;
            }
            int tempDirection;
            for(int i = 2; i>=-3; i--)
            {
                tempDirection = (currentDirection + i) % 8;
                if(tempDirection < 0)
                    tempDirection +=8;

                if(isOnEdge(tempDirection, currentPoint))
                {
                    //if we need to change our course
                    if(i != 0)
                    {
                        points.add(currentPoint);
                        currentPoint = new Point(currentPoint);
                        move(currentPoint, tempDirection);
                        currentDirection = tempDirection;
                    }
                    else
                        move(currentPoint, currentDirection);
                    break;
                }
            }

        }

        int[] xpoints = new int[points.size()];
        int[] ypoints = new int[points.size()];
        int i = 0;
        Iterator iter = points.iterator();
        while (iter.hasNext())
        {
            Point item = (Point)iter.next();
            xpoints[i] = item.x;
            ypoints[i] = item.y;
            i++;
        }

        System.out.println("Done finding polygon. total points;" +xpoints.length);
        return new Polygon(xpoints, ypoints, xpoints.length);
    }
}
