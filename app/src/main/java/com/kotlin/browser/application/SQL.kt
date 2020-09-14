package com.kotlin.browser.application

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import com.kotlin.browser.App
import com.kotlin.browser.Pin
import com.kotlin.browser.Record

object PinTable {
    val NAME = "Pin"
    val ID = "_id"
    val TITLE = "title"
    val URL = "url"
    val TIME = "time"
    val VISIT = "visit"
}


object RecordTable {
    val NAME = "Record"
    val TITLE = "title"
    val URL = "url"
    val TIME = "time"
    val VISIT = "visit"
}


class SQL(ctx: Context = App.instance) : ManagedSQLiteOpenHelper(ctx, SQL.DB_NAME, null, SQL.DB_VERSION) {

    companion object {
        const val DB_NAME = "ninja2.db"
        const val DB_VERSION = 1
        val instance: SQL by lazy { SQL() }
    }

    override fun onCreate(db: SQLiteDatabase?) {


        db?.createTable(PinTable.NAME, true,
                PinTable.ID to INTEGER + PRIMARY_KEY,
                PinTable.TITLE to TEXT,
                PinTable.URL to TEXT,
                PinTable.TIME to TEXT,
                PinTable.VISIT to INTEGER)


        db?.createTable(RecordTable.NAME, true,
                RecordTable.TITLE to TEXT,
                RecordTable.URL to TEXT + PRIMARY_KEY,
                RecordTable.TIME to TEXT,
                RecordTable.VISIT to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.dropTable(PinTable.NAME, true)
        db?.dropTable(RecordTable.NAME, true)
        onCreate(db)
    }
}

object SQLHelper {


    fun savePin(pin: Pin) {
        SQL.instance.use {
            insert(PinTable.NAME,
                    PinTable.TITLE to pin.title,
                    PinTable.URL to pin.url,
                    PinTable.TIME to pin.time,
                    PinTable.VISIT to pin.visit)
        }
    }


    fun savePin(title: String, url: String) {
        SQL.instance.use {
            insert(PinTable.NAME,
                    PinTable.TITLE to title,
                    PinTable.URL to url,
                    PinTable.TIME to System.currentTimeMillis().toString(),
                    PinTable.VISIT to 0)
        }
    }


    fun findAllPins(): List<Pin> {
        return SQL.instance.use {
            select(PinTable.NAME).parseList(classParser())
        }
    }


    fun updatePinById(pin: Pin) {
        SQL.instance.use {
            update(PinTable.NAME,
                    PinTable.TITLE to pin.title,
                    PinTable.URL to pin.url)
                    .whereArgs("_id = {id}", "id" to pin._id)
                    .exec()
        }
    }


    fun deletePin(pin: Pin) {
        SQL.instance.use {
            delete(PinTable.NAME, "_id = {id}", "id" to pin._id)
        }
    }



    fun saveOrUpdateRecord(title: String, url: String) {
        SQL.instance.use {

            val records = select(RecordTable.NAME)
                    .whereArgs("url = {url}", "url" to url)
                    .limit(1)
                    .parseList(classParser<Record>())
            records.forEach {
                //update record data
                update(RecordTable.NAME,
                        RecordTable.TITLE to title,
                        RecordTable.URL to url,
                        RecordTable.TIME to System.currentTimeMillis(),
                        RecordTable.VISIT to it.visit.inc())
                        .whereArgs("url = {url}", "url" to it.url)
                        .exec()

                return@use
            }

            insert(RecordTable.NAME,
                    RecordTable.TITLE to title,
                    RecordTable.URL to url,
                    RecordTable.TIME to System.currentTimeMillis(),
                    RecordTable.VISIT to 0)
        }
    }


    fun findAllRecords(): List<Record> {
        return SQL.instance.use {
            select(PinTable.NAME).parseList(classParser())
        }
    }


    fun searchRecord(key: String): List<Record> {
        return SQL.instance.use {
            select(RecordTable.NAME).whereArgs("title LIKE {key} OR url LIKE {key} ORDER BY visit DESC",
                    "key" to "%$key%")
                    .parseList(classParser())
        }
    }


    fun clearAllRecord() {
        SQL.instance.use {
            delete(RecordTable.NAME)
        }
    }


    fun clearOldRecord(time: Long = 1296000000) {
        SQL.instance.use {
            delete(RecordTable.NAME, "time < {time}",
                    "time" to (System.currentTimeMillis() - time))
        }
    }
}