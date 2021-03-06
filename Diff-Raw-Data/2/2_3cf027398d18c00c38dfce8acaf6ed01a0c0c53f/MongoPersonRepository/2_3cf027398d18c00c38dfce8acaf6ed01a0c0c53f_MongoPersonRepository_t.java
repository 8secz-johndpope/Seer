 package org.soluvas.mongo;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Pattern;
 
 import javax.annotation.Nullable;
 
 import org.soluvas.commons.AccountStatus;
 import org.soluvas.commons.CommonsPackage;
 import org.soluvas.commons.EnumNameFunction;
 import org.soluvas.commons.Person;
 import org.soluvas.commons.SlugUtils;
 import org.soluvas.commons.impl.PersonImpl;
 import org.soluvas.data.EntityLookupException;
 import org.soluvas.data.Existence;
 import org.soluvas.data.LookupKey;
 import org.soluvas.data.StatusMask;
 import org.soluvas.data.TrashResult;
 import org.soluvas.data.UntrashResult;
 import org.soluvas.data.domain.CappedRequest;
 import org.soluvas.data.domain.Page;
 import org.soluvas.data.domain.PageRequest;
 import org.soluvas.data.domain.Pageable;
 import org.soluvas.data.domain.Sort;
 import org.soluvas.data.domain.Sort.Direction;
 import org.soluvas.data.person.PersonRepository;
 
 import scala.util.Try;
 
 import com.google.common.base.Preconditions;
 import com.google.common.collect.FluentIterable;
 import com.google.common.collect.ImmutableList;
 import com.google.common.collect.ImmutableMap;
 import com.google.common.collect.ImmutableSet;
 import com.mongodb.BasicDBObject;
 import com.mongodb.DBObject;
 
 /**
  * MongoDB powered {@link Person} repository.
  * @author ceefour
  */
 public class MongoPersonRepository extends MongoRepositoryBase<Person> implements
 		PersonRepository {
 	
 	private static ImmutableMap<String, Integer> indexMap;
 
 	static {
		final ImmutableMap.Builder<String, Integer> indexMab = ImmutableMap.builder();
 		indexMab.put("name", 1); // for sorting in list
 		indexMab.put("creationTime", -1);
 		indexMab.put("modificationTime", -1);
 		indexMab.put("securityRoleIds", 1);
 		indexMab.put("customerRole", 1);
 		indexMab.put("memberRole", 1);
 		indexMab.put("managerRole", 1);
 		indexMap = indexMab.build();
 	}
 	
 	public MongoPersonRepository(String mongoUri, boolean migrationEnabled) {
 		super(Person.class, PersonImpl.class, PersonImpl.CURRENT_SCHEMA_VERSION, mongoUri, "person",
 				ImmutableList.of("canonicalSlug"), indexMap, migrationEnabled);
 	}
 
 	@Override
 	protected void beforeSave(Person entity) {
 		super.beforeSave(entity);
 		entity.setCanonicalSlug(SlugUtils.canonicalize(entity.getSlug()));
 	}
 	
 	@Override @Nullable
 	public Person findOneBySlug(StatusMask statusMask, String upSlug) {
 		return findOneByQuery(new BasicDBObject("canonicalSlug", SlugUtils.canonicalize(upSlug)));
 	}
 
 	@Override
 	public Existence<String> existsBySlug(StatusMask statusMask, String upSlug) {
 		final DBObject dbo = findDBObjectByQuery(new BasicDBObject("canonicalSlug", SlugUtils.canonicalize(upSlug)),
 				new BasicDBObject("slug", 1));
 		if (dbo != null) {
 			return Existence.of((String) dbo.get("slug"), (String) dbo.get("_id"));
 		} else {
 			return Existence.<String>absent();
 		}
 	}
 
 	@Override
 	public Person findOneByFacebook(@Nullable Long facebookId,
 			@Nullable String facebookUsername) {
 		if (facebookId == null && facebookUsername == null) {
 			return null;
 		}
 		final List<DBObject> orCriteria = new ArrayList<>();
 		if (facebookId != null) {
 			orCriteria.add(new BasicDBObject(CommonsPackage.Literals.FACEBOOK_IDENTITY__FACEBOOK_ID.getName(), 
 					facebookId));
 		}
 		if (facebookUsername != null) {
 			orCriteria.add(new BasicDBObject(CommonsPackage.Literals.FACEBOOK_IDENTITY__FACEBOOK_USERNAME.getName(), 
 					facebookUsername));
 		}
 		final BasicDBObject query = new BasicDBObject("$or", orCriteria);
 		return findOneByQuery(query);
 	}
 
 	@Override @Nullable
 	public Person findOneByEmail(StatusMask statusMask, @Nullable String email) {
 		if (email == null) {
 			return null;
 		}
 		final BasicDBObject query = new BasicDBObject("emails", new BasicDBObject("$elemMatch", new BasicDBObject("email", email.toLowerCase().trim())));
 		augmentQueryForStatusMask(query, statusMask);
 		return findOneByQuery(query);
 	}
 	
 	@Override @Nullable
 	public Person findOneByPhoneNumber(StatusMask statusMask, @Nullable String phoneNumber) {
 		if (phoneNumber == null) {
 			return null;
 		}
 		final BasicDBObject query = new BasicDBObject();
 		final BasicDBObject qMobileNumber = new BasicDBObject("mobileNumbers", new BasicDBObject("$elemMatch", new BasicDBObject("phoneNumber", phoneNumber)));
 		final BasicDBObject qPhoneNumber = new BasicDBObject("phoneNumbers", new BasicDBObject("$elemMatch", new BasicDBObject("phoneNumber", phoneNumber)));
 		query.put("$or", new BasicDBObject[] {qMobileNumber, qPhoneNumber});
 		augmentQueryForStatusMask(query, statusMask);
 		return findOneByQuery(query);
 	}
 
 	@Override @Nullable
 	public Person findOneByTwitter(@Nullable Long twitterId,
 			@Nullable String twitterScreenName) {
 		if (twitterId == null && twitterScreenName == null) {
 			return null;
 		}
 		final List<DBObject> orCriteria = new ArrayList<>();
 		if (twitterId != null) {
 			orCriteria.add(new BasicDBObject(CommonsPackage.Literals.TWITTER_IDENTITY__TWITTER_ID.getName(), 
 					twitterId));
 		}
 		if (twitterScreenName != null) {
 			orCriteria.add(new BasicDBObject(CommonsPackage.Literals.TWITTER_IDENTITY__TWITTER_SCREEN_NAME.getName(), 
 					twitterScreenName));
 		}
 		final BasicDBObject query = new BasicDBObject("$or", orCriteria);
 		return findOneByQuery(query);
 	}
 
 	@Override @Nullable
 	public Person findOneByClientAccessToken(@Nullable String clientAccessToken) {
 		if (clientAccessToken == null) {
 			return null;
 		}
 		final BasicDBObject query = new BasicDBObject("clientAccessToken", clientAccessToken);
 		return findOneByQuery(query);
 	}
 
 	@SuppressWarnings("null")
 	@Override
 	public Page<Person> findBySearchText(StatusMask statusMask, @Nullable String searchText, Pageable pageable) {
 		final BasicDBObject queryBySearchText = getQueryBySearchText(searchText);
 		augmentQueryForStatusMask(queryBySearchText, statusMask);
 		final Sort mySort;
 		if (pageable.getSort() != null) {
 			mySort = pageable.getSort().and(new Sort(Direction.DESC, "modificationTime"));
 		} else {
 			mySort = new Sort(Direction.DESC, "modificationTime");
 		}
 		
 		final PageRequest myPageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), mySort);
 		
 		return findAllByQuery(queryBySearchText, myPageable);
 	}
 	
 	private BasicDBObject getQueryBySearchText(String searchText) {
 		final Pattern regex = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
 		
 		final BasicDBObject nameQuery = new BasicDBObject("name", regex);
 		final BasicDBObject idQuery = new BasicDBObject("_id", regex);
 		
 		final BasicDBObject emailQuery = new BasicDBObject("email", regex);
 		final BasicDBObject emailsQuery = new BasicDBObject("emails", new BasicDBObject("$elemMatch", emailQuery));
 		
 		final BasicDBObject phoneNumberQuery = new BasicDBObject("phoneNumber", searchText);
 		final BasicDBObject mobileNumbersQuery = new BasicDBObject("mobileNumbers", new BasicDBObject("$elemMatch", phoneNumberQuery));
 		final BasicDBObject phoneNumbersQuery = new BasicDBObject("phoneNumbers", new BasicDBObject("$elemMatch", phoneNumberQuery));
 		
 		final BasicDBObject query = new BasicDBObject("$or", ImmutableList.of(nameQuery, idQuery , emailsQuery, mobileNumbersQuery, phoneNumbersQuery));
 		log.debug("Query is {}", query);
 		return query;
 	}
 
 	@Override
 	public long countBySearchText(StatusMask statusMask, String searchText) {
 		final BasicDBObject query = getQueryBySearchText(searchText);
 		augmentQueryForStatusMask(query, statusMask);
 		
 		final long count = countByQuery(query);
 		log.debug("Got {} people by query: {}", count, query);
 		return count;
 	}
 	
 	@Override
 	public Page<Person> findAll(StatusMask statusMask, Pageable pageable) {
 		final BasicDBObject query = new BasicDBObject();
 		augmentQueryForStatusMask(query, statusMask);
 		final Page<Person> page = findAllByQuery(query, pageable);
 		return page;
 	}
 
 	@Override
 	public long count(StatusMask statusMask) {
 		final BasicDBObject query = new BasicDBObject();
 		augmentQueryForStatusMask(query, statusMask);
 		final long count = countByQuery(query);
 		log.debug("Got {} record(s) by query: {}", count, query);
 		return count;
 	}
 	
 	protected void augmentQueryForStatusMask(BasicDBObject query, StatusMask statusMask) {
 		Preconditions.checkArgument(!query.containsField("accountStatus"),
 				"Query to be augmented using StatusMask must not already have a 'status' criteria.");
 		switch (statusMask) {
 		case RAW:
 			break;
 		case ACTIVE_ONLY:
 			query.put("accountStatus", new BasicDBObject("$in", 
 					ImmutableSet.of(AccountStatus.ACTIVE.name(), AccountStatus.VERIFIED.name())));
 			break;
 		case INCLUDE_INACTIVE:
 			query.put("accountStatus", new BasicDBObject("$in", 
 					ImmutableSet.of(AccountStatus.ACTIVE.name(), AccountStatus.VERIFIED.name(),
 							AccountStatus.INACTIVE.name())));
 			break;
 		case VOID_ONLY:
 			query.put("accountStatus", AccountStatus.VOID.name());
 			break;
 		default:
 			throw new IllegalArgumentException("Unrecognized StatusMask: " + statusMask);	
 		}
 	}
 
 	@Override
 	public Person findOneActive(String personId) {
 		// FIXME: implement status=ACTIVE|VALIDATED|VERIFIED filter
 		return findOne(personId);
 	}
 
 	@Override
 	public <S extends Person, K extends Serializable> S lookupOne(
 			StatusMask statusMask, LookupKey lookupKey, K key)
 			throws EntityLookupException {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public <S extends Person, K extends Serializable> Map<K, Try<S>> lookupAll(
 			StatusMask statusMask, LookupKey lookupKey, Collection<K> keys) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public <K extends Serializable> Map<K, Existence<K>> checkExistsAll(
 			StatusMask statusMask, LookupKey lookupKey, Collection<K> keys) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public <K extends Serializable> Existence<K> checkExists(StatusMask statusMask,
 			LookupKey lookupKey, K key) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public TrashResult trash(Person entity) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public TrashResult trashById(String id) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public Map<String, Try<TrashResult>> trashAll(Collection<Person> entities) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public Map<String, Try<TrashResult>> trashAllByIds(Collection<String> ids) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public UntrashResult untrash(Person entity) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public UntrashResult untrashById(String id) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public Map<String, Try<UntrashResult>> untrashAll(
 			Collection<Person> entities) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public Map<String, Try<UntrashResult>> untrashAllByIds(
 			Collection<String> ids) {
 		throw new UnsupportedOperationException("to be implemented");
 	}
 
 	@Override
 	public List<Person> findAll(StatusMask statusMask, Collection<String> ids) {
 		final BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", ids));
 		augmentQueryForStatusMask(query, statusMask);
 		return findAllByQuery(query, new CappedRequest(500)).getContent();
 	}
 
 	@Override
 	public List<Person> findAllBySecRoleIds(StatusMask statusMask, Collection<String> secRoleIds) {
 		final BasicDBObject query = new BasicDBObject();
 		query.put("securityRoleIds", new BasicDBObject("$in", secRoleIds));
 		augmentQueryForStatusMask(query, statusMask);
 		log.debug("Find All by secRoleIds + status: {}", query);
 		return findAllByQuery(query, new CappedRequest(500)).getContent();
 	}
 
 	@Override
 	public boolean hasMatchWithSecRoleIds(String personId, Collection<String> secRoleIds) {
 		final BasicDBObject query = new BasicDBObject();
 		query.put("_id", personId);
 		query.put("securityRoleIds", new BasicDBObject("$in", secRoleIds));
 		final Person person = findOneByQuery(query);
 		return person != null;
 	}
 
 	@Override
 	public List<Person> findAllCustomerRoleIds(StatusMask statusMask, Collection<String> customerRoleIds) {
 		final BasicDBObject query = new BasicDBObject("customerRole", new BasicDBObject("$in", customerRoleIds));
 		augmentQueryForStatusMask(query, statusMask);
 		return findAllByQuery(query, new CappedRequest(500)).getContent();
 	}
 
 	@Override
 	public Page<Person> findBySearchText(
 			Collection<AccountStatus> accountStatuses, String searchText,
 			Pageable pageable) {
 		final BasicDBObject query = getQueryBySearchText(searchText);
 		if (!accountStatuses.isEmpty()) {
 			query.put("accountStatus", new BasicDBObject("$in", FluentIterable.from(accountStatuses).transform(new EnumNameFunction()).toList()));
 		}
 		
 		final Sort mySort;
 		if (pageable.getSort() != null) {
 			mySort = pageable.getSort().and(new Sort(Direction.DESC, "modificationTime"));
 		} else {
 			mySort = new Sort(Direction.DESC, "modificationTime");
 		}
 		
 		final PageRequest myPageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), mySort);
 		
 		return findAllByQuery(query, myPageable);
 	}
 
 	@Override
 	public Page<Person> findAll(Collection<AccountStatus> accountStatuses,
 			Pageable pageable) {
 		final BasicDBObject query = new BasicDBObject();
 		if (!accountStatuses.isEmpty()) {
 			query.put("accountStatus", new BasicDBObject("$in", FluentIterable.from(accountStatuses).transform(new EnumNameFunction()).toList()));
 		}
 		
 		final Sort mySort;
 		if (pageable.getSort() != null) {
 			mySort = pageable.getSort().and(new Sort(Direction.DESC, "modificationTime"));
 		} else {
 			mySort = new Sort(Direction.DESC, "modificationTime");
 		}
 		
 		final PageRequest myPageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), mySort);
 		
 		return findAllByQuery(query, myPageable);
 	}
 
 	@Override
 	public long countBySearchText(Collection<AccountStatus> accountStatuses,
 			String searchText) {
 		final BasicDBObject query = getQueryBySearchText(searchText);
 		if (!accountStatuses.isEmpty()) {
 			query.put("accountStatus", new BasicDBObject("$in", FluentIterable.from(accountStatuses).transform(new EnumNameFunction()).toList()));
 		}
 		
 		final long count = countByQuery(query);
 		log.debug("Got {} people by query: {}", count, query);
 		return count;
 	}
 
 	@Override
 	public long countByStatuses(Collection<AccountStatus> accountStatuses) {
 		final BasicDBObject query = new BasicDBObject();
 		if (!accountStatuses.isEmpty()) {
 			query.put("accountStatus", new BasicDBObject("$in", FluentIterable.from(accountStatuses).transform(new EnumNameFunction()).toList()));
 		}
 		final long count = countByQuery(query);
 		log.debug("Got {} record(s) by query: {}", count, query);
 		return count;
 	}
 
 }
