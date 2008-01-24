/*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
* UnitAutoChooser.java
*
* Created on July 29, 2007, 7:32 PM
*/

package games.strategy.triplea.util;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author: Tony Clayton
 * @version: 1.0
 */

public class UnitAutoChooser
{
    // m_allUnits: 
    // Set through the constructor and contains all available units for 
    // determining the solution.
    // The order of these units is preserved throughout the solver so 
    // the order of the units affects the order of the solutions found.
    private final Collection<Unit> m_allUnits;

    // m_chosenUnits: 
    // Set through the constructor and contains all chosen units.
    private final Collection<Unit> m_chosenUnits;

    // m_candidateUnits: 
    // Determined by solver and set to all units that match a chosen unit
    // category.  
    // For dependent units, all dependents of units that match
    // a chosen unit category are included, regardless of whether the 
    // dependent itself matched a chosen unit category.
    private final Set<Unit> m_candidateUnits;


    // m_bCategorizeMovement: 
    // Set to true through constructor if movement should be categorized.
    private final boolean m_bCategorizeMovement;

    // m_bAllowImplicitDependents: 
    // Set to true through constructor if chosenUnits is not required
    // to include all dependent units.  If dependent units ARE included,
    // then they will still effect the solution filtering.
    private final boolean m_bAllowImplicitDependents;


    // m_dependentsMap: 
    // Set to the Map of unit->dependents for all relevant units
    private final Map<Unit, Collection<Unit>> m_dependentsMap;


    // SOLUTION LINGO:
    // There are three types of solutions the solver may find, in terms of the 
    // completedness of the solution:
    // 1. EXACT SOLUTION            : All chosen units were found and no other units
    //                                (ie: dependents) were included.
    // 2. GREEDY SOLUTION           : All chosen units are accounted for, but some 
    //                                unchosen dependents are also included 
    // 3. INCOMPLETE SOLUTION       : Not all chosen units are accounted for.
    //
    // There are also three ways in which a solution can be found:
    // 1. COMPOSITE SOLUTION        : Dependent units were involved, and at least
    //                                one solution was found containing composite categories.
    // 2. INDEPENDENT SOLUTION      : No dependent units were involved, and a
    //                                solution was found (all units are independent)
    // 3. SIMPLE SOLUTION           : No composite or independent solution could be found,
    //                                so a simple solution is found by searching the sorted
    //                                units for each chosen category as if they are all independent. 
    //                                The resulting solution will usually be a greedy one. 

    // m_bFoundCompleteSolution:
    // set to true if at least one complete solution (exact or greedy) was found
    private boolean m_bFoundCompleteSolution = false;

    // m_selectedUnitSolutions:
    // Determined by solver and contains all exact 
    // solutions, followed by all greedy solutions.
    // If no exact or greedy solutions are found, then 
    // it contains the best partial solution.
    private final List<Set<Unit>> m_selectedUnitSolutions;

    // m_exactSolutionCount:
    // Determined by solver and is set to the number of exact solutions found.
    private int m_exactSolutionCount = 0;

    // m_chosenCategoryCounts:
    // Determined by solver and contains a count of all chosen categories
    private final IntegerMap<UnitCategory> m_chosenCategoryCounts;

    // m_candidateCategoryCounts:
    // Determined by solver and contains a count of all candidate categories,
    // found by categorizing all candidate units.
    private final IntegerMap<UnitCategory> m_candidateCategoryCounts; 

    // m_candidateCompositeCategories:
    // Determined by solver and contains all composite category sets that match 
    // chosen categories.  This is the main intermediate output of the solver.
    // A composite category is a List of UnitCategories that together satisfy 
    // all units-having-dependents and dependent-units in the chosen categories.
    // ie: transports, arm, and inf can be found in here, but battleships 
    // cannot. 
    // The candidate composite categories retain the order in which they
    // were determined, which is based on the order that m_allUnits was passed 
    // into the constructor.
    private final LinkedHashSet<List<UnitCategory>>  m_candidateCompositeCategories;

    // m_candidateCategories:
    // Determined by solver and contains all candidate categories; that is, 
    // categories for candidate units that match chosen categories.  The order
    // of the categories is preserved from the original ordering of m_allUnits.
    private final LinkedHashSet<UnitCategory> m_candidateCategories;

    // m_candidateCategoriesWithoutDependents: 
    // Determined by solver and contains all independent units; ie: units that
    // aren't found in m_candidateCompositeCategories (see above).
    // The order of the categories is preserved from the original ordering of 
    // m_allUnits.
    private final Set<UnitCategory> m_candidateCategoriesWithoutDependents;

    // m_candidateToChosenCategories:
    // Determined by solver and contains a map of all candidate categories to 
    // their respective chosen categories.  A candidate category matches a 
    // chosen category if everything but their dependents match, since chosen 
    // categories don't categorize dependents but candidate categories do.
    // More than one candidate category may map to a single chosen category.
    private final Map<UnitCategory,UnitCategory> m_candidateToChosenCategories;

