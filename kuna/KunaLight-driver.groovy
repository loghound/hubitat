/**
 *  Switch Child Device
 *
 *  Copyright 2020 John McLaughlin
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
 * This child device will be instantiated the service manager -- it will serve as both a light on/off as well
 * as a global enable (multiple devices) -- if the device network id ends in
 * GLOB_NOTIFICATION_EN then this device will effect the global enable for notificaiton
 *
 *  Kuna Light
 *
 *  Author: John McLaughlin (loghound)
 *  Date: 2020-05-29
 */
metadata {
    definition (name: "Kuna Light", 
            namespace: "loghound", 
            author: "John McLaughlin", 
            vid: "generic-switch",
            importURL: "https://raw.githubusercontent.com/loghound/hubitat/master/KunaLight-driver.groovy"
        ) {
            capability "Switch"
            capability "Actuator"
            capability "Sensor"
            capability "Refresh"
            //capability "Image Capture"

            // Calls from Parent to Child
            command "notificationEnable", ["number"]
            command "generateEvent", ["JSON_OBJECT"]
            command "generateEvents", ["JSON_OBJECT"]
            command "log", ["string", "string"]
            // command "childStoreImage", ["string"]

    }

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
        details ("cameraDetails","take","switch","refresh")
        
    }
}

void on() {
    parent.childOn(device.deviceNetworkId)
}

void off() {
    parent.childOff(device.deviceNetworkId)
}

void refresh() {
    parent.childRefresh(device.deviceNetworkId)
}

void notificationEnable(enabled)
{
    log.trace "notification Enabled $enabled"
    parent.globalCameraNotificationEnabled(enabled)

}

def log( message,  level = "trace") {
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
    log.trace "name ${results.name} value ${results.value}"
    sendEvent(name:results.name, value:results.value )
}

// I keep on calling it with a "s"
void generateEvents(results) {
    generateEvent(results)
}

def take() {
    parent.take(device.deviceNetworkId) // calls 'childStoreImage on succesful completion'
}

// Not really a hubitat thing
def childStoreImage(imageBytes) {
    def name = getImageName()
    try {
        storeImage(name, imageBytes)
    } catch (e) {
        log.error "Error storing image ${name}: ${e}"
    }
}


// NOt really a hubitat thing
def getImageName() {
    return java.util.UUID.randomUUID().toString().replaceAll('-','')
}
