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
 * ClientOptions.java
 *
 * Created on February 1, 2002, 1:50 PM
 */
package games.strategy.engine.framework.startup.ui;

import games.strategy.ui.IntTextField;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

/**
 * UI for choosing client options.
 *
 * @author  Sean Bridges
 */
public class ClientOptions extends JDialog
{

  private JTextField m_nameField;
  private JTextField m_addressField;
  private IntTextField m_portField;
  private boolean m_okPressed;

  /**
   * Creates a new instance of ClientOptions
   */
  public ClientOptions(Component parent, String defaultName, int defaultPort, String defaultAddress)
  {
    super(JOptionPane.getFrameForComponent(parent), "Client options", true);

    initComponents();
    layoutComponents();

    m_nameField.setText(defaultName);
    m_portField.setValue(defaultPort);
    m_addressField.setText(defaultAddress);

    pack();
  }


  public String getName()
  {
  	//fixes crash by truncating names to 20 characters
  	String s=m_nameField.getText().trim();
  	if(s.length()>20)
  		return s.substring(0,20);
  	return s;
  }

  public String getAddress()
  {
    return m_addressField.getText().trim();
  }

  public int getPort()
  {
    return m_portField.getValue();
  }

  private void initComponents()
  {
    m_nameField = new JTextField(10);
    m_addressField = new JTextField(10);
    m_portField = new IntTextField(0, Integer.MAX_VALUE);
    m_portField.setColumns(7);
  }

  private void layoutComponents()
  {
    Container content = getContentPane();
    content.setLayout(new BorderLayout());

    JPanel title = new JPanel();
    title.add(new JLabel("Select client options"));
    content.add(title, BorderLayout.NORTH);

    Insets labelSpacing = new Insets(3,7,0,0);
    Insets fieldSpacing = new Insets(3,5,0,7);

    GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.anchor = GridBagConstraints.EAST;
    labelConstraints.gridx = 0;
    labelConstraints.insets = labelSpacing;

    GridBagConstraints fieldConstraints = new GridBagConstraints();
    fieldConstraints.anchor = GridBagConstraints.WEST;
    fieldConstraints.gridx = 1;
    fieldConstraints.insets = fieldSpacing;

    JPanel fields = new JPanel();
    GridBagLayout layout = new GridBagLayout();

    fields.setLayout(layout);

    JLabel nameLabel = new JLabel("Name:");
    JLabel portLabel = new JLabel("Server Port:");
    JLabel addressLabel = new JLabel("Server Address:");

    layout.setConstraints(portLabel, labelConstraints);
    layout.setConstraints(nameLabel, labelConstraints);
    layout.setConstraints(addressLabel, labelConstraints);

    layout.setConstraints(m_portField, fieldConstraints);
    layout.setConstraints(m_nameField, fieldConstraints);
    layout.setConstraints(m_addressField, fieldConstraints);

    fields.add(nameLabel);
    fields.add(m_nameField);
    fields.add(portLabel);
    fields.add(m_portField);
    fields.add(addressLabel);
    fields.add(m_addressField);

    content.add(fields, BorderLayout.CENTER);

    JPanel buttons = new JPanel();
    buttons.add(new JButton(m_okAction));
    buttons.add(new JButton(m_cancelAction));

    content.add(buttons, BorderLayout.SOUTH);
  }

  public boolean getOKPressed()
  {
    return m_okPressed;
  }

  private Action m_okAction = new AbstractAction("Connect")
  {

    public void actionPerformed(ActionEvent e)
    {
      setVisible(false);
      m_okPressed = true;
    }
  };

  private Action m_cancelAction = new AbstractAction("Cancel")
  {
    public void actionPerformed(ActionEvent e)
    {
      setVisible(false);
    }
  };
}
