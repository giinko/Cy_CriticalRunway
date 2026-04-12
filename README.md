# Cy Critical Runway

**Cy Critical Runway** est un projet étudiant de modélisation et de vérification d’un système d’atterrissage critique à l’aide de **Scala**, **Akka Typed**, **réseaux de Pétri** et **LTL**.

L’objectif est de montrer comment relier :
- une **implémentation concurrente** basée sur des acteurs ;
- une **modélisation formelle** avec un réseau de Pétri ;
- une **spécification temporelle** de propriétés de sûreté et de vivacité ;
- et une **validation** par tests et exploration d’espace d’états.

---

## Idée du projet

Le système modélise un scénario d’atterrissage simplifié autour de trois acteurs principaux :

- **Avion** : représente un avion individuel et son cycle de vie ;
- **TourDeControle** : coordonne les demandes d’atterrissage et gère la file d’attente ;
- **Piste** : représente la ressource critique et impose l’exclusion mutuelle.

Le principe central est simple : **une seule piste, un seul avion à la fois**.

---

## Objectifs

Le projet vise à :

- modéliser un système distribué critique avec **Akka Typed** ;
- gérer la concurrence autour d’une ressource partagée ;
- traduire le comportement du système en **réseau de Pétri** ;
- exprimer des propriétés en **LTL** ;
- valider le comportement par **tests** et **analyse formelle**.

---

## Architecture

### Acteurs principaux

- `Avion`
- `TourDeControle`
- `Piste`

### États d’un avion

- `AvionEnVol`
- `AvionEnAttente`
- `AvionAutorise`
- `AvionAtterrissageEnCours`
- `AvionAuSol`

### États de la piste

- `PisteLibre`
- `PisteReservee`
- `PisteOccupee`

### Fonctionnement général

1. Un avion envoie une `DemandeAtterrissage`.
2. Si la piste est libre, la tour demande une réservation.
3. Une fois la réservation confirmée, l’avion reçoit `AutorisationAccordee`.
4. L’avion occupe la piste et effectue sa manœuvre.
5. À la fin, il envoie `FinAtterrissage`.
6. La piste est libérée.
7. Si d’autres avions attendent, la file FIFO est traitée.

---

## Propriétés vérifiées

Le projet s’appuie sur plusieurs propriétés métier importantes :

- **exclusion mutuelle** : deux avions ne peuvent jamais occuper la piste en même temps ;
- **unicité de l’état de la piste** : la piste est toujours dans exactement un état parmi `Libre`, `Réservée`, `Occupée` ;
- **pas d’atterrissage sans autorisation** ;
- **pas d’autorisation sans réservation préalable** ;
- **progression correcte vers l’état `AuSol`** ;
- **absence de deadlock non terminal** dans le scénario nominal.

---

## Réseau de Pétri et LTL

Le comportement du système est abstrait à l’aide d’un **réseau de Pétri** pour représenter les états critiques et les transitions du protocole d’atterrissage.

La **LTL** est utilisée comme langage de spécification pour formaliser des propriétés de :

- **sûreté** : quelque chose de mauvais ne doit jamais arriver ;
- **vivacité** : quelque chose de souhaité doit finir par arriver.

Le projet ne met pas en œuvre un model checker LTL générique, mais relie la LTL :
- au comportement Akka ;
- aux tests ;
- à l’analyse de l’espace d’états du réseau de Pétri.

---
