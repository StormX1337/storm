Storm loads its UI font from this folder.

Drop a TrueType file named after the theme font here, for example:

    storm.ttf

The name maps directly onto Theme.font(), so "storm" -> storm.ttf.
When the file is missing the client falls back to a system sans serif and
then to the vanilla bitmap font, so a missing font never breaks the UI.

No font is shipped in this repository, pick one whose licence allows
redistribution before you publish a build.
