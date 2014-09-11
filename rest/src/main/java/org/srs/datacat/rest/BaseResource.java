
package org.srs.datacat.rest;

import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.ws.rs.core.Context;
import org.srs.datacat.vfs.DcFileSystemProvider;

/**
 *
 * @author bvan
 */
public class BaseResource {
    
    @Context HttpServletRequest request;
    @Context HttpServletResponse response;
    @Inject DcFileSystemProvider provider;
    @Inject DataSource dataSource;

    private void initDatasource(String jndi) throws NamingException {
        javax.naming.Context ctx = new InitialContext();
        // relative to standard JNDI root for J2EE app
        javax.naming.Context envCtx = (javax.naming.Context) ctx.lookup( "java:comp/env" );
        this.dataSource = (DataSource) envCtx.lookup( jndi );
    }
    
    public Connection getConnection() throws SQLException {
        if(dataSource == null){
            try {
                initDatasource("jdbc/datacat-prod");
            } catch(NamingException ex) {
                throw new SQLException("Unable to initialize datasource", ex);
            }
        }
        return dataSource.getConnection();
    }
    
    public DcFileSystemProvider getProvider(){
        return this.provider;
    }
    
    public <T> List<T> paginateList( List<T> list, int offset, int max){
        if(offset > 0){
            if(offset > list.size()){
                list.clear();
            } else {
                list = new ArrayList<>( list.subList( offset, list.size() ) );
            }
        }
        if(max > 0){
            if(max < list.size()){ list = new ArrayList<>( list.subList( 0, max ) ); }
        }
        setPaginationHeaders( offset, max, list.size(), true);
        return list;
    }
    
    protected void setPaginationHeaders(Integer offset, Integer max, Integer size, boolean showSize){
        StringBuilder link = new StringBuilder();
        boolean next = offset + max < size;
        boolean previous = offset > 0 && offset < size;
        StringBuffer baseURL = request.getRequestURL();
        
        if(previous){
            int iOffset = offset - max < 0 ? 0 : offset - max;
            link.append( getLink(baseURL.toString(), request.getParameterMap(), iOffset, "prev"));
        }
        if(next){
            int iOffset = offset + max;
            if(link.length() != 0){ link.append(","); }
            link.append( getLink(baseURL.toString(), request.getParameterMap(), iOffset, "next"));
        }
        
        response.setHeader( "Link", link.toString());
        response.setHeader( "x-pagination-offset", offset.toString());
        response.setHeader( "x-pagination-max", max.toString());
        if(showSize){
            response.setHeader( "x-pagination-size", size.toString());
        }
    }
    
    private String getLink(String bURL, Map<String, String[]> reqParams, int iOffset, String rel){
        HashMap<String, String[]> params = new HashMap<>( reqParams );
        params.remove( "offset" );
        params.put( "offset", new String[]{Integer.toString( iOffset )} );
        StringBuilder link = new StringBuilder( bURL );
        link.append( "?" );
        String key = null;
        for(Iterator<String> itr = params.keySet().iterator(); itr.hasNext(); ){
            key = itr.next();
            String[] vals = params.get( key );
            for(int i = 0; i < vals.length; i++){
                link.append( key );
                link.append( "=" );
                link.append( URLEncoder.encode( vals[i] ) );
                if(i < vals.length - 1){
                    link.append( "&" );
                }
            }
            if(itr.hasNext()){
                link.append( "&" );
            }
        }
        link.append( "; rel=" + rel );
        return link.toString();
    }
    
}
