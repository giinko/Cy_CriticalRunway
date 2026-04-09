import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Piste {

  def libre(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case ReserverPour(id, replyTo, avionRef) =>
        context.log.info(s"PISTE : Réservation confirmée pour $id.")
        replyTo ! ReservationAccordee(id, avionRef)
        reservee(id)

      case DemandeEtatPiste(replyTo) =>
        replyTo ! EtatPisteActuel(PisteLibre)
        Behaviors.same

      case Occuper(id) =>
        context.log.warn(s"PISTE : Occupation ignorée pour $id car la piste n'est pas réservée.")
        Behaviors.same

      case Liberer(id, _) =>
        context.log.warn(s"PISTE : Libération ignorée pour $id car la piste est déjà libre.")
        Behaviors.same

      case _ =>
        Behaviors.same
    }
  }

  def reservee(avionId: String): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Occuper(id) if id == avionId =>
        context.log.info(s"PISTE : Physiquement OCCUPÉE par $id.")
        occupee(id)

      case Occuper(id) =>
        context.log.warn(s"PISTE : Occupation refusée pour $id. Réservée à $avionId.")
        Behaviors.same

      case DemandeEtatPiste(replyTo) =>
        replyTo ! EtatPisteActuel(PisteReservee)
        Behaviors.same

      case Liberer(id, replyTo) if id == avionId =>
        context.log.info(s"PISTE : Réservation annulée/libérée pour $id. Retour à l'état LIBRE.")
        replyTo ! LiberationConfirmee(id)
        libre()

      case Liberer(id, _) =>
        context.log.warn(s"PISTE : Libération refusée pour $id. Réservée à $avionId.")
        Behaviors.same

      case ReserverPour(id, _, _) =>
        context.log.warn(s"PISTE : Nouvelle réservation refusée pour $id. Déjà réservée à $avionId.")
        Behaviors.same

      case _ =>
        Behaviors.same
    }
  }

  def occupee(avionId: String): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Liberer(id, replyTo) if id == avionId =>
        context.log.info(s"PISTE : Libérée par $id. Retour à l'état LIBRE.")
        replyTo ! LiberationConfirmee(id)
        libre()

      case Liberer(id, _) =>
        context.log.warn(s"PISTE : Libération refusée pour $id. Piste occupée par $avionId.")
        Behaviors.same

      case DemandeEtatPiste(replyTo) =>
        replyTo ! EtatPisteActuel(PisteOccupee)
        Behaviors.same

      case Occuper(id) =>
        context.log.warn(s"PISTE : Occupation refusée pour $id. Piste déjà occupée par $avionId.")
        Behaviors.same

      case ReserverPour(id, _, _) =>
        context.log.warn(s"PISTE : Réservation refusée pour $id. Piste occupée par $avionId.")
        Behaviors.same

      case _ =>
        Behaviors.same
    }
  }
}
