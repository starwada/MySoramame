package com.example.wada.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * そらまめデータ
 * Intentで渡せるようにParcelableを使用
 * Created by Wada on 2015/07/03.
 */
public class Soramame implements Parcelable{

    // そらまめの測定局データ
    public  class SoramameStation {
        private int m_nCode;                // 測定局コード
        private String m_strName;       // 測定局名称
        private String m_strAddress;    // 住所
        private boolean m_bAllow[] = {false, false, false};     // 取得データフラグ（OX、PM2.5、風向）

        public SoramameStation(int nCode, String strName, String strAddress) {
            setCode(nCode);
            setName(strName);
            setAddress(strAddress);
        }

        // Set
        public void setCode(int nCode) {
            m_nCode = nCode;
        }

        public void setName(String strName) {
            m_strName = strName;
        }

        public void setAddress(String strAddress) {
            m_strAddress = strAddress;
        }

        public void setAllow(boolean bAllow[]) {
            m_bAllow = bAllow;
        }

        // Get
        public int getCode() {
            return m_nCode;
        }

        public String getName() {
            return m_strName;
        }

        public String getAddress() {
            return m_strAddress;
//            return String.format("%s:OX(%s)PM2.5(%s)WD(%s)", m_strAddress, (m_bAllow[0] ? "○" : "×"), (m_bAllow[1] ? "○" : "×"), (m_bAllow[2] ? "○" : "×"));
        }

        public String getString() {
//            return String.format("%d %s:%s", m_nCode, m_strName, m_strAddress);
            return String.format("%s:%s", m_strName, m_strAddress);
        }

        public boolean[] getAllow() {
            return m_bAllow;
        }

        public boolean getAllowOX() {
            return m_bAllow[0];
        }
        public boolean getAllowPM25(){
            return  m_bAllow[1];
        }
        public boolean getAllowWS(){
            return m_bAllow[2];
        }

        public boolean isAllow() {
            for (int i = 0; i < 3; i++) {
                if (m_bAllow[i]) {
                    return true;
                }
            }
            return false;
        }
    }

    // そらまめの測定データクラス
    public class SoramameData {
        private GregorianCalendar m_dDate;       // 測定日時 UTCのみのようだ
        // SO2 ppm 二酸化硫黄
        // NO ppm 一酸化窒素
        // NO2 ppm 二酸化窒素
        // NOX ppm 窒素酸化物
        // CO ppm 一酸化炭素
        private float m_fOX;// OX ppm 光化学オキシダント
        // SPM mg/m3 浮遊粒子状物質
        private Integer m_nPM25;    // μg/m3 微小粒子状物質 PM2.5測定値 未計測は-100を設定
        private Integer m_nWD;  // WD 16方位(静穏) 風向 静穏は0、北を1として、時計回りに
        private float m_fWS;      // WS m/s 風速

        SoramameData(String strYear, String strMonth, String strDay, String strHour, String strOX, String strPM25, String strWD, String strWS)
        {
            // 月は０から11で表現する。取得時も。
            m_dDate = new GregorianCalendar(Integer.valueOf(strYear), Integer.valueOf(strMonth)-1,
                    Integer.valueOf(strDay), Integer.valueOf(strHour), 0);
            // 未計測の場合、"-"が出力される。他のパターンもあった。
//            if( strValue.codePointAt(0) == 12288 || strValue.equalsIgnoreCase("-") ){ m_nPM25 = -100 ; }
//            else{ m_nPM25 = Integer.valueOf(strValue); }
            m_fOX = -0.1f;
            m_nPM25 = -100;
            m_nWD = -1;
            m_fWS = 0.0f;
            try{
                // データを取得できる確率が高い順
                m_nWD = parseWD(strWD);
                m_nPM25 = getValue(strPM25, -100);
                m_fOX = getValue(strOX, -0.1f);
                m_fWS = getValue(strWS, -0.1f);
            }
            // getValue()内にて例外キャッチしているので以下は不要。
            catch(NumberFormatException e){
                e.printStackTrace();
            }
        }

        SoramameData(GregorianCalendar date, float fOX, Integer nPM25, Integer nWD, float fWS){
            //m_dDate = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.HOUR_OF_DAY), 0, 0);
            m_dDate = date ;
            m_fOX = fOX;
            m_nPM25 = nPM25;
            m_nWD = nWD;
            m_fWS = fWS;
        }

