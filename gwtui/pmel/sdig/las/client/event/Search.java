package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class Search extends GwtEvent<SearchHandler> {

    String query;

    public Search(String q) {
        query = q;
    }

    public static Type<SearchHandler> TYPE = new Type<SearchHandler>();

    public Type<SearchHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(SearchHandler handler) {
        handler.onSearch(this);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
