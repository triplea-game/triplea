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

package games.strategy.engine.framework.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import games.strategy.engine.random.*;
import games.strategy.triplea.*;

public class PBEMStartup extends JPanel
{
  GridBagLayout m_gridBagLayout1 = new GridBagLayout();
  JTextField m_email1TextField = new JTextField();
  JTextField m_email2TextField = new JTextField();
  JLabel m_email1Label = new JLabel();
  JLabel m_email2Label = new JLabel();
  JButton m_testButton = new JButton();
  JTextArea m_instructionsText = new JTextArea();

  public PBEMStartup()
  {
    try
    {
      jbInit();
      userInit();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  private void userInit()
  {
    m_instructionsText.setText(
    "\nPBEM differs from single player in that dice rolls are done by a dice server, and the results " +
    "are mailed to the email addresses below.\n\n" +
    "Dice are rolled using the dice server at http://www.irony.com/mailroll.html"
    );

    m_instructionsText.setBackground(this.getBackground());
  }

  private void jbInit() throws Exception
  {
    this.setLayout(m_gridBagLayout1);
    m_email1Label.setText("Email Address 1:");
    m_email2Label.setText("Email Address 2:");
    m_testButton.setText("Test Email");
    m_testButton.addActionListener(new PBEMStartup_m_testButton_actionAdapter(this));
    m_email2TextField.setText("");
    m_email2TextField.setColumns(50);

    m_email1TextField.setText("");
    m_email1TextField.setColumns(50);
    m_instructionsText.setEditable(false);
    m_instructionsText.setText("PGEM Properties");
    m_instructionsText.setLineWrap(true);
    m_instructionsText.setWrapStyleWord(true);
    this.add(m_email1TextField, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0
      , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    this.add(m_email2TextField, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0
      , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    this.add(m_testButton, new GridBagConstraints(0, 4, 3, 1, 0.2, 0.2
                                                  , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    this.add(m_email2Label,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0, 5), 0, 0));
    this.add(m_email1Label,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 5), 0, 0));
    this.add(m_instructionsText,  new GridBagConstraints(0, 0, 5, 1, 0.0, 0.2
            ,GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }

  void m_testButton_actionPerformed(ActionEvent e)
  {
    IronyGamesDiceRollerRandomSource random = new IronyGamesDiceRollerRandomSource(m_email1TextField.getText(), m_email2TextField.getText());
    random.test();
  }

  public String getEmail1()
  {
    return m_email1TextField.getText();
  }

  public String getEmail2()
  {
    return m_email2TextField.getText();
  }


}

class PBEMStartup_m_testButton_actionAdapter implements java.awt.event.ActionListener {
  PBEMStartup adaptee;

  PBEMStartup_m_testButton_actionAdapter(PBEMStartup adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.m_testButton_actionPerformed(e);
  }
}
