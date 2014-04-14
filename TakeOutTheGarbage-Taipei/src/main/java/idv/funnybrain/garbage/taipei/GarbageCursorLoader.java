package idv.funnybrain.garbage.taipei;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Created by Freeman on 2014/3/20.
 */
public class GarbageCursorLoader extends AsyncTaskLoader<List<GarbageObject>> {
    private static final boolean D = true;
    private static final String TAG = "GarbageCursorLoader";

    private final Context mContext;
    private final String mDist;
    private List<GarbageObject> allData;

    public GarbageCursorLoader(Context context) {
        super(context);
        mContext = context;
        mDist = (new Utils()).getSelectedDist();
        //mDist = "萬華區";
        if(D) { Log.d(TAG, "constructor: " + mDist); }
    }

    @Override
    public List<GarbageObject> loadInBackground() {
        if(D) { Log.d(TAG, "loadInBackground"); }
        DBHelperTaipei dbHelper = new DBHelperTaipei(mContext);
        allData = dbHelper.queryGarbageByDist(mDist);

        return allData;
    }

    @Override
    public void deliverResult(List<GarbageObject> data) {
        if(D) { Log.d(TAG, "deliverResult"); }
        if(isReset()) {
            if(allData != null) {
                onReleaseResources(allData);
            }
        }
        List<GarbageObject> oldData = allData;
        allData = data;

        if(isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(data);
        }

        if(oldData != null) {
            onReleaseResources(oldData);
        }
    }

    @Override
    protected void onStopLoading() {
        //super.onStopLoading();
        if(D) { Log.d(TAG, "onStopLoading"); }
        cancelLoad();
    }

    @Override
    public void onCanceled(List<GarbageObject> data) {
        super.onCanceled(data);
        if(D) { Log.d(TAG, "onCanceled"); }
        onReleaseResources(data);
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if(allData != null) {
            onReleaseResources(allData);
            allData = null;
        }
        if(D) { Log.d(TAG, "onReset"); }
    }

    protected void onReleaseResources(List<GarbageObject> data) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
        if(D) { Log.d(TAG, "onReleaseResources"); }
    }
}
