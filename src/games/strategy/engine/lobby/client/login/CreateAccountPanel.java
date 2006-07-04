package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.ui.Util;

import java.awt.*;

import javax.swing.*;

public class CreateAccountPanel extends JPanel
{
    private JTextField m_userName;
    private JTextField m_email;
    private JPasswordField m_password;
    private JPasswordField m_password2;
    private JButton m_okButton;
    private JButton m_cancelButton;
    
    
    public CreateAccountPanel()
    {
        createComponents();
        layoutComponents();
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

    private void layoutComponents()
    {
        JLabel label = new JLabel(new ImageIcon(Util.getBanner("Create Account")));
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
        d.add(new CreateAccountPanel());
        d.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        d.pack();
        d.setVisible(true);
    }
    
    private void setupListeners()
    {

    }

    private void setWidgetActivation()
    {

    }
    
    
}
