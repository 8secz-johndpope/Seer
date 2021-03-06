 package com.brentpanther.bitcoinwidget;
 
 import org.apache.http.HttpVersion;
 import org.apache.http.client.HttpClient;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.conn.ClientConnectionManager;
 import org.apache.http.conn.scheme.PlainSocketFactory;
 import org.apache.http.conn.scheme.Scheme;
 import org.apache.http.conn.scheme.SchemeRegistry;
 import org.apache.http.conn.ssl.SSLSocketFactory;
 import org.apache.http.impl.client.BasicResponseHandler;
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
 import org.apache.http.params.BasicHttpParams;
 import org.apache.http.params.HttpParams;
 import org.apache.http.params.HttpProtocolParams;
 import org.apache.http.protocol.HTTP;
 import org.json.JSONArray;
 import org.json.JSONObject;
 
 import java.security.KeyStore;
 
 public enum BTCProvider {
 
     MTGOX(R.array.currencies_mtgox, "mtgx") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             return null;
         }
     },
     COINBASE(R.array.currencies_coinbase, "cb") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject(String.format("https://coinbase.com/api/v1/prices/spot_rate?currency=%s", currencyCode));
             return obj.getString("amount");
         }
     },
     BITSTAMP(R.array.currencies_bitstamp, "btsmp") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject("https://www.bitstamp.net/api/ticker/");
             return obj.getString("last");
         }
     },
     BITCOIN_AVERAGE(R.array.currencies_bitcoinaverage, "btavg") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject(String.format("https://api.bitcoinaverage.com/ticker/%s", currencyCode));
             return obj.getString("last");
         }
     },
     CAMPBX(R.array.currencies_campbx, "cmpbx") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject("http://campbx.com/api/xticker.php");
             return obj.getString("Last Trade");
         }
     },
     BTCE(R.array.currencies_btce, "btce") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject(String.format("https://btc-e.com/api/2/btc_%s/ticker", currencyCode.toLowerCase()));
             return obj.getJSONObject("ticker").getString("last");
         }
     },
     MERCADO(R.array.currencies_mercado, "merc") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject("http://www.mercadobitcoin.com.br/api/ticker/");
             return obj.getJSONObject("ticker").getString("last");
         }
     },
     BITCOINDE(R.array.currencies_bitcoinde, "bt.de") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             return getFromBitcoinCharts("btcdeEUR");
         }
     },
     BITCUREX(R.array.currencies_bitcurex, "btcrx") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject("https://pln.bitcurex.com/data/ticker.json");
             return obj.getString("last");
         }
     },
     BITFINEX(R.array.currencies_bitfinex, "btfnx") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject("https://api.bitfinex.com/v1/ticker/btcusd");
             return obj.getString("last_price");
         }
     },
     BITCOIN_AVERAGE_GLOBAL(R.array.currencies_bitcoinaverage_global, "gbtav") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject(String.format("https://api.bitcoinaverage.com/ticker/global/%s", currencyCode));
             return obj.getString("last");
         }
     },
     BTC_CHINA(R.array.currencies_btcchina, "btchn") {
         @Override
         public String getValue(String currencyCode) throws Exception {
            return getFromBitcoinCharts("btcnCNY");
         }
     },
     BIT2C(R.array.currencies_bit2c, "bit2c") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject("https://www.bit2c.co.il/Exchanges/BtcNis/Ticker.json");
             return obj.getString("av");
         }
     },
     BITPAY(R.array.currencies_bitpay, "btpay") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             HttpGet get = new HttpGet("https://bitpay.com/api/rates");
             HttpClient client = new DefaultHttpClient();
             String result = client.execute(get, new BasicResponseHandler());
             JSONArray array = new JSONArray(result);
             for (int i = 0; i < array.length(); i++) {
                 JSONObject obj = array.getJSONObject(i);
                 if(currencyCode.equals(obj.getString("code"))) {
                     return obj.getString("rate");
                 }
             }
             return null;
         }
     },
     KRAKEN(R.array.currencies_kraken, "krkn") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject(String.format("https://api.kraken.com/0/public/Ticker?pair=XBT%s", currencyCode));
             JSONObject obj2 = obj.getJSONObject("result").getJSONObject("XXBTZ" + currencyCode);
             return (String)obj2.getJSONArray("c").get(0);
         }
     },
     BTCTURK(R.array.currencies_btcturk, "turk") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             JSONObject obj = getJSONObject("https://www.btcturk.com/api/ticker");
             return obj.getString("last");
         }
     },
     VIRTEX(R.array.currencies_virtex, "vrtx") {
         @Override
         public String getValue(String currencyCode) throws Exception {
             return getFromBitcoinCharts("virtexCAD");
         }
     };
 
     private final int currencyArrayID;
     private String label;
 
     BTCProvider(int currencyArrayID, String label) {
         this.currencyArrayID = currencyArrayID;
         this.label = label;
     }
 
     public abstract String getValue(String currencyCode) throws Exception;
 
     public int getCurrencies() {
         return currencyArrayID;
     }
 
     public String getLabel() {
         return label;
     }
 
     private static String getFromBitcoinCharts(String symbol) throws Exception {
         JSONArray array = getJSONArray("http://api.bitcoincharts.com/v1/markets.json");
         for (int i = 0; i < array.length(); i++) {
             JSONObject obj = array.getJSONObject(i);
             if(!symbol.equals(obj.getString("symbol"))) continue;
            return obj.getString("bid");
         }
         return null;
     }
 
     private static JSONObject getJSONObject(String url) throws Exception {
         return new JSONObject(getString(url));
     }
 
     private static JSONArray getJSONArray(String url) throws Exception {
         return new JSONArray(getString(url));
     }
 
     private static String getString(String url) throws Exception {
         HttpGet get = new HttpGet(url);
 
         KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
         trustStore.load(null, null);
         MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
         sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
 
         HttpParams params = new BasicHttpParams();
         HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
         HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
 
         SchemeRegistry registry = new SchemeRegistry();
         registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
         registry.register(new Scheme("https", sf, 443));
 
         ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
         HttpClient client = new DefaultHttpClient(ccm, params);
 
         return client.execute(get, new BasicResponseHandler());
     }
 
 }
