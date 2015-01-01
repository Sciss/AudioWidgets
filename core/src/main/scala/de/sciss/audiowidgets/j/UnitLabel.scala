package de.sciss.audiowidgets
package j

import java.awt.{AWTEventMulticaster, Color, Component, Dimension, FontMetrics, Graphics, Graphics2D, RenderingHints}
import java.awt.event.{ActionEvent, ActionListener, MouseAdapter, MouseEvent}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.{SwingConstants, AbstractAction, Action, ButtonGroup, Icon, JCheckBoxMenuItem, JLabel, JPopupMenu, MenuElement}

import scala.math.{max, Pi}

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
  private val colrTri : Color = new Color(0x00, 0x00, 0x00, 0xB0)
  private val colrTriD: Color = new Color(0x00, 0x00, 0x00, 0x55)

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

  private final val colrLab   = null
  private final val colrLabD  = new Color(0x00, 0x00, 0x00, 0x7F)
}

class UnitLabel extends JLabel with Icon { label =>
  import UnitLabel._

  private final val pop       = new JPopupMenu
  private final val bg        = new ButtonGroup
  private final var units     = Vector.empty[UnitAction]

  private var al: ActionListener = null

  private var _selectedIdx  = -1
  private var _cycle        = false

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
        setForeground(if (isEnabled) colrLab else colrLabD)

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
          units((_selectedIdx + 1) % units.size).setLabel()
          pop.getComponent(_selectedIdx).asInstanceOf[JCheckBoxMenuItem].setSelected(true)
        } else {
          pop.show(label, 0, label.getHeight)
        }
      }
  })

  addPropertyChangeListener("font"   , prop)
  addPropertyChangeListener("enabled", prop)
  addPropertyChangeListener("insets" , prop)

  /** Queries the action for a given index. This
    * action may contain a <code>NAME</code> or <code>ICON</code> field.
    *
    * @return	the action at the given index
    */
  def getUnit(idx: Int): Action = units(idx)

  /** Queries the action for selected index. This
    * action may contain a <code>NAME</code> or <code>ICON</code> field.
    *
    * @return	the action at the selected index or <code>null</code> if no unit is selected
    */
  def selectedUnit: Option[Action] =
    if ((_selectedIdx < 0) || (_selectedIdx >= units.size)) None else Some(units(_selectedIdx))

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
      units(idx).setLabel()
      pop.getComponent(idx).asInstanceOf[JCheckBoxMenuItem].setSelected(true)
    }
  }

  /** Adds a new unit (text label) to the end
    * of the label list. If the unit list had been
    * empty, this new label will be selected.
    *
    * @param	name	the name of the new label.
    */
  def addUnit(name: String): Unit = addUnit(new UnitAction(name))

  /** Adds a new unit (icon label) to the end
    * of the label list. If the unit list had been
    * empty, this new label will be selected.
    *
    * @param	icon	the icon view of the new label.
    */
  def addUnit(icon: Icon): Unit = addUnit(new UnitAction(icon))

  /** Adds a new unit (text/icon combo label) to the end
    * of the label list. If the unit list had been
    * empty, this new label will be selected.
    *
    * @param	name	the name of the new label.
    * @param	icon	the icon view of the new label.
    */
  def addUnit(name: String, icon: Icon): Unit = addUnit(new UnitAction(name, icon))

  def cycling: Boolean = _cycle

  def cycling_=(b: Boolean): Unit =
    if (b != _cycle) {
      _cycle = b
      repaint()
    }
  
  private def addUnit(a: UnitAction): Unit = {
    val mi: JCheckBoxMenuItem = new JCheckBoxMenuItem(a)
    bg.add(mi)
    pop.add(mi)
    units :+= a
    if (units.size == 1) {
      a.setLabel()
      mi.setSelected(true)
    }
    updatePreferredSize()
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
      w = max(w, d.width)
      h = max(h, d.height)
    }
    d.width  = w + in.left + in.right
    d.height = h + in.top + in.bottom
    setMinimumSize  (d)
    setPreferredSize(d)
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

  private class UnitAction(name: String, icon0: Icon) extends AbstractAction(name) { act =>
    
    private val icon = new CompoundIcon(icon0, label, label.getIconTextGap)
    putValue(Action.SMALL_ICON, icon)
    
    def this(name: String) {
      this(name, null)
    }

    def this(icon: Icon) {
      this(null, icon)
    }

    def actionPerformed(e: ActionEvent): Unit = setLabel()

    def setLabel(): Unit = {
      label.setText(name)
      label.setIcon(icon)
      val newIndex = label.units.indexOf(act)
      if (newIndex != label._selectedIdx) {
        _selectedIdx = newIndex
        fireUnitChanged()
      }
    }

    def getPreferredSize(fntMetrics: FontMetrics, d0: Dimension): Dimension = {
      var w = 0
      var h = 0
      if (name != null) {
        w = fntMetrics.stringWidth(name) + label.getIconTextGap
        h = fntMetrics.getHeight
      }
      val d = if (d0 == null) new Dimension else d0
      d.width  = w + icon.getIconWidth
      d.height = max(h, icon.getIconHeight)
      d
    }
  }
}