package com.vinaya.blecentralrole.logic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.vinaya.blecentralrole.R;
import com.vinaya.blecentralrole.logic.scanner.BLEScanner;
import com.vinaya.blecentralrole.logic.scanner.BLEScannerV21;
import com.vinaya.blecentralrole.model.Peripheral;
import com.vinaya.blecentralrole.model.UUIDRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main logic of the whole application.
 */
public class Central {
	private final static String TAG = "Central";	//for logging use

	//--------------------------------------------------
	//data members
	private Context context;
	private UUIDRepository uuidRepository;
	private AtomicInteger recieveCounter;

	private List<Peripheral> peripheralList;
	private BluetoothAdapter bluetoothAdapter;

	private BLEScanner.ScanTask scanTask;

	private BluetoothGatt gatt;
	private boolean isManuallyDisconnect = false;

	//--------------------------------------------------
	//listener class definition

	public interface ScanListener {
		void onScanned(List<Peripheral> peripheralList);
		void onFailed(int errorCode);
	}

	public interface ConnectListener {
		void onConnected(Peripheral peripheral);

		/**react when
		 *
		 * @param peripheral connected peripheral
		 * @param isManually is the disconnect triggered by us manually?
		 */
		void onDisconnected(Peripheral peripheral, boolean isManually);

		/**
		 * react on what received from the subscribe characterics
		 * @param peripheral connected peripheral
		 * @param data received data, assumed to be string
		 */
		void onReceived(Peripheral peripheral, String data);

		void onConnectFail();
	}


	//--------------------------------------------------
	//class methods
	public Central(Context context, UUIDRepository uuidRepository) {
		this.context = context;
		this.uuidRepository = uuidRepository;
		this.peripheralList = Collections.synchronizedList(new ArrayList());
		this.recieveCounter = new AtomicInteger(0);
	}


	/**
	 *  check if bluetooth is on and preform initialization
	 */
	public boolean checkAndStart() {
		final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		this.bluetoothAdapter = bluetoothManager.getAdapter();

		if (bluetoothAdapter == null) { return false; }
		if (!bluetoothAdapter.isEnabled()) { return false; }

		return true;
	}


	/**
	 * feature 1: scan for BLE peripherals around you
	 *
	 * @param listener what to do after scanned or failed
	 */
	public void scan(final ScanListener listener) {
		stopScan();

		//if bluetoothAdapter is null, bluetooth seems to be not supported

		//TODO: better inject a scanner instead of contruct here
		//TODO: implement a BLEScanner support android API < 21
		final BLEScanner scanner = new BLEScannerV21(bluetoothAdapter);
		this.scanTask = scanner.asyncScan(new BLEScanner.BLEScanListener() {
			@Override
			public void onDiscovered(Peripheral peripheral) {
				if (peripheral == null) return;

				int index = peripheralList.indexOf(peripheral);
				if (index == -1) {
					//avoid duplication
					peripheralList.add(peripheral);
				} else {
					//updated the existing peripherals
					peripheralList.set(index, peripheral);
				}

				listener.onScanned(peripheralList);
			}

			@Override
			public void onFailed(int errorCode) {
				listener.onFailed(errorCode);
			}
		});

	}

	/**
	 * check if the peripheral is enabled by the application
	 */
	public boolean canConnect(Peripheral peripheral) {
		return peripheral.getServiceUUIDs().contains(uuidRepository.getServiceID());
	}

	/**
	 * feature 4: Allow user to connect to peripherals with Service
	 *
	 * @param peripheral the BLE Device
	 * @param listener   callbacks about what to do upon connection/disconnect
	 */
	public void connect(final Peripheral peripheral, final ConnectListener listener) {

		Log.d(TAG, "Trying to reconnect.");
		if (bluetoothAdapter == null) {
			Log.e(TAG, "BluetoothAdapter not initialized.");
			listener.onConnectFail();
			return;
		}

		final String address = peripheral.getAddress();
		final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			Log.e(TAG, "Device not found.  Unable to connect.");
			listener.onConnectFail();
			return;
		}

