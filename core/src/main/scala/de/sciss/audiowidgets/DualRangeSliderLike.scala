/*
 *  DualRangeSliderLike.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

trait DualRangeSliderLike {
  var model: DualRangeModel

  def minimum        : Int                = model.minimum
  def minimum_=(value: Int): Unit         = model.minimum = value

  def maximum        : Int                = model.maximum
  def maximum_=(value: Int): Unit         = model.maximum = value

  def value          : Int                = model.value
  def value_=  (value: Int): Unit         = model.value = value

  def range          : (Int, Int)         = model.range
  def range_=  (value: (Int, Int)): Unit  = model.range = value

  def adjusting      : Boolean            = model.adjusting

  def extent         : Int                = model.extent
  def extent_= (value: Int): Unit         = model.extent = value

  var valueEditable : Boolean
  var rangeEditable : Boolean
  var valueVisible  : Boolean
  var rangeVisible  : Boolean
  var extentFixed   : Boolean
}