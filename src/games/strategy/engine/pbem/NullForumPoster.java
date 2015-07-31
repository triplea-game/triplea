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
package games.strategy.engine.pbem;

import java.io.File;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

/**
 * A dummy forum poster, for when Forum posting is disabled
 */
public class NullForumPoster implements IForumPoster {
  private static final long serialVersionUID = 6465230505089142268L;

  public NullForumPoster() {}

  @Override
  public String getDisplayName() {
    return "disabled";
  }

  @Override
  public boolean getCanViewPosted() {
    return false;
  }

  @Override
  public void setTopicId(final String forumId) {}

  @Override
  public void setUsername(final String s) {}

  @Override
  public void setPassword(final String s) {}

  @Override
  public String getTopicId() {
    return null;
  }

  @Override
  public String getUsername() {
    return null;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public void viewPosted() {}

  @Override
  public void clearSensitiveInfo() {

  }

  @Override
  public String getTestMessage() {
    return "You should not be able to test a Null Poster";
  }

  @Override
  public String getHelpText() {
    return "Will never be called";
  }

  public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
      final String displayName) {}

  public void gameDataChanged(final Change change) {}

  @Override
  public boolean postTurnSummary(final String summary, final String subject) {
    return false;
  }

  @Override
  public String getTurnSummaryRef() {
    return null;
  }

  @Override
  public boolean getIncludeSaveGame() {
    return false;
  }

  @Override
  public void setIncludeSaveGame(final boolean include) {}

  @Override
  public boolean getAlsoPostAfterCombatMove() {
    return false;
  }

  @Override
  public void setAlsoPostAfterCombatMove(final boolean include) {}

  @Override
  public void addSaveGame(final File saveGame, final String fileName) {}

  @Override
  public IForumPoster doClone() {
    return null;
  }

  @Override
  public boolean supportsSaveGame() {
    return false;
  }

  @Override
  public EditorPanel getEditor() {
    return null;
  }

  @Override
  public boolean sameType(final IBean other) {
    return other.getClass() == NullForumPoster.class;
  }
}
