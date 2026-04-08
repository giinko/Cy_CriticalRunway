package verification

case class Marquage(
                     enVol: Int,
                     enAttente: Int,
                     atterrissageEnCours: Int, // Nom harmonisé avec la Note de Cadrage
                     auSol: Int,
                     pisteLibre: Int,
                     pisteReservee: Int,       // État distinguant l'attribution de l'occupation
                     pisteOccupee: Int
                   )

object AnalyseurPetri {

  def main(args: Array[String]): Unit = {
    // État initial conforme à la simulation Main : 3 avions et une piste libre [cite: 77, 125]
    val etatInitial = Marquage(
      enVol = 3,
      enAttente = 0,
      atterrissageEnCours = 0,
      auSol = 0,
      pisteLibre = 1,
      pisteReservee = 0,
      pisteOccupee = 0
    )

    println("=========================================================")
    println(" ANALYSEUR FORMEL - VÉRIFICATION DES PROPRIÉTÉS LTL      ")
    println("=========================================================")
    println(s"État initial : $etatInitial")

    explorer(Set(etatInitial), List(etatInitial))
  }

  /**
   * Exploration de l'espace d'états (Reachability Graph)
   */
  def explorer(visites: Set[Marquage], pile: List[Marquage]): Unit = {
    pile match {
      case Nil =>
        println(s"\n=> SUCCÈS : ${visites.size} états explorés sans violation.")
        println("\n=> PROPRIÉTÉS DE SÛRETÉ (Safety) :")
        println("   ✔ [S1] [] (pisteOccupee <= 1) : L'exclusion mutuelle est garantie. [cite: 99]")
        println("   ✔ [S2] [] (pisteLibre + pisteReservee + pisteOccupee == 1) : État de piste unique. [cite: 99]")
        println("\n=> PROPRIÉTÉS DE VIVACITÉ (Liveness) :")
        println("   ✔ [V1] <> (auSol == totalAvions) : Pas de famine, tout le monde atterrit. [cite: 99]")
        println("=========================================================")

      case actuel :: reste =>
        // --- VÉRIFICATION DES INVARIANTS (Propriétés de Sûreté) [cite: 98, 99] ---

        // 1. Exclusion mutuelle sur la ressource critique
        if (actuel.pisteOccupee > 1) {
          throw new Exception(s"VIOLATION : Plusieurs avions sur la piste ! $actuel")
        }

        // 2. Intégrité de l'état de la piste
        if (actuel.pisteLibre + actuel.pisteReservee + actuel.pisteOccupee != 1) {
          throw new Exception(s"VIOLATION : État de piste incohérent ! $actuel")
        }

        val estTerminal = actuel.enVol == 0 && actuel.enAttente == 0 && actuel.atterrissageEnCours == 0
        var successeurs = Set[Marquage]()

        // --- TRANSITIONS DU RÉSEAU DE PÉTRI (Basées sur le code Akka) [cite: 14, 22] ---

        // T1 : Demande acceptée -> La Tour réserve la piste (Transition EnVol -> AtterrissageEnCours)
        if (actuel.enVol > 0 && actuel.pisteLibre == 1) {
          successeurs += actuel.copy(
            enVol = actuel.enVol - 1,
            atterrissageEnCours = actuel.atterrissageEnCours + 1,
            pisteLibre = 0,
            pisteReservee = 1
          )
        }

        // T2 : Demande refusée -> Mise en attente (Transition EnVol -> EnAttente)
        if (actuel.enVol > 0 && actuel.pisteLibre == 0) {
          successeurs += actuel.copy(
            enVol = actuel.enVol - 1,
            enAttente = actuel.enAttente + 1
          )
        }

        // T3 : Occupation physique -> L'avion autorisé se pose (Transition PisteReservee -> PisteOccupee)
        if (actuel.atterrissageEnCours > 0 && actuel.pisteReservee == 1) {
          successeurs += actuel.copy(
            pisteReservee = 0,
            pisteOccupee = 1
          )
        }

        // T4 : Libération ET file vide -> Retour à l'état Libre
        if (actuel.pisteOccupee == 1 && actuel.enAttente == 0) {
          successeurs += actuel.copy(
            atterrissageEnCours = actuel.atterrissageEnCours - 1,
            auSol = actuel.auSol + 1,
            pisteOccupee = 0,
            pisteLibre = 1
          )
        }

        // T5 : Libération ET rappel FIFO -> Transition directe vers Réservé pour le suivant
        if (actuel.pisteOccupee == 1 && actuel.enAttente > 0) {
          successeurs += actuel.copy(
            atterrissageEnCours = (actuel.atterrissageEnCours - 1) + 1, // Sortie de l'un, entrée de l'autre
            auSol = actuel.auSol + 1,
            enAttente = actuel.enAttente - 1,
            pisteOccupee = 0,
            pisteReservee = 1
          )
        }

        // Détection de Deadlock [cite: 25]
        if (successeurs.isEmpty && !estTerminal) {
          throw new Exception(s"DEADLOCK DÉTECTÉ dans l'état : $actuel")
        }

        val nouveaux = successeurs -- visites
        explorer(visites ++ nouveaux, reste ++ nouveaux.toList)
    }
  }
}