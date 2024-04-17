package com.atrule.currencyconverter.classes;

public class MyCurrency {
    //region member variables
    String code;
    int flag;
    //endregion

    //region constructor
    public MyCurrency() {
    }
    //endregion

    //region getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
    //endregion
}
