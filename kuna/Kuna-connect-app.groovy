/**
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
 *  Kuna (Connect)
 *
 *  Author: John McLaughlin (loghound)
 *  Date: 2020-05-29
 * 
 */


definition(
        name: "Kuna (connect)",
        namespace: "loghound",
        author: "John McLaughlin",
        description: "Service Manager for kuna switches",
        category: "Convenience",
        importURL: "https://raw.githubusercontent.com/loghound/hubitat/master/kuna/Kuna-connect-app.groovy",
        iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn.png",
        iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@2x.png",
        iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@3x.png"
)

preferences {
    section("Internal Access") {
        input "myKunaEmail", "email", title: "Email", required: true
        input "myKunaPassword", "password", title: "Password", required: true
    }
}


def getVerifiedDevices() {
    getDevices().findAll { it?.value?.verified == true }
}


def configured() {
    log.debug("configured")

}

def buttonConfigured(idx) {
    return settings["lights_$idx"]
}

def isConfigured() {
    if (getChildDevices().size() > 0) return true else return false
}

def isVirtualConfigured(did) {
    def foundDevice = false
    getChildDevices().each {
        if (it.deviceNetworkId != null) {
            if (it.deviceNetworkId.startsWith("${did}/")) foundDevice = true
        }
    }
    return foundDevice
}

private virtualCreated(number) {
    if (getChildDevice(getDeviceID(number))) {
        return true
    } else {
        return false
    }
}

private getDeviceID(number) {
    return "${state.currentDeviceId}/${app.id}/${number}"
}

def installed() {
    log.debug "initialite()"
    initialize()

}

def updated() {
    log.debug("Updated")
    log.debug(updated)
    unsubscribe()
    unschedule()
    initialize()

}

def initialize() {
    logInToKuna()
    schedule("0 15 10 ? * *", logInToKuna) // every day at 10:15 refresh the token
}


def getDevices() {
    state.devices = state.devices ?: [:]
}


def uninstalled() {
    unsubscribe()
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}




def logInToKuna() {

    log.debug "logInToKuna"

    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : '/api/v1/account/auth/',
			requestContentType : 'application/json',
            headers: [
                    'Content-Type': 'application/json',
                    'User-Agent'  : 'Luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
					'Accept' : '*/*',


            ],
            body   : ["email"   : myKunaEmail,
                      "password": myKunaPassword
            ]
    ]

    log.debug "Update Toeken Params $params"

    asynchttpPost(authTokenHandler, params)

}
//Returned is a json token
//<pre>
//{
//	"token": "aa629aab2900075bd634e430"
//}
//</pre>

def authTokenHandler(response, data) {

    if (response.hasError()) {
        log.error "raw error response: $response.getErrorMessage()"
    } else {
        def jsonData = response.json
        state.token = jsonData['token']
        log.debug("got a idtoken=${state.token}")

    }
    getAllCameras()

}


private getAllCameras() {
    log.debug "Getting cameras and setting up child devices"

    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : '/api/v1/user/cameras/',
							requestContentType : 'application/json',
            headers: [
                    'Content-Type' : 'application/json',

                    'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "authorization": "Token  ${state.token}",
            ],
            body   : """"""
    ]

    log.debug "Get all cameras $params"

    asynchttpGet(getAllCamerasHandler, params)
}

def getAllCamerasHandler(response, data) {
    if (response.hasError()) {
        log.error "raw error response: $response.errorData"
    } else {
        def jsonData = response.json
        def cameras = jsonData['results']

        cameras.each {
            def dni = it.serial_number
            def label = "Kuna: ${it.name}"
            if (1) {

                def childDevice
                def currentDevice = getChildDevice(dni)
                if (currentDevice == null) {
                    log.debug "Adding ${it}\n"
                    childDevice = addChildDevice("loghound", "Kuna Light", dni, location.hubs[0].id, [
                            "label": label,
                            "data" : it
                    ])
                } else {                 // Update it if its changed
                    childDevice = getChildDevice(dni)
                }

                if (childDevice) {
                    childDevice.label = label
                    // now turn the light on/off and other things
                    def events = [:]

                        def onOff = it.bulb_on == true ? "on" : "off"
                        events << [name: "switch", value: onOff, displayed: true]
                        log.debug "Events to send $events to $childDevice from "
                        childDevice.generateEvent(events)
                    
                }

            }
        }
    }
}


