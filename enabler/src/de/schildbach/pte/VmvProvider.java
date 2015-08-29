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
 * @author Andreas Schildbach
 */
public class VmvProvider extends AbstractEfaProvider
{
	private static final String API_BASE = "http://80.146.180.107/vmv2/";

	// http://80.146.180.107/vmv/
	// http://80.146.180.107/delfi/

	public VmvProvider()
	{
		super(NetworkId.VMV, API_BASE);

		setUseRouteIndexAsTripId(false);
	}
}
