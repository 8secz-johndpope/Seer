 /*
  * Copyright 2013 Grzegorz Ligas <ligasgr@gmail.com> and other contributors (see the CONTRIBUTORS file).
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.intellij.xquery.runner.rt.unit.datasource;
 
 import org.intellij.xquery.runner.rt.XQueryRunConfig;
 import org.intellij.xquery.runner.rt.datasource.BaseXXQDataSourceFactory;
 import org.intellij.xquery.runner.rt.datasource.ExistXQDataSourceFactory;
 import org.intellij.xquery.runner.rt.datasource.MarklogicXQDataSourceFactory;
 import org.intellij.xquery.runner.rt.datasource.SaxonXQDataSourceFactory;
 import org.intellij.xquery.runner.rt.datasource.SednaXQDataSourceFactory;
 import org.intellij.xquery.runner.rt.datasource.ZorbaXQDataSourceFactory;
 import org.junit.Before;
 import org.junit.Test;
 
 import javax.xml.xquery.XQDataSource;
 
 import static org.hamcrest.Matchers.is;
 import static org.hamcrest.Matchers.not;
 import static org.hamcrest.Matchers.nullValue;
 import static org.junit.Assert.assertThat;
 import static org.mockito.BDDMockito.given;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.verify;
 
 /**
  * User: ligasgr
  * Date: 04/11/13
  * Time: 18:26
  */
 public class XQDataSourceFactoryTest {
 
     private XQueryRunConfig config;
 
     @Before
     public void setUp() {
         config = mock(XQueryRunConfig.class);
     }
 
     @Test
     public void shouldReturnXQDataSourceForBaseXFactory() throws Exception {
         assertThat(new BaseXXQDataSourceFactory().getXQDataSource(config), is(not(nullValue())));
     }
 
     @Test
     public void shouldReturnXQDataSourceForExistFactory() throws Exception {
         assertThat(new ExistXQDataSourceFactory().getXQDataSource(config), is(not(nullValue())));
     }
 
     @Test
     public void shouldReturnXQDataSourceForMarklogicFactory() throws Exception {
         assertThat(new MarklogicXQDataSourceFactory().getXQDataSource(config), is(not(nullValue())));
     }
 
     @Test
     public void shouldReturnXQDataSourceForSaxonFactory() throws Exception {
         assertThat(new SaxonXQDataSourceFactory().getXQDataSource(config), is(not(nullValue())));
     }
 
     @Test
     public void shouldReturnXQDataSourceForSednaFactory() throws Exception {
         assertThat(new SednaXQDataSourceFactory().getXQDataSource(config), is(not(nullValue())));
     }
 
     @Test
     public void shouldReturnXQDataSourceForZorbaFactory() throws Exception {
         assertThat(new ZorbaXQDataSourceFactory().getXQDataSource(config), is(not(nullValue())));
     }
 }
