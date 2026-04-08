import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}


  // L'acteur Aeroport
object Aeroport {

  // Protocole
  sealed trait Message

  // Messages pour la Tour de ctrl
  case class DemandeAtterrissage(idAvion: String, replyTo: ActorRef[ReponseTour]) extends Message
  case class PisteLiberee(idAvion: String) extends Message

  // Messages pour l'avion
  sealed trait ReponseTour extends Message
  case object AutorisationAccordee extends ReponseTour
  case object Attendre extends ReponseTour

  // L'acteur Avion
  object Avion {
    def apply(id: String, tour: ActorRef[Message]): Behavior[Message] = Behaviors.setup { context =>
      context.log.info(s"Avion $id : Demande d'atterrissage envoyée.")
      tour ! DemandeAtterrissage(id, context.self)

      Behaviors.receiveMessage {
        case AutorisationAccordee =>
          context.log.info(s"Avion $id : J'atterris sur la piste...")
          // On simule le temps d'atterrissage et après on libère
          tour ! PisteLiberee(id)
          context.log.info(s"Avion $id : je libère la piste, je suis au parking ")
          Behaviors.stopped // L'acteur a fini sa mission

        case Attendre =>
          context.log.info(s"Avion $id : J'ai reçu, je reste en orbite et j'attends...")
          Behaviors.same
      }
    }
  }

  // L'acteur Tour de ctrl
  object TourDeControle {
    // Etat piste libre
    def libre(): Behavior[Message] = Behaviors.receive { (context, message) =>
      message match {
        case DemandeAtterrissage(id, replyTo) =>
          context.log.info(s"Tour : piste libre. Je donne l'autorisation à $id ")
          replyTo ! AutorisationAccordee
          occupee(id) // passage à l'état occupé
        case _ => Behaviors.same
      }
    }

    // Etat piste occupée
    def occupee(avionActuel: String): Behavior[Message] = Behaviors.receive { (context, message) =>
      message match {
        case DemandeAtterrissage(id, replyTo) =>
          context.log.warn(s"Tour : Non. Piste occupée par $avionActuel. refus pour $id ")
          replyTo ! Attendre
          Behaviors.same
        case PisteLiberee(id) if id == avionActuel =>
          context.log.info(s"Tour : Piste libérée par $id. La piste est maintenant libre")
          libre() // Retour à l'état libre
        case _ => Behaviors.same
      }
    }
  }

  // Le main (lancement de la simulation)
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Behaviors.setup[Unit] { context =>
      val tour = context.spawn(TourDeControle.libre(), "TourDeControle")

      // On lance 3 avions en même temps pour tester la concurrence
      context.spawn(Avion("AirNassym", tour), "Avion1")
      context.spawn(Avion("AirFares", tour), "Avion2")
      context.spawn(Avion("EasyMaxime", tour), "Avion3")

      Behaviors.empty
    }, "SimulationAeroport")
  }
}