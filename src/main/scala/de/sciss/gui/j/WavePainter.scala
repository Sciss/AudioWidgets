package de.sciss.gui.j

import java.awt.{Rectangle, Stroke, BasicStroke, Paint, Color, Graphics2D}
import collection.immutable.{IndexedSeq => IIdxSeq}

object WavePainter {
   def sampleAndHold : OneLayer  = new SHImpl
   def linear        : OneLayer  = new LinearImpl
   def peakRMS       : PeakRMS   = new PeakRMSImpl

   private trait HasScalingImpl {
      final val scaleX = new ScalingImpl
      final val scaleY = new ScalingImpl
   }

   private trait OneLayerImpl extends HasScalingImpl with OneLayer {
      final var color: Paint = Color.black

      final def tupleSize = 1

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

         var di = dataOffset; var pi = 0; var i = 0; var x = (scaleX( 0 ) * 16).toInt; while( i < dataLength ) {
            val y = (scaleY( data( di )) * 16).toInt
            polyX( pi ) = x
            polyY( pi ) = y
            pi += 1
            i  += 1
            x   = (scaleX( i ) * 16).toInt
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
            val x = (scaleX( i ) * 16).toInt
            val y = (scaleY( data( di )) * 16).toInt
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

   private final class PeakRMSImpl extends HasScalingImpl with PeakRMS with HasPeakRMSImpl {
      def tupleSize = 3

      def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) {
         val polySize   = dataLength * 2 // / 3
         val peakPolyX  = new Array[ Int ]( polySize )
         val peakPolyY  = new Array[ Int ]( polySize )
         val rmsPolyX   = new Array[ Int ]( polySize )
         val rmsPolyY   = new Array[ Int ]( polySize )

         var i = 0; var j = dataOffset * 3; var k = polySize - 1; while( i < dataLength ) {
            val x					= (scaleX( i ) * 16).toInt
            peakPolyX( i )    = x
            peakPolyX( k )		= x
            rmsPolyX( i )     = x
            rmsPolyX( k )     = x
            val peakP         = data( j )
            j += 1
            val peakN         = data( j )
            j += 1
            peakPolyY( i )	   = (scaleY( peakP ) * 16).toInt + 8 // 2
            peakPolyY( k )		= (scaleY( peakN ) * 16).toInt - 8 // 2
            // peakC = (peakP + peakN) / 2
            val rms           = math.sqrt( data( j )).toFloat
            j += 1
            rmsPolyY( i )     = (scaleY( math.min( peakP,  rms )) * 16).toInt
            rmsPolyY( k )		= (scaleY( math.max( peakN, -rms )) * 16).toInt
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

   private trait ScalingImplLike extends Scaling {
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

   private final class ScalingImpl extends ScalingImplLike {
      protected def didRecalc() {}
   }

   private final case class PeakRMSDecimator( factor: Int ) extends Decimator {
      override def toString = "WavePainter.Decimator.peakRMS(" + factor + ")"

      def tupleInSize   = 3
      def tupleOutSize  = 3

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

      def tupleInSize   = 1
      def tupleOutSize  = 3

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

//   trait HasZoom {
//      def zoomX: WavePainter.Zoom
//      def zoomY: WavePainter.Zoom
//   }

   trait Scaling {
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
      def dummy : Decimator = Dummy

      private object Dummy extends Decimator {
         def factor        = 1
         def tupleInSize   = 1
         def tupleOutSize  = 1
         def decimate( in: Array[ Float ], inOffset: Int, out: Array[ Float ], outOffset: Int, outLength: Int ) {}
      }
   }
   trait Decimator {
      def tupleInSize: Int
      def tupleOutSize: Int
      def factor: Int
      def decimate( in: Array[ Float ], inOffset: Int, out: Array[ Float ], outOffset: Int, outLength: Int ) : Unit
   }

   object MultiResolution {
      def apply( source: Source, placement: ChannelPlacement ) : MultiResolution =
         new MultiResImpl( source, placement )

      trait Reader {
         def decimationFactor : Int
         def tupleSize : Int
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
   trait MultiResolution extends HasPeakRMS {
      def magLow : Double
      def magLow_=( value: Double ) : Unit
      def magHigh : Double
      def magHigh_=( value: Double ) : Unit

      def startFrame : Long
      def startFrame_=( value: Long ) : Unit
      def stopFrame: Long
      def stopFrame_=( value: Long ) : Unit

      def paint( g: Graphics2D ) : Unit
   }

   private final class MultiResImpl( source: MultiResolution.Source, placement: MultiResolution.ChannelPlacement )
   extends MultiResolution {
      override def toString = "MultiResolution@" + hashCode().toHexString

      private def setZoomY() {
         if( magLow == magHigh ) {
            validZoom = false
            return
         }
         val zy         = pnt.scaleY
         zy.sourceLow   = magLow
         zy.sourceHigh  = magHigh
         zy.targetLow   = rectCache( 0 ).height - 1
         zy.targetHigh  = 0
         magDirty       = false
      }

      private val readers     = source.readers.sortBy( _.decimationFactor )
      private val numReaders  = readers.size
      private val rect        = new Rectangle()
      private val rectCache   = Array.fill( source.numChannels )( new Rectangle() )

      private var validZoom   = true

      private val pntSH       = WavePainter.sampleAndHold
      private val pntLin      = WavePainter.linear
      private val pntDecim    = WavePainter.peakRMS

      private var pnt : WavePainter = pntLin
      private var reader            = readers.head
      private var decimStart        = 0L
      private var decimFrames       = 1
      private var decimTuples       = 1
      private var decimInline       = Decimator.dummy

      recalcDecim()

      def peakColor: Paint = pntDecim.peakColor
      def peakColor_=( value: Paint ) {
         pntDecim.peakColor   = value
      }
      def rmsColor: Paint = pntDecim.rmsColor
      def rmsColor_=( value: Paint ) {
         pntSH.color          = value
         pntLin.color         = value
         pntDecim.rmsColor    = value
      }

      private var spanDirty      = true
      private var startFrameVar  = 0L
      private var stopFrameVar   = 1L

      def startFrame : Long = startFrameVar
      def startFrame_=( value: Long ) {
         if( startFrameVar != value ) {
            startFrameVar  = value
            spanDirty      = true
         }
      }

      def stopFrame: Long = stopFrameVar
      def stopFrame_=( value: Long ) {
         if( stopFrameVar != value ) {
            stopFrameVar   = value
            spanDirty      = true
         }
      }

      private var magDirty    = true
      private var magLowVar   = -1.0
      private var magHighVar  = 1.0

      def magLow : Double = magLowVar
      def magLow_=( value: Double ) {
         if( magLowVar != value ) {
            magLowVar   = value
            magDirty    = true
         }
      }

      def magHigh : Double = magHighVar
      def magHigh_=( value: Double ) {
         if( magHighVar != value ) {
            magHighVar  = value
            magDirty    = true
         }
      }

      private def recalcDecim() {
         val numFrames  = stopFrame - startFrame // zoomX.sourceHigh - zoomX.sourceLow
         if( numFrames <= 0L ) {
            validZoom = false
            return
         }
         val numPixels  = rectCache( 0 ).width
         if( numPixels <= 0 ) {
            validZoom = false
            return
         }

         val sh        = (numPixels >> 2) > numFrames
//         val dispDecim = numFrames.toDouble / numPixels
         val dispDecim = ((numFrames + numPixels - 1) / numPixels).toInt
         var i    = 0
         while( i < numReaders && readers( i ).decimationFactor < dispDecim ) i += 1
         i        = math.max( 0, i - 1 )
         reader   = readers( i )
         val fRead = reader.decimationFactor
//         val oldPnt = pnt
         decimTuples = reader.tupleSize
         val readIsPCM = decimTuples == 1
         if( !readIsPCM && decimTuples != 3 ) {
            validZoom = false
            return
         }

         val fInline0   = math.max( 1, (dispDecim / fRead) /* .toInt */ )
         // tricky: current decimation algorithm assumes write head is slower than read head.
         // but in the case of PCMtoPeakRMS, this holds only for an inline decimation factor
         // of >= 3. thus, if reader is PCM, we use inline decimation only for factors >= 3
         val fInline    = if( readIsPCM && fInline0 < 3 ) 1 else fInline0
         if( fInline > 1 ) {
            decimInline = if( decimTuples == 1 ) {
               Decimator.pcmToPeakRMS( fInline )
            } else {
               Decimator.peakRMS( fInline )
            }
            pnt = pntDecim
         } else {
            decimInline = Decimator.dummy
            pnt = if( decimTuples == 1 ) {
               if( sh /* dispDecim <= 0.25 */ ) pntSH else pntLin
            } else {
               pntDecim
            }
         }
         val fPaint     = fRead * fInline

         validZoom = true

         decimStart     = startFrame / fPaint
//         val decimStop  = (stopFrame + fPaint - 1) / fPaint
         val decimStop  = (stopFrame + fRead - 1) / fPaint
         decimFrames    = (decimStop - decimStart).toInt // math.ceil( numFrames / reader.decimationFactor ).toInt

//println( "reader " + reader.decimationFactor + "; decimStart = " + decimStart + "; decimStop = " + decimStop + "; inlineFactor = " + decimInline.factor )

         // `decimStart * f` is `<= startFrame`, due to truncation (floor)
         // for full scale, the zoom X source low, which is counted form zero, would thus  be
         // `startFrame - decimStart * f`, and the decimated zoom X source low would thus be
         // `startFrame.toDouble/f - decimStart` (source low is _subtracted_ from the x count)
         val zx         = pnt.scaleX
         zx.sourceLow   = (startFrame.toDouble / fPaint) - decimStart
         zx.sourceHigh  = zx.sourceLow + (numFrames.toDouble / fPaint) // correct?
         zx.targetLow   = 0.0
         zx.targetHigh  = numPixels
      }

      def paint( g: Graphics2D ) {
         val numCh      = source.numChannels

         var rectDirty  = false
         var ch = 0; while( ch < numCh ) {
            placement.rectangleForChannel( ch, rect )
            val cr = rectCache( ch )
            if( cr.x != rect.x || cr.y != rect.y || cr.width != rect.width || cr.height != rect.height ) {
               cr.setBounds( rect )
               rectDirty = true
            }
         ch +=1 }

         if( rectDirty || spanDirty ) {
            recalcDecim()
            spanDirty      = false
            setZoomY()
         } else if( magDirty ) {
            setZoomY()
         }
         if( !validZoom ) return

         val clipOrig   = g.getClip
         val atOrig     = g.getTransform
         val readFrames = decimFrames * decimInline.factor
         val data       = Array.ofDim[ Float ]( numCh, readFrames * decimTuples )
         val success    = reader.read( data, 0, decimStart, readFrames )
         if( !success ) return   // XXX TODO: paint busy rectangle

         ch = 0; while( ch < numCh ) {
            try {
               val r    = rectCache( ch )
               val dch  = data( ch )
               decimInline.decimate( dch, 0, dch, 0, decimFrames )
               g.clipRect( r.x, r.y, r.width, r.height )
               g.translate( r.x, r.y )
               pnt.paint( g, dch, 0, decimFrames )

            } finally {
               g.setTransform( atOrig )
               g.setClip( clipOrig )
            }
         ch += 1 }
      }
   }
}
trait WavePainter {
   def scaleX : WavePainter.Scaling
   def scaleY : WavePainter.Scaling
   def tupleSize : Int
   def paint( g: Graphics2D, data: Array[ Float ], dataOffset: Int, dataLength: Int ) : Unit
}