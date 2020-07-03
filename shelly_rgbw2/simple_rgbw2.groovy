/**

* worlds most simple shelly rgbw2 driver

 */

import hubitat.helper.ColorUtils
import groovy.transform.Field
import groovy.json.*




//	==========================================================

metadata {
	definition (
        name: "Simple Shelly rgbw2",
		namespace: "loghound",
		author: "John McLaughlin"
		)
	{
        capability "Actuator"
        capability "Sensor"
        capability "Refresh" // refresh command
        capability "Switch"
        capability "Switch Level"
        capability "Polling"
        capability "Power Meter"

        capability "Light"
        capability "ColorControl"
        
        attribute "RGBw", "string"
        attribute "RGB", "string"
        attribute "HEX", "string"
        attribute "HSV", "string"
        attribute "hue", "number"
        attribute "saturation", "number"
        attribute "level", "number"
    }

	preferences {
	def refreshRate = [:]
		refreshRate << ["1 min" : "Refresh every minute or so"]
		refreshRate << ["5 min" : "Refresh every 5 minutes"]
		refreshRate << ["15 min" : "Refresh every 15 minutes"]
		refreshRate << ["30 min" : "Refresh every 30 minutes"]


	input("ip", "string", title:"Shelly IP Address:", description:"EG; 192.168.0.100", defaultValue:"" , required: true)
	input name: "username", type: "text", title: "Username:", description: "(blank if none)", required: false
	input name: "password", type: "password", title: "Password:", description: "(blank if none)", required: false

  
    input ("refresh_Rate", "enum", title: "Device Refresh Rate", options: refreshRate, defaultValue: "30 min")

	input name: "debugOutput", type: "bool", title: "Enable debug logging?", defaultValue: true
	input name: "debugParse", type: "bool", title: "Enable JSON parse logging?", defaultValue: true
	input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	input name: "Shellyinfo", type: "text", title: "<center><font color=blue>Info Box</font><br>Shelly API docs located</center>", description: "<center><a href='http://shelly-api-docs.shelly.cloud/'>[here]</a></center>"
	}
}



def installed() {

}

def initialize() {
	log.info "initialize"
}

def poll() {

    refresh()
}

def updated() {
    log.info "Preferences updated..."
    log.warn "Debug logging is: ${debugOutput == true}"
    log.warn "JSON parsing logging is: ${debugParse == true}"
    log.warn "Description Text logging is: ${txtEnable == true}"
    unschedule()


        sendSettings "mode=color"
        if (txtEnable) log.info "Executing mode=color"

 

    switch(refresh_Rate) {
		case "1 min" :
			runEvery1Minute(autorefresh)
			break
		case "5 min" :
			runEvery5Minutes(autorefresh)
			break
		case "15 min" :
			runEvery15Minutes(autorefresh)
			break
		default:
			runEvery30Minutes(autorefresh)
	}

    if (txtEnable) log.info ("Refresh set for every ${refresh_Rate} minute(s).")
    if (debugOutput) runIn(1800,logsOff)
	if (txtEnable) runIn(600,txtOff)
    if (debugParse) runIn(600,parseOff)
    


    

    refresh()
}




def refresh() {
    PollShellyStatus()
    PollShellySettings()
}

