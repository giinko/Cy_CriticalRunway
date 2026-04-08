import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration._

object Avion {
  def apply(id: String, tour: ActorRef[Message], piste: ActorRef[Message]): Behavior[Message] =
    enVol(id, tour, piste)

  // État : EnVol
  def enVol(id: String, tour: ActorRef[Message], piste: ActorRef[Message]): Behavior[Message] = Behaviors.setup { context =>
    context.log.info(s"Avion $id : [EN VOL] Demande d'atterrissage...")
    tour ! DemandeAtterrissage(id, context.self)

    Behaviors.receiveMessage {
      case AutorisationAccordee => atterrissageEnCours(id, tour, piste) // Transition vers l'atterrissage
      case MiseEnAttente => enAttente(id, tour, piste) // Transition vers l'attente
      case _ => Behaviors.same
    }
  }

  // État : EnAttente
  def enAttente(id: String, tour: ActorRef[Message], piste: ActorRef[Message]): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case AutorisationAccordee =>
        context.log.info(s"Avion $id : [SORTIE D'ATTENTE] Autorisation reçue.")
        atterrissageEnCours(id, tour, piste)
      case _ => Behaviors.same
    }
  }

  // État : AtterrissageEnCours
  def atterrissageEnCours(id: String, tour: ActorRef[Message], piste: ActorRef[Message]): Behavior[Message] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      context.log.info(s"Avion $id : [ATTERRISSAGE EN COURS] Occupation de la piste...")
      piste ! Occuper(id)
      timers.startSingleTimer(FinDeManoeuvre, 1.second) // Délai d'occupation réel

      Behaviors.receiveMessage {
        case FinDeManoeuvre => auSol(id, tour) // Transition finale
        case _ => Behaviors.same
      }
    }
  }

  // État : AuSol
  def auSol(id: String, tour: ActorRef[Message]): Behavior[Message] = Behaviors.setup { context =>
    context.log.info(s"Avion ${context.self.path.name} : [AU SOL] Terminé.")
    tour ! FinAtterrissage(context.self.path.name.split("-").last) // Notification de libération [cite: 92]
    Behaviors.stopped
  }
}