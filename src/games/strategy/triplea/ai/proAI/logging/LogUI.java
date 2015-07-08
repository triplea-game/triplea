/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.ai.proAI.logging;

import games.strategy.triplea.ui.TripleAFrame;

import java.util.logging.Level;

import javax.swing.SwingUtilities;

/**
 * Class to manage log window display.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class LogUI
{
	private static TripleAFrame s_frame = null;
	private static LogWindow s_settingsWindow = null;
	private static String currentName = "";
	private static int currentRound = 0;
	
	public static void initialize(final TripleAFrame frame)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread, should be running on AWT thread.");
		s_frame = frame;
		s_settingsWindow = new LogWindow(frame);
	}
	
	public static void clearCachedInstances()
	{
		s_frame = null;
		if (s_settingsWindow != null)
			s_settingsWindow.clear();
		s_settingsWindow = null;
	}
	
	public static void showSettingsWindow()
	{
		if (s_settingsWindow == null) // Shouldn't happen
			return;
		s_settingsWindow.setVisible(true);
	}
	
	public static void notifyAILogMessage(final Level level, final String message)
	{
		if (s_settingsWindow == null) // Shouldn't happen
			return;
		s_settingsWindow.addMessage(level, message);
	}
	
	public static void notifyStartOfRound(final int round, final String name)
	{
		if (s_settingsWindow == null) // Shouldn't happen
			return;
		if (round != currentRound || !name.equals(currentName))
		{
			currentRound = round;
			currentName = name;
			s_settingsWindow.notifyNewRound(round, name);
		}
	}
	
}
