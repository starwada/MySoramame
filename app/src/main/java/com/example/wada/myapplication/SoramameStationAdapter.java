package com.example.wada.myapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Wada on 2016/05/31
 * CardView,RecyclerView用に書き直し
 */
public class SoramameStationAdapter extends RecyclerView.Adapter<SoramameStationAdapter.ViewHolder> {
    private Context mContext;
    private List<Soramame> mList;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView stationname, address ;
        public ImageView iPM25, iOX, iWS;
        public CheckBox sel;

        public ViewHolder(View view){
            super(view);
            sel = (CheckBox) view.findViewById(R.id.select);
            iPM25 = (ImageView)view.findViewById(R.id.imagePM25);
            iOX = (ImageView)view.findViewById(R.id.imageOX);
            iWS = (ImageView)view.findViewById(R.id.imageWS);
            stationname = (TextView)view.findViewById(R.id.name);
            address = (TextView)view.findViewById(R.id.address);
        }
    }

    public SoramameStationAdapter(Context context, List<Soramame> objects){
        this.mContext = context;
        this.mList = objects;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stationlayout, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Soramame data = mList.get(position);

        holder.sel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int flag = 0;
                if(holder.sel.isChecked()){ flag = 1; }
                Soramame data = mList.get(position);
                data.setSelected(flag);
                data.setEdit(true);
            }
        });
        holder.sel.setChecked(data.isSelected());
        holder.stationname.setText(data.getMstName());
        holder.address.setText(data.getAddress());
        holder.iOX.setImageResource(R.mipmap.ic_launcher_ox_off);
        if(data.getAllow(0)){
            holder.iOX.setImageResource(R.mipmap.ic_launcher_ox_on);
        }
        holder.iPM25.setImageResource(R.mipmap.ic_launcher_pm25_off);
        if(data.getAllow(1)){
            holder.iPM25.setImageResource(R.mipmap.ic_launcher_pm25_on);
        }
        holder.iWS.setImageResource(R.mipmap.ic_launcher_ws_off);
        if(data.getAllow(2)){
            holder.iWS.setImageResource(R.mipmap.ic_launcher_ws_on);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}