    public UnitAutoChooser(final Collection<Unit> allUnits,
                           final Collection<Unit> chosenUnits,
                           final Map<Unit, Collection<Unit>> dependentsMap,
                           boolean bAllowImplicitDependents,
                           boolean bCategorizeMovement)
                           
    {
        if (allUnits == null)
            m_allUnits = Collections.emptyList();
        else
            m_allUnits = allUnits;
        if (chosenUnits == null)
            m_chosenUnits = Collections.emptyList();
        else
            m_chosenUnits = chosenUnits;

        m_bCategorizeMovement = bCategorizeMovement;
        m_bAllowImplicitDependents = bAllowImplicitDependents;

        if (dependentsMap == null)
            m_dependentsMap = Collections.emptyMap();
        else
            m_dependentsMap = dependentsMap;

        // preserve insertion order
        m_candidateUnits = new LinkedHashSet<Unit>(allUnits.size()+1, 1);


        // some member variables for saving state
        m_selectedUnitSolutions = new ArrayList<Set<Unit>>(allUnits.size());

        // don't categorize dependents for allCategories 
        // if m_bCategorizeMovement is true then we use allCategoriesWithMovement as well (see below).
        // tell UnitSeperator not to sort the results since we want to preserve order
        Set<UnitCategory> allCategories    = UnitSeperator.categorize(m_allUnits, m_dependentsMap, false, false);

        // don't categorize dependents for chosenCategories
        // tell UnitSeperator not to sort the results since we want to preserve order
        Set<UnitCategory> chosenCategories = UnitSeperator.categorize(m_chosenUnits, null, false, false);

        // store occurrence count for chosen categories
        m_chosenCategoryCounts = new IntegerMap<UnitCategory>(chosenCategories.size()+1, 1);

        // store occurrence count for candidate categories (based on chosen categories)
        m_candidateCategoryCounts = new IntegerMap<UnitCategory>(allCategories.size()+1, 1);

        // 
        m_candidateToChosenCategories = new HashMap<UnitCategory,UnitCategory>(allCategories.size()+1, 1);

        // preserve insertion order
        m_candidateCompositeCategories = new LinkedHashSet<List<UnitCategory>>(allCategories.size()+1, 1);
        // preserve insertion order
        m_candidateCategories = new LinkedHashSet<UnitCategory>(allCategories.size()+1, 1);
        // preserve insertion order
        m_candidateCategoriesWithoutDependents = new LinkedHashSet<UnitCategory>(allCategories.size()+1, 1);

        // do the bulk of the category-related work to find our solutions in terms of categories
        if (m_bCategorizeMovement)
        {
            Set<UnitCategory> allCategoriesWithMovement = UnitSeperator.categorize(m_allUnits, m_dependentsMap, true, false);
            solveCandidateCompositeCategories(allCategories, allCategoriesWithMovement, chosenCategories);
        }
        else
        {
            solveCandidateCompositeCategories(allCategories, allCategories, chosenCategories);
        }

        // assemble the solution and solve the members
        chooseUnits();
    }

    public List<Unit> getCandidateUnits(boolean selectDependents)
    {
        // always select dependents for candidate units
        List<Unit> candidateUnits = new ArrayList<Unit>(m_allUnits.size());
        for (Unit unit : m_candidateUnits)
        {
            candidateUnits.add(unit);
            if (selectDependents)
            {
                Collection<Unit> dependents = m_dependentsMap.get(unit);
                if (dependents == null)
                    dependents = Collections.emptyList();
                for (Unit dependent : dependents)
                    candidateUnits.add(dependent);
            }
        }
        return candidateUnits;
    }

    public Collection<Unit> getChosenUnits()
    {
        return m_chosenUnits;
    }

    public Iterator<Set<Unit>> solutionIterator()
    {
        return m_selectedUnitSolutions.iterator();
    }

    public int solutionCount()
    {
        return m_selectedUnitSolutions.size();
    }

    public int exactSolutionCount()
    {
        return m_exactSolutionCount;
    }

    public boolean foundCompleteSolution()
    {
        return m_bFoundCompleteSolution;
    }

    public List<Set<Unit>> getAllSolutions(boolean selectImplicitDependents)
    {
        List<Set<Unit>> allSolutions = new ArrayList<Set<Unit>>(solutionCount());
        for (int i=0; i < solutionCount(); i++)
            allSolutions.add(getSolution(i, selectImplicitDependents));
        return allSolutions;
    }

    public Set<Unit> getSolution(int solutionIndex)
    {
        return getSolution(solutionIndex, false);
    }

