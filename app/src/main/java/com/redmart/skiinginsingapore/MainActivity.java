package com.redmart.skiinginsingapore;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.redmart.skiinginsingapore.util.AsyncHttpURLConnection;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private class Result {
        public int length;
        public final int droppingFrom;
        public final int droppingTo;
        public List<Integer> path;

        public Result(int length, int droppingFrom, int droppingTo, List<Integer> path) {
            this.length = length;
            this.droppingTo = droppingTo;
            this.droppingFrom = droppingFrom;
            this.path = path;
        }
    }

    private class Coords {
        public final int x;
        public final int y;

        public Coords(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private Button loadMapButton;
    private ProgressBar progressBarLoading;
    private TextView textStatus;
    private EditText uriToMap;
    private Integer[][] allMap;
    private AsyncHttpURLConnection httpConnection;
    private Result maxResult;
    private List<Coords> sortedCoords;
    private BackgroundExecution backgroundTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        checkSuitableness();
        initDefaults();
        initUI();
    }

    protected void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    private void checkSuitableness() {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        maxMemory = (maxMemory/1024)/1024;
        if(maxMemory < 128)
            showDialog(getResources().getString(R.string.error_title), getResources().getString(R.string.error), false);
    }

    private void initDefaults() {
        backgroundTask = null;
        allMap = null;
        httpConnection = null;
        maxResult = new Result(0,0,0,null);
        sortedCoords = null;
    }

    private void initUI() {

        loadMapButton = (Button) findViewById(R.id.buttonMapLoad);
        progressBarLoading = (ProgressBar) findViewById(R.id.progressBarLoading);
        textStatus = (TextView) findViewById(R.id.textViewStatus);
        uriToMap = (EditText) findViewById(R.id.editTextUriToMap);
        loadMapButton.setOnClickListener(LoadMap);

        findViewById(R.id.mainView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(v);
            }
        });
    }

    protected void unlockUI(final boolean unlocked) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadMapButton.setEnabled(unlocked);
                uriToMap.setEnabled(unlocked);
                progressBarLoading.setVisibility((unlocked) ? View.INVISIBLE :View.VISIBLE);
            }
        });
    }

    protected void statusMessage(final String message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText(message);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(httpConnection != null)
            httpConnection.close();
        if(backgroundTask != null && backgroundTask.getStatus() == AsyncTask.Status.RUNNING)
            backgroundTask.cancel(true);
    }

    protected void loadMap(String mapData) {

        try {
            mapData = mapData.replace("\r", "");
            String[] rows = mapData.split("\n");
            String[] size = rows[0].split(" ");
            allMap = new Integer[Integer.parseInt(size[0])][Integer.parseInt(size[1])];
            sortedCoords = new LinkedList<>();
            for(int x = 1; x < rows.length; x++)
            {
                String[] cols = rows[x].split(" ");
                statusMessage(getResources().getString(R.string.map_loading) + " " + x + "/" + (rows.length-1));
                for(int y = 0; y < cols.length; y++) {
                    allMap[x-1][y] = Integer.parseInt(cols[y]);
                    sortedCoords.add(new Coords(x-1, y));
                }
            }
        }catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
            throw e;
        }

    }

    protected void sortCoords() {
        Collections.sort(sortedCoords, new Comparator<Coords>() {
            @Override
            public int compare(Coords lhs, Coords rhs) {
                return allMap[rhs.x][rhs.y].compareTo(allMap[lhs.x][lhs.y]);
            }
        });
    }


    protected boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    protected Result max(Result r1, Result r2, int increment) {
        if((r1.length == r2.length + increment && (r1.droppingFrom - r1.droppingTo) < (r2.droppingFrom - r2.droppingTo)) || r1.length < r2.length + increment) {
            r2.length += increment;
            return r2;
        }
        else
            return r1;
    }

    protected Result findLongestPath(final int x, final int y, final int droppingFrom, List<Integer> path) {
        Result result = new Result(1, droppingFrom, allMap[x][y], path);
        result.path.add(allMap[x][y]);
        if(y < allMap[x].length-1 && allMap[x][y] > allMap[x][y+1]) result = max(result, findLongestPath(x, y+1, droppingFrom, result.path), 1);
        if(y > 0 && allMap[x][y] > allMap[x][y-1]) result = max(result, findLongestPath(x, y-1, droppingFrom, result.path), 1);
        if(x < allMap.length-1 && allMap[x][y] > allMap[x+1][y]) result = max(result, findLongestPath(x+1, y, droppingFrom, result.path), 1);
        if(x > 0 && allMap[x][y] > allMap[x-1][y]) result = max(result, findLongestPath(x-1, y, droppingFrom, result.path), 1);
        maxResult = max(maxResult,result, 0);
        return result;
    }

    protected void search() {
        Iterator<Coords> i_coords = sortedCoords.iterator();
        while(i_coords.hasNext()) {
            Coords coords = i_coords.next();
            Result r = findLongestPath(coords.x, coords.y, allMap[coords.x][coords.y], new LinkedList<Integer>());
            if(r.length >= r.droppingFrom || maxResult.length >= r.droppingFrom)
                break;
        }
    }

    protected void showResult() {
        int drop = (maxResult.droppingFrom - maxResult.droppingTo);
        String message = getResources().getString(R.string.success);
       message = String.format(message, drop, maxResult.droppingFrom, maxResult.droppingTo, maxResult.length);

        showDialog(getResources().getString(R.string.success_title), message, true);

    }

    protected void showDialog(final String title, final String message, final boolean reload) {

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(reload) {
                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                        finish();
                        System.exit(0);
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

    }

    private View.OnClickListener LoadMap = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(!isOnline())
            {
                Toast.makeText(getApplicationContext(), "No connection to Internet",
                        Toast.LENGTH_LONG).show();
                return;
            }

            initDefaults();
            unlockUI(false);
            httpConnection = new AsyncHttpURLConnection("GET", uriToMap.getText().toString(), "", HttpEvents);
            httpConnection.submit();
            statusMessage(getResources().getString(R.string.map_loading));
        }
    };

    private AsyncHttpURLConnection.AsyncHttpEvents HttpEvents = new AsyncHttpURLConnection.AsyncHttpEvents() {

        @Override
        public void onHttpError(final String errorMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), errorMessage,
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onHttpComplete(final String response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initDefaults();
                    backgroundTask = new BackgroundExecution();
                    backgroundTask.execute(response);
                }
            });
        }
    };

    private class BackgroundExecution extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {

            try {
                loadMap(params[0]);
                statusMessage(getResources().getString(R.string.sorting));
                sortCoords();
                statusMessage(getResources().getString(R.string.searching));
                search();
            }catch(Exception e)
            {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result)
                showResult();
            else
                statusMessage(getResources().getString(R.string.fail));
            sortedCoords.clear();
            unlockUI(true);
        }



        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... nothing ) {}
    }

}
