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

import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import javax.swing.*;

import games.strategy.ui.Util;
import games.strategy.util.*;


/** old comments
 *
 *   Utility to break a map into polygons.
 *   Not pretty, meant only for one time use.
 *
 *   Inputs - a map with 1 pixel wide borders
 *           - a list of centers - this is used to guess the territory name and to verify the
 *           - territory name entered
 *   Outputs - a list of polygons for each country
 */

public class PolygonGrabber extends JFrame
{

    private List m_current;                   // the current set of polyongs
    private Image m_image;                    // holds the map image
    private BufferedImage m_bufferedImage;
    private Map m_polygons  = new HashMap();  // maps String -> List of polygons
    private Map m_centers;                    // holds the centers for the polygons
    private JLabel location = new JLabel();

    
    /**
       main(java.lang.String[])
       
       Main program begins here.
       Asks the user to select the map then runs the
       the actual polygon grabber program.
       
       @param java.lang.String[] args  the command line arguments

       @see PolygonGrabber(java.lang.String) picker
    */
    public static void main(String[] args)
    {
        System.out.println("Select the map");
        String mapName = new FileOpen("Select The Map").getPathString();
        
	if(mapName != null)
	{
	    System.out.println("Map : "+mapName);
	
            PolygonGrabber grabber = new PolygonGrabber(mapName);
            grabber.setSize(600,550);
            grabber.show();
	}
	else {
	    System.out.println("No Image Map Selected. Shutting down.");
            System.exit(0);
	}
    
    }//end main
    
    
    
    /**
       Constructor PolygonGrabber(java.lang.String)
       
       Asks user to specify a file with center points. If not
       program will exit. We setup the mouse listenrs and toolbars
       and load the actual image of the map here.
       
       @param java.lang.String mapName  path to image map
    */
    public PolygonGrabber(String mapName)
    {
        super("Polygon gragger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try
        {
	    System.out.println("Select the Centers file");
	    String centerPath = new FileOpen("Select A Center File").getPathString();
	    
	    if(centerPath != null)
	    {
	        System.out.println("Centers : "+centerPath);
                m_centers = PointFileReaderWriter.readOneToOne(new FileInputStream(centerPath));
            }
	    else
	    {
	        System.out.println("You must specify a centers file.");
		System.out.println("Shutting down.");
		System.exit(0);
	    }
	}
        catch (IOException ex1)
        {
            ex1.printStackTrace();
	    System.exit(0);
        }

        createImage(mapName);

        JPanel imagePanel = createMainPanel();


        /*
	   Add a mouse listener to show
	   X : Y coordinates on the lower
	   left corner of the screen.
        */
        imagePanel.addMouseMotionListener(
            new MouseMotionAdapter()
            {
                 public void mouseMoved(MouseEvent e)
                 {
                     location.setText("x:"+ e.getX() + " y:" + e.getY());
                 }
            }
        );


        /*
           Add a mouse listener to monitor
	   for right mouse button being
	   clicked.	
        */
        imagePanel.addMouseListener(
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    mouseEvent(e.getPoint(), e.isControlDown(), SwingUtilities.isRightMouseButton(e));
                }
            }
        );


        //set up the image panel size dimensions ...etc

        imagePanel.setMinimumSize(new Dimension( m_image.getWidth(this), m_image.getHeight(this)));
        imagePanel.setPreferredSize(new Dimension( m_image.getWidth(this), m_image.getHeight(this)));
        imagePanel.setMaximumSize(new Dimension( m_image.getWidth(this), m_image.getHeight(this)));

        //set up the layout manager
	
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(new JScrollPane(imagePanel),  BorderLayout.CENTER);
        this.getContentPane().add(location, BorderLayout.SOUTH);

        JToolBar toolBar = new JToolBar();
	
	
        /*
	   Add a save button
        */
        toolBar.add(new AbstractAction("Save Polygons")
        {
            public void actionPerformed(ActionEvent e)
            {
                savePolygons();
            }
        });


        /*
	   Add a load button
        */
        toolBar.add(new AbstractAction("Load Polygons")
        {
           public void actionPerformed(ActionEvent e)
           {
               loadPolygons();
           }
        });


        /*
	   Add an exit button
        */
        toolBar.add(new AbstractAction("Exit")
        {
           public void actionPerformed(ActionEvent e)
           {
               System.exit(0);
           }
        });

