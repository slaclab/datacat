/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.search;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.freehep.util.argv.ArgumentFormatException;
import org.freehep.util.argv.MissingArgumentException;
import org.srs.datacat.client.DataCatClient;
import org.srs.datacat.client.sql.DataCatUtilities;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.DatacatObject.Type;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetGroup;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.dataset.DatasetBuilder;
//import org.srs.datacat.client.sql.Dataset;
//import org.srs.datacat.client.sql.DatasetLocation;

/**
 *
 * @author bvan
 */
public class SearchHelper {
    
   /*private BooleanOption recurseFoldersOpt = new BooleanOption("--recurse", "-R", "Recurse sub-folders");
   private BooleanOption searchFoldersOpt = new BooleanOption("--search-folders", "-f", "Search for datasets inside folder(s)");
   private BooleanOption searchGroupsOpt = new BooleanOption("--search-groups", "-g", "Search in groups.  This option is superseded by the -G (--group) option if they are both supplied.");
   private StringOption datasetGroupOpt = new StringOption("--group", "-G", "group name", null, "Dataset Group under which to search for datasets.");
   private MultiStringOption siteOpt = new MultiStringOption("--site", "-S", "site name", "Name of Site to search.  May be used multiple times to specify a list of sites in which case order is taken as preference.  Defaults to the Master-location if not provided.");
   private StringOption filterOpt = new StringOption("--filter", "-F", "filter expression", null, "Criteria by which to filter datasets.  ie: \'DatasetDataType==\"MERIT\" && nMetStart>=257731220 && nMetStop <=257731580\'");
   //private MultiStringOption displayOpt = new MultiStringOption("--display", "-d", "meta name", "Name of meta-data field to display in output.  Default is to display only the file location.  May be used multiple times to specify an ordered list of fields to display.");
   private MultiStringOption sortOpt = new MultiStringOption("--sort", "-s", "meta name", "Name of meta-data field to sort on.  May be used multiple times to specify a list of fields to sort on.  Order determines precedence.");
   private BooleanOption showUnscannedLocationsOpt = new BooleanOption("--show-unscanned-locations", "If no \"OK\" (ie: verified by file crawler) location exists, display first location (if any) which has not yet been scanned.  If this option and '--show-non-ok-locations' are both specified, an unscanned location will be returned before a non-ok location regardless of their sequence in the ordered site list.");
   private BooleanOption showNonOkLocationsOpt = new BooleanOption("--show-non-ok-locations", "If no \"OK\" (ie: verified by file crawler) location exists, display first location (if any) which exists in the list of sites.");
   private StringParameter logicalFolderPar = new StringParameter("logical folder", "Logical Folder Path at which to begin performing the search.");
   
   /**
    * Creates a new instance of FindDatasets

   public FindDatasets(DataCatCommand parent) {
      this.parent = parent;
      parser.add(recurseFoldersOpt);
      parser.add(searchFoldersOpt);
      parser.add(searchGroupsOpt);
      parser.add(datasetGroupOpt);
      parser.add(siteOpt);
      parser.add(filterOpt);
      parser.add(displayOpt);
      parser.add(sortOpt);
      parser.add(showUnscannedLocationsOpt);
      parser.add(showNonOkLocationsOpt);
      parser.add(logicalFolderPar);
   }
    */
   
