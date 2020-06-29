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
 * 	Airscape Fan (Gen 2 controls)
 *
 * 	Author: loghound
 * 	Date: May 1, 2018
 */
preferences {
    input("ip", "string", title: "IP Address", description: "10.0.0.236", defaultValue: "10.0.0.236", required: true, displayDuringSetup: true)

}

/* Stringify needs switch, polling, configuraiton, switch level, refresh, actuator */

metadata {
    definition(name: "airscapefan", namespace: "loghound", author: "loghound") {
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Actuator"
        capability "Configuration"

        capability "Temperature Measurement"
        capability "Switch"
        capability "Switch Level"

        attribute "temperature", "number"
        attribute "temperatureoa", "number"
        attribute "temperatureat", "number"
        attribute "timeLeft", "number"
        attribute "level","number"

        command "speedDown"
        command "addTime"

      //  command addTime

    }



    // UI tile definitions
    tiles(scale: 1) {
        valueTile("temperature", "device.temperature", width: 1, height: 1) {
            state "temperature", label: '${currentValue}°F', unit: "",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }

        valueTile("temperatureoa", "device.temperatureoa", width: 1, height: 1) {
            state "temperature", label: 'OA ${currentValue}°F', unit: "",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }

        valueTile("temperatureat", "device.temperatureattic", width: 1, height: 1) {
            state "temperature", label: 'Attic ${currentValue}°F', unit: "",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }

        valueTile("timeLeft", "device.timeLeft", width: 2, height: 1) {
            state "timeLeft", label: '${currentValue} minutes', unit: "minutes" , action : "addTime"
        }


        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "switch.on",
                    icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "switch.off",
                    icon: "st.switches.switch.on", backgroundColor: "#79b821"
        }

        controlTile("level", "device.level", "slider",
                height: 2, width: 2, range: "(0..5)") {
            state "level", action: "switch level.setLevel"
        }

        valueTile("power", "device.power", decoration: "flat", width: 1, height: 1) {
            state "power", label: '${currentValue} Watts'
        }



        standardTile("refresh", "device.backdoor", inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }



        main "temperature"
        details([ "temperature", "temperatureoa", "temperatureat", "timeLeft", "switch", "power", "level", "refresh"])
    }
}




def addTime () {
    def result = []
    result.add(setDir(2))  // '2' equals add time
    sendHubCommand(result)  // send to deice 
    runIn(30, poll)      // in 5 seconds check poll device.. unecessary?
    log.debug("Time added")

}


def configure() {
    log.debug "************ In Configure ***********"
	def outsideAirDNI="${device.deviceNetworkId}-OA"
	def atticAirDNI="${device.deviceNetworkId}-Attic"
	def roomAirDNI="${device.deviceNetworkId}-Room"

	if (getChildDevice(atticAirDNI)==null) {
	addChildDevice("hubitat","Virtual Temperature Sensor",atticAirDNI, [name: "Attic Temperature", isComponent: false])
	}
		 	if (getChildDevice(outsideAirDNI)==null) {
	addChildDevice("hubitat","Virtual Temperature Sensor",outsideAirDNI, [name: "Outside Temperature", isComponent: false])
			}
			 	if (getChildDevice(roomAirDNI)==null) {
	addChildDevice("hubitat","Virtual Temperature Sensor",roomAirDNI, [name: "Room Temperature", isComponent: false])
				}
	
	log.debug "ip address is $ip"
	
	
	// dev myId=device.deviceNetworkId()
	// log.debug "DNI is ${device.deviceNetworkId}"
/*
def children=getChildDevices()
	children.each { child ->
		child.setTemperature(432);
    	log.debug "child ${child.displayName} has deviceNetworkId ${child.deviceNetworkId}"
	}
	
	dev device=getChildDevices.find(it.deviceNetworkId=="$ip")
	log.debug(device)
	
	

*/	
}

def speedDown () {
    def result = []
    result.add(setDir(3))  // '3' equals speed down
    sendHubCommand(result)  // send to deice 
    runIn(60, poll)      // in 5 seconds check poll device.. unecessary?
    log.debug("Time added")
}


/* on() and off() are used as part of binary switches */

def on() {
    log.debug "Switch On"
    state.desiredFanSpeed = 1
  //  result << createEvent(name: "switch", value: "off", displayed: true)
    def result = levelHandler()

    //  return result
}

def off() {
    log.debug "Switch Off"
    state.desiredFanSpeed = 0
   // result << createEvent(name: "switch", value: "off", displayed: true)
    def result = levelHandler()

    // return result
}

/* Level is used as part of a multilevel switch */

def level(level, rate) {
    log.debug "level is ${level}"
    return state.level
}


def levelHandler() {
    def result = []

    log.debug "Level Handler called with a desired level of ${state.desiredFanSpeed} and an existing state of ${state.currentFanSpeed}"

    def i // counting varialbe
    if (state.desiredFanSpeed <= 0) {
        result.add(setDir(4))
        log.debug "Set fan to off ${result}"
    } else if (state.desiredFanSpeed - state.currentFanSpeed > 0) {
        for (i = 0; i < state.desiredFanSpeed - state.currentFanSpeed; i++) {
            result.add(setDir(1))
        }

        log.debug "Set fan to a faster speed ${result}"

    } else if (state.desiredFanSpeed - state.currentFanSpeed < 0) {
        for (i = 0; i < state.currentFanSpeed - state.desiredFanSpeed; i++) {
            result.add(setDir(3))
        }
        log.debug "Set fan down a speed ${result}"
    }

    sendHubCommand(result)


    runIn(10, poll)

}

