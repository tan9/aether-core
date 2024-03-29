/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.util.graph.selector;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 * A dependency selector that combines zero or more other selectors using a logical {@code AND}.
 */
public final class AndDependencySelector
    implements DependencySelector
{

    private final Set<DependencySelector> selectors;

    private int hashCode;

    /**
     * Creates a new selector from the specified selectors.
     * 
     * @param selectors The selectors to combine, may be {@code null}.
     */
    public AndDependencySelector( DependencySelector... selectors )
    {
        if ( selectors != null && selectors.length > 0 )
        {
            this.selectors = new LinkedHashSet<DependencySelector>();
            Collections.addAll( this.selectors, selectors );
        }
        else
        {
            this.selectors = Collections.emptySet();
        }
    }

    /**
     * Creates a new selector from the specified selectors.
     * 
     * @param selectors The selectors to combine, may be {@code null}.
     */
    public AndDependencySelector( Collection<DependencySelector> selectors )
    {
        if ( selectors != null && !selectors.isEmpty() )
        {
            this.selectors = new LinkedHashSet<DependencySelector>( selectors );
        }
        else
        {
            this.selectors = Collections.emptySet();
        }
    }

    private AndDependencySelector( Set<DependencySelector> selectors )
    {
        if ( selectors != null && !selectors.isEmpty() )
        {
            this.selectors = selectors;
        }
        else
        {
            this.selectors = Collections.emptySet();
        }
    }

    /**
     * Creates a new selector from the specified selectors.
     * 
     * @param selector1 The first selector to combine, may be {@code null}.
     * @param selector2 The first selector to combine, may be {@code null}.
     * @return The combined selector or {@code null} if both selectors were {@code null}.
     */
    public static DependencySelector newInstance( DependencySelector selector1, DependencySelector selector2 )
    {
        if ( selector1 == null )
        {
            return selector2;
        }
        else if ( selector2 == null )
        {
            return selector1;
        }
        return new AndDependencySelector( selector1, selector2 );
    }

    public boolean selectDependency( Dependency dependency )
    {
        for ( DependencySelector selector : selectors )
        {
            if ( !selector.selectDependency( dependency ) )
            {
                return false;
            }
        }
        return true;
    }

    public DependencySelector deriveChildSelector( DependencyCollectionContext context )
    {
        int seen = 0;
        Set<DependencySelector> childSelectors = null;

        for ( DependencySelector selector : selectors )
        {
            DependencySelector childSelector = selector.deriveChildSelector( context );
            if ( childSelectors != null )
            {
                childSelectors.add( childSelector );
            }
            else if ( !selector.equals( childSelector ) )
            {
                childSelectors = new LinkedHashSet<DependencySelector>();
                if ( seen > 0 )
                {
                    for ( DependencySelector s : selectors )
                    {
                        if ( childSelectors.size() >= seen )
                        {
                            break;
                        }
                        childSelectors.add( s );
                    }
                }
                childSelectors.add( childSelector );
            }
            else
            {
                seen++;
            }
        }

        return childSelectors != null ? new AndDependencySelector( childSelectors ) : this;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        else if ( null == obj || !getClass().equals( obj.getClass() ) )
        {
            return false;
        }

        AndDependencySelector that = (AndDependencySelector) obj;
        return selectors.equals( that.selectors );
    }

    @Override
    public int hashCode()
    {
        if ( hashCode == 0 )
        {
            int hash = 17;
            hash = hash * 31 + selectors.hashCode();
            hashCode = hash;
        }
        return hashCode;
    }

}
