/*
 *  UnitLabel.scala
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
package j

import java.awt.event.{ActionEvent, ActionListener, MouseAdapter, MouseEvent}
import java.awt.{AWTEventMulticaster, Color, Component, Dimension, FontMetrics, Graphics, Graphics2D, RenderingHints}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.{AbstractAction, Action, ButtonGroup, Icon, JCheckBoxMenuItem, JLabel, JPopupMenu, MenuElement, SwingConstants}

import scala.collection.immutable.{Seq => ISeq}
import scala.math.{Pi, max}

/** This class extends <code>JLabel</code> by adding support
  * for a list of labels which can be easily switched programmatically
  * or by the user to whom a popup menu is presented whenever there
  * is more than one label item. This is useful for adding switchable unit
  * labels to number fields and is used by the <code>ParamField</code>
  * class. You can think of <code>UnitLabel</code> as a <code>JComboBox</code>
  * which uses a text and/or icon label as renderer and not a button.
  */
object UnitLabel {
  private val polyX: Array[Int] = Array(0, 4, 8)
  private val polyY: Array[Int] = Array(0, 4, 0)

  private final class CompoundIcon(iconWest: Icon, iconEast: Icon, gap: Int) extends Icon {
    def getIconWidth: Int =
      (if (iconWest == null) 0 else iconWest.getIconWidth + gap) + (if (iconEast == null) 0 else iconEast.getIconWidth)

    def getIconHeight: Int =
      max(if (iconWest == null) 0 else iconWest.getIconHeight, if (iconEast == null) 0 else iconEast.getIconHeight)

    def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
      if (iconWest != null) {
        iconWest.paintIcon(c, g, x, (iconWest.getIconHeight - getIconHeight) >> 1)
      }
      if (iconEast != null) {
        iconEast.paintIcon(c, g, x + (if (iconWest == null) 0 else iconWest.getIconWidth + gap), y + getIconHeight - iconEast.getIconHeight)
      }
    }
  }
}

class UnitLabel extends JLabel with Icon { label =>
  import UnitLabel._

  private[this] final val pop       = new JPopupMenu
  private[this] final val bg        = new ButtonGroup
  private[this] final var units     = Vector.empty[UnitAction]

  private[this] def setAlpha(in: Color, alpha: Int) = new Color(in.getRGB & 0x00FFFFFF | (alpha << 24), true)

  private[this] val colrTri   = setAlpha(getForeground, 0xB0)
  private[this] val colrTriD  = setAlpha(getForeground, 0x40)
  private[this] val colrLabD  = setAlpha(getForeground, 0x60)

  private[this] var al: ActionListener = _

  private[this] var _selectedIdx  = -1
  private[this] var _cycle        = false

