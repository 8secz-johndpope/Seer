 /**
  *  Copyright 2012 Diego Ceccarelli
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 package eu.europeana.ranking.bm25f.similarity;
 
 import java.io.IOException;
 import java.util.Map;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.FieldInvertState;
 import org.apache.lucene.index.Norm;
 import org.apache.lucene.search.CollectionStatistics;
 import org.apache.lucene.search.Explanation;
 import org.apache.lucene.search.TermStatistics;
 import org.apache.lucene.search.similarities.Similarity;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.SmallFloat;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import eu.europeana.ranking.bm25f.params.BM25FParameters;
 
 /**
  * BM25FSimililarity implements the BM25F similarity function.
  * 
  * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
  * 
  *         Created on Nov 15, 2012
  */
 public class BM25FSimilarity extends Similarity {
 	/**
 	 * Logger for this class
 	 */
 	private static final Logger logger = LoggerFactory
 			.getLogger(BM25FSimilarity.class);
 
 	BM25FParameters params;
 	Map<String, Float> boosts;
 	Map<String, Float> lengthBoosts;
 	float k1;
 
 	public BM25FSimilarity() {
 		// logger.info("no defaults");
 		params = new BM25FParameters();
 		boosts = params.getBoosts();
 		lengthBoosts = params.getbParams();
 		k1 = params.getK1();
 	}
 
 	public void setBM25FParams(BM25FParameters bm25fparams) {
 		params = bm25fparams;
 
 		boosts = params.getBoosts();
 		lengthBoosts = params.getbParams();
 		k1 = params.getK1();
 	}
 
 	public String[] getFields() {
 		return params.getFields();
 	}
 
 	public BM25FSimilarity(BM25FParameters params) {
 		// logger.info("defaults");
 		this.params = params;
 		boosts = params.getBoosts();
 		lengthBoosts = params.getbParams();
 		k1 = params.getK1();
 	}
 
 	public BM25FSimilarity(float k1, Map<String, Float> boosts,
 			Map<String, Float> lengthBoosts) {
 		this.k1 = k1;
 		this.boosts = boosts;
 		this.lengthBoosts = lengthBoosts;
 	}
 
 	// Default true
 	protected boolean discountOverlaps = true;
 
 	/** @see #setDiscountOverlaps */
 	public boolean getDiscountOverlaps() {
 		return discountOverlaps;
 	}
 
 	/** Cache of decoded bytes. */
 	private static final float[] NORM_TABLE = new float[256];
 
 	// since lucene store the field lengths is a lossy format,
 	// which is encoded in 1 byte (i.e., 256 different values).
 	// the decoded values are stored in a cache.
 	static {
		NORM_TABLE[0] = 0;
		for (int i = 1; i < 256; i++) {
 			float f = SmallFloat.byte315ToFloat((byte) i);
 			System.out.println(i + "\t-> " + f);
 			NORM_TABLE[i] = 1.0f / (f * f);
 		}
 	}
 
 	/**
 	 * Determines whether overlap tokens (Tokens with 0 position increment) are
 	 * ignored when computing norm. By default this is true, meaning overlap
 	 * tokens do not count when computing norms.
 	 */
 	public void setDiscountOverlaps(boolean v) {
 		discountOverlaps = v;
 	}
 
 	/**
 	 * The default implementation encodes <code>boost / sqrt(length)</code> with
 	 * {@link SmallFloat#floatToByte315(float)}. This is compatible with
 	 * Lucene's default implementation. If you change this, then you should
 	 * change {@link #decodeNormValue(byte)} to match.
 	 */
 	protected byte encodeNormValue(float boost, int fieldLength) {
 		return SmallFloat
 				.floatToByte315(boost / (float) Math.sqrt(fieldLength));
 	}
 
 	/**
 	 * The default implementation returns <code>1 / f<sup>2</sup></code> where
 	 * <code>f</code> is {@link SmallFloat#byte315ToFloat(byte)}.
 	 */
 	protected float decodeNormValue(byte b) {
 		return NORM_TABLE[b & 0xFF];
 	}
 
 	@Override
 	public final void computeNorm(FieldInvertState state, Norm norm) {
 		final int numTerms = discountOverlaps ? state.getLength()
 				- state.getNumOverlap() : state.getLength();
 		norm.setByte(encodeNormValue(state.getBoost(), numTerms));
 	}
 
 	@Override
 	public final ExactSimScorer exactSimScorer(SimWeight weight,
 			AtomicReaderContext context) throws IOException {
 		BM25FSimWeight w = (BM25FSimWeight) weight;
 		System.out.println("field: " + w.field);
 		System.out.println("norms " + context.reader().normValues(w.field));
 		return new BM25FExactSimScorer(w, context.reader().normValues(w.field));
 
 	}
 
 	/**
 	 * Compute the average length for a field, given its stats.
 	 * 
 	 * @param the
 	 *            length statistics of a field.
 	 * @return the average length of the field.
 	 */
 	private float avgFieldLength(CollectionStatistics stats) {
 		// logger.info("sum total term freq \t {}", stats.sumTotalTermFreq());
 		// logger.info("doc count \t {}", stats.docCount());
 		return (float) stats.sumTotalTermFreq() / (float) stats.docCount();
 	}
 
 	/** Implemented as <code>1 / (distance + 1)</code>. */
 	protected float sloppyFreq(int distance) {
 		return 1.0f / (distance + 1);
 	}
 
 	/** The default implementation returns <code>1</code> */
 	protected float scorePayload(int doc, int start, int end, BytesRef payload) {
 		return 1;
 	}
 
 	public Explanation idfExplain(CollectionStatistics collectionStats,
 			TermStatistics termStats) {
 		final long df = termStats.docFreq();
 		final long max = collectionStats.maxDoc();
 		final float idf = idf(df, max);
 		return new Explanation(idf, "idf(docFreq=" + df + ", maxDocs=" + max
 				+ ")");
 	}
 
 	public Explanation idfExplain(CollectionStatistics collectionStats,
 			TermStatistics termStats[]) {
 		final long max = collectionStats.maxDoc();
 		float idf = 0.0f;
 		final Explanation exp = new Explanation();
 		exp.setDescription("idf(), sum of:");
 		for (final TermStatistics stat : termStats) {
 			final long df = stat.docFreq();
 			final float termIdf = idf(df, max);
 			exp.addDetail(new Explanation(termIdf, "idf(docFreq=" + df
 					+ ", maxDocs=" + max + ")"));
 			idf += termIdf;
 		}
 		exp.setValue(idf);
 		return exp;
 	}
 
 	/**
 	 * Return the inverse document frequency (IDF), given the document frequency
 	 * and the number of document in a collection. Implemented as
 	 * 
 	 * <code>log(1 + (numDocs - docFreq + 0.5)/(docFreq + 0.5))</code>.
 	 * 
 	 * @param numDocs
 	 *            the number of documents in the index.
 	 * @param docFreq
 	 *            the number of documents containing the term
 	 * @return the inverse document frequency.
 	 * 
 	 */
 	protected float idf(long docFreq, long numDocs) {
 		return (float) Math.log(1 + (numDocs - docFreq + 0.5D)
 				/ (docFreq + 0.5D));
 	}
 
 	/**
 	 * @return the saturation parameter.
 	 */
 	public float getK1() {
 		return k1;
 	}
 
 	@Override
 	public SimWeight computeWeight(float queryBoost,
 			CollectionStatistics collectionStats, TermStatistics... termStats) {
 		Explanation idf = termStats.length == 1 ? idfExplain(collectionStats,
 				termStats[0]) : idfExplain(collectionStats, termStats);
 
 		boosts = params.getBoosts();
 		lengthBoosts = params.getbParams();
 		k1 = params.getK1();
 
 		String field = collectionStats.field();
 		float avgdl = avgFieldLength(collectionStats);
 
 		float bField = 0;
 		if (lengthBoosts.containsKey(field)) {
 			bField = lengthBoosts.get(field);
 		}
 		// ignoring query boost, using bm25f query boost
 		float boost = 1;
 		if (boosts.containsKey(field)) {
 			boost = boosts.get(field);
 		}
 
 		// compute freq-independent part of bm25 equation across all norm values
 		float cache[] = new float[256];
 		for (int i = 0; i < cache.length; i++) {
 			cache[i] = ((1 - bField) + bField * decodeNormValue((byte) i)
 					/ avgdl);
 		}
 
 		return new BM25FSimWeight(field, idf, boost, avgdl, cache, k1);
 	}
 
 	@Override
 	public SloppySimScorer sloppySimScorer(SimWeight weight,
 			AtomicReaderContext context) throws IOException {
 		BM25FSimWeight w = (BM25FSimWeight) weight;
 		return new BM25FSloppySimScorer(w, context.reader().normValues(w.field));
 	}
 
 	public class BM25FExactSimScorer extends ExactSimScorer {
 
 		private final BM25FSimWeight stats;
 		private final float queryBoost;
 		private final byte[] norms;
 		private final float[] cache;
 
 		// private final float[] cache;
 
 		BM25FExactSimScorer(BM25FSimWeight stats, DocValues norms)
 				throws IOException {
 
 			this.stats = stats;
 			this.cache = stats.cache;
 			this.queryBoost = stats.queryBoost;
 			// this.cache = stats.cache;
 
 			this.norms = norms == null ? null : (byte[]) norms.getSource()
 					.getArray();
 			System.out.println("norms = " + this.norms);
 
 		}
 
 		@Override
 		public float score(int doc, int freq) {
 			if (norms == null) {
 				return queryBoost * freq
 						/ (1 - params.getbParams().get(stats.getField()));
 			}
 			return queryBoost * freq / cache[norms[doc] & 0xFF];
 		}
 
 		@Override
 		public Explanation explain(int doc, Explanation freq) {
 			System.out
 					.println("explain " + this.getClass() + " norms " + norms);
 			return explainScore(doc, freq, stats, norms,
 					score(doc, (int) freq.getValue()));
 		}
 	}
 
 	public class BM25FSimWeight extends SimWeight {
 
 		String field;
 		Explanation idf;
 		float queryBoost;
 		float avgdl;
 		float cache[];
 		float k1;
 
 		float topLevelBoost;
 
 		BM25FParameters params;
 
 		/**
 		 * @param field
 		 * @param idf
 		 * @param queryBoost
 		 * @param avgdl
 		 * @param params
 		 */
 		public BM25FSimWeight(String field, Explanation idf, float queryBoost,
 				float avgdl, float cache[], float k1) {
 			this.field = field;
 			this.idf = idf;
 			this.queryBoost = queryBoost;
 			this.avgdl = avgdl;
 			this.cache = cache;
 			this.k1 = k1;
 
 		}
 
 		@Override
 		public float getValueForNormalization() {
 			// we return a TF-IDF like normalization to be nice, but we don't
 			// actually normalize ourselves.
 			final float queryWeight = idf.getValue() * queryBoost;
 			return queryWeight * queryWeight;
 		}
 
 		@Override
 		public void normalize(float queryNorm, float topLevelBoost) {
 			// we don't normalize with queryNorm at all, we just capture the
 			// top-level boost
 			// this.topLevelBoost = topLevelBoost;
 			// this.weight = queryBoost * topLevelBoost;
 		}
 
 		public String getField() {
 			return field;
 		}
 
 	}
 
 	private Explanation explainScore(int doc, Explanation freq,
 			BM25FSimWeight stats, byte[] norms, float finalScore) {
 		boosts = params.getBoosts();
 		lengthBoosts = params.getbParams();
 		k1 = params.getK1();
 		Explanation result = new Explanation();
 		result.setDescription("score(doc=" + doc + ",field=" + stats.field
 				+ ", freq=" + freq + "), division of:");
 
 		Explanation num = new Explanation();
 		num.setDescription(" numerator, product of: ");
 
 		Explanation boostExpl = new Explanation(stats.queryBoost, "boost["
 				+ stats.field + "]");
 
 		num.addDetail(freq);
 		num.addDetail(boostExpl);
 		num.setValue(freq.getValue() * boostExpl.getValue());
 
 		float b = lengthBoosts.get(stats.field);
 		Explanation bField = new Explanation(b, "lengthBoost(" + stats.field
 				+ ")");
 		Explanation averageLength = new Explanation(stats.avgdl,
 				"avgFieldLength(" + stats.field + ")");
 
 		float length = -1;
 		Explanation fieldLength;
 		if (norms != null) {
 			length = decodeNormValue(norms[doc]);
 			System.out.println("decode norm table norms[doc] " + norms[doc]
 					+ " length = " + length);
 
 			fieldLength = new Explanation(length, "length(" + stats.field + ")");
 			System.out.println("diego cane " + fieldLength.toHtml());
 		} else {
 			fieldLength = new Explanation(-1, "NONORMS - length(" + stats.field
 					+ ")");
 			System.out.println("NO diego cane " + fieldLength.toHtml());
 		}
 
 		Explanation product = new Explanation();
 		product.setDescription("denominator: ((1 - bField) + bField * length / avgFieldLength) :");
 		product.setValue((1 - b) + b * (length / stats.avgdl));
 		product.addDetail(bField);
 		product.addDetail(fieldLength);
 		product.addDetail(averageLength);
 
 		result.addDetail(num);
 		result.addDetail(product);
 		result.setValue(finalScore);
 
 		return result;
 	}
 
 	public class BM25FSloppySimScorer extends SloppySimScorer {
 
 		private final BM25FSimWeight stats;
 		// private final float weightValue; // boost * idf * (k1 + 1)
 		private final byte[] norms;
 
 		BM25FSloppySimScorer(BM25FSimWeight stats, DocValues norms)
 				throws IOException {
 			this.stats = stats;
 			// this.weightValue = stats.weight ;
 			this.norms = norms == null ? null : (byte[]) norms.getSource()
 					.getArray();
 
 		}
 
 		@Override
 		public float score(int doc, float freq) {
 			// FIXME compute score in sloppy sim scorer
 			return freq;
 		}
 
 		@Override
 		public Explanation explain(int doc, Explanation freq) {
 			System.out
 					.println("explain " + this.getClass() + " norms " + norms);
 
 			return explainScore(doc, freq, stats, norms,
 					score(doc, freq.getValue()));
 		}
 
 		@Override
 		public float computeSlopFactor(int distance) {
 			return sloppyFreq(distance);
 		}
 
 		@Override
 		public float computePayloadFactor(int doc, int start, int end,
 				BytesRef payload) {
 			return scorePayload(doc, start, end, payload);
 		}
 
 	}
 
 }
