/*
*  Copyright 2019 SanderSoft
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
*  Ambient Weather Station
*
*  Author: Kurt Sanders
*
*  Date: 2018,2019
*/
import groovy.time.*
import java.text.DecimalFormat
import groovy.time.TimeCategory

String DTHDNI() 				{ return "MyAmbientWeatherStation" }
String DTHDNIActionTiles() 		{ return "MyAmbientWeatherActionTiles" }
String DTHDNIRemoteSensorName() { return "remoteTempfHumiditySensorName"}
String version()				{ return "V3.1.0" }
String appModified()			{ return "Jan-22-2019"}
String appName() 				{ return "Ambient Weather Station Service Manager ${version()}" }
String appShortName() 			{ return "STAmbientWeather ${version()}" }
String AWSNameActionTilesHide()	{ return false }

String DTHName() 				{ return (noColorTiles)?"Ambient Weather Station V3 No Color Tiles":"Ambient Weather Station V3" }
String DTHRemoteSensorName() 	{ return "Ambient Weather Station Remote Sensor V3"}

String DTHNameActionTiles() 	{ return "SmartWeather Station Tile" }
String AWSNameActionTiles()		{ return "Ambient SmartWeather ${version()}" }

String DTHnamespace()			{ return "kurtsanders" }
String appAuthor()	 			{ return "SanderSoft" }
String getAppImg(imgName) 		{ return "https://raw.githubusercontent.com/KurtSanders/STAmbientWeather/master/images/$imgName" }
String wikiURL(pageName)		{ return "https://github.com/KurtSanders/STAmbientWeather/wiki/$pageName"}
// ============================================================================================================
// This APP key is ONLY for this application - Do not copy or use elsewhere
String appKey() 				{return "33054086b3d745779f5ac35e147baa76f13e75d44ea245388ba598911905fb50"}
// ============================================================================================================

definition(
    name: 			"Ambient Weather Station Service Manager V3",
    namespace: 		"kurtsanders",
    author: 		"kurt@kurtsanders.com",
    description: 	"Ambient Personal Weather Station Service Manager ${version()}",
    category: 		"My Apps",
    iconUrl:   		getAppImg("blue-ball-100.jpg"),
    iconX2Url: 		getAppImg("blue-ball-200.jpg"),
    iconX3Url: 		getAppImg("blue-ball.jpg"),
    singleInstance: true
)
{
// The following API Key is to be entered in this SmartApp's settings in the SmartThings IDE App Setting.
    appSetting 		"apiKey"
}
preferences {
    page(name: "mainPage")
    page(name: "optionsPage")
    page(name: "remoteSensorPage")
    page(name: "notifyPage")
}

def mainPage() {
    def apiappSetupCompleteBool = appSettings.apiKey?true:false
    def setupMessage = ""
    def setupTitle = "${appName()} API Check"
    def nextPageName = "optionsPage"
    state.retry = 0
    def getAmbientStationDataRC = getAmbientStationData()
    if ( (apiappSetupCompleteBool) && (getAmbientStationDataRC) ) {
        setupMessage = "SUCCESS! You have completed entering a valid Ambient API Key for ${appName()}. "
        setupMessage += (weatherStationMac)?"Please Press 'Next' for additional configuration choices.":"I found ${state.ambientMap.size()} reporting weather station(s)."
        setupTitle = "Please confirm the Ambient Weather Station Information below and if correct, Tap 'NEXT' to continue to the 'Settings' page'"
    } else {
        setupMessage = "Setup Incomplete: Please check and/or complete the REQUIRED API key setup in the SmartThings IDE (App Settings Section) for ${appName()}"
        nextPageName = null
    }
    dynamicPage(name: "mainPage", title: setupTitle, submitOnChange: true, nextPage: nextPageName, uninstall:true, install:false) {
        section(hideable: apiappSetupCompleteBool, hidden: apiappSetupCompleteBool, setupMessage ) {
            paragraph "The API string key is used to securely connect your weather station to ${appName()}."
            paragraph image: getAppImg("blue-ball.jpg"),
                title: "Required API Key",
                required: false,
                informationList("apiHelp")
            href(name: "hrefReadme",
                 title: "${appName()} Setup/Read Me Page",
                 required: false,
                 style: "external",
                 url: "https://github.com/KurtSanders/STAmbientWeather",
                 description: "tap to view the Setup/Read Me page")
            href(name: "hrefAmbient",
                 title: "Ambient Weather Dashboard Account Page for API Key",
                 required: false,
                 style: "external",
                 url: "https://dashboard.ambientweather.net/account",
                 description: "tap to login and view your Ambient Weather's dashboard")
            href(name: "hrefUSA",
                 title: "SmartThings IDE USA",
                 required: false,
                 style: "external",
                 url: "https://graph.api.smartthings.com/",
                 description: "tap to view the US SmartThings IDE website in mobile browser")
            href(name: "hrefEurope",
                 title: "SmartThings IDE Europe",
                 required: false,
                 style: "external",
                 url: "https://graph-eu01-euwest1.api.smartthings.com/",
                 description: "tap to view the Europe SmartThings IDE website in mobile browser")
        }
        if (apiappSetupCompleteBool && getAmbientStationDataRC) {
            if (weatherStationMac) {
                setStateWeatherStationData()
                state.weatherStationMac = weatherStationMac
                state.countRemoteTempHumiditySensors =  state.ambientMap[state.weatherStationDataIndex].lastData.keySet().count { it.matches('temp[0-9]f') }
                section ("Ambient Weather Station Information") {
                    paragraph image: getAppImg("blue-ball.jpg"),
                        title: "${state.ambientMap[state.weatherStationDataIndex].info.name}",
                        required: false,
                        "Location: ${state?.ambientMap[state.weatherStationDataIndex].info.location}" +
                        "\nMac Address: ${state.ambientMap[state.weatherStationDataIndex].macAddress}" +
                        "\nRemote Temp/Hydro Sensors: ${state.countRemoteTempHumiditySensors}"
                }
            } else {
                def weatherStationList = [:]
                state.ambientMap.each {
                    weatherStationList << [[ "${it.macAddress}" : "${it.info.name} @ ${it.info.location}" ]]
                }
                section ("Ambient Weather Station Information") {
                    input name: "weatherStationMac", submitOnChange: true, type: "enum",
                        title: "Select the Weather Station to Install",
                        options: weatherStationList,
                        multiple: false,
                        required: true
                }
            }
        }
        section ("STAmbientWeather™ - ${appAuthor()}") {
            href(name: "hrefVersions",
                 image: getAppImg("wi-direction-right.png"),
                 title: "${version()} : ${appModified()}",
                 required: false,
                 style:"embedded",
                 url: wikiURL("Features-by-Version")
                )
        }
    }
}