       this.getContentPane().add(toolBar, BorderLayout.NORTH);

    }//end constructor



    /**
       createImage(java.lang.String)
       
       We create the image of the map here and
       assure that it is loaded properly.  

       @param java.lang.String mapName  the path of the image map
    */
    private void createImage(String mapName)
    {
        m_image = Toolkit.getDefaultToolkit().createImage(mapName);

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



    /**
       javax.swing.JPanel createMainPanel()
       
       Creates a JPanel to be used.
       
       @return javax.swing.JPanel  the newly create panel
    */
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
                    
		    }//while
                
		}//while
		
                g.setColor(Color.red);
		
                if(m_current != null)
                {
                    Iterator currentIter = m_current.iterator();
                    while (currentIter.hasNext())
                    {
                        Polygon item = (Polygon) currentIter.next();
                        g.fillPolygon(item.xpoints, item.ypoints, item.npoints);
                    
		    }//while
                
		}//if
		
            }//paint
        };
	
        return imagePanel;
    }



    /**
       savePolygons()
       
       Saves the polygons to disk.
    */
    private void savePolygons()
    {
        try
        {
            String polyName = JOptionPane.showInputDialog(this, "Save Polygon File As");

	    if(polyName == null)
            {
	        return;
            }
	    
            FileOutputStream out = new FileOutputStream(polyName);
            PointFileReaderWriter.writeOneToManyPolygons(out, m_polygons);
            out.flush();
            out.close();
	    
            System.out.println("Data written to :" + new File(polyName).getCanonicalPath());
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



    /**
       loadPolygons()
       
       Loads a pre-defined file with map polygon points.
    */
    private void loadPolygons()
    {
        try
        {
            System.out.println("Load a polygon file");
            
	    String polyName = new FileOpen("Load A Polygon File").getPathString();
	    
	    if(polyName == null)
	    {
	        return;
            }
	    
            FileInputStream in = new FileInputStream(polyName);
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


    /**
       mouseEvent(java.awt.Point, java.lang.boolean, java.lang.boolean)
       
       @param java.awt.Point    point       a point clicked by mouse
       @param java.lang.boolean ctrlDown    true if ctrl key was hit
       @param java.lang.boolean rightMouse  true if the right mouse button was hit
    */
    private void mouseEvent(Point point, boolean ctrlDown, boolean rightMouse)
    {
        Polygon p = findPolygon(point.x, point.y);
	
        if(p == null)
	{
            return;
        }
	
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
	    {
                m_current = new ArrayList();
            }
	    m_current.add(p);
        }
        else
        {
            m_current = new ArrayList();
            m_current.add(p);
        }
        repaint();
    }


    /**
       java.lang.boolean pointInCurrentPolygon(java.awt.Point)
       
       returns false if there is no points in a current polygon.
       returns true if there is.
       
       @param java.awt.Point p  the point to check for
       
       @return java.lang.boolean
    */
    private boolean pointInCurrentPolygon(Point p)
    {
        if(m_current == null)
	{
            return false;
        }
	
	Iterator iter = m_current.iterator();
        
	while (iter.hasNext()) {
            Polygon item = (Polygon)iter.next();
            
	    if(item.contains(p))
	    {
                return true;
	    }
	}
	
        return false;
    }



    /**
       doneCurrentGroup()
       
       Does something with respect to check if the name
       of a territory is valid or not.
       
       @throws HeadlessException
    
    */
    private void doneCurrentGroup() throws HeadlessException
    {
        JTextField text = new JTextField();
        Iterator centersiter = m_centers.entrySet().iterator();

        guessCountryName(text, centersiter);
        JOptionPane.showMessageDialog(this, text);
	
        if(!m_centers.keySet().contains(text.getText()))
        {
            //not a valid name
            JOptionPane.showMessageDialog(this, "not a valid name");
            return;
        }
	
        m_polygons.put(text.getText(), m_current);
        m_current = null;
    }



    /**
       guessCountryName(javax.swing.JTextField, java.util.Iterator)
       
       Guess the country name based on the location of the previous centers
       
       @param javax.swing.JTextField text         text dialog
       @param java.util.Iterator     centersiter  center iterator
    */
    private void guessCountryName(JTextField text, Iterator centersiter)
    {
        while (centersiter.hasNext())
	{
            Map.Entry item = (Map.Entry)centersiter.next();
            Point p = new Point((Point) item.getValue());

            Iterator currentIter = m_current.iterator();
            
	    while (currentIter.hasNext())
	    {
                Polygon polygon = (Polygon)currentIter.next();
                
		if(polygon.contains(p))
                {
                    text.setText(item.getKey().toString());
                    break;
		    
                }//if
		
            }//while
	    
        }//while
    }



    /**
       java.lang.boolean inBounds(java.lang.int, java.lang.int)
       
       Checks if the given x/y coordinate point is inbounds or not
       
       @param java.lang.int x   the x coordinate
       @param java.lang.int y   the y coordinate
       
       @return java.lang.boolean
    */
    private final boolean inBounds(int x, int y)
    {
        return x >= 0 && x < m_image.getWidth(null) && y >= 0 && y < m_image.getHeight(null);
    }



    /**
       java.lang.boolean isBlack(java.awt.Point)
       
       Checks to see if the given point is of color black.
       
       @param java.awt.Point p  the point to check
       
       @return java.lang.boolean
    */
    private final boolean isBlack(Point p)
    {
        return isBlack(p.x, p.y);
    }



    /**
       java.lang.boolean isBlack(java.lang.int, java.lang.int)
       
       Checks to see if the x/y coordinates from a given point
       are inbounds and if so is it black.
       
       @param java.lang.int x  the x coordinate
       @param java.lang.int y  the y coordinate
       
       @return java.lang.boolean
           
    */
    private final boolean isBlack(int x, int y)
    {
        if(!inBounds(x,y))
	{
            return false;   //not inbounds, can't be black
        }
	
	return (m_bufferedImage.getRGB(x,y) & 0x00FFFFFF) == 0;
    }



    /**
       move(java.awt.Point, java.lang.int)
       
       Moves to a specified direction
    
       Directions
           0 - North
           1 - North east
           2 - East
           3 - South east
           4 - South
           5 - South west
           6 - West
           7 - North west
	   
        @param java.awt.Point p          the given point
	@param java.lang.int  direction  the specified direction to move
    */
    private final void move(Point p, int direction)
    {
        if(direction < 0 || direction > 7)
	{
            throw new IllegalArgumentException("Not a direction :" + direction);
        }
	
        if(direction == 1 || direction == 2 || direction == 3)
        {
	    p.x++;
        }
	else if(direction == 5 || direction == 6 || direction == 7)
        {
	    p.x--;
        }
	
        if(direction == 5 || direction == 4 || direction == 3)
	{
            p.y++;
        }
	else if(direction == 7 || direction == 0 || direction == 1)
	{
            p.y--;
        }
    }


    private Point m_testPoint = new Point();  //used below

    /**
       java.lang.boolean isOnEdge(java.lang.int, java.awt.Point)
       
       Checks to see if the direction we're going is on the edge.
       At least thats what I can understand from this.
       
       @param java.lang.int  direction     the given direction
       @param java.awt.Point currentPoint  the current point
       
       @return java.lang.boolean
    */
    private boolean isOnEdge(int direction, Point currentPoint)
    {
        m_testPoint.setLocation(currentPoint);
        move(m_testPoint, direction);
	
        return   m_testPoint.x == 0 || m_testPoint.y == 0 ||
                 m_testPoint.y == m_image.getHeight(this) ||
		 m_testPoint.x == m_image.getWidth(this)  ||
                 isBlack(m_testPoint);
    }


    /**
       java.awt.Polygon findPolygon(java.lang.int, java.lang.int)
       
       Algorithm to find a polygon given a x/y coordinates and
       returns the found polygon.
       
       @param java.lang.int x  the x coordinate
       @param java.lang.int y  the y coordinate
       
       @return java.awt.Polygon
    */

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
	    
            for(int i = 2; i >= -4; i--)
            {
                tempDirection = (currentDirection + i) % 8;
		
                if(tempDirection < 0)
		{
                    tempDirection +=8;
                }
		
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
                    else {
                        move(currentPoint, currentDirection);
                    }

		    break;

                }//if

            }//for

        }//while

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

}//end class PolygonGrabber
