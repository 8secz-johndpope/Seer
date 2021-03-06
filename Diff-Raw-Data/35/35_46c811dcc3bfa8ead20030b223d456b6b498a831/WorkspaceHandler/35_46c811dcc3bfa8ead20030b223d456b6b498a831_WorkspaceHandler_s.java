 package net.jps.atom.hopper.abdera;
 
 import net.jps.atom.hopper.adapter.TargetResolverField;
 import net.jps.atom.hopper.config.v1_0.WorkspaceConfiguration;
 import org.apache.abdera.model.Workspace;
 import org.apache.abdera.parser.stax.FOMWorkspace;
 import org.apache.abdera.protocol.server.CollectionInfo;
 import org.apache.abdera.protocol.server.RequestContext;
 import org.apache.abdera.protocol.server.WorkspaceInfo;
 import org.apache.commons.lang.StringUtils;
 
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 
 public class WorkspaceHandler implements WorkspaceInfo {
 
     private final Map<String, TargetAwareAbstractCollectionAdapter> collectionAdapterMap;
     private final WorkspaceConfiguration myConfig;
 
     public WorkspaceHandler(WorkspaceConfiguration myConfig) {
         this.myConfig = myConfig;
         this.collectionAdapterMap = new HashMap<String, TargetAwareAbstractCollectionAdapter>();
     }
 
     public void addCollectionAdapter(String collectionId, TargetAwareAbstractCollectionAdapter adapter) {
         collectionAdapterMap.put(collectionId, adapter);
     }
 
     public TargetAwareAbstractCollectionAdapter getAnsweringAdapter(RequestContext rc) {
         final String feedSpec = rc.getTarget().getParameter(TargetResolverField.FEED.toString());
 
         return !StringUtils.isBlank(feedSpec) ? collectionAdapterMap.get(feedSpec) : null;
     }
 
     @Override
     public Workspace asWorkspaceElement(RequestContext rc) {
        final Workspace ws = new FOMWorkspace(myConfig.getTitle());
        return ws;
     }
 
     @Override
     public Collection<CollectionInfo> getCollections(RequestContext rc) {
         return (Collection) Collections.unmodifiableCollection(collectionAdapterMap.values());
     }
 
     @Override
     public String getTitle(RequestContext rc) {
         return myConfig.getTitle();
     }
 }
