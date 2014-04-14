/*
{
    "unit":"台北市政府環境保護局",
    "title":"垃圾清運點：臺北市士林區基河路100號對面",
    "dep_content":"車號：101-037，車次：第1車，時間：17:15~17:20",
    "lng":"121.52385234832764",
    "lat":"25.088096100187595",
    "modifydate":"2013-04-01T18:43:39.63+08:00"
}
 */
package idv.funnybrain.garbage.taipei;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Freeman on 2014/3/19.
 */
public class DBHelperTaipei extends SQLiteOpenHelper {
    private static final boolean D = true;
    private static final String TAG = "DBHelperTaipei";

    private static final String DB_NAME = "garbage_taipei.db";
    private static final int DB_VERSION = 1;
    private static final String DB_TABLE = "garbage_taipei";
    private static final String DB_COL_ID = "_id";
    public static final String DB_COL_UNIT = "unit";

    //private final String DB_COL_TITLE = "title";
    public static final String DB_COL_ADDRESS = "address";
    public static final String DB_COL_DISTRICT = "district";

    //private final String DB_COL_DEP_CONTENT = "dep_content";
    public static final String DB_COL_CAR_NUMBER = "carNumber";
    public static final String DB_COL_CAR_SERIAL = "carSerial";
    public static final String DB_COL_START_TIME = "startTime";
    public static final String DB_COL_END_TIME = "endTime";

    public static final String DB_COL_LAT = "lat";
    public static final String DB_COL_LNG = "lng";
    public static final String DB_COL_MODIFY_DATE = "modifydate";

    public static final String DB_COL_NOTICE = "my_notice";

    private final String DB_CREATE = "CREATE TABLE " + DB_TABLE + " (" +
                                     DB_COL_ID + " INTEGER PRIMARY KEY, " +
                                     DB_COL_UNIT + " TEXT, " +
                                     DB_COL_ADDRESS + " TEXT, " +
                                     DB_COL_DISTRICT + " TEXT, " +
                                     DB_COL_CAR_NUMBER + " TEXT, " +
                                     DB_COL_CAR_SERIAL + " TEXT, " +
                                     DB_COL_START_TIME + " TEXT, " +
                                     DB_COL_END_TIME + " TEXT, " +
                                     DB_COL_LAT + " TEXT, " +
                                     DB_COL_LNG + " TEXT, " +
                                     DB_COL_MODIFY_DATE + " TEXT, " +
                                     DB_COL_NOTICE + " TEXT)";

    private SQLiteDatabase db;

    //private Context context;


    public DBHelperTaipei(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        db.execSQL(DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
        onCreate(db);
    }

    public long insert(ContentValues values) {
        db = getWritableDatabase();
        long result = 0L;
        try {
            result = db.insert(DB_TABLE, null, values);
        } catch(SQLiteConstraintException e) {
            return result;
        }
        db.close();
        return result;
    }


    public List<GarbageObject> queryGarbageByDist(String dist) {
        List<GarbageObject> result = new ArrayList<GarbageObject>();

        db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE, new String[]{DB_COL_ADDRESS, DB_COL_START_TIME, DB_COL_END_TIME, DB_COL_LAT, DB_COL_LNG, DB_COL_NOTICE},
                                 DB_COL_DISTRICT + "=?", new String[]{dist}, null, null, null);
        if(D) { Log.d(TAG, cursor.toString()); }
        int idx_address = cursor.getColumnIndexOrThrow(DB_COL_ADDRESS);
        int idx_start = cursor.getColumnIndexOrThrow(DB_COL_START_TIME);
        int idx_end = cursor.getColumnIndexOrThrow(DB_COL_END_TIME);
        int idx_lat = cursor.getColumnIndexOrThrow(DB_COL_LAT);
        int idx_lng = cursor.getColumnIndexOrThrow(DB_COL_LNG);
        int idx_not = cursor.getColumnIndexOrThrow(DB_COL_NOTICE);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                GarbageObject obj = new GarbageObject(
                        cursor.getString(idx_address),
                        dist,
                        cursor.getString(idx_start),
                        cursor.getString(idx_end),
                        cursor.getString(idx_lat),
                        cursor.getString(idx_lng),
                        cursor.getString(idx_not)
                );
                result.add(obj);
            } while(cursor.moveToNext());
        }
        return result;
    }

    public List<GarbageObject> queryGarbageByDistance(Double lat, Double lng, int dist) {
        LatLng center = new LatLng(lat, lng);
        List<GarbageObject> result = new ArrayList<GarbageObject>();
        db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE, new String[]{DB_COL_ADDRESS, DB_COL_DISTRICT, DB_COL_START_TIME, DB_COL_END_TIME, DB_COL_LAT, DB_COL_LNG, DB_COL_NOTICE},
                                 null, null, null, null, null);
        int idx_address = cursor.getColumnIndexOrThrow(DB_COL_ADDRESS);
        int idx_district = cursor.getColumnIndexOrThrow(DB_COL_DISTRICT);
        int idx_start_time = cursor.getColumnIndexOrThrow(DB_COL_START_TIME);
        int idx_end_time = cursor.getColumnIndexOrThrow(DB_COL_END_TIME);
        int idx_lat = cursor.getColumnIndexOrThrow(DB_COL_LAT);
        int idx_lng = cursor.getColumnIndexOrThrow(DB_COL_LNG);
        int idx_notice = cursor.getColumnIndexOrThrow(DB_COL_NOTICE);
        if(cursor.moveToFirst()) {
            do {
                //if(D) { Log.d(TAG, "queryGarbageByDistance" + cursor.getString(idx_address)); }
                LatLng tmp = new LatLng(Double.valueOf(cursor.getString(idx_lat)),
                                      Double.valueOf(cursor.getString(idx_lng)));
                double calculate = SphericalUtil.computeDistanceBetween(center, tmp);
                if(D) { Log.d(TAG, "calculate: " + calculate + ", dist: " + dist); }
                if(dist >= calculate) {
                    result.add(new GarbageObject(cursor.getString(idx_address), cursor.getString(idx_district),
                                                 cursor.getString(idx_start_time), cursor.getString(idx_end_time),
                                                 cursor.getString(idx_lat), cursor.getString(idx_lng),
                                                 cursor.getString(idx_notice)));
                }
            } while(cursor.moveToNext());
        }
        if(D) { Log.d(TAG, "queryGarbageByDistance result size: " + result.size()); }
        return result;
    }

//    public Cursor queryGarbageByDist_cursorLoader(String dist) {
//        db = getReadableDatabase();
//        Cursor cursor = db.query(DB_TABLE, new String[]{DB_COL_ADDRESS, DB_COL_START_TIME, DB_COL_END_TIME, DB_COL_LAT, DB_COL_LNG, DB_COL_NOTICE},
//                DB_COL_DISTRICT + "=?", new String[]{dist}, null, null, null);
//        return cursor;
//    }
}
