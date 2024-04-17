package com.atrule.currencyconverter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atrule.currencyconverter.R;
import com.atrule.currencyconverter.classes.GoldScrap;

import java.util.ArrayList;

public class ScrapGoldAdapter extends RecyclerView.Adapter<ScrapGoldAdapter.ViewHolder> {
    //region member variables
    final ArrayList<GoldScrap> scrapDataArrayList;
    //endregion

    public ScrapGoldAdapter(ArrayList<GoldScrap> scrapDataArrayList) {
        //region initialization
        this.scrapDataArrayList = scrapDataArrayList;
        //endregion
    }

    @NonNull
    @Override
    public ScrapGoldAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //region recycler item view creation
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_gold_item, parent, false);
        return new ScrapGoldAdapter.ViewHolder(view);
        //endregion
    }

    @Override
    public void onBindViewHolder(@NonNull ScrapGoldAdapter.ViewHolder holder, int position) {
        //region setting values
        holder.tvGoldWeight.setText(scrapDataArrayList.get(position).getGoldWeight());
        holder.tvCarat24.setText(scrapDataArrayList.get(position).getCarat24());
        holder.tvCarat22.setText(scrapDataArrayList.get(position).getCarat22());
        holder.tvCarat21.setText(scrapDataArrayList.get(position).getCarat21());
        holder.tvCarat18.setText(scrapDataArrayList.get(position).getCarat18());
        //endregion
    }

    @Override
    public int getItemCount() {
        return scrapDataArrayList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        //region declaration
        final TextView tvGoldWeight;
        final TextView tvCarat24;
        final TextView tvCarat22;
        final TextView tvCarat21;
        final TextView tvCarat18;
        //endregion

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            //region initialization
            tvGoldWeight = itemView.findViewById(R.id.tvGoldWeight);
            tvCarat24 = itemView.findViewById(R.id.tvCarat24);
            tvCarat22 = itemView.findViewById(R.id.tvCarat22);
            tvCarat21 = itemView.findViewById(R.id.tvCarat21);
            tvCarat18 = itemView.findViewById(R.id.tvCarat18);
            //endregion
        }
    }
}