 package com.anibug.smsmanager.model;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import android.content.ContentValues;
 import android.content.Context;
 import android.database.Cursor;
 import android.database.sqlite.SQLiteDatabase;
 import android.util.Log;
 
 import com.anibug.smsmanager.database.SQLiteHelper;
 
 public abstract class Manager<T extends Model> {
 
 	private static SQLiteHelper sqliteHelper;
 	private static SQLiteDatabase sqliteDatabase;
 
 	protected static final String[] ALL = null;
 	protected static final String ID_DESC = "id DESC";
 
 	abstract public String getTableName();
 
 	abstract public String[] getTableDefinitionSQLs();
 
 	public Manager(Context context) {
 		if (sqliteHelper == null)
 			sqliteHelper = new SQLiteHelper(context);
 
 		// FIXME: We need a better way to added SQLs just once.
 		String[] sqls = getTableDefinitionSQLs();
 		for (String sql : sqls) {
 			if (sqliteHelper.addSQL(sql))
 				Log.d(getClass().getName(), "New definition added -- " + sql);
 		}
 	}
 
 	protected synchronized SQLiteDatabase getSqliteDatabase() {
 		if (sqliteDatabase == null)
 			sqliteDatabase = sqliteHelper.getWritableDatabase();
 		return sqliteDatabase;
 	}
 
 	public T get(long id) {
 		final String where = "id=?";
 		String[] whereArgs = new String[] { String.valueOf(id) };
 		Cursor cursor = getSqliteDatabase().query(getTableName(), ALL, where, whereArgs, null, null, null);
 		if (cursor.moveToFirst())
 			return createObject(cursor);
 		return null;
 	}
 
 	public List<T> fetchAll() {
 		Cursor cursor = getSqliteDatabase().query(getTableName(), ALL, null, null, null, null, ID_DESC);
 
 		return fetchList(cursor);
 	}
 
 
     public List<T> fetchAllBy(String column, Object value) {
         return fetchAllBy(column, value, 0);
     }
 
 	public List<T> fetchAllBy(String column, Object value, int limit) {
 		final String where = column + "=?";
 		String[] whereArgs = new String[] { String.valueOf(value) };
         String limitStr = null;
         if (limit > 0)
            limitStr = Integer.toString(limit);
 
		Cursor cursor = getSqliteDatabase().query(getTableName(), ALL, where, whereArgs, null, null, ID_DESC, limitStr);
 
 		return fetchList(cursor);
 	}
 
     public T fetchOneBy(String column, Object value) {
         final String where = column + "=?";
         String[] whereArgs = new String[] { String.valueOf(value) };
 
         Cursor cursor = getSqliteDatabase().query(getTableName(), ALL, where, whereArgs, null, null, ID_DESC, "1");
 
         List<T> result = fetchList(cursor);
         if (result.size() == 0)
             return null;
         return result.get(0);
     }
 
     protected List<T> fetchList(Cursor cursor) {
 		ArrayList<T> result = new ArrayList<T>();
 
 		if (cursor.moveToFirst()) {
 			do {
 				result.add(getObject(cursor));
 			} while (cursor.moveToNext());
 		}
 		if (!cursor.isClosed()) {
 			cursor.close();
 		}
 
 		return result;
 	}
 
 	public boolean save(T obj) {
 		if (obj.getId() >= 0) {
 			return update(obj);
 		}
 		return insert(obj);
 	}
 
 	protected boolean update(T obj) {
 		final String where = "id=?";
 		String[] whereArgs = new String[] { String.valueOf(obj.getId()) };
 
 		ContentValues record = createRecord(obj);
 		return getSqliteDatabase().update(getTableName(), record, where, whereArgs) == 1;
 	}
 
 	protected boolean insert(T obj) {
 		ContentValues record = createRecord(obj);
 		long id = getSqliteDatabase().insert(getTableName(), null, record);
 		if (id < 0)
 			return false;
 		obj.setId(id);
 		return true;
 	}
 
 	public boolean delete(T obj) {
 		final String where = "id=?";
 		String[] whereArgs = new String[] { String.valueOf(obj.getId()) };
 		return getSqliteDatabase().delete(getTableName(), where, whereArgs) > 0;
 	}
 
 	abstract public ContentValues createRecord(T obj);
 
 	public T getObject(Cursor cursor) {
 		T obj = createObject(cursor);
 		obj.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
 		return obj;
 	}
 
 	abstract public T createObject(Cursor cursor);
 
     public long getObjectId(Cursor cursor) {
         final int indexId = cursor.getColumnIndexOrThrow("id");
         return cursor.getLong(indexId);
     }
 
 }
