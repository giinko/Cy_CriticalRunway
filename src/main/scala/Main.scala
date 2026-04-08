import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Main {
  object Superviseur {
    def apply(): Behavior[Unit] = Behaviors.setup { context =>
      context.log.info("SUPERVISEUR : Démarrage du système de l'aéroport...")

      val piste = context.spawn(Piste.libre(), "PisteUnique")
      val tour = context.spawn(TourDeControle(piste), "TourDeControle")

      context.spawn(Avion("AF-111", tour, piste), "Avion1")
      context.spawn(Avion("EZ-222", tour, piste), "Avion2")
      context.spawn(Avion("RY-333", tour, piste), "Avion3")

      Behaviors.empty
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Superviseur(), "AeroportCritiqueSystem")
  }
}