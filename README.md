# BuildingStaff

## Optional Rebar/Pylon integration (1.0.35)

The same Java 21 plugin still runs on Minecraft 1.21.11 without either provider.
Rebar and Pylon remain optional and require a server version supported by their
own releases. The compiled provider baseline is Rebar 0.43.0-26.2.

Custom blocks are selected by provider identity, not just backing material. The
staff uses their exact pick item, including portable tank data, and charges only
matching storage inventory slots. Payment is reserved before provider callbacks.
Protection, research, placement, break and custom-drop events remain active.
A rejected placement is refunded only after confirmed rollback; a refused or
ambiguous rollback leaves the registered block intact and logs its location for
manual recovery rather than overwriting metadata or risking duplicate items.

Normal Slimefun item IDs, recipes, axis/projection data and vanilla placement are
retained. Provider classes are not shaded. This integration uses the existing
synchronous Bukkit lifecycle; it is not a Folia region-safety guarantee.

For source builds, run `bash scripts/install_rebar_api.sh` before `mvn clean verify`.
This checksum-verifies a published compile-only API; it does not install a server
plugin. The test suite covers transaction ordering with explicit doubles; real
provider/server results must be recorded separately before publication.
