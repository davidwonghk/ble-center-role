package com.vinaya.blecentralrole;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.vinaya.blecentralrole.logic.Central;
import com.vinaya.blecentralrole.model.Peripheral;
import com.vinaya.blecentralrole.model.UUIDRepository;
import com.vinaya.blecentralrole.viewadapter.PeripheralListAdapter;

import java.util.List;

//TODO: add logger
//TODO: add unittest

/**
 * Entry point of the android app and the Controller of MVC
 *
 * The basic function of this Bluetooth Low Energy(BLE) Central Role application is to
 * allow user connect to peripherals with specific Service,
 * and perform the tasks implemented in the class {@link Central}
 */
public class MainActivity extends AppCompatActivity {
	private final static int REQUEST_ENABLE_BT = 1;

	//--------------------------------------------------
	//UI components

	private ListView listViewPeripherals;
	private PeripheralListAdapter listAdapter;
	private Button buttonDisconnect;
	private EditText editText;


	//--------------------------------------------------
	//Controller -- the main logic
	private Central central;


	//--------------------------------------------------
	//Override callback methods of Activity
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//init UI components

		this.listAdapter = new PeripheralListAdapter(MainActivity.this);
		listAdapter.setOnItemClickListener(onPeripheralClickListener);
		listAdapter.setDisableFilter(disablePeripheralFilter);
		listAdapter.setOnRssiAlertListener(getResources().getInteger(R.integer.rssi_threshold), rssiAlertListener);

		this.listViewPeripherals = (ListView) findViewById(R.id.listViewPeripherals);
		listViewPeripherals.setAdapter(listAdapter);

		this.buttonDisconnect = (Button) findViewById(R.id.buttonDisconnect);
		buttonDisconnect.setVisibility(View.GONE);
		buttonDisconnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//feature 6: Implement a “disconnect” button: when it is pressed the Central should disconnect from the Peripheral.
				if (central != null) {
					central.disconnect();
				}
			}
		});

		this.editText = (EditText) findViewById(R.id.editText);
		editText.setVisibility(View.GONE);

		//init the models
		UUIDRepository repository = new UUIDRepository(getResources());

		//init the controller
		this.central = new Central(this, repository);
	}


	@Override
	protected void onStart() {
		super.onStart();
		displayLoadingScreen();

		final long scanPeriod = getResources().getInteger(R.integer.scan_period);
		if (false == central.start()) {
			askToOpenBluetooth();
		}
		else {
			central.scan(onBtScanListener, scanPeriod);
		}
	}


	@Override
	protected void onStop() {
		central.stop();
		super.onStop();
	}

	//--------------------------------------------------
	//listener callbacks
	final Central.ScanListener onBtScanListener = new Central.ScanListener() {
		@Override
		public void onScanned(List<Peripheral> list) {
			listAdapter.setPeripheralList(list);
			listAdapter.notifyDataSetChanged();

			cancelLoadingScreen();
		}

		@Override
		public void onFailed(int errorCode) {
			showAlert(R.string.alert_scan_fail, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			});
		}
	};

	final PeripheralListAdapter.RssiAlertListener rssiAlertListener = new PeripheralListAdapter.RssiAlertListener() {
		@Override
		public void onAlertRssi(Peripheral peripheral) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, R.string.alert_rssi_too_low, Toast.LENGTH_SHORT);
				}
			});
		}
	};

	final PeripheralListAdapter.OnItemClickListener onPeripheralClickListener = new PeripheralListAdapter.OnItemClickListener() {
		@Override
		public void onClick(Peripheral peripheral) {
			central.connect(peripheral, connectListener);
		}
	};


	final PeripheralListAdapter.DisableFilter disablePeripheralFilter = new PeripheralListAdapter.DisableFilter() {
		@Override
		public boolean isDisable(Peripheral peripheral) {
			return !central.canConnect(peripheral);
		}
	};


	final Central.ConnectListener connectListener = new Central.ConnectListener() {
		@Override
		public void onConnected(Peripheral peripheral) {
			Log.i("MainActivity", "connected");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					buttonDisconnect.setVisibility(View.VISIBLE);
					editText.setVisibility(View.VISIBLE);
				}
			});
		}

		@Override
		public void onDisconnected(Peripheral peripheral, boolean manually) {

			if (!manually) {
				showAlert(R.string.disconnect, null);
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					buttonDisconnect.setVisibility(View.GONE);
					editText.setText("");
					editText.setVisibility(View.GONE);
				}
			});

		}

		@Override
		public void onReceived(Peripheral peripheral, final String data) {
			Log.i("MainActivity", data);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					editText.append(data + "\n");
				}
			});
		}

		@Override
		public void onConnectFail() {
			Log.i("MainActivity", "connection fail");
		}
	};

	//--------------------------------------------------
	//helper methods
	private void displayLoadingScreen() {
		//TODO: to be implmented
	}

	private void cancelLoadingScreen() {
		//TODO: to be implmented
	}

	private void showAlert(final int strId, final DialogInterface.OnClickListener listener) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
				dialogBuilder.setTitle(strId);
				dialogBuilder.setPositiveButton("OK", listener);
				dialogBuilder.create().show();
			}
		});
	}


	private void askToOpenBluetooth() {
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		MainActivity.this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}

}

