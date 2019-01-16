package com.i14yokoro.tecterminal;

import android.support.annotation.NonNull;

class BleDevice {
    private String address;
    private String name;

    BleDevice(String a, String n) {
        address = a;
        name = n;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof BleDevice) {
            return this.address.equals(((BleDevice) object).address);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @NonNull
    @Override
    public String toString(){
        String str;
        str = "Name: " + this.name  + "   --address: " + this.address;
        return str;
    }

}