        public GregorianCalendar getDate()
        {
            return m_dDate;
        }
        public String getCalendarString(){
            return String.format("%s/%s/%s %s時",
                    m_dDate.get(Calendar.YEAR), m_dDate.get(Calendar.MONTH)+1, m_dDate.get(Calendar.DAY_OF_MONTH), m_dDate.get(Calendar.HOUR_OF_DAY));
        }
        public String getDateString(){
            return String.format("%s/%s/%s",
                    m_dDate.get(Calendar.YEAR), m_dDate.get(Calendar.MONTH)+1, m_dDate.get(Calendar.DAY_OF_MONTH));
        }
        public String getHourString(){
            return String.format("%d時", m_dDate.get(Calendar.HOUR_OF_DAY));
        }
        public float getOX(){ return m_fOX; }
        public String getOXString(){
            return (m_fOX < 0.0f ? "未計測" : String.format("%.2f", m_fOX ));
        }
        public  Integer getPM25()
        {
            return (m_nPM25 < 0 ? 0 : m_nPM25);
        }
        public String getPM25String(){ return String.format("%s",(m_nPM25 < 0 ? "未計測" : m_nPM25.toString()));}
        public Integer getWD(){ return m_nWD; }
        public float getWDRotation(){
            return (22.5f*(m_nWD-1));
        }
        public float getWS(){ return m_fWS; }
        public String getWSString(){
            return ( m_fWS < 0.0f ? "未計測" : String.format("%.1f", m_fWS));
        }

        public void setPM25(Integer pm25)
        {
            m_nPM25 = pm25;
        }

        public String Format()
        {
            return String.format("%s:%s", getDateString(), getPM25String()) ;
        }
    }

    // メンバーデータ
    private SoramameStation m_Station;                  // 測定局データ
    private ArrayList< SoramameData > m_aData;  // 測定データ
    private boolean m_Select;                                   // 選択フラグ true 表示対象/false 非表示
    private boolean mEdit;                                          // 編集フラグ true 編集した/false 未編集

    private int mSelIndex;

    @Override
    public int describeContents(){
        return 0;
    }

