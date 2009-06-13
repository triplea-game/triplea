package games.strategy.engine.framework.startup.login;

import java.awt.Component;
import java.util.*;

import javax.swing.*;

import games.strategy.engine.EngineVersion;
import games.strategy.net.IConnectionLogin;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.MD5Crypt;

public class ClientLogin implements IConnectionLogin
{
    public static final String ENGINE_VERSION_PROPERTY = "Engine.Version";
    public static final String JDK_VERSION_PROPERTY = "JDK.Version";
    public static final String PASSWORD_PROPERTY = "Password";
    
    private final Component m_parent;
    
    public ClientLogin(Component parent)
    {
        m_parent = parent;
    }

    public Map<String, String> getProperties(Map<String,String> challengProperties)
    {
        Map<String,String> rVal = new HashMap<String, String>();
        
        if(challengProperties.get(ClientLoginValidator.PASSWORD_REQUIRED_PROPERTY).equals(Boolean.TRUE.toString()))
        {
            JPasswordField passwordField = new JPasswordField();
            passwordField.setColumns(15);
            
            EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_parent),passwordField, "Enter a password to join the game", JOptionPane.QUESTION_MESSAGE);
            
            String password = new String(passwordField.getPassword());
            rVal.put(PASSWORD_PROPERTY, MD5Crypt.crypt(password, challengProperties.get(ClientLoginValidator.SALT_PROPERTY)));
        }
        
        rVal.put(ENGINE_VERSION_PROPERTY, EngineVersion.VERSION.toString());
        rVal.put(JDK_VERSION_PROPERTY, System.getProperty("java.runtime.version"));
        
        return rVal;
    }

    public void notifyFailedLogin(final String message)
    {
        EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_parent), message);
    }

}
