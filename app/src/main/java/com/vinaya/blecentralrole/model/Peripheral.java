package com.vinaya.blecentralrole.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import java.util.UUID;

/**
 * Representation of BLE Peripheral
 */
public class Peripheral extends BluetoothScanInfo {
	private UUID uuid;

	public Peripheral(ScanResult scanResult) {
		super.device = scanResult.getDevice();
		super.rssi = scanResult.getRssi();
	}

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (o == null) return false;
		if (o instanceof Peripheral == false) return false;

		Peripheral p = (Peripheral)o;
		return getName().equals(p.getName());
	}

	public String getName() {
		return device.getName();
	}

	public String getAddress() {
		return device.getAddress();
	}

	public UUID getUUID() {
		return this.uuid;
	}
}

