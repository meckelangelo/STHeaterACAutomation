definition(
    name: "Heater/AC Automation",
    namespace: "meckelangelo",
    author: "David Meck",
    description: "Automate a heater/AC unit that is plugged into a smart outlet, based on contact sensor and/or motion sensor.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/meckelangelo/STHeaterACAutomation/master/HeatCool60x60.png",
    iconX2Url: "https://raw.githubusercontent.com/meckelangelo/STHeaterACAutomation/master/HeatCool120x120.png",
    iconX3Url: "https://raw.githubusercontent.com/meckelangelo/STHeaterACAutomation/master/HeatCool.png")


preferences {    
    section("Select the outlet(s)...") {
        input "outlet", "capability.switch", title: "Outlet", required: true, multiple: false
    }
    
    section("What is plugged into the outlet?") {
        input "outletMode", "enum", title:"Device", options:["Heater", "AC", "Disabled"], required: true, multiple: false, refreshAfterSelection:true
    }

    section("Choose a temperature sensor...") {
        input "temperatureSensor", "capability.temperatureMeasurement", title: "Sensor", required: true, multiple: false
    }
    
    section("(Optional) Turn the outlet on/off when this contact sensor is opened/closed...") {
        input "door", "capability.contactSensor", title: "Sensor", required: false, multiple:false
    }
    
    section("Should the outlet turn on or off (or do nothing) when the contact sensor is opened? Choose 'Nothing' if no contact sensor selected.") {
        input "opened", "enum", title: "Opened", required: true, options: ["On", "Off", "Nothing"]
    }
    
    section("Should the outlet turn on or off (or do nothing) when the contact sensor is closed? Choose 'Nothing' if no contact sensor selected.") {
        input "closed", "enum", title: "Closed", required: true, options: ["On", "Off", "Nothing"]
    }
    
    section("Turn the outlet on when motion has been detected by this sensor (and the temperature exceeds the criteria further down)...") {
        input "motionSensor", "capability.motionSensor", title: "Sensor", required: true, multiple: false
    }
    
    section("Turn the outlet off when there has been no motion for this number of minutes...") {
        input "minutes", "number", title: "Minutes", required: true
    }
    
    section("Set the comfort temperature...") {
        input "setComfTemp", "number", title: "Degrees Fahrenheit", required: true
    }

    section("Set the vacant temperature (this temperature will be maintained regardless of contact sensor or motion detection)...") {
        input "setVacTemp", "number", title: "Degrees Fahrenheit", required: true
    }
    
    section("Regardless of contact sensor or motion sensor, maintain the comfort temperature in these modes... (WARNING: You must also select this mode in 'Set for specific mode(s)' - otherwise it will not function properly.)") {
        input "modes", "mode", title: "Mode", multiple: true, required: false
    }
    
    section("Name and modes... WARNING: It is strongly advised not to select 'Away' when choosing modes!"){}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    if (outletMode != "Disabled") {
        initialize()
    }
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    if (outletMode != "Disabled") {
        initialize()
    }
}

def initialize() {
    subscribe(temperatureSensor, "temperature", temperatureHandler)
    if (door != null && door != "") {
	    subscribe(door, "contact", contactHandler)
    }
    subscribe(motionSensor, "motion.active", motionHandler)
    subscribe(motionSensor, "motion.inactive", motionStoppedHandler)
    subscribe(location, "mode", modeChangeHandler)
}

def turnOn() {
    outlet.on()
}

def turnOff() {
    outlet.off()
}

def checkMotion() {
    def motionState = motionSensor.currentState("motion")
    
    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.date.time
        def threshold = minutes * 60 * 1000
        
        if (elapsed >= threshold) {
            turnOff()
        }
        else {
        }
    } else {
    }
}

def evaluateTemperature() {
    if (outletMode == "Heater") {
        if (temperatureSensor.latestValue("temperature") < setVacTemp) {
            turnOn()
        } else if (modes.contains(location.mode)) {
            if (temperatureSensor.latestValue("temperature") < setComfTemp) {
                turnOn()
            } else {
                turnOff()
            }
        } else if (door.latestValue("contact") == "open") {
        	if (opened == "On") {
                if (temperatureSensor.latestValue("temperature") < setComfTemp) {
                    turnOn()
                } else {
                    turnOff()
                }
        	}
            else if (opened == "Off") {
            	if (temperatureSensor.latestValue("temperature") < setVacTempt) {
                	turnOn()
                } else {
                	turnOff()
                }
            }
        } else if (door.latestValue("contact") == "closed") {
        	if (closed == "On") {
                if (temperatureSensor.latestValue("temperature") < setComfTemp) {
                    turnOn()
                } else {
                    turnOff()
                }
        	}
            else if (closed == "Off") {
            	if (temperatureSensor.latestValue("temperature") < setVacTempt) {
                	turnOn()
                } else {
                	turnOff()
                }
            }
        } else {
            turnOff()
        }
    } else if (outletMode == "AC") {
        if (temperatureSensor.latestValue("temperature") > setVacTemp) {
            turnOn()
        } else if (modes.contains(location.mode)) {
            if (temperatureSensor.latestValue("temperature") > setComfTemp) {
                turnOn()
            } else {
                turnOff()
            }
        } else if (door.latestValue("contact") == "open") {
        	if (opened == "On") {
                if (temperatureSensor.latestValue("temperature") > setComfTemp) {
                    turnOn()
                } else {
                    turnOff()
                }
        	}
            else if (opened == "Off") {
            	if (temperatureSensor.latestValue("temperature") > setVacTempt) {
                	turnOn()
                } else {
                	turnOff()
                }
            }
        } else if (door.latestValue("contact") == "closed") {
        	if (closed == "On") {
                if (temperatureSensor.latestValue("temperature") > setComfTemp) {
                    turnOn()
                } else {
                    turnOff()
                }
        	}
            else if (closed == "Off") {
            	if (temperatureSensor.latestValue("temperature") > setVacTempt) {
                	turnOn()
                } else {
                	turnOff()
                }
            }
        } else {
            turnOff()
        }
    } else {
        turnOff()
    }
}

def contactHandler(evt) {
    evaluateTemperature()
}

def temperatureHandler(evt) {
    evaluateTemperature()
}

def motionHandler(evt) {
    evaluateTemperature()
}

def motionStoppedHandler(evt) {
    runIn(minutes * 60, checkMotion)
}

def modeChangeHandler(evt) {
    evaluateTemperature()
}
