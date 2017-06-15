/*
 *  Jog.scala
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

import java.awt.event.MouseEvent
import java.awt.geom.{Ellipse2D, Point2D}
import java.awt.{BasicStroke, Color, Cursor, Dimension, GradientPaint, Graphics, Graphics2D, Insets, Paint, RenderingHints, Shape, Stroke, Window}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.{EventListener, EventObject}
import javax.swing.event.MouseInputAdapter
import javax.swing.{JComponent, SwingUtilities}

import scala.math.{Pi, atan2, cos, max, min, sin, sqrt}

object Jog {

  private final class ColorScheme(
    val pntBack: Paint,
    val pntOutline: Paint,
    val pntLight: Paint,
    val pntArcLight: Paint,
    val pntArcShadow: Paint,
    val pntBelly: Paint
   )

  private val lightScheme : ColorScheme = new Jog.ColorScheme(new GradientPaint(10, 9, new Color(235, 235, 235), 10, 19, new Color(248, 248, 248)), new Color(40, 40, 40), new Color(251, 251, 251), new Color(255, 255, 255), new GradientPaint(12, 0, new Color(40, 40, 40, 0xA0), 8, 15, new Color(40, 40, 40, 0x00)), new GradientPaint(0, -3, new Color(0x58, 0x58, 0x58), 0, 3, new Color(0xD0, 0xD0, 0xD0)))
  private val lightSchemeD: ColorScheme = new Jog.ColorScheme(new GradientPaint(10, 9, new Color(235, 235, 235, 0x7F), 10, 19, new Color(248, 248, 248, 0x7F)), new Color(40, 40, 40, 0x7F), new Color(251, 251, 251, 0x7F), new Color(255, 255, 255, 0x7F), new GradientPaint(12, 0, new Color(40, 40, 40, 0x50), 8, 15, new Color(40, 40, 40, 0x00)), new GradientPaint(0, -3, new Color(0x58, 0x58, 0x58, 0x7F), 0, 3, new Color(0xD0, 0xD0, 0xD0, 0x7F)))
  private val darkScheme  : ColorScheme = new Jog.ColorScheme(new GradientPaint(10, 9, new Color(24, 24, 24), 10, 19, new Color(32, 32, 32)), new Color(0, 0, 0), new GradientPaint(0, 1, new Color(72, 72, 72, 0x80), 0, 10, new Color(48, 48, 48, 0x40)), new Color(64, 64, 64), new GradientPaint(12, 0, new Color(16, 16, 16, 0xA0), 8, 15, new Color(16, 16, 16, 0x00)), new GradientPaint(0, -3, new Color(0x48, 0x48, 0x48), 0, 3, new Color(0xA0, 0xA0, 0xA0)))
  private val darkSchemeD : ColorScheme = new Jog.ColorScheme(new GradientPaint(10, 9, new Color(24, 24, 24, 0x7F), 10, 19, new Color(32, 32, 32, 0x7F)), new Color(0, 0, 0, 0x7F), new GradientPaint(0, 1, new Color(72, 72, 72, 0x40), 0, 10, new Color(48, 48, 48, 0x20)), new Color(64, 64, 64), new GradientPaint(12, 0, new Color(16, 16, 16, 0x50), 8, 15, new Color(16, 16, 16, 0x00)), new GradientPaint(0, -3, new Color(0x48, 0x48, 0x48, 0x7F), 0, 3, new Color(0xA0, 0xA0, 0xA0, 0x7F)))

//  private val pntBack       : Paint   = new GradientPaint(10, 9, new Color(235, 235, 235), 10, 19, new Color(248, 248, 248))
//  private val colrOutline   : Color   = new Color(40, 40, 40)
//  private val colrLight     : Color   = new Color(251, 251, 251)
//  private val colrArcLight  : Color   = new Color(255, 255, 255)
//  private val pntArcShadow  : Paint   = new GradientPaint(12, 0, new Color(40, 40, 40, 0xA0), 8, 15, new Color(40, 40, 40, 0x00))
//  private val pntBelly      : Paint   = new GradientPaint(0, -3, new Color(0x58, 0x58, 0x58), 0, 3, new Color(0xD0, 0xD0, 0xD0))
//  private val pntBackD      : Paint   = new GradientPaint(10, 9, new Color(235, 235, 235, 0x7F), 10, 19, new Color(248, 248, 248, 0x7F))
//  private val colrOutlineD  : Color   = new Color(40, 40, 40, 0x7F)
//  private val colrLightD    : Color   = new Color(251, 251, 251, 0x7F)
//  private val colrArcLightD : Color   = new Color(255, 255, 255, 0x7F)
//  private val pntArcShadowD : Paint   = new GradientPaint(12, 0, new Color(40, 40, 40, 0x50), 8, 15, new Color(40, 40, 40, 0x00))
//  private val pntBellyD     : Paint   = new GradientPaint(0, -3, new Color(0x58, 0x58, 0x58, 0x7F), 0, 3, new Color(0xD0, 0xD0, 0xD0, 0x7F))

  private val strkOutline   : Stroke  = new BasicStroke(0.5f)
  private val strkArcShadow : Stroke  = new BasicStroke(1.2f)
  private val strkArcLight  : Stroke  = new BasicStroke(1.0f)
  private val shpBelly      : Shape   = new Ellipse2D.Double(-2.5, -2.5, 5.0, 5.0)
  private val dragCursor    : Cursor  = new Cursor(Cursor.MOVE_CURSOR)

  private final val Pi2 = math.Pi * 2

  trait Listener extends EventListener {
    def jogDragged(event: Event): Unit
  }

  class Event(source: AnyRef, val value: Int, val isAdjusting: Boolean) extends EventObject(source)
}

class Jog extends JComponent { me =>
  import Jog._
  
  private[this] final val bellyPos  = new Point2D.Double(-0.7071064, -0.7071064)

  private[this] var savedCursor : Cursor = _
  private[this] var in          : Insets = _

  private[this] var dragX         = 0
  private[this] var dragY         = 0
  private[this] var dragArc       = 0.0
  private[this] var displayArc    = -2.356194
  private[this] var propagateFire = false

  private[this] final val scheme  = if (Util.isDarkSkin) Jog.darkScheme  else Jog.lightScheme
  private[this] final val schemeD = if (Util.isDarkSkin) Jog.darkSchemeD else Jog.lightSchemeD

  init()
  
  private def init(): Unit = {
    updatePreferredSize()
    setFocusable(true)
    val mia: MouseInputAdapter = new MouseInputAdapter {

      override def mousePressed(e: MouseEvent): Unit = {
        propagateFire = false
        if (!isEnabled) return
        requestFocus()
        val w: Window = SwingUtilities.getWindowAncestor(me)
        if (w != null) {
          savedCursor = w.getCursor
          w.setCursor(dragCursor)
        }
        processMouse(e, isDrag = false)
      }

      override def mouseReleased(e: MouseEvent): Unit = {
        if (!isEnabled) return
        val w: Window = SwingUtilities.getWindowAncestor(me)
        if (w != null) {
          w.setCursor(savedCursor)
        }
        if (propagateFire) {
          dispatchChange(0, adjusting = false)
          propagateFire = false
        }
      }

      override def mouseDragged(e: MouseEvent): Unit = {
        if (!isEnabled) return
        processMouse(e, isDrag = true)
      }

      private def processMouse(e: MouseEvent, isDrag: Boolean): Unit = {
        val w0              = getWidth
        val w               = w0 - in.left - in.right
        val h               = w0 - in.top - in.bottom
        val dx0             = e.getX - in.left - w * 0.5
        val dy0             = e.getY - in.top  - h * 0.5
        if (isDrag) {
          val thisArc = atan2(dx0, dy0) + Pi
          val dx1 = dx0 / w
          val dy1 = dy0 / h
          val weight = max(0.125, sqrt(dx1 * dx1 + dy1 * dy1) / 2)
          val deltaArc0 = thisArc - dragArc
          val deltaArc = if (deltaArc0 < -Pi) {
            Pi2 - deltaArc0
          }
          else if (deltaArc0 > Pi) {
            -Pi2 + deltaArc0
          } else {
            deltaArc0
          }
          val dx = e.getX - dragX
          val dy = e.getY - dragY
          val dragAmount0 = (sqrt(dx * dx + dy * dy) * 0.5).toInt
          val newDisplayArc = (displayArc + (if (deltaArc < 0) -1 else 1) * min(0.4, weight * dragAmount0)) % Pi2
          if (dragAmount0 >= 1) {
            val dragAmount1 = if (dragAmount0 >= 17) {
              dragAmount0 * (dragAmount0 - 16)
            } else {
              dragAmount0
            }
            displayArc = newDisplayArc
            dragArc = thisArc
            dragX = e.getX
            dragY = e.getY
            repaint()
            val dragAmount2 = dragAmount1 * (if (deltaArc < 0) 1 else -1)
            dispatchChange(dragAmount2, adjusting = true)
            propagateFire = true
          }
          
        } else {
          dragX = e.getX
          dragY = e.getY
          dragArc = atan2(dx0, dy0) + Pi
        }
        bellyPos.setLocation(cos(displayArc), sin(displayArc))
        repaint()
      }
    }

    addMouseListener(mia)
    addMouseMotionListener(mia)
    addPropertyChangeListener("border", new PropertyChangeListener {
      def propertyChange(evt: PropertyChangeEvent): Unit = updatePreferredSize()
    })
  }

  private def updatePreferredSize(): Unit = {
    in = getInsets(in)
    val d = new Dimension(20 + in.left + in.right, 20 + in.top + in.bottom)
    setMinimumSize  (d)
    setPreferredSize(d)
  }

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    val g2        = g.asInstanceOf[Graphics2D]
    val strkOrig  = g2.getStroke
    val atOrig    = g2.getTransform
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )
    g2.translate(0.5f + in.left, 0.5f + in.top)

    val colors = if (isEnabled) scheme else schemeD
    import colors._
    g2.setPaint(pntBack)
    g2.fillOval(2, 3, 16, 16)
    g2.setPaint(pntLight)
    g2.fillOval(5, 1, 9, 10)
    g2.setPaint(pntArcShadow)
    g2.setStroke(strkArcShadow)
    g2.drawOval(1, 1, 17, 17)
    g2.setStroke(strkArcLight)
    g2.setPaint(pntArcLight)
    g2.drawArc(1, 2, 17, 17, 180, 180)
    g2.setPaint(pntOutline)
    g2.setStroke(strkOutline)
    g2.drawOval(1, 1, 17, 17)
    g2.translate(bellyPos.getX * 4 + 10.0, -bellyPos.getY * 4.5 + 10.0)
    g2.setPaint(pntBelly)
    g2.fill(shpBelly)

    g2.setStroke(strkOrig)
    g2.setTransform(atOrig)
  }

  def addListener   (l: Listener): Unit = listenerList.add   (classOf[Listener], l)
  def removeListener(l: Listener): Unit = listenerList.remove(classOf[Listener], l)

  protected def dispatchChange(delta: Int, adjusting: Boolean): Unit = {
    val listeners = getListeners(classOf[Listener])
    if (listeners.length == 0) return

    val e = new Event(me, value = delta, isAdjusting = adjusting)
    var i = 0; while (i < listeners.length) {
      listeners(i).jogDragged(e)
      i += 1
    }
  }
}
