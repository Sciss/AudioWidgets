/*
 *  TimeField.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2016 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import java.text.{NumberFormat, ParseException}
import java.util.Locale
import javax.swing.JFormattedTextField.AbstractFormatter
import javax.swing.text.{MaskFormatter, NumberFormatter}

import de.sciss.icons.raphael
import de.sciss.span.{Span, SpanLike}

import scala.util.Try

object TimeField {
  private final class TimeFormat(var span: SpanLike, clip: Boolean, sampleRate: Double) extends ParamFormat[Long] {
    outer =>

    override def toString = s"TimeField.TimeFormat($span, sampleRate = $sampleRate)@${hashCode.toHexString}"

    private[this] val axis = AxisFormat.Time(hours = true, millis = true)

    private def millisToFrames(m: Double): Long = (m * sampleRate / 1000 + 0.5).toLong

    val unit = UnitView("HH:MM:SS.mmm", raphael.Icon(20, raphael.DimPaint)(raphael.Shapes.WallClock))

    val formatter: AbstractFormatter = new MaskFormatter("*#:##:##.###") {
      override def toString = s"$outer.formatter"

      // setAllowsInvalid(true)
      setPlaceholderCharacter('_')
      // the next line is crucial because retarded MaskFormatter calls into super.stringToValue
      // from other methods, and DefaultFormatter finds a Long with a static constructor method
      // that takes a String, and blindly tries to feed that crap into it. Setting an explicit
      // value-class that does _not_ have a string constructor will bypass that bull crap.
      setValueClass(classOf[AnyRef])

      override def stringToValue(value: String): AnyRef = {
        val v0 = super.stringToValue(value)
        val s0 = v0.toString
        val res = tryParse(s0)
        // println(s"res = $res")
        res.asInstanceOf[AnyRef]
      }

      override def valueToString(value: Any): String = {
        val s0 = format(value.asInstanceOf[Long])
        super.valueToString(s0)
      }
    }

    def adjust(in: Long, inc: Int): Long = {
      val incM  = millisToFrames(inc)
      val out   = in + incM
      if (clip) span.clip(out) else out
    }

    private def tryParse(s: String): Long = {
      // HH:MM:SS.mmm
      val arr = s.replace(' ', '0').split(':')
      if (arr.length != 3) throw new ParseException(s, 0)
      try {
        val hours   = arr(0).toLong
        val minutes = arr(1).toLong
        val secs    = arr(2).toDouble
        val millis  = (secs * 1000).toLong
        val allMS   = (hours * 60 + minutes) * 60000 + millis
        span.clip(millisToFrames(allMS))
      } catch {
        case _: NumberFormatException => throw new ParseException(s, 0)
      }
    }

    def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

    def format(value: Long): String = axis.format(value / sampleRate, pad = 12)
  }

  private final class FramesFormat(var span: SpanLike, clip: Boolean, sampleRate: Double, var viewSampleRate: Double)
    extends ParamFormat[Long] {
    outer =>

    override def toString =
      s"TimeField.FramesFormat($span, sampleRate = $sampleRate, viewSampleRate = $viewSampleRate)@${hashCode.toHexString}"

    val unit = UnitView("sample frames", raphael.Icon(20, raphael.DimPaint)(Shapes.SampleFrames))

    def modelToView(in: Long): Long = (in * viewSampleRate / sampleRate + 0.5).toLong
    def viewToModel(in: Long): Long = (in * sampleRate / viewSampleRate + 0.5).toLong

    private[this] val numFmt = NumberFormat.getIntegerInstance(Locale.US)
    numFmt.setGroupingUsed(false)

    val formatter: AbstractFormatter = new NumberFormatter(numFmt) {
      override def toString = s"$outer.formatter"

      override def valueToString(value: Any   ): String = format  (value.asInstanceOf[Long])
      override def stringToValue(text : String): AnyRef = tryParse(text).asInstanceOf[AnyRef]

      if (clip) {
        span match { case hs: Span.HasStart => setMinimum(modelToView(hs.start)); case _ => }
        span match { case hs: Span.HasStop  => setMaximum(modelToView(hs.stop )); case _ => }
      }
    }

    def adjust(in: Long, inc: Int): Long = {
      val incM  = viewToModel(inc)
      val out   = in + incM
      if (clip) span.clip(out) else out
    }

    private def tryParse(s: String): Long =
      try {
        val in  = s.toLong
        val out = viewToModel(in)
        if (clip) span.clip(out) else out
      } catch {
        case _: NumberFormatException => throw new ParseException(s, 0)
      }

    def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

    def format(value: Long): String = {
      val v = modelToView(value)
      v.toString
    }
  }

  private final class MilliFormat(var span: SpanLike, clip: Boolean, sampleRate: Double) extends ParamFormat[Long] {
    outer =>

    override def toString = s"TimeField.MilliFormat($span, sampleRate = $sampleRate)@${hashCode.toHexString}"

    private def framesToMillis (n: Long): Double =  n / sampleRate * 1000
    private def millisToFrames (m: Double): Long = (m * sampleRate / 1000 + 0.5).toLong

    val unit = UnitView("milliseconds", "ms")

    private[this] val numFmt = NumberFormat.getIntegerInstance(Locale.US)
    numFmt.setGroupingUsed(false)

    val formatter: AbstractFormatter = new NumberFormatter(numFmt) {
      override def toString = s"$outer.formatter"

      override def valueToString(value: Any   ): String = format  (value.asInstanceOf[Long])
      override def stringToValue(text : String): AnyRef = tryParse(text).asInstanceOf[AnyRef]

      if (clip) {
        span match { case hs: Span.HasStart => setMinimum(framesToMillis(hs.start)); case _ => }
        span match { case hs: Span.HasStop  => setMaximum(framesToMillis(hs.stop )); case _ => }
      }
    }

    def adjust(in: Long, inc: Int): Long = {
      val incM  = millisToFrames(inc)
      val out   = in + incM
      if (clip) span.clip(out) else out
    }

    private def tryParse(s: String): Long =
      try {
        val out = millisToFrames(s.toLong)
        if (clip) span.clip(out) else out
      } catch {
        case _: NumberFormatException => throw new ParseException(s, 0)
      }

    def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

    def format(value: Long): String = (framesToMillis(value) + 0.5).toLong.toString
  }

  private final class PercentFormat(var span: Span) extends ParamFormat[Long] {
    private def framesToPercent(n: Long  ): Double = (n - span.start).toDouble / span.length
    private def percentToFrames(p: Double): Long   = (p * span.length + span.start + 0.5).toLong

    val unit = UnitView("percent", "%")

    private[this] val numFmt = NumberFormat.getIntegerInstance(Locale.US)
    numFmt.setGroupingUsed(false)
    val formatter = new NumberFormatter(numFmt) {
      override def valueToString(value: Any   ): String = format  (value.asInstanceOf[Long])
      override def stringToValue(text : String): AnyRef = tryParse(text).asInstanceOf[AnyRef]
    }
    formatter.setMinimum(  0.0)
    formatter.setMaximum(100.0)

    private def tryParse(s: String): Long =
      try {
        val out = percentToFrames(s.toDouble * 0.01)
        span.clip(out)
      } catch {
        case _: NumberFormatException => throw new ParseException(s, 0)
      }

    def adjust(in: Long, inc: Int): Long = {
      val out = percentToFrames(framesToPercent(in) + inc * 0.0001)
      span.clip(out)
    }

    def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

    def format(value: Long): String = f"${framesToPercent(value) * 100}%1.3f"
  }
}

/** Extension of `ParamField` for displaying time positions.
  *
  * @param span0              the bounds, if they exist. when bounds are given
  *                           and `clip` is `true`, the time position is clipped
  *                           to the span of `bounds`
  * @param sampleRate         the underlying model's sample rate
  * @param viewSampleRate0    the sample rate used for displaying frame positions.
  *                           if zero, then the model's sample-rate is used
  * @param clip               `true` to clip values to the span of `bounds`, `false`
  *                           to allow values that lie outside this span
  */
