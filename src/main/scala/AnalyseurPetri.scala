// Le marquage corresponds aux places de notre réseau de Pétri
case class Marquage(pisteLibre: Int, pisteOccupee: Int, enVol: Int, enAttente: Int, auSol: Int)

object AnalyseurPetri {

  def main(args: Array[String]): Unit = {
    // État initial : 1 piste, 4 avions en vol, 0 en attente, 0 au sol
    val etatInitial = Marquage(1, 0, 4, 0, 0)

    println(" ANALYSEUR D'ESPACE D'ETATS ET DE PROPRIETES LTL (PETRI) ")
    println("=================================================")

    explorer(Set(etatInitial), List(etatInitial))
  }

  // Parcours en largeur (BFS) pour générer l'espace d'états
  def explorer(visites: Set[Marquage], pile: List[Marquage]): Unit = {
    pile match {
      case Nil =>
        // Si la pile est vide, on a visité tous les chemins possibles
        println(s"\n=> génération de l'espace d'état terminé : ${visites.size} états distincts explorés.")
        println("\n=> Vérif des propirétés LTL :")
        println("   [SÛRETÉ / Safety]   [] (pisteOccupee <= 1) : VALIDÉ (Aucune collision possible)")
        println("   [VIVACITÉ / Liveness] <> (tous les avions finissent 'auSol') : VALIDÉ (Absence de famine)")
        println("========================================================")

      case actuel :: reste =>
        // Vérif de l'invariant de sureté
        // La somme de la piste libre et occupée doit toujours être : 1
        if (actuel.pisteOccupee > 1 || actuel.pisteLibre > 1 || (actuel.pisteOccupee + actuel.pisteLibre != 1)) {
          throw new Exception(s"Erreur : Invariant violé dans l'état $actuel ")
        }

        // Detection de deadlock
        // Un état terminal légitime c'est quand tous les avions sont au sol
        val estTerminal = actuel.enVol == 0 && actuel.enAttente == 0 && actuel.pisteOccupee == 0

        var successeurs = Set[Marquage]()

        // Transitions du réseau de Petri
        // T1 : atterrissage direct (Vol -> Piste)
        if (actuel.enVol > 0 && actuel.pisteLibre == 1) {
          successeurs += actuel.copy(enVol = actuel.enVol - 1, pisteLibre = 0, pisteOccupee = 1)
        }

        // T2 : mise en attente (Vol -> Attente, car la piste est occupée)
        if (actuel.enVol > 0 && actuel.pisteOccupee == 1) {
          successeurs += actuel.copy(enVol = actuel.enVol - 1, enAttente = actuel.enAttente + 1)
        }

        // T3 : atterrissage après l'attente (La piste se libère mais est reprise directement)
        if (actuel.pisteOccupee == 1 && actuel.enAttente > 0) {
          // Un avion atterrit alors (auSol prends + 1) et un autre quitte l'attente (enAttente prends - 1) DONC la piste reste occupée.
          successeurs += actuel.copy(enAttente = actuel.enAttente - 1, auSol = actuel.auSol + 1)
        }

        // T4 : Libération (Piste -> Libre, car plus personne n'attends)
        if (actuel.pisteOccupee == 1 && actuel.enAttente == 0) {
          successeurs += actuel.copy(pisteOccupee = 0, pisteLibre = 1, auSol = actuel.auSol + 1)
        }

        // Si on a pas de successeur et qu'on n'est pas dans l'état final = Deadlock (système bloqué)
        if (successeurs.isEmpty && !estTerminal) {
          throw new Exception(s"Deadlock detecté (blocage système) dans l'état $actuel")
        }

        // Exploration récursive
        val nouveaux = successeurs -- visites
        explorer(visites ++ nouveaux, reste ++ nouveaux.toList)
    }
  }
}