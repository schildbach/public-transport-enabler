/*
 * Copyright 2010-2014 the original author or authors.
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
public class VblProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VBL;
	private final static String API_BASE = "http://mobil.vbl.ch/vblmobil/";

	public VblProvider()
	{
		super(API_BASE);

		setUseRouteIndexAsTripId(false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.SUGGEST_LOCATIONS || capability == Capability.DEPARTURES || capability == Capability.TRIPS)
				return true;

		return false;
	}

	@Override
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if ("BLS".equals(trainType) && trainNum != null)
				return "RBLS" + trainNum;
			if ("ASM".equals(trainType) && trainNum != null) // Aare Seeland mobil
				return "RASM" + trainNum;
			if ("SOB".equals(trainType) && trainNum != null) // Schweizerische Südostbahn
				return "RSOB" + trainNum;
			if ("RhB".equals(trainType) && trainNum != null) // Rhätische Bahn
				return "RRhB" + trainNum;
			if ("AB-".equals(trainType) && trainNum != null) // Appenzeller Bahnen
				return "RAB" + trainNum;
			if ("BDW".equals(trainType) && trainNum != null) // BDWM Transport
				return "RBDW" + trainNum;
			if ("ZB".equals(trainType) && trainNum != null) // Zentralbahn
				return "RZB" + trainNum;
			if ("TPF".equals(trainType) && trainNum != null) // Transports publics fribourgeois
				return "RTPF" + trainNum;
			if ("MGB".equals(trainType) && trainNum != null) // Matterhorn Gotthard Bahn
				return "RMGB" + trainNum;
			if ("CJ".equals(trainType) && trainNum != null) // Chemins de fer du Jura
				return "RCJ" + trainNum;
			if ("LEB".equals(trainType) && trainNum != null) // Lausanne-Echallens-Bercher
				return "RLEB" + trainNum;
			if ("FAR".equals(trainType) && trainNum != null) // Ferrovie Autolinee Regionali Ticinesi
				return "RFAR" + trainNum;
			if ("WAB".equals(trainType) && trainNum != null) // Wengernalpbahn
				return "RWAB" + trainNum;
			if ("JB".equals(trainType) && trainNum != null) // Jungfraubahn
				return "RJB" + trainNum;
			if ("NSt".equals(trainType) && trainNum != null) // Nyon-St-Cergue-Morez
				return "RNSt" + trainNum;
			if ("RA".equals(trainType) && trainNum != null) // Regionalps
				return "RRA" + trainNum;
			if ("TRN".equals(trainType) && trainNum != null) // Transport Publics Neuchâtelois
				return "RTRN" + trainNum;
			if ("TPC".equals(trainType) && trainNum != null) // Transports Publics du Chablais
				return "RTPC" + trainNum;
			if ("MVR".equals(trainType) && trainNum != null) // Montreux-Vevey-Riviera
				return "RMVR" + trainNum;
			if ("MOB".equals(trainType) && trainNum != null) // Montreux-Oberland Bernois
				return "RMOB" + trainNum;
			if ("TRA".equals(trainType) && trainNum != null) // Transports Vallée de Joux-Yverdon-Ste-Croix
				return "RTRA" + trainNum;
			if ("TMR".equals(trainType) && trainNum != null) // Transports de Martigny et Régions
				return "RTMR" + trainNum;
			if ("GGB".equals(trainType) && trainNum != null) // Gornergratbahn
				return "RGGB" + trainNum;
			if ("BLM".equals(trainType) && trainNum != null) // Lauterbrunnen-Mürren
				return "RBLM" + trainNum;
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}
}
