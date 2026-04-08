import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import scala.collection.immutable.Queue

object Aeroport {

  // Protocole
  sealed trait Message
  case class DemandeAtterrissage(idAvion: String, replyTo: ActorRef[ReponseTour]) extends Message
  case class PisteLiberee(idAvion: String) extends Message

  sealed trait ReponseTour extends Message
  case object AutorisationAccordee extends ReponseTour
  case object MiseEnAttente extends ReponseTour

  // Acteur Avion
  object Avion {
    def apply(id: String, tour: ActorRef[Message]): Behavior[Message] = Behaviors.setup { context =>
      context.log.info(s"Avion $id : Approche initiée. Demande à la tour...")
      tour ! DemandeAtterrissage(id, context.self)

      Behaviors.receiveMessage {
        case AutorisationAccordee =>
          context.log.info(s"Avion $id : J'ATTERRIS ! (Phase critique en cours...)")
          // L'avion libère la piste immédiatement après
          tour ! PisteLiberee(id)
          context.log.info(s"Avion $id : Piste dégagée. Fin du vol.")
          Behaviors.stopped

        case MiseEnAttente =>
          context.log.info(s"Avion $id : Bien reçu. Je tourne en orbite et j'attends votre signal.")
          Behaviors.same // L'avion ne fait rien, il attend passivement que la tour le rappelle
      }
    }
  }

  // Tour de ctrl
  object TourDeControle {

    // Etat piste libre
    def libre(): Behavior[Message] = Behaviors.receive { (context, message) =>
      message match {
        case DemandeAtterrissage(id, replyTo) =>
          context.log.info(s"Tour : Piste libre. Autorisation directe accordée à $id.")
          replyTo ! AutorisationAccordee
          // On passe à l'état occupé avec une file d'attente vide
          occupee(id, Queue.empty)
        case _ => Behaviors.same
      }
    }

    // Etat piste occupée
    // On sauvegarde l'avion actuel et une file (Queue) des avions qui attendent
    def occupee(avionActuel: String, fileDAttente: Queue[(String, ActorRef[ReponseTour])]): Behavior[Message] = Behaviors.receive { (context, message) =>
      message match {
        case DemandeAtterrissage(id, replyTo) =>
          context.log.warn(s"Tour : Piste occupée par $avionActuel. On ajoute $id en file d'attente.")
          replyTo ! MiseEnAttente
          // On reste dans l'état occupé mais on ajoute le nouvel avion dans la file
          occupee(avionActuel, fileDAttente.enqueue((id, replyTo)))

        case PisteLiberee(id) if id == avionActuel =>
          context.log.info(s"Tour : Piste libérée par $id.")

          if (fileDAttente.isEmpty) {
            context.log.info("Tour : Plus aucun avion en attente. La piste devient maintenant libre.")
            libre()
          } else {
            // S'il y a des avions en attente, on sort le premier de la file
            val ((prochainAvion, ref), nouvelleFile) = fileDAttente.dequeue
            context.log.info(s"Tour : Appel de l'avion en attente -> $prochainAvion. Autorisation envoyée.")
            ref ! AutorisationAccordee
            // On reste occupé, mais c'est le prochain avion qui a la piste
            occupee(prochainAvion, nouvelleFile)
          }

        case _ => Behaviors.same
      }
    }
  }

  // Simulation de test
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Behaviors.setup[Unit] { context =>
      val tour = context.spawn(TourDeControle.libre(), "TourDeControle")

      // On lance 4 avions en même temps pour provoquer un gros conflit
      context.spawn(Avion("AF447", tour), "Avion_1")
      context.spawn(Avion("EZ202", tour), "Avion_2")
      context.spawn(Avion("LH123", tour), "Avion_3")
      context.spawn(Avion("RYA99", tour), "Avion_4")

      Behaviors.empty
    }, "AeroportSystem")
  }
}