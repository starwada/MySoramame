package com.example.wada.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


/**
 * そらまめ PM2.5測定値用グラフカスタムビュー
 * UI Component/Custom View にて作成
 */
public class SoraGraphView extends View {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private Soramame mSoramame;     // 測定局のPM2.5データ
    private float mMax[] = new float[3];  // 表示データのMAX
    private int mMaxTime[] = new int[3];    // 最大値の時間（インデックス）
    private float mAve[] = new float[3];    // 表示データの24時間平均値
    private Paint mBack;
    private Paint mLine ;
    private Paint mDot ;
    private Paint mHourLine;
    private Path mHourPath;
    private float mHourTextWidth[] = new float[4];
    private RectF mRect;
//    private float[] mVert;

    private Paint mOX;
//    private float[] mOXLines;
    // 表示区分 PM2.5 OX（光化学オキシダント） WS（風速）
    private float mDotY[][] = { {10.0f, 15.0f, 35.0f, 50.0f, 70.0f, 100.0f },
        {0.02f, 0.04f, 0.06f, 0.12f, 0.24f, 0.34f },
        {4.0f, 7.0f, 10.0f, 13.0f, 15.0f, 25.0f}};

    private int mIndex;                     // 強調日時インデックス
    private String mstrValue;
    private int mToastPos[] = { 0,0 };
    private int mMode;                      // 表示データモード 0 PM2.5/1 OX/2 風速
    private int mDispDay;               // 表示日数 0 全て

    public SoraGraphView(Context context) {
        super(context);
        init(null, 0);
    }

    public SoraGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SoraGraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        try {
            // Load attributes
            final TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.SoraGraphView, defStyle, 0);

            mExampleString = a.getString(
                    R.styleable.SoraGraphView_exampleString);
            mExampleColor = a.getColor(
                    R.styleable.SoraGraphView_exampleColor,
                    mExampleColor);
            // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
            // values that should fall on pixel boundaries.
            mExampleDimension = a.getDimension(
                    R.styleable.SoraGraphView_exampleDimension,
                    mExampleDimension);

            if (a.hasValue(R.styleable.SoraGraphView_exampleDrawable)) {
                mExampleDrawable = a.getDrawable(
                        R.styleable.SoraGraphView_exampleDrawable);
                mExampleDrawable.setCallback(this);
            }

            a.recycle();

            // Set up a default TextPaint object
            mTextPaint = new TextPaint();
            mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setTextAlign(Paint.Align.LEFT);

            // Update TextPaint and text measurements from attributes
            invalidateTextPaintAndMeasurements();

