 package org.nosco;
 
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Set;
 
 
 /**
  * This class represents a SQL conditional statement.  (ie: the contents of the {@code where} clause)
  * They are almost always created by {@code Field} instances. &nbsp; For example:
  * {@code SomeClass.ID.eq(123)}
  * would be a condition equivalent to {@code "someclass.id=123"} in SQL.
  * <p>
  * Conditions can be built into any arbitrary tree. &nbsp; For example:
  * {@code SomeClass.ID.eq(123).or(SomeClass.NAME.like("%me%"))} would generate
  * {@code "someclass.id=123 or someclass.name like '%me%'"}
  *
  * @author Derek Anderson
  */
 public abstract class Condition {
 
 	@Override
 	public int hashCode() {
 		final int prime = 31;
 		int result = 1;
 		final String sql = getSQL(null);
 		result = prime * result
 				+ ((sql == null) ? 0 : sql.hashCode());
 		for (final Object x : bindings) {
 			result = prime * result
 					+ ((x == null) ? 0 : x.hashCode());
 		}
 		//System.err.println("Condition hashCode()" +result);
 		return result;
 	}
 
 	@Override
 	public boolean equals(final Object obj) {
 		if (this == obj)
 			return true;
 		if (obj == null)
 			return false;
 		if (getClass() != obj.getClass())
 			return false;
 		final Condition other = (Condition) obj;
 		final String sql = getSQL(null);
 		final String sqlOther = other.getSQL(null);
 		if (sql == null) {
 			if (sqlOther != null)
 				return false;
 		} else if (!sql.equals(sqlOther))
 			return false;
 		final Iterator<Object> it = bindings.iterator();
 		final Iterator<Object> itOther = other.bindings.iterator();
 		while (it.hasNext() && itOther.hasNext()) {
 			final Object x = it.next();
 			final Object y = itOther.next();;
 			if (x == y) continue;
 			if (x == null || y == null) return false;
 			if (!x.equals(y)) return false;
 		}
 		if (it.hasNext() || itOther.hasNext()) return false;
 		return true;
 	}
 
 	static class InTmpTable<T> extends Condition {
 
 		private final Field<T> field;
 		private final Collection<T> set;
 		private final String tmpTableName = "#NOSCO_"+ Math.round(Math.random() * Integer.MAX_VALUE);
 		private String type = null;
 
 		public InTmpTable(final Field<T> field, final Collection<T> set) {
 			this.field = field;
 			// we make a set because we create a PK index on the tmp table
 			this.set = new LinkedHashSet<T>(set);
 			if (Integer.class.equals(field.TYPE)) type = "int";
 			if (Long.class.equals(field.TYPE)) type = "long";
 			if (String.class.equals(field.TYPE)) type = "varchar(4096)";
 			if (Character.class.equals(field.TYPE)) type = "char(1)";
 			if (type == null) throw new RuntimeException("unsupported type for temp table " +
 					"generation: "+ field.TYPE);
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			return set.contains(t.get(field));
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings,
 				final SqlContext context) {
 			sb.append(' ');
 			sb.append(Util.derefField(field, context));
 			sb.append(" in ");
 			sb.append("(select id from "+ tmpTableName +")");
 		}
 
 		@Override
 		public void _preExecute(final Connection conn) throws SQLException {
 			Statement stmt = null;
 			PreparedStatement ps = null;
 			try {
 				stmt = conn.createStatement();
 				final String sql = "CREATE TABLE "+ tmpTableName + "(id "+ type +", PRIMARY KEY (id))";
 				Util.log(sql, null);
 				stmt.execute(sql);
 				ps = conn.prepareStatement("insert into "+ tmpTableName +" values (?)");
 				int i = 0;
 				int added = 0;
 				for (final T t : set) {
 					++i;
 					if (t instanceof Character) ps.setString(1, t.toString());
 					else ps.setObject(1, t);
 					ps.addBatch();
 					if (i%64 == 0) for (final int x : ps.executeBatch()) added += x;
 				}
 				if (i%64 != 0) {
 					for (final int x : ps.executeBatch()) {
 						added += x;
 					}
 				}
 			} catch (final SQLException e) {
 				throw e;
 			} finally {
 				try {
 					if (stmt!=null && !stmt.isClosed()) stmt.close();
 				} catch (final SQLException e) {
 					e.printStackTrace();
 				}
 				try {
 					if (ps!=null && !ps.isClosed()) ps.close();
 				} catch (final SQLException e) {
 					e.printStackTrace();
 				}
 			}
 		}
 
 		@Override
 		public void _postExecute(final Connection conn) throws SQLException {
 			Statement stmt = null;
 			try {
 				stmt = conn.createStatement();
 				final String sql = "DROP TABLE "+ tmpTableName;
 				Util.log(sql, null);
 				stmt.execute(sql);
 			} catch (final SQLException e) {
 				throw e;
 			} finally {
 				try {
 					if (stmt!=null && !stmt.isClosed()) stmt.close();
 				} catch (final SQLException e) {
 					e.printStackTrace();
 				}
 			}
 		}
 
 	}
 
 	/**
 	 * always true
 	 */
 	public static final Condition TRUE = new Condition() {
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(" 1=1");
 		}
 		@Override
 		boolean matches(final Table t) {
 			return true;
 		}
 	};
 
 	/**
 	 * always false
 	 */
 	public static final Condition FALSE = new Condition() {
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(" 1=0");
 		}
 		@Override
 		boolean matches(final Table t) {
 			return false;
 		}
 	};
 
 	/**
 	 * Use this class to add custom SQL code to your where clause. &nbsp;
 	 * For Example:
 	 * <pre>   {@code SomeClass.ALL.where(new Condition.Literal("id = 1"));}</pre>
 	 * Obviously this should be used sparingly, and can break compatibility between
 	 * different databases. &nbsp;  But it's here if you really need it.
 	 *
 	 * @author Derek Anderson
 	 */
 	public static class Literal extends Condition {
 
 		private final String s;
 
 		public Literal(final String s) {
 			this.s = s;
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			throw new RuntimeException("literal conditions cannot be applied to in-memory queries");
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(" ");
 			sb.append(s);
 		}
 
 	}
 
 	List<Object> bindings = null;
 
 	/**
 	 * Internal function.  Do not use.  Subject to change.
 	 */
 	String getSQL(final SqlContext context) {
 		final StringBuffer sb = new StringBuffer();
 		bindings = new ArrayList<Object>();
 		getSQL(sb, bindings, context);
 		return sb.toString();
 	}
 
 	/**
 	 * Internal function.  Do not use.  Subject to change.
 	 */
 	List<?> getSQLBindings() {
 		return bindings;
 	}
 
 	/**
 	 * Internal function.  Do not use.  Subject to change.
 	 */
 	abstract boolean matches(Table t);
 
 	/**
 	 * Internal function.  Do not use.  Subject to change.
 	 */
 	protected abstract void getSQL(StringBuffer sb, List<Object> bindings, SqlContext context);
 
 	/**
 	 * Creates a new condition negating the current condition.
 	 * @return A new condition negating the current condition
 	 */
 	public Condition not() {
 		return new Not(this);
 	}
 
 	/**
 	 * Creates a condition that represents the logical AND between this condition and the given conditions.
 	 * @param conditions
 	 * @return A new condition ANDing this with the given conditions
 	 */
 	public Condition and(final Condition... conditions) {
 		final AndCondition c = new AndCondition(conditions);
 		c.conditions.add(this);
 		return c;
 	}
 
 	/**
 	 * Creates a condition that represents the logical OR between this condition and the given conditions.
 	 * @param conditions
 	 * @return A new condition ORing this with the given conditions
 	 */
 	public Condition or(final Condition... conditions) {
 		final OrCondition c = new OrCondition(conditions);
 		c.conditions.add(this);
 		return c;
 	}
 
 	private static class AndCondition extends Condition {
 
 		List<Condition> conditions = new ArrayList<Condition>();
 
 		public AndCondition(final Condition[] conditions) {
 			for (final Condition condition : conditions) {
 				if (condition instanceof AndCondition) {
 					for (final Condition c : ((AndCondition)condition).conditions) {
 						this.conditions.add(c);
 					}
 				} else {
 					this.conditions.add(condition);
 				}
 			}
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append("(");
 			for (int i=0; i<conditions.size(); ++i) {
 				final Condition condition = conditions.get(i);
 				condition.getSQL(sb, bindings, context);
 				if (i<conditions.size()-1) {
 					sb.append(" and ");
 				}
 			}
 			sb.append(")");
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			for (final Condition c : conditions) {
 				if (!c.matches(t)) return false;
 			}
 			return true;
 		}
 
 		@Override
 		void _preExecute(Connection conn) throws SQLException {
 			super._preExecute(conn);
 			for (Condition condition : conditions) {
 				condition._preExecute(conn);
 			}
 		}
 
 		@Override
 		void _postExecute(Connection conn) throws SQLException {
 			super._postExecute(conn);
 			for (Condition condition : conditions) {
 				condition._postExecute(conn);
 			}
 		}
 
 	}
 
 	private static class OrCondition extends Condition {
 
 		List<Condition> conditions = new ArrayList<Condition>();
 
 		public OrCondition(final Condition[] conditions) {
 			for (final Condition condition : conditions) {
 				if (condition instanceof OrCondition) {
 					for (final Condition c : ((OrCondition)condition).conditions) {
 						this.conditions.add(c);
 					}
 				} else {
 					this.conditions.add(condition);
 				}
 			}
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append("(");
 			for (int i=0; i<conditions.size(); ++i) {
 				final Condition condition = conditions.get(i);
 				condition.getSQL(sb, bindings, context);
 				if (i<conditions.size()-1) {
 					sb.append(" or ");
 				}
 			}
 			sb.append(")");
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			for (final Condition c : conditions) {
 				if (c.matches(t)) return true;
 			}
 			return false;
 		}
 
 		@Override
 		void _preExecute(Connection conn) throws SQLException {
 			super._preExecute(conn);
 			for (Condition condition : conditions) {
 				condition._preExecute(conn);
 			}
 		}
 
 		@Override
 		void _postExecute(Connection conn) throws SQLException {
 			super._postExecute(conn);
 			for (Condition condition : conditions) {
 				condition._postExecute(conn);
 			}
 		}
 
 	}
 
 	static class Not extends Condition {
 
 		private final Condition condition;
 		private boolean parens = true;
 
 		public Not(final Condition condition) {
 			this.condition = condition;
 		}
 
 		public Not(final Condition condition, final boolean parens) {
 			this.condition = condition;
 			this.parens = parens;
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(" not ");
 			if (parens) sb.append("(");
 			condition.getSQL(sb, bindings, context);
 			if (parens) sb.append(")");
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			return !condition.matches(t);
 		}
 
 		@Override
 		void _preExecute(Connection conn) throws SQLException {
 			super._preExecute(conn);
 			condition._preExecute(conn);
 		}
 
 		@Override
 		void _postExecute(Connection conn) throws SQLException {
 			super._postExecute(conn);
 			condition._postExecute(conn);
 		}
 
 	}
 
 	static class Ternary extends Condition {
 
 		private final Field<?> field;
 		private final String cmp1;
 		private final String cmp2;
 		private final Object v1;
 		private final Object v2;
 		private final Function function1;
 		private final Function function2;
 
 		public Ternary(final Field<?> field, final String cmp1, final Object v1, final String cmp2, final Object v2) {
 			this.field = field;
 			this.cmp1 = cmp1;
 			this.cmp2 = cmp2;
 			this.v1 = v1;
 			this.v2 = v2;
 			function1 = null;
 			function2 = null;
 		}
 
 		public Ternary(final Field<?> field, final String cmp1, final Function f1, final String cmp2, final Object v2) {
 			this.field = field;
 			this.cmp1 = cmp1;
 			this.cmp2 = cmp2;
 			this.v1 = null;
 			this.v2 = v2;
 			function1 = f1;
 			function2 = null;
 		}
 
 		public Ternary(final Field<?> field, final String cmp1, final Object v1, final String cmp2, final Function f2) {
 			this.field = field;
 			this.cmp1 = cmp1;
 			this.cmp2 = cmp2;
 			this.v1 = v1;
 			this.v2 = null;
 			function1 = null;
 			function2 = f2;
 		}
 
 		public Ternary(final Field<?> field, final String cmp1, final Function f1, final String cmp2, final Function f2) {
 			this.field = field;
 			this.cmp1 = cmp1;
 			this.cmp2 = cmp2;
 			this.v1 = null;
 			this.v2 = null;
 			function1 = f1;
 			function2 = f2;
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(' ');
 			sb.append(Util.derefField(field, context));
 			sb.append(cmp1);
 			if (function1 == null) {
 				sb.append("?");
 				bindings.add(v1);
 			} else {
 				sb.append(function1.getSQL(context));
 				bindings.addAll(function1.getSQLBindings());
 			}
 			sb.append(cmp2);
 			if (function2 == null) {
 				sb.append("?");
 				bindings.add(v2);
 			} else {
 				sb.append(function2.getSQL(context));
 				bindings.addAll(function2.getSQLBindings());
 			}
 		}
 
 		@SuppressWarnings("rawtypes")
 		@Override
 		boolean matches(final Table t) {
 			if (!cmp1.trim().equalsIgnoreCase("between")) {
 				throw new IllegalStateException("unknown comparision function '"+ cmp1
 						+"' for in-memory conditional check");
 			}
 			if (!cmp2.trim().equalsIgnoreCase("and")) {
 				throw new IllegalStateException("unknown comparision function '"+ cmp2
 						+"' for in-memory conditional check");
 			}
 			final Comparable o1 = (Comparable) v1;
 			final Comparable o2 = (Comparable) v2;
 			final Comparable v = (Comparable) t.get(field);
 			if (o1 != null && o1.compareTo(v) > 0) return false;
 			if (o2 != null && o2.compareTo(v) <= 0) return false;
 			return true;
 		}
 
 	}
 
 	static class Unary extends Condition {
 
 		private final Field<?> field;
 		private final String cmp;
 
 		public <T> Unary(final Field<T> field, final String cmp) {
 			this.field = field;
 			this.cmp = cmp;
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(' ');
 			sb.append(Util.derefField(field, context));
 			sb.append(cmp);
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			final Object v = t.get(field);
 			if (" is null".equals(this.cmp)) {
 				return v == null;
 			}
 			if (" is not null".equals(this.cmp)) {
 				return v != null;
 			}
 			throw new IllegalStateException("unknown comparision function '"+ cmp
 					+"' for in-memory conditional check");
 		}
 
 	}
 
 	static class Binary extends Condition {
 
 		private final Field<?> field;
 		private Object v;
 		private Field<?> field2;
 		private final String cmp;
 		private Select<?> s;
 		private Function function;
 
 		public <T> Binary(final Field<T> field, final String cmp, final Object v) {
 			// note "v" should be of type T here - set to object to work around
 			// this bug: http://stackoverflow.com/questions/5361513/reference-is-ambiguous-with-generics
 			this.field = field;
 			this.cmp = cmp;
 			this.v = v;
 		}
 
 		public <T> Binary(final Field<T> field, final String cmp, final Field<T> field2) {
 			this.field = field;
 			this.cmp = cmp;
 			this.field2 = field2;
 		}
 
 		public <T> Binary(final Field<T> field, final String cmp, final Query<?> q) {
 			this.field = field;
 			this.cmp = cmp;
			this.s = (Select<?>) q.all().iterator();
 		}
 
 		public <T> Binary(final Field<T> field, final String cmp, final Function f) {
 			this.field = field;
 			this.cmp = cmp;
 			this.function  = f;
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(' ');
 			if (v!=null) {
 				sb.append(Util.derefField(field, context));
 				sb.append(cmp);
 				sb.append("?");
 				bindings.add(v);
 			} else if (field2!=null) {
 				if (!field.isBound() && !field2.isBound() && field.sameField(field2)) {
 					try {
 						final Table table = field.TABLE.newInstance();
 						final String id = context.getFullTableName(table);
 						final Set<String> tableNames = context.tableNameMap.get(id);
 						if (tableNames.size() > 2) {
 							throw new RuntimeException("field ambigious");
 						} else if (tableNames.size() < 2) {
 							sb.append(Util.derefField(field, context));
 							sb.append(cmp);
 							sb.append(Util.derefField(field2, context));
 						} else {
 							final Iterator<String> i = tableNames.iterator();
 							sb.append(i.next() + "."+ field);
 							sb.append(cmp);
 							sb.append(i.next() + "."+ field2);
 						}
 					} catch (final InstantiationException e) {
 						e.printStackTrace();
 					} catch (final IllegalAccessException e) {
 						e.printStackTrace();
 					}
 				} else {
 					sb.append(Util.derefField(field, context));
 					sb.append(cmp);
 					sb.append(Util.derefField(field2, context));
 				}
 			} else if (s!=null) {
 				sb.append(Util.derefField(field, context));
 				sb.append(cmp);
 				sb.append('(');
 				final SqlContext innerContext = new SqlContext(s.getUnderlyingQuery(), context);
 				if (" in ".equals(cmp)) {
 					innerContext.maxFields = 1;
 				}
 				final Tuple2<String, List<Object>> ret = s.getSQL(innerContext);
 				sb.append(ret.a);
 				bindings.addAll(ret.b);
 				sb.append(')');
 			} else if (function!=null) {
 				sb.append(Util.derefField(field, context));
 				sb.append(cmp);
 				sb.append(function.getSQL(context));
 				bindings.addAll(function.getSQLBindings());
 			} else {
 				sb.append(Util.derefField(field, context));
 				sb.append(" is null");
 			}
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			if (v!=null) {
 				return v.equals(t.get(field));
 			} else if (field2!=null) {
 				final Object a = t.get(field);
 				final Object b = t.get(field2);
 				return a == b || (a != null && a.equals(b));
 			} else {
 				return t.get(field) == null;
 			}
 		}
 
 	}
 
 	static class Binary2 extends Condition {
 
 		private String cmp;
 		private Object o2;
 		private Object o1;
 
 		Binary2(Object o1, String cmp, Object o2) {
 			this.o1 = o1;
 			this.cmp = cmp;
 			this.o2 = o2;
 		}
 
 		@Override
 		boolean matches(Table t) {
 			if ((o1 instanceof Function) || (o2 instanceof Function)) {
 				throw new RuntimeException("Condition checking of functions cached queries not yet supported.");
 			}
 			// TODO Auto-generated method stub
 			return false;
 		}
 
 		@Override
 		protected void getSQL(StringBuffer sb, List<Object> bindings, SqlContext context) {
 			sb.append(' ');
 			if (o1 instanceof Function) {
 				Function<?> f = (Function<?>) o1;
 				sb.append(f.getSQL(context));
 				bindings.addAll(f.getSQLBindings());
 			} else if (o1 instanceof Field) {
 				Field<?> f = (Field<?>) o1;
 				sb.append(Util.derefField(f, context));
 			} else {
 				sb.append("?");
 				bindings.add(o1);
 			}
 			sb.append(cmp);
 			if (o2 instanceof Function) {
 				Function<?> f = (Function<?>) o2;
 				sb.append(f.getSQL(context));
 				bindings.addAll(f.getSQLBindings());
 			} else if (o2 instanceof Field) {
 				Field<?> f = (Field<?>) o2;
 				sb.append(Util.derefField(f, context));
 			} else {
 				sb.append("?");
 				bindings.add(o2);
 			}
 		}}
 
 	static class In extends Condition {
 
 		private final Field<?> field;
 		private final String cmp;
 		private final Object[] set;
 		private final Collection<?> set2;
 
 		public In(final Field<?> field, final String cmp, final Object... set) {
 			this.field = field;
 			this.cmp = cmp;
 			this.set = set;
 			this.set2 = null;
 		}
 
 		public In(final Field<?> field, final String cmp, final Collection<?> set) {
 			this.field = field;
 			this.cmp = cmp;
 			this.set = null;
 			this.set2 = set;
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(' ');
 			sb.append(Util.derefField(field, context));
 			sb.append(cmp);
 			sb.append('(');
 			if (set != null && set.length > 0) {
 				for (int i=0; i<set.length; ++i) {
 					final Object v = set[i];
 					sb.append("?");
 					if (i<set.length-1) sb.append(",");
 					bindings.add(v);
 				}
 			} else if (set2 != null && set2.size() > 0) {
 				int i = 0;
 				for (final Object v : set2) {
 					sb.append("?");
 					if (i<set2.size()-1) sb.append(",");
 					bindings.add(v);
 					++i;
 				}
 			} else {
 				sb.append("null");
 			}
 			sb.append(')');
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			boolean rev;
 			if (cmp.trim().equalsIgnoreCase("in")) rev = false;
 			else if (cmp.trim().equalsIgnoreCase("not in")) rev = true;
 			else throw new IllegalStateException("unknown comparision function '"+ cmp
 					+"' for in-memory conditional check");
 			if (set != null && set.length > 0) {
 				for (int i=0; i<set.length; ++i) {
 					if (eq(t.get(field), set[i])) return rev ? false : true;
 				}
 				return rev ? true : false;
 			} else if (set2 != null && set2.size() > 0) {
 				final boolean v = set2.contains(t.get(field));
 				return rev ? !v : v;
 			} else {
 				return false;
 			}
 		}
 	}
 
 	private static boolean eq(final Object a, final Object b) {
 		return a == b || (a != null && a.equals(b));
 	}
 
 	static class Exists extends Condition {
 
 		private final Query<? extends Table> q;
 		private final Select<?> s;
 
 		Exists(final Query<? extends Table> q) {
 			this.q = q;
 			this.s = (Select<?>) q.all().iterator();
 		}
 
 		@Override
 		public Condition not() {
 			return new Not(this, false);
 		}
 
 		@Override
 		boolean matches(final Table t) {
 			try {
 				return q.size() > 0;
 			} catch (final SQLException e) {
 				e.printStackTrace();
 				return false;
 			}
 		}
 
 		@Override
 		protected void getSQL(final StringBuffer sb, final List<Object> bindings, final SqlContext context) {
 			sb.append(" exists (");
 			final SqlContext innerContext = new SqlContext(s.getUnderlyingQuery(), context);
 			innerContext.dbType = context.dbType;
 			final Tuple2<String, List<Object>> ret = s.getSQL(innerContext);
 			sb.append(ret.a);
 			bindings.addAll(ret.b);
 			sb.append(")");
 		}
 
 	}
 
 	void _preExecute(final Connection conn) throws SQLException {
 		// default to do nothing
 	}
 
 	void _postExecute(final Connection conn) throws SQLException {
 		// default to do nothing
 	}
 
 }
