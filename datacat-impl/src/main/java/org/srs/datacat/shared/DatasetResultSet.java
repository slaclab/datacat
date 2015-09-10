package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Iterator;
import java.util.List;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.shared.DatasetResultSet.Builder;

/**
 * Implementation of DatasetResultSetModel with Jackson annotations.
 * @author bvan
 */
@JsonTypeName(value="searchResults")
@JsonDeserialize(builder=Builder.class)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="_type", defaultImpl=DatasetContainer.class)
public class DatasetResultSet implements DatasetResultSetModel {

    private List<DatasetModel> results;
    private Integer count;

    public DatasetResultSet(List<DatasetModel> results, Integer count){
        this.results = results;
        this.count = count;
    }

    @Override
    public List<DatasetModel> getResults(){
        return results;
    }

    @Override
    public Integer getCount(){
        return count;
    }

    @Override
    @JsonIgnore
    public Iterator<DatasetModel> iterator(){
        return results.iterator();
    }

    /**
     * Implementation of Builder.
     */
    public static class Builder implements DatasetResultSetModel.Builder {

        private List<DatasetModel> results;
        private Integer count;

        public Builder(){ }

        @Override
        public DatasetResultSet build(){
            return new DatasetResultSet(results, count);
        }

        @Override
        @JsonSetter 
        public Builder results(List<DatasetModel> val){
            this.results = val;
            return this;
        }

        @Override
        @JsonSetter 
        public Builder count(Integer val){
            this.count = val;
            return this;
        }

    }

}
