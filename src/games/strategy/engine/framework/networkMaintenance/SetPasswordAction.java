package games.strategy.engine.framework.networkMaintenance;

import games.strategy.engine.framework.startup.login.ClientLoginValidator;

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
	private final ClientLoginValidator m_validator;
	private final Component m_parent;
	
	public SetPasswordAction(final Component parent, final ClientLoginValidator validator)
	{
		super("Set Game Password...");
		// TODO Auto-generated constructor stub
		m_validator = validator;
		m_parent = parent;
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
		if (password.trim().length() > 0)
		{
			m_validator.setGamePassword(password);
		}
		else
		{
			m_validator.setGamePassword(null);
		}
	}
}
