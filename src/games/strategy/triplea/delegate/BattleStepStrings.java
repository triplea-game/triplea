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

  final static String SUBS_SNEAK_ATTACK = "Subs sneak attack";
  final static String SELECT_SNEAK_ATTACK_CASUALTIES = "Select sneak attack casualties";
  final static String REMOVE_SNEAK_ATTACK_CASUALTIES = "Remove sneak attack casualties";

  final static String FIRE = " fire";

  final static String SUBS_FIRE = " subs fire";
  final static String SELECT_SUB_CASUALTIES = " select sub casualties";

  final static String SELECT_CASUALTIES = " select casualties";

  final static String REMOVE_CASUALTIES = "Remove casualties";
  final static String SUBS_WITHDRAW = " withdraw subs?";
  final static String PLANES_WITHDRAW = " withdraw planes?";
  final static String SUBS_SUBMERGE = " submerge subs?";

  final static String ATTACKER_WITHDRAW = " withdraw?";
}
