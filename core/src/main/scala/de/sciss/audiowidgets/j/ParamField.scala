/*
 *  ParamField.scala
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
package j

import java.awt.event.{ActionEvent, ActionListener, KeyEvent}
import java.awt.{Font, GridBagConstraints, GridBagLayout, Toolkit}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.JFormattedTextField.{AbstractFormatter, AbstractFormatterFactory}
import javax.swing.{AbstractAction, BorderFactory, JFormattedTextField, JPanel, KeyStroke, SwingConstants}

import scala.collection.immutable.{Seq => ISeq}

class ParamField[A](value0: A, formats0: ISeq[ParamFormat[A]])
  extends JPanel with ParamFieldLike[A] { field =>

  private[this] var _value        = value0
  private[this] var _formats      = formats0
  private[this] var _protoVals: ISeq[A] = value0:: Nil

  private[this] val lbUnit		    = new UnitLabel(formats0.map(_.unit))

  private[this] val ggNumber: JFormattedTextField = new JFormattedTextField(new AbstractFormatterFactory {
    override def toString = s"ParamField(${_value})@${field.hashCode.toHexString}.FormatterFactory"

    def getFormatter(tf: JFormattedTextField): AbstractFormatter = {
      selectedFormat.map(_.formatter).orNull
      // formatter.setOverwriteMode(selectedFormat.exists(_.useOverwriteMode))
      // formatter
    }
  }, value.asInstanceOf[AnyRef]) {
    private var columnWidth = 0

    override def setFont(f: Font): Unit = {
      super.setFont(f)
      columnWidth = 0
    }

    override def getColumnWidth: Int = {
      if (columnWidth == 0) {
        val metrics = getFontMetrics(getFont)
        columnWidth = metrics.charWidth('0')  // use that instead of 'm' character
      }
      columnWidth
    }
  }

  init()

  private def init(): Unit = {
    val lay       = new GridBagLayout
    val con       = new GridBagConstraints

    unitVisibility()

    setLayout(lay)
    con.anchor    = GridBagConstraints.WEST
    con.fill      = GridBagConstraints.HORIZONTAL

    val ggJog			= new Jog()

    ggJog.addListener(new Jog.Listener {
      def jogDragged(e: Jog.Event): Unit = incValue(e.value, adjusting = e.isAdjusting)
    })

    lbUnit.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = unitUpdated()
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

    ggNumber.setHorizontalAlignment(SwingConstants.RIGHT)

    val aMap = ggNumber.getActionMap
    val iMap = ggNumber.getInputMap
    val meta = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
    aMap.put("param-prev-unit", new ActionCycleUnit(-1))
    iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP  , meta), "param-prev-unit")
    aMap.put("param-next-unit", new ActionCycleUnit(+1))
    iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, meta), "param-next-unit")
    aMap.put("param-inc-value", new ActionIncValue(+1))
    iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP  , 0), "param-inc-value")
    aMap.put("param-dec-value", new ActionIncValue(-1))
    iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "param-dec-value")

    updatePreferredSize()
  }

  // private def _currentUnit  = lbUnit.selectedEntry

  private def incValue(amount: Int, adjusting: Boolean): Unit = selectedFormat.foreach { pf =>
    val newValue  = pf.adjust(_value, amount)
    val changed   = newValue != _value
    if (changed) {
      value = newValue
      // ggNumber.setValue(newValue)
    }
    if (changed || !adjusting) {
      // XXX TODO: fireValueChanged(e.isAdjusting)
    }
  }

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
    unitVisibility()
    unitUpdated()
    updatePreferredSize()
  }

  private def unitVisibility(): Unit = {
    val xs      = _formats
    val hidden  = xs.isEmpty || (xs.size == 1 && {
      val u = xs.head.unit
      u.label.isEmpty && u.icon.isEmpty
    })
    lbUnit.setVisible(!hidden)
  }

  def prototypeDisplayValues: ISeq[A] = _protoVals

  def prototypeDisplayValues_=(xs: ISeq[A]): Unit = {
    _protoVals = xs
    if (xs.nonEmpty) updatePreferredSize()
  }

  private def updatePreferredSize(): Unit = {
    val col = _protoVals.foldLeft(0) { (res0, value) =>
      _formats.foldLeft(res0) { (res1, pf) =>
        math.max(res1, pf.format(value).length)
      }
    }
    ggNumber.setColumns(col)
  }

  def selectedFormat: Option[ParamFormat[A]] = {
    val idx = lbUnit.selectedIndex
    if (idx < 0) None else Some(_formats(idx))
  }

  def selectedFormat_=(opt: Option[ParamFormat[A]]): Unit = {
    val idx = opt.fold(-1) { pf =>
      val res = _formats.indexOf(pf)
      if (res < 0) throw new IllegalArgumentException(s"Format $pf is not among current formats")
      res
    }
    val oldIdx = lbUnit.selectedIndex
    if (idx != oldIdx) {
//      val oldValue = if (oldIdx < 0 || oldIdx >= _formats.size) None else Some(_formats(oldIdx))
      lbUnit.selectedIndex = idx
      unitUpdated()
    }
  }

  def editable: Boolean = ggNumber.isEditable
  def editable_=(value: Boolean): Unit = ggNumber.setEditable(value)

  private def unitUpdated(): Unit = {
    ggNumber.setValue(ggNumber.getValue)
    firePropertyChange("selectedFormat", null, selectedFormat)
  }

  override def getBaseline(width: Int, height: Int): Int =
    ggNumber.getBaseline(width, height) + ggNumber.getY

  def textField: JFormattedTextField = ggNumber

  ////

  private final class ActionIncValue(amount: Int) extends AbstractAction {
    def actionPerformed(e: ActionEvent): Unit = incValue(amount, adjusting = false)
  }

  private final class ActionCycleUnit(inc: Int) extends AbstractAction {
    def actionPerformed(e: ActionEvent): Unit = {
      val idxOld = lbUnit.selectedIndex
      if (idxOld < 0) return
      val idxNew = math.max(0, math.min(_formats.size - 1, idxOld + inc))
      if (idxNew != idxOld) {
        lbUnit.selectedIndex = idxNew
        unitUpdated()
      }
    }
  }
}