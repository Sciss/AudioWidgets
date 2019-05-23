/*
 *  WavePainter.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets.j

import java.awt.event.{ActionEvent, InputEvent, KeyEvent, MouseWheelEvent, MouseWheelListener}
import java.awt.{BasicStroke, Color, Dimension, Graphics2D, Paint, Point, Stroke, Toolkit}
import javax.swing.{AbstractAction, Action, JComponent, KeyStroke}

import scala.collection.immutable.{IndexedSeq => Vec}

object WavePainter {
  def sampleAndHold: OneLayer  = new SHImpl
  def linear       : OneLayer  = new LinearImpl
  def peakRMS      : PeakRMS   = new PeakRMSImpl

  private trait HasScalingImpl {
    final val scaleX = new ScalingImpl
    final val scaleY = new ScalingImpl
  }

  private trait OneLayerImpl extends HasScalingImpl with OneLayer {
    final var color: Paint = Color.black

    final def tupleSize = 1

    final protected var strkVar  : Stroke = new BasicStroke(1f)
    final protected var strkVarUp: Stroke = new BasicStroke(16f)

    final def stroke: Stroke = strkVar
    final def stroke_=(value: Stroke): Unit = {
      strkVar = value
      strkVarUp = value match {
        case bs: BasicStroke =>
          new BasicStroke(bs.getLineWidth * 16f, bs.getEndCap, bs.getLineJoin, bs.getMiterLimit, bs.getDashArray, bs.getDashPhase)
        case _ => value
      }
    }
  }

  private final class SHImpl extends OneLayerImpl {
    override def toString = s"WavePainter.sampleAndHold@${hashCode().toHexString}"

    def paint(g: Graphics2D, data: Array[Float], dataOffset: Int, dataLength: Int): Unit = {
      val polySize  = dataLength << 1
      val polyX     = new Array[Int](polySize)
      val polyY     = new Array[Int](polySize)

      var di = dataOffset; var pi = 0; var i = 0; var x = (scaleX(0) * 16).toInt; while (i < dataLength) {
        val y     = (scaleY(data(di)) * 16).toInt
        polyX(pi) = x
        polyY(pi) = y
        pi       += 1
        i        += 1
        x         = (scaleX(i) * 16).toInt
        polyX(pi) = x
        polyY(pi) = y
        pi       += 1
        di       += 1
      }

      val atOrig = g.getTransform
      g.scale(0.0625, 0.0625)
      g.setPaint(color)
      g.setStroke(strkVarUp)
      g.drawPolyline(polyX, polyY, polySize)
      g.setTransform(atOrig)
      //         g.scale( 16.0, 16.0 )
    }
  }

  private final class LinearImpl extends OneLayerImpl {
    override def toString = s"WavePainter.linear@${hashCode().toHexString}"

    def paint(g: Graphics2D, data: Array[Float], dataOffset: Int, dataLength: Int): Unit = {
      val polyX = new Array[Int](dataLength)
      val polyY = new Array[Int](dataLength)

      var di = dataOffset; var i = 0; while (i < dataLength) {
        val x = (scaleX(i)        * 16).toInt
        val y = (scaleY(data(di)) * 16).toInt
        polyX(i) = x
        polyY(i) = y
        i  += 1
        di += 1
      }

      val atOrig = g.getTransform
      g.scale(0.0625, 0.0625)
      g.setPaint(color)
      g.setStroke(strkVarUp)
      g.drawPolyline(polyX, polyY, dataLength)
      g.setTransform(atOrig)
    }
  }

  private trait HasPeakRMSImpl {
    var peakColor: Paint = Color.gray
    var rmsColor : Paint = Color.black
  }

  private final class PeakRMSImpl extends HasScalingImpl with PeakRMS with HasPeakRMSImpl {
    def tupleSize = 3

    def paint(g: Graphics2D, data: Array[Float], dataOffset: Int, dataLength: Int): Unit = {
      val polySize  = dataLength * 2 // / 3
      val peakPolyX = new Array[Int](polySize)
      val peakPolyY = new Array[Int](polySize)
      val rmsPolyX  = new Array[Int](polySize)
      val rmsPolyY  = new Array[Int](polySize)

      var i = 0; var j = dataOffset * 3; var k = polySize - 1; while (i < dataLength) {
        val x         = (scaleX(i) * 16).toInt
        peakPolyX(i)  = x
        peakPolyX(k)  = x
        rmsPolyX(i)   = x
        rmsPolyX(k)   = x
        val peakP     = data(j)
        j += 1
        val peakN     = data(j)
        j += 1
        peakPolyY(i)  = (scaleY(peakP) * 16).toInt + 8 // 2
        peakPolyY(k)  = (scaleY(peakN) * 16).toInt - 8 // 2
        // peakC = (peakP + peakN) / 2
        val rms       = math.sqrt(data(j)).toFloat
        j += 1
        rmsPolyY(i)   = (scaleY(math.min(peakP, rms)) * 16).toInt
        rmsPolyY(k)   = (scaleY(math.max(peakN, -rms)) * 16).toInt
        i += 1
        k -= 1
      }

      val atOrig = g.getTransform
      g.scale(0.0625, 0.0625)
      g.setPaint(peakColor)
      g.fillPolygon(peakPolyX, peakPolyY, polySize)
      g.setPaint(rmsColor)
      g.fillPolygon(rmsPolyX, rmsPolyY, polySize)
      g.setTransform(atOrig)
    }
  }

  private trait ScalingImplLike extends Scaling {
    private var srcLoVar = 0.0
    private var srcHiVar = 1.0
    private var tgtLoVar = 0.0
    private var tgtHiVar = 1.0

    final def apply  (in : Double): Double = (in  + preAdd ) * scale + postAdd
    final def unapply(out: Double): Double = (out - postAdd) / scale - preAdd

    final def sourceLow: Double = srcLoVar
    final def sourceLow_=(value: Double): Unit = {
      srcLoVar = value
      recalc()
    }

    final def sourceHigh: Double = srcHiVar
    final def sourceHigh_=(value: Double): Unit = {
      srcHiVar = value
      recalc()
    }

    final def targetLow: Double = tgtLoVar
    final def targetLow_=(value: Double): Unit = {
      tgtLoVar = value
      recalc()
    }

    final def targetHigh: Double = tgtHiVar
    final def targetHigh_=(value: Double): Unit = {
      tgtHiVar = value
      recalc()
    }

    private var preAdd  = 0.0
    private var scale   = 1.0
    private var postAdd = 0.0

    private var invalid = false

    private def recalc(): Unit = {
      val div = srcHiVar - srcLoVar
      invalid = div == 0.0
      if (invalid) return

      scale   = (tgtHiVar - tgtLoVar) / div // * 16
      preAdd  = -srcLoVar
      postAdd = tgtLoVar // * 16

      didRecalc()
    }

    protected def didRecalc(): Unit
  }

  private final class ScalingImpl extends ScalingImplLike {
    protected def didRecalc(): Unit = ()
  }

  private final case class PeakRMSDecimator(factor: Int) 
    extends Decimator {
    
    override def toString = s"WavePainter.Decimator.peakRMS($factor)"

    def tupleInSize  = 3
    def tupleOutSize = 3

    def decimate(in: Array[Float], inOffset: Int, out: Array[Float], outOffset: Int, outLength: Int): Unit = {
      var j     = outOffset * 3
      val stop  = j + (outLength * 3)
      var k     = inOffset * 3
      while (j < stop) {
        var f1 = in(k)
        k += 1
        var f2 = in(k)
        k += 1
        var f3 = in(k)
        k += 1
        var m = 1
        while (m < factor) {
          val f5 = in(k)
          k += 1
          if (f5 > f1) f1 = f5
          val f6 = in(k)
          k += 1
          if (f6 < f2) f2 = f6
          f3 += in(k)
          k += 1
          m += 1
        }
        out(j) = f1
        j += 1
        out(j) = f2
        j += 1
        out(j) = f3 / factor
        j += 1
      }
    }
  }

  private final case class PCMToPeakRMSDecimator(factor: Int) 
    extends Decimator {
    
    override def toString = s"WavePainter.Decimator.pcmToPeakRMS($factor)"

    def tupleInSize  = 1
    def tupleOutSize = 3

    def decimate(in: Array[Float], inOffset: Int, out: Array[Float], outOffset: Int, outLength: Int): Unit = {
      var j     = outOffset * 3
      val stop  = j + (outLength * 3)
      var k     = inOffset
      while (j < stop) {
        val f = in(k)
        k += 1
        var f1 = f
        var f2 = f
        var f3 = f * f
        var m = 1
        while (m < factor) {
          val g = in(k)
          k += 1
          if (g > f1) f1 = g
          if (g < f2) f2 = g
          f3 += g * g
          m += 1
        }
        out(j) = f1 // positive half-wave peak
        j += 1
        out(j) = f2 // negative half-wave peak
        j += 1
        out(j) = f3 / factor // full-wave mean square
        j += 1
      }
    }
  }

  trait OneLayer extends WavePainter {
    var color : Paint
    var stroke: Stroke
  }

  trait HasPeakRMS {
    var peakColor: Paint
    var rmsColor : Paint
  }

  trait PeakRMS extends WavePainter with HasPeakRMS

  //   trait HasZoom {
  //      def zoomX: WavePainter.Zoom
  //      def zoomY: WavePainter.Zoom
  //   }

  trait Scaling {
    var sourceLow : Double
    var sourceHigh: Double

    var targetLow : Double
    var targetHigh: Double

    //      var logarithmic : Boolean
  }

  object Decimator {
    def pcmToPeakRMS(factor: Int): Decimator = PCMToPeakRMSDecimator(factor)
    def peakRMS     (factor: Int): Decimator = PeakRMSDecimator     (factor)

    def dummy: Decimator = Dummy

    private final val ln32 = math.log(32)

    def suggest(numFrames: Long): Vec[Decimator] = {
      val numDecim = (math.log(numFrames) / ln32 - 1).toInt
      if (numDecim <= 0) return Vec.empty

      // decimate in steps of 32
      pcmToPeakRMS(32) +: Vec.tabulate(numDecim - 1)(i => peakRMS(32 << (i * 5)))
    }

    private object Dummy extends Decimator {
      val factor        = 1
      val tupleInSize   = 1
      val tupleOutSize  = 1

      def decimate(in: Array[Float], inOffset: Int, out: Array[Float], outOffset: Int, outLength: Int): Unit = ()
    }
  }

  trait Decimator {
    def tupleInSize : Int
    def tupleOutSize: Int
    def factor      : Int

    def decimate(in: Array[Float], inOffset: Int, out: Array[Float], outOffset: Int, outLength: Int): Unit
  }

  object MultiResolution {
    def apply(source: Source, display: Display): MultiResolution = new MultiResImpl(source, display)

    trait Reader {
      def decimationFactor: Int
      def tupleSize       : Int

      def available(sourceOffset: Long, length: Int): Vec[Int]

      def read(buf: Array[Array[Float]], bufOffset: Int, sourceOffset: Long, length: Int): Boolean
    }

    object Source {
      def wrap(data: Array[Array[Float]], numFrames: Int = -1): Source = {
        val numCh = data.length
        val numF  = if (numFrames == -1) {
          if (numCh > 0) data(0).length else 0
        } else numFrames

        val decim   = Decimator.suggest(numF)
        val tailBuf: Array[Float] = if (decim.nonEmpty) new Array(decim.map(_.factor).max * 3) else null
        var prevBuf = data
        var prevSz  = numF
        val fullR   = new ArrayReader(data, 1)
        var fTot    = 1
        val decimR  = decim.map { d =>
          val f         = d.factor
          fTot         *= f
          val decimSzF  = prevSz / f
          val decimSz   = (prevSz + f - 1) / f
          val decimBuf  = Array.ofDim[Float](numCh, decimSz * d.tupleOutSize)
          var ch = 0
          while (ch < numCh) {
            val pch = prevBuf(ch)
            val dch = decimBuf(ch)
            d.decimate(pch, 0, dch, 0, decimSzF)
            if (decimSzF < decimSz) {
              // System.arraycopy( pch, decimSzF * f, tailBuf, 0, f * d.tupleInSize )
              // d.decimate( tailBuf, 0, dch, decimSzF, 1 )

              // XXX TODO: somehow there are still 1 or 2 pixels of black remaining...
              val off  = decimSzF * f
              var tlen = prevSz - off
              System.arraycopy(pch, off, tailBuf, 0, tlen)
              val stop = f * d.tupleInSize
              while (tlen < stop) {
                tailBuf(tlen) = 0f; tlen += 1
              }
              d.decimate(tailBuf, 0, dch, decimSzF, 1)
            }
            ch += 1
          }
          prevBuf = decimBuf
          prevSz  = decimSz
          new ArrayReader(decimBuf, fTot)
        }

        new WrapImpl(data, numCh, numF.toLong, fullR +: decimR)
      }

      private final class ArrayReader(data: Array[Array[Float]], val decimationFactor: Int)
        extends Reader {

        val tupleSize: Int = if (decimationFactor == 1) 1 else 3

        def available(srcOff: Long, len: Int): Vec[Int] = Vector(0, len)

        def read(buf: Array[Array[Float]], bufOff: Int, srcOff: Long, len: Int): Boolean = {
          val bufOffT = bufOff * tupleSize
          val srcOffT = srcOff.toInt * tupleSize
          var ch = 0
          while (ch < buf.length) {
            val bch  = buf(ch)
            val dch  = data(ch)
            var i    = bufOffT
            var j    = srcOffT
            val stop = j + len * tupleSize
            while (j < stop) {
              bch(i) = dch(j)
              i     += 1
              j     += 1
            }
            ch += 1
          }
          true
        }
      }

      private final class WrapImpl(data: Array[Array[Float]], val numChannels: Int, val numFrames: Long,
                                   val readers: Vec[Reader])
        extends Source {

        override def toString = s"MultiResolution.Source.wrap@${hashCode().toHexString}"
      }
    }

    trait Source {
      def numChannels: Int
      def numFrames  : Long

      def readers: Vec[Reader]
    }
  }

  trait Display {
    def numChannels: Int
    def numFrames  : Long

    def refreshAllChannels(): Unit

    def channelDimension(result: Dimension): Unit

    def channelLocation(ch: Int, result: Point): Unit
  }

  object HasZoom {
    private abstract class ActionImpl extends AbstractAction with InstallableAction {
      def install(component: JComponent, condition: Int): Unit = {
        val amap  = component.getActionMap
        val imap  = component.getInputMap(condition)
        val id    = getValue(Action.ACTION_COMMAND_KEY)
        amap.put(id, this)
        val ks = getValue(Action.ACCELERATOR_KEY).asInstanceOf[KeyStroke]
        imap.put(ks, id)
      }

      final def actionPerformed(e: ActionEvent): Unit = perform()

      protected def perform(): Unit
    }

    private val chanDim   = new Dimension()
    private val chanPoint = new Point()

    private def hZoom(zoom: HasZoom, display: Display, factor: Double, fixDisplayPos: Int): Unit = {
      val visiStart = zoom.startFrame
      val visiStop  = zoom.stopFrame
      val visiLen   = visiStop - visiStart

      val (newStart, newStop) = if (factor == Float.PositiveInfinity) {
        // zoom all out
        (0L, display.numFrames)
      } else {
        display.channelDimension(chanDim)
        val w = chanDim.width
        val targetLen = if (factor == 0.0) w.toLong else (factor * visiLen + 0.5).toLong
        if (targetLen < 4) return

        val fixRel   = fixDisplayPos.toDouble / w
        val fixFrame = fixRel * visiLen + visiStart
        val stop     = math.min(display.numFrames, (fixFrame + (1 - fixRel) * targetLen + 0.5).toLong)
        val start    = math.max(0L, stop - targetLen)
        (start, stop)
      }

      val newVisiIsEmpty = newStart >= newStop
      if (!newVisiIsEmpty) {
        zoom.startFrame = newStart
        zoom.stopFrame  = newStop
        display.refreshAllChannels()
      }
    }

    private final class ActionSpanWidth(zoom: HasZoom, display: Display, factor: Double)
      extends ActionImpl {

      def perform(): Unit = hZoom(zoom, display, factor, 0)
    }

    private def vMaxZoom(zoom: HasZoom, display: Display, factor: Double): Unit = {
      val min = zoom.magLow
      val max = zoom.magHigh

      if (((factor >= 1.0) && (min > -1.0e6) && (max < 1.0e6)) || (factor < 1.0 && (min < -1.0e-4) && (max > 1.0e-4))) {
        zoom.magLow = min * factor
        zoom.magHigh = max * factor
        display.refreshAllChannels()
      }
    }

    private def linexp(x: Double, srcLo: Double, srcHi: Double, dstLo: Double, dstHi: Double) =
      math.pow(dstHi / dstLo, (x - srcLo) / (srcHi - srcLo)) * dstLo

    private final class ActionVerticalMax(zoom: HasZoom, display: Display, factor: Double)
      extends ActionImpl {

      def perform(): Unit = vMaxZoom(zoom, display, factor)
    }

    private final class MouseWheelImpl(zoom: HasZoom, display: Display,
                                       sensitivity: Double, zoomModifiers: Int, horizontalScroll: Boolean)
      extends MouseWheelListener {
      // OS X special handling: shift modifier indicates horizontal wheel
      private val handleHoriz = (zoomModifiers & InputEvent.SHIFT_MASK) == 0

      def mouseWheelMoved(e: MouseWheelEvent): Unit = {
        val mods    = e.getModifiers
        val wheel   = math.max(-1.0, math.min(1.0, e.getWheelRotation / sensitivity))
        val isHoriz = handleHoriz && (mods & InputEvent.SHIFT_MASK) != 0
        val isZoom  = (mods & zoomModifiers) == zoomModifiers

        if (isZoom) {
          if (isHoriz) {
            val factor = linexp(wheel, -1, 1, 2.0, 0.5)
            display.channelDimension(chanDim)
            val numCh = display.numChannels
            var x     = -1
            val mx    = e.getX
            val my    = e.getY
            var ch    = 0
            while (ch < numCh && x < 0) {
              display.channelLocation(ch, chanPoint)
              if (chanPoint.x <= mx && (chanPoint.x + chanDim.width) > mx &&
                  chanPoint.y <= my && (chanPoint.y + chanDim.height) > my) x = mx - chanPoint.x
              ch += 1
            }
            hZoom(zoom, display, factor, math.max(0, x))

          } else {
            val factor = linexp(wheel, -1, 1, 0.5, 2.0)
            vMaxZoom(zoom, display, factor)
          }
        } else if (isHoriz && horizontalScroll) {
          display.channelDimension(chanDim)
          if (chanDim.width == 0) return
          val startOld = zoom.startFrame
          val visiLen  = zoom.stopFrame - startOld
          // val framesPerPixel   = visiLen.toDouble / chanDim.width
          // val delta            = (wheel * framesPerPixel / 2).toLong
          val maxScroll = visiLen.toDouble * 0.5
          val delta     = (wheel * math.abs(wheel) * maxScroll).toLong
          val startNew  = math.max(0L, math.min(display.numFrames - visiLen, startOld + delta))
          //println( "framesPerPixel " + framesPerPixel + "; delta " + delta + "; startOld " + startOld + "; startNew " + startNew )
          if (startNew != startOld) {
            zoom.startFrame = startNew
            zoom.stopFrame  = startNew + visiLen
            display.refreshAllChannels()
          }
        }
      }
    }

    def defaultMouseWheelAction(zoom: HasZoom, display: Display, sensitivity: Double = 12,
                                zoomModifiers: Int = InputEvent.ALT_MASK,
                                horizontalScroll: Boolean = true): MouseWheelListener =
      new MouseWheelImpl(zoom, display, sensitivity, zoomModifiers, horizontalScroll)

    def defaultKeyActions(zoom: HasZoom, display: Display): Iterable[InstallableAction] = {
      val menuModif = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
      // META on Mac, CTRL+SHIFT on PC
      val ctrlNoMenu = if (menuModif == InputEvent.CTRL_MASK)
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK
      else
        menuModif

      // horizontal zoom in
      val idHZoomIn = "synth.swing.HZoomIn"
      val ksHZoomIn1 = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK)
      val ksHZoomIn2 = KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, menuModif)
      val acHZoomIn1 = new ActionSpanWidth(zoom, display, 0.5)
      acHZoomIn1.putValue(Action.ACTION_COMMAND_KEY, idHZoomIn)
      acHZoomIn1.putValue(Action.ACCELERATOR_KEY, ksHZoomIn1)
      val acHZoomIn2 = new ActionSpanWidth(zoom, display, 0.5)
      acHZoomIn2.putValue(Action.ACTION_COMMAND_KEY, idHZoomIn)
      acHZoomIn2.putValue(Action.ACCELERATOR_KEY, ksHZoomIn2)
      // horizontal zoom out
      val idHZoomOut = "synth.swing.HZoomOut"
      val ksHZoomOut1 = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK)
      val ksHZoomOut2 = KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, menuModif)
      val acHZoomOut1 = new ActionSpanWidth(zoom, display, 2.0)
      acHZoomOut1.putValue(Action.ACTION_COMMAND_KEY, idHZoomOut)
      acHZoomOut1.putValue(Action.ACCELERATOR_KEY, ksHZoomOut1)
      val acHZoomOut2 = new ActionSpanWidth(zoom, display, 2.0)
      acHZoomOut2.putValue(Action.ACTION_COMMAND_KEY, idHZoomOut)
      acHZoomOut2.putValue(Action.ACCELERATOR_KEY, ksHZoomOut2)
      // zoom to sample level
      val idHZoomSmp = "synth.swing.HZoomSample"
      val ksHZoomSmp = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ctrlNoMenu)
      val acHZoomSmp = new ActionSpanWidth(zoom, display, 0.0)
      acHZoomSmp.putValue(Action.ACTION_COMMAND_KEY, idHZoomSmp)
      acHZoomSmp.putValue(Action.ACCELERATOR_KEY, ksHZoomSmp)
      // zoom out entirely
      val idHZoomAllOut = "synth.swing.HZoomAllOut"
      val ksHZoomAllOut1 = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK)
      val ksHZoomAllOut2 = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ctrlNoMenu)
      val acHZoomAllOut1 = new ActionSpanWidth(zoom, display, Float.PositiveInfinity)
      acHZoomAllOut1.putValue(Action.ACTION_COMMAND_KEY, idHZoomAllOut)
      acHZoomAllOut1.putValue(Action.ACCELERATOR_KEY, ksHZoomAllOut1)
      val acHZoomAllOut2 = new ActionSpanWidth(zoom, display, Float.PositiveInfinity)
      acHZoomAllOut2.putValue(Action.ACTION_COMMAND_KEY, idHZoomAllOut)
      acHZoomAllOut2.putValue(Action.ACCELERATOR_KEY, ksHZoomAllOut2)

      // vertical amplitude zoom in
      val idCeilZoomIn = "synth.swing.VCeilZoomIn"
      val ksCeilZoomIn = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK)
      val acCeilZoomIn = new ActionVerticalMax(zoom, display, 0.5)
      acCeilZoomIn.putValue(Action.ACTION_COMMAND_KEY, idCeilZoomIn)
      acCeilZoomIn.putValue(Action.ACCELERATOR_KEY, ksCeilZoomIn)
      // vertical amplitude zoom out
      val idCeilZoomOut = "synth.swing.VCeilZoomOut"
      val ksCeilZoomOut = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK)
      val acCeilZoomOut = new ActionVerticalMax(zoom, display, 2.0)
      acCeilZoomOut.putValue(Action.ACTION_COMMAND_KEY, idCeilZoomOut)
      acCeilZoomOut.putValue(Action.ACCELERATOR_KEY, ksCeilZoomOut)

      acHZoomIn1 :: acHZoomIn2 :: acHZoomOut1 :: acHZoomOut2 :: acHZoomSmp :: acHZoomAllOut1 :: acHZoomAllOut2 ::
        acCeilZoomIn :: acCeilZoomOut :: Nil

      //         imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), "retn" )
      //         amap.put( "retn", new ActionScroll( ActionScroll.SCROLL_SESSION_START ))
      //
      //       		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, InputEvent.CTRL_MASK | InputEvent.ALT_MASK ), "incvmin" );
      //       		amap.put( "incvmin", actionIncVertMin );
      //       		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_UP, InputEvent.CTRL_MASK | InputEvent.ALT_MASK ), "decvmin" );
      //       		amap.put( "decvmin", actionDecVertMin );
      //       		actionIncVertMin	= new ActionVerticalMin( 6f );
      //       		actionDecVertMin	= new ActionVerticalMin( -6f );

    }
  }

  trait HasZoom {
    var magLow    : Double
    var magHigh   : Double
    var startFrame: Long
    var stopFrame : Long
  }

  trait MultiResolution extends HasPeakRMS with HasZoom {
    def paint(g: Graphics2D): Unit
  }

  private final class MultiResImpl(source: MultiResolution.Source, display: Display)
    extends MultiResolution {

    override def toString = s"MultiResolution@${hashCode().toHexString}"

    private def setZoomY(): Unit = {
      if (magLow == magHigh) {
        validZoom = false
        return
      }
      val zy        = pnt.scaleY
      zy.sourceLow  = magLow
      zy.sourceHigh = magHigh
      zy.targetLow  = dim.height - 1
      zy.targetHigh = 0
      magDirty      = false
    }

    private val readers     = source.readers.sortBy(_.decimationFactor)
    private val numReaders  = readers.size
    private val dim         = new Dimension()
    private val point       = new Point()

    private var validZoom   = true

    private val pntSH       = WavePainter.sampleAndHold
    private val pntLin      = WavePainter.linear
    private val pntDecim    = WavePainter.peakRMS

    private var pnt: WavePainter = pntLin
    private var reader      = readers.head
    private var readStart   = 0L
    private var readFrames  = 1
    private var decimTuples = 1
    private var decimInline = Decimator.dummy

    recalcDecim()

    def peakColor        : Paint        = pntDecim.peakColor
    def peakColor_=(value: Paint): Unit = pntDecim.peakColor = value

    def rmsColor: Paint = pntDecim.rmsColor
    def rmsColor_=(value: Paint): Unit = {
      pntSH   .color    = value
      pntLin  .color    = value
      pntDecim.rmsColor = value
    }

    private var spanDirty     = true
    private var startFrameVar = 0L
    private var stopFrameVar  = 1L

    def startFrame: Long = startFrameVar
    def startFrame_=(value: Long): Unit = if (startFrameVar != value) {
      startFrameVar = value
      spanDirty     = true
    }

    def stopFrame: Long = stopFrameVar
    def stopFrame_=(value: Long): Unit = if (stopFrameVar != value) {
      stopFrameVar  = value
      spanDirty     = true
    }

    private var magDirty   = true
    private var magLowVar  = -1.0
    private var magHighVar = 1.0

    def magLow: Double = magLowVar
    def magLow_=(value: Double): Unit = if (magLowVar != value) {
      magLowVar = value
      magDirty  = true
    }

    def magHigh: Double = magHighVar
    def magHigh_=(value: Double): Unit = if (magHighVar != value) {
      magHighVar  = value
      magDirty    = true
    }

    private def recalcDecim(): Unit = {
      val numFrames = stopFrame - startFrame // zoomX.sourceHigh - zoomX.sourceLow
      if (numFrames <= 0L) {
        validZoom = false
        return
      }
      val numPixels = dim.width
      if (numPixels <= 0) {
        validZoom = false
        return
      }

      //         val sh        = (numPixels >> 2) > numFrames
      val dispDecim = numFrames.toDouble / numPixels
      var i = 0
      while (i < numReaders && readers(i).decimationFactor < dispDecim) i += 1
      i = math.max(0, i - 1)
      reader = readers(i)
      val fRead = reader.decimationFactor
      //         val oldPnt = pnt
      decimTuples = reader.tupleSize
      val readIsPCM = decimTuples == 1
      if (!readIsPCM && decimTuples != 3) {
        validZoom = false
        return
      }

      val fInline0 = math.max(1, math.ceil(dispDecim / fRead).toInt)
      // tricky: current decimation algorithm assumes write head is slower than read head.
      // but in the case of PCMtoPeakRMS, this holds only for an inline decimation factor
      // of >= 3. thus, if reader is PCM, we use inline decimation only for factors >= 3
      val fInline = if (readIsPCM && fInline0 < 3) 1 else fInline0
      if (fInline > 1) {
        decimInline = if (decimTuples == 1) {
          Decimator.pcmToPeakRMS(fInline)
        } else {
          Decimator.peakRMS(fInline)
        }
        pnt = pntDecim
      } else {
        decimInline = Decimator.dummy
        pnt = if (decimTuples == 1) {
          if (dispDecim <= 0.25) pntSH else pntLin
        } else {
          pntDecim
        }
      }
      val fPaint = fRead * fInline

      validZoom = true

      val decimStart = startFrame / fPaint
      readStart = decimStart * fInline
      //         val decimStop  = (stopFrame + fPaint - 1) / fPaint
      val decimStop = (stopFrame + fRead - 1) / fPaint
      val decimFrames = (decimStop - decimStart).toInt // math.ceil( numFrames / reader.decimationFactor ).toInt
      readFrames = decimFrames * fInline

      //println( "reader " + reader.decimationFactor + "; decimStart = " + decimStart + "; decimStop = " + decimStop + "; inlineFactor = " + decimInline.factor )

      // `decimStart * f` is `<= startFrame`, due to truncation (floor)
      // for full scale, the zoom X source low, which is counted form zero, would thus  be
      // `startFrame - decimStart * f`, and the decimated zoom X source low would thus be
      // `startFrame.toDouble/f - decimStart` (source low is _subtracted_ from the x count)
      val zx        = pnt.scaleX
      zx.sourceLow  = (startFrame.toDouble / fPaint) - decimStart
      zx.sourceHigh = zx.sourceLow + (numFrames.toDouble / fPaint) // correct?
      zx.targetLow  = 0.0
      zx.targetHigh = numPixels
    }

    def paint(g: Graphics2D): Unit = {
      val numCh = source.numChannels

      val oldW = dim.width
      val oldH = dim.height
      display.channelDimension(dim)
      val rectDirty = dim.width != oldW || dim.height != oldH

      if (rectDirty || spanDirty) {
        recalcDecim()
        spanDirty = false
        setZoomY()
      } else if (magDirty) {
        setZoomY()
      }
      if (!validZoom) return

      val clipOrig  = g.getClip
      val atOrig    = g.getTransform
      // val readFrames = decimFrames * decimInline.factor
      val data      = Array.ofDim[Float](numCh, readFrames * decimTuples)
      val success   = reader.read(data, 0, readStart, readFrames)
      if (!success) return // XXX TODO: paint busy rectangle

      val decimFrames = readFrames / decimInline.factor
      var ch = 0
      while (ch < numCh) {
        try {
          val dch = data(ch)
          decimInline.decimate(dch, 0, dch, 0, decimFrames)
          display.channelLocation(ch, point)
          g.clipRect(point.x, point.y, dim.width, dim.height)
          g.translate(point.x, point.y)
          pnt.paint(g, dch, 0, decimFrames)

        } finally {
          g.setTransform(atOrig)
          g.setClip(clipOrig)
        }
        ch += 1
      }
    }
  }
}

trait WavePainter {
  def scaleX: WavePainter.Scaling
  def scaleY: WavePainter.Scaling

  def tupleSize: Int

  def paint(g: Graphics2D, data: Array[Float], dataOffset: Int, dataLength: Int): Unit
}