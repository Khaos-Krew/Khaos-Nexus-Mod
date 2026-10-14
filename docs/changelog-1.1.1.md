# Khaos Nexus 1.1.1

## Safe graves
- Dying inside a gate dungeon no longer leaves your items inside. They're moved to a grave chest at the gate entrance you came through, even if the gate closed while you were dead.
- Leaving or dying in the Spire now returns you to where you entered it instead of world spawn (Spire deaths already keep your items).
- Blocked entrance? The grave goes to the nearest safe spot within 8 blocks (solid floor, no liquid, no void).
- No safe spot, or the entry dimension is gone? Your items go to your Recovery Chest. Claim them with `/khaos recover`, from the Spire town or anywhere. Items are never deleted, even if the server crashes mid-death.
- Party deaths each get their own grave. Graves are locked to the owner and their party for 60 minutes, then open to anyone.
- Large inventories spill into extra grave chests next to the first.
- Items are captured before grave mods (Gravestone Mod, YIGD, etc.) see them, so no grave is made inside the rift. Works with no grave mod installed.
- Config `[safeGraves]`: `enabled`, `searchRadius`, `partyAccessMinutes`.

## Naming
- The top Hunter Tome tier is now the **Zenith Hunter Tome** (item ID unchanged, existing worlds are fine).
