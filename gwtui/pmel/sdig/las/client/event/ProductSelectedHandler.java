package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by rhs on 2/10/17.
 */
public interface ProductSelectedHandler extends EventHandler {
    void onProductSelected(ProductSelected event);
}
