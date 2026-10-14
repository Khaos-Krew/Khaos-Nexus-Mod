"""Bake freeze_<pose> review animations (DebugPose) for the 1.1 bosses. Run after their anims are written."""
import sys
import rigs_saga2 as S
POSES = {
    'rift_warden': {'idle_combat': ('idle_combat', 0.0), 'draw': ('draw', 0.75), 'attack_windup': ('attack', 0.45), 'attack': ('attack', 0.7),
                    'slam': ('slam', 0.9), 'lunge': ('lunge', 0.55), 'roar': ('roar', 1.0)},
    'spire_guardian': {'idle_combat': ('idle_combat', 0.0), 'attack_windup': ('attack', 0.35), 'attack': ('attack', 0.6), 'sweep': ('sweep', 0.6),
                       'spin': ('spin', 0.7), 'barrier': ('barrier', 0.6), 'roar': ('roar', 1.0)},
}
def run(name, poses=None):
    old = S.FREEZE; S.FREEZE = poses or POSES[name]
    try: S.bake_freezes(name)
    finally: S.FREEZE = old
if __name__ == '__main__':
    for n in sys.argv[1:] or POSES: run(n)
