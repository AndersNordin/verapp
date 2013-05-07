package chalmers.verapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBLiteHelper extends SQLiteOpenHelper {	
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "verapp";
    
    public static final String TABLE_NAME = "File_Sent";
    public static final String TABLE_COLUMN_1 = "fileName";

	private static final String CREATE_TABLE_FILE_SENT = 
			 "CREATE TABLE " + TABLE_NAME +" ("+ 
			  TABLE_COLUMN_1 + " TEXT primary key)";
	 

	public DBLiteHelper(Context context) {
		super(context, DATABASE_NAME + ".db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_FILE_SENT);			
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    Log.w(DBLiteHelper.class.getName(),
	        "Upgrading database from version " + oldVersion + " to "
	            + newVersion + ", which will destroy all old data");
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
	    onCreate(db);
	  }
	
	public static boolean databaseExists(){		
		SQLiteDatabase dbFile = null;
		try{
			dbFile = SQLiteDatabase.openDatabase(TABLE_NAME, null, SQLiteDatabase.OPEN_READONLY);
			dbFile.close();
		}catch (SQLiteException e){
			// DB does not exist!
		}
		if (dbFile != null){
			return true;
		}else{
			return false;
		}
	}
}