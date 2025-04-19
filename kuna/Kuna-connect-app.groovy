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
 *  Updated: 2025-04-19 - Enhanced logging, error handling, and documentation
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
    section("Kuna Account Credentials") {
        input "myKunaEmail", "email", title: "Email", required: true
        input "myKunaPassword", "password", title: "Password", required: true
    }
    section("Configuration Options") {
        input name: "logLevel", title: "Log Level", type: "enum", 
            options: ["ERROR", "WARN", "INFO", "DEBUG", "TRACE"], 
            defaultValue: "INFO", description: "Set the level of detail for logging"
        input name: "refreshInterval", title: "Refresh Interval (minutes)", type: "number", 
            defaultValue: 30, range: "5..60", description: "How often to refresh device status"
        input name: "autoRetry", title: "Auto Retry on Failure", type: "bool", 
            defaultValue: true, description: "Automatically retry failed API requests"
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

/**
 * Lifecycle Methods
 */

def installed() {
    logger("INFO", "Kuna Connect app installed")
    initialize()
}

def updated() {
    logger("INFO", "Kuna Connect app configuration updated")
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    logger("INFO", "Initializing Kuna Connect")
    // Authenticate with Kuna API
    logInToKuna()
    
    // Schedule daily token refresh
    schedule("0 15 10 ? * *", logInToKuna)
    
    // Schedule periodic device status refresh based on user preference
    if (refreshInterval) {
        def minutes = refreshInterval.toInteger()
        logger("DEBUG", "Setting refresh schedule to run every ${minutes} minutes")
        schedule("0 */${minutes} * * * ?", refreshDevices)
    }
    
    // Schedule a token check every 6 hours to ensure we have a valid token
    schedule("0 0 */6 ? * *", checkTokenValidity)
}

def uninstalled() {
    logger("INFO", "Uninstalling Kuna Connect")
    unsubscribe()
    unschedule()
    getChildDevices().each {
        logger("DEBUG", "Removing child device: ${it.deviceNetworkId}")
        deleteChildDevice(it.deviceNetworkId)
    }
}


def getDevices() {
    state.devices = state.devices ?: [:]
}


/**
 * API Communication Methods
 */

/**
 * Authenticate with the Kuna API and get token
 */
def logInToKuna() {
    logger("DEBUG", "Logging in to Kuna API")

    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : '/api/v1/account/auth/',
            requestContentType : 'application/json',
            headers: [
                    'Content-Type': 'application/json',
                    'User-Agent'  : 'Luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    'Accept' : '*/*',
            ],
            body   : [
                "email"   : myKunaEmail,
                "password": myKunaPassword
            ]
    ]

    logger("DEBUG", "Authenticating with Kuna API")
    asynchttpPost(authTokenHandler, params)
}

/**
 * Handler for authentication API response
 * @param response - The API response
 * @param data - Additional data passed to the handler
 */
def authTokenHandler(response, data) {
    if (response.hasError()) {
        logger("ERROR", "Authentication failed: ${response.getErrorMessage()}")
        state.authenticationFailed = true
        
        // Retry authentication if enabled and this wasn't already a retry
        if (settings.autoRetry && !(data?.isRetry)) {
            logger("WARN", "Will retry authentication in 60 seconds")
            runIn(60, retryAuthentication)
        }
    } else {
        try {
            def jsonData = response.json
            state.token = jsonData['token']
            state.lastTokenRefresh = now()
            state.authenticationFailed = false
            logger("INFO", "Authentication successful")
            
            // Get cameras after successful authentication
            getAllCameras()
        } catch (e) {
            logger("ERROR", "Error parsing authentication response: ${e.message}")
        }
    }
}

/**
 * Retry failed authentication
 */
def retryAuthentication() {
    logger("INFO", "Retrying authentication")
    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : '/api/v1/account/auth/',
            requestContentType : 'application/json',
            headers: [
                    'Content-Type': 'application/json',
                    'User-Agent'  : 'Luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    'Accept' : '*/*',
            ],
            body   : [
                "email"   : myKunaEmail,
                "password": myKunaPassword
            ]
    ]
    
    // Pass isRetry flag to avoid infinite retry loops
    asynchttpPost([authTokenHandler, [isRetry: true]], params)
}

/**
 * Get all cameras from the Kuna API and create child devices
 */
private getAllCameras() {
    // Don't proceed if no token is available
    if (!state.token) {
        logger("WARN", "No authentication token available, skipping camera refresh")
        return
    }
    
    logger("DEBUG", "Fetching cameras from Kuna API")

    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : '/api/v1/user/cameras/',
            requestContentType : 'application/json',
            headers: [
                    'Content-Type' : 'application/json',
                    'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "authorization": "Token ${state.token}",
            ],
            body   : ""
    ]

    asynchttpGet(getAllCamerasHandler, params)
}

/**
 * Handler for get all cameras API response
 * @param response - The API response
 * @param data - Additional data passed to the handler
 */
def getAllCamerasHandler(response, data) {
    if (response.hasError()) {
        def errorCode = response.getStatus()
        logger("ERROR", "Failed to get cameras: ${response.getErrorMessage()} (${errorCode})")
        
        // If unauthorized, try to refresh token
        if (errorCode == 401) {
            logger("WARN", "Unauthorized response, refreshing token")
            state.token = null
            runIn(5, logInToKuna)
            return
        }
    } else {
        try {
            def jsonData = response.json
            def cameras = jsonData['results']
            logger("INFO", "Found ${cameras.size()} Kuna cameras")
            
            // Process each camera and create/update child devices
            cameras.each { camera ->
                createOrUpdateCameraDevice(camera)
            }
            
            // Record the last successful refresh time
            state.lastCameraRefresh = now()
        } catch (e) {
            logger("ERROR", "Error parsing cameras response: ${e.message}")
        }
    }
}

/**
 * Create or update a camera device
 * @param deviceData - Camera data from the API
 */
private createOrUpdateCameraDevice(deviceData) {
    def dni = deviceData.serial_number
    def label = "Kuna: ${deviceData.name}"
    
    try {
        def childDevice = getChildDevice(dni)
        if (!childDevice) {
            logger("INFO", "Creating new Kuna device: ${deviceData.name} (${dni})")
            childDevice = addChildDevice("loghound", "Kuna Light", dni, location.hubs[0].id, [
                    "label": label,
                    "data" : deviceData
            ])
        } else {
            logger("DEBUG", "Updating existing Kuna device: ${deviceData.name} (${dni})")
            childDevice.label = label
        }
        
        if (childDevice) {
            // Update device state
            def events = [:]
            def onOff = deviceData.bulb_on == true ? "on" : "off"
            events << [name: "switch", value: onOff, displayed: true]
            childDevice.generateEvent(events)
        }
    } catch (e) {
        logger("ERROR", "Error creating/updating device ${deviceData.name}: ${e.message}")
    }
}

/**
 * Enable or disable global camera notifications
 * @param enabled - Whether notifications should be enabled
 */
def globalCameraNotificationEnabled(enabled) {
    logger("DEBUG", "Setting global notifications to: ${enabled}")
    
    // Create properly formatted JSON
    def bodyMap = [
        profile: [
            notifications_enable_at: null,
            notifications_enabled: enabled
        ]
    ]
    
    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : '/api/v1/user/',
            requestContentType : 'application/json',
            headers: [
                    'Content-Type' : 'application/json',
                    'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "authorization": "token ${state.token}",
            ],
            body   : bodyMap
    ]

    asynchttpPatch(globalCameraNotificationEnabledHandler, params)
}

/**
 * Turn Kuna light on or off
 * @param dni - Device network ID
 * @param enabled - Whether the light should be on
 */
private kunaLightsOn(dni, enabled) {
    logger("DEBUG", "Setting light for ${dni} to: ${enabled ? "ON" : "OFF"}")

    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : "/api/v1/cameras/${dni}/",
            requestContentType : 'application/json',
            headers: [
                    'Content-Type' : 'application/json',
                    'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
                    "authorization": "token ${state.token}",
            ],
            body   : [bulb_on: enabled]
    ]

    asynchttpPatch(kunaLightsOnHandler, params)
}

/**
 * Refresh a specific Kuna device
 * @param dni - Device network ID
 */
private kunaRefresh(dni) {
    logger("DEBUG", "Refreshing device: ${dni}")

    def params = [
            uri    : 'https://server.kunasystems.com',
            path   : "/api/v1/cameras/${dni}/",
            requestContentType : 'application/json',
            query  : [live: 1],
            headers: [
                    'User-Agent'   : 'luna/2.4.0 (iPhone; iOS 11.4; Scale/3.00)',
                    "Authorization": "Token ${state.token}",
            ],
            body   : ""
    ]

    asynchttpGet(kunaLightsOnHandler, params)
}

/**
 * Handlers for API responses
 */

/**
 * Handler for light control API response
 * @param response - The API response
 * @param data - Additional data passed to the handler
 */
def kunaLightsOnHandler(response, data) {
    if (response.hasError()) {
        logger("ERROR", "Light control failed: ${response.getErrorMessage()}")
        
        // If unauthorized, try to refresh token
        if (response.getStatus() == 401) {
            logger("WARN", "Unauthorized response, refreshing token")
            state.token = null
            runIn(5, logInToKuna)
        }
    } else {
        try {
            def jsonData = response.json
            logger("DEBUG", "Light control response: ${jsonData}")
            
            // Refresh the device to get the updated status
            runIn(2, refreshDevices)
        } catch (e) {
            logger("ERROR", "Error parsing light control response: ${e.message}")
        }
    }
}

/**
 * Handler for global notification setting API response
 * @param response - The API response
 * @param data - Additional data passed to the handler
 */
def globalCameraNotificationEnabledHandler(response, data) {
    if (response.hasError()) {
        logger("ERROR", "Setting global notifications failed: ${response.getErrorMessage()}")
        
        // If unauthorized, try to refresh token
        if (response.getStatus() == 401) {
            logger("WARN", "Unauthorized response, refreshing token")
            state.token = null
            runIn(5, logInToKuna)
        }
    } else {
        try {
            def jsonData = response.json
            logger("INFO", "Global notifications updated successfully")
            logger("DEBUG", "Notification response: ${jsonData}")
        } catch (e) {
            logger("ERROR", "Error parsing notification response: ${e.message}")
        }
    }
}

/**
 * Get device health status
 * @return A map with device health information
 */
def getApiStatus() {
    def status = [
        lastTokenRefresh: state.lastTokenRefresh ? new Date(state.lastTokenRefresh).toString() : "Never",
        lastCameraRefresh: state.lastCameraRefresh ? new Date(state.lastCameraRefresh).toString() : "Never",
        tokenPresent: state.token ? true : false,
        deviceCount: getChildDevices().size(),
        authenticationFailed: state.authenticationFailed ?: false
    ]
    
    return status
}

/**
 * Improved image capture functionality
 * @param dni - Device network ID of the camera
 */
def take(dni) {
    logger("DEBUG", "Requesting image from Kuna camera: ${dni}")
    
    if (!state.token) {
        logger("ERROR", "Cannot capture image: No valid token")
        runIn(5, logInToKuna)
        return
    }
    
    def params = [
        uri    : 'https://server.kunasystems.com',
        path   : "/api/v1/cameras/${dni}/thumbnail/",
        headers: [
            'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
            "Authorization": "Token ${state.token}",
            'Accept'       : 'image/jpeg'
        ]
    ]

    try {
        httpGet(params) { response ->
            if (response.status == 200 && response.headers.'Content-Type'.contains("image/jpeg")) {
                def imageBytes = response.data
                if (imageBytes) {
                    def camera = getChildDevice(dni)
                    logger("INFO", "Image received, sending to device: ${camera?.displayName}")
                    camera.childStoreImage(imageBytes)
                } else {
                    logger("ERROR", "No image data received")
                }
            } else {
                logger("ERROR", "Image response not successful (${response.status}) or wrong format (${response.headers.'Content-Type'})")
            }
        }
    } catch (Exception e) {
        logger("ERROR", "Error capturing image: ${e.message}")
        if (e.message.contains("401")) {
            logger("WARN", "Unauthorized response when capturing image, refreshing token")
            state.token = null
            runIn(5, logInToKuna)
        }
    }
}

/***********************************************
 * child commands sent up
 ************************************************/


// Child On means 'open' cameras (value = 100), 200 is full up and 0 is full down
def childOn(dni) {
    logger("DEBUG", "Child On ${dni}")
    kunaLightsOn(dni, true)
}


// Child off means closed (in this case position 0 or 200) --
def childOff(dni) {
    logger("DEBUG", "Child Off ${dni}")
    kunaLightsOn(dni, false)
}

def childRefresh(dni) {
    kunaRefresh(dni)
}


// Images -- not so much a hubitat thing.....
def take(dni) {
    logger("DEBUG", "Requesting image from Kuna camera: ${dni}")
    
    if (!state.token) {
        logger("ERROR", "Cannot capture image: No valid token")
        runIn(5, logInToKuna)
        return
    }
    
    def params = [
        uri    : 'https://server.kunasystems.com',
        path   : "/api/v1/cameras/${dni}/thumbnail/",
        headers: [
            'User-Agent'   : 'luna/2.3.6 (iPhone; iOS 11.4; Scale/3.00)',
            "Authorization": "Token ${state.token}",
            'Accept'       : 'image/jpeg'
        ]
    ]

    try {
        httpGet(params) { response ->
            if (response.status == 200 && response.headers.'Content-Type'.contains("image/jpeg")) {
                def imageBytes = response.data
                if (imageBytes) {
                    def camera = getChildDevice(dni)
                    logger("INFO", "Image received, sending to device: ${camera?.displayName}")
                    camera.childStoreImage(imageBytes)
                } else {
                    logger("ERROR", "No image data received")
                }
            } else {
                logger("ERROR", "Image response not successful (${response.status}) or wrong format (${response.headers.'Content-Type'})")
            }
        }
    } catch (Exception e) {
        logger("ERROR", "Error capturing image: ${e.message}")
        if (e.message.contains("401")) {
            logger("WARN", "Unauthorized response when capturing image, refreshing token")
            state.token = null
            runIn(5, logInToKuna)
        }
    }
}

/**
 * Utility Methods
 */

/**
 * Logging utility that honors the user's log level preference
 * @param level - The level of the log message (ERROR, WARN, INFO, DEBUG, TRACE)
 * @param message - The message to log
 */
private void logger(String level, String message) {
    // If no level is specified in preferences, default to INFO
    def selectedLogLevel = settings.logLevel ?: "INFO"
    
    // Define log level order for comparison
    def logLevels = ["ERROR": 1, "WARN": 2, "INFO": 3, "DEBUG": 4, "TRACE": 5]
    
    // Only log if the current log level is less than or equal to the selected level
    if (logLevels[level] <= logLevels[selectedLogLevel]) {
        switch(level) {
            case "ERROR":
                log.error(message)
                break
            case "WARN":
                log.warn(message)
                break
            case "INFO":
                log.info(message)
                break
            case "DEBUG":
                log.debug(message)
                break
            case "TRACE":
                log.trace(message)
                break
            default:
                log.debug(message)
                break
        }
    }
}

/**
 * Check if the token is still valid and refresh it if needed
 */
def checkTokenValidity() {
    logger("DEBUG", "Checking Kuna API token validity")
    
    // Refresh token if not set or if no lastTokenRefresh timestamp
    if (!state.token || !state.lastTokenRefresh) {
        logger("INFO", "Token missing or no timestamp - requesting new token")
        logInToKuna()
        return
    }
    
    // Calculate token age in milliseconds
    def tokenAge = now() - state.lastTokenRefresh
    
    // Refresh token if it's older than 20 hours (72,000,000 milliseconds)
    if (tokenAge > 72000000) {
        logger("INFO", "Token is ${tokenAge/3600000} hours old - refreshing")
        logInToKuna()
    } else {
        logger("DEBUG", "Token is still valid (${tokenAge/3600000} hours old)")
    }
}

/**
 * Trigger a refresh of all connected devices
 */
def refreshDevices() {
    logger("DEBUG", "Refreshing all Kuna devices")
    getAllCameras()
}