def optionsPage () {
    log.info "Ambient Weather Station Selected = Mac: ${weatherStationMac}, Name/Loc: ${state.ambientMap[state.weatherStationDataIndex].info.name}/${state.ambientMap[state.weatherStationDataIndex].info.location}"
    def remoteSensorsExist = (state.countRemoteTempHumiditySensors>0)
    def lastPageName = remoteSensorsExist?"remoteSensorPage":""
    dynamicPage(name: "optionsPage", title: "Ambient Tile Settings for: '${state.ambientMap[state.weatherStationDataIndex].info.name}'",
                nextPage: lastPageName,
                uninstall:false,
                install : !remoteSensorsExist ) {
        section("Weather Station Options") {
            input "zipCode", type: "number",
                title: "Enter ZipCode for local Weather API Forecast/Moon (Required)",
                required: true
            input name: "schedulerFreq", type: "enum",
                title: "Run Weather Station Refresh Every (mins)?",
                options: ['0','1','2','3','4','5','10','15','30','60','180'],
                required: true
            input "${DTHDNIRemoteSensorName()}0", type: "text",
                title: "Weather Station Console Room Location Short Name",
                required: true
            input name: "solarRadiationTileDisplayUnits", type: "enum",
                title: "Select Solar Radiation ('Light' Tile) Units of Measure",
                options: ['W/m²','lux', 'fc'],
                required: true
            if ( (!state.deviceId) && (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) ) {
                input name: "createActionTileDevice", type: "bool",
                    title: "Create ${DTHNameActionTiles()} for use as ActionTiles™ SmartWeather Station Tile?",
                    required: false
            }
            href(name: "Define Weather Alerts/Notification",
                 title: "Weather Alerts/Notification",
                 required: false,
                 page: "notifyPage")
        }
        if (!state.deviceId) {
            section {
                paragraph "Background Color Option for Displaying Values"
                paragraph image: getAppImg("No-Color-Option.jpg"),
                    title: "Initial ANDROID Install Setup Choice",
                    required: false,
                    "This NO COLOR backgrounds option is highly recommended to set ON for Android devices.'"
                input name: "noColorTiles", type: "bool",
                    title: "Remove Color Backgrounds in Tiles (Recommended ON for Android Users)",
                    required: false
            }
        }
        section(hideable: true, hidden: true, "Optional: SmartThings IDE Live Logging Levels") {
            input name: "debugVerbose", type: "bool",
                title: "Show Debug Messages in Live Logging IDE",
                required: false
            input name: "infoVerbose", type: "bool",
                title: "Show Info Messages in Live Logging IDE",
                required: false
            input name: "WUVerbose", type: "bool",
                title: "Show Local Weather Info Messages in Live Logging IDE",
                required: false
        }
    }
}
def remoteSensorPage() {
    dynamicPage(name: "remoteSensorPage", title: "Ambient Tile Settings", uninstall:false, install : true ) {
        def i = 1
        section("Provide Location names for your ${state?.countRemoteTempHumiditySensors} remote temperature/hydro sensors") {
            paragraph "Information Regarding Ambient Remote Sensors"
            paragraph image: getAppImg("blue-ball.jpg"),
                title: "Information, Issues and Instructions",
                required: false,
                "You MUST create short descriptive names for each remote sensor. Do not use special characters in the names.\n\n" +
                "If you wish to change the short name of the remote sensor, DO NOT change or rename them in the Device Tile or ST IDE 'My Devices' editor, as this app will rename them automatically when you SAVE the page.\n\n" +
                "Please note that remote sensors are numbered based in the bit switch on the sensor (1-8) and reported on Ambient Network API as 'tempNf' where N is an integer 1-8.  " +
                "If a remote sensor is deleted from your network or non responsive from your group of Ambient remote sensors, you may have to re-verify and/or rename the remainder of the remote sensors in this app and manually delete that sensor from the IDE 'My Devices' editor."
            for (i; i <= state?.countRemoteTempHumiditySensors; i++) {
                input "${DTHDNIRemoteSensorName()}${i}", type: "text",
                    title: "Ambient Remote Sensor #${i}",
                    required: true
            }
        }
    }
}

def notifyPage() {
    dynamicPage(name: "notifyPage", title: "Weather Alerts/Notification", uninstall: false, install: false) {
        section("Mobile SMS Notify Options") {
            input name: "mobilePhone", type: "phone",
                title: "Required: Enter the mobile phone number to receive SMS weather events. Leave field blank to cancel all notifications",
                required: true
            input name: "notifyAlertFreq", type: "enum",
                required: true,
                title: "Notify via SMS once every NUMBER of hours (Default is 24, Once/day)",
                options: [1,2,4,6,12,24],
                multiple: false

        }
        section ("Weather Station Notify Options") {
            input name: "notifySevereAlert", type: "bool", required: false,
                title: "Notify when a SEVERE weather related ALERT is issued for your zipcode"
            if ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) ) {
                input name: "notifyAlertLowTemp", type: "number", required: false,
                    title: "Notify when a temperature value is EQUAL OR BELOW this value. Leave field blank to cancel notification."
            }
            if ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) ) {
                input name: "notifyAlertHighTemp", type: "number", required: false,
                    title: "Notify when a temperature value is EQUAL OR ABOVE this value.  Leave field blank to cancel notification."
            }
            if ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('hourlyrainin')) ) {
                input name: "notifyRain", type: "bool", required: false,
                    title: "Notify when RAIN is detected"
            }
        }
        section (hideable: true, hidden: true, "Last SMS Notifications") {
            paragraph image: "",
                required: false,
                SMSNotifcationHistory()
        }
    }
}


