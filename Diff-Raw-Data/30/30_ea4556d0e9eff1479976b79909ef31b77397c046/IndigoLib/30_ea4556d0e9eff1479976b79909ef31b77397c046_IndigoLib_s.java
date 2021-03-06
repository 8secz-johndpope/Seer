 /****************************************************************************
  * Copyright (C) 2011 GGA Software Services LLC
  *
  * This file is part of Indigo toolkit.
  *
  * This file may be distributed and/or modified under the terms of the
  * GNU General Public License version 3 as published by the Free Software
  * Foundation and appearing in the file LICENSE.GPL included in the
  * packaging of this file.
  *
  * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
  * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
  ***************************************************************************/
 
 package com.ggasoftware.indigo;
 
 import com.sun.jna.*;
 import com.sun.jna.ptr.*;
 
 public interface IndigoLib extends Library
 {
    String indigoVersion ();
    long indigoAllocSessionId ();
    void indigoSetSessionId (long id);
    void indigoReleaseSessionId (long id);
    String indigoGetLastError ();
 
    int indigoFree (int handle);
    int indigoClone (int handle);
    int indigoCountReferences ();
    int indigoSetOption (String name, String value);
    int indigoSetOptionInt (String name, int value);
    int indigoSetOptionBool (String name, int value);
    int indigoSetOptionFloat (String name, float value);
    int indigoSetOptionColor (String name, float r, float g, float b);
    int indigoSetOptionXY (String name, int x, int y);
 
    int indigoReadFile (String filename);
    int indigoLoadString (String str);
    int indigoLoadBuffer (byte[] buf, int size);
 
    int indigoWriteFile (String filename);
    int indigoWriteBuffer ();
    int indigoClose (int handle);
 
    int indigoNext (int iter);
    int indigoHasNext (int iter);
    int indigoIndex (int item);
    int indigoRemove (int item);
 
    int indigoCreateMolecule ();
    int indigoCreateQueryMolecule ();
 
    int indigoLoadMolecule  (int source);
    int indigoLoadMoleculeFromString (String str);
    int indigoLoadMoleculeFromFile   (String filename);
    int indigoLoadMoleculeFromBuffer (byte[] buffer, int size);
 
    int indigoLoadQueryMolecule  (int source);
    int indigoLoadQueryMoleculeFromString (String str);
    int indigoLoadQueryMoleculeFromFile   (String filename);
    int indigoLoadQueryMoleculeFromBuffer (byte[] buffer, int size);
 
    int indigoLoadSmarts  (int source);
    int indigoLoadSmartsFromString (String str);
    int indigoLoadSmartsFromFile   (String filename);
    int indigoLoadSmartsFromBuffer (byte[] buffer, int size);
 
    int indigoSaveMolfile (int molecule, int output);
    int indigoSaveMolfileToFile (int molecule, String filename);
    Pointer indigoMolfile (int molecule);
 
    int indigoSaveCml (int object, int output);
    int indigoSaveCmlToFile (int object, String filename);
    Pointer indigoCml (int object);
 
    int indigoSaveMDLCT (int item, int output);
 
    int indigoLoadReaction  (int source);
    int indigoLoadReactionFromString (String string);
    int indigoLoadReactionFromFile   (String filename);
    int indigoLoadReactionFromBuffer (byte[] buf, int size);
 
    int indigoLoadQueryReaction (int source);
    int indigoLoadQueryReactionFromString (String str);
    int indigoLoadQueryReactionFromFile   (String filename);
    int indigoLoadQueryReactionFromBuffer (byte[] buf, int size);
 
    int indigoCreateReaction ();
    int indigoCreateQueryReaction ();
 
    int indigoAddReactant (int reaction, int molecule);
    int indigoAddProduct  (int reaction, int molecule);
 
    int indigoCountReactants (int reaction);
    int indigoCountProducts  (int reaction);
    int indigoCountMolecules (int reaction);
    int indigoIterateReactants (int reaction);
    int indigoIterateProducts  (int reaction);
    int indigoIterateMolecules (int reaction);
 
    int indigoSaveRxnfile (int reaction, int output);
    int indigoSaveRxnfileToFile (int reaction, String filename);
    Pointer indigoRxnfile (int reaction);
 
    int indigoAutomap (int reaction, String mode);
 
    int indigoIterateAtoms (int molecule);
    int indigoIteratePseudoatoms (int molecule);
    int indigoIterateRSites (int molecule);
    int indigoIterateStereocenters (int molecule);
    int indigoIterateRGroups (int molecule);
    int indigoIsPseudoatom (int atom);
    int indigoIsRSite (int atom);
 
    int indigoStereocenterType (int atom);
    int indigoSingleAllowedRGroup (int rsite);
 
    int indigoIterateRGroupFragments (int rgroup);
    int indigoCountAttachmentPoints (int rgroup);
 
    Pointer indigoSymbol (int atom);
    int indigoDegree (int atom);
 
    int indigoGetCharge (int atom, IntByReference charge);
    int indigoGetExplicitValence (int atom, IntByReference valence);
    int indigoGetRadicalElectrons (int atom, IntByReference electrons);
    int indigoAtomicNumber (int atom);
    int indigoIsotope (int atom);
    int indigoValence (int atom);
    int indigoCountHydrogens (int atom, IntByReference valence);
    int indigoCountImplicitHydrogens (int item);
 
    Pointer indigoXYZ (int atom);
    int indigoSetXYZ (int atom, float x, float y, float z);
 
    int indigoCountSuperatoms (int molecule);
    int indigoCountDataSGroups (int molecule);
    int indigoCountRepeatingUnits (int molecule);
    int indigoCountMultipleGroups (int molecule);
    int indigoCountGenericSGroups (int molecule);
    int indigoIterateDataSGroups (int molecule);
    int indigoIterateSuperatoms (int molecule);
    int indigoIterateGenericSGroups (int molecule);
    int indigoIterateRepeatingUnits (int molecule);
    int indigoIterateMultipleGroups (int molecule);
    int indigoGetSuperatom (int molecule, int index);
    int indigoGetDataSGroup (int molecule, int index);
    Pointer indigoDescription (int data_sgroup);
 
    int indigoAddDataSGroup (int molecule, int natoms, int atoms[],
         int nbonds, int bonds[], String description, String data);
 
    int indigoSetDataSGroupXY (int sgroup, float x, float y, String options);
 
    int indigoResetCharge (int atom);
    int indigoResetExplicitValence (int atom);
    int indigoResetRadical (int atom);
    int indigoResetIsotope (int atom);
 
    int indigoSetAttachmentPoint (int atom, int order);
 
    int indigoRemoveConstraints  (int item, String type);
    int indigoAddConstraint    (int item, String type, String value);
    int indigoAddConstraintNot (int item, String type, String value);
 
    int indigoResetStereo (int item);
    int indigoInvertStereo (int item);
 
    int indigoCountAtoms (int molecule);
    int indigoCountBonds (int molecule);
    int indigoCountPseudoatoms (int molecule);
    int indigoCountRSites (int molecule);
 
    int indigoIterateBonds (int molecule);
    int indigoBondOrder  (int bond);
 
    int indigoBondStereo (int bond);
    int indigoTopology (int bond);
    int indigoIterateNeighbors (int atom);
    int indigoBond (int nei);
    int indigoGetAtom (int molecule, int idx);
    int indigoGetBond (int molecule, int idx);
 
    int indigoSource (int bond);
    int indigoDestination (int bond);
    int indigoClearCisTrans (int handle);
    int indigoClearStereocenters (int handle);
    int indigoCountStereocenters (int molecule);
 
    int indigoResetSymmetricCisTrans (int handle);
    int indigoMarkEitherCisTrans (int handle);
 
    int indigoAddAtom (int molecule, String symbol);
 
    int indigoSetCharge (int atom, int charge);
    int indigoSetIsotope (int atom, int isotope);
 
    int indigoAddBond (int source, int destination, int order);
    int indigoSetBondOrder (int bond, int order);
 
    int indigoMerge (int where_to, int what);
 
    int indigoHighlight (int atom);
    int indigoUnhighlight (int item);
 
    int indigoCountComponents (int molecule);
    int indigoComponentIndex (int atom);
    int indigoIterateComponents (int molecule);
 
    int indigoComponent (int molecule, int index);
 
    int indigoCountSSSR (int molecule);
    int indigoIterateSSSR (int molecule);
 
    int indigoIterateSubtrees (int molecule, int min_atoms, int max_atoms);
    int indigoIterateRings (int molecule, int min_atoms, int max_atoms);
    int indigoIterateEdgeSubmolecules (int molecule, int min_bonds, int max_bonds);
 
    int   indigoCountHeavyAtoms (int molecule);
    int   indigoGrossFormula    (int molecule);
    float indigoMolecularWeight (int molecule);
    float indigoMostAbundantMass (int molecule);
    float indigoMonoisotopicMass (int molecule);
 
    Pointer indigoCanonicalSmiles (int molecule);
    Pointer indigoLayeredCode (int molecule);
 
    int indigoHasCoord (int molecule);
    int indigoHasZCoord (int molecule);
    int indigoIsChiral (int molecule);
 
    int indigoCreateSubmolecule (int molecule, int nvertices, int vertices[]);
    int indigoCreateEdgeSubmolecule (int molecule, int nvertices, int vertices[], int nedges, int edges[]);
 
    int indigoRemoveAtoms (int molecule, int nvertices, int vertices[]);
    float indigoAlignAtoms (int molecule, int natoms, int atom_ids[], float desired_xyz[]);
 
    int indigoAromatize (int item);
    int indigoDearomatize (int item);
 
    int indigoFoldHydrogens (int item);
    int indigoUnfoldHydrogens (int item);
 
    int indigoLayout (int object);
 
    Pointer indigoSmiles (int item);
 
    int indigoExactMatch (int item1, int item2);
 
    Pointer indigoName (int handle);
    int indigoSetName (int handle, String name);
 
    int indigoSerialize (int handle, PointerByReference buf, IntByReference size);
 
    int indigoUnserialize (byte[] buf, int size);
 
    int indigoHasProperty (int handle, String prop);
    Pointer indigoGetProperty (int handle, String prop);
 
    int indigoSetProperty (int item, String prop, String value);
    int indigoRemoveProperty (int item, String prop);
    int indigoIterateProperties (int handle);
    int indigoClearProperties (int handle);
 
    Pointer indigoCheckBadValence (int handle);
    Pointer indigoCheckAmbiguousH (int handle);
    int indigoFingerprint (int item, String type);
    int indigoCountBits (int fingerprint);
    int indigoCommonBits (int fingerprint1, int fingerprint2);
    float indigoSimilarity (int item1, int item2, String metrics);
 
    int indigoIterateSDF    (int reader);
    int indigoIterateRDF    (int reader);
    int indigoIterateSmiles (int reader);
    int indigoIterateCML    (int reader);
 
    int indigoIterateSDFile     (String filename);
    int indigoIterateRDFile     (String filename);
    int indigoIterateSmilesFile (String filename);
    int indigoIterateCMLFile    (String filename);
 
    Pointer indigoRawData (int item);
    int indigoTell (int handle);
    int indigoSdfAppend (int output, int item);
    int indigoSmilesAppend (int output, int item);
    int indigoRdfHeader (int output);
    int indigoRdfAppend (int output, int item);
 
    int indigoCmlHeader (int output);
    int indigoCmlAppend (int output, int item);
    int indigoCmlFooter (int output);
 
    int indigoCreateArray ();
    int indigoArrayAdd (int arr, int object);
    int indigoAt (int item, int index);
    int indigoCount (int item);
    int indigoClear (int arr);
    int indigoIterateArray (int arr);
 
    int indigoSubstructureMatcher (int target, String mode);
    int indigoIgnoreAtom (int matcher, int atom);
    int indigoUnignoreAtom (int matcher, int atom);
    int indigoUnignoreAllAtoms (int matcher);
    int indigoMatch (int matcher, int query);
    int indigoCountMatches (int matcher, int query);
    int indigoIterateMatches (int matcher, int query);
    int indigoHighlightedTarget (int match);
    int indigoMapAtom (int match, int query_atom);
    int indigoMapBond (int match, int query_bond);
 
    int indigoExtractCommonScaffold (int structures, String options);
    int indigoAllScaffolds (int extracted);
    int indigoDecomposeMolecules (int scaffold, int structures);
    int indigoDecomposedMoleculeScaffold (int decomp);
    int indigoIterateDecomposedMolecules (int decomp);
    int indigoDecomposedMoleculeHighlighted (int decomp);
    int indigoDecomposedMoleculeWithRGroups (int decomp);
    Pointer indigoToString (int handle);
    int indigoToBuffer (int handle, PointerByReference buf, IntByReference size);
    int indigoReactionProductEnumerate (int reaction, int monomers);
 }
