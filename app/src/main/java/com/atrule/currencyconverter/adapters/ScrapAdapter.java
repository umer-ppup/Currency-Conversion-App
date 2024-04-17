package com.atrule.currencyconverter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atrule.currencyconverter.R;
import com.atrule.currencyconverter.classes.ScrapData;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ScrapAdapter extends RecyclerView.Adapter<ScrapAdapter.ViewHolder> {
    //region member variables
    final ArrayList<ScrapData> scrapDataArrayList;
    //endregion

    public ScrapAdapter(ArrayList<ScrapData> scrapDataArrayList) {
        //region initialization
        this.scrapDataArrayList = scrapDataArrayList;
        //endregion
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //region recycler item view creation
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_scrap_item, parent, false);
        return new ViewHolder(view);
        //endregion
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //region setting values
        holder.tvCurrency.setText(scrapDataArrayList.get(position).getCurrency());
        holder.tvSymbol.setText(scrapDataArrayList.get(position).getSymbol());
        holder.tvBuying.setText(scrapDataArrayList.get(position).getBuying());
        holder.tvSelling.setText(scrapDataArrayList.get(position).getSelling());
        holder.tvFlag.setImageResource(scrapDataArrayList.get(position).getFlag());
        //endregion
    }

    @Override
    public int getItemCount() {
        return scrapDataArrayList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        //region declaration
        final TextView tvCurrency;
        final TextView tvSymbol;
        final TextView tvBuying;
        final TextView tvSelling;
        final CircleImageView tvFlag;
        //endregion

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            //region initialization
            tvCurrency = itemView.findViewById(R.id.tvCurrency);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            tvBuying = itemView.findViewById(R.id.tvBuying);
            tvSelling = itemView.findViewById(R.id.tvSelling);
            tvFlag = itemView.findViewById(R.id.tvFlag);
            //endregion
        }
    }
}