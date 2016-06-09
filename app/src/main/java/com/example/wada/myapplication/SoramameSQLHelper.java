package com.example.wada.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Wada on 2016/06/03.
 */
public class SoramameSQLHelper  extends SQLiteOpenHelper{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SoramameSQL.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SoramameContract.FeedEntry.TABLE_NAME + " (" +
                    SoramameContract.FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    SoramameContract.FeedEntry.COLUMN_NAME_STATION + TEXT_TYPE + COMMA_SEP +
                    SoramameContract.FeedEntry.COLUMN_NAME_CODE + " INTEGER" + COMMA_SEP +
                    SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS + TEXT_TYPE + COMMA_SEP +
                    SoramameContract.FeedEntry.COLUMN_NAME_PREFCODE + " INTEGER" + COMMA_SEP +
//                    SoramameContract.FeedEntry.COLUMN_NAME_LAT + " REAL" + COMMA_SEP +
//                    SoramameContract.FeedEntry.COLUMN_NAME_LNG + " REAL" + COMMA_SEP +
                    SoramameContract.FeedEntry.COLUMN_NAME_OX + " INTEGER" + COMMA_SEP +
                    SoramameContract.FeedEntry.COLUMN_NAME_PM25 + " INTEGER" + COMMA_SEP +
                    SoramameContract.FeedEntry.COLUMN_NAME_WD + " INTEGER" + COMMA_SEP +
                    SoramameContract.FeedEntry.COLUMN_NAME_SEL + " INTEGER" +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SoramameContract.FeedEntry.TABLE_NAME;

    public SoramameSQLHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
