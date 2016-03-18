package com.vinaya.blecentralrole.logic.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import com.vinaya.blecentralrole.model.Peripheral;

import java.util.List;

/**
 * BLEScanner support Android API Version >= 21
 */
public class BLEScannerV21 implements BLEScanner {

	private BluetoothAdapter adapter;
	private BluetoothLeScanner leScanner;

	public BLEScannerV21(BluetoothAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public ScanTask asyncScan(final BLEScanListener listener) {
		//achieve a bluetoothLehScanner for scanning bluetooth devices
		this.leScanner = adapter.getBluetoothLeScanner();
		if (leScanner == null) {
			listener.onFailed(-1);
			return NULL_SCAN_HANDLER;
		}

		final ScanCallback callback = new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				Log.i("callbackType", String.valueOf(callbackType));
				Log.i("result", result.toString());
				listener.onDiscovered(new Peripheral(result));
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				for (ScanResult result : results) {
					Log.i("ScanResult - Results", result.toString());
					listener.onDiscovered(new Peripheral(result));
				}
			}

			@Override
			public void onScanFailed(int errorCode) {
				Log.e("Scan Failed", "Error Code: " + errorCode);
				listener.onFailed(errorCode);
			}
		};

		leScanner.startScan(callback);


		return new ScanTask() {
			@Override
			public void stop() {
				if (leScanner == null) return;
				if (adapter.isEnabled() == false) return;
				leScanner.stopScan(callback);
			}
		};
	}


	/**
	 * use of NullObject design pattern
	 */
	private final static ScanTask NULL_SCAN_HANDLER = new ScanTask() {
		@Override
		public void stop() {
			//do nothing
		}
	};
}
