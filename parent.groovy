// ZBMLC30 Driver with Children
 
 import groovy.transform.Field

 @Field final Endpoint2 = 0x02
 @Field final MeteringCluster = 0x0702
 @Field final MeteringInstantDemand = 0x0400
 @Field final MeteringInstantDemandDivisor = 0x0304
 @Field final MeteringCurrentSummation = 0x0000

 @Field final OnOffCluster = 0x0006
 @Field final OnOffAttr = 0x0000
 @Field final OffCommand = 0x0000
 @Field final OnCommand  = 0x0001

 @Field final BasicCluster = 0x0000
 @Field final ModelIdAttr = 0x0005
 

metadata {
	definition (name: "Smartenit ZBMLC30 Parent", namespace: "MrYutz", author: "MrYutz") {
        
        capability "Initialize"
        capability "Refresh"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"
		capability "Energy Meter"
        
        command "recreateChildDevices"

        command "switch1_on"
        command "switch1_off"
        
        command "switch2_on"
        command "switch2_off"
        
        command "turn_on", ["Integer"]
        command "turn_off", ["Integer"]

		attribute "heartbeat", "string"
        attribute "switch1", "ENUM",["on","off"]
		attribute "switch2", "ENUM",["on","off"]

		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0702", outClusters: "0019", model: "ZBMLC30", deviceJoinName: "Smartenit Metering Dual Load Controller"
		fingerprint profileId: "0104", inClusters: "0000,0003,0006,0702", outClusters: "0019", model: "ZBMLC30-1", deviceJoinName: "Smartenit Metering Dual Load Controller"
    }
}




private removeChildDevices(delete) {
    
    log.debug "Parent deleteChildren"
	state.childrencreated = false
	def children = getChildDevices()
    
    children.each {child->
  		deleteChildDevice(child.deviceNetworkId)
    }
}

def getFPoint(String FPointHex){
    return (Float)Long.parseLong(FPointHex, 16)
}


def initialize() {
    
    log.debug "Clearing settings."
    state.version = version()
	state.cookie = null
    sendEvent(name: "switch", value: "unknown", isStateChange: true)
	if (!state.configured) return
	
    if (!state.childrencreated) {
	   log.debug "State 'childrencreated' is ${state.childrencreated} so we create new children."
    	recreateChildDevices()
	}
}

def recreateChildDevices() {
    log.debug "Parent recreateChildDevices"
    removeChildDevices()
    log.debug "Parent removeChildDevices"
    createChildDevices()
    log.debug "Parent createChildDevices"
}

def createChildDevices() {
    def createdCount = 0
    log.debug "Creating child devices"
    for (i in 1..2) {                            
       addChildDevice("MrYutz", "Smartenit ZBMLC30 Child", "${device.deviceNetworkId} Child Switch $i",[name: "$device.displayName Child Switch $i", label: "$device.displayName Child Switch $i", isComponent: true]) 
	}
    state.childrencreated = true
}


