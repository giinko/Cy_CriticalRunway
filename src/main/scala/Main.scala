import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors

object Main {
  object Superviseur {
    def apply(): Behavior[Unit] = Behaviors.setup { context =>
      context.log.info("SUPERVISEUR : Démarrage du système de l'aéroport...")

      // Ajout d'une stratégie de supervision "Restart" (Tolérance aux pannes)
      val pisteBehavior = Behaviors.supervise(Piste.libre()).onFailure[Exception](SupervisorStrategy.restart)
      val piste = context.spawn(pisteBehavior, "PisteUnique")

      val tourBehavior = Behaviors.supervise(TourDeControle(piste)).onFailure[Exception](SupervisorStrategy.restart)
      val tour = context.spawn(tourBehavior, "TourDeControle")

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