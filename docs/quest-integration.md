# Quest / progression integration (optional)

Khaos Nexus has no hard dependency on any quest mod and ships no quest chapters.

## Advancement trigger (always on)
`khaosnexus:hunter_event` with conditions `event`, optional `value`, optional `min`.

| event | value | min compares |
|---|---|---|
| gate_cleared | gate rank (e,d,c,b,a,s) | - |
| rank_up | new hunter rank | rank tier (E=0 … S=5) |
| class_selected | class id | - |
| bounty_completed | gates, raids, spire | - |
| raid_boss_killed | boss entity id | - |
| spire_floor | floor number | floor cleared |
| title_earned | title id | - |

Example `data/mypack/advancement/clear_s_gate.json`:
```json
{"criteria":{"c":{"trigger":"khaosnexus:hunter_event","conditions":{"event":"gate_cleared","value":"s"}}}}
```
Use it from FTB Quests with an Advancement task.

## FTB Quests stages (only when FTB Quests is installed and `compat.ftbQuests` is on)
Stages are granted as player tags, which FTB Library reads by default (Stage task):
`khaosnexus.gate_cleared`, `khaosnexus.gate_cleared.<rank>`, `khaosnexus.rank.<rank>`,
`khaosnexus.class_chosen`, `khaosnexus.class.<id>` (only the current class),
`khaosnexus.bounty`, `khaosnexus.bounty.<id>`, `khaosnexus.raid_boss`, `khaosnexus.raid_boss.<ns>.<path>`,
`khaosnexus.spire.<floor>` (every 5th floor), `khaosnexus.title.<id>`.
If a stage mod such as Game Stages replaces FTB Library's stage backend, use the advancement trigger instead.