/*
* Parse incoming device messages to generate events
*/
def parse(String description) {
    def attrName = null
	def attrValue = null
    
    child1 = getChildDevice("${device.deviceNetworkId} Child Switch 1")
    child2 = getChildDevice("${device.deviceNetworkId} Child Switch 2")
    
    def mapDescription = zigbee.parseDescriptionAsMap(description)
	log.debug "parse... mapDescription: ${mapDescription}"
    
    if (mapDescription.command != "0A"){
        
        def event = zigbee.getEvent(description)
        log.debug "parse... event: ${event} on endpoint: ${mapDescription.sourceEndpoint}"
    }
    
    if(mapDescription.cluster == "0702")
    {
        if(mapDescription.attrId == "0400")
        {
            return sendEvent(name:"power", value: getFPoint(mapDescription.value)/100.0)
        }
        else if(mapDescription.attrId == "0000")
        {
            return sendEvent(name:"energy", value: getFPoint(mapDescription.value)/10000.0)
        }
    }
    
    // mapDescription.clusterInt == 6
    // clusterInt:6 == On off Report for each Endpoint
    //Refresh called via GUI results in this output
    //       Endpoint 02 on                            mapDescription: [raw:A29A0200060A00001001, 		dni:A29A, endpoint:02, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:01, 		clusterInt:6, 		attrInt:0]
    //       Endpoint 01 off                           mapDescription: [raw:A29A0100060A00001000, 		dni:A29A, endpoint:01, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:00, 		clusterInt:6, 		attrInt:0]
    
    //clusterInt:6 && command:0B - endpoint sets switch, data[0] = on or off
    // mapDescription: [raw:catchall: 0104 0006 01 01 0040 00 A29A 00 00 0000 0B 01 0000, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[00, 00]]
    // mapDescription: [raw:catchall: 0104 0006 02 01 0040 00 A29A 00 00 0000 0B 01 0000, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:02, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[00, 00]]
    // mapDescription: [raw:catchall: 0104 0006 01 01 0040 00 A29A 00 00 0000 0B 01 0100, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[01, 00]]
    // mapDescription: [raw:catchall: 0104 0006 02 01 0040 00 A29A 00 00 0000 0B 01 0100, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:02, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[01, 00]]
    
    // mapDescription: [raw:catchall: 0104 0006 01 01 0040 00 8743 00 00 0000 0B 01 0100, profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:8743, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[01, 00]]
    

    else if(mapDescription.clusterInt == 6)
    {
        sendEvent(name: "parseSwitch", value: mapDescription)

       
        // Parse data where we have a sourceEndpoint tag. On refresh we only have an enpoint tag and have a seperate section to parse that data.
 
        if ( mapDescription?.sourceEndpoint != null){
            if((mapDescription?.sourceEndpoint == "01") || ( mapDescription?.endpoint == "01")) {
                attrName = "switch1"
                if (child1 != null){ 
                    //Set child deviced handle
                    child = child1
                }       
            }

            else if((mapDescription?.sourceEndpoint == "02") || ( mapDescription?.endpoint == "02")){
                attrName = "switch2"
                if (child2 != null){ 
                    //Set child Variable
                    child = child2
                }
            }

            else{
                return
            }

            if(mapDescription.command == "0B") {
                if(mapDescription.data[0] == "00") { 
                    attrValue = "off"
                }else if(mapDescription.data[0] == "01") {
                    attrValue = "on"
                }else{
                    return
                }

            }else {
                if(mapDescription.value == "00") {
                    attrValue = "off"
                }else if(mapDescription.value == "01") {
                    attrValue = "on"
                }else{
                    return
                }
            }
        }
        
        if ( mapDescription?.endpoint != null){
            
            // Endopint One = Switch 1
            if(mapDescription?.endpoint == "01") {
                attrName = "switch1"
                if (child1 != null){ 
                    //Set child deviced handle
                    child = child1
                }            
            }

            // Endpoint 2 = Switch 2
            else if(mapDescription?.endpoint == "02"){
                attrName = "switch2"
                if (child2 != null){ 
                    //Set child Variable
                    child = child2

                }
            }
            else{
                
                return
            }
            
            if (mapDescription?.command == "01" && mapDescription?.value == "00") { 
               attrValue = "off"
                     
            }
            else if (mapDescription?.command == "01" && mapDescription?.value == "01") {
               attrValue = "on"
            }
            else{
                
                return
            }
        }
               
        sendEvent(name: attrName, value: attrValue)
        child.sendEvent(name: "switch", value: attrValue)
        
        def result = createEvent(name: attrName, value: attrValue)
        
        return result
    }
    
    
    // mapDescription.clusterInt == 0 = ?
    else if(mapDescription.clusterInt == 0)
    {
        sendEvent(name: "parseBasic", value: mapDescription)
        if(mapDescription.attrId == "0005")
        {
            attrName = "ModelId"
            attrValue = mapDescription.value.toString()
            log.debug "ModelName attrValue: ${attrValue}"
            state.ModelName = mapDescription.value
            log.debug "ModelName attr received: ${state.ModelName}"
            if (state.ModelName == "5A424D4C4333302D31") 
            {
                state.MeteringEP = 0x01
            }
        }
        sendEvent(name: attrName, value: attrValue)

        def result = createEvent(name: attrName, value: attrValue)
        return result
    }
    
    
    //mapDescription.clusterInt == 8 = ?
    else if(mapDescription.clusterInt == 8)
    {
		log.debug "parsing level control..value: ${mapDescription.value}"
        if(mapDescription.value == "00") {
            attrValue = "off"
            sendEvent(name: "switch1", value: attrValue)
            sendEvent(name: "switch2", value: attrValue)
            
            for (child in getChildDevices()) {
                child.sendEvent(name: "switch", value: attrValue)
            }
            
        }else if(mapDescription.value == "80") {
            sendEvent(name: "switch1", value: "off")
            sendEvent(name: "switch2", value: "on")
            child1.sendEvent(name: "switch", value: "off")
            child2.sendEvent(name: "switch", value: "on")

            
        }else if(mapDescription.value == "ff") {
            sendEvent(name: "switch1", value: "on")
            sendEvent(name: "switch2", value: "off")
            child1.sendEvent(name: "switch", value: "on")
            child2.sendEvent(name: "switch", value: "off")
            
            
        }else{
            return
        }
    }
    
    
    
    else
    {
    	if(description.contains("on/off")) {
        	log.debug "must be a report, but don't know which Endpoint"
        }else {
        	log.warn "Did not parse message: $description"
        }
    }

    return createEvent([:])
}


