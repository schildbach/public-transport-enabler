/*
 * Copyright 2010 the original author or authors.
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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * @author Andreas Schildbach
 */
public final class NetworkProviderFactory
{
	private static Reference<BahnProvider> bahnProviderRef;
	private static Reference<OebbProvider> oebbProviderRef;
	private static Reference<SbbProvider> sbbProviderRef;
	private static Reference<VbbProvider> vbbProviderRef;
	private static Reference<RmvProvider> rmvProviderRef;
	private static Reference<MvvProvider> mvvProviderRef;
	private static Reference<TflProvider> tflProviderRef;

	public static synchronized NetworkProvider provider(final String networkId)
	{
		if (networkId.equals(BahnProvider.NETWORK_ID))
		{
			if (bahnProviderRef != null)
			{
				final BahnProvider provider = bahnProviderRef.get();
				if (provider != null)
					return provider;
			}

			final BahnProvider provider = new BahnProvider();
			bahnProviderRef = new SoftReference<BahnProvider>(provider);
			return provider;
		}
		else if (networkId.equals(OebbProvider.NETWORK_ID))
		{
			if (oebbProviderRef != null)
			{
				final OebbProvider provider = oebbProviderRef.get();
				if (provider != null)
					return provider;
			}

			final OebbProvider provider = new OebbProvider();
			oebbProviderRef = new SoftReference<OebbProvider>(provider);
			return provider;
		}
		else if (networkId.equals(SbbProvider.NETWORK_ID))
		{
			if (sbbProviderRef != null)
			{
				final SbbProvider provider = sbbProviderRef.get();
				if (provider != null)
					return provider;
			}

			final SbbProvider provider = new SbbProvider();
			sbbProviderRef = new SoftReference<SbbProvider>(provider);
			return provider;
		}
		else if (networkId.equals(VbbProvider.NETWORK_ID))
		{
			if (vbbProviderRef != null)
			{
				final VbbProvider provider = vbbProviderRef.get();
				if (provider != null)
					return provider;
			}

			final VbbProvider provider = new VbbProvider();
			vbbProviderRef = new SoftReference<VbbProvider>(provider);
			return provider;
		}
		else if (networkId.equals(RmvProvider.NETWORK_ID) || networkId.equals(RmvProvider.NETWORK_ID_ALT))
		{
			if (rmvProviderRef != null)
			{
				final RmvProvider provider = rmvProviderRef.get();
				if (provider != null)
					return provider;
			}

			final RmvProvider provider = new RmvProvider();
			rmvProviderRef = new SoftReference<RmvProvider>(provider);
			return provider;
		}
		else if (networkId.equals(MvvProvider.NETWORK_ID))
		{
			if (mvvProviderRef != null)
			{
				final MvvProvider provider = mvvProviderRef.get();
				if (provider != null)
					return provider;
			}

			final MvvProvider provider = new MvvProvider();
			mvvProviderRef = new SoftReference<MvvProvider>(provider);
			return provider;
		}
		else if (networkId.equals(TflProvider.NETWORK_ID))
		{
			if (tflProviderRef != null)
			{
				final TflProvider provider = tflProviderRef.get();
				if (provider != null)
					return provider;
			}

			final TflProvider provider = new TflProvider();
			tflProviderRef = new SoftReference<TflProvider>(provider);
			return provider;
		}
		else
		{
			throw new IllegalArgumentException(networkId);
		}
	}

	public static String networkId(final NetworkProvider provider)
	{
		if (provider == null)
			throw new IllegalArgumentException("null provider");
		else if (provider instanceof BahnProvider)
			return BahnProvider.NETWORK_ID;
		else if (provider instanceof OebbProvider)
			return OebbProvider.NETWORK_ID;
		else if (provider instanceof SbbProvider)
			return SbbProvider.NETWORK_ID;
		else if (provider instanceof VbbProvider)
			return VbbProvider.NETWORK_ID;
		else if (provider instanceof RmvProvider)
			return RmvProvider.NETWORK_ID;
		else if (provider instanceof MvvProvider)
			return MvvProvider.NETWORK_ID;
		else if (provider instanceof TflProvider)
			return TflProvider.NETWORK_ID;
		else
			throw new IllegalArgumentException(provider.getClass().toString());
	}
}
