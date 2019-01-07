DOMAIN SATELLITE_Domain
{
	TEMPORAL_MODULE temporal_module = [0, 700], 500;
	
	COMP_TYPE SingletonStateVariable SatelliteOperationModeType (Earth(), Slewing(), Science(), Comm())
	{
		VALUE Earth() [1, +INF]
		MEETS {
			Slewing();
			Comm();
		}
		
		VALUE Slewing() [10, 10]
		MEETS {
			Earth();
			Science();
		}
		
		VALUE Science() [16, +INF]
		MEETS {
			Slewing();
		}
		
		VALUE Comm() [15, 35]
		MEETS {
			Slewing();
		}
	}
	
	COMP_TYPE SingletonStateVariable InstrumentType (Idle(), Warmup(), Turnoff(), Process())
	{
		VALUE Idle() [1, +INF]
		MEETS {
			Warmup();
		}
		
		VALUE Warmup() [3, 3]
		MEETS {
			Process();
		}
		
		VALUE Process() [10, 30]
		MEETS {
			Turnoff();
		}
		
		VALUE Turnoff() [3, 3]
		MEETS {
			Idle();
		}

	}
	
	COMP_TYPE SingletonStateVariable GroundStationVisibilityWindowType (Available(), NotAvailable())
	{
		VALUE Available() [1, +INF]
		MEETS {
			NotAvailable();
		}
		
		VALUE NotAvailable() [1, +INF]
		MEETS {
			Available();
		}
	}
	
	COMPONENT Satellite_Operation {FLEXIBLE op_trace(trex_internal_dispatch_asap)} : SatelliteOperationModeType;
	COMPONENT Satellite_Instrument_1 {FLEXIBLE inst1(trex_internal_dispatch_asap)} : InstrumentType;
	COMPONENT Satellite_Instrument_2 {FLEXIBLE inst2(trex_internal_dispatch_asap)} : InstrumentType;
	COMPONENT Satellite_Instrument_3 {FLEXIBLE inst3(trex_internal_dispatch_asap)} : InstrumentType;
	COMPONENT Satellite_Instrument_4 {FLEXIBLE inst4(trex_internal_dispatch_asap)} : InstrumentType;
	COMPONENT Satellite_Instrument_5 {FLEXIBLE inst5(trex_internal_dispatch_asap)} : InstrumentType;
	COMPONENT GraoundStationVisibility {FLEXIBLE window(uncontrollable)} : GroundStationVisibilityWindowType;
	
	SYNCHRONIZE Satellite_Operation.op_trace
	{
		VALUE Science()
		{
			cd0 <!> Satellite_Instrument_1.inst1.Process();
			cd1 Satellite_Operation.op_trace.Comm();
			cd2 <?> GraoundStationVisibility.window.Available();
			cd3 <!> Satellite_Instrument_2.inst2.Process();
			cd4 <!> Satellite_Instrument_3.inst3.Process();
			cd5 <!> Satellite_Instrument_4.inst4.Process();
			cd6 <!> Satellite_Instrument_5.inst5.Process();
			cd7 <!> Satellite_Instrument_5.inst5.Idle();
			
			cd8 Satellite_Instrument_1.inst1.Idle();
			cd91 Satellite_Instrument_2.inst2.Idle();
			cd92 Satellite_Instrument_2.inst2.Idle();
			cd101 Satellite_Instrument_3.inst3.Idle();
			cd102 Satellite_Instrument_3.inst3.Idle();
			cd111 Satellite_Instrument_4.inst4.Idle();
			cd112 Satellite_Instrument_4.inst4.Idle();
			cd12 Satellite_Instrument_5.inst5.Idle();
			
			CONTAINS [0, +INF] [0, +INF] cd0;
			CONTAINS [0, +INF] [0, +INF] cd3;
			CONTAINS [0, +INF] [0, +INF] cd4;
			CONTAINS [0, +INF] [0, +INF] cd5;
			CONTAINS [0, +INF] [0, +INF] cd6;
			BEFORE [0, +INF] cd1;
			
			cd1 DURING [0, +INF] [0, +INF] cd2;
			cd0 BEFORE [3, +INF] cd3;
			cd3 BEFORE [3, +INF] cd4;
			cd4 BEFORE [3, +INF] cd5;
			cd5 BEFORE [3, +INF] cd6;
			cd6 BEFORE [3, +INF] cd7;
			
			
			cd0 DURING [0, +INF] [0, +INF] cd91;
			cd0 DURING [0, +INF] [0, +INF] cd101;
			cd0 DURING [0, +INF] [0, +INF] cd111;
			cd0 DURING [0, +INF] [0, +INF] cd12;
			
			cd3 DURING [0, +INF] [0, +INF] cd8;
			cd3 DURING [0, +INF] [0, +INF] cd101;
			cd3 DURING [0, +INF] [0, +INF] cd111;
			cd3 DURING [0, +INF] [0, +INF] cd12;
			
			cd4 DURING [0, +INF] [0, +INF] cd8;
			cd4 DURING [0, +INF] [0, +INF] cd92;
			cd4 DURING [0, +INF] [0, +INF] cd111;
			cd4 DURING [0, +INF] [0, +INF] cd12;
			
			cd5 DURING [0, +INF] [0, +INF] cd8;
			cd5 DURING [0, +INF] [0, +INF] cd92;
			cd5 DURING [0, +INF] [0, +INF] cd102;
			cd5 DURING [0, +INF] [0, +INF] cd12;
			
			cd6 DURING [0, +INF] [0, +INF] cd8;
			cd6 DURING [0, +INF] [0, +INF] cd92;
			cd6 DURING [0, +INF] [0, +INF] cd102;
			cd6 DURING [0, +INF] [0, +INF] cd112;
		}
	}
}