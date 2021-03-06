 /**
  * 
  */
 package org.cotrix.web.manage.server.modify;
 
 import static org.cotrix.domain.dsl.Codes.*;
 
 import javax.enterprise.inject.Default;
 import javax.inject.Inject;
 import javax.inject.Singleton;
 
 import org.cotrix.domain.codelist.Codelist;
 import org.cotrix.domain.codelist.CodelistLink;
 import org.cotrix.repository.CodelistRepository;
 import org.cotrix.web.common.server.util.LinkTypes;
 import org.cotrix.web.common.shared.codelist.linktype.UILinkType;
 import org.cotrix.web.manage.shared.modify.ModifyCommandResult;
 import org.cotrix.web.manage.shared.modify.linktype.LinkTypeCommand;
 import org.cotrix.web.manage.shared.modify.linktype.UpdatedLinkType;
 
 /**
  * @author "Federico De Faveri federico.defaveri@fao.org"
  *
  */
 @Singleton
 @Default
 public class LinkTypeCommandHandler {
 
 	@Inject
 	CodelistRepository repository;
 
 	public ModifyCommandResult handle(String codelistId, LinkTypeCommand command)
 	{
 		Codelist codelist = repository.lookup(codelistId);
 		
 		UILinkType linkType = command.getItem();
 		CodelistLink codelistLink = null;
 		
 		switch (command.getAction()) {
 			case ADD: {
 				Codelist target = repository.lookup(linkType.getTargetCodelist().getId());
 				codelistLink = ChangesetUtil.addCodelistLink(linkType, target);
 			} break;
 			case UPDATE: {
 				Codelist target = repository.lookup(linkType.getTargetCodelist().getId());	
				CodelistLink oldCodelistLink = lookupLink(codelist, linkType.getId());
 				codelistLink = ChangesetUtil.updateCodelistLink(linkType, target, oldCodelistLink);
 			} break;
 			case REMOVE: {
 				codelistLink = ChangesetUtil.removeCodelistLink(linkType.getId());
 			} break;
 		}
 		
 		if (codelistLink == null) throw new IllegalArgumentException("Unknown command "+command);
 
 		Codelist changeset = modifyCodelist(codelistId).links(codelistLink).build();
 		repository.update(changeset);
 		
		CodelistLink updatedLink = lookupLink(codelist, codelistLink.id());
 		
		return new UpdatedLinkType(updatedLink==null?null:LinkTypes.toUILinkType(updatedLink));
 	}
 	
	private CodelistLink lookupLink(Codelist codelist, String id) {
 		for (CodelistLink link:codelist.links()) if (link.id().equals(id)) return link;
 		return null;
 	}
 }