def initialize() {
    def now = now()
    if(infoVerbose){log.info "initialize Section: Start"}
    unsubscribe()
    subscribe(app, appTouchHandler)
    if(infoVerbose){
        log.info "Ambient API string-> ${appSettings?.apiKey}"
        log.info "The location zip code for your Hub Location is '${location.zipCode}' and your preference zipCode value is '${zipCode}'"
    }

    // Initialize/Reset Alert Warnings DateTime values
    state.notifyAlertLowTempDT 		= state.notifyAlertLowTempDT?:now
    state.notifyAlertHighTempDT 	= state.notifyAlertHighTempDT?:now
    state.notifyRainDT 				= state.notifyRainDT?:now
    state.notifySevereAlertDT 		= state.notifySevereAlertDT?:now
    state.notifyAlertFreq 			= notifyAlertFreq?:24

    // Check for all devices needed to run this app
    addAmbientChildDevice()
    // Set user defined refresh rate
    if(state.schedulerFreq!=schedulerFreq) {
        log.info "Updating your Cron REFRESH schedule from ${state.schedulerFreq} mins to ${schedulerFreq} mins"
        state.schedulerFreq = schedulerFreq
        if(debugVerbose){log.debug "state.schedulerFreq->${state.schedulerFreq}"}
        setScheduler(schedulerFreq)
        def d = getChildDevice(state.deviceId)
        d.sendEvent(name: "scheduleFreqMin", value: state.schedulerFreq)
    }
    log.info "initialize Section: End"
}

def installed() {
    log.info "Installed Section: Start"
    state.deviceId = DTHDNI()
    initialize()
    runIn(10, main)
    log.info "Installed Section: End"
}

def updated() {
    log.info "Updated Section: Start"
	initialize()
    log.info "Updated Section: End"
}

def uninstalled() {
    log.info "Section Started: Uninstalled"
    log.info "Removing Cron Job for Refresh"
    unschedule()
    // Remove all devices
    getAllChildDevices().each {
        log.info "Deleting AmbientWS device: ${it.label}"
        deleteChildDevice(it.deviceNetworkId)
    }
    log.info "Section Ended: Uninstalled"
}

def scheduleCheckReset() {
    if (schedulerFreq!='0'){
        Date start = new Date()
        Date end = new Date()
        use( TimeCategory ) {
            end = start + schedulerFreq.toInteger().minutes
        }
        setScheduler(schedulerFreq)
        log.info "Reset the next CRON Refresh to ~${schedulerFreq} mins from now (${end.format("h:mm:ss a", location.timeZone)}) to avoid excessive HTTP requests"
    }
}

def appTouchHandler(evt="") {
    def timeStamp = new Date().format("h:mm:ss a", location.timeZone)
    log.info "App Touch: 'Refresh ALL' requested at ${timeStamp}"
    scheduleCheckReset()
    main()
}

def refresh() {
    log.info "Device: 'Refresh ALL'"
    scheduleCheckReset()
    main()
}

def autoScheduleHandler() {
    log.info "Executing Cron Schedule every ${schedulerFreq} min(s): 'Refresh ALL'"
    main()
}

def main() {
    def runID = new Random().nextInt(10000)
    if (state?.runID == runID as String) {
        log.warn "DUPLICATE EXECUTION RUN AVOIDED: Current runID: ${runID} Past runID: ${state?.runID}"
        return
    }
    state.runID = runID

    log.info "Main (#${runID}) Section: Executing Local Weather for: ${zipCode} & Ambient Weather Station API's for: '${state.ambientMap[state.weatherStationDataIndex].info.name}'"

    // TWC Local Weather
    localWeatherInfo()

    // TWC Local Weather Alerts
    checkForSevereWeather()

    // Ambient Weather Station API
    ambientWeatherStation()

    // Notify Events Check
    notifyEvents()
}

def localWeatherInfo() {

    if(infoVerbose){log.info "Executing 'localWeatherInfo', zipcode: ${zipCode}"}
    def d = getChildDevice(state.deviceId)
    d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)

    if(infoVerbose){log.info "Getting TWC Current Weather Conditions"}
    def obs = getTwcConditions(zipCode)
    state.weatherIcon = obs?.iconCode as String
    state.wxPhraseShort = obs?.wxPhraseShort
    if (obs) {
        d.sendEvent(name: "weatherIcon", value: state.weatherIcon, displayed: false)
    }

    if(infoVerbose){log.info "Getting TWC Location Info for ${zipCode}"}
    def loc = getTwcLocation(zipCode)?.location
    state.cityValue = "${loc?.city}, ${loc?.adminDistrictCode} ${loc.countryCode}"
    state.latitude = "${loc?.latitude}"
    state.longitude = "${loc?.longitude}"

    //Getting Sunrise & Sunset
    def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def sunriseDate = dtf.parse(obs.sunriseTimeLocal)
    def sunsetDate = dtf.parse(obs.sunsetTimeLocal)

    def tf = new java.text.SimpleDateFormat("h:mm")
    tf.setTimeZone(TimeZone.getTimeZone(loc.ianaTimeZone))

    state.localSunrise = "${tf.format(sunriseDate)}"
    state.localSunset = "${tf.format(sunsetDate)}"

    d.sendEvent(name: "localSunrise", value: state.localSunrise , displayed: false)
    d.sendEvent(name: "localSunset" , value: state.localSunset  , displayed: false)

    // Get the Weather Forecast
    if(infoVerbose){log.info "Getting TWC Forecast for ${state.cityValue}"}
    def f = getTwcForecast(zipCode)
    if (f) {
        state.forecastIcon = f.daypart[0].iconCode[0] ?: f.daypart[0].iconCode[1]
        state.precipChance = f.daypart[0].precipChance[0] ?: f.daypart[0].precipChance[1]
        def rainType = f.daypart[0].precipType[0] ?: f.daypart[0].precipType[1]
        def windPhrase = f.daypart[0].windPhrase[0] ?: f.daypart[0].windPhrase[1]
        d.sendEvent(name: "moonPhase", value: "Lunar Day: ${f.moonPhaseDay[0]}\n${f.moonPhase[0]}", displayed: false)
        d.sendEvent(name: "moonAge", value: "${f.moonPhaseDay[0]}", displayed: false)
        d.sendEvent(name: "rainForecast", value: "${rainType.capitalize()}\n${state.precipChance}%", displayed: false)
        d.sendEvent(name: "windPhrase", value: "${windPhrase}", displayed: false)

        def narrative = f.daypart[0].narrative
        def daypartName = f.daypart[0].daypartName
        def f2= sprintf(
            "%s forecast for %s\n%s %s %s %s %s %s",
            f.dayOfWeek[0],
            state.cityValue,
            daypartName[0]?:"",
            narrative[0]?narrative[0].uncapitalize():"",
            daypartName[1]?:"",
            narrative[1]?narrative[1].uncapitalize():"",
            daypartName[2]?:"",
            narrative[2]?narrative[2].uncapitalize():"",
        )
        d.sendEvent(name: "weather", value: f2, displayed: false)
    }
    else {
        log.warn "TWC Forecast Data not provided by API"
    }
}

