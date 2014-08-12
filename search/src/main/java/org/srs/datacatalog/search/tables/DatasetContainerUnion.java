package org.srs.datacatalog.search.tables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Op;
import org.zerorm.core.Param;
import org.zerorm.core.Select;
import org.zerorm.core.Val;
import org.zerorm.core.interfaces.MaybeHasAlias;
import org.srs.datacatalog.search.tables.Folder.FolderSelect;
import org.srs.datacatalog.search.tables.Group.GroupSelect;
import org.srs.rest.datacat.shared.DatacatObject;
import org.srs.rest.datacat.shared.DatasetGroup;
import org.srs.rest.datacat.shared.LogicalFolder;

/**
 *
 * @author bvan
 */
public class DatasetContainerUnion extends MetajoinedStatement {
    public Group dsg = new Group().as( "dsg", Group.class );
    public Folder dslf = new Folder().as( "dslf", Folder.class );
    public Column<String> type;
    
    public GroupSelect dsg_sel = dsg.new GroupSelect(dsg.datasetGroup.as( "pk"), dsg.name, 
            new Val<>("GROUP", "type"), dsg.datasetLogicalFolder.as( "parent") );
    
    public FolderSelect dslf_sel = dslf.new FolderSelect(dslf.datasetLogicalFolder.as( "pk"), dslf.name, 
            new Val<>("FOLDER", "type"), dslf.parent.as( "parent") );
    
    private Select union = Select.unionAll( dsg_sel, dslf_sel);
    public Select statement;
    
    public DatasetContainerUnion(){
        type = new Column("type", union);
        from(union.as( "cntnr_un"));
    }
    
    @Override
    public boolean hasParams(){
        return union.hasParams();
    }

    @Override
    public List<Param> getParams(){
        List<Param> params = union.getParams();
        params.addAll( super.getParams());
        return params;
    }

    @Override
    public List<MaybeHasAlias> getColumns(){
        List<MaybeHasAlias> columns = new ArrayList<>();
        for(MaybeHasAlias c: union.getColumns()){
            columns.add( new Column(c.canonical(), this));
        }
        return columns;
    }
    
    public DatasetContainerUnion parentIn(Select statment){
        dsg_sel.where( dsg.datasetLogicalFolder.in( statement ) );
        dslf_sel.where( dslf.parent.in( statement ) );
        return this;
    }
    
    public DatasetContainerUnion parentIs(Long pk){
        dsg_sel.where( dsg.datasetLogicalFolder.eq( pk ) );
        dslf_sel.where( dslf.parent.eq( pk ) );
        return this;
    }
    
    public DatasetContainerUnion folderIs(Long pk){
        dsg_sel.where( dsg.datasetLogicalFolder.eq( pk ) );
        dslf_sel.where( dslf.datasetLogicalFolder.eq( pk ) );
        return this;
    }

    @Override
    public Map<String, Select> getMetajoins(){
        HashMap<String, Select> joinMap = new LinkedHashMap<>();
        joinMap.putAll( dsg_sel.getMetajoins());
        joinMap.putAll( dslf_sel.getMetajoins());
        return joinMap;
    }
    
    @Override
    public List<MaybeHasAlias> getAvailableSelections(){
        return union.getColumns();
    }

    @Override
    public Column getMetajoinColumn(){
        return new Column("pk", union);
    }

    @Override
    public Metatable getMetatableForType(String alias, Class type){
        throw new UnsupportedOperationException( "Not supported yet." );
    }
    
    public DatasetContainerUnion groupsAndFoldersStarting(Long pk){
        return null;
    }
    
    public DatasetContainerUnion groupsAndFoldersIn(Select statement){
        return null;
    }
    
    public static void main(String[] argv) throws Exception {
        DatasetContainerUnion union = new DatasetContainerUnion();
        union.as( "cntnr");
        
        System.out.println(union.formatted());
        DatacatObject parent = new DatasetGroup.Builder().pk( 94L ).build();
        boolean searchGroups = true;
        boolean recurseFolders = false;
        boolean searchFolders = false;
        
        if(parent instanceof LogicalFolder){
            if(recurseFolders){
                union.parentIn( Folder.recursiveFoldersFrom( parent.getPk()) );
            } else {
                union.folderIs( parent.getPk() );
            }
        }
        
        if(parent instanceof DatasetGroup){
            union.dsg_sel.where( union.dsg.datasetGroup.eq( parent.getPk()) );
            searchFolders = false;
        }
        
        ArrayList<String> typeList = new ArrayList<>();
        if(searchGroups)
            typeList.add( "GROUP" );
        if(searchFolders)
            typeList.add( "FOLDER");
        union.where( union.type.in( typeList ) );
        
        //union.where( sd.evaluateNode( ast.getRoot(), union) );
        
        System.out.println(union.formatted());
    }
    
    @Override
    public Column setupMetadataJoin(String metaName, Object tRight){
        String columnName = dsg_sel.setupMetadataJoin( metaName, tRight ).canonical();
        dslf_sel.setupMetadataJoin( metaName, tRight );        
        return new Column(columnName, this);
    }
    
    public Expr getMetadataExpression(Object tLeft, Op tOper, Object tRight){
        
        for(MaybeHasAlias c: getColumns()){
            if(c.canonical().equals( tLeft )){
                return tOper.apply( c, tRight );
            }
        }
        
        if(getMetajoins().get( tLeft ) == null){  // join not yet set up
            Param p = new Param( tLeft.toString() );
            p.setValue( tRight );
            return tOper.apply( setupMetadataJoin(tLeft.toString(), tRight ), p );
        }

        
        Metatable ms = (Metatable) getMetajoins().get( tLeft ).getFrom();

        return tOper.apply( new Column(ms.metaValue.canonical(), ms.metaValue.getJavaType(), this ), tRight );
    }

    @Override
    public String getMetanamePrefix(){
        return "cntr_mv";
    }
}
