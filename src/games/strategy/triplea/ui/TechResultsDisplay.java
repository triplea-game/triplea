package games.strategy.triplea.ui;

import java.util.Vector;
import java.awt.*;
import javax.swing.*;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.delegate.message.TechResultsMessage;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class TechResultsDisplay extends JPanel
{

  public TechResultsDisplay(TechResultsMessage msg)
  {
    setLayout(new GridBagLayout());

    add(new JLabel("You got " + msg.getHits() + " hit" + (msg.getHits() != 1 ? "s" : "") + "."),
        new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,5,0), 0,0));
    if(msg.getHits() != 0)
    {
      add(new JLabel("Technologies discovered:"),
        new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0)
      );
      JList list = new JList(new Vector(msg.getAdvances()));
      add(list,
        new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,5,0), 0,0)
          );
      list.setBackground(this.getBackground());
    }

    JPanel dice = new JPanel();

    dice.setLayout(new BoxLayout(dice,BoxLayout.X_AXIS));
    for(int i = 0; i < msg.getRolls().length; i++)
    {
      //add 1 since dice are 0 based
      int roll = msg.getRolls()[i] + 1;
      JLabel die = new JLabel(DiceImageFactory.getInstance().getDieIcon(roll, roll ==6));
      dice.add(die);
      dice.add(Box.createHorizontalStrut(2));
      dice.setMaximumSize(new Dimension(200, (int)dice.getMaximumSize().getHeight()));
    }
    JScrollPane diceScroll = new JScrollPane(dice);
    diceScroll.setBorder(null);
    add(diceScroll,
        new GridBagConstraints(0,3,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,5,0), 0,0)
        );


  }
}
