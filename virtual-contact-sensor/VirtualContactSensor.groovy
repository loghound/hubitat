/**
 * Virtual Contact Sensor
 *
 * A virtual contact sensor that takes the state of the last physical contact sensor to change
 *
 * Copyright 2025 John McLaughlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
    definition(
        name: "Virtual Contact Sensor",
        namespace: "loghound",
        author: "John McLaughlin",
        importUrl: "https://raw.githubusercontent.com/yourusername/hubitat-groovy/main/virtual-contract-sensor/VirtualContactSensor.groovy"
    ) {
        capability "Sensor"
        capability "ContactSensor"
        
        attribute "lastUpdatedSensor", "string"
        attribute "lastUpdatedDate", "string"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}

def updated() {
    log.info "Virtual Contact Sensor updated"
    initialize()
}

def installed() {
    log.info "Virtual Contact Sensor installed"
    initialize()
}

def initialize() {
    // Nothing specific to initialize for this device
    if (logEnable) log.debug "Debug logging enabled"
}

// Method to update the sensor state
def updateSensorState(sensorDeviceId, newState, sensorName = null) {
    def displayName = sensorName ?: sensorDeviceId
    if (logEnable) log.debug "Updating sensor state from ${displayName} with new state: ${newState}"
    
    def currentDate = new Date().format("yyyy-MM-dd HH:mm:ss")
    
    sendEvent(name: "contact", value: newState)
    sendEvent(name: "lastUpdatedSensor", value: displayName)
    sendEvent(name: "lastUpdatedDate", value: currentDate)
    
    if (logEnable) log.debug "Virtual Contact Sensor state updated to: ${newState}"
}

// Method to manually set the sensor state (for testing)
// Method to manually set the contact sensor state (useful for testing or manual overrides)
def setContactState(String newState) {
    // Check if the new state is valid (either "open" or "closed")
    if (newState == "open" || newState == "closed") {
        // Update the sensor state using the updateSensorState method
        updateSensorState("manual", newState, "Manual Update")
    } else {
        // Log a warning if the provided state is invalid
        log.warn "Invalid contact state: ${newState}. Must be 'open' or 'closed'."
    }
}