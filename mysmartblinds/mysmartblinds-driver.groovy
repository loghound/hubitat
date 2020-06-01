/**
 *  Copyright 2018 John McLaughlin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    preferences {
        input "onValue", "number", title: "On Value", description: "What do we set the blinds when on (0..100)",
                range: "0..100", displayDuringSetup: false, defaultValue: 50
        input "offValue", "number", title: "Off Value", description: "What do we set the blinds when on (0..100)",
                defaultValue: 0,
                range: "0..100", displayDuringSetup: false
    }

    definition(name: "MySmartBlinds Blind", namespace: "loghound", author: "John McLaughlin") {
        capability "Switch"
        capability "Polling"
        capability "Configuration"
        capability "Switch Level"
        capability "Refresh"
        capability "Actuator"

        // these are required for smartthigns to show them as a dimmer:  Switch,Polling,Configuration,Switch Level,Refresh,Actuator
        attribute "rssi", "number"
        attribute "batteryLevel", "number"

        // Calls from Parent to Child
        command "generateEvent", ["JSON_OBJECT"]
        command "log", ["string", "string"]

        //  https://community.smartthings.com/t/childdevices-parent-somemethod-this/12798/12 from rboy
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
                attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action: "switch level.setLevel"
            }
        }
        controlTile("levelSliderControl", "device.level", "slider",
                height: 2, width: 2) {
            state "level", label: '${currentValue} %', unit: "%", action:"switch level.setLevel"
        }
        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "level", label: '${currentValue} %', unit: "%", backgroundColor: "#ffffff"
        }
        valueTile("rssi", "device.rssi", decoration: "flat", width: 2, height: 2) {
            state "rssi", label: '${currentValue}', unit: "dBm", icon:"st.Entertainment.entertainment15",
                    backgroundColors:[
                            [value: -90, color: "#bc2323"],
                            [value: -80, color: "#ffae19"],
                            [value: -70, color: "#f1d801"],
                            [value: -67, color: "#44b621"],
                            [value: -30, color: "#02aaff"]
                    ]
        }

        valueTile("batteryLevel", "device.batteryLevel", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "batteryLevel", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
                    backgroundColors:[
                            [value: 10, color: "#bc2323"],
                            [value: 26, color: "#f1d801"],
                            [value: 51, color: "#44b621"]
                    ]
        }
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        // main switch
        details(['switch', 'levelSliderControl', 'rssi', 'batteryLevel', 'refresh'])
    }

}

// If you add the Polling capability to your device type, this command
// will be called approximately every 5 minutes to check the device's state
def poll() {
    // TODO:  Setup the polling from the parent app
    debug.error "Poll called -- not sure why from mysmartblinds"
}

// If you add the Configuration capability to your device type, this
// command will be called right after the device joins to set
// device-specific configuration commands.
def configure() {
    // TODO:  setup the configuration
    debug.log "Configure called on mysmartblinds"
}

// Should never be called?
def parse(String description) {
    log.error "Parse called on mysmartblinds -- should never be called? $description"

}

void on() {
    def v = settings.onValue ?: 100 // Use a default value of 0 if nothing set
    v = v * 2
    log.debug "Child Device turning blinds on $v"

    parent.childOn(device.deviceNetworkId, v)

}

void off() {

    def v = settings.offValue ?: 0 // Use a default value of 0 if nothing set
    v = v * 2
    log.debug "Child Device turning blinds off $v"
    parent.childOff(device.deviceNetworkId, v)

}

void refresh() {
    parent.getBlindsStatus(device.deviceNetworkId)
}

def setLevel(value) {
    log.debug "Setting level of blinds to $value"
    parent.childSetLevel(device.deviceNetworkId, value)
    sendEvent(name: "level", value: value, isStateChange: true, display: true)
}

def log(message, level = "trace") {
    switch (level) {
        case "trace":
            log.trace "LOG FROM PARENT>" + message
            break

        case "debug":
            log.debug "LOG FROM PARENT>" + message
            break

        case "warn":
            log.warn "LOG FROM PARENT>" + message
            break

        case "error":
            log.error "LOG FROM PARENT>" + message
            break

        default:
            log.error "LOG FROM PARENT>" + message
            break
    }

    return null // always child interface call with a return value
}

// Register the event attributes with the device
void generateEvent(results) {
    log.trace "Generate Event called: ${results.inspect()}"

    results.each { event ->
        log.trace "Sending event name: ${event.inspect()}"
        sendEvent(event)
    }
}

