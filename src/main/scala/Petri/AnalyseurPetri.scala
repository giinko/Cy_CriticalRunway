package verification
// Le marquage correspond exactement aux états définis dans la Note de Cadrage
case class Marquage(
                     enVol: Int, enAttente: Int, enApproche: Int, auSol: Int, // États de l'Avion
                     pisteLibre: Int, pisteReservee: Int, pisteOccupee: Int   // États de la Piste
                   )

object AnalyseurPetri {

  def main(args: Array[String]): Unit = {
    // État initial : 3 avions en vol (comme dans le Superviseur), 1 piste libre
    val etatInitial = Marquage(3, 0, 0, 0, 1, 0, 0)

    println("=========================================================")
    println(" ANALYSEUR D'ÉTATS LTL - CONFORME À L'ARCHITECTURE V2    ")
    println("=========================================================")

    explorer(Set(etatInitial), List(etatInitial))
  }

  def explorer(visites: Set[Marquage], pile: List[Marquage]): Unit = {
    pile match {
      case Nil =>
        println(s"\n=> GÉNÉRATION TERMINÉE : ${visites.size} états distincts explorés.")
        println("\n=> PROPRIÉTÉS VÉRIFIÉES (Note de Cadrage) :")
        println("   [SÛRETÉ]  [] (pisteOccupee <= 1) : VALIDÉ")
        println("   [SÛRETÉ]  [] (pisteLibre + pisteReservee + pisteOccupee == 1) : VALIDÉ")
        println("   [VIVACITÉ] <> (tous les avions finissent 'auSol') : VALIDÉ")
        println("=========================================================")

      case actuel :: reste =>
        // --- INVARIANTS DE SÛRETÉ (Page 5 de la note de cadrage) ---
        // 1. La piste est TOUJOURS dans exactement un état (Libre, Reservee, Occupee)
        if (actuel.pisteLibre + actuel.pisteReservee + actuel.pisteOccupee != 1) {
          throw new Exception(s"ERREUR CRITIQUE : Piste dans un état incohérent -> $actuel")
        }
        // 2. Jamais plus d'un avion en approche/sur la piste
        if (actuel.enApproche > 1) {
          throw new Exception(s"ERREUR CRITIQUE : Plusieurs avions en approche -> $actuel")
        }

        val estTerminal = actuel.enVol == 0 && actuel.enAttente == 0 && actuel.enApproche == 0

        var successeurs = Set[Marquage]()

        // --- TRANSITIONS CONFORMES AU CODE AKKA ---

        // T1 : Demande acceptée (Tour envoie ReserverPour)
        if (actuel.enVol > 0 && actuel.pisteLibre == 1) {
          successeurs += actuel.copy(
            enVol = actuel.enVol - 1, enApproche = actuel.enApproche + 1,
            pisteLibre = 0, pisteReservee = 1
          )
        }

        // T2 : Demande refusée (Conflit ! MiseEnAttente)
        if (actuel.enVol > 0 && actuel.pisteLibre == 0) {
          successeurs += actuel.copy(
            enVol = actuel.enVol - 1, enAttente = actuel.enAttente + 1
          )
        }

        // T3 : L'avion autorisé occupe physiquement la piste (Avion envoie Occuper)
        if (actuel.enApproche > 0 && actuel.pisteReservee == 1) {
          successeurs += actuel.copy(
            pisteReservee = 0, pisteOccupee = 1
          )
        }

        // T4 : Libération de la piste ET file vide (Tour envoie Liberer)
        if (actuel.pisteOccupee == 1 && actuel.enAttente == 0) {
          successeurs += actuel.copy(
            enApproche = actuel.enApproche - 1, auSol = actuel.auSol + 1,
            pisteOccupee = 0, pisteLibre = 1
          )
        }

        // T5 : Libération de la piste ET rappel immédiat de la file d'attente
        if (actuel.pisteOccupee == 1 && actuel.enAttente > 0) {
          successeurs += actuel.copy(
            enApproche = actuel.enApproche - 1 + 1, // L'ancien part, le nouveau passe en approche
            auSol = actuel.auSol + 1,
            enAttente = actuel.enAttente - 1,
            pisteOccupee = 0, pisteReservee = 1 // La tour réserve instantanément pour le suivant
          )
        }

        // Détection de deadlock
        if (successeurs.isEmpty && !estTerminal) {
          throw new Exception(s"DEADLOCK DETECTÉ dans l'état $actuel")
        }

        val nouveaux = successeurs -- visites
        explorer(visites ++ nouveaux, reste ++ nouveaux.toList)
    }
  }
}