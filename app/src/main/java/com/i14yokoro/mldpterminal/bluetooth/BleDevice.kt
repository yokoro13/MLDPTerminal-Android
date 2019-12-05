package com.i14yokoro.mldpterminal.bluetooth

/**
 * デバイスを表すクラス
 */
internal class BleDevice(val address: String?, val name: String?) {

    override fun toString(): String {
        return "Name: $name   --address: $address"
    }

    override fun equals(other: Any?): Boolean {
        return if (other is BleDevice) {
            this.address == other.address
        } else false
    }

    override fun hashCode(): Int {
        return this.address.hashCode()
    }

}

