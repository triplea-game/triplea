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

package games.strategy.triplea.oddsCalculator.zengland;

import java.math.BigDecimal;

public class tester {

	public tester() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BigDecimal per = new BigDecimal(1.001d);
		String res = per.toString();
		int endSpace = 0;
		if(res.indexOf(".")+3>=res.length()||res.indexOf(".") == -1)
		{
			endSpace = res.length();
		}
		else
			endSpace = res.indexOf(".")+3;
		res = res.substring(0, endSpace);
		if(res.indexOf(".")==-1)
			res += ".00";
		res+="%";
	}

}
