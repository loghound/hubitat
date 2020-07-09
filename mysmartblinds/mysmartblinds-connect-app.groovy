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
 *  Mysmartblinds (connect)
 *  This handles the IO to smartblinds servers as well as device discovery and creation
 *
 *  Author: John McLaughlin (loghound)
 *  Date: 2016-06-02
 */



definition(
        name: "Mysmartblinds (connect)",
        namespace: "loghound",
        author: "John McLaughlin",
        description: "Service Manager for Mysmartblinds switches",
        category: "Convenience",
        iconUrl: "https://yt3.ggpht.com/a-/AN66SAy1L1pKAmeCmuGRzG6YZKSe0iJQINv9oVI_QA=s288-mo-c-c0xffffffff-rj-k-no",
        iconX2Url: "https://yt3.ggpht.com/a-/AN66SAy1L1pKAmeCmuGRzG6YZKSe0iJQINv9oVI_QA=s288-mo-c-c0xffffffff-rj-k-no",
        iconX3Url: "https://yt3.ggpht.com/a-/AN66SAy1L1pKAmeCmuGRzG6YZKSe0iJQINv9oVI_QA=s288-mo-c-c0xffffffff-rj-k-no"
)

preferences {
    section("Internal Access") {
        input "mySmartBlindsEmail", "email", title: "Email", required: true
        input "mySmartBlindsPassword", "password", title: "Password", required: true
        input "syncBlindNames", "bool", title: "Keep blind names synchronized with mySmartBlinds App"
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
    logInToMySmartBlinds()
    schedule("0 15 10 ? * *",logInToMySmartBlinds) // every day at 10:15 refresh the token
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
/****************************************
 * Authentication
 *****************************************/
// curl -H 'Host: mysmartblinds.auth0.com' -H 'accept: */*' -H 'content-type: application/json'
// -H 'auth0-client: aFSuYW1lIjoiTG9jay5pT1MiLCJ2ZXJzaW9uIjoiMS4yOS4yIn0'
//-H 'user-agent: MySmartBlinds/1.4.6 (iPhone; iOS 11.4.1; Scale/3.00)'
// -H 'accept-language: en-US;q=1'
// --data-binary '{"device":"Johns iphone","password":"PASSWORD","scope":"openid offline_access","grant_type":"password","username":"EMAIL","client_id":"3r34231c3vuqWtpUt1U577QX38GzCJZzm8AFJ","connection":"Username-Password-Authentication"}'
// --compressed 'https://mysmartblinds.auth0.com/oauth/ro’
// Here is what is actually required
// curl -H 'Host: mysmartblinds.auth0.com' -H 'accept: */*' -H 'content-type: application/json'
// --data-binary '{"device":"Johns iphone","password":"PASSWORD","scope":"openid offline_access","grant_type":"password","username":"EMAIL","client_id":"fgdaa322342323231c3vuqWtpUt1U577QX38GzCJZzm8AFJ","connection":"Username-Password-Authentication"}' --compressed 'https://mysmartblinds.auth0.com/oauth/ro'


/* 
 Async calls as documentd here https://community.hubitat.com/t/async-http-calls/1694
 Duplicate the curl below

 curl --location --request POST 'https://postman-echo.com/post' \
 --data 'foo1=bar1' \
 --data 'foo2=bar2'
*/ 
def _privateAsyncTest() {
    log.debug "_privateAsyncTest"

    def params = [
            uri    : 'https://postman-echo.com/headers',
            timeout : 100,
            contentType: 'application/json',
            requestContentType: 'application/json',
            headers:[ 
                   'User-Agent'  : 'Luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)'
            ],

            body   : [
                        "foo"  : "bar",
                        "foo1"  : "bar1",
                    ]
    ]

    try {
        asynchttpGet("private_handler", params,params)
    } catch (e) {
        log.error "Error in asynchttpPos: $e \n $params"
  }

}
def private_handler (response,data) {
    if (response.hasError()) {
        log.debug "private-handler raw error response: $response.errorData"
        log.debug "private-handler total error is $response"
        log.debug response.getStatus()
        log.debug "private-handler error data is $data"
    } else  {

            dataR=response.getData()
            log.debug "private-handler was good! $dataR"

    }
}

def logInToMySmartBlinds() {
    _privateAsyncTest()
    return


    def params = [
            uri    : 'https://mysmartblinds.auth0.com/oauth/ro',
            timeout : 100,
            contentType: 'application/json',
            requestContentType: 'application/json',
            headers: [
                "User-Agent"  : 'Luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)'
            ],

            body   : [
                        "username"  : mySmartBlindsEmail,
                        "password"  : mySmartBlindsPassword,
                        "device"    : "Johns Iphone",
                        "scope"     : "openid offline_access",
                        "grant_type": "password",
                        "client_id" : "adfasd1c3vuqWtpUt1U577a38GzCJZzm8AFJ",
                        "connection": "Username-Password-Authentication",
                    ]
    ]

    //log.debug "Update Token Params $params"
    try {
        asynchttpPost("authTokenHandler", params,params)
    } catch (e) {
        log.error "Error in logInToMySmartBlinds(): $e \n $params"
  }
}
//
// response is like
// {"id_token":"aFS0eXAiOiJxxGciOiJIUzI1NiJ9.aFSpc3MiOiJodHRwczovL215c21hcnRibGluZHMuYXV0aDAuY29tLyIsInN1YiI6ImFafdasdf323GgwfDU4Y2M1NjE1NDEzMzY5MDM3MjMzYTE1YyIsImF1ZCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIiwiaWF0IjoxNTM0NTU4NzEwLCJleHAiOjE1MzQ1OTQ3MTB9.FFB5Vw9VmZZ6uLN2NSMCRTlG6KoOwjArBGW-kVALgFc",
// "refresh_token":"x5jQ_lu_aIxxc5N3kNyaF_QD5DMK",
// "access_token":"qhbmNbiY5x-VMQxWxxTgo0Tbw-I",
// "token_type":"bearer"}

def authTokenHandler(response, data) {
    if (response.hasError()) {
        log.debug "authTokenHandler raw error response: $response.errorData"
        log.debug "authTokenHandler total error is $response"
        log.debug response.getStatus()
        log.debug "authTokenHandler error data is $data"
    } else {
        def jsonData = response.json
        state.id_token = jsonData['id_token']
        state.access_token = jsonData['access_token']
        state.token_type = jsonData['token_type']
        state.refresh_token = jsonData['refresh_token']

        log.debug("got a idtoken=${state.id_token}")
        log.debug("got a access_token=${state.access_token}")
    }
    getAllBlinds()
    //adjustBlinds("2kJr9hLZ",0)
}
// Reauthenticate --- do this if you have a valid refresh_token and     client_id     (known as access_token)
// curl -H 'Host: mysmartblinds.auth0.com' -H 'accept: */*' -H 'content-type: application/json' -H 'auth0-client: aFSuYW1lIjoiTG9jay5pT1MiLCJ2ZXJzaW9uIjoiMS4yOS4yIn0' -H 'user-agent: MySmartBlinds/1.4.6 (iPhone; iOS 11.4.1; Scale/3.00)' -H 'accept-language: en-US;q=1' --data-binary '{"client_id":"gdsa1c3vuqWtpUt1U577QX38GzCJZzm8AFJ","scope":"openid offline_access","device":"Johns iphone","refresh_token":"MdEcAvrtOVtHWZtP-3b0_CROAEGFVtHJL9oZNegiDjuAW","api_type":"app","grant_type":"urn:ietf:params:oauth:grant-type:jwt-bearer"}' --compressed 'https://mysmartblinds.auth0.com/delegation’

def reauthToken() {

    log.debug "logInToMySmartBlinds"

    def params = [
            uri    : 'https://mysmartblinds.auth0.com',
            path   : '/oauth/ro',
            headers: [
                    'Content-Type': '/delegation',
                    'User-Agent'  : 'Luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    'method'      : 'post',
                    'body'        : body,

            ],
            body   : """{"client_id":"afsda1c3vuqWtpUt1U577QX38GzCJZzm8AFJ","scope":"openid offline_access","device":"Johns iphone","refresh_token":"${
                state.refresh_token
            }","api_type":"app","grant_type":"urn:ietf:params:oauth:grant-type:jwt-bearer"}"""
    ]

    log.debug "Update Toeken Params $params"

    asynchttpPost(reAuthTokenHandler, params)

}
//
// response is like
//      "token_type": "Bearer",
//    "expires_in": 36000,
//    "id_token": "aFS0eXAiREWWiLCJhbGciOiJIUzI1NasdfasfasdfashcnRibGluZHMuFFFGgwfDU4Y2M1NjE1NDEzMzY5MDM3MjMzYTE1YyIsImF1ZCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIiwiZXhwIjoxNTM0NzU0MDQ0LCJpYXQiOjE1MzQ3MTgwNDQsImF6cCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIn0.2UTknz8YqEgP-drA9REFEWlTQ7YL0X23Fe2JgM"
//}

def reAuthTokenHandler(response, data) {
    if (response.hasError()) {
        log.error "reauth token handler raw error response: $response.errorData"
        log.error "Will try to get a brand new token"

        logInToMySmartBlinds()
    } else {
        def jsonData = response.json
        state.id_token = jsonData['id_token']
        log.debug("got a idtoken=${state.id_token}")

    }
    getAllBlinds()
    //adjustBlinds("2kJr9hLZ",0)
}
// Get blinds and set up child objectss
// curl -H 'Host: api.mysmartblinds.com' -H 'accept: */*' -H 'content-type: application/json' -H 'auth0-client-id: asdfgdas1c3aaewxx38GzCJZzm8AFJ' -H 'user-agent: MySmartBlinds/0 CFNetwork/902.2 Darwin/17.7.0' -H 'accept-language: en-us' -H 'authorization: Bearer aFS0eXAiOiJKV1QiLCJhbG23afasdfadszovL215c21hcnRibGluZHMuYXV0aDAuY29tLyIsInN1YiI6ImFadfasdfaGgwfDU4Y2M1NjE1NDEzMzY5MDM3MjMzYTE1YyIsImF1ZCI6IFDEjFkMWafMzdnVxV3RwVXxxz1FYNWd6Q0paem04V09CIiwiaWF0IjoxNTM0NTY3MzA4LCJleHAiOjE1MzQ2MDMzMDh9.MfKOiY6hawdqVGJnV_nIFXBXJrmeqZ4SGXE8fywC3PQ' --data-binary '{"query":"query GetUserInfo {  user {    __typename    id    email    globalSmartClosePosition    globalSmartOpenPosition    lastUpdatedGlobalPositionTimestamp    tokenExpiration    locationDescription    latitude    longitude    clientUpdatedAt    rooms {      __typename      userId      id      name      defaultClosePosition      defaultOpenPosition      energySavingsEnabled      energySavingsPosition      energySavingsTemperatureSetPoint      energySavingsResumeEnabled      lastUpdateDefaultPositions      lastUpdateSchedule      scheduleData      deleted      clientUpdatedAt    }    blinds {      __typename      userId      id      roomId      name      encodedPasskey      encodedMacAddress      batteryPercent      hasSyncedPositions      hasSyncedSchedule      lastUpdateName      lastUpdateStatus      passkeyNeedsChanging      reverseRotation      deleted      clientUpdatedAt    }    smartSwitches {      __typename      userId      id      name      encodedMacAddress      blueGroup      greenGroup      redGroup      whiteGroup      yellowGroup      bottomSwitchDefaultBlue      bottomSwitchDefaultGreen      bottomSwitchDefaultRed      bottomSwitchDefaultWhite      bottomSwitchDefaultYellow      topSwitchDefaultBlue      topSwitchDefaultGreen      topSwitchDefaultRed      topSwitchDefaultWhite      topSwitchDefaultYellow      deleted      clientUpdatedAt    }    hubs {      __typename      id      userId      encodedMacAddress      rssi      certificateId      name      nordicVersion      wifiVersion      deleted    }  }}","variables":null}' --compressed 'https://api.mysmartblinds.com/v1/graphql'

private getAllBlinds() {
    log.debug "Getting blinds and setting up child devices"

    def params = [
            uri    : 'https://api.mysmartblinds.com',
            path   : '/v1/graphql',
            headers: [
                    'Content-Type'   : 'application/json',
                    'User-Agent'     : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "auth0-client-id": "${state.access_token}",
                    "authorization"  : "Bearer ${state.id_token}",
                    'method'         : 'post'
            ],
            body   : """{"query":"query GetUserInfo {  user {    __typename    id    email    globalSmartClosePosition    globalSmartOpenPosition    lastUpdatedGlobalPositionTimestamp    tokenExpiration    locationDescription    latitude    longitude    clientUpdatedAt    rooms {      __typename      userId      id      name      defaultClosePosition      defaultOpenPosition      energySavingsEnabled      energySavingsPosition      energySavingsTemperatureSetPoint      energySavingsResumeEnabled      lastUpdateDefaultPositions      lastUpdateSchedule      scheduleData      deleted      clientUpdatedAt    }    blinds {      __typename      userId      id      roomId      name      encodedPasskey      encodedMacAddress      batteryPercent      hasSyncedPositions      hasSyncedSchedule      lastUpdateName      lastUpdateStatus      passkeyNeedsChanging      reverseRotation      deleted      clientUpdatedAt    }    smartSwitches {      __typename      userId      id      name      encodedMacAddress      blueGroup      greenGroup      redGroup      whiteGroup      yellowGroup      bottomSwitchDefaultBlue      bottomSwitchDefaultGreen      bottomSwitchDefaultRed      bottomSwitchDefaultWhite      bottomSwitchDefaultYellow      topSwitchDefaultBlue      topSwitchDefaultGreen      topSwitchDefaultRed      topSwitchDefaultWhite      topSwitchDefaultYellow      deleted      clientUpdatedAt    }    hubs {      __typename      id      userId      encodedMacAddress      rssi      certificateId      name      nordicVersion      wifiVersion      deleted    }  }}","variables":null}"""
    ]

    log.debug "Get all blinds $params"

    asynchttpPost(blindsGetChildren, params)
}
/*
{
    "data": {
        "user": {
            "__typename": "User",
            "id": "auth0|58cc5615FDASD13369037233a15c",
            "email": "EMAIL",
            "globalSmartClosePosition": 1,
            "globalSmartOpenPosition": 0.5,
            "lastUpdatedGlobalPositionTimestamp": 0,
            "tokenExpiration": 1534603308,
            "locationDescription": "Santa Rosa, CA  95405\nUnited States",
            "latitude": 38,
            "longitude": -122,
            "clientUpdatedAt": 1519001129.4557738,
      "blinds": [{
            "__typename": "Blind",
            "userId": "auth0|58cc5615413369037233a15c",
            "id": "1c9e97ca-7c8b-438d-8763-383f7bce0c76",
            "roomId": "473846ad-ad6e-4204-802e-46b0a83f3a68",
            "name": "Office",
            "encodedPasskey": "BuPvoyAX",
            "encodedMacAddress": "2kJr9hLZ",
            "batteryPercent": -1,
            "hasSyncedPositions": true,
            "hasSyncedSchedule": true,
            "lastUpdateName": 549608000,
            "lastUpdateStatus": 495441152,
            "passkeyNeedsChanging": false,
            "reverseRotation": false,
            "deleted": false,
            "clientUpdatedAt": 1534544405.283072
          }, {
            "__typename": "Blind",
            "userId": "auth0|58cc561541fFDAD3369037233a15c",
            "id": "2f2fd571-f1a2-42b8-b7c9-3b54a215d2f7",
            "roomId": "d465578e-b63c-46cc-a342-6361ab250e42",
            "name": "Left Dining",
            "encodedPasskey": "fniOaVXX",
            "encodedMacAddress": "XK4gmx/W",
            .........
*/

def blindsGetChildren(response, data) {
    log.info $response
    log.trace $data
    if (response.hasError()) {
        log.debug "raw error response: $response.errorData"
    } else {
        // log.debug("response is ${response.data}")
        def jsonData = response.json
        def blindsData = jsonData['data']
        def user = blindsData['user']
        def blinds = user['blinds']

        blinds.each {
            def dni = it.encodedMacAddress

            if (1) {

                def label = it.name
                def childDevice
                def currentDevice = getChildDevice(it.encodedMacAddress)
                if (currentDevice == null && it['deleted'] == false) {
                    log.debug "Adding ${it}\n"
                    childDevice = addChildDevice("loghound", "MySmartBlinds Blind", dni, location.hubs[0].id, [
                            "label": label,
                            "data" : it
                    ])

                } else {                 // Update it if its changed
                    if (syncBlindNames) {
                        childDevice = getChildDevice(dni)
                        if (childDevice) {
                            childDevice.label = label
                        }
                    }
                }

            }
        }

    }
    // adjustBlinds("2kJr9hLZ",100)
}
//curl -H 'Host: api.mysmartblinds.com' -H 'accept: */*' -H 'content-type: application/json' -H 'auth0-client-id: gdasga1c3vuqWtpUt1U577QX38GzCJZzm8AFJ' -H 'user-agent: MySmartBlinds/0 CFNetwork/902.2 Darwin/17.7.0' -H 'accept-language: en-us' -H 'authorization: Bearer aFS0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.aFSpc3MiOiJodHRwczovL215c21hcnRibGluZHMuYXV0aDAuY29tLyIsInN1YiI6ImFadfasfaGgwfDU4Y2M1NjE1NDEzMzY5MDM3MjMzYTE1YyIsImF1ZCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIiwiZXhwIjoxNTM0NTgwNDAzLCJpYX5323234231241mF6cCI6IjFkMWMzdnVxV3RwVXfsdsWd6Q0paem04V09CIn0.5QL7objl7kOi0tYFYcJFztv2gvKFo9-haqS4RUJe4kE'
// --data-binary '{"query":"mutation UpdateBlindsPosition($blinds: [String], $position: Int!) {  updateBlindsPosition(encodedMacAddresses: $blinds, position: $position) {    __typename    encodedMacAddress    position    rssi    batteryLevel  }}","variables":{"position":106,"blinds":["2kJr9hLZ"]}}' --compressed 'https://api.mysmartblinds.com/v1/graphql’
// setBlind("2kJr9hLZ",0)
// GetBlindsState -- update the blinds states as far as we can tell
//   curl -H 'Host: api.mysmartblinds.com' -H 'accept: */*' -H 'content-type: application/json' -H 'auth0-client-id: gdasdagda1c3vuqWtpU38GzCJZzm8AFJ' -H 'user-agent: MySmartBlinds/0 CFNetwork/902.2 Darwin/17.7.0' -H 'accept-language: en-us' -H 'authorization: Bearer aFS0eXAiOiJKadsfadsfas9.aFSpc3MiOiJodHRwczovL215c21hcnRibGluZHMuYIjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIiwiZXhwIjoxNTM1MjcyNTA1LCJpYXQiOjE1MzUyMzY1MDUsImF6cCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIn0.h-Uh8vXRCXJh-jd4arHxUcU_wsAbJxhgedj29Ao0KbA' --data-binary '{"query":"query GetBlindsState($blinds: [String]) {  blindsState(encodedMacAddresses: $blinds) {    __typename    encodedMacAddress    position    rssi    batteryLevel  }}","variables":{"blinds":["6i5DKpHF","sXk43hD6"]}}' --compressed 'https://api.mysmartblinds.com/v1/graphql'
def getBlindsStatus(dni) {
    log.debug "Updating Status of Blinds..."

    def blindsToLookFor = "[" //  """["$dni"]"""
//     Only 7 devices can be quieried at a time right now?
    def childDevices = getChildDevices()

    childDevices.each { blind ->
        def ndni = blind.deviceNetworkId
        blindsToLookFor = blindsToLookFor + """"$ndni","""
    }
    blindsToLookFor = blindsToLookFor[0..-2]
    blindsToLookFor = blindsToLookFor + "]"
    
    log.debug "Blinds to look for $blindsToLookFor"

    def httpBody = """{"query":"query GetBlindsState(\$blinds: [String]) {  blindsState(encodedMacAddresses: \$blinds) {    __typename    encodedMacAddress    position    rssi    batteryLevel  }}","variables":{"blinds":""" + blindsToLookFor + "}}"



    def params = [
            uri    : 'https://api.mysmartblinds.com',
            path   : '/v1/graphql',
            headers: [
                    'Content-Type'   : 'application/json',
                    'User-Agent'     : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "auth0-client-id": "${state.access_token}",
                    "authorization"  : "Bearer ${state.id_token}",
                    'method'         : 'post'
            ],
            body   : httpBody
    ]

    log.debug "getBlindsStatus $params"

    asynchttpPost(getBlindsStatusHandler, params)
}
/* Can be either a returned with data
    {
    "data": {
        "blindsState": [{
            "__typename": "BlindState",
            "encodedMacAddress": "6i5DKpHF",
            "position": 2,
            "rssi": -68,
            "batteryLevel": 62
        }, {
            "__typename": "BlindState",
            "encodedMacAddress": "sXk43hD6",
            "position": 0,
            "rssi": -76,
            "batteryLevel": 100
        }]
    }
}

If the blind wasn't found then position will be -1, rssi will be 0 and batteryLevel will be -1
*/

def getBlindsStatusHandler(response, data) {

    log.debug "getBlindsStatusHandler"
    if (response.hasError()) {
        log.error "response has error: $response.errorMessage"
    } else {
        log.debug("response is ${response.data}")
        def jsonData = response.json
        def blindsData = jsonData['data']
        def blinds = blindsData['blindsState']
        log.debug("jsonData is $blinds")
        blinds.each {
            def dni = it.encodedMacAddress
            def rssi = it.rssi
            def batteryLevel = it.batteryLevel
            def level = it.position / 2 // 200 is full up -- 100 is defined as 50% level

            def childDevice = getChildDevice(dni)

            if (childDevice) {

                def events = []
                if (Math.abs(level - 50) < 50) {
                    events << [name: "switch", value: "on", displayed: true]
                } else {
                    events << [name: "switch", value: "off", displayed: true]
                }
                events << [name: "level", value: level]

                events << [name: "rssi", value: rssi, displayed: true]
                events << [name: "batteryLevel", value: batteryLevel, displayed: true]
                log.debug("Sending Events to child $dni $events")
                childDevice.generateEvent(events) // Update the device status
            } else {
                log.debug "Cound't find child to send events to $dni"
            }

        }

    }
}

private adjustBlinds(dni, level) {
    log.debug "adjustBlinds to ${level}"

    def params = [
            uri    : 'https://api.mysmartblinds.com',
            path   : '/v1/graphql',
            headers: [
                    'Content-Type'   : 'application/json',
                    'User-Agent'     : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "auth0-client-id": "${state.access_token}",
                    "authorization"  : "Bearer ${state.id_token}",
                    'method'         : 'post'
            ],
            body   : """{"query":"mutation UpdateBlindsPosition(\$blinds: [String], \$position: Int!) {  updateBlindsPosition(encodedMacAddresses: \$blinds, position: \$position) {    __typename    encodedMacAddress    position    rssi    batteryLevel  }}","variables":{"position":""" + level + ""","blinds":["$dni"]}}"""
    ]

    log.debug "Update token Params $params"
    asynchttpPost(blindsMoveHandler, params, params)
    //asynchttp_v1.post(blindsMoveHandler, params) // do it twice because sometimes the blinds don't respond

}

def blindsMoveHandler(response, data) {
    if (response.hasError()) {
        // Could get raw error response: {"message":"Unauthorized"}
        try {
            // TODO:  How to avoid an infinite loop?
            def responseError = response.errorData
            def dataString = data.inspect()
            if (responseError == "java.util.concurrent.TimeoutException") {
                log.debug "Request Timed Out, Try again: $dataString"
                def resendData = evaluate(dataString)
                asynchttpPost(blindsMoveHandler, resendData, resendData)
            }
        } catch (e) {
            log.debug "error resending request: $e"
        }
        try {

            def jsonError = response.errorJson
            log.debug "error json: $jsonError"
            if (jsonError['message'] == "Unauthorized") {
                logInToMySmartBlinds()     // re-authorize
            }
        } catch (e) {
            log.debug "error parsing json - raw error data is $response.errorData"
        }
    } else {
        log.debug("response is ${response.data}")
        def events = []
        def jsonData = response.json
        def firstBlind = jsonData['data']['updateBlindsPosition'][0]
        def rssi = firstBlind['rssi']
        def position = firstBlind['position']
        if (Math.abs(position - 100) < 50) {
            events << [name: "switch", value: "on", displayed: true]
        } else {
            events << [name: "switch", value: "off", displayed: true]
        }
        events << [name: "level", value: position / 2]

        events << [name: "rssi", value: firstBlind['rssi'], displayed: true]
        events << [name: "batteryLevel", value: firstBlind['batteryLevel'], displayed: true]

        def dni = firstBlind['encodedMacAddress']
        def child = getChildDevice(dni)


        log.debug("Got json data $jsonData")
        if (child != null) {
            log.debug("Sending Events to child $dni $events")
            child.generateEvent(events) // Update the device status
        } else {
            log.debug "Cound't find child to send events to"
        }
    }
}
/***********************************************
 * child commands sent up
 ************************************************/
// Value is between 0 and 100 -- 50 is 'open' and the other two extremes are closed
// Multiple x2 as smartblinds expect that
def childSetLevel(dni, value) {
    log.debug "childSetLevel ${dni} ${value} "
    adjustBlinds(dni, 2 * value)
}
//curl -H 'Host: api.mysmartblinds.com' -H 'accept: */*' -H 'content-type: application/json' -H 'auth0-client-id: 1sdfsfdsU577QX38GzCJZzm8AFJ' -H 'user-agent: MySmartBlinds/0 CFNetwork/902.2 Darwin/17.7.0' -H 'accept-language: en-us' -H 'authorization: Bearer aFS0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.aFSpc3MiOiJodHRwczovL215c21hcnRibGluZHMuYXV0aDAuY29tLyIsInN1YiI6ImFgdasGgwfDU4Y2M1NjE1NDEzMzY5MDM3MjMzYTE1YyIsImF1ZCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIiwiZXhwIjoxNTM0NTgwNDAzLCJpYXQiOjE1MzQ1NDQ0MDMsImF6cCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIn0.5QL7objl7kOi0tYFYcJFztv2gvKFo9-haqS4RUJe4kE' --data-binary '{"query":"mutation UpdateBlindsPosition($blinds: [String], $position: Int!) {  updateBlindsPosition(encodedMacAddresses: $blinds, position: $position) {    __typename    encodedMacAddress    position    rssi    batteryLevel  }}","variables":{"position":106,"blinds":["2kJr9hLZ"]}}' --compressed 'https://api.mysmartblinds.com/v1/graphql’
// Child On means 'open' blinds (value = 100), 200 is full up and 0 is full down
def childOn(dni, v = 100) {
    log.debug "childOn ${dni} $v"
    adjustBlinds(dni, v)
}
// curl -H 'Host: api.mysmartblinds.com' -H 'accept: */*' -H 'content-type: application/json' -H 'auth0-client-id: asdfadfaasdaspUt1U5asd' -H 'user-agent: MySmartBlinds/0 CFNetwork/902.2 Darwin/17.7.0' -H 'accept-language: en-us' -H 'authorization: Bearer aFS0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.aFSpc3MiOiJodHRwczovLafsdaadsfaZHMuYXV0aDAuY29tLyIsInN1YiI6ImFfdafadGgwfDU4Y2M1NjE1NDEzMzY5MDM3MjMzYTE1YyIsImF1ZCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIiwiZXhwIjoxNTM0NTgwNDAzLCJpYXQiOjE1MzQ1NDQ0MDMsImF6cCI6IjFkMWMzdnVxV3RwVXQxVTU3N1FYNWd6Q0paem04V09CIn0.5QL7objl7kOi0tYFYcJFztv2gvKFo9-haqS4RUJe4kE' --data-binary '{"query":"mutation UpdateBlindsPosition($blinds: [String], $position: Int!) {  updateBlindsPosition(encodedMacAddresses: $blinds, position: $position) {    __typename    encodedMacAddress    position    rssi    batteryLevel  }}","variables":{"position":0,"blinds":["2kJr9hLZ"]}}' --compressed 'https://api.mysmartblinds.com/v1/graphql’
// Child off means closed (in this case position 0 or 200) --
def childOff(dni, v = 0) {
    log.debug "Child Off ${dni} $v"
    adjustBlinds(dni, v)
}