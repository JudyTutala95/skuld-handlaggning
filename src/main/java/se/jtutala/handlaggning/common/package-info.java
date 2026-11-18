/// Delad modul med teknisk infrastruktur (felhantering, klocka).
///
/// `OPEN` betyder att alla andra moduler får använda allt här.
@ApplicationModule(type = ApplicationModule.Type.OPEN)
@NullMarked
package se.jtutala.handlaggning.common;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
