//> using scala 3

package verification

case class Marquage(
  enVol               : Int,
  enAttente           : Int,
  autorise            : Int,
  atterrissageEnCours : Int,
  auSol               : Int,
  dansFile            : Int,
  pisteLibre          : Int,
  pisteReservee       : Int,
  pisteOccupee        : Int
) {
  override def toString: String = {
    val parts = Seq(
      if (enVol > 0)               s"EnVol=$enVol"                             else "",
      if (enAttente > 0)           s"EnAttente=$enAttente"                     else "",
      if (autorise > 0)            s"Autorise=$autorise"                       else "",
      if (atterrissageEnCours > 0) s"AtterrissageEnCours=$atterrissageEnCours" else "",
      if (auSol > 0)               s"AuSol=$auSol"                             else "",
      if (dansFile > 0)            s"DansFile=$dansFile"                       else "",
      if (pisteLibre > 0)          s"PisteLibre=$pisteLibre"                   else "",
      if (pisteReservee > 0)       s"PisteReservee=$pisteReservee"             else "",
      if (pisteOccupee > 0)        s"PisteOccupee=$pisteOccupee"               else ""
    ).filter(_.nonEmpty)
    s"Marquage(${parts.mkString(", ")})"
  }
}

object AnalyseurPetriBug {

  val NB_AVIONS = 3

  def main(args: Array[String]): Unit = {
    val etatInitial = Marquage(
      enVol               = NB_AVIONS,
      enAttente           = 0,
      autorise            = 0,
      atterrissageEnCours = 0,
      auSol               = 0,
      dansFile            = 0,
      pisteLibre          = 1,
      pisteReservee       = 0,
      pisteOccupee        = 0
    )

    println("=" * 60)
    println("  ANALYSEUR FORMEL — VERSION AVEC BUG VOLONTAIRE")
    println("=" * 60)
    println()
    println("  BUG INTRODUIT : T4 (Occuper) oublie de retirer")
    println("  le jeton de PisteReservee → la piste peut être")
    println("  à la fois Reservee ET Occupee en même temps.")
    println()
    println(s"Etat initial : $etatInitial")
    println()

    try {
      val (etats, arcs) = explorer(Set(etatInitial), List(etatInitial), Set.empty)
      println(s"Etats atteignables : ${etats.size}")
      println(s"Arcs               : ${arcs.size}")
      println()
      println("Resultat : AUCUNE VIOLATION DETECTEE")
      println("(le bug n'a pas ete attrapé — modele defaillant)")
    } catch {
      case e: Exception =>
        println()
        println("!" * 60)
        println(e.getMessage)
        println("!" * 60)
        println()
        println("=> L'analyseur a detecte la violation.")
        println("=> Sans lui, ce bug serait invisible dans les tests classiques.")
    }
  }

  // ── Transitions ──────────────────────────────────────────────

  def t1(m: Marquage): Option[Marquage] =
    if (m.enVol > 0)
      Some(m.copy(enVol = m.enVol - 1, enAttente = m.enAttente + 1))
    else None

  def t2(m: Marquage): Option[Marquage] =
    if (m.enAttente > 0 && m.pisteLibre == 1)
      Some(m.copy(
        enAttente     = m.enAttente - 1,
        autorise      = m.autorise + 1,
        pisteLibre    = 0,
        pisteReservee = 1
      ))
    else None

  def t3(m: Marquage): Option[Marquage] =
    if (m.enAttente > 0 && m.pisteLibre == 0)
      Some(m.copy(
        enAttente = m.enAttente - 1, 
        dansFile = m.dansFile + 1))
    else None

  // ⚠ BUG ICI : pisteReservee n'est pas remis à 0
  // Version correcte :
  //   pisteReservee = 0,
  //   pisteOccupee  = 1
  // Version buguée : on oublie de retirer pisteReservee
  // → la piste se retrouve Reservee=1 ET Occupee=1 en même temps
  def t4_BUGGUEE(m: Marquage): Option[Marquage] =
    if (m.autorise > 0 && m.pisteReservee == 1)
      Some(m.copy(
        autorise            = m.autorise - 1,
        atterrissageEnCours = m.atterrissageEnCours + 1,
        // pisteReservee = 0  ← LIGNE OUBLIÉE VOLONTAIREMENT
        pisteOccupee        = m.pisteOccupee + 1
      ))
    else None