  /* Forwards <code>Font</code> property
   * changes to the child gadgets
   */
  private val prop = new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent): Unit = {
      if (e.getPropertyName == "font") {
        val fnt   = label.getFont
        val items: Array[MenuElement] = pop.getSubElements

        var i = 0
        while (i < items.length) {
          items(i).getComponent.setFont(fnt)
          i += 1
        }
        updatePreferredSize()

      } else if (e.getPropertyName == "enabled") {
        setForeground(if (isEnabled) null else colrLabD)

      } else if (e.getPropertyName == "insets") {
        updatePreferredSize()
      }
    }
  }

  setHorizontalTextPosition(SwingConstants.LEFT)
  setFocusable(true)
  addMouseListener(new MouseAdapter {
    override def mousePressed(e: MouseEvent): Unit =
      if (isEnabled && units.size > 1) {
        requestFocus()
        if (_cycle) {
          val a = units((_selectedIdx + 1) % units.size)
          a.setLabel()
          a.menuItem.setSelected(true)
        } else {
          pop.show(label, 0, label.getHeight)
        }
      }
  })

  addPropertyChangeListener("font"   , prop)
  addPropertyChangeListener("enabled", prop)
  addPropertyChangeListener("insets" , prop)

  def this(entries0: ISeq[UnitView]) = {
    this()
    entries = entries0
  }

  /** Queries the action for a given index. This
    * action may contain a <code>NAME</code> or <code>ICON</code> field.
    *
    * @return	the action at the given index
    */
  def getUnitView(idx: Int): UnitView = units(idx).entry

  /** Queries the action for selected index. This
    * action may contain a <code>NAME</code> or <code>ICON</code> field.
    *
    * @return	the action at the selected index or <code>null</code> if no unit is selected
    */
  def selectedUnitView: Option[UnitView] =
    if ((_selectedIdx < 0) || (_selectedIdx >= units.size)) None else Some(units(_selectedIdx).entry)

  /** Queries the index of the currently selected unit.
    *
    * @return	the index of the active unit or <code>-1</code> if no unit has been selected
    */
  def selectedIndex: Int = _selectedIdx

  /** Changes the currently selected unit.
    * This method does not fire an action event.
    *
    * @param	idx		the new index. Values outside the allowed range (0 ... numUnits-1)
    *                are ignored.
    */
  def selectedIndex_=(idx: Int): Unit = {
    _selectedIdx = idx
    if (idx >= 0 && idx < units.size) {
      val a = units(idx)
      a.setLabel()
      a.menuItem.setSelected(true)
    }
  }

  /** Adds a new unit (text/icon combo label) to the end
    * of the label list. If the unit list had been
    * empty, this new label will be selected.
    */
  def addUnitView(entry: UnitView): Unit = addUnit(entry, update = true)
  
  def removeUnitView(entry: UnitView): Boolean = {
    val idx = units.indexWhere(_.entry == entry)
    if (idx < 0) return false
    removeUnit(idx, update = true)
    true
  }

  def entries: ISeq[UnitView] = units.map(_.entry)

  def entries_=(xs: ISeq[UnitView]): Unit = {
    clear()
    xs.foreach(addUnit(_, update = false))
    updatePreferredSize()
  }

  def cycling: Boolean = _cycle

  def cycling_=(b: Boolean): Unit =
    if (b != _cycle) {
      _cycle = b
      repaint()
    }

  private def clear(): Unit = {
    var idx = units.size
    while (idx > 0) {
      idx -= 1
      removeUnit(idx, update = false)
    }
  }

  private def addUnit(entry: UnitView, update: Boolean): Unit = {
    val a   = new UnitAction(entry)
    val mi  = a.menuItem
    bg .add(mi)
    pop.add(mi)
    val wasEmpty = units.isEmpty
    units :+= a
    if (wasEmpty) {
      selectedIndex = -1
      a.setLabel()
      mi.setSelected(true)
    }
    if (update) updatePreferredSize()
  }

  private def removeUnit(idx: Int, update: Boolean): Unit = {
    val a   = units(idx)
    val mi  = a.menuItem
    bg .remove(mi)
    pop.remove(mi)
    units = units.patch(idx, Nil, 1)
    if (units.isEmpty) {
      _selectedIdx = -1
      label.setText(null)
      label.setIcon(null)
      label.setToolTipText(null)
    }
    if (update) updatePreferredSize()
  }

  private def updatePreferredSize(): Unit = {
    val fnt           = getFont
    val fntMetrics    = getFontMetrics(fnt)
    val d             = new Dimension
    var w: Int = 4
    var h: Int = 4
    val in            = getInsets

    units.foreach { ua =>
      ua.getPreferredSize(fntMetrics, d)
      w = max(w, d.width )
      h = max(h, d.height)
    }
    d.width  = w + in.left + in.right // + label.getIconTextGap
    d.height = h + in.top  + in.bottom
    setMinimumSize  (d)
    setPreferredSize(d)

    // println(s"preferredSize: w = ${d.width}, h = ${d.height}")
  }

  private def fireUnitChanged(): Unit = {
    val l: ActionListener = al
    if (l != null) {
      l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText))
    }
  }

  /** Registers a new listener to be informed
    * about unit switches. Whenever the user
    * switches the unit by selecting an item from
    * the popup menu, an <code>ActionEvent</code> is fired
    * and delivered to all registered listeners.
    *
    * @param	l	the listener to register
    */
  def addActionListener(l: ActionListener): Unit =
    al = AWTEventMulticaster.add(al, l)

  /** Deregisters a new listener from being informed
    * about unit switches.
    *
    * @param	l	the listener to unregister
    */
  def removeActionListener(l: ActionListener): Unit =
    al = AWTEventMulticaster.remove(al, l)
  
  // ---- Icon interface ----

  def getIconWidth : Int = if (units.size > 1) 9 else 0
  def getIconHeight: Int = if (units.size > 1) 5 else 0

  def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
    if (units.size < 2) return

    val g2      = g.asInstanceOf[Graphics2D]
    val atOrig  = g2.getTransform
    g2.translate(x, y)
    if (_cycle) g2.rotate(Pi, 4, 2)
    
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(if (isEnabled) colrTri else colrTriD)
    g2.fillPolygon(polyX, polyY, 3)
    g2.setTransform(atOrig)
  }

  private class UnitAction(val entry: UnitView) 
    extends AbstractAction(entry.label.orNull) { act =>
    
    val menuItem = new JCheckBoxMenuItem(this)
    
    private val icon  = entry.icon.orNull
    private val iconC = new CompoundIcon(icon, label, label.getIconTextGap)
    if (icon != null) putValue(Action.SMALL_ICON, icon)
    putValue(Action.SHORT_DESCRIPTION, entry.name)
    
    def actionPerformed(e: ActionEvent): Unit = setLabel()

    def setLabel(): Unit = {
      label.setText(entry.label.orNull)
      label.setIcon(iconC)
      label.setToolTipText(entry.name)
      val newIndex = label.units.indexOf(act)
      if (newIndex != label._selectedIdx) {
        _selectedIdx = newIndex
        fireUnitChanged()
      }
    }

    def getPreferredSize(fntMetrics: FontMetrics, d0: Dimension): Dimension = {
      val d = if (d0 == null) new Dimension else d0
      d.width   = iconC.getIconWidth + label.getIconTextGap
      d.height  = iconC.getIconHeight
      entry.label.foreach { name =>
        d.width  += fntMetrics.stringWidth(name) + 4 // + label.getIconTextGap
        d.height  = math.max(d.height, fntMetrics.getHeight)
      }
      d
    }
  }
}