package games.strategy.engine.lobby.client.login;

import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.*;
import games.strategy.util.MD5Crypt;

import java.awt.Window;
import java.io.IOException;
import java.util.*;

import javax.swing.JOptionPane;

public class LobbyLogin
{
    private final Window m_parent;

    private final LobbyServerProperties m_serverProperties;

    public LobbyLogin(Window parent, LobbyServerProperties properties)
    {
        m_parent = parent;
        m_serverProperties = properties;
    }

    /**
     * Attempt to login to the LobbyServer
     * <p>
     * 
     * If we could not login, return null.
     */
    public LobbyClient login()
    {

        if (!m_serverProperties.isServerAvailable())
        {
            JOptionPane.showMessageDialog(m_parent, "Could not connect to server:" + m_serverProperties.getServerErrorMessage(), "Could not connect to server",
                    JOptionPane.ERROR_MESSAGE);

            return null;
        }

        return loginToServer();
    }

    private LobbyClient loginToServer()
    {
        final LoginPanel panel = new LoginPanel();
        LoginPanel.ReturnValue value = panel.show(m_parent);
        if (value == LoginPanel.ReturnValue.LOGON)
        {
            return login(panel);
        } else if (value == LoginPanel.ReturnValue.CANCEL)
        {
            return null;
        } else if (value == LoginPanel.ReturnValue.CREATE_ACCOUNT)
        {
            return createAccount();
        } else
            throw new IllegalStateException("??");
    }

    private LobbyClient login(final LoginPanel panel)
    {
        try
        {
            ClientMessenger messenger = new ClientMessenger(m_serverProperties.getHost(), m_serverProperties.getPort(), panel.getUserName(), new IConnectionLogin()
            {

                public void notifyFailedLogin(String message)
                {
                    JOptionPane.showMessageDialog(m_parent, message, "Login Failed", JOptionPane.ERROR_MESSAGE);

                }

                public Map<String, String> getProperties(Map<String, String> challengProperties)
                {
                    Map<String, String> props = new HashMap<String, String>();
                    if(panel.isAnonymous())
                    {
                        props.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
                        
                    }
                    else
                    {
                        String salt = challengProperties.get(LobbyLoginValidator.SALT_KEY);
                        String hashedPassword = MD5Crypt.crypt(panel.getPassword(), salt);
                        props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
                    }

                    
                    
                    props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());

                    return props;
                }

            }

            );
            return new LobbyClient(messenger);
        } catch (CouldNotLogInException clne)
        {
            // this has already been dealt with
            return createAccount();
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
            return null;
        }
    }

    private LobbyClient createAccount()
    {
        final CreateAccountPanel createAccount = new CreateAccountPanel();
        CreateAccountPanel.ReturnValue value = createAccount.show(m_parent);
        if (value == CreateAccountPanel.ReturnValue.CREATE_ACCOUNT)
        {
            return createAccount(createAccount);
        } else
        {
            return null;
        }

    }

    private LobbyClient createAccount(final CreateAccountPanel createAccount)
    {
        try
        {
            ClientMessenger messenger = new ClientMessenger(m_serverProperties.getHost(), m_serverProperties.getPort(), createAccount.getUserName(), new IConnectionLogin()
            {

                public void notifyFailedLogin(String message)
                {
                    JOptionPane.showMessageDialog(m_parent, message, "Login Failed", JOptionPane.ERROR_MESSAGE);

                }

                public Map<String, String> getProperties(Map<String, String> challengProperties)
                {
                    Map<String, String> props = new HashMap<String, String>();
                    props.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
                    props.put(LobbyLoginValidator.EMAIL_KEY, createAccount.getEmail());
                    props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt(createAccount.getPassword()));
                    props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());

                    return props;
                }

            }

            );
            return new LobbyClient(messenger);
        } catch (CouldNotLogInException clne)
        {
            // this has already been dealt with
            return createAccount();
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
            return null;
        }
    }

}
