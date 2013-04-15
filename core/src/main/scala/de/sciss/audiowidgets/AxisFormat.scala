package de.sciss.audiowidgets

object AxisFormat {
  case object Decimal extends AxisFormat
  case object Integer extends AxisFormat

  final case class Time(hours: Boolean = false, millis: Boolean = true) extends AxisFormat {

  }
}

sealed trait AxisFormat