            mMax[0] = mMax[1] = mMax[2] = 0.0f;
            mMaxTime[0] = mMaxTime[1] = mMaxTime[2] = 0;
            mAve[0] = mAve[1] = mAve[2] = 0.0f;
            mBack = new Paint();
            mBack.setColor(Color.argb(75, 0, 0, 255));
            mLine = new Paint();
            mLine.setColor(Color.argb(125, 0, 0, 0));
            mLine.setStrokeWidth(4);
            mDot = new Paint();
            mDot.setColor(Color.argb(255, 255, 0, 0));
            mDot.setStrokeWidth(2);
            mRect = new RectF();
            mIndex = 0;
            mMode = 0;
            mDispDay = 3;
            // OX用のペイント情報
            mOX = new Paint();
            mOX.setColor(Color.argb(75, 255, 0, 0));
            mOX.setStrokeWidth(2.4f);
            // 時間線
            mHourLine = new Paint();
            mHourLine.setColor(Color.argb(75, 0, 0, 0));
            mHourLine.setStyle(Paint.Style.STROKE);
            mHourLine.setStrokeWidth(1);
            mHourLine.setPathEffect(new DashPathEffect(new float[]{ 5.0f, 5.0f }, 0));
            mHourPath = new Path();
            mHourTextWidth[0] = mHourTextWidth[1] = 0.0f;
            mHourTextWidth[2] = mHourTextWidth[3] = 0.0f;
        }
        catch(java.lang.NullPointerException e){
            e.getMessage();
        }
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        mHourTextWidth[0] = mTextPaint.measureText("0");
        mHourTextWidth[1] = mTextPaint.measureText("6");
        mHourTextWidth[2] = mTextPaint.measureText("12");
        mHourTextWidth[3] = mTextPaint.measureText("18");

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
//        mTextHeight = fontMetrics.bottom;
        // ほぼ文字高さのようなので、マイナスで返るので反転
        mTextHeight = -fontMetrics.ascent;
    }

    public void setData(Soramame sora){
        if(mSoramame != null){ mSoramame = null; }
        if( sora.getSize() < 1 ){ return ; }

        mMax[0] = mMax[1] = mMax[2] = 0.0f;
        mMaxTime[0] = mMaxTime[1] = mMaxTime[2] = 0;
        mAve[0] = mAve[1] = mAve[2] = 0.0f;
        mSoramame = new Soramame(sora.getMstCode(), sora.getMstName(), sora.getAddress());
        ArrayList<Soramame.SoramameData> list = sora.getData();
        int nCount=0;
        for( Soramame.SoramameData data : list){
            // それぞれのMAX値を取得
            if( (float)data.getPM25() > mMax[0] ){ mMax[0] = (float)data.getPM25(); mMaxTime[0] = nCount; }
            if( data.getOX() > mMax[1] ){ mMax[1] = data.getOX(); mMaxTime[1] = nCount; }
            if( data.getWS() > mMax[2] ){ mMax[2] = data.getWS(); mMaxTime[2] = nCount; }
            // 24時間平均値計算
            if( nCount++ < 24){
                mAve[0] += (float)data.getPM25();
                mAve[1] += data.getOX();
                mAve[2] += data.getWS();
            }
            mSoramame.setData(data);
        }
        mAve[0] /= 24.0f;
        mAve[1] /=24.0f;
        mAve[2] /= 24.0f;

        // 再描画
        invalidate();
    }

    // 強調日時設定
    public void setPos(int position){
        mIndex = position;
        invalidate();
    }

    public int getPos(){
        return mIndex;
    }

    // 表示データ設定
    public void setMode(int mode){
        mMode = mode;
        invalidate();
    }

    // 表示日数設定
    public void setDispDay(int dispDay){
        // 入力はインデックスなので１を足す。
        mDispDay = dispDay+1;
        // 最大（8）は全日数（0）とする。
        if(mDispDay == 8){
            mDispDay = 0;
        }
        invalidate();
    }

    // 最大値
    public String getMaxString(){
        ArrayList<Soramame.SoramameData> list = mSoramame.getData();
        return String.format(Locale.JAPANESE, "最高値：%s (%s)", getSpecString(mMax), list.get(mMaxTime[mMode]).getCalendarString());
    }

    // 平均値
    public String getAveString(){
        ArrayList<Soramame.SoramameData> list = mSoramame.getData();
        return String.format(Locale.JAPANESE, "24時間平均値：%s", getSpecString(mAve));
    }

    private String getSpecString(float mode[]){
        String strSpec = "";
        switch (mMode){
            case 0:
                strSpec = String.format(Locale.JAPANESE, "%.0f μg/m3", mode[mMode]);
                break;
            case 1:
                strSpec = String.format(Locale.JAPANESE, "%.2f ppm", mode[mMode]);
                break;
            case 2:
                strSpec = String.format(Locale.JAPANESE, "%.1f m/s", mode[mMode]);
                break;
        }
        return strSpec;
    }

    // タッチ処理
    public void Touch(float px){
        if (mSoramame.getSize() > 0) {
            int contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();

            ArrayList<Soramame.SoramameData> list = mSoramame.getData();
            float x = getWidth() - getPaddingRight();
            float gap = 0.0f;
            if( mDispDay == 0 ){ gap = (float)contentWidth/list.size(); }
            else { gap = (float)contentWidth/(mDispDay*24) ; }
            int pos = 0;
            if(x-px < 0.0f){ pos = 0; }
            else if(px < getPaddingLeft()){ pos = list.size()-1; }
            else if(gap > 0.0){ pos = (int)((x-px)/gap); }
            setPos(pos);
        }
    }

    // カレントにトースト（ツールチップ）表示
    public void showToast(){
        Toast toast = Toast.makeText(this.getContext(), mstrValue, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP|Gravity.START, mToastPos[0], mToastPos[1]);
        toast.show();
    }

    // キャプチャ
    public void Capture() {
        setDrawingCacheEnabled(true);
        // Viewのキャッシュを取得
        Bitmap cache = getDrawingCache();
        Bitmap screenShot = Bitmap.createBitmap(cache);
        setDrawingCacheEnabled(false);
        // 読み書きするファイル名を指定
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/capture.jpeg");
        // 指定したファイル名が無ければ作成する。
        file.getParentFile().mkdir();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, false);
            // 画像のフォーマットと画質と出力先を指定して保存
            // 100で165KB、値を半分にすると1/4に減る
            screenShot.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ie) {
                    fos = null;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        if(mSoramame == null){ return ; }
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        mExampleDimension = 32.0f;
        invalidateTextPaintAndMeasurements();

        // グラフ描画
        // グラフ背景
        float y = (float)(paddingTop+contentHeight);
        float rh = (float)contentHeight/mDotY[mMode][5];

        // PM2.5/OX/WS
        // ～１０/0.0-0.02/0.2-3.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][0], (float)(paddingLeft+contentWidth), y);
//        mBack.setColor(Color.argb(75, 0, 0, 255));
        mBack.setColor(0xFF2196F3);
        canvas.drawRect(mRect, mBack);
        // １１～１５/0.021-0.04/4.0-6.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][1], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][0]);
//        mBack.setColor(Color.argb(75, 0, 255,255));
        mBack.setColor(0xFF81D4FA);
        canvas.drawRect(mRect, mBack);
        // １６～３５/0.041-0.06/7.0-9.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][2], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][1]);
        mBack.setColor(Color.argb(75, 0, 255,128));
        canvas.drawRect(mRect, mBack);
        // ３６～５０/0.061-0.119/10.0-12.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][3], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][2]);
        mBack.setColor(Color.argb(75, 255, 255,0));
        canvas.drawRect(mRect, mBack);
        // ５１～７０/0.12-0.239/13.0-14.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][4], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][3]);
        mBack.setColor(Color.argb(75, 255, 128,0));
        canvas.drawRect(mRect, mBack);
        // 70-100/0.24-0.34/15.0-25.0
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][5], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][4]);
        mBack.setColor(Color.argb(75, 255, 0,0));
        canvas.drawRect(mRect, mBack);

        // グラフ枠
        mLine.setStrokeWidth(4);
        canvas.drawLine(paddingLeft, paddingTop + contentHeight, paddingLeft + contentWidth, paddingTop + contentHeight, mLine);
        canvas.drawLine( paddingLeft, paddingTop, paddingLeft, contentHeight+paddingTop, mLine );
        y = (float)(paddingTop+contentHeight);
        mLine.setStrokeWidth(1);
        for(int i=0; i<5; i++){
            y -= (float)contentHeight/5;
            canvas.drawLine(paddingLeft, y, paddingLeft + contentWidth, y, mLine);
            switch(mMode){
                case 0:
                    canvas.drawText(String.format("%d", i*20+20), 0, y + mTextHeight/2, mTextPaint);
                    break;
                case 1:
                    canvas.drawText(String.format("%.2f", i*mDotY[mMode][5]/5.0f+mDotY[mMode][5]/5.0f), 0, y + mTextHeight/2, mTextPaint);
                    break;
                case 2:
                    canvas.drawText(String.format("%.1f", i*mDotY[mMode][5]/5.0f+mDotY[mMode][5]/5.0f), 0, y + mTextHeight/2, mTextPaint);
                    break;
            }
        }

        // グラフ
        if(mSoramame.getSize() > 0){
            ArrayList<Soramame.SoramameData> list = mSoramame.getData();
            float x=paddingLeft+contentWidth;
            // ここで、時間（データ数）での分割
            // listには新しいデータから入っている
            float gap = 0.0f ;
            if( mDispDay == 0 ){ gap = (float)contentWidth/list.size(); }
            else { gap = (float)contentWidth/(mDispDay*24) ; }

            y = (float)(paddingTop + contentHeight);

            int nCount=0;
            int nHour = 0;
            float doty = 0f;
            float fradius = 3.0f;
            float fOXY[] = { 0.0f, 0.0f  };
            for( Soramame.SoramameData data : list){
                if( mDispDay != 0 && nCount > mDispDay*24 ){ break; }
                fradius = 3.0f;
                switch(mMode){
                    case 0:
                        doty = y-(data.getPM25() * (float)contentHeight/mDotY[mMode][5]);
                        break;
                    case 1:
                        doty = y-(data.getOX() * (float)contentHeight/mDotY[mMode][5]);
                        break;
                    case 2:
                        doty = y-(data.getWS() * (float)contentHeight/mDotY[mMode][5]);
                        break;
                }

                if( (mMode == 0 && data.getPM25() > 0) ||
                        (mMode == 1 && data.getOX() > 0.0) ||
                        (mMode == 2 && data.getWS() > 0.0 )) {
                    if( nCount == mIndex) {
                        fradius = 12.0f;
                        mstrValue = data.getCalendarString() + " ";
                        switch(mMode){
                            case 0:
                                mstrValue += data.getPM25String();
                                break;
                            case 1:
                                mstrValue += data.getOXString();
                                break;
                            case 2:
                                mstrValue += data.getWSString();
                                break;
                        }
                        mToastPos[0] = (int)x;
                        mToastPos[1] = (int)doty;
                    }
                    canvas.drawCircle(x, doty, fradius, mDot);
                }
                // 時間軸描画
                if( data.getDate().get(Calendar.HOUR_OF_DAY) == 0 ){
                    canvas.drawLine(x, paddingTop, x, contentHeight + paddingTop, mLine);
                    if(0 < mDispDay && mDispDay < 6) {
                        nHour=0;
                        canvas.drawText("0", x - mHourTextWidth[nHour++]/2, paddingTop + mTextHeight, mTextPaint);
                    }
                }
                else if(data.getDate().get(Calendar.HOUR_OF_DAY) % 6 == 0 && ( 0 < mDispDay && mDispDay < 6)){
                    // Pathは内部でパスを保持しているので、リセットが必要。
                    mHourPath.reset();
                    mHourPath.moveTo(x, paddingTop);
                    mHourPath.lineTo(x, contentHeight + paddingTop);
                    canvas.drawPath(mHourPath, mHourLine);
                    canvas.drawText(String.format("%d", data.getDate().get(Calendar.HOUR_OF_DAY)),
                            x - mHourTextWidth[nHour++]/2, paddingTop + mTextHeight, mTextPaint);
                }
                if( data.getDate().get(Calendar.HOUR_OF_DAY) == 1 ){
                    // 日付描画
                    canvas.drawText(String.format("%d日", data.getDate().get(Calendar.DAY_OF_MONTH)),
                            x, paddingTop + contentHeight + mTextHeight, mTextPaint);
                }
                // リストにてクリックしたインデックスデータに描画<-ここを画像に切替える
//                if( nCount == mIndex){
//                    mVert[0]=x;
//                    mVert[1] = doty;
//                    mVert[2]=x+gap*2;
//                    mVert[3]=doty-30;
//                    mVert[4]=x-gap*2;
//                    mVert[5]=mVert[3];
//
//                    canvas.drawVertices(Canvas.VertexMode.TRIANGLES, 6, mVert, 0, null, 0,null,0,null,0,0, mDot);
//                }
                nCount += 1;
                x -= gap;
                // グラフ用ポリライン描画
                fOXY[1] = fOXY[0];
                fOXY[0] = (doty > y ? y : doty);
                if( nCount > 1){
                    canvas.drawLine(x+gap, fOXY[0], x+gap+gap, fOXY[1], mOX);
                }
            }
        }

