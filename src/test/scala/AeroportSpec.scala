import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class AeroportSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Le système de l'aéroport (Akka / Scala)" should {

    "autoriser directement un avion si la piste est libre" in {
      val piste = spawn(Piste.libre(), "Piste-Test1")
      val tour = spawn(TourDeControle(piste), "Tour-Test1")
      val probeAvion = createTestProbe[Message]()

      tour ! DemandeAtterrissage("Avion-VIP", probeAvion.ref)

      probeAvion.expectMessage(AutorisationAccordee)
    }

    "mettre en attente un deuxième avion si la piste est occupée" in {
      val piste = spawn(Piste.libre(), "Piste-Test2")
      val tour = spawn(TourDeControle(piste), "Tour-Test2")
      val probeAvion1 = createTestProbe[Message]()
      val probeAvion2 = createTestProbe[Message]()

      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-1")

      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(MiseEnAttente)
    }

    "respecter l'ordre FIFO lors de l'attente" in {
      val piste = spawn(Piste.libre(), "Piste-Test3")
      val tour = spawn(TourDeControle(piste), "Tour-Test3")

      val probeAvion1 = createTestProbe[Message]()
      val probeAvion2 = createTestProbe[Message]()
      val probeAvion3 = createTestProbe[Message]()

      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-1")

      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(MiseEnAttente)
      tour ! DemandeAtterrissage("Avion-3", probeAvion3.ref)
      probeAvion3.expectMessage(MiseEnAttente)

      tour ! FinAtterrissage("Avion-1")

      probeAvion2.expectMessage(AutorisationAccordee)
      probeAvion3.expectNoMessage(300.millis)
    }

    "ignorer une libération avec un mauvais identifiant" in {
      val piste = spawn(Piste.libre(), "Piste-Test4")
      val tour = spawn(TourDeControle(piste), "Tour-Test4")
      val probeAvion1 = createTestProbe[Message]()
      val probeAvion2 = createTestProbe[Message]()
      val probeEtat = createTestProbe[Message]()
      val probeLiberation = createTestProbe[Message]()

      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-1")

      piste ! Liberer("Mauvais-Id", probeLiberation.ref)
      probeLiberation.expectNoMessage(300.millis)

      piste ! DemandeEtatPiste(probeEtat.ref)
      probeEtat.expectMessage(EtatPisteActuel(PisteOccupee))

      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(MiseEnAttente)
    }

    "redevenir libre quand la file est vide" in {
      val piste = spawn(Piste.libre(), "Piste-Test5")
      val tour = spawn(TourDeControle(piste), "Tour-Test5")
      val probeAvion1 = createTestProbe[Message]()
      val probeAvion2 = createTestProbe[Message]()
      val probeEtat = createTestProbe[Message]()

      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-1")
      tour ! FinAtterrissage("Avion-1")
      Thread.sleep(200)

      piste ! DemandeEtatPiste(probeEtat.ref)
      probeEtat.expectMessage(EtatPisteActuel(PisteLibre))

      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(AutorisationAccordee)
    }

    "se resynchroniser avec la piste après un crash de la tour" in {
      val piste = spawn(Piste.libre(), "Piste-Supervision")
      val tour = spawn(
        Behaviors.supervise(TourDeControle(piste)).onFailure[RuntimeException](SupervisorStrategy.restart),
        "Tour-Supervisee"
      )
      val probe = createTestProbe[Message]()

      tour ! DemandeAtterrissage("Avion-Initial", probe.ref)
      probe.expectMessage(AutorisationAccordee)
      piste ! Occuper("Avion-Initial")

      tour ! DeclencherCrashTest
      tour ! DemandeAtterrissage("Avion-Post-Crash", probe.ref)

      probe.expectMessage(MiseEnAttente)

      tour ! FinAtterrissage("Avion-Initial")
      probe.expectMessage(AutorisationAccordee)
    }

    "faire terminer correctement deux vrais avions avec la piste libérée à la fin" in {
      val piste = spawn(Piste.libre(), "Piste-Integration-2")
      val tour = spawn(TourDeControle(piste), "Tour-Integration-2")
      val obs1 = createTestProbe[Message]()
      val obs2 = createTestProbe[Message]()
      val probeEtat = createTestProbe[Message]()

      spawn(Avion("AF-111", tour, piste, 400.millis, Some(obs1.ref)), "Avion-Integration-AF-111")

      obs1.expectMessage(EtatAvionObserve("AF-111", AvionEnVol))
      obs1.expectMessage(EtatAvionObserve("AF-111", AvionAutorise))
      obs1.expectMessage(EtatAvionObserve("AF-111", AvionAtterrissageEnCours))

      spawn(Avion("EZ-222", tour, piste, 200.millis, Some(obs2.ref)), "Avion-Integration-EZ-222")

      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionEnVol))
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionEnAttente))

      obs1.expectMessage(EtatAvionObserve("AF-111", AvionAuSol))
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionAutorise))
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionAtterrissageEnCours))
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionAuSol))

      Thread.sleep(150)
      piste ! DemandeEtatPiste(probeEtat.ref)
      probeEtat.expectMessage(EtatPisteActuel(PisteLibre))
    }

    "faire terminer correctement trois vrais avions en conservant l'ordre logique" in {
      val piste = spawn(Piste.libre(), "Piste-Integration-3")
      val tour = spawn(TourDeControle(piste), "Tour-Integration-3")
      val obs1 = createTestProbe[Message]()
      val obs2 = createTestProbe[Message]()
      val obs3 = createTestProbe[Message]()
      val probeEtat = createTestProbe[Message]()

      spawn(Avion("AF-111", tour, piste, 500.millis, Some(obs1.ref)), "Avion-Integration3-AF-111")

      obs1.expectMessage(EtatAvionObserve("AF-111", AvionEnVol))
      obs1.expectMessage(EtatAvionObserve("AF-111", AvionAutorise))
      obs1.expectMessage(EtatAvionObserve("AF-111", AvionAtterrissageEnCours))

      spawn(Avion("EZ-222", tour, piste, 200.millis, Some(obs2.ref)), "Avion-Integration3-EZ-222")
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionEnVol))
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionEnAttente))

      spawn(Avion("RY-333", tour, piste, 200.millis, Some(obs3.ref)), "Avion-Integration3-RY-333")
      obs3.expectMessage(EtatAvionObserve("RY-333", AvionEnVol))
      obs3.expectMessage(EtatAvionObserve("RY-333", AvionEnAttente))

      obs1.expectMessage(EtatAvionObserve("AF-111", AvionAuSol))

      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionAutorise))
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionAtterrissageEnCours))
      obs2.expectMessage(EtatAvionObserve("EZ-222", AvionAuSol))

      obs3.expectMessage(EtatAvionObserve("RY-333", AvionAutorise))
      obs3.expectMessage(EtatAvionObserve("RY-333", AvionAtterrissageEnCours))
      obs3.expectMessage(EtatAvionObserve("RY-333", AvionAuSol))

      Thread.sleep(150)
      piste ! DemandeEtatPiste(probeEtat.ref)
      probeEtat.expectMessage(EtatPisteActuel(PisteLibre))
    }
  }
}
