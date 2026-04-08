# ✈️ Akka Airport : Simulation de Système Distribué Critique

## 📋 Présentation du Projet
Ce projet est une simulation haute fiabilité d'un système d'atterrissage aéroportuaire. Il utilise le framework **Akka Typed** et le langage **Scala** pour modéliser un environnement distribué où la sécurité est primordiale.

L'objectif est de combiner la puissance du modèle d'acteurs pour la simulation et la rigueur des **réseaux de Pétri** pour la vérification formelle, garantissant ainsi un système sans interblocages (deadlocks) ni violations d'invariants.

---

## 🏗️ Architecture du Système
L'application repose sur une architecture décentralisée et résiliente :

* **Avion** : Acteur autonome gérant son cycle de vie (En vol, En attente, Atterrissage, Au sol). Il simule un délai de manœuvre réel pour tester la concurrence.
* **Tour de Contrôle** : Coordinateur central. Elle gère une file d'attente FIFO (First-In-First-Out) pour assurer un traitement équitable des demandes.
* **Piste** : Représente la ressource critique. Elle garantit l'exclusion mutuelle grâce à une machine à états stricte (Libre, Réservée, Occupée).
* **Superviseur** : Garantit la tolérance aux pannes. Il utilise des stratégies de redémarrage et un système de mise en tampon (Stash) pour ne perdre aucun message en cas de crash d'un composant.

---

## 🔍 Vérification Formelle & Logique LTL
Le système est validé par la logique temporelle linéaire (LTL) pour assurer le respect des propriétés critiques :

### Propriétés de Sûreté (Safety)
* **Exclusion Mutuelle** : Un seul avion peut occuper la piste physiquement à la fois.
    * Formule : `[] (pisteOccupee <= 1)`
* **Intégrité** : La piste est toujours dans exactement un état valide.

### Propriétés de Vivacité (Liveness)
* **Absence de Famine** : Toute demande d'atterrissage finit par recevoir une autorisation si la piste se libère.
* **Progression** : Le système ne peut pas rester bloqué indéfiniment ; chaque avion finit par atteindre son état final "Au sol".

---

## 🛠️ Analyse par Réseau de Pétri
Le projet inclut un outil de vérification personnalisé (`AnalyseurPetri.scala`). Cet analyseur explore l'intégralité de l'espace d'états du modèle pour prouver :
* **Bornage** : Les ressources (jetons) sont limitées et contrôlées.
* **Absence de Deadlock** : Chaque chemin d'exécution mène à une progression du système.
* **Conformité** : Le comportement du code Akka correspond strictement au modèle mathématique.

---

## 🧪 Robustesse & Tests
* **Tolérance aux Pannes** : Un scénario de crash volontaire est inclus pour démontrer la capacité de la Tour de Contrôle à se resynchroniser avec la Piste.
* **Validation unitaire** : Une suite de tests (`AeroportSpec.scala`) vérifie automatiquement la logique de file d'attente et les priorités d'accès.

---

## 🚀 Mise en route

### Prérequis
* JDK 11 ou supérieur
* SBT (Scala Build Tool)

### Commandes utiles
* **Lancer la simulation** : `sbt run`
* **Exécuter les tests** : `sbt test`
* **Lancer l'analyseur formel** : `sbt "runMain verification.AnalyseurPetri"`
