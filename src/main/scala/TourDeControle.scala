import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable.Queue

object TourDeControle {

  def apply(piste: ActorRef[Message]): Behavior[Message] =
    Behaviors.withStash(100) { buffer =>
      Behaviors.setup { context =>
        context.log.info("TOUR : Initialisation / redémarrage. Synchronisation avec la piste...")
        piste ! DemandeEtatPiste(context.self)

        def initialisation(): Behavior[Message] = Behaviors.receiveMessage {
          case EtatPisteActuel(etat) =>
            context.log.info(s"TOUR : Synchronisation terminée. État reçu = $etat")
            buffer.unstashAll(gestion(piste, Queue.empty, etat))

          case message =>
            context.log.info("TOUR : Synchronisation en cours, message mis en tampon.")
            buffer.stash(message)
            Behaviors.same
        }

        initialisation()
      }
    }

  private def gestion(
      piste: ActorRef[Message],
      file: Queue[(String, ActorRef[Message])],
      etatPiste: EtatPiste
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case DemandeAtterrissage(id, avionRef) =>
        etatPiste match {
          case PisteLibre =>
            context.log.info(s"TOUR : Piste libre, réservation demandée pour $id.")
            piste ! ReserverPour(id, context.self, avionRef)
            gestion(piste, file, PisteReservee)

          case PisteReservee | PisteOccupee =>
            context.log.warn(s"TOUR : Piste indisponible, $id placé en file d'attente.")
            avionRef ! MiseEnAttente
            gestion(piste, file.enqueue((id, avionRef)), etatPiste)
        }

      case ReservationAccordee(id, avionRef) =>
        context.log.info(s"TOUR : Réservation confirmée. Autorisation envoyée à $id.")
        avionRef ! AutorisationAccordee
        Behaviors.same

      case FinAtterrissage(id) =>
        context.log.info(s"TOUR : Fin d'atterrissage reçue pour $id. Demande de libération envoyée à la piste.")
        piste ! Liberer(id, context.self)
        attenteLiberation(piste, file)

      case EtatPisteActuel(etat) =>
        context.log.info(s"TOUR : Mise à jour explicite de l'état piste = $etat")
        gestion(piste, file, etat)

      case DeclencherCrashTest =>
        context.log.error("TOUR : Crash volontaire déclenché pour test de supervision.")
        throw new RuntimeException("Crash simulé de la tour de contrôle")

      case _ =>
        Behaviors.same
    }
  }

  private def attenteLiberation(
      piste: ActorRef[Message],
      file: Queue[(String, ActorRef[Message])]
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case LiberationConfirmee(id) =>
        context.log.info(s"TOUR : Libération confirmée pour $id.")
        if (file.nonEmpty) {
          val ((prochainId, prochainRef), nouvelleFile) = file.dequeue
          context.log.info(s"TOUR : Prochain avion en FIFO = $prochainId")
          piste ! ReserverPour(prochainId, context.self, prochainRef)
          gestion(piste, nouvelleFile, PisteReservee)
        } else {
          gestion(piste, Queue.empty, PisteLibre)
        }

      case DemandeAtterrissage(id, avionRef) =>
        context.log.warn(s"TOUR : Libération en cours, $id placé en file d'attente.")
        avionRef ! MiseEnAttente
        attenteLiberation(piste, file.enqueue((id, avionRef)))

      case EtatPisteActuel(etat) =>
        context.log.info(s"TOUR : État piste reçu pendant attente de libération = $etat")
        etat match {
          case PisteLibre =>
            if (file.nonEmpty) {
              val ((prochainId, prochainRef), nouvelleFile) = file.dequeue
              piste ! ReserverPour(prochainId, context.self, prochainRef)
              gestion(piste, nouvelleFile, PisteReservee)
            } else {
              gestion(piste, Queue.empty, PisteLibre)
            }
          case PisteReservee =>
            gestion(piste, file, PisteReservee)
          case PisteOccupee =>
            gestion(piste, file, PisteOccupee)
        }

      case DeclencherCrashTest =>
        context.log.error("TOUR : Crash volontaire déclenché pendant l'attente de libération.")
        throw new RuntimeException("Crash simulé de la tour de contrôle")

      case _ =>
        Behaviors.same
    }
  }
}
