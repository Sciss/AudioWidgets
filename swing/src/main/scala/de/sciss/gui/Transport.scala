package de.sciss.gui

import j.TransportCompanion
import swing.{Button, Orientation, BoxPanel, AbstractButton, Component}
import collection.immutable.{IndexedSeq => IIdxSeq}

object Transport extends TransportCompanion {
   type ComponentType      = Component
   type AbstractButtonType = AbstractButton
   type Action             = swing.Action with ActionLike

   protected def makeAction( icn: IconImpl, fun: => Unit ) : Action = new ActionImpl( icn, fun )

   private final class SButtonStripImpl( protected val actions: Seq[ Action ], protected val scheme: ColorScheme )
   extends BoxPanel( Orientation.Horizontal ) with ButtonStripImpl {
      protected def addButtons( seq: IIdxSeq[ AbstractButton ]) {
         seq.foreach( contents += )
      }

      protected def makeButton( pos: String, action: Action ) : AbstractButton = {
         val b = new Button( action )
         b.focusable = false
         b.peer.putClientProperty( "JButton.buttonType", "segmentedCapsule" )   // "segmented" "segmentedRoundRect" "segmentedCapsule" "segmentedTextured" "segmentedGradient"
         b.peer.putClientProperty( "JButton.segmentPosition", pos )
         b
      }
   }

   def makeButtonStrip( actions: Seq[ ActionElement ], scheme: ColorScheme = DarkScheme ) : Component with ButtonStrip = {
      val a = actions.map( _.apply( 0.8f, scheme ))
      new SButtonStripImpl( a, scheme )
   }

   private final class ActionImpl( icn: IconImpl, fun: => Unit ) extends swing.Action( null ) with ActionLike {
      icon = icn

      def element: Element = icn.element
      def scale: Float = icn.scale

      def apply() {
         fun
      }
   }
}