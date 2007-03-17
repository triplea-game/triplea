package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MoveValidationResult implements Serializable
{
    private String m_error = null;
    private List<String> m_disallowedUnitWarnings;
    private List<Collection<Unit>> m_disallowedUnitsList;

    private List<String> m_unresolvedUnitWarnings;
    private List<Collection<Unit>> m_unresolvedUnitsList;

    
    public MoveValidationResult()
    {
        m_disallowedUnitWarnings = new ArrayList<String>();
        m_disallowedUnitsList = new ArrayList<Collection<Unit>>();
        m_unresolvedUnitWarnings = new ArrayList<String>();
        m_unresolvedUnitsList = new ArrayList<Collection<Unit>>();
    }

    public MoveValidationResult(MoveValidationResult toCopy)
    {
        this();
        for (String warning : toCopy.getDisallowedUnitWarnings())
            for (Unit unit : toCopy.getDisallowedUnits(warning)) addDisallowedUnit(warning, unit);
        for (String warning : toCopy.getUnresolvedUnitWarnings())
            for (Unit unit : toCopy.getUnresolvedUnits(warning))
                addUnresolvedUnit(warning, unit);
        setError(toCopy.getError());
    }

    public void addDisallowedUnit(String warning, Unit unit)
    {
        int index = m_disallowedUnitWarnings.indexOf(warning);
        if (index == -1)
        {
            index = m_disallowedUnitWarnings.size();
            m_disallowedUnitWarnings.add(warning);
            m_disallowedUnitsList.add(new ArrayList<Unit>());
        }
        Collection<Unit> disallowedUnits = m_disallowedUnitsList.get(index);
        disallowedUnits.add(unit);
    }

    public boolean removeDisallowedUnit(String warning, Unit unit)
    {
        int index = m_disallowedUnitWarnings.indexOf(warning);
        if (index == -1)
            return false;
        Collection<Unit> disallowedUnits = m_disallowedUnitsList.get(index);
        if (!disallowedUnits.remove(unit))
            return false;
        if (disallowedUnits.isEmpty())
        {
            m_disallowedUnitsList.remove(disallowedUnits);
            m_disallowedUnitWarnings.remove(warning);
        }
        return true;
    }

    public void addUnresolvedUnit(String warning, Unit unit)
    {
        int index = m_unresolvedUnitWarnings.indexOf(warning);
        if (index == -1)
        {
            index = m_unresolvedUnitWarnings.size();
            m_unresolvedUnitWarnings.add(warning);
            m_unresolvedUnitsList.add(new ArrayList<Unit>());
        }
        Collection<Unit> unresolvedUnits = m_unresolvedUnitsList.get(index);
        unresolvedUnits.add(unit);
    }

    public boolean removeUnresolvedUnit(String warning, Unit unit)
    {
        int index = m_unresolvedUnitWarnings.indexOf(warning);
        if (index == -1)
            return false;
        Collection<Unit> unresolvedUnits = m_unresolvedUnitsList.get(index);
        if (!unresolvedUnits.remove(unit))
            return false;
        if (unresolvedUnits.isEmpty())
        {
            m_unresolvedUnitsList.remove(unresolvedUnits);
            m_unresolvedUnitWarnings.remove(warning);
        }
        return true;
    }

    public void setError(String error)
    {
        m_error = error;
    }

    public MoveValidationResult setErrorReturnResult(String error)
    {
        m_error = error;
        return this;
    }

    public String getError()
    {
        return m_error;
    }

    public Collection<Unit> getDisallowedUnits()
    {
        Set<Unit> allDisallowedUnits = new LinkedHashSet<Unit>();
        for (Collection<Unit> unitList : m_disallowedUnitsList)
            for (Unit unit : unitList)
                allDisallowedUnits.add(unit);

        return allDisallowedUnits;
    }

    public Collection<Unit> getUnresolvedUnits()
    {
        Set<Unit> allUnresolvedUnits = new LinkedHashSet<Unit>();
        for (Collection<Unit> unitList : m_unresolvedUnitsList)
            for (Unit unit : unitList)
                allUnresolvedUnits.add(unit);

        return allUnresolvedUnits;
    }

    public Collection<UnitType> getUnresolvedUnitTypes()
    {
        Set<UnitType> unresolvedUnitTypes = new HashSet<UnitType>();
        for (Unit unit : getUnresolvedUnits())
            unresolvedUnitTypes.add(unit.getType());
        return unresolvedUnitTypes;
    }

    public Collection<Unit> getDisallowedUnits(String warning)
    {
        int index = m_disallowedUnitWarnings.indexOf(warning);
        if (index == -1)
            return Collections.emptyList();
        return new ArrayList<Unit>(m_disallowedUnitsList.get(index));
    }

    public Collection<Unit> getUnresolvedUnits(String warning)
    {
        int index = m_unresolvedUnitWarnings.indexOf(warning);
        if (index == -1)
            return Collections.emptyList();
        return new ArrayList<Unit>(m_unresolvedUnitsList.get(index));
    }
    

    public Collection<String> getDisallowedUnitWarnings()
    {
        return new ArrayList<String>(m_disallowedUnitWarnings);
    }

    public Collection<String> getUnresolvedUnitWarnings()
    {
        return new ArrayList<String>(m_unresolvedUnitWarnings);
    }

    public String getDisallowedUnitWarning(int index)
    {
        if (index < 0 || index >= m_disallowedUnitWarnings.size())
            return null;
        return m_disallowedUnitWarnings.get(index);
    }

    public String getUnresolvedUnitWarning(int index)
    {
        if (index < 0 || index >= m_unresolvedUnitWarnings.size())
            return null;
        return m_unresolvedUnitWarnings.get(index);
    }

    public boolean hasError()
    {
        return m_error != null;
    }

    public boolean hasDisallowedUnits()
    {
        return m_disallowedUnitWarnings.size() > 0;
    }

    public boolean hasUnresolvedUnits()
    {
        return m_unresolvedUnitWarnings.size() > 0;
    }

    public boolean isMoveValid()
    {
        return !hasError() 
                && !hasDisallowedUnits() 
                && !hasUnresolvedUnits();
    }

    public int getTotalWarningCount()
    {
        return m_unresolvedUnitWarnings.size() 
               + m_disallowedUnitWarnings.size();
    }

    public void removeAnyUnresolvedUnitsThatAreDisallowed()
    {
        final MoveValidationResult oldResult = new MoveValidationResult(this);
        Collection<Unit> disallowedUnits = oldResult.getDisallowedUnits();
        Collection<Unit> unresolvedAndDisallowed = new ArrayList<Unit>(disallowedUnits);
        unresolvedAndDisallowed.retainAll(oldResult.getUnresolvedUnits());
        for (String warning : oldResult.getUnresolvedUnitWarnings())
            for (Unit unit : oldResult.getUnresolvedUnits(warning))
                if (disallowedUnits.contains(unit))
                    removeUnresolvedUnit(warning, unit);
    } }
