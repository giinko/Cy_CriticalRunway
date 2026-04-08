import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Piste {
  def libre(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case ReserverPour(id, replyTo) =>
        context.log.info(s"PISTE : Réservation confirmée pour $id.")
        replyTo ! ReservationAccordee(id)
        reservee(id)
      case _ => Behaviors.same
    }
  }

  def reservee(avionId: String): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Occuper(id) if id == avionId =>
        context.log.info(s"PISTE : Physiquement OCCUPÉE par $id.")
        occupee(id)
      case _ => Behaviors.same
    }
  }

  def occupee(avionId: String): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Liberer(id) if id == avionId =>
        context.log.info(s"PISTE : Libérée par $id. Retour à l'état LIBRE.")
        libre()
      case _ => Behaviors.same
    }
  }
}