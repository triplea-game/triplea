package games.strategy.triplea.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A Dialog window which has a single component, and a number of buttons.
 * Thi is a wrapper around JOptionPane.showOptionDialog, that allows you to use any names for the button
 *
 * @author Klaus Groenbaek
 */
public class JButtonDialog
{

	/**
	 * Show a new modal dialog and block until the user press a button of closes the dialog
	 *
	 * @param frame   the frame owner
	 * @param title   the dialog title
	 * @param message the String message, or a JPanel
	 * @param buttons the string button options (may not be null)
	 * @return the option pressed or null if the dialog is closed without pressing a button
	 */
	public static String showDialog(final Frame frame, final String title, final Object message, final String... buttons)
	{
		return showDialog(frame, title, message, JOptionPane.PLAIN_MESSAGE, buttons);
	}

	/**
	 * Show a new modal dialog and block until the user press a button of closes the dialog
	 *
	 * @param component the component owner
	 * @param title   the dialog title
	 * @param message the String message, or a JPanel
	 * @param buttons the string button options (may not be null)
	 * @return the option pressed or null if the dialog is closed without pressing a button
	 */
	public static String showDialog(final Component component, final String title, final Object message, final String... buttons)
	{
		return showDialog(JOptionPane.getFrameForComponent(component), title, message, JOptionPane.PLAIN_MESSAGE, buttons);
	}


	/**
	 * Show a new modal dialog and block until the user press a button of closes the dialog
	 *
	 * @param frame   the frame owner
	 * @param title   the dialog title
	 * @param message the String message, or a JPanel
	 * @param buttons the string button options (may not be null)
	 * @param messageType the message type see <code>JOptionPane.PLAIN_MESSAGE, </code>
	 * @return the option pressed or null if the dialog is closed without pressing a button
	 */
	public static String showDialog(final Frame frame, final String title, final Object message, final int messageType, final String... buttons)
	{
		Object[] options = new Object[buttons.length];
		for (int i = 0, buttonsLength = buttons.length; i < buttonsLength; i++)
		{
			options[i] = buttons[i];
		}

		final JOptionPane pane = new JOptionPane(message, messageType);
		pane.setOptions(options);
		final JDialog window = pane.createDialog(frame, title);
		window.setVisible(true);
		return (String) pane.getValue();
	}

	//-----------------------------------------------------------------------
	// constructor
	//-----------------------------------------------------------------------


	private JButtonDialog()
	{
	}
}
