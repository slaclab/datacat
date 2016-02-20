package org.srs.datacat.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.srs.datacat.client.auth.HeaderFilter;
import org.srs.datacat.client.exception.DcClientException;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcPermissions;
import org.srs.datacat.model.security.DcUser;
import org.srs.datacat.rest.App;
import org.srs.datacat.rest.resources.ContainerResource;
import org.srs.datacat.rest.resources.DatasetsResource;
import org.srs.datacat.rest.resources.PathResource;
import org.srs.datacat.rest.resources.PermissionsResource;
import org.srs.datacat.shared.Provider;
import org.srs.datacat.test.DbHarness;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.vfs.TestUtils;

/**
 *
 * @author bvan
*/
public class DatasetClientsTest extends JerseyTest {

    @Override
    protected Application configure(){
        try {
            DbHarness harness = DbHarness.getDbHarness();
        
            ResourceConfig app = new App(harness.getDataSource(), TestUtils.getLookupService())
                    .register(TestSecurityFilter.class)
                    .register(ContainerResource.class)
                    .register(PermissionsResource.class)
                    .register(PathResource.class)
                    .register(DatasetsResource.class);
            app.property(ServerProperties.TRACING, "ALL");
            return app;
        } catch(SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Client getDatacatClient() throws URISyntaxException{
        HashMap<String, Object> authHeader = new HashMap<>();
        authHeader.put("authentication", DbHarness.TEST_USER);
        HeaderFilter filter = new HeaderFilter(authHeader);
        Client c = ClientBuilder.newBuilder().setUrl(getBaseUri())
                .addClientRequestFilter(filter)
                .build();
        return c;
    }
    
    @Test
    public void testCreation() throws JsonProcessingException, IOException, URISyntaxException {
        Client client = getDatacatClient();
        ContainerClientTest.generateFolders(client, 1);
        DatasetModel created = createOne(client);
    }
    
    @Test
    public void testDatasetAcl() throws JsonProcessingException, IOException, URISyntaxException {
        Client client = getDatacatClient();
        ContainerClientTest.generateFolders(client, 1);
        DatasetModel created = createOne(client);
        Assert.assertTrue(client.getAcl(created.getPath()).get(0).toString().contains("SRS"));
        HashSet<DcPermissions> expectedPermissions = new HashSet<>(
                Arrays.asList(DcPermissions.READ, DcPermissions.INSERT,
                        DcPermissions.WRITE, DcPermissions.DELETE, DcPermissions.ADMIN
                )
        );

        Assert.assertEquals(expectedPermissions, client.getPermissions(created.getPath(), null).getPermissions());
        expectedPermissions.clear();
        expectedPermissions.add(DcPermissions.READ);
        Assert.assertEquals(expectedPermissions, 
                client.getPermissions(created.getPath(), DcGroup.fromSpec("fake@")).getPermissions());
    }
        
    public DatasetModel createOne(Client client) throws JsonProcessingException, URISyntaxException{
        String parent = "/testpath/folder00000";
        String name = "dataset0001";
        
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put(DbHarness.numberName, DbHarness.numberMdValues[0]);
        metadata.put(DbHarness.alphaName, DbHarness.alphaMdValues[0]);
        
        ModelProvider provider = new Provider();
        
        DatasetModel newDataset = provider.getDatasetBuilder()
                .name(name)
                .dataType(HSqlDbHarness.JUNIT_DATASET_DATATYPE)
                .fileFormat(HSqlDbHarness.JUNIT_DATASET_FILEFORMAT)
                .versionId(DatasetView.NEW_VER)
                .versionMetadata(metadata)
                .build();
        
        return client.createDataset(parent, newDataset);
    }

    @PreMatching
    @Priority(Priorities.AUTHENTICATION)
    public static class TestSecurityFilter implements ContainerRequestFilter {

        public TestSecurityFilter(){
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException{
            final String userName = requestContext.getHeaderString("authentication");

            SecurityContext sc = new SecurityContext() {

                @Override
                public Principal getUserPrincipal(){
                    if(userName == null){
                        return null;
                    }
                    return new DcUser(userName);
                }

                @Override
                public boolean isUserInRole(String role){
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isSecure(){
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getAuthenticationScheme(){
                    throw new UnsupportedOperationException();
                }
            };
            requestContext.setSecurityContext(sc);
        }

    }

}
