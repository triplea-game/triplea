package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.ui.Util;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class LoginPanel extends JPanel
{
    public static enum ReturnValue {CANCEL, LOGON, CREATE_ACCOUNT}
    
    private JDialog m_dialog;
    private JPasswordField m_password;
    private JTextField m_userName;
    private JCheckBox m_anonymous;
    private JButton m_createAccount;
    
    private ReturnValue m_returnValue;
    private JButton m_logon;
    private JButton m_cancel;
    
    public LoginPanel()
    {
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_password = new JPasswordField();
        m_userName = new JTextField();
        m_anonymous = new JCheckBox("Login Anonymously?");
        m_createAccount = new JButton("Create Account...");
        
        m_logon = new JButton("Login");
        m_cancel = new JButton("Cancel");
    }
    
    
    
    
    private void layoutComponents()
    {
        JLabel label = new JLabel(new ImageIcon(Util.getBanner("Login")));
        setLayout(new BorderLayout());
        add(label, BorderLayout.NORTH);
        
        JPanel main = new JPanel();
        add(main, BorderLayout.CENTER);
        main.setLayout(new GridBagLayout());

        main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));       
        main.add(m_userName, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(10, 5, 0, 40), 0, 0));
        
        
        main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
        main.add(m_password, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 40), 0, 0));

        
        main.add(m_anonymous, new GridBagConstraints(0, 2,  2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
        
        
        main.add(m_createAccount, new GridBagConstraints(0, 3,  2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
                
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(m_logon);
        buttons.add(m_cancel);
        

        add(buttons, BorderLayout.SOUTH);

        
    }

    private void setupListeners()
    {
        m_logon.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                logonPressed();
            }
        });
        
        m_createAccount.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                m_returnValue = ReturnValue.CREATE_ACCOUNT;
                m_dialog.setVisible(false);
            }
        
        });
        
        m_cancel.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_dialog.setVisible(false);
            }
        
        });
        
        m_anonymous.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                setWidgetActivation();
            }
        
        });
    }

    private void logonPressed()
    {
        if(DBUserController.validateUserName(m_userName.getText()) != null)
        {
            JOptionPane.showMessageDialog(this, DBUserController.validateUserName(m_userName.getText()), "Invalid Username" , JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if(m_password.getPassword().length < 3 && !m_anonymous.isSelected())
        {
            JOptionPane.showMessageDialog(LoginPanel.this, "You must enter a password", "No password" , JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        m_returnValue = ReturnValue.LOGON;
        m_dialog.setVisible(false);
    }

    
    private void setWidgetActivation()
    {
        if(!m_anonymous.isSelected())
        {
            m_password.setEnabled(true);
            m_password.setBackground(m_userName.getBackground());
            
        }
        else
        {
            m_password.setEnabled(false);
            m_password.setBackground(this.getBackground());
            
        }
        
    }
    
    
    
    public static void main(String[] args)
    {
        GameRunner2.setupLookAndFeel();
        
        JDialog d = new JDialog();
        d.add(new LoginPanel());
        d.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        d.pack();
        d.setVisible(true);
    }
    
    
    public boolean isAnonymous()
    {
        return m_anonymous.isSelected();
    }
    
    public String getUserName()
    {
        return m_userName.getText();
    }
    
    @SuppressWarnings("deprecation")
    public String getPassword()
    {
        return m_password.getText();
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
}
