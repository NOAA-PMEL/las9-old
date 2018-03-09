package pmel.sdig.las.client.state;

import pmel.sdig.las.shared.autobean.*;

import java.util.List;

/**
 * Created by rhs on 6/2/16.
 */
public class PanelState {

    LASRequest lasRequest;
    ProductResults productResults;

    public String getImageUrl() {

        ResultSet resultSet = productResults.getResultSet();
        List<Result> results = resultSet.getResults();

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if ( result.getType().equals("image") ) {
                return result.getUrl();
            }
        }
        return null;
    }

    public MapScale getMapScale() {
        return productResults.getMapScale();
    }

    public LASRequest getLasRequest() {
        return lasRequest;
    }

    public void setLasRequest(LASRequest lasRequest) {
        this.lasRequest = lasRequest;
    }

    public ProductResults getProductResults() {
        return productResults;
    }

    public void setProductResults(ProductResults productResults) {
        this.productResults = productResults;
    }
}
