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
  private Map<Integer, Image> m_images = new HashMap<Integer, Image>();
  private Map<Integer, Image> m_imagesHit = new HashMap<Integer, Image>();

  //maps Integer -> ImageIcon
  private Map<Integer, ImageIcon> m_icons = new HashMap<Integer, ImageIcon>();
  private Map<Integer, ImageIcon> m_iconsHit = new HashMap<Integer, ImageIcon>();

  public static DiceImageFactory getInstance()
  {
    return s_instance;
  }

  public DiceImageFactory()
  {
    final int PIP_SIZE = 6;

    generateDice(PIP_SIZE, Color.black, m_images, m_icons);
    generateDice(PIP_SIZE, Color.red, m_imagesHit, m_iconsHit);
  }

  private void generateDice(int PIP_SIZE, Color color, Map<Integer, Image> images, Map<Integer, ImageIcon> icons)
  {
    for(int i = 1; i <= 6; i++)
    {
      Image canvas = Util.createImage(DIE_WIDTH, DIE_HEIGHT, true);

      Graphics graphics = canvas.getGraphics();


      graphics.setColor(color);

      graphics.drawRoundRect(1,1,DIE_WIDTH - 3, DIE_HEIGHT -3,5,5);
      ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


      //center dot
      if(i == 1 || i == 3 || i == 5)
      {
        graphics.fillOval(DIE_WIDTH / 2 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      //dots in top left and bottom right
      if(i == 3 || i == 5 || i == 4)
      {
        graphics.fillOval(DIE_WIDTH / 4 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        graphics.fillOval(3 * DIE_WIDTH / 4 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      //dots in bottom left and top right
      if(i == 5 || i == 4)
      {
        graphics.fillOval(3 * DIE_WIDTH / 4 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        graphics.fillOval(DIE_WIDTH / 4 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      //center two for 2
      if(i == 2 || i == 6)
      {
        graphics.fillOval( DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        graphics.fillOval(2* DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      if(i == 6)
      {
        graphics.fillOval(DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        graphics.fillOval(2 * DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);

        graphics.fillOval(DIE_WIDTH / 3 - (PIP_SIZE / 2), 3*  DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        graphics.fillOval(2 * DIE_WIDTH / 3 - (PIP_SIZE / 2), 3* DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
      }
      images.put(new Integer(i), canvas);

      icons.put(new Integer(i), new ImageIcon(canvas));
      graphics.dispose();
    }
  }

  public Image getDieImage(int i, boolean hit)
  {
    if(i <= 0)
      throw new IllegalArgumentException("die must be greater than 0, not:" + i);
    if(i > 6)
      throw new IllegalArgumentException("die must be less than 6, not:" + i);
    if(hit)
      return m_imagesHit.get(new Integer(i));
    else
      return m_images.get(new Integer(i));

  }

  public Icon getDieIcon(int i, boolean hit)
  {
    if(i <= 0)
      throw new IllegalArgumentException("die must be greater than 0, not:" + i);
    if(i > 6)
      throw new IllegalArgumentException("die must be less than 6, not:" + i);

    if (hit)
      return m_iconsHit.get(new Integer(i));
    else
      return m_icons.get(new Integer(i));
  }


  public static void main(String[] args)
  {
    JFrame frame = new JFrame();
    for(int i = 1; i <= 6; i++)
    {
      frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
      frame.getContentPane().add(new JLabel(DiceImageFactory.getInstance().getDieIcon(i, false)));
      frame.getContentPane().add(Box.createVerticalStrut(4));
    }
    for(int i = 1; i <= 6; i++)
    {
      frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
      frame.getContentPane().add(new JLabel(DiceImageFactory.getInstance().getDieIcon(i, true)));
      frame.getContentPane().add(Box.createVerticalStrut(4));
    }

    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);


  }

}