// Get the list that was sent from the device.    
//def parse(String description) {
    
//    def attrName = null
//	def attrValue = null
        
//    if (dbgEnable) {
//        log.debug "${device.label} Parsing Incoming message: ${description}"
//    }
        
//    def mapDescription = zigbee.parseDescriptionAsMap(description)
//	log.debug "parse... mapDescription: ${mapDescription}"

//    def event = zigbee.getEvent(description)
//    log.debug "parse... event: ${event}"

//	switch (message.substring(2, 4)) {
//		case "SourceEndpoint":
            // either 01 or 02 for switch 01 or switch 02
//			zoneChange(message);
//			break;
//		case "attrId":
            // attribute 0400 = power report.  0000 = energy report.
//			heartbeat();
//			break;
//		case "command":
            // command was confirmed.  check "data[0]" for the on or off report on switch
//			outputChange(message);
//			break;
//		case "value":
            // an on/off event was reported.   Figure out what switch it happened on.
//			taskChange(message);
//			break;
//		case "EE":
//		default:
//			if (txtEnable) log.info "${device.label}: The ${message.substring(2, 4)} command is unknown";
//			break;
//	}
    
// clusterInt:1794, attrInt:1024 	= Power report for whole device		
// mapDescription: [raw:00890207020E00042A000000,         dni:0089, endpoint:02, cluster:0702, size:0E, attrId:0400, encoding:2A, command:01, value:000000, clusterInt:1794, attrInt:1024]
// mapDescription: [raw:A29A0207020C00042ADE8802,         dni:A29A, endpoint:02, cluster:0702, size:0C, attrId:0400, encoding:2A, command:0A, value:0288DE, clusterInt:1794, attrInt:1024]
// mapDescription: [raw:A29A0207020C00042A029C02,         dni:A29A, endpoint:02, cluster:0702, size:0C, attrId:0400, encoding:2A, command:0A, value:029C02, clusterInt:1794, attrInt:1024]
// mapDescription: [raw:A29A0207020C00042A0A2701, 		  dni:A29A, endpoint:02, cluster:0702, size:0C, attrId:0400, encoding:2A, command:0A, value:01270A, clusterInt:1794, attrInt:1024]


// clusterInt:1794, attrInt:0 		= Energy report for whole device I think.
// mapDescription: [raw:A29A02070212000025CC5D93040000,   dni:A29A, endpoint:02, cluster:0702, size:12, attrId:0000, encoding:25, command:0A, value:000004935DCC, clusterInt:1794, attrInt:0]


// No idea......
// mapDescription: [raw:catchall: 0000 8001 00 00 0040 00 A29A 00 00 0000 00 00 8D00F6691C0000A322003CFE, 	profileId:0000, clusterId:8001, clusterInt:32769, sourceEndpoint:00, destinationEndpoint:00, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[8D, 00, F6, 69, 1C, 00, 00, A3, 22, 00, 3C, FE]]


//clusterINt:6 && command: 01 && attrId = 0000 - endpoint sets switch, value = on or off
// mapDescription: [raw:A29A0200060A00001000, 												dni:A29A, endpoint:02, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:00, clusterInt:6, attrInt:0]
// mapDescription: [raw:A29A0100060A00001000, 												dni:A29A, endpoint:01, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:00, clusterInt:6, attrInt:0]
// mapDescription: [raw:A29A0200060A00001001, 												dni:A29A, endpoint:02, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:01, clusterInt:6, attrInt:0]
// mapDescription: [raw:A29A0100060A00001000, 												dni:A29A, endpoint:01, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:00, clusterInt:6, attrInt:0]


//clusterInt:6 && command:0B - endpoint sets switch, data[0] = on or off
// mapDescription: [raw:catchall: 0104 0006 01 01 0040 00 A29A 00 00 0000 0B 01 0000, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[00, 00]]
// mapDescription: [raw:catchall: 0104 0006 02 01 0040 00 A29A 00 00 0000 0B 01 0000, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:02, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[00, 00]]
// mapDescription: [raw:catchall: 0104 0006 01 01 0040 00 A29A 00 00 0000 0B 01 0100, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[01, 00]]
// mapDescription: [raw:catchall: 0104 0006 02 01 0040 00 A29A 00 00 0000 0B 01 0100, 		profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:02, destinationEndpoint:01, options:0040, messageType:00, dni:A29A, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[01, 00]]