class TimeField(value0: Long, private[this] var span0: SpanLike, val sampleRate: Double,
                private[this] var viewSampleRate0: Double = 0.0, clip: Boolean = true)
  extends ParamField[Long](value0, Nil) {

  if (viewSampleRate0 == 0) viewSampleRate0 = sampleRate

  private[this] val fmtTime     = new TimeField.TimeFormat  (span0, clip = clip, sampleRate = sampleRate)
  private[this] val fmtFrames   = new TimeField.FramesFormat(span0, clip = clip, sampleRate = sampleRate,
    viewSampleRate = viewSampleRate0)

  private[this] val fmtMilli    = new TimeField.MilliFormat (span0, clip = clip, sampleRate = sampleRate)
  private[this] var fmtPercent  = span0 match {
    case sp: Span => Some(new TimeField.PercentFormat(sp))
    case _ => None
  }

  private def sqFormats() = fmtTime :: fmtFrames :: fmtMilli :: fmtPercent.toList

  private def updateFormats(): Unit = {
    fmtTime   .span = span0
    fmtFrames .span = span0
    fmtMilli  .span = span0
    fmtFrames.viewSampleRate = viewSampleRate0

    val hadP = fmtPercent.isDefined

    (span0, fmtPercent) match {
      case (sp: Span, Some(fmt))  => fmt.span = sp
      case (sp: Span, None)       => fmtPercent = Some(new TimeField.PercentFormat(sp))
      case (_, Some(_))           => fmtPercent = None
      case _ =>
    }

    val hasP = fmtPercent.isDefined

    if (hadP == hasP) {
      selectedFormat.foreach { pf =>
        pf.parse(textField.text).foreach { v =>
          value = v
        }
      }

    } else {
      formats = sqFormats()
    }
  }

  def span: SpanLike = span0
  def span_=(value: SpanLike): Unit = if (span0 != value) {
    span0 = value
    updateFormats()
  }

  def viewSampleRate: Double = viewSampleRate0
  def viewSampleRate_=(value: Double): Unit = {
    val v = if (value == 0) sampleRate else value
    if (viewSampleRate0 != v) {
      viewSampleRate0 = v
      updateFormats()
    }
  }

  formats = sqFormats()

  prototypeDisplayValues = {
    var res = List.empty[Long]
    bounds match { case hs: Span.HasStop  => res = hs.stop  :: res; case _ => }
    bounds match { case hs: Span.HasStart => res = hs.start :: res; case _ => }
    res
  }

  {
    val pref  = textField.preferredSize
    val min   = textField.minimumSize
    val max   = textField.maximumSize
    textField.preferredSize = pref
    min.width = pref.width
    max.width = pref.width
    textField.minimumSize = min
    textField.maximumSize = max
  }
}