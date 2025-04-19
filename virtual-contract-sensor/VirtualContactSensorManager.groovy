/**
 * Virtual Contact Sensor Manager
 *
 * Parent app to manage virtual contact sensors that represent the state of multiple physical sensors
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
    name: "Virtual Contact Sensor Manager",
    namespace: "loghound",
    author: "John McLaughlin",
    description: "Creates and manages virtual contact sensors that represent multiple physical sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true
)

preferences {
    page(name: "mainPage")
    page(name: "addVirtualSensorPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Virtual Contact Sensor Manager", install: true, uninstall: true) {
        section {
            paragraph "This app creates and manages virtual contact sensors that track the state of multiple physical sensors"
            input "logEnable", "bool", title: "Enable debug logging", defaultValue: false
        }
        section("Virtual Sensors") {
            app(name: "childApps", appName: "Virtual Contact Sensor Instance", namespace: "loghound", title: "Add Virtual Contact Sensor", multiple: true)
        }
    }
}

def installed() {
    log.info "Virtual Contact Sensor Manager installed"
    initialize()
}

def updated() {
    log.info "Virtual Contact Sensor Manager updated"
    initialize()
}

def initialize() {
    if (logEnable) log.debug "Debug logging enabled"
    childApps.each {child ->
        log.info "Child app: ${child.label}"
    }
}

def logDebug(message) {
    if (logEnable) log.debug message
}