def checkForSevereWeather() {
    if(infoVerbose){log.info "Getting TWC Alerts"}
    def d = getChildDevice(state.deviceId)
    def alertMsg = []
    def alertDescription = []
    def alerts = getTwcAlerts()
    def msg = ""
    if (alerts) {
        alerts.eachWithIndex {alert, i ->
            msg += "${alert.headlineText}"
            if (alert.effectiveTimeLocal && !msg.contains(" from ")) {
                msg += " from ${parseAlertTime(alert.effectiveTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.effectiveTimeLocalTimeZone))}"
            }
            if (alert.expireTimeLocal && !msg.contains(" until ")) {
                msg += " until ${parseAlertTime(alert.expireTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.expireTimeLocalTimeZone))}"
            }
            alertMsg << msg
            def detailKey = alert.detailKey
            def TwcAlertDetail = getTwcAlertDetail(detailKey)
            alertDescription << TwcAlertDetail.alertDetail.texts.description.join(",").replaceAll("[\\t\\n\\r&]+"," ")
            msg = ""
        }
    } else {
        // Time Stamp
        def timeStamp = new Date().format("h:mm:ss a", location.timeZone)
        alertMsg = "No current weather alerts for ${state.cityValue} at ${timeStamp}\nlatitude = ${state.latitude}º\nlongitude = ${state.longitude}º"
    }
    if(infoVerbose){log.info "Alert msg: ${alertMsg}"}
    if(infoVerbose){log.info "Alert description: ${alertDescription}"}
    d.sendEvent(name: "alertMessage", value: informationList(alertMsg), displayed: false)
    d.sendEvent(name: "alertDescription", value: informationList(alertDescription), displayed: false)
    if ( (mobilePhone) && (notifySevereAlert) && (alerts) ) {
        if (lastNotifyDT(state.notifySevereAlertDT, "${alerts.size()} Weather Alert(s)")) {
            msg = "${appShortName()}: SEVERE WEATHER ALERT: ${alertMsg.join(', ')}"
            if(debugVerbose){log.debug "SMS: ${msg}"}
            state.notifySevereAlertDT = now()
            sendNotification("${msg}", [method: "both", phone: mobilePhone])
        }
    }
}

