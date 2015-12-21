/*
 * Copyright 2015 the original author or authors.
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
 * @author Antonio El Khoury
 */
public class ItalyProvider extends AbstractNavitiaProvider
{
	private static String API_REGION = "it";

	public ItalyProvider(final String apiBase, final String authorization)
	{
		super(NetworkId.IT, apiBase, authorization);

		setTimeZone("Europe/Rome");
	}

	public ItalyProvider(final String authorization)
	{
		super(NetworkId.IT, authorization);

		setTimeZone("Europe/Rome");
	}

	@Override
	public String region()
	{
		return API_REGION;
	}
}
