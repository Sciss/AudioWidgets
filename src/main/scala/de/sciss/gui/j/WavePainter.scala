package de.sciss.gui.j

import java.awt.{Stroke, BasicStroke, Paint, Color, Graphics2D}

object WavePainter {
//   trait Source {
//      def buffer: Array[ Array[ Float ]]
//      def numChannels: Int
//      def bufferSize: Int
//      def availableFrames: Int
//   }

   def sampleAndHold : OneLayer = new SHImpl

   private final class SHImpl extends OneLayer {
      var color: Paint = Color.black

      private var strkVar   : Stroke = new BasicStroke( 1f )
      private var strkVarUp : Stroke = new BasicStroke( 16f )
      def stroke : Stroke = strkVar
      def stroke_=( value: Stroke ) {
         strkVar = value
         strkVarUp = value match {
            case bs: BasicStroke =>
               new BasicStroke( bs.getLineWidth * 16f, bs.getEndCap, bs.getLineJoin, bs.getMiterLimit, bs.getDashArray, bs.getDashPhase )
            case _ => value
         }
      }

      val zoomX = new ZoomImpl
      val zoomY = new ZoomImpl

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

   trait OneLayer extends WavePainter {
      def color: Paint
      def color_=( value: Paint ) : Unit
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
}
trait WavePainter {
   def zoomX: WavePainter.Zoom
   def zoomY: WavePainter.Zoom
   def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) : Unit
}