//        mTextPaint.setTextSize(60.0f);
        // Draw the text.
//        canvas.drawText(mExampleString,
//                paddingLeft + (contentWidth - mTextWidth) / 2,
//                paddingTop + (float)contentHeight/5 + mTextHeight,
//                mTextPaint);

        // Draw the example drawable on top of the text.
//        if (mExampleDrawable != null) {
//            mExampleDrawable.setBounds(paddingLeft, paddingTop,
//                    paddingLeft + contentWidth, paddingTop + contentHeight);
//            mExampleDrawable.draw(canvas);
//        }
    }

    // 使用側にてリスナーにて処理をする
//    @Override
//    public boolean onTouchEvent(MotionEvent event){
//
//        if(event.getActionMasked() == MotionEvent.ACTION_MOVE ) {
//            float px = event.getX(0);
//            if (mSoramame.getSize() > 0) {
//                int contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
//
//                ArrayList<Soramame.SoramameData> list = mSoramame.getData();
//                float x = getWidth() - getPaddingRight();
//                float gap = (float) contentWidth / list.size();
//                int pos = 0;
//                if(x-px < 0.0f){ pos = 0; }
//                else if(px < getPaddingLeft()){ pos = list.size()-1; }
//                else if(gap > 0.0){ pos = (int)((x-px)/gap); }
//                setPos(pos);
//            }
//        }
//        else if(event.getActionMasked() == MotionEvent.ACTION_UP){
//            Toast toast = Toast.makeText(this.getContext(), mstrValue, Toast.LENGTH_SHORT);
//            toast.setGravity(Gravity.TOP|Gravity.LEFT, mToastPos[0], mToastPos[1]);
//            toast.show();
//        }
//
//        return true;
//    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }
}
