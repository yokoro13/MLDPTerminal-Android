package com.i14yokoro.tecterminal;

class BleDevice {
    private String address;
    private String name;

    public BleDevice(String a, String n) {
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
        if (object != null && object instanceof BleDevice) {
            if (this.address.equals(((BleDevice) object).address)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public String toString(){
        String str = "";
        str = "Name: " + this.name  + "   --address: " + this.address;
        return str;
    }

}