    public Set<Unit> getSolution(int solutionIndex, boolean selectImplicitDependents)
    {
        Set<Unit> selectedUnits = new LinkedHashSet<Unit>(m_allUnits.size()+1, 1);
        List<Unit> dependentUnits = new ArrayList<Unit>(m_allUnits.size());
        selectedUnits.addAll(m_selectedUnitSolutions.get(solutionIndex));
        for (Unit unit : selectedUnits)
        {
            Collection<Unit> dependents = m_dependentsMap.get(unit);
            if (dependents == null)
                dependents = Collections.emptyList();
            for (Unit dependent : dependents)
                dependentUnits.add(dependent);
        }

        if (!selectImplicitDependents)
        {
            // add only the dependent units that were in chosen units
            Set<UnitCategory> dependentCategories = UnitSeperator.categorize(dependentUnits, m_dependentsMap, false, false);
            IntegerMap<UnitCategory> usedCategoryCounts = new IntegerMap<UnitCategory>(m_chosenCategoryCounts.size()+1, 1);
            for (UnitCategory chosenCategory : m_chosenCategoryCounts.keySet())
            {
                for (UnitCategory dependentCategory : dependentCategories)
                {
                    for (int i=0; i < dependentCategory.getUnits().size(); i++)
                    {
                        if (chosenCategory.equals(dependentCategory) 
                            && (m_chosenCategoryCounts.getInt(chosenCategory) 
                                  - usedCategoryCounts.getInt(chosenCategory) > 0))
                        {
                            usedCategoryCounts.add(chosenCategory, 1);
                            selectedUnits.add(dependentCategory.getUnits().get(i));
                        }
                    }
                }
            }
        }
        else
        {
            selectedUnits.addAll(dependentUnits);
        }

        return selectedUnits;
    }

    public boolean isMovementCategorized()
    {
        return m_bCategorizeMovement;
    }

    public Match<Collection<Unit>> getChooserBoundaryMatch()
    {
        Match<Collection<Unit>> unitCategoryCountMatch = new Match<Collection<Unit>>()
        {
            public boolean match(Collection<Unit> units)
            {
                IntegerMap<UnitCategory> currentMap = new IntegerMap<UnitCategory>(units.size()+1, 1);
                for (Unit unit : units)
                    currentMap.add(new UnitCategory(unit, false, m_bCategorizeMovement), 1);
                return m_chosenCategoryCounts.greaterThanOrEqualTo(currentMap);
            }
        };
        return unitCategoryCountMatch;
    }

