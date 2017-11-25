package com.movesense.mds.sampleapp.example_app_using_mds_api.model;


import com.movesense.mds.sampleapp.model.MdsAddressModel;

import java.util.List;

public class MovesenseDevice {

    private String name;
    private String serial;
    private String swVersion;
    private String macAddress;
    private final List<MdsAddressModel> addressList;

    public MovesenseDevice(String name, String serial, String swVersion, String macAddress, List<MdsAddressModel> addressList) {
        this.name = name;
        this.serial = serial;
        this.swVersion = swVersion;
        this.macAddress = macAddress;
        this.addressList = addressList;
    }

    public String getName() {
        return name;
    }

    public String getSerial() {
        return serial;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public List<MdsAddressModel> getAddressList() {
        return addressList;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
