import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Avion {
  def apply(id: String, tour: ActorRef[Message], piste: ActorRef[Message]): Behavior[Message] = Behaviors.setup { context =>
    context.log.info(s"Avion $id : [EN VOL] Demande d'atterrissage...")
    tour ! DemandeAtterrissage(id, context.self)

    Behaviors.receiveMessage {
      case AutorisationAccordee =>
        context.log.info(s"Avion $id : [AUTORISÉ] Début de la manœuvre...")
        piste ! Occuper(id)

        // Simulation du délai d'atterrissage
        Thread.sleep(1000)

        context.log.info(s"Avion $id : [AU SOL] Atterrissage terminé.")
        tour ! FinAtterrissage(id)
        Behaviors.stopped

      case MiseEnAttente =>
        context.log.info(s"Avion $id : [EN ATTENTE] Boucle d'attente activée...")
        Behaviors.same

      case _ => Behaviors.same
    }
  }
}