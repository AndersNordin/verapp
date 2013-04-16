package chalmers.verapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBLiteHelper extends SQLiteOpenHelper {
	
	private static final String TABLE_LOG_DATA = "Log_Data";
	private static final String TABLE_SIGNALS = "Signals";
	
	 private static final String CREATE_TABLE_LOG_DATA = 
			 "CREATE TABLE "+ TABLE_LOG_DATA +"(" +
			 "dataId integer primary key autoincrement," +
			 "signalId integer NOT NULL," +
			 "inputDate DATETIME," +
			 "x_coordinate FLOAT," +
			 "y_coordinate FLOAT," +
			 "warning BOOLEAN," +
			 "FOREIGN KEY(signalId) REFERENCES)";
	 
	 private static final String CREATE_TABLE_SIGNALS = 
			 "CREATE TABLE "+ TABLE_SIGNALS +"(" +
			 "signalId integer primary key autoincrement," +
			 "signalName TEXT," +
			 "signalUnit TEXT)";
	 

	public DBLiteHelper(Context context) {
		super(context, "verapp.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_LOG_DATA);		
		db.execSQL(CREATE_TABLE_SIGNALS);	
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    Log.w(DBLiteHelper.class.getName(),
	        "Upgrading database from version " + oldVersion + " to "
	            + newVersion + ", which will destroy all old data");
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG_DATA);
	    onCreate(db);
	  }

}
