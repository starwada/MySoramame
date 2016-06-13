package com.example.wada.myapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wada on 2016/06/07.
 */
public class GraphViewAdapter extends RecyclerView.Adapter<GraphViewAdapter.ViewHolder> {
    private Context mContext;
    private List<Soramame> mList;
    private int mMode;      // データ表示モード　0 PM2.5/1 光化学オキシダント/2 風速
    private int mDay;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView stationname;
        public SoraGraphView soragraph;
        public TextView soramax, soraave;
        public TextView date, hour, value;
        public ImageView imageWS;

        public ViewHolder(View view){
            super(view);
            soragraph = (SoraGraphView) view.findViewById(R.id.soragraph);
            stationname = (TextView)view.findViewById(R.id.MstName);
            soramax = (TextView)view.findViewById(R.id.soramax);
            soraave = (TextView)view.findViewById(R.id.soraave);
            date = (TextView)view.findViewById(R.id.date);
            hour = (TextView)view.findViewById(R.id.hour);
            value = (TextView)view.findViewById(R.id.value);
            imageWS = (ImageView)view.findViewById(R.id.imageWS);
        }
    }

    public GraphViewAdapter(Context context, List<Soramame> objects){
        this.mContext = context;
        this.mList = objects;
        this.mMode = 0;
        this.mDay = 0;
    }

    public void SetMode(int mode){ mMode = mode; }
    public void SetDispDay(int day){ mDay = day; }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.graphcard, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Soramame data = mList.get(position);

        holder.soragraph.setData(data);
        holder.soragraph.setMode(mMode);
        holder.soragraph.setDispDay(mDay);
        holder.soramax.setText(holder.soragraph.getMaxString());
        holder.soraave.setText(holder.soragraph.getAveString());
        holder.stationname.setText(data.getMstName() + ":" + String.valueOf(data.getSelIndex()));

        TouchEvent( holder, position);

        // グラフビューにタッチリスナー設定
        holder.soragraph.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getActionMasked() == MotionEvent.ACTION_MOVE ) {
                    float px = event.getX(0);
                    int sum = event.getPointerCount();
                    holder.soragraph.Touch(px);
                }
                else if(event.getActionMasked() == MotionEvent.ACTION_UP){
//                    holder.soragraph.showToast();
                    TouchEvent( holder, position);
                }
                return true;
            }
        });
    }

    public void TouchEvent(final ViewHolder holder, final int position ){
        ArrayList<Soramame.SoramameData> list = mList.get(position).getData();
        Soramame.SoramameData val = list.get(holder.soragraph.getPos());
        holder.date.setText(val.getDateString());
        holder.hour.setText(val.getHourString());
        switch(mMode){
            case 0:
                holder.value.setText(val.getPM25String());
                holder.imageWS.setVisibility(View.INVISIBLE);
                break;
            case 1:
                holder.value.setText(val.getOXString());
                holder.imageWS.setVisibility(View.INVISIBLE);
                break;
            case 2:
                // 風向のアイコン用にスペースを入れる
                holder.value.setText(val.getWSString()+"　");
                // 静穏の場合
                if(val.getWDRotation() < 0.0f) {
                    holder.imageWS.setVisibility(View.INVISIBLE);
                }else{
                    holder.imageWS.setVisibility(View.VISIBLE);
                    // 風向の向きにアイコンを回転させる
                    holder.imageWS.setRotation(val.getWDRotation());
                }
                break;
        }

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}
