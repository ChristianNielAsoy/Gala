# Gala App

## Project Summary Document (Consolidated)

------------------------------------------------------------------------

# 1. Project Vision

**Gala** is a mobile-first travel planning and expense management
application designed for Filipino friend groups ("barkada").

It centralizes: - Trip planning - Shared expense tracking - Advanced
split logic - Settlement computation - Collaborative coordination

The app is built around real Filipino social dynamics such as: -
"Kontri" culture - Custom splits - Libre items - Fair but flexible
settlement logic

------------------------------------------------------------------------

# 2. Core Problem

Existing expense apps: - Are too generic - Don't reflect Filipino group
behavior - Lack item-level splits - Don't support real barkada
workflows - Feel too financial and not social

Gala solves this by combining: - Trip coordination - Social
collaboration - Flexible expense computation - Clean UX - Offline-first
architecture - Future cloud sync

------------------------------------------------------------------------

# 3. System Goals

## Primary Goals

-   Accuracy in expense computation
-   Fair and transparent settlement
-   Easy trip coordination
-   Real-time collaboration (future-ready)
-   Beginner-friendly architecture
-   Offline-first reliability

## Long-Term Goals

-   Firebase multi-device sync
-   International trip support (multi-currency)
-   Scalable architecture
-   Industry-standard documentation (SDD/PRD style)

------------------------------------------------------------------------

# 4. Project Objectives

## General Objective

Develop an Android-based collaborative trip planner and expense tracker
using:

-   Modern Android Architecture
-   Room (offline database)
-   Repository pattern
-   Future Firebase Authentication + Firestore integration

------------------------------------------------------------------------

## Specific Objectives

-   Implement advanced expense splitting logic
-   Support equal and custom splits
-   Support item-based allocation
-   Enable settlement computation (who owes who)
-   Provide collaborative trip editing
-   Maintain clean navigation structure
-   Ensure modular and scalable codebase
-   Follow Git-based workflow

------------------------------------------------------------------------

# 5. Target Users

### 1. The Planner

-   Manages itinerary
-   Coordinates members
-   Uploads documents

### 2. Passive Member

-   Views details
-   Occasionally adds items
-   Relies on system calculations

------------------------------------------------------------------------

# 6. Core Feature Modules

------------------------------------------------------------------------

## 6.1 Dashboard (Home)

Purpose: Overview & Insights

Features: - Trip cards (Past / Ongoing / Upcoming) - Calendar view -
Budget snapshot - Quick totals - Activity feed - Filters by status/month

------------------------------------------------------------------------

## 6.2 Trip Planning

Purpose: Trip setup and coordination

Features: - Create trip (title, date, location) - Budget goal - Cover
photo - Member assignment - Itinerary builder - Shared checklist - Trip
notes - Packing list - Documents vault

------------------------------------------------------------------------

## 6.3 People & Collaboration

Purpose: Member management

Features: - Member directory - TripMember roles - Avatar/profile
support - Google Sign-in (Firebase future) - Cloud-ready ID system

------------------------------------------------------------------------

## 6.4 Expense Management (Core Engine)

Purpose: Advanced shared expense computation

### Expense Capabilities:

-   Add expense
-   Category tagging
-   Receipt attachment
-   GPS tagging
-   Multi-currency (manual exchange rate)
-   Expense filters
-   Tags
-   Reactions & comments

------------------------------------------------------------------------

### Advanced Split Logic (Key Differentiator)

Supports:

1.  Equal Split
2.  Custom Split
3.  Item-Based Split
4.  Libre items
5.  Per-item consumer assignment
6.  Mixed split logic per expense

Entities involved:

-   Expense
-   ExpenseItem
-   ExpenseItemShare
-   ExpenseShare

------------------------------------------------------------------------

## 6.5 Settlement Module

Purpose: Calculate final balances

Features: - Automatic "Who owes who" computation - Minimized transaction
logic - Manual payment logging - Payment proof - Real-time recalculation

------------------------------------------------------------------------

## 6.6 Timeline & Analytics

Purpose: Visualization & insights

Features: - Chronological feed - Expense breakdown chart - Average spend
per member - Trip narrative view

------------------------------------------------------------------------

## 6.7 Notifications & System

-   In-app notifications
-   Payment reminders
-   Dark mode
-   Export / Import data

------------------------------------------------------------------------

# 7. Navigation Structure

Bottom Navigation (5 Tabs):

\[ Home \| Trips \| Expenses \| People \| Settings \]

### Trips Flow

Trips → Trip Details →\
Tabs inside Trip: - Members - Overview - Expenses - Settlement

------------------------------------------------------------------------

# 8. Architecture Overview

Layered Clean Architecture:

UI Layer\
→ ViewModels\
→ Repository\
→ Local Database (Room)\
↘ Future Firebase Sync

------------------------------------------------------------------------

## Architectural Principles

-   Separation of concerns
-   Offline-first
-   Repository mediator pattern
-   Eventual cloud consistency
-   Modular scalability

------------------------------------------------------------------------

# 9. Data Model (Core Entities)

-   Trip
-   Member
-   TripMember
-   Expense
-   ExpenseItem
-   ExpenseItemShare
-   ExpenseShare
-   ItineraryEvent
-   PackingItem
-   Document
-   Reaction
-   Comment

------------------------------------------------------------------------

# 10. Key User Flows

### Create Trip

1.  Tap +Trip
2.  Enter details
3.  Add members
4.  Save

### Add Expense

1.  Open Trip
2.  Add expense
3.  Add items (optional)
4.  Assign consumers
5.  Attach receipt
6.  Save → recalculation

### Settlement

1.  Open Settlement tab
2.  View computed balances
3.  Log payments (optional)

------------------------------------------------------------------------

# 11. UX Design Principles

-   Everything important within 2 taps
-   Clean hierarchy
-   Avoid overload
-   Powerful but simple editor
-   Consistent UI patterns
-   Mobile-first thinking

------------------------------------------------------------------------

# 12. Future Cloud Integration Plan

-   Firebase Authentication
-   Firestore sync
-   Real-time updates
-   Global TripMember IDs
-   Multi-device collaboration
-   Offline-first remains primary

------------------------------------------------------------------------

# 13. What Makes Gala Unique

-   Filipino barkada-focused
-   Advanced split logic (item-level)
-   Libre support
-   Hybrid social + financial design
-   Clean mobile UX
-   Offline-first with future cloud
-   Modular professional architecture
