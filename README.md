# BLECenterRole
## Introduction
An Android App that implements a Bluetooth Low Energy Central Role with features for demostration purpose.

## Purpose
To demostrate
- my Java programming level
- my ability of anroid application development
- my capability to learn new API and concepts (Bluetooth BLE). 

## features
1. Ability to scan for BLE peripherals around you.
2. Display peripherals found and their RSSI values.
3. Improve 2) by greying out the Peripherals that do not offer the service with UUID: abc00001-1234-5678-1234-abcd0123abcd
4. Allow user to connect to peripherals with Service with UUID: abc00001- 1234-5678-1234-abcd0123abcd.
5. Upon connection: 
* a) Discover TX Characteristic (UUID: abc00002-1234-5678-1234- abcd0123abcd) and RX Characteristic (UUID: abc00003-1234-5678-1234- abcd0123abcd).
* b) Subscribe to RX Characteristic.
* c) Once successfully subscribed to RX Characteristic, send the following Zero terminated string through TX Characteristic: “Ready”.
* d) Display the data arrived via RX Characteristic (assume that it is a string).
* e) Reformat every string received as follows and loop it back by sending it via TX Characteristic: <N><StringReceived><\0>  Where: N = 8 bit number represented in hexadecimal that increments at every communication StringReceived = string received on RX characteristic \0 = null terminator character Example: if you received “Hello World” as first string, you would have “01Hello World\0” 
6. Implement a “disconnect” button: when it is pressed the Central should disconnect from the Peripheral. 
7. Automatically Reconnect if there is a cause of disconnection other than the intentional disconnection performed by the user. Note that it should reconnect to the latest.

## Screenshots
![alt tag](https://raw.githubusercontent.com/davidwonghk/ble-center-role/master/screenshot/1.png)
![alt tag](https://raw.githubusercontent.com/davidwonghk/ble-center-role/master/screenshot/2.png)
![alt tag](https://raw.githubusercontent.com/davidwonghk/ble-center-role/master/screenshot/3.png)
![alt tag](https://raw.githubusercontent.com/davidwonghk/ble-center-role/master/screenshot/4.png)
