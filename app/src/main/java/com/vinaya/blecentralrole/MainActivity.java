package com.vinaya.blecentralrole;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.vinaya.blecentralrole.logic.Central;
import com.vinaya.blecentralrole.model.Peripheral;
import com.vinaya.blecentralrole.model.UUIDRepository;
import com.vinaya.blecentralrole.viewadapter.PeripheralListAdapter;

import java.util.List;

//TODO: add logger
//TODO: add unittest
/**
 * Entry point of and the only view class of this android app.
 *
 * The basic function of this Bluetooth Low Energy(BLE) Central Role application is to
 * allow user connect to peripherals with specific Service,
 * and perform the tasks implemented in the class [Central]
 *
 */
public class MainActivity extends AppCompatActivity {
	private final static int REQUEST_ENABLE_BT = 1;

	//--------------------------------------------------
	//UI components
	private ListView listViewPeripherals;
	private PeripheralListAdapter listAdapter;


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

		this.listViewPeripherals = (ListView)findViewById(R.id.listViewPeripherals);
		listViewPeripherals.setAdapter(listAdapter);

		//init the models
		UUIDRepository repository = new UUIDRepository(getResources());

		//init the controller
		this.central = new Central(this, repository);
	}


	@Override
	protected void onStart() {
		super.onStart();
		displayLoadingScreen();
		central.scan(onBtScanListener);

	}


	@Override
	protected void onStop() {
		central.stop();
		super.onStop();
	}

	//--------------------------------------------------
	//listener callbacks
	Central.ScanListener onBtScanListener = new Central.ScanListener() {
		@Override
		public void onScanned(List<Peripheral> list) {
			listAdapter.setPeripheralList(list);
			listAdapter.notifyDataSetChanged();

			cancelLoadingScreen();
		}

		@Override
		public void onFailed() {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
			dialogBuilder
				.setTitle(R.string.alert_scan_fail)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			}).create().show();
		}

		@Override
		public void onBluetoothNotEnabled() {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			MainActivity.this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	};

	PeripheralListAdapter.OnItemClickListener onPeripheralClickListener = new PeripheralListAdapter.OnItemClickListener() {
		@Override
		public void onClick(Peripheral peripheral) {
		}
	};


	PeripheralListAdapter.DisableFilter disablePeripheralFilter = new PeripheralListAdapter.DisableFilter() {
		@Override
		public boolean isDisable(Peripheral peripheral) {
			return !central.canConnect(peripheral);
		}
	};

	//--------------------------------------------------
	//helper methods
	private void displayLoadingScreen() {
	}

	private void cancelLoadingScreen() {
	}

}

