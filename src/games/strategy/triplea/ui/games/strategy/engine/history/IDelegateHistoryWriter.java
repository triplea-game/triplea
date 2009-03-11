/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.history;


/**
 * Has a subset of the historyWriters functionality.
*  Delegates should only have access to these functions.
*  The rest of the history writers functions should only
*  be used by the GameData
 */

public interface IDelegateHistoryWriter
{
    public void startEvent(String eventName);

    public void addChildToEvent(String child);

    public void addChildToEvent(String child, Object renderingData);

    /**
     * Set the redering data for the current event.
     */
    public void setRenderingData(Object renderingData);

}
