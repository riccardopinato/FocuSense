package com.utilitylab.focussense

import com.utilitylab.focussense.domain.*
import org.junit.Assert.*
import org.junit.Test

class FocusScoreEngineTest {
    @Test fun perfectConditionsAreHigh(){ val s=FocusScoreEngine.liveScore(-65f,500f,5f,availability=SensorAvailability(true,true,true,true)); assertTrue(s>=95) }
    @Test fun unavailableSensorIsNotPenalized(){ val a=SensorAvailability(true,false,true,false); val s=FocusScoreEngine.liveScore(-65f,0f,5f,availability=a); assertTrue(s>=95) }
    @Test fun weightsAreRenormalizedOverAvailableSensors(){ val b=FocusScoreEngine.liveBreakdown(-65f,0f,5f,availability=SensorAvailability(true,false,true,false),weights=FocusWeights(.6f,.3f,.1f)); assertEquals(1f,b.effectiveWeights.noise+b.effectiveWeights.light+b.effectiveWeights.motion,.0001f); assertEquals(0f,b.effectiveWeights.light,.0001f) }
    @Test fun customMotionTargetChangesScore(){ val strict=FocusScoreEngine.liveScore(-65f,500f,25f,targets=FocusTargets(stableMotionIndex=10f)); val tolerant=FocusScoreEngine.liveScore(-65f,500f,25f,targets=FocusTargets(stableMotionIndex=30f)); assertTrue(tolerant>strict) }
    @Test fun noUsableSensorsReturnsZero(){ assertEquals(0,FocusScoreEngine.liveScore(-50f,500f,10f,availability=SensorAvailability(false,false,false,false))) }
}
