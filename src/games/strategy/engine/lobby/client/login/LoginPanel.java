package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.ui.Util;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.swing.plaf.*;

public class LoginPanel extends JPanel
{

    private JPasswordField m_password;
    private JTextField m_userName;
    private JCheckBox m_anonymous;
    private JButton m_createAccount;
    
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

    }

    private void setWidgetActivation()
    {
        
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
    
 
}