   public List<Dataset> search(Connection conn, DatacatObject root, 
           List<String> sortList, boolean recurseFolders, boolean searchFolders, 
           boolean searchGroups, boolean showUnscanned, boolean showNonOk, List<String> sites, String filter) 
           throws SQLException, MissingArgumentException, IllegalArgumentException, ArgumentFormatException, IOException, DataCatUtilities.DataCatException, CompilationException
   {
      //getParser().parse(args);
      DataCatClient client = new DataCatClient(conn, "LINEMODE CLIENT");
      List<Dataset> restDatasets = new ArrayList<>();
      String logicalFolderPath = null;
      String datasetGroup = "";
      if (root != null) {
          if (root instanceof LogicalFolder) {
              logicalFolderPath = root.getPath();
          } else if (root != null && root instanceof DatasetGroup) {
              LinkedList<DatacatObject> plist = root.getPathList();
              //plist.removeLast();
              String gpath = "";
              for (DatacatObject o : plist) {
                  gpath = gpath + "/" + o.getName();
              }
              datasetGroup = root.getName();
              logicalFolderPath = gpath;
          }
      }
       
      try {
      
         // Parameter Values:



         // Reformat site list to an array:
         List<String> siteList = sites;
         String siteArr[] = null;
         if (siteList != null && siteList.size() > 0) {
            siteArr = new String[siteList.size()];
            for (int i=0; i<siteList.size(); i++) {
               siteArr[i] = siteList.get(i);
            }
         }

         /* Reformat display list to an array:
         List<String> displayList = (List<String>)displayOpt.getValue();
         String displayArr[] = null;
         if (displayList != null && displayList.size() > 0) {
            displayArr = new String[displayList.size()];
            for (int i=0; i<displayList.size(); i++)
               displayArr[i] = displayList.get(i);
         } */
         
         // Reformat sort list to an array:
         //List<String> sortList = (List<String>)sortOpt.getValue();
         String sortArr[] = null;
         if (sortList != null && sortList.size() > 0) {
            sortArr = new String[sortList.size()];
            for (int i=0; i<sortList.size(); i++)
               sortArr[i] = sortList.get(i);
         }

         //String filter = filterOpt.getValue();
         String[] metafields = null;
         // query Data Catalog for datasets:
         System.out.println("logicalFolderPath" + logicalFolderPath + "," +  recurseFolders + "," + searchFolders + "," + datasetGroup +  "," + searchGroups + "," + filter +"," + siteArr+ "," + sortArr);
         List<org.srs.datacat.client.sql.Dataset> datasets = client.getDatasets(logicalFolderPath, recurseFolders, searchFolders, datasetGroup, searchGroups, filter, siteArr, metafields /* TODO: Fix so it returns all metafields */, sortArr);
         
         // display datasets:
         for (org.srs.datacat.client.sql.Dataset ds: datasets) {
            org.srs.datacat.client.sql.DatasetLocation location = ds.getFirstOKLocation();
            if (location == null && showUnscanned) { // if we didn't find an 'OK' dataset, see if user will accept an unscanned one...
               location = ds.getFirstUnscannedLocation();
            }
            if (location == null && showNonOk) { // no ok (or unscanned) location exists, if user will take a non-ok location, see if one exists...
               for (org.srs.datacat.client.sql.DatasetLocation loc: ds.getLocations().values()) {
                  if (loc.getPath() != null && !loc.getPath().trim().isEmpty()) {
                     location=loc;
                     break;
                  }
               }
            }
            
            

            if (location != null) {
                DatasetBuilder r = DatasetBuilder.create();
                r.name(ds.getName());
                ds.getMetaData();
                restDatasets.add(convertDataset(ds));
               }
         }
      } finally {
         client.close();
      }
      
      return restDatasets;
   }
   
    
   private Dataset convertDataset(org.srs.datacat.client.sql.Dataset ds){
       
       DatasetBuilder s = DatasetBuilder.create();
       
       s.locationPk(ds.getMasterLocation().getLocationPK());
       s.checkSum(ds.getCheckSum());
       s.eventCount(ds.getEvents());
       s.fileSystemPath(ds.getMasterLocation().getPath());
       s.fileSize(ds.getByteCount());
       s.site(ds.getMasterLocation().getSite());
       s.scanStatus(ds.getMasterLocation().getStatus());
       s.runMax(ds.getRunMax());
       s.runMin(ds.getRunMin());
       
       s.versionPk(ds.getVersionPK());       
       s.datasetSource(ds.getSource());
       s.versionId(ds.getVersionID());
       s.taskName(null);

       s.pk(ds.getPK());
       s.parentPk(ds.getLogicalFolderPK() + ds.getDatasetGroupPK()); // TODO: fix this ugly
       s.parentType( ds.getLogicalFolderPK() != 0 ? Type.FOLDER : Type.GROUP);
       s.name(ds.getName());
       s.datasetFileFormat(ds.getFileFormat());
       s.datasetDataType(ds.getDataType());
       
       if(!ds.getMetaData().isEmpty())
           s.metadata(ds.getMetaData());
       
       return s.build();
   }
}
