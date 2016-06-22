package com.example.wada.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Wada on 2016/05/31
 * CardView,RecyclerView用に書き直し
 * 測定局選択画面
 * メイン画面の測定局毎グラフ用に表示する測定局を選択する画面
 * 前回指定した都道府県を保持しておく
 * 都道府県の測定局を取得する（どの時点で更新されるか分からないので）
 * メイン画面で表示する測定局（コード）はファイルに保持しておく
 */
public class SelectStationActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private static final String SORAPREFFILE = "SoraPrefFile";

    private static  final  String SORABASEURL="http://soramame.taiki.go.jp/";
    private static final String SORASUBURL ="MstItiran.php";
    private static final String SORADATAURL = "DataList.php?MstCode=";
    // 指定都道府県の測定局一覧取得
    private static final String SORAPREFURL ="MstItiranFrame.php?Pref=";

    ProgressDialog mProgressDialog;
    String m_strMstURL;     // 測定局のURL
    private Soramame mSoramame;
    ArrayList<Soramame> mList;
    int mPref ;                     // 都道府県コード

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectstation);

        try {
            Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(myToolbar);
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setIcon(R.drawable.ic_action_name);
            // Get a support ActionBar corresponding to this toolbar
            ActionBar ab = getSupportActionBar();
            // Enable the Up button
            ab.setDisplayHomeAsUpEnabled(true);

            mPref = 0;
            // 都道府県インデックスを取得
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            mPref = sharedPref.getInt("CurrentPref", 1);

            // SORASUBURLから都道府県名とコードを取得、スピナーに設定
            Spinner prefspinner = (Spinner) findViewById(R.id.spinner);
            if(prefspinner != null) {
                prefspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        mPref = position + 1;

                        // 選択都道府県での測定局データを取得するまえに、
                        // 現在の選択状態をDBに保存
                        setSelectedStation();

                        // 選択都道府県での測定局データ取得
                        new SoraStation().execute();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            // 都道府県取得
            new PrefSpinner().execute();

        }catch(NullPointerException e){
            e.printStackTrace();
        }

    }

    // 測定局の選択フラグをDBに設定する
    private int setSelectedStation(){
        int rc = 0;
        if( mList != null ){
            SoramameSQLHelper mDbHelper = new SoramameSQLHelper(SelectStationActivity.this);
            try {
                int nCount[] = {0,0};
                for(Soramame data : mList){
                    if(data.isEdit()){
                        if(!data.isSelected()){ nCount[0]++; }
                        else{ nCount[1]++; }
                    }
                }

                SQLiteDatabase mDb = mDbHelper.getWritableDatabase();
                String strWhereCause;
                ContentValues values = new ContentValues();
                for( int i=0; i<2; i++) {
                    strWhereCause = SoramameContract.FeedEntry.COLUMN_NAME_CODE + " IN(";
                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_SEL, i);
                    if(nCount[i] > 0) {
                        String strWhereArg[] = new String[nCount[i]];
                        int l=0;
                        for (Soramame data : mList) {
                            if (data.isEdit()) {
                                if ((i == 0 && !data.isSelected()) ||
                                        (i == 1 && data.isSelected())) {
                                    strWhereArg[l++] = String.valueOf(data.getMstCode());
                                    strWhereCause += "?";
                                    if(l < nCount[i]){ strWhereCause += ","; }
                                }
                            }
                        }
                        strWhereCause += ")";
                        mDb.update(SoramameContract.FeedEntry.TABLE_NAME, values, strWhereCause, strWhereArg);
                    }
                }
                mDb.close();
            }catch (SQLiteException e){
                e.printStackTrace();
            }
        }

        return rc;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_select_station, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mList != null){ mList.clear(); }
    }

    @Override
    public void onPause()
    {
        // 都道府県インデックスを保存する
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("CurrentPref", mPref);
        editor.apply();

        // 測定局の選択状態をDBに保存
        setSelectedStation();

        super.onPause();
    }

    // 都道府県
    // 内部ストレージにファイル保存する
    // 都道府県名なので固定でも問題ないが。
    private class PrefSpinner extends AsyncTask<Void, Void, Void>
    {
        ArrayList<String> prefList = new ArrayList<String>();

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                if( getPrefInfo() > 0) {
                    FileOutputStream outfile = openFileOutput(SORAPREFFILE, Context.MODE_PRIVATE);

                    String url = String.format("%s%s", SORABASEURL, SORASUBURL);
                    Document doc = Jsoup.connect(url).get();
                    Elements elements = doc.getElementsByTag("option");

                    String strPref;
                    for (Element element : elements) {
                        if (Integer.parseInt(element.attr("value")) != 0) {
                            strPref = element.text();
                            prefList.add(strPref);
                            // ファイルから取得時に分割できるようにセパレータを追加する
                            outfile.write((strPref + ",").getBytes());
                        }
                    }
                    outfile.close();
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            // simple_spinner_itemはAndroidの初期設定
            ArrayAdapter<String> pref = new ArrayAdapter<String>(SelectStationActivity.this, R.layout.prefspinner_item, prefList);
            pref.setDropDownViewResource(R.layout.prefspinner_drop_item);
            // スピナーリスト設定
            Spinner prefSpinner = (Spinner)findViewById(R.id.spinner);
            if(prefSpinner != null) {
                prefSpinner.setAdapter(pref);
                prefSpinner.setSelection(mPref - 1);
            }
        }

        private int getPrefInfo()
        {
            int rc = 0 ;
            try
            {
                FileInputStream infile = openFileInput(SORAPREFFILE);
                int byteCount = infile.available();
                byte[] readBytes = new byte[byteCount];
                rc = infile.read(readBytes, 0, byteCount) ;
                String strBytes = new String(readBytes);
                infile.close();
                rc = 0;

                prefList.clear();
                String Pref[] = strBytes.split(",");
                Collections.addAll(prefList, Pref);
            }
            catch (FileNotFoundException e)
            {
                // ファイルが無ければそらまめサイトにアクセス
                rc = 1;
            }
            catch(IOException e)
            {
                rc = -1;
            }

            return rc;
        }
    }

    // 都道府県の測定局データ取得
    private class SoraStation extends AsyncTask<Void, Void, Void>
    {
        String url;
        String strOX;           // OX
        String strPM25;     // PM2.5
        String strWD;       // 風向

        SoramameSQLHelper mDbHelper = new SoramameSQLHelper(SelectStationActivity.this);
        SQLiteDatabase mDb = null;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(SelectStationActivity.this);
            mProgressDialog.setTitle( "そらまめ（測定局取得）");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            // テストで、データをDBに保存する。都道府県単位か全国か。
            // 既存DBに保存されているかをチェック。
            try
            {
                // まず、DBをチェックする。
                mDb = mDbHelper.getReadableDatabase();
                if( !mDb.isOpen() ){ return null; }

                String[] selectionArgs = { String.valueOf(mPref)};
                Cursor c = mDb.query(SoramameContract.FeedEntry.TABLE_NAME, null,
                        SoramameContract.FeedEntry.COLUMN_NAME_PREFCODE + " = ?",  selectionArgs, null, null, null);
                if( c.getCount() > 0 )
                {
                    // DBにデータがあれば、DBから取得する。
                    if( c.moveToFirst() ) {
                        if(mList != null) {
                            mList.clear();
                        }
                        mList = new ArrayList<Soramame>();
                        while (true) {
                            Soramame mame = new Soramame(
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_CODE)),
                                    c.getString(c.getColumnIndexOrThrow( SoramameContract.FeedEntry.COLUMN_NAME_STATION)),
                                    c.getString(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS)));
                            mame.setAllow(
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_OX)),
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_PM25)),
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_WD))
                            );
                            mame.setSelected(c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_SEL)));
                            mList.add(mame);

                            if( !c.moveToNext()){ break; }
                        }
                    }
                    c.close();
                    mDb.close();
                    return null;
                }
                c.close();
                mDb.close();

                // DBに無ければ、検索してDBに登録する。
                mDb = mDbHelper.getWritableDatabase();

                url = String.format(Locale.ENGLISH, "%s%s%d", SORABASEURL, SORAPREFURL, mPref);
                Document doc = Jsoup.connect(url).get();
                Elements elements = doc.getElementsByAttributeValue("name", "Hyou");
                for( Element element : elements)
                {
                    if( element.hasAttr("src")) {
                        url = element.attr("src");
                        String soraurl = SORABASEURL + url;

                        Document sora = Jsoup.connect(soraurl).get();
                        Element body = sora.body();
                        Elements tables = body.getElementsByTag("tr");
                        url = "";
                        Integer cnt = 0;
                        if(mList != null) {
                            mList.clear();
                        }
                        mList = new ArrayList<Soramame>();

                        for( Element ta : tables) {
                            if( cnt++ > 0) {
                                Elements data = ta.getElementsByTag("td");
                                // 測定対象取得 OX(8)、PM2.5(13)、風向(15)
                                // 想定は○か✕
                                strOX = data.get(8).text();
                                strPM25 = data.get(13).text();
                                strWD = data.get(15).text();
                                // 最後のデータが空なので
                                if(strPM25.length() < 1){ break; }

                                int nCode = strPM25.codePointAt(0);
                                // PM2.5測定局のみ ○のコード(9675)
                                //if( nCode == 9675 ) {
                                Soramame ent = new Soramame(Integer.parseInt(data.get(0).text()), data.get(1).text(), data.get(2).text());
                                if(ent.setAllow(strOX, strPM25, strWD)){
                                    mList.add(ent);

                                    // 測定局DBに保存
                                    ContentValues values = new ContentValues();
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_IND, cnt);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_STATION, data.get(1).text());
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_CODE, Integer.valueOf(data.get(0).text()));
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS, data.get(2).text());
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_PREFCODE, mPref);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_OX, ent.getAllow(0) ? 1: 0);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_PM25, ent.getAllow(1) ? 1 : 0);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_WD, ent.getAllow(2) ? 1 : 0);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_SEL, 0);
                                    // 重複は追加しない
                                    long newRowId = mDb.insertWithOnConflict(SoramameContract.FeedEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                                }
                                //}
                            }
                        }
                    }
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if( mDb.isOpen()){ mDb.close(); }
            // 測定局データ取得後にリスト表示
            mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            if(mList != null && mRecyclerView != null)
            {
                mAdapter = mRecyclerView.getAdapter();
                if( mAdapter != null ){
                    mAdapter = null;
                }
                mAdapter = new SoramameStationAdapter(SelectStationActivity.this, mList);

                mLayoutManager = new LinearLayoutManager(SelectStationActivity.this);
                mRecyclerView.setLayoutManager(mLayoutManager);
//        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(10), true));
                mRecyclerView.setItemAnimator(new DefaultItemAnimator());
                mRecyclerView.setAdapter(mAdapter);
            }
            mProgressDialog.dismiss();
        }
    }
}
