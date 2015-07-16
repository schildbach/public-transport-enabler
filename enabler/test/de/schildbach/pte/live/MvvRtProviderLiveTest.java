package de.schildbach.pte.live;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.schildbach.pte.MvvRtProvider;
import de.schildbach.pte.dto.QueryDeparturesResult;


public class MvvRtProviderLiveTest extends AbstractProviderLiveTest {
	public MvvRtProviderLiveTest()
	{
		super(new MvvRtProvider());
	}


	public void queryDepartures(String id) throws Exception
	{
		final QueryDeparturesResult result = queryDepartures(id, false);
		assertEquals(QueryDeparturesResult.Status.OK, result.status);
		print(result);
	}


	@Test
	public void queryDeparturesMarienplatz() throws Exception
	{
		queryDepartures("1000002");
	}


	@Test
	public void queryDeparturesHauptbahnhof() throws Exception
	{
		queryDepartures("1000006");
	}


	@Test
	public void queryDeparturesSiemenswerke() throws Exception
	{
		queryDepartures("1001310");
	}


	@Test
	public void queryDeparturesKolumbusplatz() throws Exception
	{
		queryDepartures("1000160");
	}

	
	@Test
	public void queryDeparturesMuenchnerFreiheit() throws Exception
	{
		queryDepartures("1000500");
	}
}
