import akka.actor.typed.ActorRef

sealed trait Message

// Messages pour la Tour
case class DemandeAtterrissage(idAvion: String, replyTo: ActorRef[Message]) extends Message
case class FinAtterrissage(idAvion: String) extends Message
case class ReservationAccordee(idAvion: String) extends Message

// Messages pour l'Avion
case object AutorisationAccordee extends Message
case object MiseEnAttente extends Message

// Messages pour la Piste
case class ReserverPour(idAvion: String, replyTo: ActorRef[Message]) extends Message
case class Occuper(idAvion: String) extends Message
case class Liberer(idAvion: String) extends Message