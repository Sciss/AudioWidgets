package de.sciss.gui.j

import java.awt.{Rectangle, Stroke, BasicStroke, Paint, Color, Graphics2D}
import collection.immutable.{IndexedSeq => IIdxSeq}

object WavePainter {
   def sampleAndHold : OneLayer  = new SHImpl
   def linear        : OneLayer  = new LinearImpl
   def peakRMS       : PeakRMS   = new PeakRMSImpl

   private trait HasZoomImpl {
      final val zoomX = new ZoomImpl
      final val zoomY = new ZoomImpl
   }

   private trait OneLayerImpl extends HasZoomImpl with OneLayer {
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

         var di = dataOffset; var pi = 0; var i = 0; var x = (zoomX( 0 ) * 16).toInt; while( i < dataLength ) {
            val y = (zoomY( data( di )) * 16).toInt
            polyX( pi ) = x
            polyY( pi ) = y
            pi += 1
            i  += 1
            x   = (zoomX( i ) * 16).toInt
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
            val x = (zoomX( i ) * 16).toInt
            val y = (zoomY( data( di )) * 16).toInt
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

   private trait HasPeakRMSImpl {
      var peakColor: Paint = Color.gray
      var rmsColor: Paint  = Color.black
   }

   private final class PeakRMSImpl extends HasZoomImpl with PeakRMS with HasPeakRMSImpl {

      def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) {
         val polySize   = dataLength * 2 // / 3
         val peakPolyX  = new Array[ Int ]( polySize )
         val peakPolyY  = new Array[ Int ]( polySize )
         val rmsPolyX   = new Array[ Int ]( polySize )
         val rmsPolyY   = new Array[ Int ]( polySize )

         var i = 0; var j = dataOffset * 3; var k = polySize - 1; while( i < dataLength ) {
            val x					= (zoomX( i ) * 16).toInt
            peakPolyX( i )    = x
            peakPolyX( k )		= x
            rmsPolyX( i )     = x
            rmsPolyX( k )     = x
            val peakP         = data( j )
            j += 1
            val peakN         = data( j )
            j += 1
            peakPolyY( i )	   = (zoomY( peakP ) * 16).toInt + 8 // 2
            peakPolyY( k )		= (zoomY( peakN ) * 16).toInt - 8 // 2
            // peakC = (peakP + peakN) / 2
            val rms           = math.sqrt( data( j )).toFloat
            j += 1
            rmsPolyY( i )     = (zoomY( math.min( peakP,  rms )) * 16).toInt
            rmsPolyY( k )		= (zoomY( math.max( peakN, -rms )) * 16).toInt
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

   private trait ZoomImplLike extends Zoom {
      private var srcLoVar = 0.0
      private var srcHiVar = 1.0
      private var tgtLoVar = 0.0
      private var tgtHiVar = 1.0

      final def apply( in: Double )    : Double = (in  + preAdd)  * scale + postAdd
      final def unapply( out: Double ) : Double = (out - postAdd) / scale - preAdd

      final def sourceLow : Double = srcLoVar
      final def sourceLow_=( value: Double ) {
         srcLoVar = value
         recalc()
      }

      final def sourceHigh : Double = srcHiVar
      final def sourceHigh_=( value: Double ) {
         srcHiVar = value
         recalc()
      }

      final def targetLow : Double = tgtLoVar
      final def targetLow_=( value: Double ) {
         tgtLoVar = value
         recalc()
      }

      final def targetHigh : Double = tgtHiVar
      final def targetHigh_=( value: Double ) {
         tgtHiVar = value
         recalc()
      }

      private var preAdd   = 0.0
      private var scale    = 1.0
      private var postAdd  = 0.0

      private var invalid  = false

      private def recalc() {
         val div  = srcHiVar - srcLoVar
         invalid  = div == 0.0
         if( invalid )return

         scale    = ((tgtHiVar - tgtLoVar) / div) // * 16
         preAdd   = -srcLoVar
         postAdd  = tgtLoVar // * 16

         didRecalc()
      }

      protected def didRecalc() : Unit
   }

   private final class ZoomImpl extends ZoomImplLike {
      protected def didRecalc() {}
   }

   private final case class PeakRMSDecimator( factor: Int ) extends Decimator {
      override def toString = "WavePainter.Decimator.peakRMS(" + factor + ")"

      def decimate( in: Array[ Float ], inOffset: Int, out: Array[ Float ], outOffset: Int, outLength: Int ) {
         var j = outOffset * 3; val stop = j + (outLength * 3); var k = inOffset * 3
         while( j < stop ) {
            var f1 = in( k )
            k += 1
            var f2 = in( k )
            k += 1
            var f3 = in( k )
            k += 1
            var m = 1; while( m < factor ) {
               val f5 = in( k )
               k += 1
               if( f5 > f1 ) f1 = f5
               val f6 = in( k )
               k += 1
               if( f6 < f2 ) f2 = f6
               f3 += in( k )
               k += 1
               m += 1
            }
            out( j ) = f1
            j += 1
            out( j ) = f2
            j += 1
            out( j ) = f3 / factor
            j += 1
         }
      }
   }

   private final case class PCMToPeakRMSDecimator( factor: Int ) extends Decimator {
      override def toString = "WavePainter.Decimator.pcmToPeakRMS(" + factor + ")"

      def decimate( in: Array[ Float ], inOffset: Int, out: Array[ Float ], outOffset: Int, outLength: Int ) {
         var j = outOffset * 3; val stop = j + (outLength * 3); var k = inOffset
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
            out( j ) = f1           // positive halfwave peak
            j += 1
            out( j ) = f2           // negative halfwave peak
            j += 1
            out( j ) = f3 / factor  // fullwave mean square
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

   trait HasPeakRMS {
      def peakColor: Paint
      def peakColor_=( value: Paint ) : Unit

      def rmsColor: Paint
      def rmsColor_=( value: Paint ) : Unit
   }

   trait PeakRMS extends WavePainter with HasPeakRMS

   trait HasZoom {
      def zoomX: WavePainter.Zoom
      def zoomY: WavePainter.Zoom
   }

   trait Zoom {
      def sourceLow: Double
      def sourceLow_=( value: Double ) : Unit

      def sourceHigh: Double
      def sourceHigh_=( value: Double ) : Unit

      def targetLow: Double
      def targetLow_=( value: Double ) : Unit

      def targetHigh: Double
      def targetHigh_=( value: Double ) : Unit

//      def logarithmic : Boolean
//      def logarithmic_=( value: Boolean ) : Unit
   }

   object Decimator {
      def pcmToPeakRMS( factor: Int ) : Decimator = new PCMToPeakRMSDecimator( factor )
      def peakRMS(      factor: Int ) : Decimator = new PeakRMSDecimator(      factor )
   }
   trait Decimator {
      def factor: Int
      def decimate( in: Array[ Float ], inOffset: Int, out: Array[ Float ], outOffset: Int, outLength: Int ) : Unit
   }

   object MultiResolution {
      def apply( source: Source, placement: ChannelPlacement ) : MultiResolution =
         new MultiResImpl( source, placement )

      trait Reader {
         def decimationFactor : Int
         def available( sourceOffset: Long, length: Int ) : IIdxSeq[ Int ]
         def read( buf: Array[ Array[ Float ]], bufOffset: Int, sourceOffset: Long, length: Int ) : Boolean
      }

      trait Source {
         def numChannels : Int
         def numFrames : Long
         def readers : IIdxSeq[ Reader ]
      }

      trait ChannelPlacement {
         def rectangleForChannel( ch: Int, result: Rectangle ) : Unit
      }
   }
   trait MultiResolution extends HasZoom with HasPeakRMS {
      def paint( g: Graphics2D ) : Unit
   }

   private final class MultiResImpl( source: MultiResolution.Source, placement: MultiResolution.ChannelPlacement )
   extends MultiResolution {
      override def toString = "MultiResolution@" + hashCode().toHexString

      object zoomX extends ZoomImplLike {
         protected def didRecalc() {
            recalcDecim()
         }
      }
      object zoomY extends ZoomImplLike {
         protected def didRecalc() {
            setZoomY()
         }
      }

      private def setZoomY() {
         import zoomY._
         val zy         = pnt.zoomY
         zy.sourceLow   = sourceLow
         zy.sourceHigh  = sourceHigh
         zy.targetLow   = targetLow
         zy.targetHigh  = targetHigh
      }

      private val readers     = source.readers.sortBy( _.decimationFactor )
      private val numReaders  = readers.size
      private val rect        = new Rectangle()

      private var dispDecim   = 1.0
      private var validZoom   = true

      private val pntSH       = WavePainter.sampleAndHold
      private val pntLin      = WavePainter.linear
      private val pntDecim    = WavePainter.peakRMS

      private var pnt : WavePainter = pntLin
      private var reader            = readers.head
      private var decimStart        = 0L
      private var decimFrames       = 1

      recalcDecim()

      def peakColor: Paint = pntDecim.peakColor
      def peakColor_=( value: Paint ) {
         pntSH.color          = value
         pntLin.color         = value
         pntDecim.peakColor   = value
      }
      def rmsColor: Paint = pntDecim.rmsColor
      def rmsColor_=( value: Paint ) {
         pntDecim.rmsColor = value
      }

      private def recalcDecim() {
         val numFrames  = zoomX.sourceHigh - zoomX.sourceLow
         val numPixels  = zoomX.targetHigh - zoomX.targetLow
         validZoom      = numFrames != 0 && numPixels != 0
         if( !validZoom ) return
         dispDecim      = numFrames / numPixels

         var i    = 0
         while( i < numReaders && readers( i ).decimationFactor < dispDecim ) i += 1
         i        = math.max( 0, i - 1 )
         reader   = readers( i )
         val f    = reader.decimationFactor
         val oldPnt = pnt
         pnt      = if( f == 1 ) {
            if( dispDecim <= 0.25 ) pntSH else pntLin
         } else {
            pntDecim
         }

         val frameStart = math.floor( zoomX.sourceLow  )
         val frameStop  = math.ceil(  zoomX.sourceHigh )
         val frameStartL= frameStart.toLong
         val frameStopL = frameStop.toLong
         decimStart     = frameStartL / f
         val decimStop  = (frameStopL + f - 1) / f
         decimFrames    = (decimStop - decimStart).toInt // math.ceil( numFrames / reader.decimationFactor ).toInt

         val floorTgtLo = zoomX( frameStart )
         val ceilTgtHi  = zoomX( frameStop )
         val zx         = pnt.zoomX
         zx.sourceLow   = frameStart
         zx.sourceHigh  = frameStop
         zx.targetLow   = floorTgtLo
         zx.targetHigh  = ceilTgtHi
         if( pnt ne oldPnt ) setZoomY()
      }

      def paint( g: Graphics2D ) {
         if( !validZoom ) return
         val clipOrig   = g.getClip
         val atOrig     = g.getTransform
         val numCh      = source.numChannels
         val data       = Array.ofDim[ Float ]( numCh, decimFrames )
         val success    = reader.read( data, 0, decimStart, decimFrames )
         if( !success ) return   // XXX TODO: paint busy rectangle

         var ch = 0; while( ch < numCh ) {
            placement.rectangleForChannel( ch, rect )
            try {
               g.clipRect( rect.x, rect.y, rect.width, rect.height )
               g.translate( rect.x, rect.y )
               pnt.paint( g, data( ch ), 0, decimFrames )
               // ...

            } finally {
               g.setTransform( atOrig )
               g.setClip( clipOrig )
            }
         ch += 1 }
      }
   }
}
trait WavePainter extends WavePainter.HasZoom {
   def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) : Unit
}