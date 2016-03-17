package com.vinaya.blecentralrole.logic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.vinaya.blecentralrole.logic.scanner.BLEScanner;
import com.vinaya.blecentralrole.logic.scanner.BLEScannerV21;
import com.vinaya.blecentralrole.model.Peripheral;
import com.vinaya.blecentralrole.model.UUIDRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 	Main logic of the whole application.
 *
 */
public class Central {

	//--------------------------------------------------
	//data members
	private Context context;
	private UUIDRepository uuidRepository;
	private AtomicInteger recieveCounter;

	private List<Peripheral> peripheralList;
	private BLEScanner.ScanTask scanTask;

	//--------------------------------------------------
	//listener class definition

	public interface ScanListener {
		void onScanned(List<Peripheral> peripheralList);
		void onFailed();
		void onBluetoothNotEnabled();
	}

	public interface OnRecievdListener {
		void onRecieved(Peripheral peripheral, String data);
	}

	public interface ConnectListener {
		void onConnected(Peripheral peripheral);
		void onDisconnected(Peripheral peripheral, int errorCode);
	}



	//--------------------------------------------------
	//class methods
	public Central(Context context, UUIDRepository uuidRepository) {
		this.context = context;
		this.uuidRepository = uuidRepository;
		this.peripheralList = Collections.synchronizedList(new ArrayList());
	}


	/**
	 * feature 1: scan for BLE peripherals around you
	 *
	 * @param listener what to do after scanned or failed
	 */
	public void scan(final ScanListener listener) {
		stop();

		final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

		if (bluetoothAdapter == null) {
			listener.onFailed();
			return;
		}

		if (!bluetoothAdapter.isEnabled()) {
			listener.onBluetoothNotEnabled();
			return;
		}

		final BLEScanner scanner = new BLEScannerV21(bluetoothAdapter);
		this.scanTask = scanner.asyncScan(new BLEScanner.BLEScanListener() {
			@Override
			public void onDiscovered(Peripheral peripheral) {
				if (peripheral == null) return;
				if (peripheralList.contains(peripheral)) return;
				peripheralList.add(peripheral);
				listener.onScanned(peripheralList);
			}

			@Override
			public void onFailed(int errorCOde) {
				listener.onFailed();
			}
		});

	}

	public boolean canConnect(Peripheral peripheral) {
		return true;
	}

	public void connect(Peripheral peripheral, ConnectListener connectListener) {

		if (connectListener != null) {
			connectListener.onConnected(peripheral);
		}

	}

	public void stop() {
		if (scanTask != null) {
			scanTask.stop();
		}
	}

}

