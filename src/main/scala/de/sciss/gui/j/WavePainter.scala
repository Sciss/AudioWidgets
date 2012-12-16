package de.sciss.gui.j

import java.awt.{Stroke, BasicStroke, Paint, Color, Graphics2D}

object WavePainter {
//   trait Source {
//      def buffer: Array[ Array[ Float ]]
//      def numChannels: Int
//      def bufferSize: Int
//      def availableFrames: Int
//   }

   def sampleAndHold : OneLayer  = new SHImpl
   def linear        : OneLayer  = new LinearImpl
   def peakRMS       : PeakRMS   = new PeakRMSImpl

   private trait BasicImpl extends WavePainter {
      final val zoomX = new ZoomImpl
      final val zoomY = new ZoomImpl
   }

   private trait OneLayerImpl extends BasicImpl with OneLayer {
      final var color: Paint = Color.black

      final protected var strkVar   : Stroke = new BasicStroke( 1f )
      final protected var strkVarUp : Stroke = new BasicStroke( 16f )
      final def stroke : Stroke = strkVar
      final def stroke_=( value: Stroke ) {
         strkVar = value
         strkVarUp = value match {
            case bs: BasicStroke =>
               new BasicStroke( bs.getLineWidth * 16f, bs.getEndCap, bs.getLineJoin, bs.getMiterLimit, bs.getDashArray, bs.getDashPhase )
            case _ => value
         }
      }
   }

   private final class SHImpl extends OneLayerImpl {
      override def toString = "WavePainter.sampleAndHold@" + hashCode().toHexString