def ambientWeatherStation() {
    // Ambient Weather Station
    if(infoVerbose){log.info "Ambient Weather Station Reporter: Executing 'Refresh Routine' auto every: ${schedulerFreq} min(s)"}
    def d = getChildDevice(state.deviceId)
    def okTOSendEvent = true
    def remoteSensorDNI = ""
    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    def nowTime = new Date().format('h:mm a',location.timeZone).toLowerCase()
    def currentDT = new Date()
    def tempUnits = getTemperatureScale()
    def windUnits = tempUnits == "C" ? "KPH" : "MPH"
    def measureUnits = tempUnits == "C" ? "CM" : "IN"
    def baroUnits = tempUnits == "C" ? "MMHG" : "INHg"
    def sendEventOptions = ""
    if (getAmbientStationData()) {
        if(debugVerbose){log.debug "httpget resp status = ${state.respStatus}"}
        if(infoVerbose){log.info "Processing Ambient Weather data returned from getAmbientStationData())"}
        setStateWeatherStationData()
        if(debugVerbose || infoVerbose) {
            state.ambientMap[state.weatherStationDataIndex].each{ k, v ->
                log.info "${k} = ${v}"
                if (k instanceof Map) {
                    k.each { x, y ->
                        log.info "${x} = ${y}"
                    }
                }
                if (v instanceof Map) {
                    v.each { x, y ->
                        log.info "${x} = ${y}"
                    }
                }
            }
        }
        if(debugVerbose){log.debug "Checking Weather Station data array for 'Last Rain Date' information..."}
        if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('lastRain')) {
            if(debugVerbose){log.debug "Weather Station has 'Last Rain Date' information...Processing"}
            def dateRain = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", state.ambientMap[state.weatherStationDataIndex].lastData.lastRain)
            use (groovy.time.TimeCategory) {
                if(debugVerbose){log.debug ("lastRainDuration -> ${currentDT - dateRain}")}
                def lastRainDuration = currentDT - dateRain
                if (lastRainDuration) {
                    d.sendEvent(name:"lastRainDuration", value: lastRainDuration, displayed: false)
                }
            }
        } else {
            if(debugVerbose){log.debug "Weather Station does NOT provide 'Last Rain Date' information...Skipping"}
            d.sendEvent(name:"lastRainDuration", value: "N/A", displayed: false)
        }
        if ((state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('totalrainin')==false) || (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('yearlyrainin')==false)) {
            d.sendEvent(name:"totalrainin", value: "N/A", unit: measureUnits, displayed: false)
        }
        d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
        d.sendEvent(name:"macAddress", value: state.ambientMap[state.weatherStationDataIndex].macAddress, displayed: false)

        if (!state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('feelsLike')) {
            if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('humidity')) {
                d.sendEvent(name: 'secondaryControl', value: sprintf("Humidity is %s%% at %s", state.ambientMap[state.weatherStationDataIndex].lastData.humidity, nowTime), displayed: false )
            } else {
                d.sendEvent(name: 'secondaryControl', value: "", displayed: false )
            }
        }

        state.ambientServerDate=Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", state.ambientMap[state.weatherStationDataIndex].lastData.date).format('EEE MMM d, h:mm:ss a',location.timeZone)

        state.ambientMap[state.weatherStationDataIndex].info.each{ k, v ->
            if(k=='name'){k='pwsName'}
            if(debugVerbose){log.debug "sendEvent(name: ${k}, value: ${v})"}
            d.sendEvent(name: k, value: v, displayed: false)
        }
        state.ambientMap[state.weatherStationDataIndex].lastData.each{ k, v ->
            okTOSendEvent = true
            try {
                if( (v.isNumber()) && (v>0) && (v<0.1) ) {
                    if(debugVerbose){log.debug "${k}: Converting number '${v}'<0.1 -> 0.1}"}
                    v = 0.1
                }
            }
            catch (e) {
                log.error("caught exception assigning ${k} : ${v} to a value of 0.1", e)
            }
            switch (k) {
                case 'dateutc':
                okTOSendEvent = false
                break
                case 'date':
                v = state.ambientServerDate
                break
                case 'battout':
                k='battery'
                v=v.toInteger()*100
                break
                case ~/^batt[0-9].*/:
                okTOSendEvent = false
                break
                case 'lastRain':
                v=Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", v).format('EEE MMM d, h:mm a',location.timeZone)
                break
                case 'tempf':
                k='temperature'
                break
                case 'hourlyrainin':
                def waterState = state.ambientMap[state.weatherStationDataIndex].lastData?.hourlyrainin?.toFloat()>0?'wet':'dry'
                if(debugVerbose){log.debug "water -> ${waterState}"}
                d.sendEvent(name:'water', value: waterState)
                break
                case 'feelsLike':
                def scText
                switch(true) {
                    case {( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('hourlyrainin')) && (state.ambientMap[state.weatherStationDataIndex].lastData?.hourlyrainin.toFloat()>0 ))} :
                        DecimalFormat df = new DecimalFormat("0.00")
                    def numberForRainLevel = df.format(state.ambientMap[state.weatherStationDataIndex].lastData.hourlyrainin)
                    scText = sprintf("Raining at %s in/hr at %s", numberForRainLevel, nowTime)
                    break
                    case { (v > state.ambientMap[state.weatherStationDataIndex].lastData.tempf) } :
                        scText = sprintf("Feels like %sº at %s", v, nowTime)
                    break
                    case { ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('windspeedmph')) && (state.ambientMap[state.weatherStationDataIndex].lastData.windspeedmph.toFloat()>0)) } :
                        scText = sprintf("Wind is %s %s %s at %s", state.ambientMap[state.weatherStationDataIndex].lastData.windspeedmph, windUnits, degToCompass(state.ambientMap[state.weatherStationDataIndex].lastData?.winddir, true), nowTime)
                    break
                    default :
                    scText = sprintf("Humidity is %s%% at %s", state.ambientMap[state.weatherStationDataIndex].lastData.humidity, nowTime)
                    break
                }
                d.sendEvent(name: 'secondaryControl', value: scText, displayed: false )
                break
                case 'windspeedmph':
                def motionState = state.ambientMap[state.weatherStationDataIndex].lastData?.windspeedmph?.toFloat()>0?'active':'inactive'
                if(debugVerbose){log.debug "Wind motion -> ${motionState}"}
                d.sendEvent(name:'motion', value: motionState)
                break
                case 'winddir':
                def winddirectionState = degToCompass(state.ambientMap[state.weatherStationDataIndex].lastData?.winddir, true)
                if(debugVerbose){log.debug "Wind Direction -> ${winddirectionState}"}
                d.sendEvent(name:'winddirection', value: winddirectionState, displayed: false)
                d.sendEvent(name:'winddir2', value: winddirectionState + " (" + state.ambientMap[state.weatherStationDataIndex].lastData.winddir + "º)")
                break
                case 'uv':
                k='ultravioletIndex'
                break
                case 'yearlyrainin':
                k='totalrainin'
                break
                case 'solarradiation':
                k='illuminance'
                v = v.toInteger()
                if(v > 0) {
                    switch(solarRadiationTileDisplayUnits) {
                        case ('lux'):
                        v = (v * 683)
                        break
                        case ('fc'):
                        v = (v * 683 * 0.0929).toInteger()
                        break
                        default:
                            break
                    }
                    d.sendEvent(name: k, value: v, units: solarRadiationTileDisplayUnits?:'W/m²')
                    okTOSendEvent = false
                }
                break
                case 'windspeedmph':
                // Send windSpeed as power and energy for pseudo tiles in ActionTiles™
                d.sendEvent(name: "power", value: v,  displayed: false)
                d.sendEvent(name: "energy", value: v, displayed: false)
                break
                // Weather Console Sensors
                case 'tempinf':
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}0")
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted temperature with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "temperature", value: v, units: tempUnits)
                    remoteSensorDNI.sendEvent(name:"date", value: state.ambientServerDate, displayed: false)
                    remoteSensorDNI.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
                    if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('battout')) {
                        remoteSensorDNI.sendEvent(name:"battery", value: state.ambientMap[state.weatherStationDataIndex].lastData.battout.toInteger()*100, displayed: false)
                    }
                } else {
                    log.error "Missing ${DTHDNIRemoteSensorName()}0"
                }
                break
                case 'humidityin':
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}0")
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted humidity with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "humidity", value: v, units: "%")
                } else {
                    log.error "Missing ${DTHDNIRemoteSensorName()}0"
                }
                break
                // Remote Temperature & Humidity Sensors
                case ~/^temp[0-9]f$/:
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}${k[4..4]}")
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted temperature with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "temperature", value: v, units: tempUnits)
                    remoteSensorDNI.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
                    remoteSensorDNI.sendEvent(name:"date", value: state.ambientServerDate, displayed: false)
                    if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey("batt${k[4..4]}")) {
                        remoteSensorDNI.sendEvent(name:"battery", value: state.ambientMap[state.weatherStationDataIndex].lastData."batt${k[4..4]}".toInteger()*100, displayed: false)
                    }

                } else {
                    log.error "Missing ${${DTHDNIRemoteSensorName()}${k[4..4]}}"
                }
                okTOSendEvent = false
                break
                case ~/^humidity[0-9]$/:
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}${k[8..8]}")
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted humidity with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "humidity", value: v, units: "%")
                } else {
                    log.error "Missing ${${DTHDNIRemoteSensorName()}${k[8..8]}}"
                }
                okTOSendEvent = false
                break
                default:
                    break
            }
            if (okTOSendEvent){
                // , unit: "%"
                switch (k) {
                    case ('battery'):
                    case ('date'):
                    sendEventOptions = [displayed : false]
                    break
                    case ~/^temp.*/:
                    case ('feelsLike'):
                    case ('dewPoint'):
                    sendEventOptions = [units : tempUnits]
                    break
                    case ~/^humidity.*/:
                    sendEventOptions = [units : '%']
                    break
                    case ~/.*rain.*/:
                    sendEventOptions = [units : measureUnits]
                    break
                    case ('windir'):
                    sendEventOptions = [units : 'º']
                    break
                    case ~/^wind.*/:
                    case ('maxdailygust'):
                    sendEventOptions = [units : windUnits]
                    break
                    case ~/^barom.*/:
                    sendEventOptions = [units : baroUnits]
                    break
                    default:
                        sendEventOptions = []
                    break
                }
                sendEventOptions << [name: k, value: v]
                d.sendEvent(sendEventOptions)
                if(debugVerbose){log.debug "${d}.sendEvent(${sendEventOptions})"}
                sendEventOptions = []
            }
        }
    } else {
        if(debugVerbose){log.debug "getAmbientStationData() did not return any weather data"}
    }
    if (createActionTileDevice) {
        // Update kurtsanders:SmartWeather Station Tile device for ActionTiles™
        d = getChildDevice(DTHDNIActionTiles())
        d.sendEvent(name: "localSunrise", 		value : state.localSunrise, displayed: false)
        d.sendEvent(name: "localSunset", 		value : state.localSunset, displayed: false)
        d.sendEvent(name: "weatherIcon", 		value : state.weatherIcon as String)
        d.sendEvent(name: "forecastIcon", 		value : state.forecastIcon as String)
        d.sendEvent(name: "weather", 			value : state?.wxPhraseShort)
        d.sendEvent(name: "percentPrecip", 		value : state.precipChance, unit: "%")
        d.sendEvent(name: "city", 				value : "Ambient - ${state.ambientMap[state.weatherStationDataIndex].info.name}") // , isStateChange: true)
        d.sendEvent(name: "location", 			value : state.cityValue, displayed: false)
        d.sendEvent(name: "temperature", 		value : state.ambientMap[state.weatherStationDataIndex].lastData?.tempf, unit: tempUnits)
        d.sendEvent(name: "humidity", 			value : state.ambientMap[state.weatherStationDataIndex].lastData?.humidity, unit: "%")
        d.sendEvent(name: "feelsLike", 			value : state.ambientMap[state.weatherStationDataIndex].lastData?.feelsLike, unit: tempUnits)
        d.sendEvent(name: "wind",
                    value : state.ambientMap[state.weatherStationDataIndex].lastData?.windspeedmph?:"N/A", unit: windUnits)
        d.sendEvent(name: "windVector",
        value : "${degToCompass(state.ambientMap[state.weatherStationDataIndex].lastData?.winddir, false)} ${state.ambientMap[state.weatherStationDataIndex].lastData.windspeedmph?:""} ${windUnits}")
        d.sendEvent(name: "lastUpdate", 		value : tileLastUpdated(), displayed: false)
    }
}

