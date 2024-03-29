/*******************************************************************************
 * Copyright (c) 2010, 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.transfer;

import java.io.File;

import org.eclipse.aether.RequestTrace;

/**
 * Describes a resource being uploaded or downloaded by the repository system.
 */
public final class TransferResource
{

    private final String repositoryUrl;

    private final String resourceName;

    private final File file;

    private final long startTime;

    private final RequestTrace trace;

    private long contentLength = -1;

    /**
     * Creates a new transfer resource with the specified properties.
     * 
     * @param repositoryUrl The base URL of the repository, may be {@code null} or empty if unknown. If not empty, a
     *            trailing slash will automatically be added if missing.
     * @param resourceName The relative path to the resource within the repository, may be {@code null}. A leading slash
     *            (if any) will be automatically removed.
     * @param file The source/target file involved in the transfer, may be {@code null}.
     * @param trace The trace information, may be {@code null}.
     */
    public TransferResource( String repositoryUrl, String resourceName, File file, RequestTrace trace )
    {
        if ( repositoryUrl == null || repositoryUrl.length() <= 0 )
        {
            this.repositoryUrl = "";
        }
        else if ( repositoryUrl.endsWith( "/" ) )
        {
            this.repositoryUrl = repositoryUrl;
        }
        else
        {
            this.repositoryUrl = repositoryUrl + '/';
        }

        if ( resourceName == null || resourceName.length() <= 0 )
        {
            this.resourceName = "";
        }
        else if ( resourceName.startsWith( "/" ) )
        {
            this.resourceName = resourceName.substring( 1 );
        }
        else
        {
            this.resourceName = resourceName;
        }

        this.file = file;

        this.trace = trace;

        startTime = System.currentTimeMillis();
    }

    /**
     * The base URL of the repository, e.g. "http://repo1.maven.org/maven2/". Unless the URL is unknown, it will be
     * terminated by a trailing slash.
     * 
     * @return The base URL of the repository or an empty string if unknown, never {@code null}.
     */
    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    /**
     * The path of the resource relative to the repository's base URL, e.g. "org/apache/maven/maven/3.0/maven-3.0.pom".
     * 
     * @return The path of the resource, never {@code null}.
     */
    public String getResourceName()
    {
        return resourceName;
    }

    /**
     * Gets the local file being uploaded or downloaded. When the repository system merely checks for the existence of a
     * remote resource, no local file will be involved in the transfer.
     * 
     * @return The source/target file involved in the transfer or {@code null} if none.
     */
    public File getFile()
    {
        return file;
    }

    /**
     * The size of the resource in bytes. Note that the size of a resource during downloads might be unknown to the
     * client which is usually the case when transfers employ compression like gzip. In general, the content length is
     * not known until the transfer has {@link TransferListener#transferStarted(TransferEvent) started}.
     * 
     * @return The size of the resource in bytes or a negative value if unknown.
     */
    public long getContentLength()
    {
        return contentLength;
    }

    /**
     * Sets the size of the resource in bytes.
     * 
     * @param contentLength The size of the resource in bytes or a negative value if unknown.
     * @return This resource for chaining, never {@code null}.
     */
    public TransferResource setContentLength( long contentLength )
    {
        this.contentLength = contentLength;
        return this;
    }

    /**
     * Gets the timestamp when the transfer of this resource was started.
     * 
     * @return The timestamp when the transfer of this resource was started.
     */
    public long getTransferStartTime()
    {
        return startTime;
    }

    /**
     * Gets the trace information that describes the higher level request/operation during which this resource is
     * transferred.
     * 
     * @return The trace information about the higher level operation or {@code null} if none.
     */
    public RequestTrace getTrace()
    {
        return trace;
    }

    @Override
    public String toString()
    {
        return getRepositoryUrl() + getResourceName() + " <> " + getFile();
    }

}
