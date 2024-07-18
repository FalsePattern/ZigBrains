## What is this?
This document describes the relationships between the different "modules" of ZigBrains.

These relationships are strictly enforced, and modules that do not directly (or indirectly) depend on another
module cannot access their classes.

The primary purpose of this strictly enforced module system is to avoid code compatible with open source IDEs (IDEA/PyCharm Community)
from depending on code that only works on proprietary IDEs (IDEA Ultimate/CLion/...).

NOTE: These "modules" are in no way related to the module system introduced in Java 9. They're just called the same.

The suffix after the module name signifies which IDE it depends on.

- IC: IDEA Community
- CL: CLion

IC modules MUST NOT depend on CL modules, as this violates the restrictions set forth above.

## Modules

### Common (IC)

### Zig (IC)
- Common (IC)

### Project (IC)
- Common (IC)
- Zig (IC)

### Zon (IC)
- Common (IC)

### Debugger (IU/CL)
- Common (IC)
- Zig (IC)
- Project (IC)