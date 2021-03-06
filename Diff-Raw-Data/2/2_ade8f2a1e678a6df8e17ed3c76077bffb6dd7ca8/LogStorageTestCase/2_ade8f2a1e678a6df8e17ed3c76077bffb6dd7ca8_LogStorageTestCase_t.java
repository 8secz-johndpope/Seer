 package com.farpost.logwatcher.storage;
 
 import com.farpost.logwatcher.AggregatedEntry;
 import com.farpost.logwatcher.CountVisitor;
 import com.farpost.logwatcher.LogEntry;
 import com.farpost.logwatcher.Severity;
 import com.farpost.timepoint.Date;
 import com.farpost.timepoint.DateTime;
 import org.testng.annotations.BeforeMethod;
 import org.testng.annotations.Test;
 
 import java.util.List;
 import java.util.Set;
 
 import static com.farpost.logwatcher.LogEntryBuilder.entry;
 import static com.farpost.logwatcher.storage.LogEntries.entries;
 import static com.farpost.timepoint.Date.*;
 import static org.hamcrest.CoreMatchers.equalTo;
 import static org.hamcrest.CoreMatchers.notNullValue;
 import static org.hamcrest.MatcherAssert.assertThat;
 import static org.hamcrest.Matchers.hasItem;
 import static org.testng.Assert.fail;
 
 abstract public class LogStorageTestCase {
 
 	private LogStorage storage;
 
 	private final Date today = today();
 	private final Date yesterday = today().minusDay(1);
 
 	@BeforeMethod
 	public void setUp() throws Exception {
 		storage = createStorage();
 	}
 
 	@Test
 	public void storageCanSaveEntry() throws Exception {
 		DateTime date = today.at("11:35");
 		LogEntry entry = entry().
 			occurred(date).
 			attribute("foo", "bar").
 			create();
 		storage.writeEntry(entry);
 
 		LogEntry entryRef = entries().
 			date(today).
 			find(storage).
 			get(0);
 
 		assertThat(entryRef.getChecksum(), notNullValue());
 		assertThat(entryRef.getDate(), equalTo(date));
 	}
 
 	@Test
 	public void storageShouldReturnNullIfEntryNotFound()
 		throws LogStorageException, InvalidCriteriaException {
 
 		List<LogEntry> entries = entries().applicationId("foo").find(storage);
 		assertThat(entries.size(), equalTo(0));
 	}
 
 	@Test
 	public void storageCanFindRawEntriesByCriteria() throws LogStorageException,
 		InvalidCriteriaException {
 		entry().saveIn(storage);
 		entry().saveIn(storage);
 
 		List<LogEntry> entries = entries().date(today()).find(storage);
 		assertThat(entries.size(), equalTo(2));
 
 		assertThat(entries.get(0).getChecksum(), equalTo(entries.get(1).getChecksum()));
 	}
 
 	@Test
 	public void storageCanSearchByStackTrace() {
 		entry().causedBy(new RuntimeException("first exception")).saveIn(storage);
 		entry().causedBy(new RuntimeException("another first exception")).saveIn(storage);
 		entry().causedBy(new RuntimeException("second exception")).saveIn(storage);
 
		List<LogEntry> entries = entries().date(today()).contains("first").find(storage);
 
 		assertThat(entries.size(), equalTo(2));
 	}
 
 	@Test
 	public void storageCanWalkByEntries() throws LogStorageException, InvalidCriteriaException {
 		entry().message("bar foo bar").saveIn(storage);
 		entry().message("bar foo bar").saveIn(storage);
 		entry().message("bar bar bar").saveIn(storage);
 
 		CountVisitor<LogEntry> visitor = new CountVisitor<LogEntry>();
 		int count = entries().
 			date(today()).
 			contains("foo").
 			walk(storage, visitor);
 
 		assertThat(count, equalTo(2));
 	}
 
 	@Test
 	public void storageCanGetAggregatedEntries() throws Exception {
 		Date date = november(12, 2008);
 		DateTime morning = date.at("11:00");
 		DateTime evening = date.at("18:05");
 
 		entry().
 			applicationId("search").
 			message("Error in search").
 			occurred(morning).
 			saveIn(storage);
 		entry().
 			applicationId("billing").
 			message("Error in billing").
 			occurred(evening).
 			saveIn(storage);
 		entry().
 			applicationId("billing").
 			message("Error in billing").
 			occurred(evening).
 			saveIn(storage);
 
 		List<AggregatedEntry> list = storage.getAggregatedEntries("billing", date, Severity.info);
 		assertThat(list.size(), equalTo(1));
 
 		AggregatedEntry entry = list.get(0);
 		assertThat(entry.getMessage(), equalTo("Error in billing"));
 		assertThat(entry.getCount(), equalTo(2));
 
 		list = storage.getAggregatedEntries("search", date, Severity.info);
 		assertThat(list.size(), equalTo(1));
 
 		entry = list.get(0);
 		assertThat(entry.getMessage(), equalTo("Error in search"));
 		assertThat(entry.getCount(), equalTo(1));
 	}
 
 	@Test
 	public void storageCanFilterEntriesByApplicationId()
 		throws LogStorageException, InvalidCriteriaException {
 		entry().
 			applicationId("frontend").
 			saveIn(storage);
 
 		entry().
 			applicationId("billing").
 			saveIn(storage);
 
 		int count = entries().applicationId("frontend").count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().applicationId("fronTend").count(storage);
 		assertThat(count, equalTo(1));
 	}
 
 	@Test
 	public void storageCanFilterEntriesByDate() throws LogStorageException, InvalidCriteriaException {
 		entry().
 			occurred(today().at("12:22")).message("a").
 			saveIn(storage);
 
 		entry().
 			occurred(yesterday().at("10:00")).message("b").
 			saveIn(storage);
 
 		int count = entries().date(today(), today()).
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().date(yesterday(), today()).
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().date(today().minusDay(2), today()).
 			count(storage);
 		assertThat(count, equalTo(2));
 
 		count = entries().date(today(), tomorrow()).
 			count(storage);
 		assertThat(count, equalTo(0));
 	}
 
 	@Test
 	public void storageShouldNotAggregateEntriesWithNonEqualsChecksum() throws Exception {
 		entry().
 			applicationId("appl").
 			message("foo").
 			saveIn(storage);
 		entry().
 			applicationId("appl").
 			message("bar").
 			saveIn(storage);
 
 		List<AggregatedEntry> list = storage.getAggregatedEntries("appl", today(), Severity.debug);
 		assertThat(list.size(), equalTo(2));
 	}
 
 	@Test
 	public void storageCanReturnUniqueApplicationIdSet() {
 		entry().
 			applicationId("foo").
 			occurred(today.at("12:00")).
 			saveIn(storage);
 		entry().
 			applicationId("bar").
 			occurred(today.at("12:00")).
 			saveIn(storage);
 		entry().
 			applicationId("baz").
 			occurred(yesterday.at("13:32")).
 			saveIn(storage);
 
 		Set<String> ids = storage.getUniquieApplicationIds(today());
 		assertThat(ids.size(), equalTo(2));
 		assertThat(ids, hasItem("foo"));
 		assertThat(ids, hasItem("bar"));
 	}
 
 	@Test
 	public void storageCanCountEntries() throws Exception {
 		LogEntry entry = entry().create();
 
 		storage.writeEntry(entry);
 
 		int count = entries().count(storage);
 		assertThat(count, equalTo(1));
 	}
 
 	@Test
 	public void storageCanCountEntriesByCriteria() {
 		entry().
 			occurred(yesterday.at("12:23")).
 			checksum("ffe").
 			message("foo").
 			saveIn(storage);
 		entry().
 			message("bar").
 			checksum("fff").
 			saveIn(storage);
 
 		int count = entries().
 			date(yesterday).
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().
 			date(yesterday).
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().
 			date(yesterday.minusDay(1)).
 			count(storage);
 		assertThat(count, equalTo(0));
 
 		count = entries().
 			checksum("ffe").
 			count(storage);
 		assertThat(count, equalTo(0));
 	}
 
 	@Test
 	public void storageCanFindEntriesByAttributeValue() {
 		LogEntry entry = entry().attribute("foo", "bar").saveIn(storage);
 
 		List<LogEntry> entries = entries().attribute("foo", "bar").find(storage);
 
 		assertThat(entries.size(), equalTo(1));
 		assertThat(entries.get(0), equalTo(entry));
 	}
 
 	@Test
 	public void storageCanCountEntriesBySeverity()
 		throws LogStorageException, InvalidCriteriaException {
 		entry().
 			severity(Severity.warning).
 			saveIn(storage);
 
 		int count = entries().
 			severity(Severity.warning).
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().
 			severity(Severity.info).
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().
 			severity(Severity.error).
 			count(storage);
 		assertThat(count, equalTo(0));
 	}
 
 	@Test
 	public void storageCanFindEntriesByExceptionType() throws LogStorageException, InvalidCriteriaException {
 		entry().causedBy(new RuntimeException(new Exception())).saveIn(storage);
 
 		int count = entries().
 			causedBy("RuntimeException").
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().
 			causedBy("Exception").
 			count(storage);
 		assertThat(count, equalTo(1));
 
 		count = entries().
 			causedBy("OutOfMemoryError").
 			count(storage);
 		assertThat(count, equalTo(0));
 	}
 
 	@Test
 	public void storageCanRemoveEntriesWithSpecifiedChecksum()
 		throws LogStorageException, InvalidCriteriaException {
 
 		entry().
 			applicationId("application").
 			occurred(today.at("12:35")).
 			message("foo").
 			saveIn(storage);
 
 		entry().
 			applicationId("application").
 			occurred(today.at("12:35")).
 			message("bar").
 			saveIn(storage);
 
 		LogEntry entry = entry().
 			applicationId("application").
 			message("foo").
 			occurred(yesterday.at("11:36")).
 			saveIn(storage);
 
 		storage.removeEntriesWithChecksum(entry.getChecksum());
 
 		assertThat(entries().count(storage), equalTo(1));
 		for (AggregatedEntry en : storage.getAggregatedEntries("application", today, Severity.info)) {
 			if (en.getChecksum().equals(entry.getChecksum())) {
 				fail("Collection must not contains entries with checksum '" + entry.getChecksum() + "'");
 			}
 		}
 	}
 
 	@Test
 	public void storageCanRemoveOldEntries() throws LogStorageException, InvalidCriteriaException {
 		entry().
 			applicationId("foo").
 			occurred(yesterday().at("15:32")).
 			saveIn(storage);
 
 		LogEntry todayEntry = entry().
 			applicationId("foo").
 			occurred(today().at("16:00")).
 			saveIn(storage);
 
 		int removedEntriesCount = storage.removeOldEntries(today());
 		assertThat(removedEntriesCount, equalTo(1));
 
 		List<LogEntryMatcher> criteria = entries().
 			applicationId("foo").
 			criterias();
 		List<LogEntry> entries = storage.findEntries(criteria);
 		assertThat(entries.size(), equalTo(1));
 		assertThat(entries, hasItem(todayEntry));
 	}
 
 	protected abstract LogStorage createStorage() throws Exception;
 }
