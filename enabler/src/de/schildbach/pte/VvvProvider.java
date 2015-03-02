/*
 * Copyright 2010-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

/**
 * Verkehrsverbund Vogtland
 * 
 * @author Andreas Schildbach
 */
public class VvvProvider extends AbstractEfaProvider
{
	private final static String API_BASE = "http://195.30.98.162:8081/vvv2/";

	public VvvProvider()
	{
		super(NetworkId.VVV, API_BASE);
	}
}
