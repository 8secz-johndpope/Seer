 /**
  *
  *   Copyright 2011 Xavier Jodoin xjodoin@gmail.com
  *
  *   Licensed under the Apache License, Version 2.0 (the "License");
  *   you may not use this file except in compliance with the License.
  *   You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  *   Unless required by applicable law or agreed to in writing, software
  *   distributed under the License is distributed on an "AS IS" BASIS,
  *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *   See the License for the specific language governing permissions and
  *   limitations under the License.
  */
 package org.torpedoquery.jpa;
 
 import static org.junit.Assert.*;
 import static org.mockito.Matchers.*;
 import static org.mockito.Mockito.*;
 import static org.torpedoquery.jpa.Torpedo.*;
 
 import java.util.Map;
 import java.util.Map.Entry;
 
 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
 import org.junit.Test;
 import org.torpedoquery.jpa.test.bo.Entity;
 import org.torpedoquery.jpa.test.bo.ExtendEntity;
 import org.torpedoquery.jpa.test.bo.SubEntity;
 
 public class TorpedoTest {
 	@Test
 	public void test_createQuery() {
 		final Entity entity = from(Entity.class);
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 		assertEquals("select entity_0 from Entity entity_0", select.getQuery());
 	}
 
 	@Test
 	public void test_selectField() {
 		final Entity entity = from(Entity.class);
 		org.torpedoquery.jpa.Query<String> select = select(entity.getCode());
 		assertEquals("select entity_0.code from Entity entity_0", select.getQuery());
 	}
 
 	@Test
 	public void test_selectMultipleFields() {
 		final Entity entity = from(Entity.class);
 
		org.torpedoquery.jpa.Query<String[]> select = select(entity.getCode(), entity.getName());
 		assertEquals("select entity_0.code, entity_0.name from Entity entity_0", select.getQuery());
 	}
 
 	@Test
 	public void test_selectMultipleFieldsFromDifferentEntities() {
 		final Entity entity = from(Entity.class);
 		final SubEntity subEntity = innerJoin(entity.getSubEntity());
 
		org.torpedoquery.jpa.Query<String[]> select = select(entity.getCode(), subEntity.getCode());
 		assertEquals("select entity_0.code, subEntity_1.code from Entity entity_0 inner join entity_0.subEntity subEntity_1", select.getQuery());
 	}
 
 	@Test
 	public void test_select_multipleFields_keepsOrder() {
 		final Entity entity = from(Entity.class);
 		final SubEntity subEntity = innerJoin(entity.getSubEntity());
 
		org.torpedoquery.jpa.Query<String[]> select = select(subEntity.getCode(), entity.getCode());
 		assertEquals("select subEntity_1.code, entity_0.code from Entity entity_0 inner join entity_0.subEntity subEntity_1", select.getQuery());
 	}
 
 	@Test
 	public void test_innerJoin() {
 		final Entity entity = from(Entity.class);
 
 		innerJoin(entity.getSubEntity());
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 
 		assertEquals("select entity_0 from Entity entity_0 inner join entity_0.subEntity subEntity_1", select.getQuery());
 	}
 
 	@Test
 	public void test_leftJoin() {
 		final Entity entity = from(Entity.class);
 
 		leftJoin(entity.getSubEntity());
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 
 		assertEquals("select entity_0 from Entity entity_0 left join entity_0.subEntity subEntity_1", select.getQuery());
 	}
 
 	@Test
 	public void test_rightJoin() {
 		final Entity entity = from(Entity.class);
 
 		rightJoin(entity.getSubEntity());
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 
 		assertEquals("select entity_0 from Entity entity_0 right join entity_0.subEntity subEntity_1", select.getQuery());
 	}
 
 	@Test
 	public void test_innerJoin_withSelect() {
 		final Entity entity = from(Entity.class);
 		final SubEntity subEntity = innerJoin(entity.getSubEntity());
 
		org.torpedoquery.jpa.Query<String[]> select = select(entity.getCode(), subEntity.getName());
 
 		assertEquals("select entity_0.code, subEntity_1.name from Entity entity_0 inner join entity_0.subEntity subEntity_1", select.getQuery());
 	}
 
 	@Test
 	public void test_innerJoin_withList() {
 		final Entity entity = from(Entity.class);
 		final SubEntity subEntity = innerJoin(entity.getSubEntities());
 
		org.torpedoquery.jpa.Query<String[]> select = select(entity.getCode(), subEntity.getName());
 		assertEquals("select entity_0.code, subEntity_1.name from Entity entity_0 inner join entity_0.subEntities subEntity_1", select.getQuery());
 	}
 
 	@Test
 	public void test_simpleWhere() {
 		final Entity entity = from(Entity.class);
 
 		where(entity.getCode()).eq("test");
 
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 
 		assertEquals("select entity_0 from Entity entity_0 where entity_0.code = :code_1", select.getQuery());
 		assertEquals("test", select.getParameters().get("code_1"));
 	}
 
 	@Test
 	public void test_isNullWhere() {
 		final Entity entity = from(Entity.class);
 		where(entity.getCode()).isNull();
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 
 		assertEquals("select entity_0 from Entity entity_0 where entity_0.code is null", select.getQuery());
 		assertTrue(select.getParameters().isEmpty());
 	}
 
 	@Test
 	public void test_where_primitiveType() {
 		final Entity entity = from(Entity.class);
 		where(entity.isActive()).eq(true);
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 
 		assertEquals("select entity_0 from Entity entity_0 where entity_0.active = :active_1", select.getQuery());
 		assertEquals(true, select.getParameters().get("active_1"));
 	}
 
 	@Test(expected = IllegalArgumentException.class)
 	public void test_multipleWhereRestrictionsResultInConjunction() {
 		final Entity entity = from(Entity.class);
 		where(entity.isActive()).eq(true);
 		where(entity.getCode()).isNull();
 	}
 
 	@Test
 	public void test_singleResult() {
 		final EntityManager entityManager = mock(EntityManager.class);
 		final Query query = mock(Query.class);
 		when(entityManager.createQuery(anyString())).thenReturn(query);
 
 		final Entity entity = from(Entity.class);
 		where(entity.getCode()).eq("test");
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 		select.get(entityManager);
 
 		verify(entityManager).createQuery(select.getQuery());
 		Entry<String, Object> next = select.getParameters().entrySet().iterator().next();
 		verify(query).setParameter(next.getKey(), next.getValue());
 		verify(query).getSingleResult();
 	}
 
 	@Test
 	public void test_resultList() {
 		final EntityManager entityManager = mock(EntityManager.class);
 		final Query query = mock(Query.class);
 		when(entityManager.createQuery(anyString())).thenReturn(query);
 
 		final Entity entity = from(Entity.class);
 		where(entity.getCode()).eq("test");
 		org.torpedoquery.jpa.Query<Entity> select = select(entity);
 		select.list(entityManager);
 
 		verify(entityManager).createQuery(select.getQuery());
 		Entry<String, Object> next = select.getParameters().entrySet().iterator().next();
 		verify(query).setParameter(next.getKey(), next.getValue());
 		verify(query).getResultList();
 	}
 
 	@Test
 	public void test_condition_only_on_join() {
 		Entity from = from(Entity.class);
 		SubEntity innerJoin = innerJoin(from.getSubEntity());
 
 		where(innerJoin.getCode()).eq("test");
 		org.torpedoquery.jpa.Query<Entity> select = select(from);
 		assertEquals("select entity_0 from Entity entity_0 inner join entity_0.subEntity subEntity_1 where subEntity_1.code = :code_2", select.getQuery());
 	}
 
 	@Test
 	public void test_the_bo() {
 		Entity from = from(Entity.class);
 		org.torpedoquery.jpa.Query<Entity> select = select(from);
 		SubEntity innerJoin = innerJoin(from.getSubEntity());
 		where(innerJoin.getCode()).eq("test");
 		assertEquals("select entity_0 from Entity entity_0 inner join entity_0.subEntity subEntity_1 where subEntity_1.code = :code_2", select.getQuery());
 	}
 
 	@Test
 	public void test_Join_Select_Come_Before_The_Root() {
 		Entity from = from(Entity.class);
 		SubEntity innerJoin = innerJoin(from.getSubEntities());
		org.torpedoquery.jpa.Query<String[]> select = select(innerJoin.getName(), from.getCode());
 		String query = select.getQuery();
 		assertEquals("select subEntity_1.name, entity_0.code from Entity entity_0 inner join entity_0.subEntities subEntity_1", query);
 	}
 
 	@Test
 	public void test_parameters_must_not_be_empty_if_ask_before_string() {
 		Entity from = from(Entity.class);
 		where(from.getIntegerField()).eq(1);
 		org.torpedoquery.jpa.Query<Entity> select = select(from);
 		Map<String, Object> parameters = select.getParameters();
 		assertEquals(1, parameters.get("integerField_1"));
 	}
 
 	@Test
 	public void testJoinWith_with_Condition() {
 		Entity from = from(Entity.class);
 		SubEntity innerJoin = innerJoin(from.getSubEntities());
 		with(innerJoin.getCode()).eq("test");
 		org.torpedoquery.jpa.Query<SubEntity> select = select(innerJoin);
 		String query = select.getQuery();
 		Map<String, Object> parameters = select.getParameters();
 		assertEquals("select subEntity_1 from Entity entity_0 inner join entity_0.subEntities subEntity_1 with subEntity_1.code = :code_2", query);
 		assertEquals("test", parameters.get("code_2"));
 	}
 
 	@Test
 	public void testJoinWith_with_ConditionGroupping() {
 		Entity from = from(Entity.class);
 		SubEntity innerJoin = innerJoin(from.getSubEntities());
 		OnGoingLogicalCondition withCondition = condition(innerJoin.getCode()).eq("test").or(innerJoin.getCode()).eq("test2");
 		with(withCondition);
 		org.torpedoquery.jpa.Query<SubEntity> select = select(innerJoin);
 		String query = select.getQuery();
 		assertEquals(
 				"select subEntity_1 from Entity entity_0 inner join entity_0.subEntities subEntity_1 with ( subEntity_1.code = :code_2 or subEntity_1.code = :code_3 )",
 				query);
 	}
 
 	@Test
 	public void testExtend_specificSubClassField() {
 		Entity from = from(Entity.class);
 		ExtendEntity extend = extend(from, ExtendEntity.class);
 		where(extend.getSpecificField()).eq("test");
 
 		org.torpedoquery.jpa.Query<Entity> select = select(from);
 
 		assertEquals("select entity_0 from Entity entity_0 where entity_0.specificField = :specificField_1", select.getQuery());
 		assertEquals("test", select.getParameters().get("specificField_1"));
 	}
 
 	@Test
 	public void testSelectWithChainedMethodCall() {
 		Entity from = from(Entity.class);
 		org.torpedoquery.jpa.Query<String> select = select(from.getSubEntity().getCode());
 		assertEquals("select entity_0.subEntity.code from Entity entity_0", select.getQuery());
 	}
 
 	@Test
 	public void testWhereWithChainedMethodCall() {
 		Entity from = from(Entity.class);
 		where(from.getSubEntity().getCode()).eq("test");
 		org.torpedoquery.jpa.Query<Entity> select = select(from);
 		assertEquals("select entity_0 from Entity entity_0 where entity_0.subEntity.code = :code_1", select.getQuery());
 		assertEquals("test", select.getParameters().get("code_1"));
 	}
 
 	@Test
 	public void testJoinOnMap() {
 		Entity from = from(Entity.class);
 		SubEntity innerJoin = innerJoin(from.getSubEntityMap());
 		String query = select(innerJoin).getQuery();
 		assertEquals("select subEntity_1 from Entity entity_0 inner join entity_0.subEntityMap subEntity_1", query);
 	}
 
 	@Test
 	public void test_innerJoinOnOverrideMethod() {
 		ExtendEntity extendEntity = from(ExtendEntity.class);
 		SubEntity innerJoin = innerJoin(extendEntity.getSubEntity());
 		org.torpedoquery.jpa.Query<SubEntity> select = select(innerJoin);
 		String query = select.getQuery();
 		assertEquals("select subEntity_1 from ExtendEntity extendEntity_0 inner join extendEntity_0.subEntity subEntity_1", query);
 	}
 	
 	@Test
 	public void testTwoJoinOnSameProperty()
 	{
 		Entity from = from(Entity.class);
 		innerJoin(from.getSubEntity());
 		leftJoin(from.getSubEntity());
 		
 		org.torpedoquery.jpa.Query<Entity> select = select(from);
 		assertEquals("select entity_0 from Entity entity_0 inner join entity_0.subEntity subEntity_1 left join entity_0.subEntity subEntity_2", select.getQuery());
 	}
 }
