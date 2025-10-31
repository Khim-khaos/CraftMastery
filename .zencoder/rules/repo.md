---
description: Repository Information Overview
alwaysApply: true
---

# CraftMastery Information

## Summary
CraftMastery is a Minecraft 1.12.2 mod that adds a flexible crafting management system with customizable tabs, experience progression, and permission controls. It allows players to unlock recipes progressively, organize them into thematic tabs, and earn experience points through various in-game activities.

## Structure
- **src/main/java**: Core Java source code organized in packages
- **src/main/resources**: Configuration files, assets, and localization
- **gradle**: Build scripts and configuration helpers
- **run**: Runtime environment for testing the mod

## Language & Runtime
**Language**: Java
**Version**: Java 8 (with optional modern Java syntax support)
**Build System**: Gradle with RetroFuturaGradle
**Package Manager**: Gradle/Maven

## Dependencies
**Main Dependencies**:
- Minecraft Forge 1.12.2 (14.23.5.2860 recommended)
- Just Enough Items (JEI) - optional but recommended

**Development Dependencies**:
- RetroFuturaGradle (1.4.1)
- JetBrains Changelog (2.2.1)
- Gradle Plugin Idea Ext (1.1.9)

## Build & Installation
```bash
./gradlew build
```
Installation requires copying the built JAR from `build/libs/craftmastery-1.0.0.jar` to the Minecraft `mods` folder.

## Testing
**Framework**: JUnit Jupiter (optional)
**Test Location**: src/test
**Run Command**:
```bash
./gradlew test
```

## Main Components

### Experience System
- Independent experience system separate from vanilla Minecraft
- Multiple experience sources: block mining, crafting, mob kills, player kills
- Experience conversion to different point types
- Configurable multipliers and conversion ratios

### Recipe Management
- Recipe locking system with progressive unlocking
- Recipe dependencies and requirements
- Recipe tagging and categorization
- Integration with JEI for recipe visibility

### Tab System
- Organization of recipes into thematic tabs
- Tab relationships (blocking/unblocking)
- Customizable requirements for learning tabs
- Tab reset functionality

### Permission System
- Flexible permission settings for players and groups
- Different access levels: players, operators, administrators
- Configuration through commands or config files
- Permission-based feature access control

### GUI Interface
- Book-style interface for recipe browsing
- Visual dependency map for recipes
- Panels showing current points and progress
- Search and filtering capabilities

## Configuration
**Main Config**: config/craftmastery/craftmastery.cfg
**Recipe Config**: config/craftmastery/vanilla_recipes.json
**Key Settings**: Configurable keybindings (default 'G' to open interface)