  def t5(m: Marquage): Option[Marquage] =
    if (m.atterrissageEnCours > 0)
      Some(m.copy(
        atterrissageEnCours = m.atterrissageEnCours - 1,
        auSol               = m.auSol + 1
      ))
    else None

  def t6(m: Marquage): Option[Marquage] =
    if (m.pisteOccupee == 1)
      Some(m.copy(
        pisteOccupee = 0, 
        pisteLibre = 1))
    else None

  def t7(m: Marquage): Option[Marquage] =
    if (m.dansFile > 0 && m.pisteLibre == 1)
      Some(m.copy(
        dansFile      = m.dansFile - 1,
        autorise      = m.autorise + 1,
        pisteLibre    = 0,
        pisteReservee = 1
      ))
    else None

  val transitions: List[(String, Marquage => Option[Marquage])] = List(
    "T1-DemandeAtterrissage"  -> t1,
    "T2-AutorisationAccordee" -> t2,
    "T3-MiseEnAttente"        -> t3,
    "T4-Occuper [BUGGUEE]"    -> t4_BUGGUEE,
    "T5-FinAtterrissage"      -> t5,
    "T6-Liberer"              -> t6,
    "T7-TraiterFile"          -> t7
  )

  // ── Invariants ───────────────────────────────────────────────

  def verifierInvariants(m: Marquage): Unit = {

    if (m.pisteOccupee > 1)
      throw new Exception(
        s"[VIOLATION S1] Exclusion mutuelle violee !\n  Plusieurs avions sur la piste en meme temps.\n  Etat fautif : $m"
      )

    if (m.pisteLibre + m.pisteReservee + m.pisteOccupee != 1)
      throw new Exception(
        s"[VIOLATION S2] Etat de piste incoherent !\n  pisteLibre=${m.pisteLibre}, pisteReservee=${m.pisteReservee}, pisteOccupee=${m.pisteOccupee}\n  Somme = ${m.pisteLibre + m.pisteReservee + m.pisteOccupee} au lieu de 1.\n  Etat fautif : $m"
      )

    val totalAvions = m.enVol + m.enAttente + m.autorise +
                      m.atterrissageEnCours + m.auSol + m.dansFile
    if (totalAvions != NB_AVIONS)
      throw new Exception(
        s"[VIOLATION S3] Conservation des avions violee !\n  Total trouve = $totalAvions au lieu de $NB_AVIONS.\n  Etat fautif : $m"
      )

    if (m.autorise > 0 && m.pisteReservee == 0 && m.pisteOccupee == 0)
      throw new Exception(
        s"[VIOLATION S4] Avion autorise sans piste reservee !\n  Etat fautif : $m"
      )
  }

  // ── Exploration BFS ──────────────────────────────────────────

  def explorer(
    visites : Set[Marquage],
    file    : List[Marquage],
    arcs    : Set[(Marquage, String, Marquage)]
  ): (Set[Marquage], Set[(Marquage, String, Marquage)]) = {
    file match {
      case Nil => (visites, arcs)
      case actuel :: reste =>
        verifierInvariants(actuel)
        val successeurs = transitions.flatMap { case (nom, t) =>
          t(actuel).map(succ => (actuel, nom, succ))
        }
        val estTerminal = actuel.enVol == 0 && actuel.enAttente == 0 &&
                          actuel.dansFile == 0 && actuel.autorise == 0 &&
                          actuel.atterrissageEnCours == 0
        if (successeurs.isEmpty && !estTerminal)
          throw new Exception(s"[DEADLOCK] Aucune transition possible dans : $actuel")
        val nouveauxArcs  = arcs ++ successeurs
        val nouveauxEtats = successeurs.map(_._3).filterNot(visites.contains)
        explorer(visites ++ nouveauxEtats, reste ++ nouveauxEtats, nouveauxArcs)
    }
  }
}