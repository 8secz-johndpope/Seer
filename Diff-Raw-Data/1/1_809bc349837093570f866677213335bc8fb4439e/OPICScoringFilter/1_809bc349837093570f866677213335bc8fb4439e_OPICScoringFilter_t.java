 /**
  * Copyright 2006 The Apache Software Foundation
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.apache.nutch.scoring.opic;
 
 import java.util.List;
 
 // Commons Logging imports
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.io.UTF8;
 import org.apache.lucene.document.Document;
 import org.apache.nutch.crawl.CrawlDatum;
 import org.apache.nutch.crawl.Inlinks;
 import org.apache.nutch.fetcher.Fetcher;
 import org.apache.nutch.metadata.Metadata;
 import org.apache.nutch.parse.Parse;
 import org.apache.nutch.parse.ParseData;
 import org.apache.nutch.protocol.Content;
 import org.apache.nutch.scoring.ScoringFilter;
 import org.apache.nutch.scoring.ScoringFilterException;
 import org.apache.nutch.util.LogUtil;
 
 /**
  * This plugin implements a variant of an Online Page Importance Computation
  * (OPIC) score, described in this paper:
  * <a href="http://www2003.org/cdrom/papers/refereed/p007/p7-abiteboul.html"/>
  * Abiteboul, Serge and Preda, Mihai and Cobena, Gregory (2003),
  * Adaptive On-Line Page Importance Computation
  * </a>.
  * 
  * @author Andrzej Bialecki
  */
 public class OPICScoringFilter implements ScoringFilter {
 
   private final static Log LOG = LogFactory.getLog(OPICScoringFilter.class);
 
   private Configuration conf;
   private float scoreInjected;
   private float scorePower;
   private boolean countFiltered;
 
   public Configuration getConf() {
     return conf;
   }
 
   public void setConf(Configuration conf) {
     this.conf = conf;
     scoreInjected = conf.getFloat("db.score.injected", 1.0f);
     scorePower = conf.getFloat("indexer.score.power", 0.5f);
     countFiltered = conf.getBoolean("db.score.count.filtered", false);
   }
 
   /** Set to the value defined in config, 1.0f by default. */
   public void initialScore(UTF8 url, CrawlDatum datum) throws ScoringFilterException {
     datum.setScore(scoreInjected);
   }
 
   /** Use {@link CrawlDatum#getScore()}. */
   public float generatorSortValue(UTF8 url, CrawlDatum datum, float initSort) throws ScoringFilterException {
     return datum.getScore();
   }
 
   /** Increase the score by a sum of inlinked scores. */
   public void updateDbScore(UTF8 url, CrawlDatum old, CrawlDatum datum, List inlinked) throws ScoringFilterException {
     float adjust = 0.0f;
     for (int i = 0; i < inlinked.size(); i++) {
       CrawlDatum linked = (CrawlDatum)inlinked.get(i);
       adjust += linked.getScore();
     }
    if (old == null) old = datum;
     datum.setScore(old.getScore() + adjust);
   }
 
   /** Store a float value of CrawlDatum.getScore() under Fetcher.SCORE_KEY. */
   public void passScoreBeforeParsing(UTF8 url, CrawlDatum datum, Content content) {
     content.getMetadata().set(Fetcher.SCORE_KEY, "" + datum.getScore());
   }
 
   /** Copy the value from Content metadata under Fetcher.SCORE_KEY to parseData. */
   public void passScoreAfterParsing(UTF8 url, Content content, Parse parse) {
     parse.getData().getContentMeta().set(Fetcher.SCORE_KEY, content.getMetadata().get(Fetcher.SCORE_KEY));
   }
 
   /** Get a float value from Fetcher.SCORE_KEY, divide it by the number of outlinks and apply. */
   public CrawlDatum distributeScoreToOutlink(UTF8 fromUrl, UTF8 toUrl, ParseData parseData, CrawlDatum target, CrawlDatum adjust, int allCount, int validCount) throws ScoringFilterException {
     float score = scoreInjected;
     String scoreString = parseData.getContentMeta().get(Fetcher.SCORE_KEY);
     if (scoreString != null) {
       try {
         score = Float.parseFloat(scoreString);
       } catch (Exception e) {
         e.printStackTrace(LogUtil.getWarnStream(LOG));
       }
     }
     if (countFiltered) {
       score /= allCount;
     } else {
       score /= validCount;
     }
     target.setScore(score);
     // XXX (ab) no adjustment? I think this is contrary to the algorithm descr.
     // XXX in the paper, where page "loses" its score if it's distributed to
     // XXX linked pages...
     return null;
   }
 
   /** Dampen the boost value by scorePower.*/
   public float indexerScore(UTF8 url, Document doc, CrawlDatum dbDatum, CrawlDatum fetchDatum, Parse parse, Inlinks inlinks, float initScore) throws ScoringFilterException {
     return (float)Math.pow(dbDatum.getScore(), scorePower);
   }
 }