// Global Enable to phone of alerts 
def globalCameraNotificationEnabled(enabled) {
    log.debug "Notification set to ${enabled}"
    def bodyString="""{"profile":{"notifications_enable_at":null,"notifications_enabled":"""+enabled+"}}"

    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : '/api/v1/user/',
			requestContentType : 'application/json',
            headers: [
                    'Content-Type' : 'application/json',

                    'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "authorization": "token ${state.token}",
            ],
            body   : bodyString
    ]

    log.debug "Update token Params $params"
    asynchttpPatch(globalCameraNotificationEnabledHandler, params)


}

def globalCameraNotificationEnabledHandler(response, data) {
    if (response.hasError()) {
        // Could get raw error response: {"message":"Unauthorized"}
        log.error "error parsing json - raw error data is $response.errorData"
    } else {

        def jsonData = response.json
        log.debug jsonData
        def profile=jsonData['profile']
        log.debug "Proifel is $profile"
        def notificationsEnabled = profile['notifications_enabled']
        log.debug "Notifications Enabled is $notificationsEnabled"

    }
}

// Turn lights on or off
private kunaLightsOn(dni, enabled) {

        log.debug "adjustLights set to $enabled"

        def params = [
                uri    : 'https://server.kunasystems.com',
                path   : "/api/v1/cameras/$dni/",
				requestContentType : 'application/json',
                headers: [
                        'Content-Type' : 'application/json',

                        'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                        "authorization": "token ${state.token}",
                        'method'       : 'post'
                ],
                body   : ["bulb_on":enabled]
        ]

        log.debug "Update token Params $params"
        asynchttpPatch(kunaLightsOnHandler, params) // do it twice because sometimes the cameras don't respond
}




private kunaRefresh(dni) {


        log.debug "refresh $dni"

        def params = [
                uri    : 'https://server.kunasystems.com',
                path   : "/api/v1/cameras/$dni/",
				requestContentType : 'application/json',
                query  : ["live":1],
                headers: [

                        'User-Agent'   : 'luna/2.4.0 (iPhone; iOS 11.4; Scale/3.00)',
                        "Authorization": "Token ${state.token}",
                ],
                body : ""
        ]

        log.debug "Update token Params $params"
        asynchttpGet(kunaLightsOnHandler, params) // do it twice because sometimes the cameras don't respond

}

def kunaLightsOnHandler(response, data) {
    if (response.hasError()) {
        // Could get raw error response: {"message":"Unauthorized"}
		def msg=response.getErrorMessage()
        log.error "error parsing json lights on handler - raw error data is $msg"
    } else {


        def jsonData = response.json
        		log.debug "got json data for update $jsonData"
        def dni = jsonData['serial_number']
        def bulb_on = jsonData['bulb_on']
        def camera = getChildDevice(dni)

        def events = []
        def onoff = bulb_on ? "on" : "off"
        events << [name: "switch", value: onoff, displayed: true]
        camera.generateEvent(events)

    }
}


/***********************************************
 * child commands sent up
 ************************************************/


// Child On means 'open' cameras (value = 100), 200 is full up and 0 is full down
def childOn(dni) {
    log.debug "childOn ${dni}"
    kunaLightsOn(dni, true)
}


// Child off means closed (in this case position 0 or 200) --
def childOff(dni) {
    log.debug "Child Off ${dni}"
    kunaLightsOn(dni, false)
}

def childRefresh(dni) {
    kunaRefresh(dni)
}


// Images -- not so much a hubitat thing.....
def take(dni) {
    def params = [
            uri: "https://server.kunasystems.com",
            path: "/some/path"
    ]

    try {
        httpGet(params) { response ->
            // we expect a content type of "image/jpeg" from the third party in this case
            if (response.status == 200 && response.headers.'Content-Type'.contains("image/jpeg")) {
                def imageBytes = response.data
                if (imageBytes) {
                    def camera = getChildDevice(dni)
                    camera.childStoreImage(imageBytes)
                }
            } else {
                log.error "Image response not successful or not a jpeg response"
            }
        }
    } catch (err) {
        log.debug "Error making request: $err"
    }
    
}


