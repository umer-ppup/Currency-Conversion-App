package com.atrule.currencyconverter.adapters;

import static android.content.Context.MODE_PRIVATE;
import static com.atrule.currencyconverter.util.MyUtil.haveNetwork;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.atrule.currencyconverter.R;
import com.atrule.currencyconverter.classes.Country;
import com.atrule.currencyconverter.receivers.MyReceiver;
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;

import java.util.ArrayList;
import java.util.Collections;

import cn.pedant.SweetAlert.SweetAlertDialog;
import de.hdodenhof.circleimageview.CircleImageView;

public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.ViewHolder> {
    //region variable declarations
    private final ArrayList<Country> countries;
    private final FragmentActivity activity;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    public MenuItem menuItem;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private Intent intent;
    private final Context context;
    private final CurrencyListener listener;
    //endregion

    public CurrencyAdapter(Context context, ArrayList<Country> countries, FragmentActivity activity, MenuItem menuItem, CurrencyListener listener) {
        this.countries = countries;
        this.activity = activity;
        this.menuItem = menuItem;
        this.listener = listener;
        this.context = context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //region setting values on items
        sharedpreferences = activity.getSharedPreferences("PinCities", MODE_PRIVATE);
        editor = sharedpreferences.edit();
        holder.tvCountryName.setText(countries.get(position).getName());
        holder.tvCountryCode.setText(countries.get(position).getCode());
        holder.tvRate.setText(countries.get(position).getRate());
        holder.tvSymbol.setText(countries.get(position).getSymbol());

        if(countries.get(position).getIncreaseDecrease() == 0){
            holder.imageView2.setVisibility(View.VISIBLE);
            holder.imageView2.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.no_change_icon, null));
            holder.textView.setText("No Change");
            holder.textView.setTextColor(context.getResources().getColor(R.color.darkGreen));
        }
        else if(countries.get(position).getIncreaseDecrease() == 1){
            holder.imageView2.setVisibility(View.VISIBLE);
            holder.imageView2.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.increase_icon, null));
            holder.textView.setText(countries.get(position).getPercent() +" %");
            holder.textView.setTextColor(context.getResources().getColor(R.color.lightGreen));
        }
        else if(countries.get(position).getIncreaseDecrease() == -1){
            holder.imageView2.setVisibility(View.VISIBLE);
            holder.imageView2.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.decrease_icon, null));
            holder.textView.setText(countries.get(position).getPercent() +" %");
            holder.textView.setTextColor(context.getResources().getColor(R.color.tomato));
        }

        if(countries.get(position).isChecked()){
            holder.btnToggle.setChecked(true);
        }
        if(!countries.get(position).isChecked()){
            holder.btnToggle.setChecked(false);
        }

        Uri uri = Uri.parse(countries.get(position).getFlag());
        if(!activity.isDestroyed()){
            GlideToVectorYou.justLoadImage(activity, uri, holder.imageView);
        }
        //endregion

        //region toogle button handler
        holder.btnToggle.setOnClickListener(v -> {
            if(holder.btnToggle.isChecked()){
                editor.putBoolean(countries.get(position).getName(), true);
                if(editor.commit()){
                    countries.get(position).setChecked(true);
                    notifyDataSetChanged();
                }
                else{
                    holder.btnToggle.setChecked(false);
                }
            }
            else{
                editor.putBoolean(countries.get(position).getName(), false);
                editor.remove(countries.get(position).getName());
                if(editor.commit()){
                    SharedPreferences sharedPreferences1 = activity.getSharedPreferences("PinCities", MODE_PRIVATE);
                    if(sharedPreferences1.getAll().isEmpty()){
                        intent = new Intent(activity, MyReceiver.class);
                        intent.setAction("MyAlarm");
                        alarmMgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                            alarmIntent = PendingIntent.getBroadcast(activity, 201, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_MUTABLE);
                        }
                        else{
                            alarmIntent = PendingIntent.getBroadcast(activity, 201, intent, PendingIntent.FLAG_NO_CREATE);
                        }

                        if (alarmIntent != null && alarmMgr != null) {
                            alarmMgr.cancel(alarmIntent);
                        }

                        SharedPreferences sharedPreferences = activity.getSharedPreferences("Notify Me", MODE_PRIVATE);
                        SharedPreferences.Editor edt = sharedPreferences.edit();
                        edt.putString("time", "Notify Me?");
                        edt.apply();

                        menuItem.setTitle("Notify Me?");
                    }
                    countries.get(position).setChecked(false);
                    notifyDataSetChanged();
                }
                else{
                    holder.btnToggle.setChecked(true);
                }
            }

            Collections.sort(countries, (o1, o2) -> Boolean.compare(o2.isChecked, o1.isChecked));
            notifyDataSetChanged();
        });
        //endregion

        //region currency amount edit text
        holder.tvRate.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if(haveNetwork(activity)){
                    listener.onCurrencyChange(holder.tvCountryCode.getText().toString(), holder.tvRate.getText().toString());
                    holder.tvRate.clearFocus();
                    holder.tvRate.setText(countries.get(position).getRate());
                }
                else{
                    holder.tvRate.clearFocus();
                    holder.tvRate.setText(countries.get(position).getRate());
                    //Toast.makeText(activity, "You don't have internet.", Toast.LENGTH_SHORT).show();
                    new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Oops...")
                            .setContentText("No Internet Connection!")
                            .show();
                }
                //Toast.makeText(activity, holder.tvRate.getText().toString(), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        //endregion

        //region focus change call
        holder.tvRate.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v, activity);
                holder.tvRate.setText(countries.get(position).getRate());
            }
        });
        //endregion
    }

    @Override
    public int getItemCount() {
        return countries.size();
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    //region view holder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvCountryName;
        public final TextView tvCountryCode;
        public final EditText tvRate;
        public final TextView tvSymbol;
        public final TextView textView;
        public final ImageView imageView2;
        public final ToggleButton btnToggle;
        public final CircleImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCountryName = itemView.findViewById(R.id.tvCountryName);
            tvCountryCode = itemView.findViewById(R.id.tvCountryCode);
            tvRate = itemView.findViewById(R.id.tvRate);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            btnToggle = itemView.findViewById(R.id.btnToggle);
            imageView = itemView.findViewById(R.id.imageView);

            textView = itemView.findViewById(R.id.textView);
            imageView2 = itemView.findViewById(R.id.imageView2);
        }
    }
    //endregion

    //region listener for currency change
    public interface CurrencyListener {
        void onCurrencyChange(String currencyCode, String currencyAmount);
    }
    //endregion

    //region function to hide keyboard
    public void hideKeyboard(View view, Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    //endregion
}