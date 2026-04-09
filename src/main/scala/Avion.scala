import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object Avion {
  def apply(
      id: String,
      tour: ActorRef[Message],
      piste: ActorRef[Message],
      dureeManoeuvre: FiniteDuration = 1.second,
      observateur: Option[ActorRef[Message]] = None
  ): Behavior[Message] =
    enVol(id, tour, piste, dureeManoeuvre, observateur)

  private def notifier(observateur: Option[ActorRef[Message]], id: String, etat: StatutAvion): Unit = {
    observateur.foreach(_ ! EtatAvionObserve(id, etat))
  }

  def enVol(
      id: String,
      tour: ActorRef[Message],
      piste: ActorRef[Message],
      dureeManoeuvre: FiniteDuration,
      observateur: Option[ActorRef[Message]]
  ): Behavior[Message] = Behaviors.setup { context =>
    context.log.info(s"Avion $id : [EN VOL] Demande d'atterrissage...")
    notifier(observateur, id, AvionEnVol)
    tour ! DemandeAtterrissage(id, context.self)

    Behaviors.receiveMessage {
      case AutorisationAccordee =>
        context.log.info(s"Avion $id : [AUTORISÉ] Autorisation reçue immédiatement.")
        notifier(observateur, id, AvionAutorise)
        atterrissageEnCours(id, tour, piste, dureeManoeuvre, observateur)

      case MiseEnAttente =>
        context.log.info(s"Avion $id : [EN ATTENTE] Mise en file d'attente.")
        notifier(observateur, id, AvionEnAttente)
        enAttente(id, tour, piste, dureeManoeuvre, observateur)

      case _ =>
        Behaviors.same
    }
  }

  def enAttente(
      id: String,
      tour: ActorRef[Message],
      piste: ActorRef[Message],
      dureeManoeuvre: FiniteDuration,
      observateur: Option[ActorRef[Message]]
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case AutorisationAccordee =>
        context.log.info(s"Avion $id : [SORTIE D'ATTENTE] Autorisation reçue.")
        notifier(observateur, id, AvionAutorise)
        atterrissageEnCours(id, tour, piste, dureeManoeuvre, observateur)

      case _ =>
        Behaviors.same
    }
  }

  def atterrissageEnCours(
      id: String,
      tour: ActorRef[Message],
      piste: ActorRef[Message],
      dureeManoeuvre: FiniteDuration,
      observateur: Option[ActorRef[Message]]
  ): Behavior[Message] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      context.log.info(s"Avion $id : [ATTERRISSAGE EN COURS] Occupation de la piste...")
      notifier(observateur, id, AvionAtterrissageEnCours)
      piste ! Occuper(id)
      timers.startSingleTimer(FinDeManoeuvre, dureeManoeuvre)

      Behaviors.receiveMessage {
        case FinDeManoeuvre =>
          auSol(id, tour, observateur)

        case _ =>
          Behaviors.same
      }
    }
  }

  def auSol(
      id: String,
      tour: ActorRef[Message],
      observateur: Option[ActorRef[Message]]
  ): Behavior[Message] = Behaviors.setup { context =>
    context.log.info(s"Avion $id : [AU SOL] Atterrissage terminé.")
    notifier(observateur, id, AvionAuSol)
    tour ! FinAtterrissage(id)
    Behaviors.stopped
  }
}
