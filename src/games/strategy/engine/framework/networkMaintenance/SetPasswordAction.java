package games.strategy.engine.framework.networkMaintenance;

import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class SetPasswordAction extends AbstractAction
{
	private static final long serialVersionUID = -7767288210554177480L;
	private final ClientLoginValidator m_validator;
	private final Component m_parent;
	private final InGameLobbyWatcher m_lobbyWatcher;
	
	public SetPasswordAction(final Component parent, final InGameLobbyWatcher watcher, final ClientLoginValidator validator)
	{
		super("Set Game Password...");
		// TODO Auto-generated constructor stub
		m_validator = validator;
		m_parent = parent;
		m_lobbyWatcher = watcher;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		final JLabel label = new JLabel("Enter Password, (Leave blank for no password).");
		final JPasswordField passwordField = new JPasswordField();
		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(label, BorderLayout.NORTH);
		panel.add(passwordField, BorderLayout.CENTER);
		final int rVal = JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(m_parent), panel, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
		if (rVal != JOptionPane.OK_OPTION)
			return;
		final String password = new String(passwordField.getPassword());
		final boolean passworded;
		if (password.trim().length() > 0)
		{
			m_validator.setGamePassword(password);
			passworded = true;
		}
		else
		{
			m_validator.setGamePassword(null);
			passworded = false;
		}
		if (m_lobbyWatcher != null && m_lobbyWatcher.isActive())
		{
			m_lobbyWatcher.setPassworded(passworded);
		}
	}
}
