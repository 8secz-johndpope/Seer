 /*
  *  Copyright 2010 cperez.
  * 
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  * 
  *       http://www.apache.org/licenses/LICENSE-2.0
  * 
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  *  under the License.
  */
 
 package org.gitools.biomart.idmapper;
 
 import edu.upf.bg.progressmonitor.IProgressMonitor;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import org.gitools.biomart.restful.BiomartRestfulService;
 import org.gitools.biomart.restful.model.Attribute;
 import org.gitools.biomart.restful.model.Dataset;
 import org.gitools.biomart.restful.model.Query;
 import org.gitools.biomart.utils.tablewriter.SequentialTableWriter;
 import org.gitools.idmapper.AbstractMapper;
 import org.gitools.idmapper.MappingContext;
 import org.gitools.idmapper.MappingData;
 import org.gitools.idmapper.MappingException;
 import org.gitools.idmapper.MappingNode;
 
 
 public class EnsemblMapper extends AbstractMapper implements AllIds {
 
 	private BiomartRestfulService service;
 	private String dataset;
 
 	public EnsemblMapper(BiomartRestfulService service, String dataset) {
		super("Ensembl", false, true);
 
 		this.service = service;
 		this.dataset = dataset;
 	}
 
 	@Override
 	public void initialize(MappingContext context, IProgressMonitor monitor) throws MappingException {
 	}
 
 	@Override
 	public MappingData map(MappingContext context, MappingData data, MappingNode src, MappingNode dst, IProgressMonitor monitor) throws MappingException {
 		String srcInternalName = getInternalName(src.getId());
 		String dstInternalName = getInternalName(dst.getId());
 		if (srcInternalName == null || dstInternalName == null)
 			throw new MappingException("Unsupported mapping from " + src + " to " + dst);
 
 		monitor.begin("Getting mappings from Ensembl ...", 1);
 
 		final Map<String, Set<String>> map = new HashMap<String, Set<String>>();
 
 		Query q = createQuery(dataset, srcInternalName, dstInternalName);
 		try {
 			service.queryModule(q, new SequentialTableWriter() {
 				@Override public void open() throws Exception { }
 				@Override public void close() { }
 
 				@Override public void write(String[] rowFields) throws Exception {
 					String srcf = rowFields[0];
 					String dstf = rowFields[1];
 					Set<String> items = map.get(srcf);
 					if (items == null) {
 						items = new HashSet<String>();
 						map.put(srcf, items);
 					}
 					items.add(dstf);
 				}
 			}, monitor);
 		}
 		catch (Exception ex) {
 			throw new MappingException(ex);
 		}
 
 		monitor.end();
 
 		monitor.begin("Mapping ...", 1);
 
 		if (data.isEmpty())
 			data.identity(map.keySet());
 
 		data.map(map);
 
 		monitor.end();
 
 		return data;
 	}
 
 	@Override
 	public void finalize(MappingContext context, IProgressMonitor monitor) throws MappingException {
 		//throw new UnsupportedOperationException("Not supported yet.");
 	}
 
 	private static final Map<String, String> inameMap = new HashMap<String, String>();
 	static {
 		inameMap.put(PDB, "pdb");
 		inameMap.put(GENEBANK, "embl");
 		inameMap.put(ENTREZ, "entrezgene");
 		inameMap.put(UNIGENE, "unigene");
 		inameMap.put(UNIPROT, "uniprot_swissprot_accession");
 		inameMap.put(GO_BP, "go_biological_process_id");
 		inameMap.put(GO_MF, "go_molecular_function_id");
 		inameMap.put(GO_CL, "go_cellular_component_id");
 	}
 
 	private String getInternalName(String id) {
 		if (id.startsWith("ensembl:")) {
 			id = id.substring(8);
 			if ("genes".equals(id))
 				return "ensembl_gene_id";
 			else if ("transcripts".equals(id))
 				return "ensembl_transcript_id";
 			else if ("proteins".equals(id))
 				return "ensembl_protein_id";
 			else
 				return id;
 		}
 		else
 			return inameMap.get(id);
 	}
 
 	private Query createQuery(String dataset, String srcInternalName, String dstInternalName) {
 		Query q = new Query();
 		Dataset ds = new Dataset();
 		ds.setName(dataset);
 		List<Attribute> attrs = ds.getAttribute();
 		Attribute srcAttr = new Attribute();
 		srcAttr.setName(srcInternalName);
 		attrs.add(srcAttr);
 		Attribute dstAttr = new Attribute();
 		dstAttr.setName(dstInternalName);
 		attrs.add(dstAttr);
 
 		q.getDatasets().add(ds);
 
 		return q;
 	}
 }
 /*biomartService.queryModule(query, new SequentialTableWriter() {
 					@Override public void open() throws Exception { }
 
 					@Override public void close() { }
 
 					@Override public void write(String[] rowFields) throws Exception {
 
 					}
 				}, monitor);*/
