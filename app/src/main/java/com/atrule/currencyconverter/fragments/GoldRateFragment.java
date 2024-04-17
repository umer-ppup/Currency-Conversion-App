package com.atrule.currencyconverter.fragments;

import static com.atrule.currencyconverter.util.MyUtil.haveNetwork;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.atrule.currencyconverter.adapters.ScrapGoldAdapter;
import com.atrule.currencyconverter.api.RetrofitClient;
import com.atrule.currencyconverter.classes.GoldScrap;
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoldRateFragment extends Fragment {
    //region declaration
    private RecyclerView rvScrap;
    private ScrapGoldAdapter scrapAdapter;

    private ArrayList<GoldScrap> scrapDataArrayList;

    private SwipeRefreshLayout swipeRefresh;
    public ImageView imageView;
    public TextView tvDate;
    private FragmentActivity activity;

    public boolean run = true;

    public ProgressBar progressBar;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gold_rate, container, false);
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
        progressBar = view.findViewById(R.id.progressBarGoldRates);
        //endregion

        //region initializations
        activity = requireActivity();
        scrapDataArrayList = new ArrayList<>();
        rvScrap.setLayoutManager(new LinearLayoutManager(requireContext()));
        scrapAdapter = new ScrapGoldAdapter(scrapDataArrayList);
        rvScrap.setAdapter(scrapAdapter);
        //endregion

        //region swipe down handler
        swipeRefresh.setOnRefreshListener(this::doYourUpdate);
        //endregion

        //region Pakistan flag load
        Uri uri = Uri.parse(getString(R.string.pak_flag_url));
        if(!activity.isDestroyed()){
            GlideToVectorYou.justLoadImage(activity, uri, imageView);
        }

        //endregion

        fetchScrapData();
    }

    //region function that is used when swipe down to refresh list
    private void doYourUpdate() {
        if(haveNetwork(activity)){
            Call<String> stringCall = RetrofitClient.getInstance().getMyApi().getResponse(getString(R.string.gold_rate_url));

            stringCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        if(scrapDataArrayList != null){
                            scrapDataArrayList.clear();
                        }

                        String responseString = response.body();

                        if(responseString != null){
                            try{
                                Document doc = Jsoup.parse(responseString);
                                Elements table = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody");
                                int size = table.get(0).childrenSize();

                                Elements date = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type(1) > td > span");
                                String stringDate = date.get(0).childNodes().get(0).toString().trim();
                                tvDate.setText(stringDate.substring(6, 22));
                                tvDate.setVisibility(View.VISIBLE);

                                for(int i=3; i <= size; i++){
                                    Elements goldWeight = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(1)");
                                    Elements carat24 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(2)");
                                    Elements carat22 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(3)");
                                    Elements carat21 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(4)");
                                    Elements carat18 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(5)");

                                    GoldScrap scrapData = new GoldScrap();
                                    scrapData.setGoldWeight(goldWeight.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat24(carat24.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat22(carat22.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat21(carat21.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat18(carat18.get(0).childNodes().get(0).toString().trim());

                                    scrapDataArrayList.add(scrapData);
                                }
                                scrapDataArrayList.get(0).setGoldWeight("1 Tola Gold");
                                scrapDataArrayList.get(1).setGoldWeight("10 Gram Gold");
                                scrapDataArrayList.get(2).setGoldWeight("1 Gram Gold");
                                scrapDataArrayList.get(3).setGoldWeight("1 Ounce Gold");

                                if(scrapAdapter == null){
                                    scrapAdapter = new ScrapGoldAdapter(scrapDataArrayList);
                                    rvScrap.setAdapter(scrapAdapter);
                                }
                                scrapAdapter.notifyDataSetChanged();
                            }catch(Exception e){}
                        }
                    }

                    swipeRefresh.setRefreshing(false);// Disables the refresh icon
                }

                @Override
                public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                    // TODO implement a refresh
                    swipeRefresh.setRefreshing(false); // Disables the refresh icon

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

            Call<String> stringCall = RetrofitClient.getInstance().getMyApi().getResponse(getString(R.string.gold_rate_url));

            stringCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        String responseString = response.body();

                        if(responseString != null){
                            try{
                                Document doc = Jsoup.parse(responseString, "UTF-8");
                                Elements table = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody");
                                int size = table.get(0).childrenSize();

                                Elements date = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type(1) > td > span");
                                String stringDate = date.get(0).childNodes().get(0).toString().trim();
                                tvDate.setText(stringDate.substring(6, 22));
                                tvDate.setVisibility(View.VISIBLE);

                                for(int i=3; i <= size; i++){
                                    Elements goldWeight = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(1)");
                                    Elements carat24 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(2)");
                                    Elements carat22 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(3)");
                                    Elements carat21 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(4)");
                                    Elements carat18 = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td > table > tbody > tr:nth-of-type("+i+") > td:nth-of-type(5)");

                                    GoldScrap scrapData = new GoldScrap();
                                    scrapData.setGoldWeight(goldWeight.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat24(carat24.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat22(carat22.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat21(carat21.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setCarat18(carat18.get(0).childNodes().get(0).toString().trim());

                                    scrapDataArrayList.add(scrapData);
                                }
                                scrapDataArrayList.get(0).setGoldWeight("1 Tola Gold");
                                scrapDataArrayList.get(1).setGoldWeight("10 Gram Gold");
                                scrapDataArrayList.get(2).setGoldWeight("1 Gram Gold");
                                scrapDataArrayList.get(3).setGoldWeight("1 Ounce Gold");

                                if(scrapAdapter == null){
                                    scrapAdapter = new ScrapGoldAdapter(scrapDataArrayList);
                                    rvScrap.setAdapter(scrapAdapter);
                                }
                                scrapAdapter.notifyDataSetChanged();
                            }catch(Exception e){}
                        }
                    }

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

            if(!run){
                if(activity != null){
                    new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Oops...")
                            .setContentText("No Internet Connection!")
                            .show();
                }
            }
            run = false;
        }
    }
    //endregion

    //region option menu for sorting
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.sort_menu_converter).setVisible(false);
        menu.findItem(R.id.sort_menu_rate).setVisible(false);
        menu.findItem(R.id.notify).setVisible(false);
        menu.findItem(R.id.btnNotify).setVisible(false);
        menu.findItem(R.id.btnNotification).setVisible(false);
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