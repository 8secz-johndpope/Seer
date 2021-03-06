 package ru.brandanalyst.frontend.services;
 
 import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
 import ru.brandanalyst.core.model.*;
 import ru.brandanalyst.core.db.provider.*;
 import ru.brandanalyst.frontend.models.GraphForWeb;
 import ru.brandanalyst.frontend.models.SimplyArticleForWeb;
 
 import java.util.List;
 import java.util.ArrayList;
 
 /**
  * Created by IntelliJ IDEA.
  * User: Dmitry Batkovich
  * Date: 10/19/11
  * Time: 7:34 PM
  * gets wide information about brand from db
  */
 public class WideBrandInfoManager {
 
     private final static int NUM_ARTICLES = 6;
     private final SimpleJdbcTemplate jdbcTemplate;
 
     public WideBrandInfoManager(SimpleJdbcTemplate jdbcTemplate) {
         this.jdbcTemplate = jdbcTemplate;
     }
 
     public Brand getBrand(long brandId) {
         BrandProvider brandProvider = new BrandProvider(jdbcTemplate);
         return brandProvider.getBrandById(brandId);
     }
 
     public List<GraphForWeb> getGraphsForBrand(long brandId) {
         GraphProvider graphProvider = new GraphProvider(jdbcTemplate);
         List<Graph> graphList = graphProvider.getGraphsByBrandId(brandId);
         List<GraphForWeb> simpleGraphsList = new ArrayList<GraphForWeb>();
         for (Graph g: graphList) {
             GraphForWeb graphForWeb = new GraphForWeb(g.getTicker());
             for (SingleDot d: g.getGraph()) {
                 graphForWeb.addDot(d);
             }
             simpleGraphsList.add(graphForWeb);
         }
         return simpleGraphsList;
     }
 
     public List<SimplyArticleForWeb> getArticlesForBrand(long brandId) {
         ArticleProvider articleProvider = new ArticleProvider(jdbcTemplate);
         List<Article> articles = articleProvider.getTopArticles(brandId, NUM_ARTICLES);
         List<SimplyArticleForWeb> simplyArticles = new ArrayList<SimplyArticleForWeb>();
         //TODO getting source info by id
         for (Article a : articles) {
             simplyArticles.add(new SimplyArticleForWeb(a.getTitle(), a.getId(), firstPhrase(a.getContent()), Long.toString(a.getSourceId()), "ya.ru"));
         }
         return simplyArticles;
     }
 
     private String firstPhrase(String text) {
//        int point = text.indexOf('.');
//        String t2 = text.substring(point + 1);
        int p = text.indexOf('.');
         return text.substring(0, p) + "...";
        //    return text;
     }
 }
