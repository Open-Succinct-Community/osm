package in.succinct.osm.controller;

import com.venky.core.util.ObjectHolder;
import com.venky.geo.GeoCoordinate;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import com.venky.swf.views.View;
import in.succinct.osm.db.model.Location;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.sandbox.document.LatLonBoundingBox;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.Query;

import java.math.BigDecimal;
import java.util.List;

public class LocationsController extends ModelController<Location> {
    public LocationsController(Path path) {
        super(path);
    }
    
    @Override
    @RequireLogin(false)
    public View index() {
        return super.index();
    }
    
    @Override
    @RequireLogin(false)
    public View search() {
        return super.search();
    }
    
    @Override
    @RequireLogin(false)
    public View search(String strQuery) {
        return super.search(strQuery);
    }
    
    @RequireLogin(false)
    public View reverse(){
        String lat = getPath().getHeader("Lat");
        String lng = getPath().getHeader(  "Lng");
        String radius = getPath().getHeader("radius");
        
        ModelReflector<Location> reflector = getReflector();
        TypeConverter<BigDecimal> tc = reflector.getJdbcTypeHelper().getTypeRef(BigDecimal.class).getTypeConverter();
        
        if (!reflector.isVoid(lat) && !reflector.isVoid(lng) && reflector.getIndexedFields().contains("LAT")) {
            GeoCoordinate center = new GeoCoordinate(tc.valueOf(lat),tc.valueOf(lng));
            
            Query query = LatLonPoint.newDistanceQuery("_GEO_LOCATION_", tc.valueOf(lat).doubleValue(), tc.valueOf(lng).doubleValue(),
                    tc.valueOf(radius == null ? 1000 : radius).doubleValue());
            
            ObjectHolder<Location> closest = new ObjectHolder<>(null);
            getIndexer().fire(query, 1, new ResultCollector() {
                @Override
                public void collect(Document doc) {
                    Location current  = Database.getTable(Location.class).newRecord();
                    setLocation(current,doc);
                    Location old = closest.get();
                    double old_distance = old == null ? Double.POSITIVE_INFINITY : old.getTxnProperty("distance");
                    double current_distance = current.getTxnProperty("distance");
                    if (current_distance < old_distance){
                        closest.set(current);
                    }
                }
                private void setLocation(Location location, Document doc){
                    TypeConverter<BigDecimal> converter = getReflector().getJdbcTypeHelper().getTypeRef(BigDecimal.class).getTypeConverter();
                    BigDecimal lat  = converter.valueOf(doc.getField("LAT").stringValue());
                    BigDecimal lng = converter.valueOf(doc.getField("LNG").stringValue());
                    location.setLat(lat);
                    location.setLng(lng);
                    location.setId(Long.parseLong(doc.getField("ID").stringValue()));
                    location.setTxnProperty("distance",new GeoCoordinate(location).distanceTo(center));
                }
                
                @Override
                public int count() {
                    return 0;
                }
            });
            if (closest.get() != null) {
                Location location = Database.getTable(Location.class).getRefreshed(closest.get());
                return show(location);
            }
        }
        throw new RuntimeException("Cannot reverse lookup");
    }
    @Override
    protected void finalizeQuery(Builder builder) {
        super.finalizeQuery(builder);
        String lat = getPath().getHeader("Lat");
        String lng = getPath().getHeader(  "Lng");
        String radius = getPath().getHeader(  "radius");
        
        ModelReflector<Location> reflector = getReflector();
        TypeConverter<BigDecimal> tc = reflector.getJdbcTypeHelper().getTypeRef(BigDecimal.class).getTypeConverter();
        
        if (!reflector.isVoid(lat) && !reflector.isVoid(lng) && reflector.getIndexedFields().contains("LAT")) {
            Query query = LatLonPoint.newDistanceQuery("_GEO_LOCATION_",tc.valueOf(lat).doubleValue(), tc.valueOf(lng).doubleValue(),tc.valueOf(radius).doubleValue()*1000.0);
            builder.add(query, Occur.MUST);
        }
    }
}
