package com.example.wada.myapplication;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

// まず、表示する測定局の番号リストを保持させる。
// ファイルか？DB？
// アクティビティは表示する測定局のCardView
// 設定用の測定局一覧アクティビティ
public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private static final String SORAPREFFILE = "SoraPrefFile";

    private static  final  String SORABASEURL="http://soramame.taiki.go.jp/";
//    private static final String SORASUBURL ="MstItiran.php";
    private static final String SORADATAURL = "DataList.php?MstCode=";
    // 指定都道府県の測定局一覧取得
    private static final String SORAPREFURL ="MstItiranFrame.php?Pref=";

//    ProgressDialog mProgressDialog;
    String m_strMstURL;     // 測定局のURL
//    private Soramame mSoramame;
    ArrayList<Soramame> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setIcon(R.drawable.ic_action_name);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menu_set:
                Setting();
                break;
            case R.id.menu_selectstation:
                SelectStation();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        // DBから選択された測定局を取得し、そのデータを問い合わせる
        getSelectedStation();

        // 表示する測定局がたくさんあるとここで時間がかかる
        if( mList != null) {
            new SoraDesc().execute();
        }
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    // 測定局選択アクティビティ
    private void SelectStation(){
        // リスト用アクティビティ
        Intent intent = new Intent(MainActivity.this, SelectStationActivity.class);
        startActivity(intent);
    }

    // 設定アクティビティ
    private void Setting(){
        // リスト用アクティビティ
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    // 測定局の選択フラグをDBより取得する
    private int getSelectedStation(){
        int rc = 0;

        SoramameSQLHelper mDbHelper = new SoramameSQLHelper(MainActivity.this);
        try {
            SQLiteDatabase mDb = mDbHelper.getReadableDatabase();
            if( !mDb.isOpen() ){ return -1; }

            Cursor c = mDb.query(SoramameContract.FeedEntry.TABLE_NAME, null,
                    SoramameContract.FeedEntry.COLUMN_NAME_SEL + " = 1",  null, null, null, null);
            if( c.getCount() > 0 )
            {
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

                        mList.add(mame);
                        if( !c.moveToNext()){ break; }
                    }
                }
            }
            c.close();
            mDb.close();
        }catch (SQLiteException e){

        }

        return rc;
    }

    //
    private int updateDBAtStation(int code){
        int rc = 0;

        SoramameSQLHelper mDbHelper = new SoramameSQLHelper(MainActivity.this);
        try {
            SQLiteDatabase mDb = mDbHelper.getWritableDatabase();
            if( !mDb.isOpen() ){ return -1; }

            // 消すんじゃ無かった、フラグを未選択にするだけ。
            String strWhereCause;
            ContentValues values = new ContentValues();
            strWhereCause = SoramameContract.FeedEntry.COLUMN_NAME_CODE + " = ?";
            values.put(SoramameContract.FeedEntry.COLUMN_NAME_SEL, 0);
            String strWhereArg[] = { String.valueOf(code)};
            mDb.update(SoramameContract.FeedEntry.TABLE_NAME, values, strWhereCause, strWhereArg);
            mDb.close();
        }catch (SQLiteException e){

        }

        return rc;
    }

    //
    private class SoraDesc extends AsyncTask<Void, Void, Void>
    {
        ProgressDialog mProgressDialog;
        int count = 0;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setTitle( "そらまめ データ取得");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                if( mList.isEmpty()){ return null; }

                for( Soramame soramame : mList) {
                    // 本来、ここに測定局コードを指定する。
                    String url = String.format(Locale.ENGLISH, "%s%s%d", SORABASEURL, SORADATAURL, soramame.getMstCode());
                    Document doc = Jsoup.connect(url).get();
                    Elements elements = doc.getElementsByAttributeValue("name", "Hyou");
//                Integer size = elements.size();
                    for (Element element : elements) {
                        if (element.hasAttr("src")) {
                            url = element.attr("src");
                            m_strMstURL = SORABASEURL + url;
                            // ここでは、測定局のURL解決まで、URLを次のアクティビティに渡す。

                            break;
                        }
                    }
                    Document sora = Jsoup.connect(m_strMstURL).get();
                    Element body = sora.body();
                    Elements tables = body.getElementsByAttributeValue("align", "right");

                    for (Element ta : tables) {
                        Elements data = ta.getElementsByTag("td");
                        // 0 西暦/1 月/2 日/3 時間
                        // 4 SO2/5 NO/6 NO2/7 NOX/8 CO/9 OX/10 NMHC/11 CH4/12 THC/13 SPM/14 PM2.5/15 SP/16 WD/17 WS

                        soramame.setData(data.get(0).text(), data.get(1).text(), data.get(2).text(), data.get(3).text(),
                                data.get(9).text(), data.get(14).text(), data.get(16).text(), data.get(17).text());
                        count++;
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
            if(mList != null)
            {
                mRecyclerView = (RecyclerView) findViewById(R.id.graphview);

                mAdapter = mRecyclerView.getAdapter();
                if( mAdapter != null ){
                    mAdapter = null;
                }
                mAdapter = new GraphViewAdapter(MainActivity.this, mList);

                mLayoutManager = new LinearLayoutManager(MainActivity.this);
                mRecyclerView.setLayoutManager(mLayoutManager);
    //        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(10), true));
                mRecyclerView.setItemAnimator(new DefaultItemAnimator());
                mRecyclerView.setAdapter(mAdapter);

                // 以下タッチヘルパー
                // リサイクラービューにて要素を入れ替えたり、スワイプで削除したりできる。
                // mListやDBと同期させないと整合が取れないが。今のところ。
                ItemTouchHelper itemDecor = new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                                ItemTouchHelper.RIGHT) {
                            @Override
                            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                                final int fromPos = viewHolder.getAdapterPosition();
                                final int toPos = target.getAdapterPosition();
                                // アダプターでの順番を入れ替えているが元のmListも入れ替えないと。
                                // 以下の様なやりかたではだめはよう。たぶん簡単ではないので実装していないのだろう。
//                                mList.set(toPos, mList.get(fromPos));
                                // 以下はめんどくさいやりかただけど、仕方ないか。
                                // DBに表示用のインデックスを追加する。
                                if (fromPos < toPos) {
                                    for (int i = fromPos; i < toPos; i++) {
                                        Collections.swap(mList, i, i + 1);
                                    }
                                } else {
                                    for (int i = fromPos; i > toPos; i--) {
                                        Collections.swap(mList, i, i - 1);
                                    }
                                }
                                mAdapter.notifyItemMoved(fromPos, toPos);
                                return true;
                            }

                            @Override
                            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                                final int fromPos = viewHolder.getAdapterPosition();
                                // ここは、mListの内容も削除するが当然DBも扱わないと。
                                // mListから削除するまえにDBのフラグを未選択にする。
                                updateDBAtStation(mList.get(fromPos).getMstCode());
                                mList.remove(fromPos);
                                mAdapter.notifyItemRemoved(fromPos);
                            }
                        });
                itemDecor.attachToRecyclerView(mRecyclerView);

                ArrayList<String> dataList = new ArrayList<String>();
                dataList.add("PM2.5");
                dataList.add("OX(光化学オキシダント)");
                dataList.add("WS(風速)");
                ArrayAdapter<String> pref = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, dataList);
                pref.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // スピナーリスト設定
                Spinner datatype = (Spinner)findViewById(R.id.spinner2);
                datatype.setAdapter(pref);
                datatype.setSelection(0);
                datatype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        GraphViewAdapter adapter = (GraphViewAdapter)mAdapter;
                        adapter.SetMode(position);
                        mAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

                ArrayList<String> dayList = new ArrayList<String>();
                dayList.add("１日");
                dayList.add("２日");
                dayList.add("３日");
                dayList.add("４日");
                dayList.add("５日");
                dayList.add("６日");
                dayList.add("７日");
                dayList.add("最大");
                ArrayAdapter<String> day = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, dayList);
                day.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // スピナーリスト設定
                Spinner dayspinner = (Spinner)findViewById(R.id.spinnerDay);
                dayspinner.setAdapter(day);
                dayspinner.setSelection(2);
                dayspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        GraphViewAdapter adapter = (GraphViewAdapter)mAdapter;
                        adapter.SetDispDay(position);
                        mAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }

            mProgressDialog.dismiss();
        }
    }
}
