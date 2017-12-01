/*
 *  NimbusRadioThumb.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets.j.ui

import java.awt.MultipleGradientPaint.CycleMethod
import java.awt.geom.Ellipse2D
import java.awt.{Color, Graphics2D, LinearGradientPaint, Paint}

import de.sciss.audiowidgets.j.ui.NimbusHelper.adjustColor

/** A painter which is imitating the nimbus appearance of radio button knobs.
  * The colors have been slightly tuned so they look good with colored knobs.
  */
object NimbusRadioThumb {
  private def enabledGrad1colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey, 0.0f, -0.053201474f, -0.12941176f, 0), 
                 adjustColor(blueGrey, 0.0f,  0.006356798f, -0.44313726f, 0))

  private def enabledGrad2colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey, 0.055555582f, -0.10654225f, 0.23921567f, 0), 
                 adjustColor(blueGrey, 0.0f,         -0.07016757f, 0.12941176f, 0), 
                 adjustColor(blueGrey, 0.0f,         -0.07016757f, 0.12941176f, 0), 
                 adjustColor(blueGrey, 0.0f,         -0.07206477f, 0.17254901f, 0))
 
  private def disabledGrad1colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey, 0.0f, -0.06766917f, 0.07843137f,  0), 
                 adjustColor(blueGrey, 0.0f, -0.06413457f, 0.015686274f, 0))

  private def disabledGrad2colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey, 0.0f, -0.08466425f , 0.16470587f, 0),
                 adjustColor(blueGrey, 0.0f, -0.07016757f , 0.12941176f, 0), 
                 adjustColor(blueGrey, 0.0f, -0.07016757f , 0.12941176f, 0), 
                 adjustColor(blueGrey, 0.0f, -0.070703305f, 0.14117646f, 0), 
                 adjustColor(blueGrey, 0.0f, -0.07052632f,  0.1372549f,  0))

  private def overGrad1colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey, -0.00505054f, -0.027819552f, -0.2235294f, 0),
                 adjustColor(blueGrey,  0.0f,         0.24241486f,  -0.6117647f, 0))

  private def overGrad2colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey, 0.055555582f, -0.10655806f,  0.24313724f, 0),
                 adjustColor(blueGrey, 0.0f,         -0.17333623f,  0.20392156f, 0), 
                 adjustColor(blueGrey, 0.0f,         -0.167389056f, 0.20392156f, 0), 
                 adjustColor(blueGrey, 0.015f,       -0.17333623f,  0.20392156f, 0), 
                 adjustColor(blueGrey, 0.03f,        -0.16628903f,  0.24313724f, 0))

  private def pressedGrad1colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey,  0.055555582f, 0.23947367f, -0.6666667f, 0), 
                 adjustColor(blueGrey, -0.0777778f, -0.06815343f, -0.28235295f, 0))

  private def pressedGrad2colr(blueGrey: Color): Array[Color] =
    Array[Color](adjustColor(blueGrey,  0.0f,          -0.06866585f,    0.09803921f,  0), 
                 adjustColor(blueGrey, -0.0027777553f, -0.0018306673f, -0.02352941f,  0), 
                 adjustColor(blueGrey, -0.0027777553f, -0.0018306673f, -0.02352941f,  0), 
                 adjustColor(blueGrey,  0.002924025f,  -0.02047892f,    0.082352936f, 0))
  
  private final val ellipse: Ellipse2D  = new Ellipse2D.Float
  private final val grad1Frac           = Array[Float](0.0f, 1.0f)
  private final val enabledGrad2Frac    = Array[Float](0.06344411f, 0.43674698f, 0.52409637f, 0.88554215f)
  private final val grad2Frac           = Array[Float](0.06344411f, 0.36858007f, 0.72809666f, 0.82175225f, 1.0f)
  private final val pressedGrad2Frac    = Array[Float](0.06344411f, 0.35240963f, 0.5481928f, 0.9487952f)
}
class NimbusRadioThumb {
  import NimbusRadioThumb._
  
