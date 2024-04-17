package com.atrule.currencyconverter.fragments;

import static android.content.Context.MODE_PRIVATE;
import static com.atrule.currencyconverter.util.MyUtil.haveNetwork;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.atrule.currencyconverter.adapters.CurrencyAdapter;
import com.atrule.currencyconverter.api.RetrofitClient;
import com.atrule.currencyconverter.classes.Country;
import com.atrule.currencyconverter.classes.CurrencyRates;
import com.atrule.currencyconverter.classes.MyCurrency;
import com.google.gson.Gson;
import com.mynameismidori.currencypicker.ExtendedCurrency;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConverterFragment extends Fragment implements CurrencyAdapter.CurrencyListener {
    //region variable declarations
    private final String CURRENCY_NAME_URL = "";
    private final NumberFormat formatter = new DecimalFormat("###.##");
    private final NumberFormat formatterForPercent = new DecimalFormat("###.###");

    private List<String> currencyCodes;
    private ArrayList<Country> countries;
    private ArrayList<Country> sortedCountries;
    private List<String> sortedCountriesNames;
    private List<ExtendedCurrency> currencies;
    private List<MyCurrency> myCurrencies;

    private RecyclerView rvCountry;
    private CurrencyAdapter currencyAdapter;
    private HashMap<String, Double> jsonRates, jsonRatesUsdOne;
    JSONObject previousRatesAtomic;

    private MenuItem menuItem;

    private SharedPreferences sharedpreferences;
    private SharedPreferences.Editor editor;
    private TextView textView2;
    private EditText etTop;

    private SwipeRefreshLayout swipeRefresh;

    private String currCode = "USD";
    private double currAmount = 1.0;

    int sort = -1;

    public boolean run = true;

    private FragmentActivity activity;
    private Context context;

    public ProgressBar progressBar;
    //endregion

    //region onCreate function
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    //endregion

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_convert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = requireActivity();
        context = requireContext();

        //region finding by ids
        rvCountry = view.findViewById(R.id.rvCountry);
        rvCountry.setLayoutManager(new LinearLayoutManager(context));
        etTop = view.findViewById(R.id.textView6);
        textView2 = view.findViewById(R.id.textView2);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBarConverter);
        //endregion

        //region initializations
        currencyCodes = Arrays.asList(getResources().getStringArray(R.array.limited_currency_codes));
        currencies = ExtendedCurrency.getAllCurrencies();
        myCurrencies = new ArrayList<>();
        countries = new ArrayList<>();
        sortedCountries = new ArrayList<>();
        sortedCountriesNames = new ArrayList<>();
        sharedpreferences = context.getSharedPreferences("PinCities", MODE_PRIVATE);
        editor = sharedpreferences.edit();
        sortedCountriesNames = Arrays.asList(getResources().getStringArray(R.array.sorted_currency_names));
        //endregion

        //region getting flags related to currency codes and making an array list
        Collections.sort(currencyCodes);
        for (int i = 0; i < currencyCodes.size(); i++) {
            for (int j = 0; j < currencies.size(); j++) {
                if (currencies.get(j).getCode().equals(currencyCodes.get(i))) {
                    MyCurrency myCurrency = new MyCurrency();
                    myCurrency.setCode(currencies.get(j).getCode());
                    myCurrency.setFlag(currencies.get(j).getFlag());
                    myCurrencies.add(myCurrency);
                }
            }
        }
        //endregion

        //region currency amount click handler
        fetchData(currCode, currAmount);
        etTop.setText(formatter.format(currAmount));
        textView2.setText(currCode);
        //endregion

        //region swipe down handler
        swipeRefresh.setOnRefreshListener(this::doYourUpdate);
        //endregion

        //region currency amount click handler
        etTop.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!etTop.getText().toString().equals("")){
                    try {
                        if(Double.parseDouble(etTop.getText().toString()) != currAmount){
                            if (haveNetwork(activity)) {
                                double amount = Double.parseDouble(etTop.getText().toString());
                                currAmount = amount;
                                fetchData(currCode, amount);
                            } else {
                                etTop.setText(formatter.format(currAmount));
                                if(activity != null){
                                    new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                            .setTitleText("Oops...")
                                            .setContentText("No Internet Connection!")
                                            .show();
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        if(activity != null && context != null){
                            Toast.makeText(activity, context.getResources().getString(R.string.number_format_exception), Toast.LENGTH_SHORT).show();
                        }

                        etTop.setText(formatter.format(currAmount));
                    }
                }
                else{
                    etTop.setText(formatter.format(currAmount));
                }
                etTop.clearFocus();
                return true;
            }
            return false;
        });

        etTop.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v, activity);
                etTop.setText(formatter.format(currAmount));
            }
        });

