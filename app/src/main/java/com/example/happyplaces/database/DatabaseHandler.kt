package com.example.happyplaces.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.contentValuesOf
import com.example.happyplaces.models.HappyPlaceModel

class DatabaseHandler (context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "HappyPlacesDatabase"
        private val TABLE_HAPPY_PLACE = "HappyPlacesTable"
        private val COLUMN_ID = "_id"
        private val COLUMN_TITLE = "title"
        private val COLUMN_IMAGE = "image"
        private val COLUMN_DESCRIPTION = "description"
        private val COLUMN_DATE = "date"
        private val COLUMN_LOCATION = "location"
        private val COLUMN_LATITUDE = "latitude"
        private val COLUMN_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_EXERCISE_TABLE = ("CREATE TABLE " + TABLE_HAPPY_PLACE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY,"
                    + COLUMN_TITLE + " TEXT," + COLUMN_IMAGE + " TEXT," + COLUMN_DESCRIPTION + " TEXT,"
                    + COLUMN_DATE + " TEXT," + COLUMN_LOCATION + " TEXT," + COLUMN_LATITUDE + " TEXT," + COLUMN_LONGITUDE + " TEXT)")
        db?.execSQL(CREATE_EXERCISE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_HAPPY_PLACE")
        onCreate(db)
    }

    fun addHappyPlace(happyPlace: HappyPlaceModel): Long{

        val db = this.writableDatabase

        val contentValue = ContentValues()
        contentValue.put(COLUMN_TITLE, happyPlace.title)
        contentValue.put(COLUMN_IMAGE, happyPlace.image)
        contentValue.put(COLUMN_DESCRIPTION, happyPlace.description)
        contentValue.put(COLUMN_DATE, happyPlace.date)
        contentValue.put(COLUMN_LOCATION, happyPlace.location)
        contentValue.put(COLUMN_LATITUDE, happyPlace.latitude)
        contentValue.put(COLUMN_LONGITUDE, happyPlace.latitude)

        val result = db.insert(TABLE_HAPPY_PLACE, null, contentValue)
        db.close()

        return result
    }

    fun getHappyPlacesList(): ArrayList<HappyPlaceModel>{
        val happyPlacesList = ArrayList<HappyPlaceModel>()
        val selectQuery = "SELECT * FROM $TABLE_HAPPY_PLACE "
        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery,null)

            if (cursor.moveToFirst()){
                do {
                    val happyplace = HappyPlaceModel(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_LOCATION)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE))
                    )

                    happyPlacesList.add(happyplace)
                } while (cursor.moveToNext())
            }

            cursor.close()
        }
        catch(e:SQLException){
            db.execSQL(selectQuery)
            return ArrayList()
        }
        db.close()

        return happyPlacesList
    }

    fun updateHappyPlace(happyPlace: HappyPlaceModel): Int{

        val db = this.writableDatabase

        val contentValue = ContentValues()
        contentValue.put(COLUMN_TITLE, happyPlace.title)
        contentValue.put(COLUMN_IMAGE, happyPlace.image)
        contentValue.put(COLUMN_DESCRIPTION, happyPlace.description)
        contentValue.put(COLUMN_DATE, happyPlace.date)
        contentValue.put(COLUMN_LOCATION, happyPlace.location)
        contentValue.put(COLUMN_LATITUDE, happyPlace.latitude)
        contentValue.put(COLUMN_LONGITUDE, happyPlace.latitude)

        val result = db.update(TABLE_HAPPY_PLACE, contentValue, COLUMN_ID +" = "+ happyPlace.id, null)
        db.close()

        return result
    }

    fun deleteHappyPlace(happyPlace: HappyPlaceModel):Int{
        val db = this.writableDatabase
        val result =  db.delete(TABLE_HAPPY_PLACE, COLUMN_ID+" = "+ happyPlace.id,null)
        db.close()
        return result
    }
}