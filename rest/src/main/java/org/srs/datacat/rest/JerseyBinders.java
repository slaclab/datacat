
package org.srs.datacat.rest;

import javax.sql.DataSource;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.srs.datacat.security.DcUserLookupService;
import org.srs.datacat.vfs.DcFileSystemProvider;

/**
 * 
 * @author bvan
 */
public class JerseyBinders {
    public static class FsBinder extends AbstractBinder {
        private final DcFileSystemProvider provider;

        FsBinder(DcFileSystemProvider provider){
            this.provider = provider;
        }

        @Override
        protected void configure(){
            bind(provider).to(DcFileSystemProvider.class);
        }
    }

    public static class LookupServiceBinder extends AbstractBinder {
        private final DcUserLookupService lookupService;

        LookupServiceBinder(DcUserLookupService lookupService){
            this.lookupService = lookupService;
        }

        @Override
        protected void configure(){
            bind(lookupService).to(DcUserLookupService.class);
        }
    }

    public static class DataSourceBinder extends AbstractBinder {
        private final DataSource dataSource;

        DataSourceBinder(DataSource dataSource){
            this.dataSource = dataSource;
        }

        @Override
        protected void configure(){
            bind(dataSource).to(DataSource.class);
        }
    }

}
