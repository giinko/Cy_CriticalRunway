import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable.Queue

object TourDeControle {

  def apply(piste: ActorRef[Message]): Behavior[Message] =
    // On alloue une capacité de stockage (stash) pour les messages reçus pendant le redémarrage
    Behaviors.withStash(100) { buffer =>
      Behaviors.setup { context =>
        context.log.info("TOUR : Initialisation/Redémarrage. Synchronisation avec la piste...")
        // On interroge la piste pour recaler l'état interne
        piste ! DemandeEtatPiste(context.self)

        def initialisation(): Behavior[Message] = Behaviors.receiveMessage {
          case EtatPisteActuel(libre) =>
            context.log.info(s"TOUR : Synchronisé ! État de la piste : ${if (libre) "LIBRE" else "OCCUPÉE"}")
            // On débloque les messages mis en tampon pendant le redémarrage
            buffer.unstashAll(attenteDemande(piste, Queue.empty, libre))

          case message =>
            // Si une demande arrive avant la fin de la synchronisation, on la garde en tampon
            context.log.info("TOUR : Synchronisation en cours, mise en tampon d'un message...")
            buffer.stash(message)
            Behaviors.same
        }

        initialisation()
      }
    }

  def attenteDemande(
                      piste: ActorRef[Message],
                      file: Queue[(String, ActorRef[Message])],
                      pisteLibre: Boolean
                    ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {

      case DemandeAtterrissage(id, avionRef) =>
        if (pisteLibre) {
          context.log.info(s"TOUR : Piste libre, demande de réservation pour $id...")
          piste ! ReserverPour(id, context.self, avionRef)
          attenteDemande(piste, file, pisteLibre = false)
        } else {
          context.log.warn(s"TOUR : Conflit ! Piste indisponible, $id placé en attente.")
          avionRef ! MiseEnAttente
          // Gestion FIFO de la file d'attente
          attenteDemande(piste, file.enqueue((id, avionRef)), pisteLibre)
        }

      case ReservationAccordee(id, avionRef) =>
        context.log.info(s"TOUR : Réservation validée. Autorisation envoyée à $id.")
        avionRef ! AutorisationAccordee
        Behaviors.same

      case FinAtterrissage(id) =>
        context.log.info(s"TOUR : Libération de la piste confirmée pour $id.")
        piste ! Liberer(id)

        if (file.nonEmpty) {
          val ((prochainAvion, refSuivant), nouvelleFile) = file.dequeue
          context.log.info(s"TOUR : Rappel du prochain avion en file : $prochainAvion")
          piste ! ReserverPour(prochainAvion, context.self, refSuivant)
          attenteDemande(piste, nouvelleFile, pisteLibre = false)
        } else {
          attenteDemande(piste, file, pisteLibre = true)
        }

      case DeclencherCrashTest =>
        context.log.error("TOUR : Déclenchement d'un crash volontaire pour test de supervision.")
        throw new RuntimeException("Crash simulé")

      case _ =>
        Behaviors.same
    }
  }
}