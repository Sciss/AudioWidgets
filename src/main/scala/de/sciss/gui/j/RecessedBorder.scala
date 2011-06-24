/*
 *  RecessedBorder.java
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.gui.j

import javax.swing.border.AbstractBorder
import java.awt.{RenderingHints, BasicStroke, Color, Component, Graphics, Graphics2D, Insets, Shape}
import java.awt.geom.{Rectangle2D, Area, RoundRectangle2D}

object RecessedBorder {
   private val diameter	   = 4
   private val colrDark		= new Color( 0x00, 0x00, 0x00, 0x88 )
   private val colrLight	= new Color( 0xFF, 0xFF, 0xFF, 0xD8 )
   private val strkOutline	= new BasicStroke( 1.0f )
   private val strkInline	= new BasicStroke( 2.0f )
   private val insets      = new Insets( 3, 3, 4, 4 )
}
class RecessedBorder( c: Color = Color.black ) extends AbstractBorder {
   import RecessedBorder._

	private var colorVar          = c
	private var shpBg: Shape      = null
   private var shpInline: Shape  = null
   private var shpOutline: Shape = null

	private var recentWidth		= -1
	private var recentHeight	= -1

	def color_=( value: Color ) {
      colorVar = value
   }
   def color : Color = colorVar

	override def getBorderInsets( c: Component ) = new Insets( insets.top, insets.left, insets.bottom, insets.right )

	override def getBorderInsets( c: Component, i: Insets ) : Insets = {
		i.top	   = insets.top
		i.left	= insets.left
		i.bottom	= insets.bottom
		i.right	= insets.right
		i
	}

	override def paintBorder( c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int ) {
		val g2      = g.asInstanceOf[ Graphics2D ]
		val atOrig  = g2.getTransform

		g2.translate( x, y )

		if( (width != recentWidth) || (height != recentHeight) ) {
			val r	   = new RoundRectangle2D.Float( 1.0f, 0.5f, width - 2f, height - 1.5f, diameter, diameter )
			val r2   = new RoundRectangle2D.Float( 0.5f, 0, width - 1.5f, height - 1, diameter, diameter )
			val a    = new Area( r )
			a.subtract( new Area( new Rectangle2D.Float( insets.left, insets.top,
				width - insets.left - insets.right, height - insets.top - insets.bottom )))

			shpOutline		= strkOutline.createStrokedShape( r2 );
			shpInline		= strkInline.createStrokedShape( r2 );
			shpBg			   = a;

			recentWidth		= width;
			recentHeight	= height;
		}

		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
		g2.setPaint( colrDark )
		g2.fill( shpOutline )
		g2.translate( 1, 1 )
		g2.setPaint( colrLight )
		g2.fill( shpInline )
		g2.translate( -1, -1 )
		g2.setPaint( colorVar )
		g2.fill( shpBg )

		g2.setTransform( atOrig )
	}
}