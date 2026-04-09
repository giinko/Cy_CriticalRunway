import akka.actor.typed.ActorRef

sealed trait Message

// ---------------------------
// États métier typés
// ---------------------------
sealed trait EtatPiste
case object PisteLibre extends EtatPiste
case object PisteReservee extends EtatPiste
case object PisteOccupee extends EtatPiste

sealed trait StatutAvion
case object AvionEnVol extends StatutAvion
case object AvionEnAttente extends StatutAvion
case object AvionAutorise extends StatutAvion
case object AvionAtterrissageEnCours extends StatutAvion
case object AvionAuSol extends StatutAvion

// ---------------------------
// Messages pour la Tour
// ---------------------------
case class DemandeAtterrissage(idAvion: String, replyTo: ActorRef[Message]) extends Message
case class FinAtterrissage(idAvion: String) extends Message
case class ReservationAccordee(idAvion: String, avionRef: ActorRef[Message]) extends Message
case class LiberationConfirmee(idAvion: String) extends Message
case object DeclencherCrashTest extends Message
case class DemandeEtatPiste(replyTo: ActorRef[Message]) extends Message
case class EtatPisteActuel(etat: EtatPiste) extends Message

// ---------------------------
// Messages pour l'Avion
// ---------------------------
case object AutorisationAccordee extends Message
case object MiseEnAttente extends Message
case object FinDeManoeuvre extends Message
case class EtatAvionObserve(idAvion: String, etat: StatutAvion) extends Message

// ---------------------------
// Messages pour la Piste
// ---------------------------
case class ReserverPour(idAvion: String, replyTo: ActorRef[Message], avionRef: ActorRef[Message]) extends Message
case class Occuper(idAvion: String) extends Message
case class Liberer(idAvion: String, replyTo: ActorRef[Message]) extends Message
