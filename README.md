# Ascendant Arms

![Java CI with Maven](https://github.com/hatimc21/AscendantArms/actions/workflows/maven.yml/badge.svg)

**Ascendant Arms** is a powerful and engaging Minecraft plugin that introduces a framework for legendary, evolving weapons. Each of the seven unique "Mythic Weapons" has its own identity, theme, and a deep mastery system that unlocks new abilities as players level them up. This plugin is designed to provide long-term goals and epic rewards for players on any Survival Multiplayer (SMP) server.

## Core Features
- **7 Unique Mythic Weapons:** From the teleporting Rift Blade to the explosive Sunfire Bow, each weapon offers a completely different playstyle.
- **Deep Mastery System:** Weapons level up from 1 to 7 by gaining Mastery XP from mob kills.
- **Ascension Mechanic:** To finalize a level up, players must sacrifice their own experience levels in a powerful ritual (`/mythic ascend`), creating a meaningful progression choice.
- **Dynamic Abilities:** Spells and passives grow stronger, have shorter cooldowns, and unlock entirely new effects at higher mastery levels.
- **Professional UI:** A custom Boss Bar appears when holding a Mythic Weapon, displaying its name, mastery level, and XP progress in real-time.
- **Admin Friendly:** Fully featured admin commands allow for easy spawning (`/mythic give`) and level setting (`/mythic setlevel`) for testing and rewards.

---

## The Armory
| Weapon Name        | Base Item         | Role                    | Primary Mechanic      |
| ------------------ | ----------------- | ----------------------- | --------------------- |
| **Rift Blade**     | Netherite Sword   | Single-Target Assassin  | Teleportation         |
| **Sunfire Bow**    | Bow               | Ranged Artillery (AoE)  | Explosions & Fire     |
| **Tidal Trident**  | Trident           | Support & Crowd Control | Water & Healing       |
| **Earthshatter Axe** | Netherite Axe     | Heavy Brawler           | Shockwaves & Defense  |
| **Cyclone Crossbow** | Crossbow          | Ranged DPS              | Speed & Piercing      |
| **Shadow Daggers** | Netherite Shovel  | Debuff Specialist       | Poison & Invisibility |
| **Soulscythe**     | Netherite Hoe     | Self-Sustaining Warlock | Lifesteal & DoT       |

---

## Commands & Permissions
| Command                         | Description                                | Permission               |
| ------------------------------- | ------------------------------------------ | ------------------------ |
| `/mythic give <weapon_id>`      | Spawns a Mythic Weapon.                    | `ascendantarms.admin`    |
| `/mythic ascend`                | Ascends your held weapon to the next level.| `(None)`                 |
| `/mythic setlevel <level>`      | Sets the level of your held weapon.        | `ascendantarms.admin`    |

## Installation
1. Download the latest `.jar` file from the [**Releases page**](https://github.com/hatimc21/AscendantArms/releases).
2. Place the `AscendantArms.jar` file into your server's `/plugins` folder.
3. Restart or reload your server.