def logsOff(){
	log.warn "debug logging auto disabled..."
	device.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def parseOff(){
	log.warn "Json logging auto disabled..."
	device.updateSetting("debugParse",[value:"false",type:"bool"])
}

def txtOff(){
	log.warn "Description text logging auto disabled..."
	device.updateSetting("debugParse",[value:"false",type:"bool"])
}

def parse(description) {
    runIn(2, refresh)
}

def PollShellyStatus(){
 logDebug "Shelly Status called"
    def paramsStatus = [uri: "http://${username}:${password}@${ip}/status"]
try {
    httpGet(paramsStatus) {
        respStatus -> respStatus.headers.each {
        logJSON "ResponseStatus: ${it.name} : ${it.value}"
    }
        obsStatus = respStatus.data

        logJSON "paramsStatus: ${paramsStatus}"
        logJSON "responseStatus contentType: ${respStatus.contentType}"
	    logJSON  "responseStatus data: ${respStatus.data}"

       // def each state 

        ison = obsStatus.lights.ison[0]
        power = obsStatus.meters.power[0]
        mode = obsStatus.mode
        red = obsStatus.lights.red[0]
        green = obsStatus.lights.green[0]
        blue = obsStatus.lights.blue[0]
        white = obsStatus.lights.white[0]
        rgbwCode = "${red},${green},${blue},${white}"
        rgbCode = "${red},${green},${blue}"
        
//        def idstr = state.mac
//        deviceid = idstr.substring(6,6)
//        sendEvent(name: "DeviceID", value: deviceid)
        
         sendEvent(name: "RGBw", value: rgbwCode)
        if (txtEnable) log.info "rgbCode = $red,$green,$blue,$white"
        sendEvent(name: "RGB", value: rgbCode)
        Hex = ColorUtils.rgbToHEX( [red, blue, green] )
        sendEvent(name: "HEX", value: Hex)

        if (ison == true) {
            sendEvent(name: "switch", value: "on")
        } else {
            sendEvent(name: "switch", value: "off")
        }

 
} // End try
        
    } catch (e) {
        log.error "something went wrong: $e"
    }
    
} // End PollShellyStatus

def PollShellySettings(){
 logDebug "Shelly Settings called"
    def paramsSettings = [uri: "http://${username}:${password}@${ip}/settings"]
try {
    httpGet(paramsSettings) {
        respSettings -> respSettings.headers.each {
        logJSON "ResponseSettings: ${it.name} : ${it.value}"
    }
        obsSettings = respSettings.data

        logJSON "paramsSettings: ${paramsSettings}"
        logJSON "responseSettings contentType: ${respSettings.contentType}"
	    logJSON "responseSettings data: ${respSettings.data}"

    
} // End try
        
    } catch (e) {
        log.error "something went wrong: $e"
    }
    
}// End PollShellySettings



//	Device Commands
//switch.on
def on() {
    if (txtEnable) log.info "Executing switch.on"
    sendSwitchCommand "/color/0?turn=on"
}

//switch.off
def off() {
    if (txtEnable) log.info "Executing switch.off"
    sendSwitchCommand "/color/0?turn=off"
}




// switch.CustomRGBwColor
def CustomRGBwColor(r,g,b,w=null) {
    if (txtEnable) log.info "Executing Custom Color Red:${r} Green:${g} Blue:${b} White=${w}"
    if (w == null) {
        sendStringCommand "red=${r}&green=${g}&blue=${b}"
    } else {
        sendStringCommand "red=${r}&green=${g}&blue=${b}&white=${w}"
    }
    r = r.toInteger()
    g = g.toInteger()
    b = b.toInteger()
	hsvColors = ColorUtils.rgbToHSV([r,g,b])
    logDebug "hsvColors = $hsvColors"
    sendEvent(name: "HSV", value: hsvColors)
    h = hsvColors[0]
    s = hsvColors[1]
    huelevel = hsvColors[2]
    state.hue=h
    state.saturation=s
    state.level=hueLevel

	sendEvent(name: "hue", value: h)
	sendEvent(name: "saturation", value: s)
	sendEvent(name: "huelevel", value: huelevel)
}

// Not used so lets null the actions
def setHue(value) {
    def s=device.currentValue("saturation")
    def l=device.currentValue("level")
   log.info "setHue $value, sat=$s, level=$l"
     setColor(['hue':value,'saturation':s,'level':l])
}
def setSaturation(value) {
    def h=device.currentValue("hue")
    def l=device.currentValue("level")
     log.info "setSaturation $value"
  setColor(['hue':h,'saturation':value,'level':l])
}


//

def setColor( parameters ){
    logDebug "Color set to ${parameters}"
    
    state.level=parameters.Level
    state.hue= parameters.hue
    state.saturation=parameters.saturation
	sendEvent(name: "hue", value: parameters.hue)
	sendEvent(name: "saturation", value: parameters.saturation)
	sendEvent(name: "level", value: parameters.level)
	rgbColors = ColorUtils.hsvToRGB( [parameters.hue, parameters.saturation, parameters.level] )
    r = rgbColors[0].toInteger()
    g = rgbColors[1].toInteger()
    b = rgbColors[2].toInteger()
    w = parameters.level.toInteger() * 1.8 // I don't know why but 1.8 gives the best color combo
    if (txtEnable) log.info "Red: ${r},Green:${g},Blue:${b},white {$w}"
    CustomRGBwColor(r,g,b,w)
}


//switch.level
def setLevel(percent) {

        def w=percent.toInteger()*1
        sendStringCommand "turn=on&gain=${percent}&white=$w"
        if (txtEnable) log.info ("Color Mode setLevel ${percent}")
}


// End Direct Device Commands



def autorefresh() {
    refresh()
}

// handle commands

def sendSwitchCommand(action) {
    if (txtEnable) log.info "Calling ${action}"
	def path = path
	def body = body
	def headers = [:]
    if (username != null) {
        headers.put("HOST", "${username}:${password}@${ip}")
    } else {
        headers.put("HOST", "${ip}")
    }
	headers.put("Content-Type", "application/x-www-form-urlencoded")
    runIn(2, refresh)

	try {
		def hubAction = new hubitat.device.HubAction(
			method: "POST",
			path: action,
			body: body,
			headers: headers
			)
		logDebug hubAction
		return hubAction
	}
	catch (Exception e) {
        logDebug "sendSwitchCommand hit exception ${e} on ${hubAction}"
	}
}

def sendStringCommand(action) {
    if (txtEnable) log.info "Setting Level ${action}"
    if (username != null) {
        host = "${username}:${password}@${ip}"
    } else {
        host = "${ip}"
    }
    sendHubCommand(new hubitat.device.HubAction(
      method: "POST",
      path: "/color/0",
      body: action,
      headers: [
        HOST: host,
        "Content-Type": "application/x-www-form-urlencoded"
      ]
    ))
    refresh()
}

def sendSettings(action) {
    if (txtEnable) log.info "Setting Level ${action}"
    if (username != null) {
        host = "${username}:${password}@${ip}"
    } else {
        host = "${ip}"
    }
    sendHubCommand(new hubitat.device.HubAction(
      method: "POST",
      path: "/settings",
      body: action,
      headers: [
        HOST: host,
        "Content-Type": "application/x-www-form-urlencoded"
      ]
    ))
    refresh()
}


private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
	log.debug "$msg"
	}
}

private logJSON(msg) {
	if (settings?.debugParse || settings?.debugParse == null) {
	log.info "$msg"
	}
}


