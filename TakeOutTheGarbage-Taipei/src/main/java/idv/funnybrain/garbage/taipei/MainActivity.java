package idv.funnybrain.garbage.taipei;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private static final boolean D = true;
    private static final String TAG = "MainActivity";

    private boolean isExternalStorageAvailable = false;

    private final String FILENAME_JSON_GARBAGE_TAIPEI = "garbage_taipei.json";

    private List<GarbageTaipeiJson> allDataList;

    private Handler handler;
    private final int MSG_GARBAGE_TAIPEI_DOWNLOAD_OK = 111;
    private final int MSG_GARBAGE_TAIPEI_PARSER_OK = 222;
    private final int MSG_GARBAGE_TAIPEI_SAVE_TO_DB_OK = 333;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Intent intent = new Intent();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_GARBAGE_TAIPEI_DOWNLOAD_OK:
                        new ParseJsonTask().execute("");
                        break;
                    case MSG_GARBAGE_TAIPEI_PARSER_OK:
                        new SaveDataTask().execute("");
                        break;
                    case MSG_GARBAGE_TAIPEI_SAVE_TO_DB_OK:
                        //System.out.println("ALL DONE!");

                        (new Utils()).saveDataToSharedPreference(MainActivity.this, "isDataOK", 1); // 0: false, 1: true

                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, Map.class);
                        startActivity(intent);
                        break;
                }

            }
        };

        int isDataOK = (new Utils()).getDataFromSharedPreference(this, "isDataOK");
        if(D) { Log.d(TAG, "isDataOK1 : " + isDataOK); }
        if(isDataOK == Integer.MIN_VALUE) { // IInteger.MIN_VALUE: false, 1: true
            if(D) { Log.d(TAG, "Start to download everything"); }
            new DownloadDataTask().execute(Utils.GARBAGE_TAIPEI_URL);
        } else {
            if(D) { Log.d(TAG, "Everything is ready. Skip."); }
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, Map.class);
            startActivity(intent);
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private class DownloadDataTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            InputStream is = null;
            OutputStream os = null;
            HttpURLConnection conn = null;

            try {
                URL url = new URL(params[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    //conn.getResponseMessage()
                    return null;
                }

                int fileLength = conn.getContentLength();
                is = conn.getInputStream();
                os = openFileOutput(FILENAME_JSON_GARBAGE_TAIPEI, Context.MODE_PRIVATE);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while( (count = is.read(data)) != -1 ) {
//                    if(isCancelled()) { // allow canceling with back button
//                        is.close();
//                        return null
//                    }
                    total += count;
                    if(fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    os.write(data, 0, count);
                }
            } catch(Exception e) {
                e.printStackTrace();
                return e.toString();
            } finally {
                try {
                    if(os != null) {
                        os.flush();
                        os.close();
                    }
                    if(is != null) {
                        is.close();
                    }
                } catch(IOException ioe) {
                }
                if(conn != null) {
                    conn.disconnect();
                }
            }
            return null; // all success, return nothing(null)
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            File tmp = new File(getFilesDir(), FILENAME_JSON_GARBAGE_TAIPEI);
            if(tmp.exists()) {
                //System.out.println("Download OK!");
                Message message = new Message();
                message.what = MSG_GARBAGE_TAIPEI_DOWNLOAD_OK;
                handler.sendMessage(message);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //System.out.println("Downlaod Progress: " + values[0]);
            super.onProgressUpdate(values);
        }
    }

    private class ParseJsonTask extends AsyncTask<String, Void, List<GarbageTaipeiJson>> {
        @Override
        protected List<GarbageTaipeiJson> doInBackground(String... params) {
            JsonFactory jsonFactory = new JsonFactory();
            File jsonFile = new File(getFilesDir(), FILENAME_JSON_GARBAGE_TAIPEI);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonParser jsonParser;
            List<GarbageTaipeiJson> results = null;
            try {
                jsonParser = jsonFactory.createJsonParser(jsonFile);
                GarbageTaipeiJson[] _results = objectMapper.readValue(jsonParser, GarbageTaipeiJson[].class);
                results = Arrays.asList(_results);
                //allDataList = Arrays.asList(_results);
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(false) {
                for(GarbageTaipeiJson gtj : results) {
                    System.out.println(gtj.title);
                    System.out.println(gtj.dep_content);
                    System.out.println("====");
                }
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<GarbageTaipeiJson> list) {
            super.onPostExecute(list);
            allDataList = list;

            Message message = new Message();
            message.what = MSG_GARBAGE_TAIPEI_PARSER_OK;
            handler.sendMessage(message);
        }
    }

    private class SaveDataTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            DBHelperTaipei dbHelperTaipei = new DBHelperTaipei(MainActivity.this);

            String[] district = getResources().getStringArray(R.array.distict_taipei);
            for(int x=0; x<allDataList.size(); x++) {//GarbageTaipeiJson gtj : allDataList) {
                GarbageTaipeiJson gtj = allDataList.get(x);
                ContentValues values = new ContentValues();
                values.put(DBHelperTaipei.DB_COL_UNIT, gtj.unit);

                String title = gtj.title;
                String address = title.replace("垃圾清運點：", "").trim();
                values.put(DBHelperTaipei.DB_COL_ADDRESS, address);
                for(String s : district) {
                    if (title.contains(s)) {
                        values.put(DBHelperTaipei.DB_COL_DISTRICT, s);
                    }
                }

                String dep_content = gtj.dep_content;
                String[] dep_array = dep_content.split("，");
                String car_num = dep_array[0].replace("車號：", "").trim();
                //String car_ser = dep_array[1].replace("車次：第", "").replace("車", "").trim();
                String car_ser = dep_array[1].replace("車次：", "").trim();
                String _time = dep_array[2].replace("時間：", "").trim();
                _time = _time.replace("～", "~").replace("-", "~").replace("：", ":");
                String start_time = _time.subSequence(0,5).toString();
                String end_time = "";
                if(_time.length()<11) {
                    end_time = _time.subSequence(6,_time.length()).toString();
                } else {
                    end_time = _time.subSequence(6, 11).toString();
                }
                if(end_time.length() == 4) {
                    end_time = end_time.subSequence(0,2).toString() + ":" + end_time.subSequence(2,4);
                }
                if(_time.length()>11) {
                    values.put(DBHelperTaipei.DB_COL_NOTICE, _time.subSequence(11, _time.length()).toString());
                }

                values.put(DBHelperTaipei.DB_COL_CAR_NUMBER, car_num);
                values.put(DBHelperTaipei.DB_COL_CAR_SERIAL, car_ser);
                values.put(DBHelperTaipei.DB_COL_START_TIME, start_time);
                values.put(DBHelperTaipei.DB_COL_END_TIME, end_time);

                values.put(DBHelperTaipei.DB_COL_LAT, gtj.lat);
                values.put(DBHelperTaipei.DB_COL_LNG, gtj.lng);
                values.put(DBHelperTaipei.DB_COL_MODIFY_DATE, gtj.modifydate);

                if(false) {
                    System.out.println(address + ", " +
                                       car_num + ", " +
                                       car_ser + ", " +
                                       start_time + ", " +
                                       end_time + ", " +
                                       gtj.lat + ", " +
                                       gtj.lng + ", " +
                                       gtj.modifydate);
                    System.out.println("===");
                }
                dbHelperTaipei.insert(values);
                publishProgress((int) x * 100 / allDataList.size());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Message message = new Message();
            message.what = MSG_GARBAGE_TAIPEI_SAVE_TO_DB_OK;
            handler.sendMessage(message);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //System.out.println("Save Database Progress: " + values[0]);
            super.onProgressUpdate(values);
        }
    }
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
}
