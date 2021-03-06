 package nu.ted.guide.tvdb;
 
 import java.io.InputStream;
import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBException;
 import javax.xml.bind.Unmarshaller;
 import javax.xml.bind.annotation.XmlElement;
 import javax.xml.bind.annotation.XmlRootElement;
 
 import nu.ted.generated.Date;
 import nu.ted.generated.Episode;
 import nu.ted.generated.EpisodeStatus;
 
 @XmlRootElement(name = "Data")
 public class FullSeriesRecord {
 
 	public static class TVDBSeries {
 
 		@XmlElement(name = "SeriesName")
 		private String name;
 
 		@XmlElement(name = "banner")
 		private String banner;
 
 		@XmlElement(name = "Overview")
 		private String overview;
 	}
 
 	private static class TVDBEpisode
 	{
 		@XmlElement(name = "SeasonNumber")
 		private int season;
 
 		@XmlElement(name = "EpisodeNumber")
 		private int episode;
 
 		@XmlElement(name = "FirstAired")
 		private Calendar firstAired;
 
 		@XmlElement(name = "EpisodeName")
 		private String title;
 
 		public int getSeason()
 		{
 			return season;
 		}
 
 		public int getEpisode()
 		{
 			return episode;
 		}
 
 		private void zeroTimeOnFirstAired() {
 			if (firstAired == null) {
 				return;
 			}
 
 			firstAired.set(Calendar.HOUR, 0);
 			firstAired.set(Calendar.MINUTE, 0);
 			firstAired.set(Calendar.SECOND, 0);
 			firstAired.set(Calendar.MILLISECOND, 0);
 		}
 
 		public long getFirstAired()
 		{
 			zeroTimeOnFirstAired();
 			return firstAired.getTimeInMillis();
 		}
 
 		public String getTitle()
 		{
 			return title;
 		}
 	}
 
	private static class EpisodeSorter implements Comparator<TVDBEpisode>, Serializable
 	{
		private static final long serialVersionUID = 1L;

 		public int compare(final TVDBEpisode ep1, final TVDBEpisode ep2)
 		{
 			final int season1 = ep1.getSeason();
 			final int season2 = ep2.getEpisode();
 
 			if (season1 != season2) {
 				return season1 - season2;
 			}
 
 			final int num1 = ep1.getEpisode();
 			final int num2 = ep2.getEpisode();
 
 			if (num1 != num2) {
 				return num1 - num2;
 			}
 			return 0;
 		}
 
 	}
 
 	@XmlElement(name = "Series")
 	private TVDBSeries series;
 
 	// This list should stay sorted.
 	@XmlElement(name = "Episode")
 	private List<TVDBEpisode> episodeList = new ArrayList<TVDBEpisode>();
 
 	public Episode getNextEpisode(Calendar date)
 	{
 		long checkDate = date.getTimeInMillis();
 		for (TVDBEpisode e : episodeList) {
 			if (e.getFirstAired() > checkDate) {
 				return new Episode((short) e.getSeason(), (short) e.getEpisode(),
 						new Date(e.getFirstAired()), EpisodeStatus.SEARCHING);
 			}
 		}
 		return null; // TODO: should throw exception
 	}
 
 	private void sort(Comparator<TVDBEpisode> comparator) {
 		Collections.sort(episodeList, comparator);
 	}
 
 	public String getBanner() {
 		return series.banner;
 	}
 
 	public String getName() {
 		return series.name;
 	}
 
 
 	public String getOverview() {
 		return series.overview;
 	}
 
 	public static FullSeriesRecord create(InputStream is)
 	{
 		try
 		{
 			JAXBContext context = JAXBContext.newInstance(FullSeriesRecord.class);
 			Unmarshaller um = context.createUnmarshaller();
 			FullSeriesRecord record = (FullSeriesRecord) um.unmarshal(is);
 			record.sort(new EpisodeSorter());
 			return record;
 		}
 		catch (JAXBException e)
 		{
 			// If we can't parse the results, return none.
 			return new FullSeriesRecord();
 		}
 	}
 
 }
