 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2010, Red Hat, Inc., and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,  
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.jboss.weld.extensions.defaultbean;
 
 import java.lang.annotation.Annotation;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 
 import javax.enterprise.event.Observes;
 import javax.enterprise.inject.Alternative;
 import javax.enterprise.inject.Default;
 import javax.enterprise.inject.spi.AfterBeanDiscovery;
 import javax.enterprise.inject.spi.Bean;
 import javax.enterprise.inject.spi.BeanManager;
 import javax.enterprise.inject.spi.Extension;
 import javax.enterprise.inject.spi.ProcessAnnotatedType;
 import javax.enterprise.inject.spi.ProcessBean;
 
 import org.jboss.weld.extensions.bean.BeanBuilder;
 import org.jboss.weld.extensions.literal.DefaultLiteral;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * This extension allows you to register a 'Default Bean' for a given type and
  * qualifiers. If no bean with the given type and qualifiers is installed then
  * this extensions installs the default bean.
  * 
  * In some ways this is similar to the functionality provided by
  * {@link Alternative} however there are some important distinctions
  * <ul>
  * <li>No XML is required, if an alternative implementation is available it is
  * used automatically</li>
  * <li>The bean is registered across all modules, not on a per module basis</li>
  * </ul>
  * 
  * It is also important to note that beans registered in the
  * {@link AfterBeanDiscovery} event may not been see by this extension
  * 
  * @author Stuart Douglas
  * 
  */
 public class DefaultBeanExtension implements Extension
 {
 
    Logger log = LoggerFactory.getLogger(DefaultBeanExtension.class);
 
    private static final Set<DefaultBeanDefinition> beans = new HashSet<DefaultBeanDefinition>();
 
    private boolean beanDiscoveryOver = false;
 
    /**
     * Adds a default bean with the {@link Default} qualifier
     */
    public static void addDefaultBean(Class<?> type, Bean<?> bean)
    {
       beans.add(new DefaultBeanDefinition(type, Collections.singleton(DefaultLiteral.INSTANCE), bean));
    }
 
    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event, BeanManager beanManager)
    {
       if (event.getAnnotatedType().isAnnotationPresent(DefaultBean.class))
       {
          DefaultBean annotation = event.getAnnotatedType().getAnnotation(DefaultBean.class);
          event.veto();
          BeanBuilder<X> builder = new BeanBuilder<X>(beanManager);
          builder.defineBeanFromAnnotatedType(event.getAnnotatedType());
         builder.setTypes((Set) Collections.singleton(annotation.type()));
          addDefaultBean(annotation.type(), builder.create());
       }
    }
 
    /**
     * Adds a default bean
     */
    public static void addDefaultBean(Class<?> type, Set<Annotation> qualifiers, Bean<?> bean)
    {
       beans.add(new DefaultBeanDefinition(type, Collections.singleton(DefaultLiteral.INSTANCE), bean));
    }
 
    public void processBean(@Observes ProcessBean<?> event)
    {
       if (beanDiscoveryOver)
       {
          return;
       }
       Iterator<DefaultBeanDefinition> it = beans.iterator();
       while (it.hasNext())
       {
          DefaultBeanDefinition definition = it.next();
          if (definition.matches(event.getBean()))
          {
             log.info("Preventing install of default bean " + definition.getDefaultBean());
             it.remove();
          }
       }
    }
 
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event)
    {
       beanDiscoveryOver = true;
       for (DefaultBeanDefinition d : beans)
       {
          log.info("Installing default bean " + d.getDefaultBean());
          event.addBean(d.getDefaultBean());
       }
    }
 
 }
