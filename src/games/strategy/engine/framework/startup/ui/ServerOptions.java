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

package games.strategy.engine.framework.startup.ui;

import games.strategy.ui.IntTextField;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * UI for choosing server options.
 * 
 * @author Sean Bridges
 */
public class ServerOptions extends JDialog
{

    private JTextField m_nameField;

    private IntTextField m_portField;

    private JPasswordField m_passwordField;

    private boolean m_okPressed;

    private JCheckBox m_requirePasswordCheckBox;
    private JTextField m_comment;
    private boolean m_showComment = false;

    /**
     * Creates a new instance of ServerOptions
     */
    public ServerOptions(Component owner, String defaultName, int defaultPort, boolean showComment)
    {
        super(JOptionPane.getFrameForComponent(owner), "Server options", true);
        m_showComment = showComment;
        
        
        initComponents();
        layoutComponents();
        setupActions();

        m_nameField.setText(defaultName);
        m_portField.setValue(defaultPort);

        setWidgetActivation();

        pack();
    }
    
    public void setNameEditable(boolean editable)
    {
        m_nameField.setEditable(editable);
    }

    private void setupActions()
    {
        m_requirePasswordCheckBox.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                setWidgetActivation();
            }

        });

    }

    public String getName()
    {
        // fixes crash by truncating names to 20 characters
        String s = m_nameField.getText().trim();
        if (s.length() > 20)
            return s.substring(0, 20);
        return s;
    }
    
    public String getPassword()
    {
        if(!m_requirePasswordCheckBox.isSelected())
            return null;
        String password = new String(m_passwordField.getPassword());
        if(password.trim().length() == 0)
            return null;
        return password;
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
        m_passwordField = new JPasswordField();
        m_passwordField.setColumns(10);
        m_comment = new JTextField();
        m_comment.setColumns(20);
    }

    private void layoutComponents()
    {
        Container content = getContentPane();
        content.setLayout(new BorderLayout());

        JPanel title = new JPanel();
        title.add(new JLabel("Select server options"));
        content.add(title, BorderLayout.NORTH);

        Insets labelSpacing = new Insets(3, 7, 0, 0);
        Insets fieldSpacing = new Insets(3, 5, 0, 7);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.gridx = 0;
        labelConstraints.insets = labelSpacing;

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.anchor = GridBagConstraints.WEST;
        fieldConstraints.gridx = 1;
        fieldConstraints.insets = fieldSpacing;

        m_requirePasswordCheckBox = new JCheckBox("");
        JLabel passwordRequiredLabel = new JLabel("Require Password:");

        JPanel fields = new JPanel();
        GridBagLayout layout = new GridBagLayout();

        fields.setLayout(layout);

        JLabel nameLabel = new JLabel("Name:");
        JLabel portLabel = new JLabel("Port:");
        JLabel passwordLabel = new JLabel("Password:");
        JLabel commentLabel = new JLabel("Comments:");
        layout.setConstraints(portLabel, labelConstraints);
        layout.setConstraints(nameLabel, labelConstraints);
        layout.setConstraints(passwordLabel, labelConstraints);        
        layout.setConstraints(m_portField, fieldConstraints);
        layout.setConstraints(m_nameField, fieldConstraints);
        layout.setConstraints(m_passwordField, fieldConstraints);
        layout.setConstraints(m_requirePasswordCheckBox, fieldConstraints);
        layout.setConstraints(passwordRequiredLabel, labelConstraints);

        fields.add(nameLabel);
        fields.add(m_nameField);
        fields.add(portLabel);
        fields.add(m_portField);

        fields.add(passwordRequiredLabel);
        fields.add(m_requirePasswordCheckBox);

        fields.add(passwordLabel);
        fields.add(m_passwordField);
        
        if(m_showComment)
        {
            layout.setConstraints(commentLabel, labelConstraints);            
            layout.setConstraints(m_comment, fieldConstraints);
            
            fields.add(commentLabel);
            fields.add(m_comment);
        }

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

    private void setWidgetActivation()
    {
        m_passwordField.setEnabled(m_requirePasswordCheckBox.isSelected());
        
        Color backGround = m_passwordField.isEnabled() ? m_portField.getBackground() : getBackground();
        m_passwordField.setBackground(backGround);
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

    public String getComments()
    {
        return m_comment.getText();
    }
    
    
}