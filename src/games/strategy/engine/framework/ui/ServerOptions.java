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
 * ServerOptions.java
 *
 * Created on February 1, 2002, 12:18 PM
 */

package games.strategy.engine.framework.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.ui.IntTextField;

/**
 * UI for choosing server options.
 *
 * @author  Sean Bridges
 */
public class ServerOptions extends JDialog
{

  private JTextField m_nameField;
  private IntTextField m_portField;
  private boolean m_okPressed;

  /**
   * Creates a new instance of ServerOptions
   */
  public ServerOptions(Frame owner, String defaultName, int defaultPort)
  {
    super(owner, "Server options", true);

    initComponents();
    layoutComponents();

    m_nameField.setText(defaultName);
    m_portField.setValue(defaultPort);

    pack();
  }

  public String getName()
  {
    return m_nameField.getText();
  }

  public int getPort()
  {
    return m_portField.getValue();
  }

  private void initComponents()
  {
    m_nameField = new JTextField(10);
    m_portField = new IntTextField(0, Integer.MAX_VALUE);
    m_portField.setColumns(7);
  }

  private void layoutComponents()
  {
    Container content = getContentPane();
    content.setLayout(new BorderLayout());

    JPanel title = new JPanel();
    title.add(new JLabel("Select server options"));
    content.add(title, BorderLayout.NORTH);

    Insets labelSpacing = new Insets(3,7,0,0);
    Insets fieldSpacing = new Insets(3,5,0,7);

    GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.anchor = GridBagConstraints.WEST;
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
    JLabel portLabel = new JLabel("Port:");
    layout.setConstraints(portLabel, labelConstraints);
    layout.setConstraints(nameLabel, labelConstraints);
    layout.setConstraints(m_portField, fieldConstraints);
    layout.setConstraints(m_nameField, fieldConstraints);

    fields.add(nameLabel);
    fields.add(m_nameField);
    fields.add(portLabel);
    fields.add(m_portField);

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


  private Action m_okAction = new AbstractAction("OK")
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