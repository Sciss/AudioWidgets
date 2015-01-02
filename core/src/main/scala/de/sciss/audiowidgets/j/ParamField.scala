package de.sciss.audiowidgets
package j

import java.awt.{GridBagLayout, GridBagConstraints}
import java.awt.event.{ActionEvent, ActionListener}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.JFormattedTextField.{AbstractFormatter, AbstractFormatterFactory}
import javax.swing.text.{MaskFormatter, DefaultFormatter, NavigationFilter}
import javax.swing.{BorderFactory, JFormattedTextField, JPanel}

import scala.collection.immutable.{Seq => ISeq}

class ParamField[A](value0: A, formats0: ISeq[ParamFormat[A]]) extends JPanel { field =>
  private var _value        = value0
  private var _formats      = formats0
  private var _protoVals: ISeq[A] = value0:: Nil

  private val lbUnit		    = new UnitLabel(formats0.map(_.unit))

  private val formatter     = new MaskFormatter("*") {
    override def valueToString(value: Any): String = selectedFormat.fold("") { pf =>
      pf.format(value.asInstanceOf[A])
    }

    override def stringToValue(text: String): AnyRef = selectedFormat.flatMap { pf =>
      pf.parse(text)
    } .getOrElse(_value).asInstanceOf[AnyRef]


  }

  private val ggNumber	    = new JFormattedTextField(new AbstractFormatterFactory {
    def getFormatter(tf: JFormattedTextField): AbstractFormatter = {
      selectedFormat.map(_.formatter).orNull
      // formatter.setOverwriteMode(selectedFormat.exists(_.useOverwriteMode))
      // formatter
    }
  }, value.asInstanceOf[AnyRef])

  init()

  private def init(): Unit = {
    val lay       = new GridBagLayout
    val con       = new GridBagConstraints

    setLayout(lay)
    con.anchor    = GridBagConstraints.WEST
    con.fill      = GridBagConstraints.HORIZONTAL

    val ggJog			= new Jog()

    ggJog.addListener(new Jog.Listener {
      def jogDragged(e: Jog.Event): Unit = selectedFormat.foreach { pf =>
        val newValue  = pf.adjust(_value, e.value)
        val changed   = newValue != _value
        if (changed) {
          value = newValue
          // ggNumber.setValue(newValue)
        }
        if (changed || !e.isAdjusting) {
          // XXX TODO: fireValueChanged(e.isAdjusting)
        }
      }
    })

    lbUnit.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        unitUpdated()
        // XXX TODO:
        //        fireSpaceChanged
        //        fireValueChanged(false)
      }
    })

    con.gridwidth   = 1
    con.gridheight  = 1
    con.gridx       = 1
    con.gridy       = 1
    con.weightx     = 0.0
    con.weighty     = 0.0
    lay.setConstraints(ggJog, con)
    ggJog.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2))
    add(ggJog)

    con.gridx      += 1
    con.weightx     = 1.0
    lay.setConstraints(ggNumber, con)
    add(ggNumber)

    con.gridx      += 1
    con.weightx     = 0.0
    con.gridwidth = GridBagConstraints.REMAINDER
    lay.setConstraints(lbUnit, con)
    lbUnit.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0))
    add(lbUnit)

    val prop = new PropertyChangeListener {
      def propertyChange(e: PropertyChangeEvent): Unit = {
        val n = e.getPropertyName
        if (n == "value") {
          _value = e.getNewValue.asInstanceOf[A]
        } else if (n == "font") {
          val fnt = field.getFont
          ggNumber.setFont(fnt)
          lbUnit  .setFont(fnt)
        } else if (n == "enabled") {
          val enabled = field.isEnabled
          ggJog   .setEnabled(enabled)
          ggNumber.setEnabled(enabled)
          lbUnit  .setEnabled(enabled)
        }
      }
    }

    addPropertyChangeListener("font"   , prop)
    addPropertyChangeListener("enabled", prop)
    ggNumber.addPropertyChangeListener("value", prop)
  }

  // private def _currentUnit  = lbUnit.selectedEntry

  def value: A = _value

  def value_=(a: A): Unit = if (_value != a) {
    // println(s"value = $a")
    _value = a
    ggNumber.setValue(a)
  }

  def formats: ISeq[ParamFormat[A]] = _formats

  def formats_=(xs: ISeq[ParamFormat[A]]): Unit = if (_formats != xs) {
    _formats = xs
    lbUnit.entries = xs.map(_.unit)
    unitUpdated()
  }

  def prototypeDisplayValues: ISeq[A] = _protoVals

  def prototypeDisplayValues_=(xs: ISeq[A]): Unit = {
    _protoVals = xs
    if (xs.nonEmpty) {
      ggNumber.getMargin
    }
  }

  def selectedFormat: Option[ParamFormat[A]] = {
    val idx = lbUnit.selectedIndex
    if (idx < 0) None else Some(_formats(idx))
  }

  def selectedFormat_=(pf: ParamFormat[A]): Unit = {
    val idx = _formats.indexOf(pf)
    if (idx < 0) throw new IllegalArgumentException(s"Format $pf is not among current formats")
    lbUnit.selectedIndex = idx
  }

  def editable: Boolean = ggNumber.isEditable
  def editable_=(value: Boolean): Unit = ggNumber.setEditable(value)

  private def unitUpdated(): Unit = {
    ggNumber.setValue(ggNumber.getValue)
  }

  override def getBaseline(width: Int, height: Int): Int =
    ggNumber.getBaseline(width, height) + ggNumber.getY
}