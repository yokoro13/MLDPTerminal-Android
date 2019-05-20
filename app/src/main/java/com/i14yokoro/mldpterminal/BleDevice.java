package com.i14yokoro.mldpterminal;

import android.support.annotation.NonNull;

class BleDevice {
    private String address;
    private String name;

    BleDevice(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString(){
        String str;
        str = "Name: " + this.name  + "   --address: " + this.address;
        return str;
    }

}

