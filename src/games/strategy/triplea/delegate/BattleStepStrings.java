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

/*
 * BattleStepStrings.java
 *
 * Created on January 16, 2002, 10:39 AM
 */

package games.strategy.triplea.delegate;

/**
 * Constants for the battle steps
 *
 * @author  Sean Bridges
 */
public interface BattleStepStrings
{
  final static String AA_GUNS_FIRE = "AA guns fire";
  final static String SELECT_AA_CASUALTIES = "Select AA casualties";
  final static String REMOVE_AA_CASUALTIES = "Remove AA casualties";

  final static String NAVAL_BOMBARDMENT = "Naval bombardment";
  final static String SELECT_NAVAL_BOMBARDMENT_CASUALTIES = "Select naval bombardment casualties";

  final static String ATTACKER_SUBS_FIRE = "Subs sneak attack";
  final static String DEFENDER_SELECT_SUB_CASUALTIES = "Select sneak attack casualties";
  final static String DEFENDER_REMOVE_SUB_CASUALTIES = "Remove sneak attack casualties";

  final static String ATTACKER_FIRES = " fire";
  final static String DEFENDER_SELECT_CASUALTIES = " select casualties";

  final static String DEFENDER_FIRES_SUBS = " subs fire";
  final static String ATTACKER_SELECT_SUB_CASUALTIES = " select sub casualties";

  final static String DEFENDER_FIRES = " fire";
  final static String ATTACKER_SELECT_CASUALTIES = " select casualties";

  final static String REMOVE_CASUALTIES = "Remove casualties";
  final static String ATTACKER_SUBS_WITHDRAW = " withdraw subs?";
  final static String DEFENDER_SUBS_WITHDRAW = " withdraw subs?";
  final static String ATTACKER_WITHDRAW = " withdraw?";
}
