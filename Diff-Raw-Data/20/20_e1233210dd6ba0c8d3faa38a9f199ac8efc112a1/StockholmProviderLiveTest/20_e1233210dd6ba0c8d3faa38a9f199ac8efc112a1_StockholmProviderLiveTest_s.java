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
 
 package de.schildbach.pte.live;
 
 import java.util.Date;
 import java.util.List;
 
 import org.junit.Test;
 
 import de.schildbach.pte.NetworkProvider.Accessibility;
 import de.schildbach.pte.NetworkProvider.WalkSpeed;
 import de.schildbach.pte.StockholmProvider;
 import de.schildbach.pte.dto.Location;
 import de.schildbach.pte.dto.LocationType;
 import de.schildbach.pte.dto.NearbyStationsResult;
 import de.schildbach.pte.dto.Product;
 import de.schildbach.pte.dto.QueryDeparturesResult;
 import de.schildbach.pte.dto.QueryTripsResult;
 
 /**
  * @author Andreas Schildbach
  */
 public class StockholmProviderLiveTest extends AbstractProviderLiveTest
 {
 	public StockholmProviderLiveTest()
 	{
 		super(new StockholmProvider());
 	}
 
 	@Test
 	public void nearbyStations() throws Exception
 	{
		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.STATION, 311109529), 0, 0);
 
 		print(result);
 	}
 
 	@Test
 	public void nearbyStationsByCoordinate() throws Exception
 	{
 		final NearbyStationsResult result = provider.queryNearbyStations(new Location(LocationType.ADDRESS, 59329897, 18072281), 0, 0);
 
 		print(result);
 	}
 
 	@Test
 	public void queryDepartures() throws Exception
 	{
		final QueryDeparturesResult result = provider.queryDepartures(311109529, 0, false);
 
 		print(result);
 	}
 
 	@Test
 	public void autocomplete() throws Exception
 	{
 		final List<Location> autocompletes = provider.autocompleteStations("Luleå Airport");
 
 		print(autocompletes);
 	}
 
 	@Test
 	public void autocompleteUmlaut() throws Exception
 	{
 		final List<Location> autocompletes = provider.autocompleteStations("östra");
 
 		print(autocompletes);
 	}
 
 	@Test
 	public void shortTrip() throws Exception
 	{
 		final QueryTripsResult result = queryTrips(new Location(LocationType.STATION, 200101051, "Stockholm", "T-Centralen"), null, new Location(
 				LocationType.STATION, 200101221, "Stockholm", "Abrahamsberg"), new Date(), true, Product.ALL, WalkSpeed.NORMAL, Accessibility.NEUTRAL);
 		System.out.println(result);
 		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
 		System.out.println(laterResult);
 	}
 
 	@Test
 	public void addressToStationTrip() throws Exception
 	{
 		final QueryTripsResult result = queryTrips(new Location(LocationType.ADDRESS, 0, 59360519, 17989266, null, "Sommarvägen 1, Solna"), null,
 				new Location(LocationType.STATION, 300109205, 59340518, 18081532, "Stockholm", "Stadion"), new Date(), true, Product.ALL,
 				WalkSpeed.NORMAL, Accessibility.NEUTRAL);
 		System.out.println(result);
 		final QueryTripsResult laterResult = queryMoreTrips(result.context, true);
 		System.out.println(laterResult);
 	}
 }
