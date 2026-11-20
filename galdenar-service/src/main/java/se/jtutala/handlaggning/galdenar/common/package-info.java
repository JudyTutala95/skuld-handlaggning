/// Teknisk infrastruktur för tjänsten: felhantering (RFC 9457 Problem Details), klocka och
/// undantag.
///
/// Samma kod finns i varje tjänst. Det är **medvetet**: ett delat bibliotek skulle koppla
/// ihop tjänsterna så att de måste uppdateras och släppas samtidigt. Lite dubblerad
/// infrastruktur är priset för att tjänsterna är helt fristående.
@NullMarked
package se.jtutala.handlaggning.galdenar.common;

import org.jspecify.annotations.NullMarked;
