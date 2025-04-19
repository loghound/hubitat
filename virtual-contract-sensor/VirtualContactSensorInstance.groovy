/**
 * Virtual Contact Sensor Instance
 *
 * Child app to manage an individual virtual contact sensor
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

definition(
    name: "Virtual Contact Sensor Instance",
    namespace: "loghound",
    author: "John McLaughlin",
    description: "Creates and manages a virtual contact sensor that represents multiple physical sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    parent: "loghound:Virtual Contact Sensor Manager"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Virtual Contact Sensor Configuration", install: true, uninstall: true) {
        section("Virtual Sensor Settings") {
            label title: "Enter a name for this virtual contact sensor", required: true
            input "contactSensors", "capability.contactSensor", title: "Select Contact Sensors to Monitor", multiple: true, required: true
            input "initialState", "enum", title: "Initial State", options: ["open", "closed"], defaultValue: "closed"
            input "logEnable", "bool", title: "Enable debug logging", defaultValue: false
        }
    }
}

def installed() {
    log.info "Virtual Contact Sensor Instance installed"
    initialize()
}

def updated() {
    log.info "Virtual Contact Sensor Instance updated"
    unsubscribe()
    initialize()
}

def initialize() {
    if (logEnable) log.debug "Initializing with sensors: ${contactSensors}"
    
    // Create the virtual sensor device if it doesn't exist
    def virtualSensor = getChildDevice(getDeviceId())
    if (!virtualSensor) {
        logDebug "Creating Virtual Contact Sensor device"
        virtualSensor = addChildDevice("loghound", "Virtual Contact Sensor", getDeviceId(), null, [name: "Virtual Contact Sensor", label: app.label])
        
        // Set initial state
        if (initialState) {
            virtualSensor.updateSensorState("initialSetup", initialState, "Initial Setup")
            logDebug "Set initial state to ${initialState}"
        }
    }
    
    // Subscribe to contact sensor events
    subscribe(contactSensors, "contact", contactEventHandler)
}

def contactEventHandler(evt) {
    logDebug "Contact event from ${evt.displayName}: ${evt.value}"
    
    def virtualSensor = getChildDevice(getDeviceId())
    if (virtualSensor) {
        virtualSensor.updateSensorState(evt.deviceId, evt.value, evt.displayName)
        logDebug "Updated virtual sensor state to ${evt.value} from ${evt.displayName}"
    } else {
        log.error "Virtual sensor device not found"
    }
}

private getDeviceId() {
    return "virtual-contact-${app.id}"
}

def logDebug(msg) {
    if (logEnable) log.debug(msg)
}