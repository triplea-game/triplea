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


public class PlacementPicker extends JFrame
{
    private Point m_currentSquare;
    private Image m_image;
    private JLabel m_location = new JLabel();
    private Map m_polygons = new HashMap();

    private Map m_placements;
    private List m_currentPlacements;
    private String m_currentCountry;

    private static final int PLACE_SIZE = 48;

    public PlacementPicker(String fileName)
    {
        super("Center Picker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try
        {
            m_polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream("polygons.txt"));
        }
        catch (IOException ex1)
        {
            ex1.printStackTrace();
        }


        m_image = Toolkit.getDefaultToolkit().createImage(fileName);

        try
        {
            Util.ensureImageLoaded(m_image, this);
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }


        JPanel imagePanel = createMainPanel();

        imagePanel.addMouseMotionListener(
        new MouseMotionAdapter()
        {

            public void mouseMoved(MouseEvent e)
             {
                 m_location.setText("x:"+ e.getX() + " y:" + e.getY());
                 m_currentSquare = new Point(e.getPoint());
                 repaint();

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
        this.getContentPane().add(m_location, BorderLayout.SOUTH);

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
                g.drawImage(m_image, 0, 0, this);
                g.setColor(Color.red);

                if(m_currentSquare != null)
                {
                    g.drawRect(m_currentSquare.x, m_currentSquare.y, PLACE_SIZE, PLACE_SIZE);
                }

                if(m_currentPlacements == null)
                    return;

                 Iterator pointIter = m_currentPlacements.iterator();
                 while (pointIter.hasNext())
                 {
                     Point item = (Point)pointIter.next();
                     g.fillRect(item.x, item.y, PLACE_SIZE ,PLACE_SIZE);
                 }
            }
        };
        return imagePanel;
    }



    private void save()
    {

        try
        {
            String fileName = JOptionPane.showInputDialog(this, "enter name to save");
            if (fileName.trim().length() == 0)
                return;
            FileOutputStream out = new FileOutputStream(fileName);
            PointFileReaderWriter.writeOneToMany(out, m_placements);
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

    private void load()
    {
        try
         {
             String fileName = JOptionPane.showInputDialog(this, "enter name to load");
             if(fileName.trim().length() == 0)
                 return;
             FileInputStream in = new FileInputStream(fileName);
             m_placements = PointFileReaderWriter.readOneToMany(in);
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
                        return name;
                }

            }
        }
        return seaName;
    }

    private void mouseEvent(Point point, boolean ctrlDown, boolean rightMouse)
    {
        //left button start in territory
        //left button + control, add point
        //right button and cntrl write
        //right button remove last
        if(!rightMouse && !ctrlDown)
        {
            m_currentCountry = findTerritoryName(point);
            m_currentPlacements =new ArrayList( (List) m_placements.get(m_currentCountry));

            JOptionPane.showMessageDialog(this, m_currentCountry);
        }

        else if(!rightMouse && ctrlDown)
        {
            m_currentPlacements.add(point);
        }

        else if (rightMouse && ctrlDown)
        {
            m_placements.put(m_currentCountry, m_currentPlacements);
            m_currentPlacements = new ArrayList();
            System.out.println("done:" + m_currentCountry);
        }
        else if (rightMouse)
        {
            if(!m_currentPlacements.isEmpty())
                m_currentPlacements.remove(m_currentPlacements.size() -1);
        }


        repaint();


    }

    public static void main(String[] args)
     {
         String fileName = "/home/sgb/dev/triplea/additonalImageData/baseMap.gif";
         PlacementPicker picker = new PlacementPicker(fileName);
         picker.setSize(600,550);
         picker.show();
     }


}
