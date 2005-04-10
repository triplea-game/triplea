/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.random.IronyGamesDiceRollerRandomSource;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

public class PBEMStartup extends JPanel
{
    private static final String EMAIL_1_PROP_NAME = "games.strategy.engine.framework.ui.PBEMStartup.EMAIL2";
    private static final String EMAIL_2_PROP_NAME = "games.strategy.engine.framework.ui.PBEMStartup.EMAIL1";

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
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void userInit()
    {
        m_instructionsText.setText("\nPBEM differs from single player in that dice rolls are done by a dice server, and the results "
                + "are mailed to the email addresses below.\n\n" + "Dice are rolled using the dice server at http://www.irony.com/mailroll.html"
                + "\n\nYou can enter up to 5 addresses in the To: or Copy: fields, seperating each address by a space." 
                + "\n\nYou must enter an address in the To: field."
                
        );

        m_instructionsText.setBackground(this.getBackground());
    }

    private void jbInit() throws Exception
    {
        this.setLayout(m_gridBagLayout1);
        m_email1Label.setText("To:");
        m_email2Label.setText("Copy:");
        m_testButton.setText("Test Email");
        m_testButton.addActionListener(new PBEMStartup_m_testButton_actionAdapter(this));
        m_email2TextField.setText("");
        m_email2TextField.setColumns(50);

        m_email1TextField.setText("");
        m_email1TextField.setColumns(50);
        m_instructionsText.setEditable(false);
        m_instructionsText.setText("PBEM Properties");
        m_instructionsText.setLineWrap(true);
        m_instructionsText.setWrapStyleWord(true);
        this.add(m_email1TextField, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 0), 0, 0));
        this.add(m_email2TextField, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 0), 0, 0));
        this.add(m_testButton, new GridBagConstraints(0, 4, 3, 1, 0.2, 0.2, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(m_email2Label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0,
                5), 0, 0));
        this.add(m_email1Label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 20,
                0, 5), 0, 0));
        this.add(m_instructionsText, new GridBagConstraints(0, 0, 5, 1, 0.0, 0.2, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5,
                5, 5), 0, 0));
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

    public void storeEmails(GameData data)
    {
        data.getProperties().set(EMAIL_1_PROP_NAME, getEmail1());
        data.getProperties().set(EMAIL_2_PROP_NAME, getEmail2());
    }
    
    public void loadEmails(GameData data)
    {
        if(data.getProperties().get(EMAIL_1_PROP_NAME) != null)
        {
            if(m_email1TextField.getText().trim().length() == 0)
                m_email1TextField.setText(data.getProperties().get(EMAIL_1_PROP_NAME).toString());            
        }
        if(data.getProperties().get(EMAIL_2_PROP_NAME) != null)
        {
            if(m_email2TextField.getText().trim().length() == 0)
                m_email2TextField.setText(data.getProperties().get(EMAIL_2_PROP_NAME).toString());            
        }

        
    }

}

class PBEMStartup_m_testButton_actionAdapter implements java.awt.event.ActionListener
{
    PBEMStartup adaptee;

    PBEMStartup_m_testButton_actionAdapter(PBEMStartup adaptee)
    {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e)
    {
        adaptee.m_testButton_actionPerformed(e);
    }
}
