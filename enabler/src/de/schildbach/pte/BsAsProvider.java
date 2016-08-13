package de.schildbach.pte;

/* 
 * @author 
 * Ignacio Caamaño <ignacio167@gmail.com>
 * */

public class BsAsProvider extends AbstractNavitiaProvider {
	private static String API_REGION = "ar";

	public BsAsProvider(final String apiBase, final String authorization)
	{
		super(NetworkId.BSAS, apiBase, authorization);

		setTimeZone("America/Toronto");
	}

	public BsAsProvider(final String authorization)
	{
		super(NetworkId.ONTARIO, authorization);

		setTimeZone("America/Argentina/Buenos Aires");
	}

	@Override
	public String region() {
		return API_REGION;
	}

}
