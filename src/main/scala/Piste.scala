import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Piste {
  def libre(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case ReserverPour(id, replyTo, avionRef) =>
        context.log.info(s"PISTE : Réservation confirmée pour $id.")
        replyTo ! ReservationAccordee(id, avionRef)
        reservee(id)

      // Réponse à la Tour lors d'une synchronisation/redémarrage
      case DemandeEtatPiste(replyTo) =>
        replyTo ! EtatPisteActuel(libre = true)
        Behaviors.same

      case _ => Behaviors.same
    }
  }

  def reservee(avionId: String): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Occuper(id) if id == avionId =>
        context.log.info(s"PISTE : Physiquement OCCUPÉE par $id.")
        occupee(id)

      // La piste est réservée, donc elle n'est pas "libre" pour un nouvel arrivant
      case DemandeEtatPiste(replyTo) =>
        replyTo ! EtatPisteActuel(libre = false)
        Behaviors.same

      case _ => Behaviors.same
    }
  }

  def occupee(avionId: String): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Liberer(id) if id == avionId =>
        context.log.info(s"PISTE : Libérée par $id. Retour à l'état LIBRE.")
        libre()

      // La piste est occupée physiquement
      case DemandeEtatPiste(replyTo) =>
        replyTo ! EtatPisteActuel(libre = false)
        Behaviors.same

      case _ => Behaviors.same
    }
  }
}