    // Parcel(小包)用、IntentにてMainActivityからDisplayMessageActivityへデータを渡すため
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(m_Station.getCode());
        out.writeString(m_Station.getName());
        out.writeString(m_Station.getAddress());
        out.writeBooleanArray(m_Station.getAllow());
    }

    public static final Parcelable.Creator<Soramame> CREATOR
            = new Parcelable.Creator<Soramame>() {
        public Soramame createFromParcel(Parcel in) {
            return new Soramame(in);
        }

        public Soramame[] newArray(int size) {
            return new Soramame[size];
        }
    };

    public Soramame(){
        super();
    }

    private Soramame(Parcel in) {
        m_Station = new SoramameStation(in.readInt(), in.readString(), in.readString());
        boolean bFlag[] = new boolean[3];
        in.readBooleanArray(bFlag);
        m_Station.setAllow(bFlag);
        m_aData  = null;
        m_Select = false;
        mEdit = false;
        mSelIndex = 0;
    }

    Soramame(int nCode, String strName, String strAddress)
    {
        m_Station = new SoramameStation(nCode, strName, strAddress);
        m_aData = null ;
        m_Select = false;
        mEdit = false;
    }

    public Integer getMstCode()
    {
        return m_Station.getCode();
    }
    public String getMstName() {
        return m_Station.getName();
    }
    public String getAddress()
    {
        return m_Station.getAddress();
    }
    public boolean isSelected(){ return m_Select; }
    public boolean isEdit(){ return  mEdit; }

    public int getSelIndex(){ return mSelIndex; }

    // Set
    public void setData(String strYear, String strMonth, String strDay, String strHour, String strOX, String strPM25, String strWD, String strWS)
    {
        SoramameData data = new SoramameData(strYear, strMonth, strDay, strHour, strOX, strPM25, strWD, strWS);
        addData(data);
    }

    public void setData(SoramameData orig){
        SoramameData data = new SoramameData(orig.getDate(), orig.getOX(), orig.getPM25(), orig.getWD(), orig.getWS());
        addData(data);
    }

    public boolean setAllow(String strOX, String strPM25, String strWD){
        boolean flag[] = new boolean[3];
        int code[] = new int[3];
        code[0] = strOX.codePointAt(0);
        code[1] = strPM25.codePointAt(0);
        code[2] = strWD.codePointAt(0);
        for(int i=0; i<3; i++){
            flag[i] = false;
            if(code[i] == 9675){
                flag[i] = true;
            }
        }
        m_Station.setAllow(flag);
        return m_Station.isAllow();
    }

    public boolean setAllow(int nOX, int nPM25, int nWD){
        boolean flag[] = new boolean[3];
        for(int i=0; i<3; i++){
            flag[i] = false;
        }
        if(nOX != 0){ flag[0] = true ; }
        if(nPM25 != 0){ flag[1] = true; }
        if(nWD != 0){ flag[2] = true; }

        m_Station.setAllow(flag);
        return m_Station.isAllow();
    }

    public boolean getAllow(int index){
        boolean flag = false;
        switch(index){
            case 0:
                flag = m_Station.getAllowOX();
                break;
            case 1:
                flag = m_Station.getAllowPM25();
                break;
            case 2:
                flag = m_Station.getAllowWS();
                break;
        }
        return flag;
    }

    public void setSelected( int flag ){
        m_Select = false;
        if( flag != 0 ) {
            m_Select = true;
        }
    }

    public void setEdit(boolean flag){
        mEdit = flag;
    }
    public void setSelIndex(int index){ mSelIndex = index; }

    public void clearData(){
        if( m_aData != null){
            m_aData.clear();
        }
        m_Select = false;
    }

    private void addData(SoramameData data){
        if( m_aData == null){
            m_aData = new ArrayList<SoramameData>();
        }
        m_aData.add(data);
    }

    // 内部データがすでに読み込まれているかを判定する。
    // 内部データが空ならfalse
    // 内部データの先頭要素にて判定する。
    public boolean isLoaded(String strYear, String strMon, String strDay, String strHour){
        // 要素が無ければfalse
        if(getSize() < 1){ return false; }

        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Integer.valueOf(strYear), Integer.valueOf(strMon), Integer.valueOf(strDay), Integer.valueOf(strHour), 0);

        boolean loaded = false;
        if( m_aData.get(0).getDate().before(cal) ){ loaded = true; }
        
        return loaded;
    }

    public String getStationInfo()
    {
        return m_Station.getString();
    }
    public String getData(int nIndex)
    {
        if( getSize() < 1){ return ""; }

        return m_aData.get(nIndex).Format() ;
    }

    public int getSize()
    {
        int size = 0;
        if( m_aData != null){ size = m_aData.size(); }
        return size;
    }

    public ArrayList<SoramameData> getData()
    {
        return m_aData;
    }

    public void addAll(int index, ArrayList<Soramame.SoramameData> list){
        if(m_aData == null){
            m_aData = new ArrayList<SoramameData>();
        }
        m_aData.addAll(index, list);
    }

    // 風向文字列->インデックス変換
    // 静穏 0/北 1/北北東 2/北東 3 -> 北北西 16
    public Integer parseWD(String strWD){
        Integer nWD = -1;
        int start = 0;
        do {
            if (strWD.startsWith("北", start)) {
                // 東北東、西北西
                if(start == 1){
                    if(nWD == 5){ nWD = 4; }
                    else if(nWD == 13){ nWD = 14; }
                }
                else{
                    nWD = 1;
                }
            } else if (strWD.startsWith("南", start)) {
                // 東南東、西南西
                if(start == 1){
                    if(nWD == 5){ nWD = 6; }
                    else if(nWD == 13){ nWD = 12; }
                }
                else {
                    nWD = 9;
                }
            }
            else if(strWD.startsWith("東", start)){
                // 北東、南東
                if(start == 1){
                    if(nWD == 1){ nWD = 3; }
                    else{ nWD = 7; }
                }
                // 北北東、南南東
                else if(start == 2){
                    if(nWD == 1){ nWD = 2; }
                    else if(nWD == 9){ nWD = 8; }
                }
                else {
                    nWD = 5;
                }
            }
            else if(strWD.startsWith("西", start)){
                // 北西、南西
                if(start == 1){
                    if(nWD == 1){ nWD = 15; }
                    else{ nWD = 11; }
                }
                // 北北西、南南西
                else if(start == 2){
                    if(nWD == 1){ nWD = 16; }
                    else if(nWD == 9){ nWD = 10; }
                }
                else {
                    nWD = 13;
                }
            }
            // 静穏
            else{
                nWD = 0;
                break ;
            }
        }while(strWD.length() > ++start);

        return nWD;
    }

    public Integer getValue(String strValue, Integer p){
        Integer value;
        value = p;
        try{
            value = Integer.parseInt(strValue);
        }
        catch(NumberFormatException e){
            value = p;
        }
        return value;
    }
    public Float getValue(String strValue, Float p){
        Float value;
        value = p;
        try{
            value = Float.parseFloat(strValue);
        }
        catch(NumberFormatException e){
            value = p;
        }
        return value;
    }
}
