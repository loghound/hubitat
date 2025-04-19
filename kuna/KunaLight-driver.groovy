/**
 *  Kuna Light Device Driver
 *
 *  Copyright 2020-2025 John McLaughlin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  This child device is instantiated by the Kuna (Connect) service manager app. It serves two purposes:
 *  1. Control the light on/off function of Kuna security cameras
 *  2. Enable/disable global notifications when the device network ID ends with GLOB_NOTIFICATION_EN
 *
 *  Author: John McLaughlin (loghound)
 *  Date: 2020-05-29
 *  Updated: 2025-04-19 - Enhanced logging, error handling, and documentation
 */

metadata {
    definition (
        name: "Kuna Light", 
        namespace: "loghound", 
        author: "John McLaughlin", 
        vid: "generic-switch",
        importURL: "https://raw.githubusercontent.com/loghound/hubitat/master/kuna/KunaLight-driver.groovy"
    ) {
        capability "Switch"
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"
        capability "ImageCapture" // Properly declare the image capture capability

        // Commands from Parent to Child
        command "notificationEnable", [[name: "Enable Notifications", type: "ENUM", description: "Enable or Disable Notifications", constraints: ["true", "false"]]]
        command "generateEvent", ["JSON_OBJECT"]
        command "generateEvents", ["JSON_OBJECT"]
        command "log", ["string", "string"]
        command "childStoreImage", ["string"]

        // Custom Attributes
        attribute "lastRefresh", "string"
        attribute "lastImage", "string"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }

    // UI Tiles
    tiles {
        standardTile("image", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: true) {
            state "default", label: "", action: "", icon: "st.camera.dropcam-centered", backgroundColor: "#FFFFFF"
        }
 
        standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
            state "taking", label:'Taking', action: "", icon: "st.camera.dropcam", backgroundColor: "#00A0DC"
            state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
        }

        multiAttributeTile(name:"switch", type: "lighting", width: 3, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState:"turningOff"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        main "image"
        details (["cameraDetails", "take", "switch", "refresh"])
    }
}

/**
 * Lifecycle Methods
 */

void installed() {
    logger("INFO", "Kuna Light device installed")
    initialize()
}

void updated() {
    logger("INFO", "Kuna Light device configuration updated")
    initialize()
}

void initialize() {
    logger("INFO", "Initializing Kuna Light device")
    // Auto-disable debug logs after 30 minutes
    if (logEnable) {
        runIn(1800, "logsOff")
        logger("DEBUG", "Debug logging will be automatically disabled in 30 minutes")
    }
    refresh()
}

/**
 * Device Commands
 */

/**
 * Turn the Kuna light on
 */
void on() {
    logger("INFO", "Turning ON")
    parent.childOn(device.deviceNetworkId)
    // Optimistic state change until we get confirmation from API
    sendEvent(name: "switch", value: "on")
}

/**
 * Turn the Kuna light off
 */
void off() {
    logger("INFO", "Turning OFF")
    parent.childOff(device.deviceNetworkId)
    // Optimistic state change until we get confirmation from API
    sendEvent(name: "switch", value: "off")
}

/**
 * Refresh the device state from the API
 */
void refresh() {
    logger("DEBUG", "Refreshing device state")
    parent.childRefresh(device.deviceNetworkId)
    sendEvent(name: "lastRefresh", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
}

/**
 * Enable or disable notifications
 * @param enabled - Whether notifications should be enabled (true/false)
 */
void notificationEnable(enabled) {
    def enabledBool = (enabled == "true") ? true : false
    logger("INFO", "Setting notifications to: ${enabledBool ? "ENABLED" : "DISABLED"}")
    parent.globalCameraNotificationEnabled(enabledBool)
    sendEvent(name: "notificationStatus", value: enabledBool ? "enabled" : "disabled")
}

/**
 * Log a message from the parent app to this device's log
 * @param message - The message to log
 * @param level - The log level (trace, debug, warn, error)
 */
def log(message, level = "trace") {
    switch (level) {
        case "trace":
            if (logEnable) log.trace "PARENT>" + message
            break
        case "debug":
            if (logEnable) log.debug "PARENT>" + message
            break
        case "warn":
            log.warn "PARENT>" + message
            break
        case "error":
            log.error "PARENT>" + message
            break
        default:
            log.trace "PARENT>" + message
            break
    }
    return null // always return null for child interface calls
}

/**
 * Event Handling
 */

/**
 * Register an event with the device
 * @param results - Event data with name and value properties
 */
void generateEvent(results) {
    logger("DEBUG", "Received event: ${results.inspect()}")
    
    // Sanitize input to prevent errors
    if (results?.name == null || results?.value == null) {
        logger("WARN", "Invalid event data received: ${results}")
        return
    }
    
    // Send the event to the device
    sendEvent(name: results.name, value: results.value, displayed: true)
    
    // Add description text if enabled
    if (txtEnable && results.descriptionText) {
        logger("INFO", results.descriptionText)
    }
}

/**
 * Alternative method name for generateEvent to maintain compatibility
 * @param results - Event data with name and value properties
 */
void generateEvents(results) {
    generateEvent(results)
}

/**
 * Image Capture
 */

/**
 * Take a photo with the Kuna camera
 */
def take() {
    logger("INFO", "Taking image from camera")
    sendEvent(name: "imageTaking", value: "taking", displayed: false)
    parent.take(device.deviceNetworkId) // calls 'childStoreImage on successful completion'
}

/**
 * Store an image received from the parent app
 * @param imageBytes - The image data bytes
 */
def childStoreImage(imageBytes) {
    def name = getImageName()
    logger("DEBUG", "Storing image with name: ${name}")
    
    try {
        storeImage(name, imageBytes)
        sendEvent(name: "image", value: name, displayed: true)
        sendEvent(name: "lastImage", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone), displayed: false)
        sendEvent(name: "imageTaking", value: "complete", displayed: false)
        logger("INFO", "Image capture successful")
    } catch (e) {
        logger("ERROR", "Error storing image ${name}: ${e}")
        sendEvent(name: "imageTaking", value: "failed", displayed: false)
    }
}

/**
 * Generate a unique image name
 * @return A UUID string with dashes removed
 */
def getImageName() {
    return java.util.UUID.randomUUID().toString().replaceAll('-','')
}

/**
 * Utility Methods
 */

/**
 * Logging utility that honors the user's preferences
 * @param level - The level of the log message (INFO, DEBUG, etc.)
 * @param message - The message to log
 */
private void logger(String level, String message) {
    switch (level) {
        case "ERROR":
            log.error "${device.displayName}: ${message}"
            break
        case "WARN":
            log.warn "${device.displayName}: ${message}"
            break
        case "INFO":
            if (txtEnable) log.info "${device.displayName}: ${message}"
            break
        case "DEBUG":
            if (logEnable) log.debug "${device.displayName}: ${message}"
            break
        case "TRACE":
            if (logEnable) log.trace "${device.displayName}: ${message}"
            break
        default:
            if (logEnable) log.debug "${device.displayName}: ${message}"
            break
    }
}

/**
 * Turn off debug logging after timeout
 */
void logsOff() {
    logger("INFO", "Debug logging disabled")
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}
