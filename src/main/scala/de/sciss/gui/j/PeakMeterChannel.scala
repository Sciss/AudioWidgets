package de.sciss.gui.j

trait PeakMeterChannel {
   def peak : Float
   def peak_=( value: Float ) : Unit
   def peakDecibels : Float

   /**
    * Reads the linear mean square value. Not that this is
    * not the _root_ mean square for optimization purposes.
    * The caller needs to take the square root of the returned value.
    */
   def rms : Float
   /**
    * Sets the linear mean square value. Not that this is
    * not the _root_ mean square for optimization purposes.
    */
   def rms_=( value: Float ) : Unit
   def rmsDecibels : Float

   def hold : Float
   def hold_=( value: Float ) : Unit
   def holdDecibels : Float

   def clearHold() : Unit
   def clearMeter() : Unit
}