//Refresh called via GUI results in this output
//       Endpoint 02 on                            mapDescription: [raw:A29A0200060A00001001, 		dni:A29A, endpoint:02, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:01, 		clusterInt:6, 		attrInt:0]
//       Endpoint 01 off                           mapDescription: [raw:A29A0100060A00001000, 		dni:A29A, endpoint:01, cluster:0006, size:0A, attrId:0000, encoding:10, command:01, value:00, 		clusterInt:6, 		attrInt:0]
//       Endpoint Power consumption     		   mapDescription: [raw:A29A0207020C00042A0A2701, 	dni:A29A, endpoint:02, cluster:0702, size:0C, attrId:0400, encoding:2A, command:0A, value:01270A, 	clusterInt:1794, 	attrInt:1024]
//                                                
// clusterInt:6 					= On off Report for each Endpoint
// clusterInt:1794, attrInt:1024 	= Power report for whole device
// clusterInt:1794, attrInt:0 		= Energy report for whole device I think.



 
//}

def switch1_on() {
	log.info "switch1_on() via turn_on(1)"
	//zigbee.on()
    turn_on(1)

}

def switch1_off() {
	log.info "switch1_off() via turn_off(1)"
	//zigbee.off()
    turn_off(1)

}

def switch2_on(){
	log.info "switch2_on() via turn_on(2)"
    turn_on(2)

}

def switch2_off(){
	log.info "switch2_off() via turn_off(2)"
    turn_off(2)

}

def refresh() {
	if( (state.MeteringEP == null) || (state.ModelName == null) || (state.MeterBound == null)) {
    	log.warn "Device not configured, configuring now.."
        return configure()
    }

    def configCmds = []
    if(state.MeterBound == 0) {
        state.MeterBound = 1
        configCmds = [ "zdo bind 0x${device.deviceNetworkId} ${state.MeteringEP} 0x01 ${MeteringCluster} {${device.zigbeeId}} {}" ]
    }

    sendEvent(name: "heartbeat", value: "alive", displayed:false)
    for (child in getChildDevices()){
 
        child.sendEvent(name: "heartbeat", value: "alive")
    }
    
    return (
        zigbee.onOffRefresh() + 
        zigbee.readAttribute(OnOffCluster, OnOffAttr, [destEndpoint:Endpoint2]) +
        zigbee.readAttribute(MeteringCluster, MeteringCurrentSummation, [destEndpoint:state.MeteringEP]) +
        zigbee.readAttribute(MeteringCluster, MeteringInstantDemand, [destEndpoint:state.MeteringEP]) +
        configCmds
    )
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    
	runEvery15Minutes(refresh)

	state.ModelName = ""
    state.MeteringEP = 0x02
    state.MeterBound = 0
    
    def retrieveModel = [
    	zigbee.readAttribute(BasicCluster, ModelIdAttr)
    ]
    
	def configCmds = [
        //"zdo bind 0x${device.deviceNetworkId} ${state.MeteringEP} 0x01 ${MeteringCluster} {${device.zigbeeId}} {}",
        "zdo bind 0x${device.deviceNetworkId} ${Endpoint2} 0x01 ${OnOffCluster} {${device.zigbeeId}} {}"
    ]

	return  retrieveModel + configCmds + zigbee.onOffConfig()
}

def turn_on(Integer output = 0) {
	//if (dbgEnable)
		log.debug "${device.label} Parent Is turning output ${output} on."
    
    if (output == 2){
        //log.info "turn_on(${output}) with cmds"
        //def additionalParams = []
        //log.debug zigbee.command(0x0006, 0x0001, [destEndpoint: 0x02], "")
        //log.debug zigbee.command(OnOffCluster, OnCommand, additionalParams=[destEndpoint:Endpoint2])
        
        def cmds = []
        cmds << "st cmd 0x${device.deviceNetworkId} ${Endpoint2} ${OnOffCluster} ${OnCommand} {}"
	    cmds
        
    }
    else if (output == 1){
        //log.info "turn_on(${output}) with zigbee.on()"
	    zigbee.on()
    }
}

def turn_off(Integer output = 0) {
	//if (dbgEnable)
		log.debug "${device.label} Parent Is turning output ${output} off."
    
    if (output == 2){
        //log.info "turn_off(${output}) with cmds"
        //def cmds = []
        //log.debug zigbee.command(0x0006, 0x0000, [destEndpoint: 0x02], "")
        //log.debug zigbee.command(OnOffCluster, OnCommand, additionalParams=[destEndpoint:Endpoint2])
        
        def cmds = []
        cmds << "st cmd 0x${device.deviceNetworkId} ${Endpoint2} ${OnOffCluster} ${OffCommand} {}"
        cmds
	   
    }
    else if (output == 1){
        //log.info "turn_off(${output}) with zigbee.off()"
	    zigbee.off()
    }
	
}
