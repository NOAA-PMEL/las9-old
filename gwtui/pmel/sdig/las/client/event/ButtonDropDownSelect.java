package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 9/13/15.
 */
public class ButtonDropDownSelect extends GwtEvent<ButtonDropDownSelect.Handler> {

    public static final Type<ButtonDropDownSelect.Handler> TYPE = new Type<ButtonDropDownSelect.Handler>();

    String value;

    public ButtonDropDownSelect() {
    }

    public ButtonDropDownSelect(String value) {
        this.value = value;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }
    @Override
    protected void dispatch(Handler handler) {
        handler.onDropDownSelect(this);

    }
    public interface Handler extends EventHandler {
        public void onDropDownSelect(ButtonDropDownSelect event);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
