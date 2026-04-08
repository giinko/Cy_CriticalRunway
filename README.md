# ✈️ Akka Airport: Distributed Critical System Simulation

## 📋 Project Overview
This project is a high-reliability simulation of an airport landing system. It uses the **Akka Typed** framework and **Scala** to model a distributed environment where safety is paramount. 

To guarantee that the system is free of deadlocks and safety violations, the implementation is paired with a **Formal Verification** tool based on Petri Nets.

---

## 🏗️ System Architecture
The application is built using a decentralized actor-based architecture:

* **Avion (Plane)**: Manages its own state machine (In-flight, Waiting, Landing, Grounded).
* **Tour de Contrôle (Control Tower)**: Acts as the orchestrator, managing a FIFO (First-In-First-Out) queue to ensure fairness between aircraft.
* **Piste (Runway)**: A dedicated actor representing the critical resource. It ensures mutual exclusion by transitioning through `Free`, `Reserved`, and `Occupied` states.
* **Superviseur**: Ensures system resilience. It monitors the Tower and Runway, providing automatic recovery (Restart strategy) in case of unexpected failures.

---

## 🔍 Formal Verification & LTL Logic
We use **Linear Temporal Logic (LTL)** to define and verify the core properties of the system:

### Safety Properties
* **Mutual Exclusion**: No two planes can ever be on the runway at the same time.
    * `[] (runwayOccupied <= 1)`
* **State Integrity**: The runway must always be in exactly one valid state.

### Liveness Properties
* **No Starvation**: Every landing request is eventually granted.
    * `[] (Request -> <> Authorized)`
* **Termination**: Every authorized plane eventually reaches the "Grounded" state.

---

## 🛠️ Petri Net Analysis
The project includes a custom **State Space Explorer** (`AnalyseurPetri.scala`). This tool maps the Akka actor states to a formal Petri Net model to prove:
* **Boundedness**: The system has a finite number of states, preventing memory overflows.
* **Deadlock Freedom**: Every reachable state has a valid next transition until the simulation is complete.

---

## 🧪 Robustness & Testing
* **Real-time Simulation**: Landing maneuvers include realistic delays to expose potential concurrency issues.
* **Fault Tolerance**: Includes a "Crash Test" scenario to demonstrate the Supervisor's ability to restore the system state after a failure.
* **Unit Tests**: A comprehensive suite of Akka TestKit specs validates the FIFO logic and resource management.

---

## 🚀 Getting Started

### Prerequisites
* JDK 11+
* SBT (Scala Build Tool)

### Commands
* **Run Simulation**: `sbt run`
* **Run Tests**: `sbt test`
* **Run Formal Analysis**: `sbt "runMain verification.AnalyseurPetri"`
