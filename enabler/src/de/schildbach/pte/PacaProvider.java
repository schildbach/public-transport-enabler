package de.schildbach.pte;

import de.schildbach.pte.dto.Product;

public class PacaProvider extends AbstractTsiProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.PACA;

	public PacaProvider()
	{
		super("PACA", "http://www.pacamobilite.fr/WebServices/TransinfoService/api");
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.TRIPS)
				return true;

		return false;
	}

	@Override
	protected String translateToLocalProduct(Product p)
	{
		switch (p)
		{
			case HIGH_SPEED_TRAIN:
				return "RAPID_TRANSIT";
			case REGIONAL_TRAIN:
				return "TRAIN|LONG_DISTANCE_TRAIN";
			case SUBURBAN_TRAIN:
				return "LOCAL_TRAIN";
			case SUBWAY:
				return "METRO";
			case TRAM:
				return "TRAMWAY";
			case BUS:
				return "BUS|COACH";
			case ON_DEMAND:
				return "TOD";
			case FERRY:
				return "FERRY";
			case CABLECAR:
			default:
				return null;
		}
	}

}
