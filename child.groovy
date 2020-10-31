////////////////////////////////////////////////////////////////////////////////////////////////////////
//  
//  Smartenit ZBMLC30 Child
//
//  My first attempt at a parent / child driver.
//  Based off of my hacked implementation from SmartThings.
//
//  ZBMLC30 allows 220V control of dual motors - like pool and spa.  
//
//
//
//  Created By: MrYutz
//  Inital Code: 2/3/2020
//
//  Use it.  Sorry there isn't any support.
//
/////////////////////////////////////////////////////////////////////////////////////////////////////\/

metadata {
    definition (name: "Smartenit ZBMLC30 Child", namespace: "MrYutz", author: "MrYutz") {
        capability "Switch"
        capability "Actuator"
        command "report", ["bool", "string"]
		command "refresh"
        
        attribute "heartbeat", "string"
        //attribute "switch", "ENUM",["on","off"]
        //attribute "heartbeat", "string"
        
    }
    
    preferences {
		input "switchNumber", "enum", title: "Primary or Secondary Switch", required: true, 
            options: [[1: "Primary"], [2: "Secondary"]]
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}


def installed() {
	"Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	refresh()
}

def uninstalled() {
	parent.unRegisterChild(device.deviceNetworkId)
}

def parse(String description) {
	if (txtEnable && description == "on")
		log.info "${device.label} was pushed"
}

def parse(List description) {
	log.warn "parse(List description) received ${description}"
	return
}


def refresh() {
	parent.refreshZoneStatus()
}

def on() {
    if (switchNumber) {
        log.debug "Attempting to turn $device switchNumber $switchNumber on"
        // this works: parent.switch1_on()
        parent.turn_on(switchNumber.toInteger())
        
    } else {
        log.debug "Switch Endpoint Is Not Set!"
    }
}

def off() {
    if (switchNumber) {
      log.debug "Attempting to turn $device switchNumber $switchNumber off"
      // this works: parent.switch1_off()
      parent.turn_off(switchNumber.toInteger())
    
    } else {
      log.debug "Switch Endpoint Is Not Set!"
    }
}
