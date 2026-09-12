# SF_BuildingStaff

Building Staff for Slimefun Legacy, maintained for modern Paper servers with optional Rebar/Pylon compatibility.

## Compatibility

- **Slimefun Legacy:** required
- **Paper:** 26.2 target
- **Java:** 25
- **Rebar:** optional, tested against `0.43.0-26.2`
- **Pylon:** optional; supported through Rebar's custom block lifecycle

BuildingStaff continues to work normally when Rebar and Pylon are not installed.

## Rebar / Pylon support

When Rebar is available, BuildingStaff treats custom blocks as custom blocks instead of reducing them to their vanilla backing material.

### Building Staff

- Uses the Rebar/Pylon block's real pick item, including persistent item data.
- Preserves Rebar research/use checks and block placement hooks.
- Fires a normal `BlockPlaceEvent` with the physical backing block already present, matching Rebar's expected placement ordering.
- Consumes the exact matching custom item rather than an ordinary vanilla item with the same material.
- Keeps different Rebar/Pylon block types separate even when they share the same vanilla backing material.
- Supports stateful pick items such as Pylon Portable Fluid Tanks without stripping their stored state.
- Fails closed if Rebar compatibility cannot safely identify or place a custom block.

### Breaking Staff

Breaking Staff continues to use the standard `BlockBreakEvent` lifecycle. Rebar handles its custom block cleanup and drops through that event before BuildingStaff clears the physical backing block. This preserves Rebar/Pylon machine inventories, special drops, and associated display entities.

### Projection preview

The placement preview understands Rebar/Pylon targets and counts the exact custom placement item rather than ordinary items sharing the same material.

## Safety notes

- Rebar and Pylon are soft dependencies, not required dependencies.
- Strict-mode surface matching compares Rebar block identity, not just Bukkit `Material`.
- Protection plugins still receive the normal Bukkit placement/break events used by the staff.
- Cancelled custom placements are rolled back through Rebar. If Rebar vetoes that rollback, BuildingStaff preserves the registered custom block rather than restoring a vanilla block over live Rebar metadata.

## Build

```bash
mvn clean package
```

The generated plugin JAR is named `SF_BuildingStaff<version>.jar`.