def setLevel(level) {


    state.desiredFanSpeed = level
    log.debug "*** set level to ${level} device is at ${state.currentFanSpeed} and desired level is ${state.desiredFanSpeed}"


    def result
    result = levelHandler()
    log.debug("Returning result from set level ${result}")
    return result

}

def level() {
    return state.level;
}

def setDir(dir) {
/**
 see this post for descriptoin of the fanspeed.cgi scrip
 http://blog.airscapefans.com/archives/gen-2-controls-api

 in short
 If you want to get data from the controller without any control actions, you can send the same HTTP command string without the “?dir=|1|2|3|4|” suffix.

 For example, if your fan is at IP 192.168.0.20 the command would be:

 http://192.168.0.20/fanspd.cgi
 http://controllerURL/fanspd.cgi?dir=|1|2|3|4|
 where 1=fan speed up, 2=timer hour add, 3=fan speed down, 4=fan off

 For XML and JSON formatted responses use the following commands with your fan IP inserted:

 http://192.168.0.20/status.xml.cgi  gives data in xml format
 http://192.168.0.20/status.json.cgi  gives data in json format



 **/

    log.debug "Polling"


    def port = 80
    def path
    if (dir == 0)
        path = "/fanspd.cgi"
    else
        path = "/fanspd.cgi?dir=${dir}"

    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id} and path ${path}"
    def result = new hubitat.device.HubAction(
            method: "GET",
            path: path,
            headers: [
                    HOST: "${ip}:${port}"
            ]
    )

    return result
}


def initialize() {
    log.debug "starting a run every 5 minute"
    runEvery5Minutes(poll)
}

// parse events into attributes
def parse(String description) {
    def result = []
    def txt = parseLanMessage(description).body

    def oa_temp = txt =~ ~/oa_temp>(.+?)</
    def house_temp = txt =~ ~/house_temp>(.+?)</
    def attic_temp = txt =~ ~/attic_temp>(.+?)</
    def power = txt =~ ~/power>(.+?)</
    def fanspd = txt =~ ~/fanspd>(.+?)</
    def timeLeft = txt =~ ~/timeremaining>(.+?)</  // time left is minutes




    log.debug "Body text is:\n$txt"

    if (fanspd) {
    state.level = fanspd[0][1].toInteger()

    state.actualFanSpeed = fanspd[0][1].toInteger()

    log.debug "Actual Fan Speed is $state.level"
    result << createEvent(name:"level",value:state.level,displayed:true)
    }
    def doorStatus = txt =~ ~/doorinprocess>(.+?)</
	
	def outsideAirDNI="${device.deviceNetworkId}-OA"
	def atticAirDNI="${device.deviceNetworkId}-Attic"
	def roomAirDNI="${device.deviceNetworkId}-Room"

    if (house_temp) {
        state.temperature = house_temp[0][1]
        result << createEvent(name: "temperature", value: state.temperature, displayed: true)
		def dev=getChildDevice(roomAirDNI)
		dev.setTemperature(state.temperature)
		
        log.debug "Setting inside temperature to $state.temperature"
    }

    if (oa_temp) {
        state.temperatureoa = oa_temp[0][1]
        result << createEvent(name: "temperatureoa", value: state.temperatureoa, displayed: true)
		def dev=getChildDevice(outsideAirDNI)
		dev.setTemperature(state.temperatureoa)
        log.debug "Setting OA temperature to $state.temperatureoa"
    }

    if (attic_temp) {
        state.temperatureattic = attic_temp[0][1]
        result << createEvent(name: "temperatureattic", value: state.temperatureattic, displayed: true)
		def dev=getChildDevice(atticAirDNI)
		dev.setTemperature(state.temperatureattic)
		
        log.debug "Setting attic temperature to $state.temperatureattic"
    }

    if (power) {
        def powerDisplay = power[0][1].toInteger()
        result << createEvent(name: "power", value: powerDisplay, displayed: true)
        log.debug "Setting power to $powerDisplay"

        state.power = powerDisplay

    }

    if (timeLeft) {
        def timeDisplay=timeLeft[0][1].toInteger()
        result << createEvent(name :"timeLeft", value: timeDisplay, displayed:true)
        state.timeLeft=timeDisplay
    }

    // Emulate a binary switch

    if (state.level == 0) {
        result << createEvent(name: "switch", value: "off", displayed: true)
        result << createEvent(name: "level", value: 0, displayed: true)
        log.debug "switch set to off"

    } else {
        result << createEvent(name: "switch", value: "on", displayed: true)
        result << createEvent(name: "level", value: spdInt, displayed: true)
        log.debug "***switch set to on with level $state.level"
    }
    state.currentFanSpeed = state.level

    /* data managemenet */


    log.debug "poll results are $result"
    return result
}


def refresh() {
    log.debug "refresh"
    poll()
}

def poll() {

    def result = setDir(0)
    return result
}