      def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) {
         val polySize   = dataLength << 1
         val polyX      = new Array[ Int ]( polySize )
         val polyY      = new Array[ Int ]( polySize )

         var di = dataOffset; var pi = 0; var i = 0; var x = zoomX( 0 ); while( i < dataLength ) {
            val y = zoomY( data( di ))
            polyX( pi ) = x
            polyY( pi ) = y
            pi += 1
            i  += 1
            x   = zoomX( i )
            polyX( pi ) = x
            polyY( pi ) = y
            pi += 1
            di += 1
         }

         val atOrig = g.getTransform
         g.scale( 0.0625, 0.0625 )
         g.setPaint( color )
         g.setStroke( strkVarUp )
         g.drawPolyline( polyX, polyY, polySize )
         g.setTransform( atOrig )
//         g.scale( 16.0, 16.0 )
      }
   }

   private final class LinearImpl extends OneLayerImpl {
      override def toString = "WavePainter.linear@" + hashCode().toHexString

      def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) {
         val polyX      = new Array[ Int ]( dataLength )
         val polyY      = new Array[ Int ]( dataLength )

         var di = dataOffset; var i = 0; while( i < dataLength ) {
            val x = zoomX( i )
            val y = zoomY( data( di ))
            polyX( i ) = x
            polyY( i ) = y
            i  += 1
            di += 1
         }

         val atOrig = g.getTransform
         g.scale( 0.0625, 0.0625 )
         g.setPaint( color )
         g.setStroke( strkVarUp )
         g.drawPolyline( polyX, polyY, dataLength )
         g.setTransform( atOrig )
      }
   }

   private final class PeakRMSImpl extends BasicImpl with PeakRMS {
      var peakColor: Paint = Color.gray
      var rmsColor: Paint  = Color.black

      def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) {
         val polySize   = dataLength * 2 // / 3
         val peakPolyX  = new Array[ Int ]( polySize )
         val peakPolyY  = new Array[ Int ]( polySize )
         val rmsPolyX   = new Array[ Int ]( polySize )
         val rmsPolyY   = new Array[ Int ]( polySize )

         var i = 0; var j = dataOffset * 3; var k = polySize - 1; while( i < dataLength ) {
            val x					= zoomX( i )
            peakPolyX( i )    = x
            peakPolyX( k )		= x
            rmsPolyX( i )     = x
            rmsPolyX( k )     = x
            val peakP         = data( j )
            j += 1
            val peakN         = data( j )
            j += 1
            peakPolyY( i )	   = zoomY( peakP ) + 8 // 2
            peakPolyY( k )		= zoomY( peakN ) - 8 // 2
            // peakC = (peakP + peakN) / 2;
            val rms           = math.sqrt( data( j )).toFloat
            j += 1
            rmsPolyY( i )     = zoomY( math.min( peakP,  rms ))
            rmsPolyY( k )		= zoomY( math.max( peakN, -rms ))
            i += 1
            k -= 1
         }

         val atOrig = g.getTransform
         g.scale( 0.0625, 0.0625 )
         g.setPaint( peakColor )
         g.fillPolygon( peakPolyX, peakPolyY, polySize )
         g.setPaint( rmsColor )
         g.fillPolygon( rmsPolyX, rmsPolyY, polySize )
         g.setTransform( atOrig )
      }
   }

   private final class ZoomImpl extends Zoom {
      private var srcLoVar = 0.0
      private var srcHiVar = 1.0
      private var tgtLoVar = 0.0
      private var tgtHiVar = 1.0

      private var preAdd   = 0.0f
      private var scale    = 1.0f
      private var postAdd  = 0.0f

      private var invalid  = false

      private def recalc() {
         val div  = srcHiVar - srcLoVar
         invalid  = div == 0.0
         if( invalid )return

         scale    = (((tgtHiVar - tgtLoVar) / div) * 16).toFloat
         preAdd   = (-srcLoVar).toFloat
         postAdd  = (tgtLoVar * 16).toFloat
      }

      def apply( in: Float ) : Int = ((in + preAdd) * scale + postAdd).toInt

      def sourceLow : Double = srcLoVar
      def sourceLow_=( value: Double ) {
         srcLoVar = value
         recalc()
      }

      def sourceHigh : Double = srcHiVar
      def sourceHigh_=( value: Double ) {
         srcHiVar = value
         recalc()
      }

      def targetLow : Double = tgtLoVar
      def targetLow_=( value: Double ) {
         tgtLoVar = value
         recalc()
      }

      def targetHigh : Double = tgtHiVar
      def targetHigh_=( value: Double ) {
         tgtHiVar = value
         recalc()
      }

//      def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) {
//         if( invalid ) return
//         val atOrig = g.getTransform
//         g.scale( scaleX, scaleY )
//         g.translate( minXVar, minYVar )
//         peer.paint( g, data, dataOffset, dataLength )
//         g.setTransform( atOrig )
//      }
   }

   private final case class PCMToPeakRMSDecimator( factor: Int ) extends Decimator {
      override def toString = "WavePainter.Decimator.pcmToPeakRMS(" + factor + ")"

      def decimate( in: Array[ Float ], inOffset: Int, out: Array[ Float ], outOffset: Int, outLength: Int ) {
         var j = outOffset * 3; val stop = j + (outLength * 3); var k = 0
         while( j < stop ) {
            val f = in( k )
            k += 1
            var f1 = f
            var f2 = f
            var f3 = f * f
            var m = 1; while( m < factor ) {
               val g = in( k )
               k += 1
               if( g > f1 ) f1 = g
               if( g < f2 ) f2 = g
               f3 += g * g
            m += 1 }
            out( j ) = f1  // positive halfwave peak
            j += 1
            out( j ) = f2  // negative halfwave peak
            j += 1
            out( j )       // fullwave mean square
            j += 1
         }
      }
   }

   trait OneLayer extends WavePainter {
      def color: Paint
      def color_=( value: Paint ) : Unit

      def stroke : Stroke
      def stroke_=( value: Stroke ) : Unit
   }

   trait PeakRMS extends WavePainter {
      def peakColor: Paint
      def peakColor_=( value: Paint ) : Unit

      def rmsColor: Paint
      def rmsColor_=( value: Paint ) : Unit
   }

   trait Zoom {
      def sourceLow: Double
      def sourceLow_=( value: Double ) :Unit

      def sourceHigh: Double
      def sourceHigh_=( value: Double ) :Unit

      def targetLow: Double
      def targetLow_=( value: Double ) :Unit

      def targetHigh: Double
      def targetHigh_=( value: Double ) :Unit

//      def logarithmic : Boolean
//      def logarithmic_=( value: Boolean ) : Unit
   }

   object Decimator {
      def pcmToPeakRMS( factor: Int ) : Decimator = new PCMToPeakRMSDecimator( factor )
   }
   trait Decimator {
      def factor: Int
      def decimate( in: Array[ Float ], inOffset: Int, out: Array[ Float ], outOffset: Int, outLength: Int ) : Unit
   }
}
trait WavePainter {
   def zoomX: WavePainter.Zoom
   def zoomY: WavePainter.Zoom
   def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) : Unit
}