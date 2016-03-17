package com.vinaya.blecentralrole.logic.scanner;

import com.vinaya.blecentralrole.model.Peripheral;

/**
 * An API to scan BLE Devices,
 * so as to hide the implementation of scanner for different Android version
 */
public interface BLEScanner {
	/**
	 *  api to let user to stop the async scanning
	 */
	interface ScanTask {
		void stop();
	}

	/**
	 * callback for async scanning task
	 */
	interface BLEScanListener {
		//what to do if a new peripheral is discover?
		void onDiscovered(Peripheral peripheral);

		//what to do if the scanner failed to scan?
		void onFailed(int errorCOde);
	}

	/**
	 * scan the BLE peripherals around asynchronously
	 * @param scanListener what to do after scanned or failed
	 */
	ScanTask asyncScan(BLEScanListener listener);
}
