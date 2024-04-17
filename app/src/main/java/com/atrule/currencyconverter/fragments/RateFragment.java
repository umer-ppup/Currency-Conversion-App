package com.atrule.currencyconverter.fragments;

import static com.atrule.currencyconverter.util.MyUtil.haveNetwork;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.atrule.currencyconverter.R;
import com.atrule.currencyconverter.activities.RateNotificationActivity;
import com.atrule.currencyconverter.adapters.ScrapAdapter;
import com.atrule.currencyconverter.api.RetrofitClient;
import com.atrule.currencyconverter.classes.ScrapData;
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.mynameismidori.currencypicker.ExtendedCurrency;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RateFragment extends Fragment {
    //region declaration
    private RecyclerView rvScrap;
    private ArrayList<ScrapData> scrapDataArrayList;
    private ArrayList<ScrapData> sortedScrapDataArrayList;
    private List<String> sortedCountriesNames;
    private SwipeRefreshLayout swipeRefresh;
    private ScrapAdapter scrapAdapter;
    private List<ExtendedCurrency> currencies;
    private FragmentActivity activity;
    private Context context;

    int sort = 5;

    public ImageView imageView;
    public TextView tvDate;

    public ProgressBar progressBar;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //region finding by id
        rvScrap = view.findViewById(R.id.rvScrap);
        imageView = view.findViewById(R.id.ivPakistan);
        tvDate = view.findViewById(R.id.tvDate);
        tvDate.setVisibility(View.INVISIBLE);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBarRates);
        //endregion

        //region initializations
        activity = requireActivity();
        context = requireContext();
        currencies = ExtendedCurrency.getAllCurrencies();
        scrapDataArrayList = new ArrayList<>();
        sortedScrapDataArrayList = new ArrayList<>();
        sortedCountriesNames = new ArrayList<>();
        sortedCountriesNames = Arrays.asList(getResources().getStringArray(R.array.scrap_code));
        rvScrap.setLayoutManager(new LinearLayoutManager(context));
        scrapAdapter = new ScrapAdapter(sortedScrapDataArrayList);
        rvScrap.setAdapter(scrapAdapter);
        //endregion

        //region swipe down handler
        swipeRefresh.setOnRefreshListener(this::doYourUpdate);
        //endregion

        //region Pakistan flag load
        Uri uri = Uri.parse(getResources().getString(R.string.pak_flag_url));
        if(!activity.isDestroyed()){
            GlideToVectorYou.justLoadImage(activity, uri, imageView);
        }
        //endregion

        fetchScrapData();
    }

    //region function that is used when swipe down to refresh list
    private void doYourUpdate() {
        if(haveNetwork(activity)){
            Call<String> stringCall = RetrofitClient.getInstance().getMyApi().getResponse(getString(R.string.open_rate_url));
            stringCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        if(scrapDataArrayList != null){
                            scrapDataArrayList.clear();
                        }
                        if(sortedScrapDataArrayList != null){
                            sortedScrapDataArrayList.clear();
                        }
                        String responseString = response.body();

                        if(responseString != null){
                            try{
                                Document doc = Jsoup.parse(responseString, "UTF-8");
                                Elements table = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody");
                                int size = table.get(0).childrenSize();

                                Elements date = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(1) > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > p:nth-of-type(1)");
                                String stringDate = date.get(0).childNodes().get(1).toString().trim();
                                tvDate.setText(stringDate.substring(6, 22));
                                tvDate.setVisibility(View.VISIBLE);

                                for(int i=2; i <= size; i++){
                                    Elements currency = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(1)");
                                    Elements symbol = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(2)");
                                    Elements buying = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(3)");
                                    Elements selling = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(4)");

                                    ScrapData scrapData = new ScrapData();
                                    scrapData.setCurrency(currency.get(0).childNodes().get(1).toString().trim());
                                    String string = scrapData.getCurrency();
                                    string = string.substring(12).trim();
                                    scrapData.setCurrency(string);
                                    scrapData.setSymbol(symbol.get(0).childNodes().get(0).childNodes().get(0).toString().trim());
                                    scrapData.setBuying(buying.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setSelling(selling.get(0).childNodes().get(0).toString().trim());



                                    for(int j = 0; j< currencies.size(); j++){
                                        if(currencies.get(j).getCode().equals(scrapData.getSymbol())){
                                            scrapData.setFlag(currencies.get(j).getFlag());
                                        }
                                    }
                                    scrapDataArrayList.add(scrapData);
                                }

                                for (int j = 0; j < sortedCountriesNames.size(); j++) {
                                    for (int k = 0; k < scrapDataArrayList.size(); k++) {
                                        if (scrapDataArrayList.get(k).getSymbol().equals(sortedCountriesNames.get(j))) {
                                            sortedScrapDataArrayList.add(scrapDataArrayList.get(k));
                                        }
                                    }
                                }

                                switch (sort){
                                    case 1:
                                        Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o2.getBuying()).compareTo(Double.valueOf(o1.getBuying())));
                                        break;
                                    case 2:
                                        Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o2.getSelling()).compareTo(Double.valueOf(o1.getSelling())));
                                        break;
                                    case 3:
                                        Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o1.getBuying()).compareTo(Double.valueOf(o2.getBuying())));
                                        break;
                                    case 4:
                                        Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o1.getSelling()).compareTo(Double.valueOf(o2.getSelling())));
                                        break;
                                    case 5:
                                        Collections.sort(sortedScrapDataArrayList, (o1, o2) -> o1.getCurrency().compareToIgnoreCase(o2.getCurrency()));
                                        break;
                                    case 6:
                                        Collections.sort(sortedScrapDataArrayList, (o1, o2) -> o2.getCurrency().compareToIgnoreCase(o1.getCurrency()));
                                        break;
                                }

                                if(scrapAdapter == null){
                                    scrapAdapter = new ScrapAdapter(sortedScrapDataArrayList);
                                    rvScrap.setAdapter(scrapAdapter);
                                }
                                scrapAdapter.notifyDataSetChanged();
                            }catch(Exception e){}
                        }
                    }

                    swipeRefresh.setRefreshing(false);
                }

                @Override
                public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                    swipeRefresh.setRefreshing(false);
                    if(activity != null){
                        Toast.makeText(activity, "Unable to refresh data.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        else{
            swipeRefresh.setRefreshing(false); // Disables the refresh icon

            if(activity != null){
                new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Oops...")
                        .setContentText("No Internet Connection!")
                        .show();
            }
        }
    }
    //endregion

    //region web scrapping function
    private void fetchScrapData(){
        if(haveNetwork(activity)){
            showProgress();
            Call<String> stringCall = RetrofitClient.getInstance().getMyApi().getResponse(getString(R.string.open_rate_url));
            stringCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        String responseString = response.body();
                        if(responseString != null){
                            try{
                                Document doc = Jsoup.parse(responseString);
                                Elements table = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody");
                                int size = table.get(0).childrenSize();

                                Elements date = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(1) > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > p:nth-of-type(1)");
                                String stringDate = date.get(0).childNodes().get(1).toString().trim();
                                tvDate.setText(stringDate.substring(6, 22));
                                tvDate.setVisibility(View.VISIBLE);

                                for(int i=2; i <= size; i++){
                                    Elements currency = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(1)");
                                    Elements symbol = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(2)");
                                    Elements buying = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(3)");
                                    Elements selling = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(4)");

                                    ScrapData scrapData = new ScrapData();
                                    scrapData.setCurrency(currency.get(0).childNodes().get(1).toString().trim());
                                    String string = scrapData.getCurrency();
                                    string = string.substring(12).trim();
                                    scrapData.setCurrency(string);
                                    scrapData.setSymbol(symbol.get(0).childNodes().get(0).childNodes().get(0).toString().trim());
                                    scrapData.setBuying(buying.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setSelling(selling.get(0).childNodes().get(0).toString().trim());

                                    for(int j = 0; j< currencies.size(); j++){
                                        if(currencies.get(j).getCode().equals(scrapData.getSymbol())){
                                            scrapData.setFlag(currencies.get(j).getFlag());
                                        }
                                    }
                                    scrapDataArrayList.add(scrapData);
                                }

                                for (int j = 0; j < sortedCountriesNames.size(); j++) {
                                    for (int k = 0; k < scrapDataArrayList.size(); k++) {
                                        if (scrapDataArrayList.get(k).getSymbol().equals(sortedCountriesNames.get(j))) {
                                            sortedScrapDataArrayList.add(scrapDataArrayList.get(k));
                                        }
                                    }
                                }

                                Collections.sort(sortedScrapDataArrayList, (o1, o2) -> o1.getCurrency().compareToIgnoreCase(o2.getCurrency()));

                                if(scrapAdapter == null){
                                    scrapAdapter = new ScrapAdapter(sortedScrapDataArrayList);
                                    rvScrap.setAdapter(scrapAdapter);
                                }
                                scrapAdapter.notifyDataSetChanged();
                            }catch(Exception e){}
                        }
                    }

                    scrapAdapter.notifyDataSetChanged();
                    hideProgress();
                }

                @Override
                public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                    hideProgress();

                    if(activity != null){
                        Toast.makeText(activity, "Unable to load data. Check your internet.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        else{
            hideProgress();

            if(activity != null){
                new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Oops...")
                        .setContentText("No Internet Connection!")
                        .show();
            }
        }
    }
    //endregion

    //region option menu for sorting
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.sort_menu_converter).setVisible(false);
        menu.findItem(R.id.sort_menu_rate).setVisible(true);
        menu.findItem(R.id.notify).setVisible(false);
        menu.findItem(R.id.btnNotify).setVisible(false);
        menu.findItem(R.id.btnNotification).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(!sortedScrapDataArrayList.isEmpty()){
            int id = item.getItemId();
            if(id == R.id.HLB){
                sort = 1;
                Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o2.getBuying()).compareTo(Double.valueOf(o1.getBuying())));
            }
            else if(id == R.id.HLS){
                sort = 2;
                Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o2.getSelling()).compareTo(Double.valueOf(o1.getSelling())));
            }
            else if(id == R.id.LHB){
                sort = 3;
                Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o1.getBuying()).compareTo(Double.valueOf(o2.getBuying())));
            }
            else if(id == R.id.LHS){
                sort = 4;
                Collections.sort(sortedScrapDataArrayList, (o1, o2) -> Double.valueOf(o1.getSelling()).compareTo(Double.valueOf(o2.getSelling())));
            }
            else if(id == R.id.ATZ){
                sort = 5;
                Collections.sort(sortedScrapDataArrayList, (o1, o2) -> o1.getCurrency().compareToIgnoreCase(o2.getCurrency()));
            }
            else if(id == R.id.ZTA){
                sort = 6;
                Collections.sort(sortedScrapDataArrayList, (o1, o2) -> o2.getCurrency().compareToIgnoreCase(o1.getCurrency()));
            }
            else if(id == R.id.btnNotification){
                if(context != null){
                    Intent intent = new Intent(context, RateNotificationActivity.class);
                    intent.putParcelableArrayListExtra("rates", sortedScrapDataArrayList);
                    startActivity(intent);
                }
            }
            scrapAdapter.notifyDataSetChanged();

        }
        else{
            if(activity != null){
                Toast.makeText(activity, "There is no data.", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }
    //endregion

    //region Progress Handling Functions
    public void showProgress(){
        progressBar.setVisibility(View.VISIBLE);
        rvScrap.setVisibility(View.GONE);
    }
    public void hideProgress(){
        progressBar.setVisibility(View.GONE);
        rvScrap.setVisibility(View.VISIBLE);
    }
    //endregion
}