def getAmbientStationData() {
	if(infoVerbose){log.info "Start: getAmbientStationData()"}
    if(!appSettings.apiKey){
        log.error("Severe Error: The API key is undefined in this SmartApp's IDE properties settings, exiting")
        return false
    }
    state.retry = state.retry?:0
    if (state?.retry.toInteger()>0) {
        log.info "Executing Retry getAmbientStationData() re-attempt #${state.retry}"
    }
    def params = [
        uri			: "http://api.ambientweather.net/v1/devices?applicationKey=${appKey()}&apiKey=${appSettings.apiKey}"
    ]
    try {
        httpGet(params) { resp ->
            // get the data from the response body
            state.ambientMap = resp.data
            state.respStatus = resp.status
            if (resp.status != 200) {
                log.error "AmbientWeather.Net: response status code: ${resp.status}: response: ${resp.data}"
                return false
            }
            if (state.weatherStationDataIndex) {
                state.countRemoteTempHumiditySensors =  state.ambientMap[state.weatherStationDataIndex].lastData.keySet().count { it.matches('temp[0-9]f') }
            }
            if (state.retry.toInteger()>0) {
                log.info "Success: Retry getAmbientStationData() re-attempt #${state.retry}"
                state.retry = 0
            }
        }
    } catch (e) {
        log.warn("Ambient Weather Station API Data: ${e}")
        state.retry = state.retry.toInteger() + 1
        if (state.retry.toInteger()<3) {
            log.info("Waiting 10 seconds to Try Again: Attempt #${state.retry}")
            runIn(10, ambientWeatherStation)
        }
        return false
    }
    if(infoVerbose){log.info "End: getAmbientStationData()"}
    return true
}

