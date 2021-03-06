 /**
  * This class is generated by jOOQ
  */
 package org.wikapidia.core.jooq;
 
 /**
  * This class is generated by jOOQ.
  */
 @javax.annotation.Generated(value    = {"http://www.jooq.org", "3.0.0"},
                             comments = "This class is generated by jOOQ")
 @java.lang.SuppressWarnings({ "all", "unchecked" })
 public class Public extends org.jooq.impl.SchemaImpl {
 
    private static final long serialVersionUID = 1586518270;
 
 	/**
 	 * The singleton instance of <code>PUBLIC</code>
 	 */
 	public static final Public PUBLIC = new Public();
 
 	/**
 	 * No further instances allowed
 	 */
 	private Public() {
 		super("PUBLIC");
 	}
 
 	@Override
 	public final java.util.List<org.jooq.Sequence<?>> getSequences() {
 		java.util.List result = new java.util.ArrayList();
 		result.addAll(getSequences0());
 		return result;
 	}
 
 	private final java.util.List<org.jooq.Sequence<?>> getSequences0() {
 		return java.util.Arrays.<org.jooq.Sequence<?>>asList(
            org.wikapidia.core.jooq.Sequences.SYSTEM_SEQUENCE_347E173A_8C2F_47AA_857F_41BC04923F7A,
            org.wikapidia.core.jooq.Sequences.SYSTEM_SEQUENCE_9F36558D_BE91_4515_BEB4_CFA2D7E2C999);
 	}
 
 	@Override
 	public final java.util.List<org.jooq.Table<?>> getTables() {
 		java.util.List result = new java.util.ArrayList();
 		result.addAll(getTables0());
 		return result;
 	}
 
 	private final java.util.List<org.jooq.Table<?>> getTables0() {
 		return java.util.Arrays.<org.jooq.Table<?>>asList(
 			org.wikapidia.core.jooq.tables.Article.ARTICLE,
 			org.wikapidia.core.jooq.tables.Link.LINK,
 			org.wikapidia.core.jooq.tables.Concept.CONCEPT,
 			org.wikapidia.core.jooq.tables.ArticleConcept.ARTICLE_CONCEPT);
 	}
 }