		disconnect();

		// We want to directly connect to the device,
		// so we are setting the autoConnect parameter to false.
		this.gatt = device.connectGatt(context, false, new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
				super.onConnectionStateChange(gatt, status, newState);

				switch (newState) {
					case BluetoothProfile.STATE_CONNECTED:
						isManuallyDisconnect = false;
						if (gatt.discoverServices()) {
							listener.onConnected(peripheral);
							peripheral.setConnected(true);
						} else {
							listener.onConnectFail();
						}
						return;

					case BluetoothProfile.STATE_DISCONNECTED:
						peripheral.setConnected(false);

						listener.onDisconnected(peripheral, isManuallyDisconnect);

						//feature 7: Automatically Reconnect if there is a cause of disconnection other
						// than the intentional disconnection performed by the user.
						if (false == isManuallyDisconnect) {
							Log.i(TAG, "Automatically Reconnect");
							gatt.connect();
						}
						return;
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				super.onServicesDiscovered(gatt, status);

				if (status != BluetoothGatt.GATT_SUCCESS) {
					Log.w("Central", "onServicesDiscovered received: " + status);
					return;
				}


				BluetoothGattService service = gatt.getService(uuidRepository.getServiceID());

				//feature 5a: Discover TX Characteristic and RX Characteristic
				BluetoothGattCharacteristic txCharacteristic = service.getCharacteristic(uuidRepository.getTXCharacteristic());
				if (txCharacteristic == null) return;

				BluetoothGattCharacteristic rxCharacteristic = service.getCharacteristic(uuidRepository.getRXCharacteristic());
				if (rxCharacteristic == null) return;

				//feature 5b: Subscribe to RX Characteristic
				if (false == subscriptCharacteristic(gatt, rxCharacteristic)) return;

				//feature 5c: Once successfully subscribed to RX Characteristic,
				// send the following Zero terminated string through TX Characteristic: “Ready”.
				txCharacteristic.setValue(context.getResources().getString(R.string.str_ready));
				gatt.writeCharacteristic(txCharacteristic);

			}


			/* Callback triggered as a result of a remote characteristic notification.*/
			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
				super.onCharacteristicChanged(gatt, characteristic);

				final UUID rxUUID = uuidRepository.getRXCharacteristic();
				if (false == characteristic.getUuid().equals(rxUUID)) return;

				final String received = new String(characteristic.getValue());
				Log.d(TAG, "Read Characteristic value = " + received);
				listener.onReceived(peripheral, received);

				//feature 5e: Reformat every string received and loop it back by sending it via TX Characteristic
				final String hexStr = String.format("%02X", recieveCounter.incrementAndGet());
				final String returnValue = hexStr + received + "\0";

				//TODO: refactor this part
				BluetoothGattService service = gatt.getService(uuidRepository.getServiceID());
				BluetoothGattCharacteristic txCharacteristic = service.getCharacteristic(uuidRepository.getTXCharacteristic());
				if (txCharacteristic == null) return;
				txCharacteristic.setValue(returnValue);
				gatt.writeCharacteristic(txCharacteristic);
			}
		});

	}

	private boolean subscriptCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic rxCharacteristic) {
		boolean subscribedResult = gatt.setCharacteristicNotification(rxCharacteristic, true);
		if (false == subscribedResult) return false;

		final BluetoothGattDescriptor descriptor = rxCharacteristic.getDescriptor(uuidRepository.getSubscriptUUID());
		if (descriptor == null) return false;

		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		return gatt.writeDescriptor(descriptor);
	}


	private void stopScan() {
		if (scanTask != null) {
			scanTask.stop();
		}
	}

	public void stop() {
		stopScan();
		disconnect();
	}



	public void disconnect() {
		if (this.gatt != null) {
			this.isManuallyDisconnect = true;
			gatt.disconnect();
		}
	}

}

