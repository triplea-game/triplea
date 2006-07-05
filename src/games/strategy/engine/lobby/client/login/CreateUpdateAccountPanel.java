package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.lobby.server.userDB.*;
import games.strategy.ui.Util;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class CreateUpdateAccountPanel extends JPanel
{

    
    public static enum ReturnValue {CANCEL, OK}
    
    private JDialog m_dialog;
    
    private JTextField m_userName;
    private JTextField m_email;
    private JPasswordField m_password;
    private JPasswordField m_password2;
    private JButton m_okButton;
    private JButton m_cancelButton;
    private ReturnValue m_returnValue;

    
    public static CreateUpdateAccountPanel newUpdatePanel(DBUser user)
    {
        CreateUpdateAccountPanel panel = new CreateUpdateAccountPanel(false);
        panel.m_userName.setText(user.getName());
        panel.m_userName.setEditable(false);
        panel.m_email.setText(user.getEmail());
        return panel;
    }
    
    public static CreateUpdateAccountPanel newCreatePanel()
    {
        CreateUpdateAccountPanel panel = new CreateUpdateAccountPanel(true);
        return panel;
    }

    
    private CreateUpdateAccountPanel(boolean create)
    {
         
        createComponents();
        layoutComponents(create);
        setupListeners();
        setWidgetActivation();
    }



    private void createComponents()
    {
        m_userName = new JTextField();
        m_email = new JTextField();
        m_password = new JPasswordField();
        m_password2 = new JPasswordField();
        m_cancelButton = new JButton("Cancel");
        m_okButton = new JButton("OK");
        
    }

    private void layoutComponents(boolean create)
    {
        JLabel label = new JLabel(new ImageIcon(Util.getBanner(create ? "Create Account" : "Update Account")));
        setLayout(new BorderLayout());
        add(label, BorderLayout.NORTH);
        
        JPanel main = new JPanel();
        add(main, BorderLayout.CENTER);
        main.setLayout(new GridBagLayout());

        main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));       
        main.add(m_userName, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(10, 5, 0, 40), 0, 0));
        
        
        main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
        main.add(m_password, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 40), 0, 0));

        main.add(new JLabel("Re-type Password:"), new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
        main.add(m_password2, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 40), 0, 0));

        main.add(new JLabel("Email:"), new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 15, 0), 0, 0));
        main.add(m_email, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 15, 40), 0, 0));
  
        
                
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(m_okButton);
        buttons.add(m_cancelButton);
        

        add(buttons, BorderLayout.SOUTH);
        
    }

    public static void main(String[] args)
    {
        GameRunner2.setupLookAndFeel();
        
        JDialog d = new JDialog();
        d.add(new CreateUpdateAccountPanel(false));
        d.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        d.pack();
        d.setVisible(true);
    }
    
    private void setupListeners()
    {
        m_cancelButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_dialog.setVisible(false);
            }
        
        });
        
        m_okButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                okPressed();
        
            }
        
        });
    }

    @SuppressWarnings("deprecation")
    private void okPressed()
    {
        if(!m_password.getText().equals(m_password2.getText()))
        {
            JOptionPane.showMessageDialog(this, "The passwords do not match", "Passwords Do Not Match" , JOptionPane.ERROR_MESSAGE);
            m_password.setText("");
            m_password2.setText("");
            return;
        }        
        if(!games.strategy.util.Util.isMailValid( m_email.getText() ))
        {
            JOptionPane.showMessageDialog(this, "You must enter a username", "No username" , JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if(DBUserController.validateUserName(m_userName.getText()) != null)
        {
            JOptionPane.showMessageDialog(this, DBUserController.validateUserName(m_userName.getText()), "Invalid Username" , JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        m_returnValue = ReturnValue.OK;
        m_dialog.setVisible(false);
        
        
        
    }

    private void setWidgetActivation()
    {

    }
    
    public ReturnValue show(Window parent)
    {
        m_dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Login", true);
        
        m_dialog.getContentPane().add(this);
        m_dialog.pack();
        m_dialog.setLocationRelativeTo(parent);
        m_dialog.setVisible(true);
        
        m_dialog.dispose();
        m_dialog = null;
        
        if(m_returnValue == null)
            return ReturnValue.CANCEL;
        
        return m_returnValue;
    }
    
    
    
    
    @SuppressWarnings("deprecation")
    public String getPassword()
    {
        return m_password.getText();
    }
    
    
    public String getEmail()
    {
        return m_email.getText();
    }
    
    public String getUserName()
    {
        return m_userName.getText();
    }
    
    
    
}
