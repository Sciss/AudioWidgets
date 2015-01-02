package de.sciss.audiowidgets

import javax.swing.JFormattedTextField

//object ParamFormat {
//
//}
trait ParamFormat[A] {
  def unit: UnitView

  def format(value: A): String
  def parse(s: String): Option[A]

  def formatter: JFormattedTextField.AbstractFormatter

  def adjust(in: A, inc: Int): A
}