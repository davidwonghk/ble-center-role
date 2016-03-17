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
	private BluetoothAdapter bluetoothAdapter;

	private BLEScanner.ScanTask scanTask;

	private BluetoothGatt gatt;
	private boolean isManuallyDisconnect = false;

	//--------------------------------------------------
	//listener class definition

	public interface ScanListener {
		void onScanned(List<Peripheral> peripheralList);
		void onFailed();
		void onBluetoothNotEnabled();
	}

	public interface ConnectListener {
		void onConnected(Peripheral peripheral);
		void onDisconnected(Peripheral peripheral, boolean isManually);
		void onRecieved(Peripheral peripheral, String data);
		void onConnectFail();
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
		this.bluetoothAdapter = bluetoothManager.getAdapter();

		//if bluetoothAdapter is null, bluetooth seems to be not supported
		if (bluetoothAdapter == null) {
			listener.onFailed();
			return;
		}

		//call the callback if bluetooth is not enabled
		if (!bluetoothAdapter.isEnabled()) {
			listener.onBluetoothNotEnabled();
			return;
		}

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
			public void onFailed(int errorCOde) {
				listener.onFailed();
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
	 * @param peripheral the BLE Device
	 * @param listener callbacks about what to do upon connection/disconnect
	 */
	public void connect(final Peripheral peripheral, final ConnectListener listener) {
		final String TAG = getClass().getSimpleName();

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

				switch(newState) {
					case BluetoothProfile.STATE_CONNECTED:
						if (gatt.discoverServices()) {
							listener.onConnected(peripheral);
						} else {
							listener.onConnectFail();
						}
						return;

					case BluetoothProfile.STATE_DISCONNECTED:
						listener.onDisconnected(peripheral, isManuallyDisconnect);

						//feature 7: Automatically Reconnect if there is a cause of disconnection other
						// than the intentional disconnection performed by the user.
						if (false == isManuallyDisconnect) {
							connect(peripheral, listener);
						}
						isManuallyDisconnect = false;
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


				BluetoothGattService service = gatt.getService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"));

				//feature 5a: Discover TX Characteristic and RX Characteristic
				BluetoothGattCharacteristic txCharacteristic = service.getCharacteristic(uuidRepository.getTXCharacteristic());
				if (txCharacteristic == null) return;

				BluetoothGattCharacteristic rxCharacteristic = service.getCharacteristic(uuidRepository.getRXCharacteristic());
				if (rxCharacteristic == null) return;

				//feature 5b: Subscribe to RX Characteristic
				if (false == subscriptCharacteristic(gatt, rxCharacteristic)) return;

				//feature 5c: Once successfully subscribed to RX Characteristic,
				// send the following Zero terminated string through TX Characteristic: “Ready”.
				txCharacteristic.setValue("Ready");
				gatt.writeCharacteristic(txCharacteristic);

			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				super.onCharacteristicRead(gatt, characteristic, status);

				if (status != BluetoothGatt.GATT_SUCCESS) {
					Log.w("Central", "Read Characteristic not success");
					return;
				}

				/*
				if (false == characteristic.getUuid().equals(uuidRepository.getRXCharacteristic())) {
					return;
				}
				*/
				final String str = new String(characteristic.getValue());
				Log.d("Central", "Read Characteristic value = " + str);
				listener.onRecieved(peripheral, str);

			}

			@Override
			public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				super.onCharacteristicWrite(gatt, characteristic, status);
			}

			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
				super.onCharacteristicChanged(gatt, characteristic);
			}
		});

	}

	private static boolean subscriptCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic rxCharacteristic) {
		boolean subscribedResult = gatt.setCharacteristicNotification(rxCharacteristic, true);
		if (false == subscribedResult) return false;

		final List<BluetoothGattDescriptor> descriptors = rxCharacteristic.getDescriptors();
		if (descriptors.size() <= 0) return false;

		BluetoothGattDescriptor descriptor = descriptors.get(0);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		return gatt.writeDescriptor(descriptor);
	}


	public void stop() {
		if (scanTask != null) {
			scanTask.stop();
		}

		if (gatt != null) {
			gatt.disconnect();
		}
	}


	public void disconnect() {
		if (this.gatt != null) {
			this.isManuallyDisconnect = true;
			gatt.disconnect();
		}
	}

}

