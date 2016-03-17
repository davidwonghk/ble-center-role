package com.vinaya.blecentralrole.model;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representation of BLE Peripheral
 */
public class Peripheral extends BluetoothScanInfo {

	private List<UUID> serviceUUIDs;

	public Peripheral(ScanResult scanResult) {
		if (scanResult == null) return;

		super.device = scanResult.getDevice();
		super.rssi = scanResult.getRssi();

		this.serviceUUIDs = new ArrayList<>();
		initServiceUUIDs(scanResult);

	}

	private void initServiceUUIDs(ScanResult scanResult) {
		ScanRecord scanRecord = scanResult.getScanRecord();
		if (scanRecord == null) return;

		List<ParcelUuid> ary = scanRecord.getServiceUuids();
		if (ary == null) return;

		for(ParcelUuid pid : scanRecord.getServiceUuids()) {
			serviceUUIDs.add(pid.getUuid());
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (o == null) return false;
		if (o instanceof Peripheral == false) return false;

		Peripheral p = (Peripheral)o;
		return getAddress().equals(p.getAddress());
	}

	public int getRssi() {
		return super.rssi;
	}

	public String getName() {
		return device.getName();
	}

	public String getAddress() {
		return device.getAddress();
	}

	public List<UUID> getServiceUUIDs() {
		return this.serviceUUIDs;
	}
}