def addAmbientChildDevice() {
    // add Ambient Weather Reporter Station device
    def AWSDNI = getChildDevice(state.deviceId)
    if (!AWSDNI) {
        def AWSName = state.ambientMap[state.weatherStationDataIndex].info.name?:DTHName()
        log.info "Adding AmbientWS Device: ${AWSName} with DNI: ${state.deviceId}"
        try {
            addChildDevice(DTHnamespace(), DTHName(), DTHDNI(), null, ["name": AWSName, "label": AWSName, completedSetup: true])
        } catch(physicalgraph.app.exception.UnknownDeviceTypeException ex) {
            log.error "The Ambient Weather Device Handler '${DTHName()}' was not found in your 'My Device Handlers', Error-> '${ex}'.  Please install this in the IDE's 'My Device Handlers'"
            return false
        }
        log.info "Added ${AWSName} with DNI: ${DTHDNI()}"
    } else {
        if(infoVerbose){log.info "Verified Weather Station '${getChildDevice(state.deviceId)}' = DNI: '${DTHDNI()}'"}
    }

    // add Ambient Weather SmartWeather Station Tile for ActionTiles™ Integration
    def actionTileDNI = getChildDevice(DTHDNIActionTiles())
    if (createActionTileDevice) {
        if (!actionTileDNI) {
            log.info "Adding Ambient ActionTiles™ Device: '${AWSNameActionTiles()}' with DNI: '${DTHDNIActionTiles()}'"
            try {
                addChildDevice(DTHnamespace(), DTHNameActionTiles(), DTHDNIActionTiles(), null, ["name": AWSNameActionTiles(), "label": AWSNameActionTiles(), isComponent: AWSNameActionTilesHide(), completedSetup: true])
            } catch(physicalgraph.app.exception.UnknownDeviceTypeException ex) {
                log.error "The Ambient Weather ActionTiles™ Device Handler '${DTHName()}' was not found in your 'My Device Handlers', Error-> '${ex}'.  Please install this in the IDE's 'My Device Handlers'"
                return false
            }
            log.info "Added ${AWSNameActionTiles()} with DNI: ${DTHDNIActionTiles()}"
        } else {
            if(infoVerbose){
                if(infoVerbose){log.info "Verified Weather Station '${getChildDevice(DTHDNIActionTiles())}' = DNI: '${DTHDNIActionTiles()}'"}
            }
        }
    } else {
        if (actionTileDNI) {
            log.info "Deleting ${AWSNameActionTiles()} with DNI: ${DTHDNIActionTiles()}"
            deleteChildDevice(actionTileDNI)
        }
    }

    // add Ambient Weather Remote Sensor Device(s)
    def remoteSensorNamePref
    def remoteSensorNameDNI
    def remoteSensorNumber
    settings.each { key, value ->
        if ( key.startsWith(DTHDNIRemoteSensorName()) ) {
            remoteSensorNamePref = "Ambient - ${value}"
            remoteSensorNameDNI = getChildDevice(key)
            remoteSensorNumber = key.reverse()[0..0]
            if (remoteSensorNumber.toInteger() <= state.countRemoteTempHumiditySensors.toInteger()) {
                if (!remoteSensorNameDNI) {
                    log.info "Adding AmbientWS Remote Sensor Device: ${value}"
                    try {
                        addChildDevice(DTHnamespace(), DTHRemoteSensorName(), "${key}", null, ["name": remoteSensorNamePref, "label": remoteSensorNamePref, completedSetup: true])
                    } catch(physicalgraph.app.exception.UnknownDeviceTypeException ex) {
                        log.error "The Ambient Weather Device Handler '${DTHRemoteSensorName()}' was not found in your 'My Device Handlers', Error-> '${ex}'.  Please install this in the IDE's 'My Device Handlers'"
                        return false
                    }
                    log.info "Added Ambient Remote Sensor: ${remoteSensorNamePref} with DNI: ${key}"
                } else {
                    if(infoVerbose){log.info "Verified Remote Sensor #${remoteSensorNumber} of ${state.countRemoteTempHumiditySensors} Exists: ${remoteSensorNamePref} = DNI: ${key}"}
                    if ( (remoteSensorNameDNI.label == remoteSensorNamePref) && (remoteSensorNameDNI.name == remoteSensorNamePref) ) {
                        if(infoVerbose){
                            log.info "Device Label/Name Pref Match: Name: ${remoteSensorNameDNI.name} = Label: ${remoteSensorNameDNI.label} == Pref Label: ${remoteSensorNamePref} -> NO CHANGE"
                        }
                    } else {
                        log.error "Device Label/Name Pref Mis-Match: Name: ${remoteSensorNameDNI.name} <> Label: ${remoteSensorNameDNI.label} <> Pref Label: ${remoteSensorNamePref} -> RENAMING"
                        remoteSensorNameDNI.label = remoteSensorNamePref
                        remoteSensorNameDNI.name  = remoteSensorNamePref
                        log.info "Successfully Renamed Device Label and Names for: ${remoteSensorNameDNI}"
                    }
                }
            } else {
                log.warn "Device ${remoteSensorNumber} DNI: ${key} '${remoteSensorNameDNI.name}' exceeds # of remote sensors (${state.countRemoteTempHumiditySensors}) reporting from Ambient -> ACTION REQUIRED"
                log.warn "Please verify that all Ambient Remote Sensors are online and reporting to Ambient Network.  If so, please manual delete the device in the SmartThings 'My Devices' view"
            }
        }
    }
}

def degToCompass(num,longTitles=true) {
    if (num) {
        def val = Math.floor((num.toFloat() / 22.5) + 0.5).toInteger()
        def arr = []
        if (longTitles) {
            arr = ["N", "North NE", "NE", "East NE", "E", "East SE", "SE", "South SE", "S", "South SW", "SW", "West SW", "W", "West NW", "NW", "North NW"]
        } else {
            arr = ["N", "N NE", "NE", "E NE", "E", "E SE", "SE", "S SE", "S", "S SW", "SW", "W SW", "W", "W NW", "NW", "N NW"]
        }
        return arr[(val % 16)]
    }
    return "N/A"
}


def setScheduler(schedulerFreq) {
    if(infoVerbose){log.info "Section: setScheduler(${schedulerFreq})"}
    def scheduleHandler = 'autoScheduleHandler'
    unschedule(scheduleHandler)
    if(infoVerbose){log.info "Auto Schedule Refresh Rate is now -> ${schedulerFreq} mins"}
    switch(schedulerFreq) {
        case '0':
        log.info "Auto Schedule Refresh Rate is now: OFF"
        break
        case '1':
        runEvery1Minute(scheduleHandler)
        break
        case '2':
        schedule("20 0/2 * * * ?",scheduleHandler)
        break
        case '3':
        schedule("20 0/3 * * * ?",scheduleHandler)
        break
        case '4':
        schedule("20 0/4 * * * ?",scheduleHandler)
        break
        case '5':
        runEvery5Minutes(scheduleHandler)
        break
        case '10':
        runEvery10Minutes(scheduleHandler)
        break
        case '15':
        runEvery15Minutes(scheduleHandler)
        break
        case '30':
        runEvery30Minutes(scheduleHandler)
        break
        case '60':
        runEvery1Hour(scheduleHandler)
        break
        case '180':
        runEvery3Hours(scheduleHandler)
        break
        default :
        unschedule()
        break
    }
}

