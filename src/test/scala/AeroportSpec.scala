import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

// Classe de test Akka officielle
class AeroportSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Le système de l'Aéroport (Tour de Contrôle + Piste)" should {

    "TEST 1 : Autoriser directement un avion si la piste est libre" in {
      // Préparation
      val piste = testKit.spawn(Piste.libre(), "Piste-Test1")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test1")
      val probeAvion = testKit.createTestProbe[Message]()

      // Action
      tour ! DemandeAtterrissage("Avion-VIP", probeAvion.ref)

      // Vérification : L'avion DOIT recevoir une autorisation directe
      probeAvion.expectMessage(AutorisationAccordee)
    }

    "TEST 2 : Mettre en attente un deuxième avion si la piste est occupée" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Test2")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test2")
      val probeAvion1 = testKit.createTestProbe[Message]()
      val probeAvion2 = testKit.createTestProbe[Message]()

      // Avion 1 prend la piste
      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)

      // Avion 2 arrive juste après
      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)

      // Vérification : L'avion 2 DOIT être mis en attente
      probeAvion2.expectMessage(MiseEnAttente)
    }

    "TEST 3 : Respecter l'ordre FIFO (First In, First Out) lors de l'attente" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Test3")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test3")

      val probeAvion1 = testKit.createTestProbe[Message]()
      val probeAvion2 = testKit.createTestProbe[Message]()
      val probeAvion3 = testKit.createTestProbe[Message]()

      // Avion 1 bloque la piste
      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)

      // Avions 2 et 3 sont mis en attente
      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(MiseEnAttente)

      tour ! DemandeAtterrissage("Avion-3", probeAvion3.ref)
      probeAvion3.expectMessage(MiseEnAttente)

      // Avion 1 a fini et libère la piste
      tour ! FinAtterrissage("Avion-1")

      // Vérification FIFO : Avion 2 DOIT recevoir l'autorisation avant Avion 3
      probeAvion2.expectMessage(AutorisationAccordee)
      probeAvion3.expectNoMessage() // L'avion 3 attend toujours
    }

    "TEST 4 : Libérer totalement la piste quand la file est vide" in {
      val piste = testKit.spawn(Piste.libre(), "Piste-Test4")
      val tour = testKit.spawn(TourDeControle(piste), "Tour-Test4")
      val probeAvion1 = testKit.createTestProbe[Message]()
      val probeAvion2 = testKit.createTestProbe[Message]()

      // Avion 1 fait un atterrissage complet
      tour ! DemandeAtterrissage("Avion-1", probeAvion1.ref)
      probeAvion1.expectMessage(AutorisationAccordee)
      tour ! FinAtterrissage("Avion-1")

      // La piste devrait être libre. Si Avion 2 arrive, il passe direct.
      tour ! DemandeAtterrissage("Avion-2", probeAvion2.ref)
      probeAvion2.expectMessage(AutorisationAccordee)
    }
  }
}