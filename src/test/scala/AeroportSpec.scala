import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.wordspec.AnyWordSpecLike

class AeroportSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Le système de l'Aéroport (Tour de Contrôle + Piste)" should {

    "TEST 1 : Autoriser directement un avion si la piste est libre" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Test1")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test1")
      val probeAvion = testKit.createTestProbe[Message]()

      // Action : Demande initiale
      tour ! DemandeAtterrissage("Avion-VIP", probeAvion.ref)

      // Vérification : L'autorisation doit être reçue
      probeAvion.expectMessage(AutorisationAccordee)
    }

    "TEST 2 : Mettre en attente un deuxième avion si la piste est occupée" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Test2")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test2")
      val probeAvion1 = testKit.createTestProbe[Message]()
      val probeAvion2 = testKit.createTestProbe[Message]()

      // L'avion 1 prend la piste
      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)

      // Simulation de l'occupation physique de la piste par l'avion 1
      piste ! Occuper("Avion-1")

      // L'avion 2 arrive pendant l'occupation
      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)

      // Vérification : L'avion 2 doit recevoir un message de mise en attente
      probeAvion2.expectMessage(MiseEnAttente)
    }

    "TEST 3 : Respecter l'ordre FIFO lors de l'attente" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Test3")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test3")

      val probeAvion1 = testKit.createTestProbe[Message]()
      val probeAvion2 = testKit.createTestProbe[Message]()
      val probeAvion3 = testKit.createTestProbe[Message]()

      // Avion 1 occupe la piste
      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-1")

      // Avions 2 et 3 entrent en file d'attente
      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(MiseEnAttente)
      tour ! DemandeAtterrissage("Avion-3", probeAvion3.ref)
      probeAvion3.expectMessage(MiseEnAttente)

      // Avion 1 termine son atterrissage
      tour ! FinAtterrissage("Avion-1")

      // Vérification : C'est l'Avion 2 (arrivé en premier) qui doit être autorisé
      probeAvion2.expectMessage(AutorisationAccordee)
      probeAvion3.expectNoMessage()
    }

    "TEST 4 : Libérer totalement la piste quand la file est vide" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Test4")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test4")
      val probeAvion1 = testKit.createTestProbe[Message]()
      val probeAvion2 = testKit.createTestProbe[Message]()

      // Cycle complet pour l'Avion 1
      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-1")
      tour ! FinAtterrissage("Avion-1") // Libère la piste via la Tour

      // L'Avion 2 doit pouvoir atterrir sans attente car la piste est redevenue LIBRE
      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(AutorisationAccordee)
    }

    "TEST 5 : Tolérance aux pannes - Redémarrage de la Tour" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Supervision")

      val tourBehavior = Behaviors.supervise(TourDeControle(piste))
        .onFailure[RuntimeException](SupervisorStrategy.restart)
      val tour = testKit.spawn(tourBehavior, "Tour-Supervisee")
      val probe = testKit.createTestProbe[Message]()

      // 1. Occupation initiale
      tour ! DemandeAtterrissage("Avion-Initial", probe.ref)
      probe.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-Initial")

      // 2. Crash provoqué
      tour ! DeclencherCrashTest

      // 3. Demande post-crash (sera mise en tampon/stash)
      tour ! DemandeAtterrissage("Avion-Post-Crash", probe.ref)

      // La tour redémarrée récupère l'état OCCUPÉE et met l'avion en file
      probe.expectMessage(MiseEnAttente)

      // 4. Libération de la piste
      tour ! FinAtterrissage("Avion-Initial")

      // La tour dépile l'avion et demande la réservation à la piste.
      // On attend l'autorisation finale.
      probe.expectMessage(AutorisationAccordee)
    }
  }
}