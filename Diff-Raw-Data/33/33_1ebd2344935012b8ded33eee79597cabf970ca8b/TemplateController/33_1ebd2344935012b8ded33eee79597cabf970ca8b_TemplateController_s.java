 /**
  * Licensed to Jasig under one or more contributor license
  * agreements. See the NOTICE file distributed with this work
  * for additional information regarding copyright ownership.
  * Jasig licenses this file to you under the Apache License,
  * Version 2.0 (the "License"); you may not use this file
  * except in compliance with the License. You may obtain a
  * copy of the License at:
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on
  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 package org.jasig.ssp.web.api;
 
 import java.util.UUID;
 
 import javax.servlet.http.HttpServletResponse;
 import javax.validation.Valid;
 
 import org.apache.commons.lang.StringUtils;
 import org.jasig.ssp.factory.reference.PlanLiteTOFactory;
 import org.jasig.ssp.factory.reference.TemplateLiteTOFactory;
 import org.jasig.ssp.factory.reference.TemplateTOFactory;
 import org.jasig.ssp.model.Message;
 import org.jasig.ssp.model.ObjectStatus;
 import org.jasig.ssp.model.Person;
 import org.jasig.ssp.model.SubjectAndBody;
 import org.jasig.ssp.model.Template;
 import org.jasig.ssp.model.reference.Config;
 import org.jasig.ssp.security.SspUser;
 import org.jasig.ssp.service.MessageService;
 import org.jasig.ssp.service.ObjectNotFoundException;
 import org.jasig.ssp.service.PersonService;
 import org.jasig.ssp.service.SecurityService;
 import org.jasig.ssp.service.TemplateService;
 import org.jasig.ssp.service.external.TermService;
 import org.jasig.ssp.service.reference.ConfigService;
 import org.jasig.ssp.transferobject.PagedResponse;
 import org.jasig.ssp.transferobject.PlanTO;
 import org.jasig.ssp.transferobject.ServiceResponse;
 import org.jasig.ssp.transferobject.TemplateLiteTO;
 import org.jasig.ssp.transferobject.TemplateOutputTO;
 import org.jasig.ssp.transferobject.TemplateTO;
 import org.jasig.ssp.util.security.DynamicPermissionChecking;
 import org.jasig.ssp.util.sort.PagingWrapper;
 import org.jasig.ssp.util.sort.SortingAndPaging;
 import org.jasig.ssp.web.api.validation.ValidationException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.stereotype.Controller;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.ResponseBody;
  
 @Controller
 @RequestMapping("/1/reference/map/template")
 public class TemplateController  extends AbstractBaseController {
 
 	private static final Logger LOGGER = LoggerFactory
 			.getLogger(TemplateController.class);
 
 	@Override
 	protected Logger getLogger() {
 		return LOGGER;
 	}
 	 
 	@Autowired
 	private TemplateService service;
 	
 	@Autowired
 	private PersonService personService;
 	
 	@Autowired
 	private TermService termService;
 	
 	@Autowired
 	private TemplateTOFactory factory;
 	
 	@Autowired
 	private TemplateLiteTOFactory liteFactory;
 	
 	@Autowired
 	private transient SecurityService securityService;
 	
 	@Autowired
 	private transient ConfigService configService;
 	
 	@Autowired
 	private transient MessageService messageService;
 
  
 	/**
 	 * Retrieves the specified list from persistent storage.
 	 * 
 	 * @param id
 	 *            The specific id to use to lookup the associated data.
 	 * @return The specified instance if found.
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 * @throws ValidationException
 	 *             If that specified data is not invalid.
 	 */ 
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_READ')")
 	@RequestMapping(method = RequestMethod.GET)
 	public @ResponseBody
 	PagedResponse<TemplateTO> get(
 			final @RequestParam(required = false) Boolean isPrivate,
 			final @RequestParam(required = false) ObjectStatus objectStatus,
 			final @RequestParam(required = false) String divisionCode,
 			final @RequestParam(required = false) String programCode,
 			final @RequestParam(required = false) String departmentCode) throws ObjectNotFoundException,
 			ValidationException {
 		final PagingWrapper<Template> data = getService().getAll(
 				SortingAndPaging.createForSingleSortWithPaging(
 						objectStatus == null ? ObjectStatus.ALL : objectStatus, null,
 						null, null, null, null),isPrivate,divisionCode,programCode,departmentCode);
 
 		return new PagedResponse<TemplateTO>(true, data.getResults(), getFactory()
 				.asTOList(data.getRows()));		
 	}
 
 	/**
 	 * Retrieves the specified list from persistent storage.
 	 * 
 	 * @param id
 	 *            The specific id to use to lookup the associated data.
 	 * @return The specified instance if found.
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 * @throws ValidationException
 	 *             If that specified data is not invalid.
 	 */
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_READ')")
 	@RequestMapping(value="/{id}", method = RequestMethod.GET)
 	public @ResponseBody
 	TemplateTO getTemplate(final @PathVariable UUID id) throws ObjectNotFoundException,
 			ValidationException {
 		Template model = getService().get(id);
 		return validatePlan(new TemplateTO(model));
 	}	
  
 	/**
 	 * Retrieves the specified list from persistent storage.  
 	 * 
 	 * @param id
 	 *            The specific id to use to lookup the associated data.
 	 * @return The specified instance if found.
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 * @throws ValidationException
 	 *             If that specified data is not invalid.
 	 */
 	@RequestMapping(value="/summary", method = RequestMethod.GET)
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_READ')")
 	public @ResponseBody
 	PagedResponse<TemplateLiteTO> getSummary(
 			final @RequestParam(required = false) Boolean isPrivate,
 			final @RequestParam(required = false) ObjectStatus objectStatus,
 			final @RequestParam(required = false) String divisionCode,
 			final @RequestParam(required = false) String programCode,
 			final @RequestParam(required = false) String departmentCode) throws ObjectNotFoundException,
 			ValidationException {
 		// Run getAll
 		final PagingWrapper<Template> data = getService().getAll(
 				SortingAndPaging.createForSingleSortWithPaging(
 						objectStatus == null ? ObjectStatus.ALL : objectStatus, null,
 						null, null, null, null),isPrivate,divisionCode,programCode,departmentCode);
 
 		return new PagedResponse<TemplateLiteTO>(true, data.getResults(), getLiteFactory()
 				.asTOList(data.getRows()));		
 	}
 	
 	
 	/**
 	 * Persist a new instance of the specified object.
 	 * <p>
 	 * Must not include an id.
 	 * 
 	 * @param obj
 	 *            New instance to persist.
 	 * @return Original instance plus the generated id.
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 * @throws ValidationException
 	 *             If the specified data contains an id (since it shouldn't).
 	 * @throws CloneNotSupportedException 
 	 */
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_WRITE')")
 	@RequestMapping(method = RequestMethod.POST)
 	public @ResponseBody
 	TemplateTO create(@Valid @RequestBody final TemplateTO obj) throws ObjectNotFoundException,
 			ValidationException, CloneNotSupportedException {
 		if (obj.getId() != null) {
 			throw new ValidationException(
 					"It is invalid to send an entity with an ID to the create method. Did you mean to use the save method instead?");
 		}
 
 		Template model = getFactory().from(obj);
 		
 		model = getService().save(model);
 
 		if (null != model) {
 			final Template createdModel = getFactory().from(obj);
 			if (null != createdModel) {
 				return validatePlan(new TemplateTO(model));
 			}
 		}
 		return null;
 	}
 	
 	/**
 	 * Returns an html page valid for printing
 	 * <p>
 	 *
 	 * 
 	 * @param obj
 	 *            instance to print.
 	 * @return html text strem
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 */
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_READ')")
 	@RequestMapping(value = "/print", method = RequestMethod.POST)
 	public @ResponseBody
 	String print(final HttpServletResponse response,
 			 @RequestBody final TemplateOutputTO planOutputDataTO) throws ObjectNotFoundException {
 
		SubjectAndBody message = getService().createOutput(planOutputDataTO);
 		if(message != null)
 			return message.getBody();
 		
 		return null;
 	}
 	
 	
 	/**
 	 * Returns an html page valid for printing
 	 * <p>
 	 *
 	 * 
 	 * @param obj
 	 *            instance to print.
 	 * @return html text strem
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 * @throws SendFailedException 
 	 */
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_READ')")
 	@RequestMapping(value = "/email", method = RequestMethod.POST)
 	public @ResponseBody
 	String email(final HttpServletResponse response,
 			 @RequestBody final TemplateOutputTO planOutputDataTO) throws ObjectNotFoundException {
 		SubjectAndBody messageText = getService().createOutput(planOutputDataTO);
 		if(messageText == null)
 			return null;
 
 	   messageService.createMessage(planOutputDataTO.getEmailTo(), 
 							planOutputDataTO.getEmailCC(),
 							messageText);
 		
 		return "Map Plan has been queued.";
 	}
 
 	/**
 	 * Persist any changes to the template instance.
 	 * 
 	 * @param id
 	 *            Explicit id to the instance to persist.
 	 * @param obj
 	 *            Full instance to persist.
 	 * @return The update data object instance.
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 * @throws ValidationException
 	 *             If the specified id is null.
 	 * @throws CloneNotSupportedException 
 	 */
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_WRITE')")
 	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
 	public @ResponseBody
 	TemplateTO save(@PathVariable final UUID id, @Valid @RequestBody final TemplateTO obj)
 			throws ValidationException, ObjectNotFoundException, CloneNotSupportedException {
 		if (id == null) {
 			throw new ValidationException(
 					"You submitted without an id to the save method.  Did you mean to create?");
 		}
         
 		if (obj.getId() == null) {
 			obj.setId(id);
 		}
 		final Template oldTemplate = getService().get(id);
 		final Person oldOwner = oldTemplate.getOwner();
 		
 		SspUser currentUser = getSecurityService().currentlyAuthenticatedUser();
 		
 		//If the currently logged in user is not the owner of this plan
 		//we need to create a clone then save it.
 		if(currentUser.getPerson().getId().equals(oldOwner.getId()))
 		{
 			final Template model = getFactory().from(obj);
 			Template savedTemplate = getService().save(model);
 			if (null != model) {
 				return validatePlan(new TemplateTO(savedTemplate));
 			}
 		}
 		else
 		{
 			obj.setId(null);
 			Template model = getFactory().from(obj);
 			final Template clonedTemplate = getService().copyAndSave(model,securityService.currentlyAuthenticatedUser().getPerson());
 
 			if (null != clonedTemplate) {
 				return validatePlan(new TemplateTO(clonedTemplate));
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Marks the specified plan instance with a status of
 	 * {@link ObjectStatus#INACTIVE}.
 	 * 
 	 * @param id
 	 *            The id of the data instance to mark deleted.
 	 * @return Success boolean.
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 */
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_WRITE')")
 	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
 	public @ResponseBody
 	ServiceResponse delete(@PathVariable final UUID id)
 			throws ObjectNotFoundException {
 		getService().delete(id);
 		return new ServiceResponse(true);
 	}
 	
 	/**
 	 * Validate the plan instance.
 	 * 
 	 * @param id
 	 *            Explicit id to the instance to persist.
 	 * @param obj
 	 *            Full instance of plan object.
 	 * @return The validated data object instance.
 	 * @throws ObjectNotFoundException
 	 *             If specified object could not be found.
 	 */
 	@PreAuthorize("hasRole('ROLE_PERSON_MAP_WRITE')")
 	@RequestMapping(value = "/validate", method = RequestMethod.POST)
 	public @ResponseBody
 	TemplateTO validatePlan(final HttpServletResponse response,
 			 @RequestBody final TemplateTO plan)
 			throws ObjectNotFoundException {
 
 		TemplateTO validatedTO = getService().validate(plan);
 		return validatedTO;
 	}
 	
 	private TemplateTO validatePlan(TemplateTO plan) throws ObjectNotFoundException{
 		return getService().validate(plan);
 	}
 
 	public TemplateService getService() {
 		return service;
 	}
 
 	public void setService(TemplateService service) {
 		this.service = service;
 	}
 
 	public TemplateTOFactory getFactory() {
 		return factory;
 	}
 
 	public void setFactory(TemplateTOFactory factory) {
 		this.factory = factory;
 	}
 
 	public SecurityService getSecurityService() {
 		return securityService;
 	}
 
 	public void setSecurityService(SecurityService securityService) {
 		this.securityService = securityService;
 	}
 
 	public TemplateLiteTOFactory getLiteFactory() {
 		return liteFactory;
 	}
 
 	public void setLiteFactory(TemplateLiteTOFactory liteFactory) {
 		this.liteFactory = liteFactory;
 	}
 
 }