//        textView6.setOnClickListener(v -> {
//            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle);
//            builder.setTitle("");
//
//            final EditText input = new EditText(context);
//            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
//            input.setHint(formatter.format(currAmount));
//            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
//            input.setHintTextColor(getResources().getColor(R.color.lightGreen));
//            input.setTextColor(getResources().getColor(R.color.darkGreen));
//            FrameLayout container1 = new FrameLayout(activity);
//            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//            params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
//            params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
//            params.topMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
//            params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
//            input.setLayoutParams(params);
//            input.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDarkGrey)));
//            container1.addView(input);
//            builder.setView(container1);
//
//            builder.setPositiveButton("OK", (dialog, which) -> {
//                if (haveNetwork(activity)) {
//                    if (!input.getText().toString().equals("")) {
//                        double amount = Double.parseDouble(input.getText().toString());
//                        currAmount = amount;
//                        fetchData(currCode, amount);
//                    } else {
//                        dialog.dismiss();
//                    }
//                } else {
//                    if(activity != null){
//                        new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
//                                .setTitleText("Oops...")
//                                .setContentText("No Internet Connection!")
//                                .show();
//                    }
//                }
//            });
//            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//
//            AlertDialog alertDialog = builder.create();
//            alertDialog.setOnShowListener(dialog -> {
//                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
//                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
//            });
//
//            alertDialog.show();
//        });
        //endregion
    }

    //region swipe down function
    public void doYourUpdate() {
        if(haveNetwork(activity)){
            fetchData(currCode, currAmount);
            etTop.setText(formatter.format(currAmount));
            textView2.setText(currCode);
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
        swipeRefresh.setRefreshing(false);
    }
    //endregion

    //region function to get currency rates from url
    public void fetchData(String currencyCode, double amount) {
        if (haveNetwork(activity)) {
            showProgress();
            String rateURL = "" + currencyCode.toLowerCase() + "" + formatter.format(amount) + "";
            Call<String> stringCall = RetrofitClient.getInstance().getMyApi().getResponse(rateURL);
            stringCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        String responseString = response.body();
                        if(responseString != null){
                            try {
                                String json = responseString.substring(responseString.indexOf("(") + 1, responseString.indexOf(")"));
                                Gson gson = new Gson();
                                CurrencyRates currencyRates = gson.fromJson(json, CurrencyRates.class);
                                String str = currencyRates.getValiutos();
                                jsonRates = new HashMap<>();
                                jsonRates = (HashMap<String, Double>) Arrays.stream(str.split(";")).map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> Double.parseDouble(e[1])));

                                sharedpreferences = activity.getSharedPreferences("PinCities", MODE_PRIVATE);

                                Call<String> stringCallAnother = RetrofitClient.getInstance().getMyApi().getResponse(CURRENCY_NAME_URL);
                                stringCallAnother.enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                                        if(response.isSuccessful()){
                                            String responseString = response.body();
                                            if(responseString != null){
                                                if (!countries.isEmpty()) {
                                                    countries.clear();
                                                }
                                                if (!sortedCountries.isEmpty()) {
                                                    sortedCountries.clear();
                                                }
                                                JSONArray jsonArray;
                                                try {
                                                    jsonArray = new JSONArray(responseString);

                                                    for (int i = 0; i < jsonArray.length(); i++) {
                                                        JSONObject mainObject = jsonArray.getJSONObject(i);
                                                        JSONArray flags = mainObject.getJSONArray("flags");
                                                        JSONObject nameJson = mainObject.getJSONObject("name");
                                                        if(mainObject.has("currencies")){
                                                            JSONObject currenciesJson = mainObject.getJSONObject("currencies");
                                                            Iterator<String> keysItr = currenciesJson.keys();
                                                            String key = "";
                                                            while(keysItr.hasNext()) {
                                                                key = keysItr.next();
                                                            }
                                                            JSONObject currencyJson = currenciesJson.getJSONObject(key);
                                                            if(currencyJson.has("symbol")){
                                                                Country country = new Country();
                                                                country.setName(nameJson.getString("common"));
                                                                country.setAlpha2Code(mainObject.getString("cca2"));
                                                                country.setAlpha3Code(mainObject.getString("cca3"));
                                                                country.setFlag(flags.get(0).toString());
                                                                country.setCode(key);
                                                                country.setSymbol(currencyJson.getString("symbol"));
                                                                country.setPercent(0.0);
                                                                country.setIncreaseDecrease(0);

                                                                if (sharedpreferences.contains(country.getName())) {
                                                                    country.setChecked(sharedpreferences.getBoolean(country.getName(), false));
                                                                }

                                                                if (jsonRates.containsKey(country.getCode().toLowerCase())) {
                                                                    String s = formatter.format(jsonRates.get(country.getCode().toLowerCase()));
                                                                    country.setRate(s);
                                                                    if (currencyCodes.contains(country.getCode())) {
                                                                        countries.add(country);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    for (int j = 0; j < sortedCountriesNames.size(); j++) {
                                                        for (int k = 0; k < countries.size(); k++) {
                                                            if (countries.get(k).getName().equals(sortedCountriesNames.get(j))) {
                                                                sortedCountries.add(countries.get(k));
                                                            }
                                                        }
                                                    }

                                                    for (int n = 0; n < sortedCountries.size(); n++) {
                                                        if (sharedpreferences.contains(sortedCountries.get(n).getName())) {
                                                            sortedCountries.get(n).setChecked(sharedpreferences.getBoolean(sortedCountries.get(n).getName(), false));
                                                        }
                                                    }

                                                    //region sorting as previous
                                                    switch (sort) {
                                                        case 1:
                                                            Collections.sort(sortedCountries, (o1, o2) -> Boolean.compare(o2.isChecked, o1.isChecked));
                                                            break;
                                                        case 2:
                                                            ArrayList<Country> temp = new ArrayList<>();
                                                            for (int j = 0; j < sortedCountriesNames.size(); j++) {
                                                                for (int k = 0; k < sortedCountries.size(); k++) {
                                                                    if (sortedCountries.get(k).getName().equals(sortedCountriesNames.get(j))) {
                                                                        temp.add(sortedCountries.get(k));
                                                                    }
                                                                }
                                                            }
                                                            for (int l = 0; l < temp.size(); l++) {
                                                                sortedCountries.set(l, temp.get(l));
                                                            }
                                                            break;
                                                        case 3:
                                                            Collections.sort(sortedCountries, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                                                            break;
                                                        case 4:
                                                            Collections.sort(sortedCountries, (o1, o2) -> o2.getName().compareToIgnoreCase(o1.getName()));
                                                            break;
                                                        default:
                                                            Collections.sort(sortedCountries, (o1, o2) -> Boolean.compare(o2.isChecked, o1.isChecked));
                                                            break;
                                                    }
                                                    //endregion

                                                    if (currencyAdapter == null) {
                                                        currencyAdapter = new CurrencyAdapter(context, sortedCountries, activity, menuItem, ConverterFragment.this);
                                                        rvCountry.setAdapter(currencyAdapter);
                                                    } else {
                                                        currencyAdapter.setMenuItem(menuItem);
                                                        currencyAdapter.notifyDataSetChanged();
                                                    }

                                                    etTop.setText(formatter.format(currAmount));
                                                    textView2.setText(currCode);

                                                    if(activity != null){
                                                        change();
                                                    }
                                                    else{
                                                        hideProgress();
                                                    }
                                                }
                                                catch (JSONException e) {
                                                    hideProgress();
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                                        hideProgress();
                                        etTop.setText(formatter.format(currAmount));
                                        textView2.setText(currCode);

                                        if(activity != null && context != null){
                                            Toast.makeText(activity, context.getResources().getString(R.string.unable_to_load), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                            catch (Exception e) {
                                hideProgress();
                                etTop.setText(formatter.format(currAmount));
                                textView2.setText(currCode);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                    hideProgress();

                    if(activity != null && context != null){
                        Toast.makeText(activity, context.getResources().getString(R.string.unable_to_load), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        else {
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
        menu.findItem(R.id.sort_menu_converter).setVisible(true);
        menu.findItem(R.id.sort_menu_rate).setVisible(false);
        menu.findItem(R.id.notify).setVisible(true);
        menu.findItem(R.id.btnNotify).setVisible(true);
        menu.findItem(R.id.btnNotification).setVisible(false);
        menuItem = menu.findItem(R.id.notify);

        if (currencyAdapter != null) {
            currencyAdapter.setMenuItem(menuItem);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!sortedCountries.isEmpty()) {
            int id = item.getItemId();

            if(id == R.id.pin){
                sort = 1;
                Collections.sort(sortedCountries, (o1, o2) -> Boolean.compare(o2.isChecked, o1.isChecked));
            }
            else if(id == R.id.MOST){
                sort = 2;
                ArrayList<Country> temp = new ArrayList<>();
                for (int j = 0; j < sortedCountriesNames.size(); j++) {
                    for (int k = 0; k < sortedCountries.size(); k++) {
                        if (sortedCountries.get(k).getName().equals(sortedCountriesNames.get(j))) {
                            temp.add(sortedCountries.get(k));
                        }
                    }
                }
                for (int l = 0; l < temp.size(); l++) {
                    sortedCountries.set(l, temp.get(l));
                }
            }
            else if(id == R.id.atz){
                sort = 3;
                Collections.sort(sortedCountries, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            }
            else if(id == R.id.zta){
                sort = 4;
                Collections.sort(sortedCountries, (o1, o2) -> o2.getName().compareToIgnoreCase(o1.getName()));
            }
            currencyAdapter.notifyDataSetChanged();

        } else {
            if(activity != null){
                Toast.makeText(activity, "There is no data.", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }
    //endregion

    //region currency change listener
    @Override
    public void onCurrencyChange(String currencyCode, String currencyAmount) {
        try {
            if (!currencyAmount.equals("")) {
                currAmount = Double.parseDouble(currencyAmount);
            } else {
                currAmount = 1.0;
            }
            currCode = currencyCode;

            fetchData(currCode, currAmount);
        } catch (NumberFormatException e) {
            if(activity != null && context != null){
                Toast.makeText(activity, context.getResources().getString(R.string.number_format_exception), Toast.LENGTH_SHORT).show();
            }
        }
    }
    //endregion

    //region percentage calculate function
    public static double getPercentage(double currentValue, double previousValue) {
        double difference, divideResult;
        if (currentValue > previousValue) {
            difference = currentValue - previousValue;
            divideResult = difference / previousValue;
            return divideResult * 100;
        } else if (currentValue < previousValue) {
            difference = previousValue - currentValue;
            divideResult = difference / previousValue;
            return divideResult * 100;
        }
        return 0.0;
    }
    //endregion

    //region change in rate function by date
    public void change() {
        SharedPreferences sp = activity.getSharedPreferences("PreviousStoredRates", MODE_PRIVATE);
        SharedPreferences.Editor edt = sp.edit();

        if (sp.contains("date")) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            try {
                Date previousDate = sdf.parse(sp.getString("date", ""));
                Date currentDate = new Date();

                String previousDateString = sdf.format(previousDate.getTime());
                String currentDateString = sdf.format(currentDate.getTime());

                if (!currentDateString.equals(previousDateString)) {
                    String rateURL = "";

                    Call<String> stringCall13 = RetrofitClient.getInstance().getMyApi().getResponse(rateURL);
                    stringCall13.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                            if(response.isSuccessful()){
                                String responseString = response.body();
                                if(responseString != null){
                                    try {
                                        String json = responseString.substring(responseString.indexOf("(") + 1, responseString.indexOf(")"));
                                        Gson gson = new Gson();
                                        CurrencyRates currencyRates = gson.fromJson(json, CurrencyRates.class);
                                        String str = currencyRates.getValiutos();
                                        jsonRatesUsdOne = new HashMap<>();
                                        jsonRatesUsdOne = (HashMap<String, Double>) Arrays.stream(str.split(";")).map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> Double.parseDouble(e[1])));

                                        String previousRate = "";
                                        if (sp.contains("stored_rates")) {
                                            String rate = sp.getString("stored_rates", "");
                                            previousRate = rate;
                                            try {
                                                previousRatesAtomic = new JSONObject(rate);

                                                double currRate, prevRate;
                                                for (int i = 0; i < sortedCountries.size(); i++) {
                                                    if (!sortedCountries.get(i).getCode().equalsIgnoreCase("usd")) {
                                                        currRate = jsonRatesUsdOne.get(sortedCountries.get(i).getCode().toLowerCase());
                                                        prevRate = (double) previousRatesAtomic.get(sortedCountries.get(i).getCode().toLowerCase());

                                                        if (currRate != prevRate) {
                                                            if (currRate > prevRate) {
                                                                sortedCountries.get(i).setIncreaseDecrease(1);
                                                                sortedCountries.get(i).setPercent(Double.parseDouble(formatterForPercent.format(getPercentage(currRate, prevRate))));
                                                            } else {
                                                                sortedCountries.get(i).setIncreaseDecrease(-1);
                                                                sortedCountries.get(i).setPercent(Double.parseDouble(formatterForPercent.format(getPercentage(currRate, prevRate))));
                                                            }
                                                        } else {
                                                            sortedCountries.get(i).setIncreaseDecrease(0);
                                                            sortedCountries.get(i).setPercent(0.0);
                                                        }
                                                    }
                                                }

                                                currencyAdapter.notifyDataSetChanged();

                                                hideProgress();
                                            } catch (JSONException e) {
                                                hideProgress();
                                                e.printStackTrace();
                                            }
                                        }

                                        edt.putString("stored_rates", jsonRatesUsdOne.toString());
                                        edt.putString("previous_stored_rates", previousRate);
                                        edt.putString("date", sdf.format(currentDate.getTime()));
                                        edt.apply();

                                        hideProgress();
                                    }
                                    catch (Exception e) {
                                        hideProgress();
                                    }
                                }
                            }
                            hideProgress();
                        }

                        @Override
                        public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                            hideProgress();

                            if(activity != null){
                                Toast.makeText(activity, context.getResources().getString(R.string.unable_to_load), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                else {
                    String rateURL = "";

                    Call<String> stringCall12 = RetrofitClient.getInstance().getMyApi().getResponse(rateURL);
                    stringCall12.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                            if(response.isSuccessful()){
                                String responseString = response.body();
                                if(responseString != null){
                                    try {
                                        String json = responseString.substring(responseString.indexOf("(") + 1, responseString.indexOf(")"));
                                        Gson gson = new Gson();
                                        CurrencyRates currencyRates = gson.fromJson(json, CurrencyRates.class);
                                        String str = currencyRates.getValiutos();
                                        jsonRatesUsdOne = new HashMap<>();
                                        jsonRatesUsdOne = (HashMap<String, Double>) Arrays.stream(str.split(";")).map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> Double.parseDouble(e[1])));

                                        if (sp.contains("previous_stored_rates")) {
                                            String rate = sp.getString("previous_stored_rates", "");
                                            try {
                                                previousRatesAtomic = new JSONObject(rate);

                                                double currRate, prevRate;
                                                for (int i = 0; i < sortedCountries.size(); i++) {
                                                    if (!sortedCountries.get(i).getCode().equalsIgnoreCase("usd")) {
                                                        currRate = jsonRatesUsdOne.get(sortedCountries.get(i).getCode().toLowerCase());
                                                        prevRate = (double) previousRatesAtomic.get(sortedCountries.get(i).getCode().toLowerCase());

                                                        if (currRate != prevRate) {
                                                            if (currRate > prevRate) {
                                                                sortedCountries.get(i).setIncreaseDecrease(1);
                                                                sortedCountries.get(i).setPercent(Double.parseDouble(formatterForPercent.format(getPercentage(currRate, prevRate))));
                                                            } else {
                                                                sortedCountries.get(i).setIncreaseDecrease(-1);
                                                                sortedCountries.get(i).setPercent(Double.parseDouble(formatterForPercent.format(getPercentage(currRate, prevRate))));
                                                            }
                                                        } else {
                                                            sortedCountries.get(i).setIncreaseDecrease(0);
                                                            sortedCountries.get(i).setPercent(0.0);
                                                        }
                                                    }
                                                }

                                                currencyAdapter.notifyDataSetChanged();

                                                hideProgress();
                                            } catch (JSONException e) {
                                                hideProgress();
                                                e.printStackTrace();
                                            }
                                        }

                                        hideProgress();
                                    }
                                    catch (Exception e) {
                                        hideProgress();
                                    }
                                }
                            }
                            hideProgress();
                        }

                        @Override
                        public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                            hideProgress();

                            if(activity != null){
                                Toast.makeText(activity, context.getResources().getString(R.string.unable_to_load), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            } catch (ParseException e) {
                hideProgress();
                e.printStackTrace();
            }
        }
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date date = new Date();
            String dateString = sdf.format(date.getTime());

            String rateURL = "";
            Call<String> stringCall11 = RetrofitClient.getInstance().getMyApi().getResponse(rateURL);
            stringCall11.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        String responseString = response.body();
                        if(responseString != null){
                            try {
                                String json = responseString.substring(responseString.indexOf("(") + 1, responseString.indexOf(")"));
                                Gson gson = new Gson();
                                CurrencyRates currencyRates = gson.fromJson(json, CurrencyRates.class);
                                String str = currencyRates.getValiutos();
                                jsonRatesUsdOne = new HashMap<>();
                                jsonRatesUsdOne = (HashMap<String, Double>) Arrays.stream(str.split(";")).map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> Double.parseDouble(e[1])));

                                edt.putString("stored_rates", jsonRatesUsdOne.toString());
                                edt.putString("previous_stored_rates", jsonRatesUsdOne.toString());
                                edt.putString("date", dateString);
                                edt.apply();

                                hideProgress();
                            }
                            catch (Exception e) {
                                hideProgress();
                            }
                        }
                    }
                    hideProgress();
                }

                @Override
                public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                    hideProgress();

                    if(activity != null){
                        Toast.makeText(activity, context.getResources().getString(R.string.unable_to_load), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    //endregion

    //region Progress Handling Functions
    public void showProgress(){
        progressBar.setVisibility(View.VISIBLE);
        rvCountry.setVisibility(View.GONE);
    }
    public void hideProgress(){
        progressBar.setVisibility(View.GONE);
        rvCountry.setVisibility(View.VISIBLE);
    }
    //endregion

    //region function to hide keyboard
    public void hideKeyboard(View view, Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    //endregion
}