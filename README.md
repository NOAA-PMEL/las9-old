# Live Access Serever 9 (repository: las9)

We are working on a redesign of LAS. Both the frontend JavaScript client and the server-side code are being rebuild.

The design goals include:

1. All configuration and management done using an Admin Console UI provide with LAS. Admin functions include:
      1. Add, remove, temporarily disable a data set
      2. Rearrange the heirarchy of how data sets are displayed in the UI
      3. Automatically check data sets for new time steps on a schedule set in the UI
      4. Incorporate interactive client-side plotting capability
      5. Simple deployment using a single war file (including F-TDS).
      6. Dynamic data set configuration where appropriate (including on background thread).
      7. ~~Multiple sites deployable from a single instance.~~
2. Easy rest URL's for basic plots (useful for thumbnails and HTML image references).
3. Simplified client-server interaction using JSON messages automatically marshalled to and from Java.
4. Nicely styled, modern client UI look-and-feel.
5. Simple upgrade process (save the persistence database and drop in the new war file).

*Certain information set forth in this presentation contains “forward-looking information”. Except for statements of historical fact, information contained herein constitutes forward-looking statements and includes, but is not limited to, the expected development of the projects described. Forward-looking statements are provided to allow potential users the opportunity to understand our beliefs and opinions in respect of the future so that they may use such beliefs and opinions as one factor in evaluating an LAS.*

*These statements are not guarantees of future performance and undue reliance should not be placed on them. Such forward-looking statements necessarily involve known and unknown risks and uncertainties, which may cause actual performance to differ materially from any projections.*

*Although forward-looking statements contained in this presentation are based upon what management believes are reasonable assumptions, there can be no assurance that forward-looking statements will prove to be accurate, as actual results and future events could differ materially from those anticipated in such statements.*
