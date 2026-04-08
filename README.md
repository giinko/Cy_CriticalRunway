# ✈️ Modélisation et Vérification d'un Aéroport Critique (Akka/Scala & Pétri)

## 📋 Présentation du Projet
[cite_start]Ce projet vise à modéliser et vérifier un système distribué critique utilisant **Akka** et **Scala**, en recourant aux **réseaux de Pétri** pour l'analyse formelle[cite: 88]. [cite_start]L'objectif est de garantir la fiabilité, l'absence de deadlocks et le respect des invariants métier dans un contexte applicatif où les erreurs sont critiques[cite: 89, 100].

[cite_start]Le domaine choisi est celui de l'aéroport (gestion des atterrissages), un environnement intrinsèquement critique et intuitif pour la modélisation[cite: 18].

---

## 🏗️ Architecture du Système (Akka Typed)
[cite_start]L'architecture a été simplifiée pour privilégier la cohérence et la clarté[cite: 10]. [cite_start]Elle se compose de quatre acteurs principaux[cite: 32]:

* [cite_start]**Avion** : Demande l'atterrissage, attend si besoin, et signale la fin d'utilisation de la piste[cite: 32].
* [cite_start]**Tour de Contrôle** : Coordinateur qui maintient la file d'attente (FIFO) et décide quel avion doit être traité[cite: 32].
* [cite_start]**Piste** : Ressource critique portant l'état réel (Libre, Réservée, Occupée) pour garantir une utilisation cohérente[cite: 32].
* [cite_start]**Superviseur** : Point d'entrée qui crée les acteurs et assure la tolérance aux pannes[cite: 32].

---

## 🔍 Vérification Formelle & Logique LTL
[cite_start]Le projet utilise la logique temporelle linéaire (**LTL**) pour formaliser les propriétés de sûreté et de vivacité[cite: 119, 120].

### Propriétés de Sûreté (Safety)
* [cite_start]**Exclusion Mutuelle** : Deux avions ne peuvent jamais être sur la piste en même temps[cite: 54]. 
    * *Formule* : `[] (pisteOccupee <= 1)`
* [cite_start]**Autorisation préalable** : Un avion ne peut pas atterrir sans autorisation[cite: 54].
* [cite_start]**Unicité d'état** : La piste est toujours dans exactement un état parmi (Libre, Réservée, Occupée)[cite: 54].

### Propriétés de Vivacité (Liveness)
* [cite_start]**Absence de Famine** : Toute demande d'atterrissage finit par recevoir une réponse[cite: 54].
    * *Formule* : `[] (Demande -> <> Réponse)`
* [cite_start]**Progression** : Tout avion autorisé finit par atteindre l'état AuSol[cite: 54].

---

## 🛠️ Analyse du Réseau de Pétri
[cite_start]Un modèle formel sous forme de réseau de Pétri a été construit pour capturer tous les chemins de communication et les espaces d'états[cite: 105, 106].

* **Bornage** : Le réseau est borné par le nombre fini d'avions injectés dans le système.
* [cite_start]**Absence de Deadlock** : L'analyseur explore l'espace d'états pour valider qu'aucune séquence de transitions ne mène à un blocage définitif[cite: 108].
* [cite_start]**Analyseur Structurel** : Un outil de vérification personnalisé (`AnalyseurPetri.scala`) permet d'étudier les invariants de marquage[cite: 118].

---

## 🧪 Simulation & Robustesse
* [cite_start]**Temporalité** : Une durée d'atterrissage réelle est simulée pour rendre la concurrence visible[cite: 18].
* [cite_start]**Gestion des pannes** : Le système utilise des stratégies de supervision Akka (`Restart`) pour démontrer sa robustesse face aux crashs d'acteurs[cite: 59].
* [cite_start]**Tests Akka** : Des tests unitaires valident le comportement FIFO et la libération correcte des ressources[cite: 59].

---

## 📚 Bibliographie de Référence
[cite_start]Conformément aux livrables attendus[cite: 122]:
1.  **Lightbend** : *Documentation officielle Akka (Actor Lifecycle & Supervision)*.
2.  **Murata, T. (1989)** : *Petri Nets: Properties, Analysis and Applications*.
3.  **Baier & Katoen** : *Principles of Model Checking* (pour les fondements de la logique LTL).

---

## 🚀 Utilisation
* **Lancement de la simulation** : `sbt run`
* **Exécution des tests** : `sbt test`
* **Vérification formelle** : `runMain verification.AnalyseurPetri`