    private void chooseUnits()
    {

        //System.out.println("chosenCounts: "+m_chosenCategoryCounts);
        //System.out.println("candidateCompositeCategories: "+m_candidateCompositeCategories);

        for (List<UnitCategory> compositeCategory : m_candidateCompositeCategories)
        {
            //System.out.println("converting composite category: " + compositeCategory);

            Set<Unit> compositeCategoryUnits = new LinkedHashSet<Unit>(compositeCategory.size()+1, 1);
            IntegerMap<UnitCategory> usedCategoryCounts = new IntegerMap<UnitCategory>(m_chosenCategoryCounts.size()+1, 1);
            boolean bUnitsCanAllBeMapped = true;
            for (UnitCategory category : compositeCategory)
            {
                UnitCategory chosenCategory = m_candidateToChosenCategories.get(category);
                boolean bDoneCategory = false;
                for (Unit unit : category.getUnits())
                {
                    if (!compositeCategoryUnits.contains(unit) && !bDoneCategory)
                    {
                        // create the mapping and add it to defaultSelections
                        if ((m_chosenCategoryCounts.getInt(chosenCategory) - usedCategoryCounts.getInt(chosenCategory)) > 0)
                        {
                            //System.out.println("converting category to units: " + category);

                            usedCategoryCounts.add(chosenCategory, 1);
                            compositeCategoryUnits.add(unit);
                            Collection<Unit> dependents = m_dependentsMap.get(unit);
                            if (dependents == null)
                                dependents = Collections.emptyList();
                            for (Unit dependent : dependents)
                            {
                                UnitCategory dependentCategory = new UnitCategory(dependent.getType(), dependent.getOwner());
                                usedCategoryCounts.add(dependentCategory, 1);
                            }
                            bDoneCategory = true;
                        }
                        else
                        {
                            //System.out.println("couldn't convert category to units: " + category);

                            bUnitsCanAllBeMapped = false;
                            break;
                        }
                    }
                    m_candidateUnits.add(unit);
                }
                //System.out.println("usedCounts: "+usedCategoryCounts);
            }

            //System.out.println("\t\tfound composite units "+compositeCategoryUnits);

            // Add independent units to solution.
            // This may also add unsatisfied units for any incomplete solutions.
            if (bUnitsCanAllBeMapped)
            {
                for (UnitCategory category : m_candidateCategoriesWithoutDependents)
                {
                    UnitCategory chosenCategory = m_candidateToChosenCategories.get(category);
                    for (Unit unit : category.getUnits())
                    {
                        if (compositeCategoryUnits.contains(unit))
                            continue;
                        // don't add dependents to candidate units
                        // but there will be dependents in this loop anyway
                        m_candidateUnits.add(unit);

                        if ((m_chosenCategoryCounts.getInt(chosenCategory) - usedCategoryCounts.getInt(chosenCategory)) > 0)
                        {
                            usedCategoryCounts.add(chosenCategory, 1);
                            //System.out.println("adding unit "+unit);
                            compositeCategoryUnits.add(unit);
                            Collection<Unit> dependents = m_dependentsMap.get(unit);
                            if (dependents == null)
                                dependents = Collections.emptyList();
                            for (Unit dependent : dependents)
                            {
                                UnitCategory dependentCategory = new UnitCategory(dependent.getType(), dependent.getOwner());
                                usedCategoryCounts.add(dependentCategory, 1);
                            }
                        }
                    }
                }
                //System.out.println("\t\tsaving units "+compositeCategoryUnits);

                m_selectedUnitSolutions.add(compositeCategoryUnits);
                if (usedCategoryCounts.equals(m_chosenCategoryCounts))
                {
                    m_exactSolutionCount++;
                    m_bFoundCompleteSolution = true;
                }
                else if (usedCategoryCounts.greaterThanOrEqualTo(m_chosenCategoryCounts))
                {
                    m_bFoundCompleteSolution = true;
                }
            }
            
            //System.out.println("usedCounts: "+usedCategoryCounts);
        }

        if (m_candidateCompositeCategories.isEmpty()
                && !m_candidateCategoriesWithoutDependents.isEmpty()) 
        {
            // no composite categories, just add independent units
            Set<Unit> independentCategoryUnits = new HashSet<Unit>(m_chosenUnits.size());
            IntegerMap<UnitCategory> usedCategoryCounts = new IntegerMap<UnitCategory>(m_chosenCategoryCounts.size()+1, 1);
            for (UnitCategory category : m_candidateCategoriesWithoutDependents)
            {
                UnitCategory chosenCategory = m_candidateToChosenCategories.get(category);
                for (Unit unit : category.getUnits())
                {
                    // don't add dependents to candidate units
                    // but there will be dependents in this loop anyway
                    m_candidateUnits.add(unit);
                    if ((m_chosenCategoryCounts.getInt(chosenCategory) - usedCategoryCounts.getInt(chosenCategory)) > 0)
                    {
                        usedCategoryCounts.add(chosenCategory, 1);
                        independentCategoryUnits.add(unit);
                    }
                }
            }
            //System.out.println("\t\tsaving independent units "+independentCategoryUnits);

            m_selectedUnitSolutions.add(independentCategoryUnits);
            if (m_chosenCategoryCounts.equals(usedCategoryCounts))
            {
                m_exactSolutionCount++;
                m_bFoundCompleteSolution = true;
            }
        }
        else if (m_selectedUnitSolutions.isEmpty())
        {
            // We can get here if there are no solutions found.
            // An example would be where m_bAllowImplicitDependents is false,
            // and only a transport is selected, but every candidate transport
            // is carrying at least one dependent.  In a case like this we 
            // just find the first transport in our list and return it as a
            // greedy solution.
            
            // There were composite categories, but none could be satisfied
            // Add the simple solution if there is no other solution
            Set<Unit> simpleSolutionUnits = new HashSet<Unit>(m_chosenUnits.size());
            IntegerMap<UnitCategory> usedCategoryCounts = new IntegerMap<UnitCategory>(m_chosenCategoryCounts.size()+1, 1);
            // preserve original category order
            for (UnitCategory category : m_candidateCategories)
            {
                for (Unit unit : category.getUnits())
                {
                    m_candidateUnits.add(unit);
                    UnitCategory chosenCategory = m_candidateToChosenCategories.get(category);
                    int chosenCategoryCount = m_chosenCategoryCounts.getInt(chosenCategory);
                    if ((chosenCategoryCount - usedCategoryCounts.getInt(chosenCategory)) > 0)
                    {
                        usedCategoryCounts.add(chosenCategory, 1);
                        simpleSolutionUnits.add(unit);
                    }
                }
            }
            //System.out.println("\t\tsaving simple greedy solution "+simpleSolutionUnits);

            m_selectedUnitSolutions.add(simpleSolutionUnits);
            if (m_chosenCategoryCounts.equals(usedCategoryCounts))
                m_bFoundCompleteSolution = true;
        }

        //System.out.println("candidateUnits: "+m_candidateUnits);

    }


