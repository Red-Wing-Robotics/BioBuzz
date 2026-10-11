# opmodes/starterbot/

OpModes for the team's **goBILDA StarterBot**, a different robot from the competition bot: a
differential drive, one launcher motor, an intake motor and continuous-rotation servos. It has its
own Robot Controller configuration, so these OpModes crash at INIT on the competition robot, and
the competition OpModes crash on the StarterBot.

| OpMode | Group | Notes |
|---|---|---|
| `StarterBot: TeleOp` | starterbot | goBILDA's sample TeleOp, adapted |

- **Config names** are in `StarterBotHardware`, not `RobotHardware`, which holds only the
  competition robot's names. Change a name there and in the StarterBot's RC config together.
- **Group `"starterbot"`** and a `StarterBot:` name prefix, so drivers never pick one by mistake
  from the Driver Station list. Use both for any new StarterBot OpMode.
- These OpModes don't follow the competition code's conventions (engine classes, control
  schemes, subsystems). They're a sample for a practice robot; keep them separate.
