package chalmers.verapp.database;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
/**
 * Data Access object. Like a layer on top of the Database
 * 
 * @author Anders
 *
 */
public class DAO{
	//Instance of the SQLite database
	private SQLiteDatabase database;
	//Object of the database handler class
	private DBLiteHelper dbHelper;
	/**
	 * Empty Constructor
	 * Gets an instance of the Helper
	 * @param context for example this
	 */
	public DAO(Context context){
		dbHelper = new DBLiteHelper(context);
	}	
	/**
	 * Needs to be called before any other operation on the database is performed.
	 * Get a writable database
	 */
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}	

	/**
	 * Call this when you are done modifying the database.
	 * Closes connection to database. Sameobject can be used again by calling open()
	 */
	public void close() {
		dbHelper.close();
	}


	public void insertIntoFileHistory(String fileName){
		ContentValues value = new ContentValues();
		value.put(DBLiteHelper.TABLE_COLUMN_1, fileName);
		
		database.insert(DBLiteHelper.TABLE_NAME, null, value);
	}

	public ArrayList<String> getAllFromTable(){
		String select = "SELECT * FROM " + DBLiteHelper.TABLE_NAME;
		Cursor c = database.rawQuery(select, null);
		ArrayList<String> result = new ArrayList<String>(c.getCount());
		if(c.getCount() == 0){
			return null;
		}else{
			while(c.moveToNext()){
				result.add(c.getString(0));
			}
			c.close();
			return result;
		}
	}
}