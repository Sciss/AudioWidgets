package de.sciss.audiowidgets.impl

import java.awt.Color
import java.text.{NumberFormat, ParseException}
import java.util.Locale
import javax.swing.JFormattedTextField.AbstractFormatter
import javax.swing.KeyStroke
import javax.swing.text.{NumberFormatter, MaskFormatter}

import de.sciss.audiowidgets.j.ParamField
import de.sciss.audiowidgets.{Shapes, UnitView, AxisFormat, ParamFormat, TimelineModel}
import de.sciss.desktop.OptionPane
import de.sciss.icons.raphael
import de.sciss.span.Span

import scala.swing.{Component, Label, FlowPanel, Action}
import scala.util.Try

class ActionGoToTime(model: TimelineModel.Modifiable, stroke: KeyStroke)
  extends Action("Set Cursor Position") {

  accelerator = Option(stroke)

  def apply(): Unit = {
    val pos      = model.position

    val selSpan  = model.selection match {
      case sp @ Span(_, _) => sp
      case _               => Span(pos, pos)
    }

    def framesToMillis(n: Long): Double = n / model.sampleRate * 1000

    def millisToFrames(m: Double): Long = (m * model.sampleRate / 1000).toLong

    val bounds = model.bounds

    val fmtTime = new ParamFormat[Long] {
      private val axis = AxisFormat.Time(hours = true, millis = true)

      val unit = UnitView("HH:MM:SS.mmm", raphael.Icon(20, Color.darkGray)(raphael.Shapes.WallClock))

      val formatter = new MaskFormatter("*#:##:##.###") {
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
        val incM = millisToFrames(inc)
        bounds.clip(in + incM)
      }

      private def tryParse(s: String): Long = {
        // HH:MM:SS.mmm
        val arr = s.replace(' ', '0').split(':')
        if (arr.size != 3) throw new ParseException(s, 0)
        try {
          val hours   = arr(0).toLong
          val minutes = arr(1).toLong
          val secs    = arr(2).toDouble
          val millis  = (secs * 1000).toLong
          val allMS   = (hours * 60 + minutes) * 60000 + millis
          bounds.clip(millisToFrames(allMS))
        } catch {
          case _: NumberFormatException => throw new ParseException(s, 0)
        }
      }

      def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

      def format(value: Long): String = axis.format(value / model.sampleRate, pad = 12)
    }

    val fmtFrames = new ParamFormat[Long] {
      val unit = UnitView("sample frames", raphael.Icon(20, Color.darkGray)(Shapes.SampleFrames))

      private val numFmt = NumberFormat.getIntegerInstance(Locale.US)
      numFmt.setGroupingUsed(false)
      val formatter = new NumberFormatter(numFmt)
      formatter.setMinimum(bounds.start)
      formatter.setMaximum(bounds.stop )

      def adjust(in: Long, inc: Int): Long =
        model.bounds.clip(in + inc)

      def parse(s: String): Option[Long] = Try(s.toLong).toOption

      def format(value: Long): String = value.toString
    }

    val fmtMilli = new ParamFormat[Long] {
      val unit = UnitView("milliseconds", "ms")

      private val numFmt = NumberFormat.getIntegerInstance(Locale.US)
      numFmt.setGroupingUsed(false)
      val formatter = new NumberFormatter(numFmt) {
        override def valueToString(value: Any): String = format(value.asInstanceOf[Long])

        override def stringToValue(text: String): AnyRef = tryParse(text).asInstanceOf[AnyRef]
      }
      formatter.setMinimum(framesToMillis(bounds.start))
      formatter.setMaximum(framesToMillis(bounds.stop ))

      def adjust(in: Long, inc: Int): Long = {
        val incM = millisToFrames(inc)
        model.bounds.clip(in + incM)
      }

      private def tryParse(s: String): Long =
        try {
          bounds.clip(millisToFrames(s.toLong))
        } catch {
          case _: NumberFormatException => throw new ParseException(s, 0)
        }

      def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

      def format(value: Long): String = framesToMillis(value).toLong.toString
    }

    val fmt     = List(fmtTime, fmtFrames, fmtMilli)
    val ggTime  = new ParamField(pos, fmt)
    val pane    = new FlowPanel(Component.wrap(ggTime))

    val opt = OptionPane.confirmation(message = pane, messageType = OptionPane.Message.Question,
      optionType = OptionPane.Options.OkCancel, focus = None)
    val res = opt.show(None, title)  // XXX TODO - find window
    println(res)
  }
}
