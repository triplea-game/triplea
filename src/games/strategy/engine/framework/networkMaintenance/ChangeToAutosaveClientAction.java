package games.strategy.engine.framework.networkMaintenance;

import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.net.IClientMessenger;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * 
 * @author veqryn
 * 
 */
public class ChangeToAutosaveClientAction extends AbstractAction
{
	private static final long serialVersionUID = 1972868158345085949L;
	private final Component m_parent;
	private final IClientMessenger m_clientMessenger;
	private final SaveGameFileChooser.AUTOSAVE_TYPE m_typeOfAutosave;
	
	public ChangeToAutosaveClientAction(final Component parent, final IClientMessenger clientMessenger, final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave)
	{
		super("Change To " + typeOfAutosave.toString().toLowerCase() + "...");
		m_parent = JOptionPane.getFrameForComponent(parent);
		m_clientMessenger = clientMessenger;
		m_typeOfAutosave = typeOfAutosave;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		final int rVal = JOptionPane.showConfirmDialog(m_parent, new JLabel("Change Game To: " + m_typeOfAutosave.toString().toLowerCase()), "Change Game To: "
					+ m_typeOfAutosave.toString().toLowerCase(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (rVal != JOptionPane.OK_OPTION)
			return;
		m_clientMessenger.changeToLatestAutosave(m_typeOfAutosave);
	}
}
