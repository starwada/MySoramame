package com.example.wada.myapplication;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ShareActionProvider;
import android.widget.Spinner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

// まず、表示する測定局の番号リストを保持させる。
// ファイルか？DB？
// アクティビティは表示する測定局のCardView
// 設定用の測定局一覧アクティビティ
public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ShareActionProvider mShareActionProvider;

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
    private Spinner mDataType;
    private Spinner mDay;
    int mCurrentType = 0;       // 表示データ種別スピナー
    int mCurrentDay = 3;        // 表示日数スピナー

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(myToolbar);

            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setIcon(R.drawable.ic_action_name);

        }
        catch(java.lang.NullPointerException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

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
        // ここで、データの更新をチェックする
//        GregorianCalendar now = new GregorianCalendar(Locale.JAPAN);

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

    @Override
    protected void onPause() {
        // 表示測定局をDBに保持
        updateDBIndex();
        // スピナーのインデックスを保持
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("CurrentType", mCurrentType);
        editor.putInt("CurrentDay", mCurrentDay);
        editor.apply();

        super.onPause();
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    // 表示データ種別および日数のスピナー設定
    private void SetSpinner(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mCurrentType = sharedPref.getInt("CurrentType", 0);
        mCurrentDay = sharedPref.getInt("CurrentDay", 3);

        ArrayList<String> dataList = new ArrayList<String>();
        dataList.add("PM2.5");
        dataList.add("OX(光化学オキシダント)");
        dataList.add("WS(風速)");
        ArrayAdapter<String> pref = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, dataList);
        pref.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // スピナーリスト設定
        mDataType = (Spinner)findViewById(R.id.spinner2);
        mDataType.setAdapter(pref);
        mDataType.setSelection(mCurrentType);
        mDataType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if( mAdapter != null) {
                    GraphViewAdapter adapter = (GraphViewAdapter) mAdapter;
                    adapter.SetMode(position);
                    mAdapter.notifyDataSetChanged();
                }
                mCurrentType = position;
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
        mDay = (Spinner)findViewById(R.id.spinnerDay);
        mDay.setAdapter(day);
        mDay.setSelection(mCurrentDay-1);
        mDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if( mAdapter != null) {
                    GraphViewAdapter adapter = (GraphViewAdapter) mAdapter;
                    adapter.SetDispDay(position);
                    mAdapter.notifyDataSetChanged();
                }
                mCurrentDay = (position+1 == 8 ? 0 : position+1);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // 測定局選択アクティビティ
    private void SelectStation(){
        Intent intent = new Intent(MainActivity.this, SelectStationActivity.class);
        startActivity(intent);
    }

    // 設定アクティビティ
    private void Setting(){
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

            // 本来ここでは、COLUMN_NAME_INDでのソートのみでOK
            Cursor c = mDb.query(SoramameContract.FeedEntry.TABLE_NAME, null,
                    SoramameContract.FeedEntry.COLUMN_NAME_SEL + " = 1",  null, null, null,
                    SoramameContract.FeedEntry.COLUMN_NAME_IND + " asc");
            if( c.getCount() > 0 )
            {
                if( c.moveToFirst() ) {
                    if(mList == null) {
                        mList = new ArrayList<Soramame>();
                    }
                    while (true) {
                        Soramame mame = new Soramame(
                                c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_CODE)),
                                c.getString(c.getColumnIndexOrThrow( SoramameContract.FeedEntry.COLUMN_NAME_STATION)),
                                c.getString(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS)));
                        mame.setSelected(1);
                        // 以下はデバッグでインデックスの値を見たいため
                        mame.setSelIndex(c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_IND)));

                        // mListを使いまわしするので、重複登録はしない。
                        boolean flag = true;
                        for( Soramame ent : mList) {
                            if( ent.getMstCode().equals(mame.getMstCode())){
                                flag = false;
                                break;
                            }
                        }
                        if(flag){ mList.add(mame); }
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

    // mListの選択順をDBに反映させる
    private int updateDBIndex(){
        int rc = 0;

        if( mList == null){ return rc; }

        SoramameSQLHelper mDbHelper = new SoramameSQLHelper(MainActivity.this);
        try {
            SQLiteDatabase mDb = mDbHelper.getWritableDatabase();
            if( !mDb.isOpen() ){ return -1; }

            int nIndex = 0;
            for( Soramame data : mList) {
                if(data.isSelected()) {
                    // 消すんじゃ無かった、フラグを未選択にするだけ。
                    String strWhereCause;
                    ContentValues values = new ContentValues();
                    strWhereCause = SoramameContract.FeedEntry.COLUMN_NAME_CODE + " = ?";
                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_IND, nIndex++);
                    String strWhereArg[] = {String.valueOf(data.getMstCode())};
                    mDb.update(SoramameContract.FeedEntry.TABLE_NAME, values, strWhereCause, strWhereArg);
                }
            }
            mDb.close();
        }catch (SQLiteException e){

        }

        return rc;
    }

    // これを呼ばれる前にmListが空になることを想定した作りとなっている。
    // 空でなければ、新規データのみ取得するように修正する。
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
                    // 現在時間と測定最新時間を比べるともっと早くなる。
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

                    // SoramameにisLoaded(西暦、月、日、時間)を実装する。
                    // 内部データが無い場合はfalse。
                    // 内部データの先頭要素にて判定する。入力より古いとtrue、同じか新しいとfalse。新しいは無いと思うが。
                    // 新規データをテンポラリ配列に保持しておき、判定でfalseになったら、元データを取り込み、入れ替える。
                    // Collections.copy()
                    count = 0;
                    Soramame aData = new Soramame();
                    for (Element ta : tables) {
                        Elements data = ta.getElementsByTag("td");
                        // 0 西暦/1 月/2 日/3 時間
                        // 4 SO2/5 NO/6 NO2/7 NOX/8 CO/9 OX/10 NMHC/11 CH4/12 THC/13 SPM/14 PM2.5/15 SP/16 WD/17 WS

                        if(soramame.isLoaded(data.get(0).text(), data.get(1).text(), data.get(2).text(), data.get(3).text())){ break; }
                        aData.setData(data.get(0).text(), data.get(1).text(), data.get(2).text(), data.get(3).text(),
                                data.get(9).text(), data.get(14).text(), data.get(16).text(), data.get(17).text());
                        count++;
                    }
                    // countにて新しいデータが無い場合はスルー
                    if(count > 0) {
                        if (soramame.getSize() > 0) {
                            aData.addAll(aData.getSize(), soramame.getData());
                            soramame.getData().clear();
                        }
                        soramame.addAll(0, aData.getData());
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
            // ここも飛ばせるが・・・
            // 以下ではだめ mList全てが最新ならスルー
            //if(count == 0){mProgressDialog.dismiss();return;}

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

                SetSpinner();

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
                                updateDBIndex();
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
            }

            mProgressDialog.dismiss();
        }
    }
}
