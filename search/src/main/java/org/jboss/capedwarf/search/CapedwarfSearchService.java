/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.capedwarf.search;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.search.GetIndexesRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchService;
import com.google.appengine.api.search.SearchServiceConfig;
import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.capedwarf.common.app.Application;
import org.jboss.capedwarf.common.async.Wrappers;
import org.jboss.capedwarf.common.infinispan.InfinispanUtils;
import org.jboss.capedwarf.shared.config.CacheName;

/**
 * @author <a href="mailto:mluksa@redhat.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfSearchService implements SearchService {

    private String namespace;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private Double deadline; // TODO
    private Cache<CacheKey, CacheValue> cache;

    private CapedwarfSearchService(String namespace, Double deadline, boolean validate) {
        if (namespace != null) {
            if (validate) {
                NamespaceManager.validateNamespace(namespace);
            }
        } else {
            namespace = resolveNamespace(namespace);
        }
        this.namespace = namespace;
        this.deadline = deadline;
        initCache();
    }

    public CapedwarfSearchService(SearchServiceConfig config) {
        this(config.getNamespace(), config.getDeadline(), false);
    }

    private static String resolveNamespace(String namespace) {
        if (namespace == null) {
            namespace = NamespaceManager.get();
            return namespace == null ? "" : namespace;
        } else {
            return namespace;
        }
    }

    private void initCache() {
        String appId = Application.getAppId();
        ClassLoader classLoader = Application.getAppClassLoader();
        this.cache = InfinispanUtils.<CacheKey, CacheValue>getCache(appId, CacheName.SEARCH).getAdvancedCache().with(classLoader);
    }

    public Index getIndex(IndexSpec indexSpec) {
        return new CapedwarfSearchIndex(indexSpec.getName(), getNamespace(), cache);
    }

    public Index getIndex(IndexSpec.Builder builder) {
        return getIndex(builder.build());
    }

    public String getNamespace() {
        return namespace;
    }

    @SuppressWarnings("unchecked")
    public GetResponse<Index> getIndexes(GetIndexesRequest request) {
        GetIndexesMapper mapper = new GetIndexesMapper(request, getNamespace());
        GetIndexesReducer reducer = new GetIndexesReducer();
        GetIndexesCollator collator = new GetIndexesCollator(request, cache);

        MapReduceTask<CacheKey, CacheValue, FullIndexSpec, String> task = new MapReduceTask<CacheKey, CacheValue, FullIndexSpec, String>(cache);
        return task.mappedWith(mapper).reducedWith(reducer).execute(collator);
    }

    public GetResponse<Index> getIndexes(GetIndexesRequest.Builder builder) {
        return getIndexes(builder.build());
    }

    public Future<GetResponse<Index>> getIndexesAsync(final GetIndexesRequest getIndexesRequest) {
        return Wrappers.future(new Callable<GetResponse<Index>>() {
            public GetResponse<Index> call() throws Exception {
                return getIndexes(getIndexesRequest);
            }
        });
    }

    public Future<GetResponse<Index>> getIndexesAsync(final GetIndexesRequest.Builder builder) {
        return Wrappers.future(new Callable<GetResponse<Index>>() {
            public GetResponse<Index> call() throws Exception {
                return getIndexes(builder);
            }
        });
    }

    public void clear() {
        cache.clear();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }
}
