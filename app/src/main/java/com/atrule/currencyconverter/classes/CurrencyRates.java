package com.atrule.currencyconverter.classes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CurrencyRates {

    @SerializedName("valiutos")
    @Expose
    private String valiutos;
    @SerializedName("valiuta")
    @Expose
    private String valiuta;
    @SerializedName("sarasas")
    @Expose
    private String sarasas;
    @SerializedName("ivestis")
    @Expose
    private String ivestis;

    public String getValiutos() {
        return valiutos;
    }

    public void setValiutos(String valiutos) {
        this.valiutos = valiutos;
    }

    public String getValiuta() {
        return valiuta;
    }

    public void setValiuta(String valiuta) {
        this.valiuta = valiuta;
    }

    public String getSarasas() {
        return sarasas;
    }

    public void setSarasas(String sarasas) {
        this.sarasas = sarasas;
    }

    public String getIvestis() {
        return ivestis;
    }

    public void setIvestis(String ivestis) {
        this.ivestis = ivestis;
    }

}
