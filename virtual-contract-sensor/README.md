# Virtual Contact Sensor for Hubitat

This package provides a virtual contact sensor that combines multiple physical contact sensors and takes the state of the last one that changed.

## Purpose

The Virtual Contact Sensor allows you to create a single virtual device that represents the combined state of multiple physical contact sensors. This is useful for:

- Creating a "master" sensor for doors/windows in a single room or zone
- Simplifying automations that need to respond to any of several sensors
- Monitoring multiple entry points with a single sensor
- Allow a way to avoid a single sensor causing failures (you can put mulitiple sensors on a single door/window and if one fails you will still get one working)

## Components

1. **VirtualContactSensor.groovy** - The device driver that provides a virtual contact sensor with open/closed states
2. **VirtualContactSensorManager.groovy** - The parent app that manages the creation of virtual contact sensors
3. **VirtualContactSensorInstance.groovy** - The child app that configures individual virtual contact sensors

## Installation

1. In the Hubitat Hubitat UI, go to **Apps Code**
2. Click **New App**
3. Paste the contents of **VirtualContactSensorManager.groovy** and click **Save**
4. Click **New App** again
5. Paste the contents of **VirtualContactSensorInstance.groovy** and click **Save**
6. Go to **Drivers Code**
7. Click **New Driver**
8. Paste the contents of **VirtualContactSensor.groovy** and click **Save**

## Usage

1. Go to **Apps** in the Hubitat UI
2. Click **Add User App**
3. Select **Virtual Contact Sensor Manager**
4. Click **Done** to install the app
5. Click on the **Virtual Contact Sensor Manager** in your apps list
6. Click **Add Virtual Contact Sensor**
7. Enter a name for your virtual contact sensor
8. Select the physical contact sensors you want to monitor
9. Choose an initial state (open or closed)
10. Click **Done**

Your virtual contact sensor will now appear in your devices list and will update its state whenever any of the selected physical sensors change state.

## Features

- Tracks which physical sensor last changed state
- Records timestamp of the last update
- Appears as a standard contact sensor that can be used in any automation
- Can monitor any number of physical contact sensors

## Support

For issues or feature requests, please open an issue on GitHub or contact the developer.