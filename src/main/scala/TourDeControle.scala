import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable.Queue

object TourDeControle {
  def apply(piste: ActorRef[Message]): Behavior[Message] = attenteDemande(piste, Queue.empty, pisteLibre = true)

  def attenteDemande(piste: ActorRef[Message], file: Queue[(String, ActorRef[Message])], pisteLibre: Boolean): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case DemandeAtterrissage(id, replyTo) =>
        if (pisteLibre) {
          context.log.info(s"TOUR : Piste libre, demande de réservation pour $id...")
          piste ! ReserverPour(id, context.self)
          replyTo ! AutorisationAccordee // <--- LA LIGNE AJOUTÉE POUR LES TESTS
          attenteDemande(piste, file, pisteLibre = false)
        } else {
          context.log.warn(s"TOUR : Conflit ! Piste indisponible, $id placé en attente.")
          replyTo ! MiseEnAttente
          attenteDemande(piste, file.enqueue((id, replyTo)), pisteLibre)
        }

      case ReservationAccordee(id) =>
        context.log.info(s"TOUR : Autorisation envoyée à $id.")
        Behaviors.same

      case FinAtterrissage(id) =>
        context.log.info(s"TOUR : Fin d'atterrissage confirmée pour $id. Libération de la piste.")
        piste ! Liberer(id)

        if (file.nonEmpty) {
          val ((prochainAvion, ref), nouvelleFile) = file.dequeue
          context.log.info(s"TOUR : On rappelle l'avion en file : $prochainAvion")
          piste ! ReserverPour(prochainAvion, context.self)
          ref ! AutorisationAccordee
          attenteDemande(piste, nouvelleFile, pisteLibre = false)
        } else {
          attenteDemande(piste, file, pisteLibre = true)
        }

      case _ => Behaviors.same
    }
  }
}