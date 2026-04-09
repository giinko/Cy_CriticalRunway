import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors

object Main {
  object Superviseur {
    def apply(): Behavior[Unit] = Behaviors.setup { context =>
      context.log.info("SUPERVISEUR : Démarrage du système de l'aéroport...")

      val piste = context.spawn(
        Behaviors.supervise(Piste.libre()).onFailure[Exception](SupervisorStrategy.restart),
        "PisteUnique"
      )

      val tour = context.spawn(
        Behaviors.supervise(TourDeControle(piste)).onFailure[Exception](SupervisorStrategy.restart),
        "TourDeControle"
      )

      context.spawn(Avion("AF-111", tour, piste), "Avion-AF-111")
      context.spawn(Avion("EZ-222", tour, piste), "Avion-EZ-222")
      context.spawn(Avion("RY-333", tour, piste), "Avion-RY-333")

      Behaviors.empty
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Superviseur(), "AeroportCritiqueSystem")
  }
}
