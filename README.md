# ✈️ Akka Airport : Simulation de système distribué critique

## Présentation
Ce projet simule un système d'atterrissage aéroportuaire critique avec **Akka Typed** et **Scala**.
La partie Akka/Scala repose sur trois acteurs principaux :

- **Avion** : gère son cycle de vie (`En vol`, `En attente`, `Autorisé`, `Atterrissage en cours`, `Au sol`)
- **Tour de contrôle** : coordonne l'accès à la piste et maintient une file d'attente FIFO
- **Piste** : ressource critique avec exclusion mutuelle stricte (`Libre`, `Réservée`, `Occupée`)

## Choix techniques principaux
- Piste modélisée comme **acteur séparé**
- Gestion **FIFO** des demandes concurrentes
- **Délai réel de manœuvre** pour rendre la concurrence crédible
- **Supervision** simple avec redémarrage de la tour de contrôle
- **Tests unitaires + tests d'intégration**

## Commandes utiles
- Lancer la simulation : `sbt run`
- Lancer les tests : `sbt test`
- Lancer l'analyseur formel : `sbt "runMain verification.AnalyseurPetri"`

## Remarque sur la supervision
Le redémarrage de la tour illustre une **resynchronisation avec l'état de la piste**.
Il s'agit d'une reprise simple et maîtrisée, pas d'une persistance complète de tout l'état métier.
