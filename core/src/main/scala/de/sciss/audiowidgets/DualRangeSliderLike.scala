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

  def valueIsAdjusting: Boolean           = model.valueIsAdjusting

  def extent         : Int                = model.extent
  def extent_= (value: Int): Unit         = model.extent = value

  var valueEditable : Boolean
  var rangeEditable : Boolean
  var valueVisible  : Boolean
  var rangeVisible  : Boolean
  var extentFixed   : Boolean
}