/*
 *  AxisFormat.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

object AxisFormat {
  private final val decimMult = Array(1, 10, 100, 1000)

  private def formatNumber(value: Double, decimals: Int, pad: Int): String = {
    val m = (value * decimMult(decimals)).toLong
    if (decimals == 0 && pad == 0) return m.toString

    val neg   = m < 0
    val dec   = decimals > 0
    val s     = (if (neg) -m else m).toString
    val sl    = s.length
    val sl1   = if (dec) sl + 1 else sl
    val sz    = if (neg) sl1 + 1 else sl1
    val sz2   = if (pad > sz) pad else sz
    val sb    = new java.lang.StringBuilder(sz2)
    if (neg) sb.append('-')
    if (pad > sz) {
      var i = pad - sz
      while (i > 0) {
        sb.append(' ')
        i -= 1
      }
    }
    if (pad == 0 || pad >= sz) {
      if (dec) {
        val j = sl - decimals
        sb.append(s, 0, j)
        sb.append('.')
        sb.append(s, j, sl)
      } else {
        sb.append(s)
      }
    } else {
      sb.append('*')
      val i = sl - (pad - sb.length())
      val j = sl - decimals
      // println(s"i = $i, j = $j, sl = $sl, sb = ${sb.length()}")
      if (dec && j >= i) {
        sb.append(s, i, j)
        sb.append('.')
        sb.append(s, j, sl)
      } else {
        sb.append(s, i, sl)
      }
    }
    sb.toString
  }

  case object Decimal extends AxisFormat {
    def format(value: Double, decimals: Int, pad: Int): String = formatNumber(value, decimals, pad)
  }

  case object Integer extends AxisFormat {
    def format(value: Double, decimals: Int, pad: Int): String = formatNumber(value, decimals, pad)
  }

  final case class Time(hours: Boolean = false, millis: Boolean = true) extends AxisFormat {
    def format(value: Double, decimals: Int, pad: Int): String = {
      val dec     = decimals > 0
      val neg     = value < 0
      val m       = (value * 1000).toInt
      val mil0    = if (neg) -m else m
      val mil     = mil0 % 1000
      val secs0   = mil0 / 1000
      val secs    = secs0 % 60
      val mins0   = secs0 / 60
      val mins    = (if (hours) mins0 % 60 else mins0).toString
      val ml      = if (hours && mins.length < 2) 2 else mins.length
      val h       = if (hours) (mins0 / 60).toString else ""
      val hl      = h.length
      val hl1     = if (neg) hl + 1 else hl
      val sz      = (if (hours) hl1 + 1 else 0) + ml + 3 + (if (dec) decimals + 1 else 0)
      // println(s"sz = $sz")
      val sb      = new java.lang.StringBuilder(if (pad > sz) pad else sz)

      if (neg) sb.append('-')
      if (pad > sz) {
        var i = pad - sz
        while (i > 0) {
          sb.append(' ')
          i -= 1
        }
      }
      if (pad == 0 || pad >= sz) {
        if (hours) {
          sb.append(h)
          sb.append(':')
          if (mins.length == 1) sb.append('0')
          sb.append(mins)
        } else {
          sb.append(mins)
        }
      } else {
        sb.append('*')
        val i = sz - pad - sb.length + 2 // sb.length
        if (hours) {
          sb.append(h, i, hl)
          sb.append(':')
          if (mins.length == 1) sb.append('0')
          sb.append(mins)
        } else {
          sb.append(mins, i, ml)
        }
      }

      sb.append(':')
      sb.append(((secs / 10) + 48).toChar)
      sb.append(((secs % 10) + 48).toChar)
      if (dec) {
        sb.append('.')
        sb.append(( (mil / 100) + 48).toChar)
        if (decimals > 1) {
          sb.append((((mil /  10) % 10) + 48).toChar)
          if (decimals > 2) sb.append(( (mil %  10) + 48).toChar)
        }
      }

      sb.toString
    }
  }
}

sealed trait AxisFormat {
  /** Formats a given value as a string with a given number of decimal digits.
    *
    * @param value        the value to format
    * @param decimals  the number of decimals. this must be >= 0 and <= 3, otherwise throws an exception
    */
  def format(value: Double, decimals: Int = 3, pad: Int = 0): String
}
