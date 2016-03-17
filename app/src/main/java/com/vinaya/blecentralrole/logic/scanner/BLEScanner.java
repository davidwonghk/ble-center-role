package com.vinaya.blecentralrole.logic.scanner;

import com.vinaya.blecentralrole.model.Peripheral;

/**
 * An API to scan BLE Devices,
 * so as to hide the implementation of scanner for different Android version
 */
public interface BLEScanner {
	interface ScanTask {
		void stop();
	}

	interface BLEScanListener {
		void onDiscovered(Peripheral peripheral);
		void onFailed(int errorCOde);
	}

	/**
	 * scan the BLE peripherals around asynchronously
	 * @param scanListener what to do after scanned or failed
	 */
	ScanTask asyncScan(BLEScanListener listener);
}