  private def createEnabledGradient1(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49789915966386533f    * shpW
    val startY  = shpY1 + -0.0042016806722689065f * shpH
    val endX    = shpX1 + 0.5f                    * shpW
    val endY    = shpY1 + 0.9978991596638656f     * shpH
    new LinearGradientPaint(startX, startY, endX, endY, grad1Frac, enabledGrad1colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  private def createEnabledGradient2(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49754901960784303f  * shpW
    val startY  = shpY1 + 0.004901960784313727f * shpH
    val endX    = shpX1 + 0.507352941176471f    * shpW
    val endY    = shpY1 + 1.0f                  * shpH
    new LinearGradientPaint(startX, startY, endX, endY, enabledGrad2Frac, enabledGrad2colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  private def createDisabledGradient1(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49789915966386533f    * shpW
    val startY  = shpY1 + -0.0042016806722689065f * shpH
    val endX    = shpX1 + 0.5f                    * shpW
    val endY    = shpY1 + 0.9978991596638656f     * shpH
    new LinearGradientPaint(startX, startY, endX, endY, grad1Frac, disabledGrad1colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  private def createDisabledGradient2(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49754901960784303f  * shpW
    val startY  = shpY1 + 0.004901960784313727f * shpH
    val endX    = shpX1 + 0.507352941176471f    * shpW
    val endY    = shpY1 + 1.0f                  * shpH
    new LinearGradientPaint(startX, startY, endX, endY, grad2Frac, disabledGrad2colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  private def createOverGradient1(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49789915966386533f    * shpW
    val startY  = shpY1 + -0.0042016806722689065f * shpH
    val endX    = shpX1 + 0.5f                    * shpW
    val endY    = shpY1 + 0.9978991596638656f     * shpH
    new LinearGradientPaint(startX, startY, endX, endY, grad1Frac, overGrad1colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  private def createOverGradient2(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49754901960784303f  * shpW
    val startY  = shpY1 + 0.004901960784313727f * shpH
    val endX    = shpX1 + 0.507352941176471f    * shpW
    val endY    = shpY1 + 1.0f                  * shpH
    new LinearGradientPaint(startX, startY, endX, endY, grad2Frac, overGrad2colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  private def createPressedGradient1(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49789915966386533f    * shpW
    val startY  = shpY1 + -0.0042016806722689065f * shpH
    val endX    = shpX1 + 0.5f                    * shpW
    val endY    = shpY1 + 0.9978991596638656f     * shpH
    new LinearGradientPaint(startX, startY, endX, endY, grad1Frac, pressedGrad1colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  private def createPressedGradient2(blueGrey: Color, shpX1: Float, shpY1: Float, shpW: Float, shpH: Float): Paint = {
    val startX  = shpX1 + 0.49754901960784303f  * shpW
    val startY  = shpY1 + 0.004901960784313727f * shpH
    val endX    = shpX1 + 0.507352941176471f    * shpW
    val endY    = shpY1 + 1.0f                  * shpH
    new LinearGradientPaint(startX, startY, endX, endY, pressedGrad2Frac, pressedGrad2colr(blueGrey), CycleMethod.NO_CYCLE)
  }

  def paint(state: Int, c: Color, g: Graphics2D, x: Int, y: Int, width: Int, height: Int): Unit = {
    val nimBase = NimbusHelper.baseColor
    if ((state & NimbusHelper.STATE_ENABLED) != 0) {
      val blueGrey = NimbusHelper.mixColorWithAlpha(NimbusHelper.blueGreyColor(nimBase), c)
      val focused = (state & NimbusHelper.STATE_FOCUSED) != 0
      if ((state & NimbusHelper.STATE_PRESSED) != 0) {
        if (focused) {
          paintFocusedPressed(g, blueGrey, x, y, width, height)
        } else {
          paintPressed(g, blueGrey, x, y, width, height)
        }
      } else if ((state & NimbusHelper.STATE_OVER) != 0) {
        if (focused) {
          paintFocusedOver(g, blueGrey, x, y, width, height)
        } else {
          paintOver(g, blueGrey, x, y, width, height)
        }
      } else {
        if (focused) {
          paintFocused(g, blueGrey, x, y, width, height)
        } else {
          paintEnabled(g, blueGrey, x, y, width, height)
        }
      }
    } else {
      val c2        = if (c == null) c else adjustColor(c, 0f, 0f, 0f, -112)
      val blueGrey  = NimbusHelper.mixColorWithAlpha(NimbusHelper.blueGreyColor(nimBase), c2)
      paintDisabled(g, blueGrey, x, y, width, height)
    }
  }

  private def paintFocusedPressed(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    paintFocusedBack(g,           x, y, width, height)
    paintPressedTop (g, blueGrey, x, y, width, height)
  }

  private def paintPressed(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    paintPressedBack(g, blueGrey, x, y, width, height)
    paintPressedTop (g, blueGrey, x, y, width, height)
  }

  private def paintPressedBack(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    val e1x = x      + 2f
    val e1y = y      + 3f
    val e1w = width  - 4f
    val e1h = height - 4f
    if (e1w > 0 && e1h > 0) {
      ellipse.setFrame(e1x, e1y, e1w, e1h)
      g.setColor(adjustColor(blueGrey, 0.0f, -0.110526316f, 0.25490195f, 0))
      g.fill(ellipse)
    }
  }

  private def paintPressedTop(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    val e2x = x      + 2f
    val e2y = y      + 2f
    val e2w = width  - 4f
    val e2h = height - 4f
    if (e2w > 0 && e2h > 0) {
      ellipse.setFrame                           (e2x, e2y, e2w, e2h)
      g.setPaint(createPressedGradient1(blueGrey, e2x, e2y, e2w, e2h))
      g.fill(ellipse)
    }
    val e3x = x      + 3f
    val e3y = y      + 3f
    val e3w = width  - 6f
    val e3h = height - 6f
    if (e3w > 0 && e3h > 0) {
      ellipse.setFrame                           (e3x, e3y, e3w, e3h)
      g.setPaint(createPressedGradient2(blueGrey, e3x, e3y, e3w, e3h))
      g.fill(ellipse)
    }
  }

  private def paintFocusedOver(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    paintFocusedBack(g,           x, y, width, height)
    paintOverTop    (g, blueGrey, x, y, width, height)
  }

  private def paintOver(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    paintEnabledBack(g, blueGrey, x, y, width, height)
    paintOverTop    (g, blueGrey, x, y, width, height)
  }

  private def paintOverTop(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    val e2x = x      + 2f
    val e2y = y      + 2f
    val e2w = width  - 4f
    val e2h = height - 4f
    if (e2w > 0 && e2h > 0) {
      ellipse.setFrame                        (e2x, e2y, e2w, e2h)
      g.setPaint(createOverGradient1(blueGrey, e2x, e2y, e2w, e2h))
      g.fill(ellipse)
    }
    val e3x = x      + 3f
    val e3y = y      + 3f
    val e3w = width  - 6f
    val e3h = height - 6f
    if (e3w > 0 && e3h > 0) {
      ellipse.setFrame                        (e3x, e3y, e3w, e3h)
      g.setPaint(createOverGradient2(blueGrey, e3x, e3y, e3w, e3h))
      g.fill(ellipse)
    }
  }

  private def paintFocused(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    paintFocusedBack(g,           x, y, width, height)
    paintEnabledTop (g, blueGrey, x, y, width, height)
  }

  private def paintFocusedBack(g: Graphics2D, x: Int, y: Int, width: Int, height: Int): Unit = {
    val e1x = x      + 0.6f
    val e1y = y      + 0.6f
    val e1w = width  - 1.2f
    val e1h = height - 1.2f
    if (e1w > 0 && e1h > 0) {
      ellipse.setFrame(e1x, e1y, e1w, e1h)
      g.setColor(NimbusHelper.focusColor)
      g.fill(ellipse)
    }
  }

  private def paintEnabled(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    paintEnabledBack(g, blueGrey, x, y, width, height)
    paintEnabledTop (g, blueGrey, x, y, width, height)
  }

  private def paintEnabledBack(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    val e1x = x      + 2f
    val e1y = y      + 3f
    val e1w = width  - 4f
    val e1h = height - 4f
    if (e1w > 0 && e1h > 0) {
      ellipse.setFrame(e1x, e1y, e1w, e1h)
      g.setColor(adjustColor(blueGrey, 0.0f, 0.0f, 0.0f, -112))
      g.fill(ellipse)
    }
  }

  private def paintEnabledTop(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    val e2x = x      + 2f
    val e2y = y      + 2f
    val e2w = width  - 4f
    val e2h = height - 4f
    if (e2w > 0 && e2h > 0) {
      ellipse.setFrame                           (e2x, e2y, e2w, e2h)
      g.setPaint(createEnabledGradient1(blueGrey, e2x, e2y, e2w, e2h))
      g.fill(ellipse)
    }
    val e3x = x      + 3f
    val e3y = y      + 3f
    val e3w = width  - 6f
    val e3h = height - 6f
    if (e3w > 0 && e3h > 0) {
      ellipse.setFrame                           (e3x, e3y, e3w, e3h)
      g.setPaint(createEnabledGradient2(blueGrey, e3x, e3y, e3w, e3h))
      g.fill(ellipse)
    }
  }

  private def paintDisabled(g: Graphics2D, blueGrey: Color, x: Int, y: Int, width: Int, height: Int): Unit = {
    val e2x = x      + 2f
    val e2y = y      + 2f
    val e2w = width  - 4f
    val e2h = height - 4f
    if (e2w > 0 && e2h > 0) {
      ellipse.setFrame                            (e2x, e2y, e2w, e2h)
      g.setPaint(createDisabledGradient1(blueGrey, e2x, e2y, e2w, e2h))
      g.fill(ellipse)
    }
    val e3x = x      + 3f
    val e3y = y      + 3f
    val e3w = width  - 6f
    val e3h = height - 6f
    if (e3w > 0 && e3h > 0) {
      ellipse.setFrame                            (e3x, e3y, e3w, e3h)
      g.setPaint(createDisabledGradient2(blueGrey, e3x, e3y, e3w, e3h))
      g.fill(ellipse)
    }
  }
}