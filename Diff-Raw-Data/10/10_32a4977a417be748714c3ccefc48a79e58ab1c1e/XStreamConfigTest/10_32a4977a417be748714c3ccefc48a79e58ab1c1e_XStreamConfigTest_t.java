 /***
  * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource - guilherme.silveira@caelum.com.br
  * All rights reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * 	http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package br.com.caelum.restfulie.config;
 
 import static org.hamcrest.Matchers.containsString;
 import static org.hamcrest.Matchers.equalTo;
 import static org.hamcrest.Matchers.is;
 import static org.hamcrest.Matchers.not;
 import static org.junit.Assert.assertThat;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.HashMap;
 import java.util.List;
 
 import org.junit.Before;
 import org.junit.Test;
 
 import com.thoughtworks.xstream.XStream;
 import com.thoughtworks.xstream.annotations.XStreamAlias;
 
 public class XStreamConfigTest {
 	
 	public static class Address {
 		String street;
 		public Address(String street) {
 			this.street = street;
 		}
 	}
 	
 	public static class Client {
 		String name;
 		Address address;
 		public Client(String name) {
 			this.name = name;
 		}
 		public Client(String name, Address address) {
 			this.name = name;
 			this.address = address;
 		}
 	}
 	
 	@XStreamAlias("item")
 	public static class Item {
 		String name;
 		double price;
 		public Item(String name, double price) {
 			this.name = name;
 			this.price = price;
 		}
 		public String getName() {
 			return name;
 		}
 	}
 	
 	@XStreamAlias("order")
 	public static class Order {
 		Client client;
 		double price;
 		String comments;
 		List<Item> items;
 
 		public Order(Client client, double price, String comments, Item... items) {
 			this.client = client;
 			this.price = price;
 			this.comments = comments;
 			this.items = new ArrayList<Item>(Arrays.asList(items));
 		}
 		public String nice() {
 			return "nice output";
 		}
 
 	}
 	public static class AdvancedOrder extends Order{
 
 		private final String notes;
 
 		public AdvancedOrder(Client client, double price, String comments, String notes) {
 			super(client, price, comments);
 			this.notes = notes;
 		}
 
 	}
 //
 //	@Test
 //	public void shouldSerializeAllBasicFields() {
 //		config.type(Order.class);
 //		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please");
 //		assertThat(create().toXML(order), containsString("<price>15.0</price>"));
 //		assertThat(create().toXML(order), containsString("<comments>pack it nicely, please</comments>"));
 //		assertThat(create().toXML(order), not(containsString("<client>")));
 //		assertThat(create().toXML(order), not(containsString("<item>")));
 //	}
 //
 //	public static enum Type { basic, advanced }
 //	public static class BasicOrder extends Order {
 //		public BasicOrder(Client client, double price, String comments, Type type) {
 //			super(client, price, comments);
 //			this.type = type;
 //		}
 //		private final Type type;
 //	}
 //
 //	@Test
 //	public void shouldSerializeEnumFields() {
 //		config.type(BasicOrder.class);
 //		Order order = new BasicOrder(new Client("guilherme silveira"), 15.0, "pack it nicely, please", Type.basic);
 //		assertThat(create().toXML(order), containsString("<type>basic</type>"));
 //	}
 //
 ////	@Test
 ////	@Ignore("not supported yet")
 ////	public void shouldSerializeCollectionWithPrefixTag() {
 ////		String expectedResult = "<order>\n  <price>15.0</price>\n  <comments>pack it nicely, please</comments>\n</order>";
 ////		expectedResult += expectedResult;
 ////		expectedResult = "<orders>" + expectedResult + "</orders>";
 ////		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please");
 ////		//serializer.from("orders", Arrays.asList(order, order)).serialize();
 ////		assertThat(result(), is(equalTo(expectedResult)));
 ////	}
 ////
 ////	@Test
 ////	@Ignore("not supported yet")
 ////	public void shouldSerializeCollectionWithPrefixTagAndNamespace() {
 ////		String expectedResult = "<o:order>\n  <o:price>15.0</o:price>\n  <o:comments>pack it nicely, please</o:comments>\n</o:order>";
 ////		expectedResult += expectedResult;
 ////		expectedResult = "<o:orders xmlns:o=\"http://www.caelum.com.br/order\">" + expectedResult + "</o:orders>";
 ////		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please");
 //////		serializer.from("orders", Arrays.asList(order, order)).namespace("http://www.caelum.com.br/order","o").serialize();
 ////		assertThat(result(), is(equalTo(expectedResult)));
 ////	}
 //
 //	@Test
 //	public void shouldSerializeParentFields() {
 //		config.type(AdvancedOrder.class);
 //		Order order = new AdvancedOrder(null, 15.0, "pack it nicely, please", "complex package");
 //		assertThat(create().toXML(order), containsString("<notes>complex package</notes>"));
 //	}
 //
 ////	@Test
 ////	public void shouldOptionallyExcludeFields() {
 ////		String expectedResult = "<order>\n  <comments>pack it nicely, please</comments>\n</order>";
 ////		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please");
 ////		serializer.from(order).exclude("price").serialize();
 ////		assertThat(result(), is(equalTo(expectedResult)));
 ////	}
 //
 //	@Test
 //	public void shouldOptionallyIncludeFieldAndNotItsNonPrimitiveFields() {
 //		config.type(AdvancedOrder.class).include("client");
 //		config.type(Client.class);
 //		Order order = new Order(new Client("guilherme silveira", new Address("R. Vergueiro")), 15.0, "pack it nicely, please");
 //		assertThat(create().toXML(order), containsString("<name>guilherme silveira</name>"));
 //		assertThat(create().toXML(order), not(containsString("R. Vergueiro")));
 //	}
 //	
 ////	@Test
 ////	public void shouldOptionallyIncludeChildField() {
 ////		Order order = new Order(new Client("guilherme silveira", new Address("R. Vergueiro")), 15.0, "pack it nicely, please");
 ////		serializer.from(order).include("client", "client.address").serialize();
 ////		assertThat(result(), containsString("<street>R. Vergueiro</street>"));
 ////	}
 ////
 ////
 ////	@Test
 ////	public void shouldOptionallyExcludeChildField() {
 ////		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please");
 ////		serializer.from(order).include("client").exclude("client.name").serialize();
 ////		assertThat(result(), containsString("<client/>"));
 ////		assertThat(result(), not(containsString("<name>guilherme silveira</name>")));
 ////	}
 //	@Test
 //	public void shouldOptionallyIncludeListChildFields() {
 //		config.type(Order.class).include("client", "items");
 //		config.type(Client.class);
 //		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please",
 //				new Item("any item", 12.99));
 //		assertThat(create().toXML(order), containsString("<items>"));
 //		assertThat(create().toXML(order), containsString("<name>any item</name>"));
 //		assertThat(create().toXML(order), containsString("<price>12.99</price>"));
 //		assertThat(create().toXML(order), containsString("</items>"));
 //	}
 //
 //	@Test
 //	public void shouldSupportImplicitCollections() {
 //		config.type(Order.class).include("client").implicit("items");
 //		config.type(Client.class);
 //		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please",
 //				new Item("any item", 12.99));
 //		assertThat(create().toXML(order), not(containsString("<items>")));
 //		assertThat(create().toXML(order), containsString("<name>any item</name>"));
 //		assertThat(create().toXML(order), containsString("<price>12.99</price>"));
 //		assertThat(create().toXML(order), not(containsString("</items>")));
 //	}
 ////	@Test
 ////	public void shouldOptionallyExcludeFieldsFromIncludedListChildFields() {
 ////		Order order = new Order(new Client("guilherme silveira"), 15.0, "pack it nicely, please", new Item("bala", 10.5), new Item("chocolate", 3.3));
 ////		serializer.from(order).include("items").exclude("items.price").serialize();
 ////		assertThat(result(), containsString("<item>\n      <name>bala</name>\n    </item>"));
 ////		assertThat(result(), containsString("<item>\n      <name>chocolate</name>\n    </item>"));
 ////	}
 //
 //	private XStream create() {
 //		return new XStreamConfig(config).create();
 //	}
 //	
 //	@XStreamAlias("receipt")
 //	public class Receipt {
 //
 //		private Calendar paymentTime;
 //
 //		public Calendar getPaymentTime() {
 //			return paymentTime;
 //		}
 //
 //	}
 //
 //	@Test
 //	public void shouldSupportDeserialization() {
 //		HashMap<Class, RestClient> map = new HashMap<Class,RestClient>();
 //		RestClient config = new SimpleConfiguration(Receipt.class);
 //		map.put(Receipt.class, config);
 //		SerializationConfig configs = new SerializationConfig(map);
 //		XStreamConfig xstreamConfig = new XStreamConfig(configs);
 //
 //		String xml = "<receipt></receipt>";
 //		Object result = xstreamConfig.create().fromXML(xml);
 //		assertThat(Receipt.class.isAssignableFrom(result.getClass()), is(equalTo(true)));
 //	}
 //
 //
 //	@Test
 //	public void shouldSupportDeserializationWithLink() {
 //		HashMap<Class, RestClient> map = new HashMap<Class,RestClient>();
 //		RestClient config = new SimpleConfiguration(Receipt.class);
 //		map.put(Receipt.class, config);
 //		SerializationConfig configs = new SerializationConfig(map);
 //		XStreamConfig xstreamConfig = new XStreamConfig(configs);
 //
 //		String xml = "<receipt><link rel=\"hell\" href=\"hell.com\" /></receipt>";
 //		Object result = xstreamConfig.create().fromXML(xml);
 //		assertThat(Receipt.class.isAssignableFrom(result.getClass()), is(equalTo(true)));
 //	}
 //
 //
 //	@Test
 //	public void shouldSupportDeserializationWithLinks() {
 //		HashMap<Class, RestClient> map = new HashMap<Class,RestClient>();
 //		RestClient config = new SimpleConfiguration(Receipt.class);
 //		map.put(Receipt.class, config);
 //		SerializationConfig configs = new SerializationConfig(map);
 //		XStreamConfig xstreamConfig = new XStreamConfig(configs);
 //
 //		String xml = "<receipt><link rel=\"hell\" href=\"hell.com\" /><link rel=\"heaven\" href=\"heaven\" /></receipt>";
 //		Object result = xstreamConfig.create().fromXML(xml);
 //		assertThat(Receipt.class.isAssignableFrom(result.getClass()), is(equalTo(true)));
 //	}
 //
 //
 //	@Test
 //	public void shouldSupportDeserializationWithLinksFromDifferentRoots() {
 //		HashMap<Class, RestClient> map = new HashMap<Class,RestClient>();
 //		RestClient config = new SimpleConfiguration(Receipt.class);
 //		map.put(Receipt.class, config);
 //		map.put(Item.class, new SimpleConfiguration(Item.class));
 //		SerializationConfig configs = new SerializationConfig(map);
 //		XStreamConfig xstreamConfig = new XStreamConfig(configs);
 //
 //		XStream xstream = xstreamConfig.create();
 //		Object first = xstream.fromXML("<item><link rel=\"hell\" href=\"hell.com\" /><link rel=\"heaven\" href=\"heaven\" /></item>");
 //		assertThat(Item.class.isAssignableFrom(first.getClass()), is(equalTo(true)));
 //		xstream = xstreamConfig.create();
 //		String xml = "<receipt><link rel=\"hell\" href=\"hell.com\" /><link rel=\"heaven\" href=\"heaven\" /></receipt>";
 //		Object second = xstreamConfig.create().fromXML(xml);
 //		assertThat(Receipt.class.isAssignableFrom(second.getClass()), is(equalTo(true)));
 //	}
 
 
 }