def tileLastUpdated() {
    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    return sprintf("%s Tile Last Refreshed at\n%s","${version()}", now)
}
def informationList(variable) {
    switch(variable) {
        case ("apiHelp") :
        // Help Text for API Key
        variable =  [
            "You MUST enter your Ambient Weather API key in the ${appName()} SmartApp Settings section.",
            "Visit your Ambient Weather Dashboards's Account page.",
            "Create/Copy your API key from the bottom of the page",
            "Return to your SmartThings IDE 'My SmartApps' browser page.",
            "EDIT the ${appName()} SmartApp.",
            "Press the App Settings button at the top right of the page.",
            "Scroll down the page, expand the 'Settings' section.",
            "Enter or paste your Ambient API key in the API value input box.",
            "Press Update on bottom of page to save.",
            "Exit the SmartApp and Start ${appName()} Setup again on your mobile phone."
        ]
        break
        default:
            break
    }
    if (variable instanceof List) {
        def numberedText = ""
        variable.eachWithIndex { item, index ->
            numberedText += "${index+1}. ${item}"
            numberedText += (index<variable.size()-1)?"\n":''
        }
        return numberedText
    }
    return variable
}

def parseAlertTime(s) {
    def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def s2 = s.replaceAll(/([0-9][0-9]):([0-9][0-9])$/,'$1$2')
    dtf.parse(s2)
}

def SMSNotifcationHistory() {
    def date = now()
    def dateToday = date
    def today = new Date(date).format("MMM-DD-YYYY", location.timeZone)
    def msg = ""
    def notifyVars = ["Severe Weather"	: state.notifySevereAlertDT ]
    if ( (state.deviceId) && (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) ) {
        notifyVars << ["Low Temp" : state.notifyAlertLowTempDT, "High Temp" : state.notifyAlertHighTempDT]
    }
    if ( (state.deviceId) && (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('hourlyrainin')) ) {
        notifyVars << ["Rain Detected" : state.notifyRainDT]
    }

    notifyVars.eachWithIndex { k, index ->
        if (k.value) {
            date = new Date(k.value).format("MMM-DD-YYYY h:mm a", location.timeZone)
            dateToday = new Date(k.value).format("MMM-DD-YYYY", location.timeZone)
            if (today==dateToday) {
                date = "Today @ " + new Date(k.value).format("h:mm a", location.timeZone)
            }
            msg += "${index+1}) ${k.key} : ${date}\n"
        } else {
            msg += "${index+1}) ${k.key} : --\n"
        }
    }
    return msg
}

def notifyEvents() {
    if (mobilePhone){
        def now = now()
        def msg
        //        state.notifySevereAlertDT = now-3600000*2
        if ( (notifyAlertLowTemp) && (state.ambientMap[state.weatherStationDataIndex].lastData?.tempf<=notifyAlertLowTemp.toInteger()) ) {
            msg = "${appShortName()}: LOW TEMP ALERT:  Current temperature of ${state.ambientMap[state.weatherStationDataIndex].lastData?.tempf}º <= ${notifyAlertLowTemp}º"
            if (lastNotifyDT(state.notifyAlertLowTempDT, "Low Temp")) {
                if(debugVerbose){log.debug "SMS: ${msg}"}
                sendNotification("${msg}", [method: "both", phone: mobilePhone])
                state.notifyAlertLowTempDT = now
            }
        }
        if ( (notifyAlertHighTemp) && (state.ambientMap[state.weatherStationDataIndex].lastData?.tempf.toInteger()>=notifyAlertHighTemp) ) {
            msg = "${appShortName()}: HIGH TEMP ALERT:  Current temperature of ${state.ambientMap[state.weatherStationDataIndex].lastData?.tempf}º >= ${notifyAlertHighTemp}º"
            if (lastNotifyDT(state.notifyAlertHighTempDT, "High Temp")) {
                if(debugVerbose){log.debug "SMS: ${msg}"}
                state.notifyAlertHighTempDT = now
                sendNotification("${msg}", [method: "both", phone: mobilePhone])
            }
        }
        if ( (notifyRain) && (state.ambientMap[state.weatherStationDataIndex].lastData.hourlyrainin?.toInteger()>0) ){
            log.debug "${appShortName()}: RAIN DETECTED ALERT: Current hourly rain sensor reading of ${state.ambientMap[state.weatherStationDataIndex].lastData?.hourlyrainin} in/hr"
            if (lastNotifyDT(state.notifyRainDT, "Rain")) {
                if(debugVerbose){log.debug "SMS: ${msg}"}
                state.notifyRainDT = now
                sendNotification("${msg}", [method: "both", phone: mobilePhone])
            }
        }
    }
}

def lastNotifyDT(lastDT, eventName) {
    if (!lastDT) { return true }
    def now = now()/1000
    def date = new Date(lastDT).format("MMM-DD-YYYY h:mm:ss a", location.timeZone)
    def hours = ((now-(lastDT/1000))/3600).toFloat().round(1)
    def days = (hours/24).toFloat().round(1)
    def rc = hours>=state.notifyAlertFreq.toInteger()
    if(infoVerbose){log.info "This '${eventName}' event was last sent on ${date}: ${days} days, ${hours} hours ago"}
    if(infoVerbose){log.info "${eventName} Alert Every ${notifyAlertFreq} hours: ${rc?'OK to SMS':'TOO EARLY TO SEND'}"}
    return rc
}

def setStateWeatherStationData() {
    if (weatherStationMac) {
        state.weatherStationDataIndex = state.ambientMap.findIndexOf {
            it.macAddress in [weatherStationMac]
        }
    }
    state.weatherStationDataIndex = state.weatherStationDataIndex?:0
    state.weatherStationMac = state.weatherStationMac?:state.ambientMap[state.weatherStationDataIndex].macAddress
    state.countRemoteTempHumiditySensors =  state.ambientMap[state.weatherStationDataIndex].lastData.keySet().count { it.matches('temp[0-9]f') }
}