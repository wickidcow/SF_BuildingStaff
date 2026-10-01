#!/usr/bin/env python3
"""Protect existing item/recipe definitions and the custom lifecycle entry points."""
from pathlib import Path
import hashlib
root=Path(__file__).resolve().parents[1]
def blob(data):return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
EXPECTED={'src/main/java/com/balugaq/buildingstaff/core/managers/StaffSetup.java': '295c7a9bd46cfa694bb626f946f5e2dea2a4c3bc', 'src/main/java/com/balugaq/buildingstaff/core/listeners/StaffModeSwitchListener.java': 'd89cc6599cd8c8ea104b1158014ba279bedbb23c', 'src/main/java/com/balugaq/buildingstaff/utils/WorldUtils.java': '527c9df956b60e345885e5f5b4f843244d17c499'}

for name,expected in EXPECTED.items():
    assert blob((root/name).read_bytes())==expected,("Preserved source changed; review this contract",name)
source=(root/'src/main/java/com/balugaq/buildingstaff/compat/CustomBlockPlacement.java').read_text()
for required in ("getStorageContents()", "current.isSimilar(one)", "new BlockPlaceEvent", "new BlockBreakEvent", "Interaction.PLACE_BLOCK", "Interaction.BREAK_BLOCK", "previous.update(true,false)", "BlockCompatibility.UNAVAILABLE"):
    assert required in source,required
assert "breakNaturally(" not in source
bridge=(root/'src/main/java/com/balugaq/buildingstaff/compat/RebarCompatibility.java').read_text()
assert "current!=expected" in bridge and "drops.isEmpty()" in bridge
assert "getPickItem(player)" in bridge and "getKey().equals" in bridge
entry=(root/'src/main/java/com/balugaq/buildingstaff/api/items/BuildingStaff.java').read_text()
assert entry.index("CustomBlockPlacement.placeBatch")<entry.index("int playerHas = 0;")
assert "player.getInventory().removeItem(new ItemStack(material, consumed))" in entry
preview=(root/'src/main/java/com/balugaq/buildingstaff/core/listeners/PrepareBuildingListener.java').read_text()
assert "setMetadata(" not in preview and "PersistentDataType" in preview
print('BuildingStaff provider and retained-definition guards passed')
