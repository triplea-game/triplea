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


package games.strategy.triplea.image;

import java.awt.*;
import java.util.*;
import games.strategy.ui.Util;
import javax.swing.*;

/**
 * Utility for creating dice images
 */

public class DiceImageFactory
{
  public int DIE_WIDTH = 32;
  public int DIE_HEIGHT = 32;
  private static DiceImageFactory s_instance = new DiceImageFactory();

  //maps Integer -> Image
  private Map m_images = new HashMap();

  //maps Integer -> ImageIcon
  private Map m_icons = new HashMap();

  public static DiceImageFactory getInstance()
  {
    return s_instance;
  }

  public DiceImageFactory()
  {
    Panel observer = new Panel();
    final int PIP_SIZE = 6;
    for(int i = 1; i <= 6; i++)
    {
      Image canvas = Util.createImage(DIE_WIDTH, DIE_HEIGHT);


      canvas.getGraphics().setColor(Color.black);
      canvas.getGraphics().drawRect(1,1,DIE_WIDTH - 2, DIE_HEIGHT -2);
      //center dot
      if(i == 1 || i == 3 || i == 5)
      {
        canvas.getGraphics().fillOval(DIE_WIDTH / 2 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      //dots in top left and bottom right
      if(i == 3 || i == 5 || i == 4)
      {
        canvas.getGraphics().fillOval(DIE_WIDTH / 4 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        canvas.getGraphics().fillOval(3 * DIE_WIDTH / 4 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      //dots in bottom left and top right
      if(i == 5 || i == 4)
      {
        canvas.getGraphics().fillOval(3 * DIE_WIDTH / 4 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        canvas.getGraphics().fillOval(DIE_WIDTH / 4 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      //center two for 2
      if(i == 2 || i == 6)
      {
        canvas.getGraphics().fillOval( DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        canvas.getGraphics().fillOval(2* DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      if(i == 6)
      {
        canvas.getGraphics().fillOval(DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        canvas.getGraphics().fillOval(2 * DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);

        canvas.getGraphics().fillOval(DIE_WIDTH / 3 - (PIP_SIZE / 2), 3*  DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        canvas.getGraphics().fillOval(2 * DIE_WIDTH / 3 - (PIP_SIZE / 2), 3* DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      m_images.put(new Integer(i), canvas);
      m_icons.put(new Integer(i), new ImageIcon(canvas));
    }
  }

  public Image getDieImage(int i)
  {
    if(i <= 0)
      throw new IllegalArgumentException("die must be greater than 0, not:" + i);
    if(i > 6)
      throw new IllegalArgumentException("die must be less than 6, not:" + i);
    return (Image) m_images.get(new Integer(i));
  }

  public Icon getDieIcon(int i)
  {
    if(i <= 0)
      throw new IllegalArgumentException("die must be greater than 0, not:" + i);
    if(i > 6)
      throw new IllegalArgumentException("die must be less than 6, not:" + i);
    return (ImageIcon) m_icons.get(new Integer(i));
  }


  public static void main(String[] args)
  {
    JFrame frame = new JFrame();
    for(int i = 1; i <= 6; i++)
    {
      frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
      frame.getContentPane().add(new JLabel(DiceImageFactory.getInstance().getDieIcon(i)));
      frame.getContentPane().add(Box.createVerticalStrut(4));
    }
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.show();


  }

}