    // solveCandidateCompositeCategories()
    //
    // IN:      allCategoriesNoMovement (with dependents, movement not categorized)
    // IN:      allCategories (with dependents, movement may be categorized)
    // IN:      chosenCategories (no dependents, movement not categorized)
    // OUT:     n/a
    //
    // This method will populate m_candidateCompositeCategories, which is 
    // a Set of Lists of UnitCategories.  Each inner-List groups the categories of all of 
    // our chosen units that are dependent on, or are depended on by other units. 
    // The categories are grouped such that each inner-list contains all the categories 
    // required to satisfy the chosenCategories completely for these unit categories.
    // If no exact groupings are found, the closest greedy grouping is used
    // and the solution is not marked as exact.
    //
    private void solveCandidateCompositeCategories(final Collection<UnitCategory> allCategoriesNoMovement,
                                                   final Collection<UnitCategory> allCategories,
                                                   final Collection<UnitCategory> chosenCategories)
    {
        // Don't preserve insertion order. It will be preserved via m_candidateCategories.
        Set<UnitCategory> candidateCategoriesWithDependents = new HashSet<UnitCategory>(allCategories.size()+1, 1);

        // keep track of total count for array allocation later
        int candidateCategoryCountWithDependents = 0;

        // Don't need to preserve insertion order; only used for testing intersections
        Set<UnitCategory> chosenDependentCategories = new HashSet<UnitCategory>(allCategories.size()+1, 1);

        Map<UnitCategory, UnitCategory> allCategoriesToCategoriesWithoutMovement = new HashMap<UnitCategory,UnitCategory>(allCategories.size()+1, 1);

        //System.out.println("allCategoriesNoMovement: "+allCategoriesNoMovement);
        //System.out.println("allCategories: "+allCategories);
        //System.out.println("chosenCategories: "+chosenCategories);

        // Build a map of allCategoriesWithMovementCategorized -> allCategoriesWithoutMovementCategorized
        // We do this because if m_bCategorizeMovement is true, then we can't compare chosen and candidate
        // categories effectively since candidate categories include movement and chosen categories don't.
        // So, we maintain this mapping so we can easily find the corresponding candidate category without 
        // movement categorized for comparisons with chosen categories.
        for (UnitCategory categoryWithMovement : allCategories)
        {
            if (m_bCategorizeMovement)
            {
                for (UnitCategory categoryNoMovement : allCategoriesNoMovement)
                {
                    if (categoryWithMovement.equalsIgnoreMovement(categoryNoMovement))
                    {
                        allCategoriesToCategoriesWithoutMovement.put(categoryWithMovement, categoryNoMovement);
                        break;
                    }
                }
            }
            else
            {
                // if movement isn't categorized then just map key->key
                allCategoriesToCategoriesWithoutMovement.put(categoryWithMovement, categoryWithMovement);
            }
        }

        // populate our category lists/sets and calculate counts
        for (UnitCategory chosenCategory: chosenCategories)
        {
            m_chosenCategoryCounts.add(chosenCategory, chosenCategory.getUnits().size());
            // find all matching candidate categories
            // there can be more than one because chosen categories 
            // don't categorize dependents, but candidate categories do
            //Iterator<UnitCategory> categoryIter = allCategoriesNoMovement.iterator();
            Iterator<UnitCategory> categoryIter = allCategories.iterator();
            while (categoryIter.hasNext())
            {
                UnitCategory candidateCategory = categoryIter.next();
                UnitCategory candidateCategoryNoMovement = allCategoriesToCategoriesWithoutMovement.get(candidateCategory);
                if (chosenCategory.equalsIgnoreDependents(candidateCategoryNoMovement))
                {
                    // m_candidateCategories preserves insertion order
                    m_candidateCategories.add(candidateCategory);
                    m_candidateCategoryCounts.add(candidateCategory, candidateCategory.getUnits().size());
                    m_candidateToChosenCategories.put(candidateCategory, chosenCategory);
                    for (int i=0; i < candidateCategory.getUnits().size(); i++)
                    {
                        if (!candidateCategory.getDependents().isEmpty())
                        {
                            // Process dependents and determine if all dependents are in our 
                            // chosen categories.
                            boolean bAllDependentsAreCategorized = true;
                            for (UnitOwner unitOwner : candidateCategory.getDependents())
                            {
                                UnitCategory dependentCategory = new UnitCategory(unitOwner.getType(), unitOwner.getOwner());
                                if (!chosenCategories.contains(dependentCategory))
                                    bAllDependentsAreCategorized = false;
                                else
                                    chosenDependentCategories.add(dependentCategory);
                            }

                            // If bAllDependentsAreCategorized is true, then we want to save this
                            // category as a candidate-with-dependents for later processing,
                            // otherwise we ignore it as it won't be part of the solution.
                            // If bAllowImplicitDependents is true however, then we always save the 
                            // category as a candidate, since we allow greedy solutions in that case.
                            if (bAllDependentsAreCategorized || m_bAllowImplicitDependents)
                            {
                                candidateCategoriesWithDependents.add(candidateCategory);
                                candidateCategoryCountWithDependents++;
                            }

                            // add category to end of independents list in case there are no 
                            // composite solutions
                            if (m_bAllowImplicitDependents)
                                m_candidateCategoriesWithoutDependents.add(candidateCategory);
                        }
                        else
                        {
                            // Add the unit if it has no dependents
                            // This will also add dependent units themselves, which 
                            // we don't want.  We will remove dependent units
                            // after processing this while loop, below.
                            m_candidateCategoriesWithoutDependents.add(candidateCategory);
                        }
                    }
                }
            }
        }


        // remove dependents from independent categories
        Iterator<UnitCategory> categoryIter = m_candidateCategoriesWithoutDependents.iterator();
        while (categoryIter.hasNext())
        {
            UnitCategory candidateCategory = categoryIter.next();
            UnitCategory chosenCategory = m_candidateToChosenCategories.get(candidateCategory);
            for (UnitCategory chosenDependentCategory : chosenDependentCategories)
            {
                if (chosenCategory.equalsIgnoreMovement(chosenDependentCategory))
                {
                    categoryIter.remove();
                    break;
                }
            }
        }

        // Find all categories without dependents that have the same chosen category 
        // as other categories with dependents, and copy them over to that set.
        // An example is an empty transport among non-empty transports.
        // We still keep them in the without-dependents set as well since 
        // they'll be processed with the units without dependents at the end 
        // if there are still unclaimed categories to fill after processing the 
        // composite solutions.  They also end up in m_candidateUnits that way.
        // Note that we mess up the category order doing it this way, but we
        // reinstate the proper ordering afterwards, in the next step.
        Set<UnitCategory> categoriesToCopy = new HashSet<UnitCategory>(m_candidateCategoriesWithoutDependents.size() + 1, 1);
        for (UnitCategory category : candidateCategoriesWithDependents)
        {
            UnitCategory chosenCategory = m_candidateToChosenCategories.get(category);
            for (UnitCategory candidateCategory : m_candidateCategoriesWithoutDependents)
            {
                UnitCategory candidateCategoryNoMovement = allCategoriesToCategoriesWithoutMovement.get(candidateCategory);
                if (chosenCategory.equalsIgnoreDependents(candidateCategoryNoMovement))
                    categoriesToCopy.add(candidateCategory);
            }
        }
        candidateCategoriesWithDependents.addAll(categoriesToCopy);
       
        // create a count of chosenCategories without dependents
        IntegerMap<UnitCategory> chosenCategoryCountsNoDependents = new IntegerMap<UnitCategory>(chosenCategories.size()+1, 1);
        for (UnitCategory candidateCategory : candidateCategoriesWithDependents)
        {
            UnitCategory chosenCategory = m_candidateToChosenCategories.get(candidateCategory);
            if (!chosenDependentCategories.contains(chosenCategory))
                chosenCategoryCountsNoDependents.put(chosenCategory, chosenCategory.getUnits().size());
        }

        // expand the Set of candidate categories with dependents into a list for easy linear processing
        // preserve original order from m_candidaateCategories
        List<UnitCategory> candidateCategoriesWithDependentsList = new ArrayList<UnitCategory>(candidateCategoryCountWithDependents);
        for (UnitCategory category : m_candidateCategories)
        {
            if (candidateCategoriesWithDependents.contains(category))
            {
                for (int i=0; i<m_candidateCategoryCounts.getInt(category); i++)
                    candidateCategoriesWithDependentsList.add(category);
            }
        }

        //System.out.println("candidateToChosenCategories: "+m_candidateToChosenCategories);
        //System.out.println("candidateCategoriesWithDependents: "+candidateCategoriesWithDependents);
        //System.out.println("candidateCategoriesWithoutDependents: "+m_candidateCategoriesWithoutDependents);
        //System.out.println("chosenCategories: "+chosenCategories);
        //System.out.println("chosenCounts: "+m_chosenCategoryCounts);

        // create and populate an IntegerMap for chosen categories with dependents
        IntegerMap<UnitCategory> chosenCategoryCountsWithDependents = new IntegerMap<UnitCategory>(m_chosenCategoryCounts.size()+1, 1);
        for (UnitCategory category : candidateCategoriesWithDependents)
        {
            UnitCategory chosenCategory = m_candidateToChosenCategories.get(category);
            chosenCategoryCountsWithDependents.put(chosenCategory, m_chosenCategoryCounts.getInt(chosenCategory));
            if (!category.getDependents().isEmpty())
            {
                for (UnitOwner dependent : category.getDependents())
                {
                    UnitCategory dependentCategory = new UnitCategory(dependent.getType(), dependent.getOwner());
                    int dependentCount = m_chosenCategoryCounts.getInt(dependentCategory);
                    if (dependentCount > 0)
                        chosenCategoryCountsWithDependents.put(dependentCategory, dependentCount);
                }
            }
        }

        //System.out.println("candidateCategoriesWithDependentsList: "+candidateCategoriesWithDependentsList);
        //System.out.println("chosenCountsWithDependents: "+chosenCategoryCountsWithDependents);
        //System.out.println("chosenCountsNoDependents: "+chosenCategoryCountsNoDependents);


        //
        // The remainder of this method implements a recursive algorithm (without recursion) 
        // to group individual categories into composite category solutions.
        // A composite category solution is just a List<UnitCategory>, where every category in the list
        // is required to satisfy the constraints.  The solution may be marked as exact, or greedy.  
        // Exact solutions are always added to the beginning of the composite category LinkedHashSet, 
        // followed by greedy solutions, so a later on a given solution can be determined to be exact or 
        // greedy by comparing its index in the solution set to the total number of exact solutions.
        // 
        // Here's a simple example:
        //    The following units are in a sea zone:   trn, trn[inf,arm], trn[inf]
        //    The user chose trn, inf, inf, arm with the mouse.
        //    This algorithm will determine which transports and units they really want. 
        // The resulting solution set contains one exact solution: [trn[inf,arm],trn[inf]]
        // That was a simple example but it can get much more complicated.  
        // This algorithm should satisfy all cases.
        // See test cases for more examples.
        //

        // intitialize structures for use during recursive processing
        List<UnitCategory> currentCandidateCategories = new ArrayList<UnitCategory>(candidateCategoriesWithDependents.size());
        IntegerMap<UnitCategory> currentCandidateCategoryCounts = new IntegerMap<UnitCategory>(candidateCategoriesWithDependents.size()+1, 1);
        IntegerMap<UnitCategory> currentChosenCategoryCounts = new IntegerMap<UnitCategory>(candidateCategoriesWithDependents.size()+1, 1);
        IntegerMap<UnitCategory> currentChosenCategoryCountsWithDependents = new IntegerMap<UnitCategory>(candidateCategoriesWithDependents.size()+1, 1);
        IntegerMap<UnitCategory> currentChosenCategoryCountsNoDependents = new IntegerMap<UnitCategory>(candidateCategoriesWithDependents.size()+1, 1);

        // Keep candidate composite categories for greedy solutions seperate, so we can discard them
        // if an exact solution is found.
        // preserve insertion order
        LinkedHashSet<List<UnitCategory>>  greedyCandidateCompositeCategories 
              = new LinkedHashSet<List<UnitCategory>>(candidateCategoriesWithDependents.size()+1, 1);

        // keep an ongoing reference to the current best solution
        List<UnitCategory> bestCandidateSolution = null;
        int bestUnitCount = 0;

        // setup a simple stack of List indexes to avoid the overhead of actual recursion
        Stack<Integer> indexStack = new Stack<Integer>();
        Integer curIndex = Integer.valueOf(0);
        // do it
        while (true)
        {
            //System.out.println("curIndex: "+curIndex);
            
            // handle stopping condition
            if (curIndex.intValue() == candidateCategoriesWithDependentsList.size())
            {
                if (indexStack.empty())
                    break; // finished!
                //System.out.println("Reached end of iteration; popping stack.");
                curIndex = indexStack.pop();
            }
            else
            {
                
                // push our state on the stack
                indexStack.push(curIndex);
                //System.out.println("stack: "+indexStack);

                // clear current state ready for processing
                currentCandidateCategories.clear(); 
                currentCandidateCategoryCounts.clear(); 
                currentChosenCategoryCounts.clear(); 
                currentChosenCategoryCountsWithDependents.clear(); 
                currentChosenCategoryCountsNoDependents.clear(); 
                int currentUnitCount = 0;

                // Populate all structures from our stack of list indexes
                for (Integer i : indexStack)
                {
                    UnitCategory category = candidateCategoriesWithDependentsList.get(i.intValue());
                    UnitCategory chosenCategory = m_candidateToChosenCategories.get(category);
                    currentCandidateCategories.add(category);
                    currentCandidateCategoryCounts.add(category, 1);
                    currentChosenCategoryCountsWithDependents.add(chosenCategory, 1);
                    currentChosenCategoryCounts.add(chosenCategory, 1);
                    if (!chosenDependentCategories.contains(chosenCategory))
                        currentChosenCategoryCountsNoDependents.add(chosenCategory, 1);
                    currentUnitCount++;
                    // populate counts of dependents
                    if (!category.getDependents().isEmpty())
                    {
                        for (UnitOwner unitOwner : category.getDependents())
                        {
                            UnitCategory dependentCategory = new UnitCategory(unitOwner.getType(), unitOwner.getOwner());
                            // add all dependents to this map, regardless of whether they are implicit or not
                            currentChosenCategoryCountsWithDependents.add(dependentCategory, 1);
                            // add dependents to this map only if they were actually chosen
                            if (m_chosenCategoryCounts.getInt(dependentCategory) > 0)
                                currentChosenCategoryCounts.add(dependentCategory, 1);
                            // add all dependents to this map
                            currentCandidateCategoryCounts.add(dependentCategory, 1);
                            currentUnitCount++;
                        }
                    }
                }

                //System.out.println("currentCandidateCounts: "+currentCandidateCategoryCounts);
                //System.out.println("currentChosenCounts: "+currentChosenCategoryCounts);
                //System.out.println("currentChosenCountsWithDependents: "+currentChosenCategoryCountsWithDependents);
                //System.out.println("currentChosenCountsNoDependents: "+currentChosenCategoryCountsNoDependents);
                
                // Determine whether chosen category counts with dependents match the current chosen category counts 
                // and proceed appropriately.
                // Implicit dependents are not included in either of these counts, thus they are ignored here.
                // We ignore implicit dependents here because they are handled below inside the first block,
                // where we determine whether an exact or greedy solution was found.
                if (   ( m_bAllowImplicitDependents
                         &&  chosenCategoryCountsNoDependents.equals(currentChosenCategoryCountsNoDependents)
                         &&  currentChosenCategoryCounts.greaterThanOrEqualTo(chosenCategoryCountsWithDependents))
                    || (!m_bAllowImplicitDependents && 
                            chosenCategoryCountsWithDependents.equals(currentChosenCategoryCounts)))
                {
                    // Found match.
                    //System.out.println("Found match for all categories! Saving and popping stack.");

                    // Decide whether to save this solution as an exact solution or as a greedy solution.
                    // Explanation:
                    //   If solver is not allowing implicit dependents, then all solutions are exact
                    //   solutions since the chosen category counts must match the current category counts exactly.
                    //   If solver is allowing implicit dependents, then all chosen category counts with dependents 
                    //   must match the current category counts with dependents for an exact solution, 
                    //   since we are trying to account for all dependents.
                    //   Note that in the case where there are no dependent units in the territory at all, the List we are processing
                    //   in this while loop would be empty so this algo would be skipped and an independent solution would be found later.
                    if (!m_bAllowImplicitDependents || 
                          (chosenCategoryCountsWithDependents.equals(currentChosenCategoryCountsWithDependents)))
                    {
                        //System.out.println("->Found exact solution");
                        // save the current candidate categories but don't save the dependent units
                        List<UnitCategory> newCandidateCompositeCategory = new ArrayList<UnitCategory>(currentCandidateCategories);
                        if (!m_candidateCompositeCategories.contains(newCandidateCompositeCategory))
                        {
                            m_candidateCompositeCategories.add(newCandidateCompositeCategory);
                            //System.out.println("candidateCompositeCategories: "+m_candidateCompositeCategories);
                        }
                    }
                    // This must be a greedy solution.
                    else
                    {
                        // only applicable if bAllowImplicitDependents is true
                        //System.out.println("->Found greedy solution");
                        greedyCandidateCompositeCategories.add(new ArrayList<UnitCategory>(currentCandidateCategories));
                        //System.out.println("greedyCandidateCompositeCategories: "+greedyCandidateCompositeCategories);
                    }
                    // pop the stack, we've gone as far as we can go
                    curIndex = indexStack.pop();
                }
                else if (   ( m_bAllowImplicitDependents
                             && !chosenCategoryCountsNoDependents.greaterThanOrEqualTo(currentChosenCategoryCountsNoDependents)
                             &&  currentChosenCategoryCounts.greaterThanOrEqualTo(chosenCategoryCountsWithDependents))
                         || ( !m_bAllowImplicitDependents
                             && !chosenCategoryCountsWithDependents.greaterThanOrEqualTo(currentChosenCategoryCounts)))
                {
                    // Too many categories.
                    //System.out.println("Too many categories selected. Discarding and popping stack.");

                    // pop the stack, we've gone as far as we can go
                    curIndex = indexStack.pop();
                }
                else
                {
                    // Found room but not exact yet
                    //System.out.println("Found room... keeping currentCounts in stack.");

                    // Save this as the best incomplete solution if it has the highest unit count
                    if (currentUnitCount > bestUnitCount)
                    {
                        bestUnitCount = currentUnitCount;
                        bestCandidateSolution = new ArrayList<UnitCategory>(currentCandidateCategories);
                    }
                    // don't pop the stack, we are still growing!
                }
            }
            // increment index and continue
            curIndex = Integer.valueOf(curIndex.intValue()+1);

        } // while(true)

        // append greedy solutions if we have any
        m_candidateCompositeCategories.addAll(greedyCandidateCompositeCategories);

        // if no exact or greedy solutions, use best incomplete solution
        if (m_candidateCompositeCategories.isEmpty() && bestCandidateSolution != null)
            m_candidateCompositeCategories.add(bestCandidateSolution);

    }

}
