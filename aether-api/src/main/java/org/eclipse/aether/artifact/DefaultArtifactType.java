/*******************************************************************************
 * Copyright (c) 2010, 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.artifact;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple artifact type.
 */
public final class DefaultArtifactType
    implements ArtifactType
{

    private final String id;

    private final String extension;

    private final String classifier;

    private final Map<String, String> properties;

    public DefaultArtifactType( String id )
    {
        this( id, id, "", "none", false, false );
    }

    public DefaultArtifactType( String id, String extension, String classifier, String language )
    {
        this( id, extension, classifier, language, true, false );
    }

    public DefaultArtifactType( String id, String extension, String classifier, String language,
                                boolean constitutesBuildPath, boolean includesDependencies )
    {
        if ( id == null || id.length() < 0 )
        {
            throw new IllegalArgumentException( "no type id specified" );
        }
        this.id = id;
        this.extension = emptify( extension );
        this.classifier = emptify( classifier );
        Map<String, String> props = new HashMap<String, String>();
        props.put( ArtifactProperties.TYPE, id );
        props.put( ArtifactProperties.LANGUAGE, ( language != null && language.length() > 0 ) ? language : "none" );
        props.put( ArtifactProperties.INCLUDES_DEPENDENCIES, Boolean.toString( includesDependencies ) );
        props.put( ArtifactProperties.CONSTITUTES_BUILD_PATH, Boolean.toString( constitutesBuildPath ) );
        properties = Collections.unmodifiableMap( props );
    }

    /**
     * Creates a new artifact type with the specified properties.
     * 
     * @param id The identifier of the type, must not be {@code null} or empty.
     * @param extension The file extension for artifacts of this type, may be {@code null}.
     * @param classifier The classifier for artifacts of this type, may be {@code null}.
     * @param properties The properties for artifacts of this type, may be {@code null}.
     */
    public DefaultArtifactType( String id, String extension, String classifier, Map<String, String> properties )
    {
        if ( id == null || id.length() < 0 )
        {
            throw new IllegalArgumentException( "no type id specified" );
        }
        this.id = id;
        this.extension = emptify( extension );
        this.classifier = emptify( classifier );
        this.properties = AbstractArtifact.copyProperties( properties );
    }

    private static String emptify( String str )
    {
        return ( str == null ) ? "" : str;
    }

    public String getId()
    {
        return id;
    }

    public String getExtension()
    {
        return extension;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

}
