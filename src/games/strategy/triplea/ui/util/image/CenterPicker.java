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

import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;


public class CenterPicker extends JFrame
{

    private Image  m_image;                     // The map image will be stored here    
    private Map<String, Point>    m_centers  = new HashMap<String, Point>();  // hash map for center points
    private Map    m_polygons = new HashMap();  // hash map for polygon points
    private JLabel m_location = new JLabel();



    /**
       main(java.lang.String[])
       
       Main program begins here.
       Asks the user to select the map then runs the
       the actual picker.
       
       @param java.lang.String[] args  the command line arguments
       
       @see Picker(java.lang.String) picker
    */
    public static void main(String[] args)
    {
         System.out.println("Select the map");
         String mapName = new FileOpen("Select The Map").getPathString();

	 if(mapName != null)
	 {
	     System.out.println("Map : "+mapName);
	     
             CenterPicker picker = new CenterPicker(mapName);
             picker.setSize(600,550);
             picker.setVisible(true);
	 }
	 else {
	 	System.out.println("No Image Map Selected. Shutting down.");
		System.exit(0);
	}
     
     }//end main



    /**
       Constructor CenterPicker(java.lang.String)
       
       Setus up all GUI components, initializes variables with
       default or needed values, and prepares the map for user
       commands.
       
       @param java.lang.String  mapName  name of map file
    */
    public CenterPicker(String mapName)
    {
        super("Center Picker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try
        {
	    System.out.println("Select the Polygons file");
	    String polyPath = new FileOpen("Select A Polygon File").getPathString();
	    
	    if(polyPath != null)
	    {
	        System.out.println("Polygons : "+polyPath);
                m_polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(polyPath));
            }
	    else
	    {
	        System.out.println("Polygons file not given. Will run regardless");
	    }
	}
        catch (IOException ex1)
        {
            ex1.printStackTrace();
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
                     m_location.setText("x:"+ e.getX() + " y:" + e.getY());
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
	
        imagePanel.setMinimumSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
        imagePanel.setPreferredSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
        imagePanel.setMaximumSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));

        //set up the layout manager
	
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(new JScrollPane(imagePanel),  BorderLayout.CENTER);
        this.getContentPane().add(m_location, BorderLayout.SOUTH);

        
	//set up the actions
	
	Action openAction = new AbstractAction("Load Centers") {
            public void actionPerformed(ActionEvent event) {
                loadCenters();
            }
        };
        openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Center Points File");


        Action saveAction = new AbstractAction("Save Centers") {
            public void actionPerformed(ActionEvent event) {
                saveCenters();
            }
        };
        saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Center Points To File");


        Action exitAction = new AbstractAction("Exit") {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        };
        exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");

       	//set up the menu items

	JMenuItem openItem = new JMenuItem(openAction);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));

        JMenuItem saveItem = new JMenuItem(saveAction);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));

        JMenuItem exitItem = new JMenuItem(exitAction);
	
	
	//set up the menu bar

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
	
	JMenu fileMenu = new JMenu("File");
	fileMenu.setMnemonic('F');
	fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

	menuBar.add(fileMenu);

    }//end constructor



    /**
       createImage(java.lang.String)
       
       creates the image map and makes sure
       it is properly loaded.
       
       @param java.lang.String mapName  the path of image map
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
    }



    /**
       javax.swing.JPanel createMainPanel()
       
       Creates the main panel and returns
       a JPanel object.
       
       @return javax.swing.JPanel  the panel to return
    */
    private JPanel createMainPanel()
    {
        JPanel imagePanel = new JPanel()
        {
            public void paint(Graphics g)
            {
                //super.paint(g);
                g.drawImage(m_image, 0, 0, this);
                g.setColor(Color.red);

                 Iterator<Point> polyIter = m_centers.values().iterator();
                 while (polyIter.hasNext()) {
                     Point item = polyIter.next();
                     g.fillOval(item.x, item.y, 15,15);
                 }
            }
        };
        return imagePanel;
    }


    /**
       saveCenters()
       
       Saves the centers to disk.
    */
    private void saveCenters()
    {

        try
        {
            String fileName = new FileSave("Where To Save centers.txt ?","centers.txt").getPathString();
	    
	    if(fileName == null)
	    {
                return;
            }
	    
	    FileOutputStream out = new FileOutputStream(fileName);
            PointFileReaderWriter.writeOneToOne(out, m_centers);
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


    /**
       loadCenters()
       
       Loads a pre-defined file with map center points.
    */
    private void loadCenters()
    {
        try
         {
	     System.out.println("Load a center file");
             String centerName = new FileOpen("Load A Center File").getPathString();

	     if(centerName == null)
	     {
                 return;
	     }
	      
             FileInputStream in = new FileInputStream(centerName);
             m_centers = PointFileReaderWriter.readOneToOne(in);
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
        java.lang.String findTerritoryName(java.awt.Point)
	
	Finds a land territory name or
	some sea zone name.
	
	@param java.awt.point p  a point on the map
    */
    private String findTerritoryName(Point p)
    {
        String seaName = "there be dragons";

        //try to find a land territory.
        //sea zones often surround a land territory
	
        Iterator keyIter = m_polygons.keySet().iterator();
        while (keyIter.hasNext())
        {
            String name = (String)keyIter.next();
            Collection polygons = (Collection) m_polygons.get(name);
            Iterator polyIter = polygons.iterator();
	    
            while (polyIter.hasNext())
            {
                Polygon poly = (Polygon)polyIter.next();
		
                if(poly.contains(p))
                {
                    if(name.endsWith("Sea Zone"))
                    {
                        seaName = name;
                    }
                    else
		    {
                        return name;
		    }
		    
		}//if

            }//while
	    
        }//while
	
        return seaName;
    }


    /**
       mouseEvent(java.awt.Point, java.lang.boolean, java.lang.boolean)
       
       @param java.awt.Point    point       a point clicked by mouse
       @param java.lang.boolean ctrlDown    true if ctrl key was hit
       @param java.lang.boolean rightMouse  true if the right mouse button was hit
    */
    private void mouseEvent(Point point, boolean ctrlDown, boolean rightMouse)
    {
        String name = findTerritoryName(point);
        JTextField message = new JTextField();
        message.setText(name);
        JOptionPane.showMessageDialog(this, message);
        name = message.getText();
        
        
        int rVal = JOptionPane.showConfirmDialog(this, name);
        if(rVal == JOptionPane.OK_OPTION)
        {
            m_centers.put(name, point);
        }
        repaint();
    }

}//end class CenterPicker
